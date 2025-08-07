package com.relation.shit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics; // Import for display metrics
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat; // Import for ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.relation.shit.adapter.ChatAdapter;
import com.relation.shit.api.DeepseekApiService;
import com.relation.shit.api.GeminiApiService;
import com.relation.shit.api.QwenApiService;
import com.relation.shit.api.BaseApiService;
import com.relation.shit.model.Agent;
import com.relation.shit.model.Conversation;
import com.relation.shit.model.Knowledge;
import com.relation.shit.model.Message;
import com.relation.shit.utils.SharedPrefManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatActivity extends AppCompatActivity {

  private MaterialToolbar chatToolbar;
  private ImageView chatBackButton;
  private TextView chatTitle;
  private RecyclerView chatRecyclerview;
  private TextInputEditText messageEditText;
  private LinearLayout sendMessageButton;
  private ImageView moreVertIcon;

  private ChatAdapter chatAdapter;
  private List<Message> messageList;
  private SharedPrefManager sharedPrefManager;
  private BaseApiService apiService;

  private Conversation currentConversation;
  private Agent currentAgent;

  private static final int MAX_CONTEXT_CHARS_DEEPSEEK = 240000; // ~60k tokens
  private static final int MAX_CONTEXT_CHARS_GEMINI_PRO = 30720; // ~8k tokens
  private static final int MAX_CONTEXT_CHARS_GEMINI_FLASH = 1048576; // 1M tokens

  private int currentMaxContextChars; // Will be set based on the agent's model
  private static final double SUMMARY_TRIGGER_PERCENTAGE = 0.8;
  private int SUMMARY_TRIGGER_CHARS;

  private boolean isAwaitingResponse = false;

  // Max width for user messages (80% of screen width)
  private int userMessageMaxWidth;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.chat);

    sharedPrefManager = new SharedPrefManager(this);

    Intent intent = getIntent();
    currentAgent = (Agent) intent.getSerializableExtra("agent");
    String conversationId = intent.getStringExtra("conversation_id");

    if (currentAgent == null) {
      Toast.makeText(this, "Error: Agent not found.", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    // Initialize the correct API service based on the agent's API provider
    if ("Deepseek".equals(currentAgent.getApiProvider())) {
      apiService = new DeepseekApiService(sharedPrefManager.getDeepseekApiKey());
      currentMaxContextChars = MAX_CONTEXT_CHARS_DEEPSEEK;
    } else if ("Gemini".equals(currentAgent.getApiProvider())) {
      apiService = new GeminiApiService(sharedPrefManager.getGeminiApiKey());
      // Set context window based on Gemini model
      if (currentAgent.getModel().contains("flash")) {
        currentMaxContextChars = MAX_CONTEXT_CHARS_GEMINI_FLASH;
      } else {
        currentMaxContextChars = MAX_CONTEXT_CHARS_GEMINI_PRO;
      }
    } else if ("Alibaba".equals(currentAgent.getApiProvider())) {
        apiService = new QwenApiService();
        // Set context chars for Qwen models
        currentMaxContextChars = 131072; // Default for now
    } else {
      Toast.makeText(this, "Error: Unknown API provider.", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    // Calculate user message max width (80% of screen width)
    DisplayMetrics displayMetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    userMessageMaxWidth = (int) (displayMetrics.widthPixels * 0.8);

    initializeViews();
    setupToolbar();
    loadOrCreateConversation(conversationId);
    if (apiService instanceof QwenApiService) {
        ((QwenApiService) apiService).setConversation(currentConversation);
    }
    SUMMARY_TRIGGER_CHARS = (int) (currentMaxContextChars * SUMMARY_TRIGGER_PERCENTAGE);
    setupRecyclerView();

    sendMessageButton.setOnClickListener(v -> sendMessage());
    moreVertIcon.setOnClickListener(this::showPopupMenu);
  }

  private void initializeViews() {
    chatToolbar = findViewById(R.id.chat_toolbar);
    chatBackButton = findViewById(R.id.chat_back_button);
    chatTitle = findViewById(R.id.chat_title);
    chatRecyclerview = findViewById(R.id.chat_recyclerview);
    messageEditText = findViewById(R.id.edit_text_message);
    sendMessageButton = findViewById(R.id.button_send_message);
    moreVertIcon = findViewById(R.id.icon_more_vert);
  }

  private void setupToolbar() {
    setSupportActionBar(chatToolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayShowTitleEnabled(false);
    }
    chatBackButton.setOnClickListener(v -> onBackPressed());

    // Set initial toolbar background color to md_theme_surface
    chatToolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_surface));

    // Add scroll listener to RecyclerView for toolbar color change
    chatRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
          // Check if the first item is completely visible (i.e., scrolled to top)
          // If the first visible item is not 0, it means we have scrolled down.
          if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0 && layoutManager.findViewByPosition(0) != null && layoutManager.findViewByPosition(0).getTop() >= 0) {
            // Not scrolled or scrolled to top, set to default surface color
            chatToolbar.setBackgroundColor(ContextCompat.getColor(ChatActivity.this, R.color.md_theme_surface));
          } else {
            // Scrolled, set to primary color (blue-ish from your colors.xml)
            chatToolbar.setBackgroundColor(ContextCompat.getColor(ChatActivity.this, R.color.toolbar)); 
          }
        }
      }
    });
  }

  private void loadOrCreateConversation(String conversationId) {
    if (conversationId != null) {
      currentConversation = sharedPrefManager.getConversationById(conversationId);
    }

    if (currentConversation == null) {
      currentConversation =
          new Conversation(
              UUID.randomUUID().toString(),
              "New Chat with " + currentAgent.getName(),
              currentAgent,
              System.currentTimeMillis());
      messageList = new ArrayList<>();
    } else {
      messageList = currentConversation.getMessages();
    }

    chatTitle.setText(currentConversation.getTitle());
  }

  private void setupRecyclerView() {
    // Pass the calculated max width to the adapter
    chatAdapter = new ChatAdapter(messageList, userMessageMaxWidth);
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    chatRecyclerview.setLayoutManager(layoutManager);
    chatRecyclerview.setAdapter(chatAdapter);
    chatRecyclerview.scrollToPosition(messageList.size() - 1);
  }

  private void showPopupMenu(View view) {
    PopupMenu popup = new PopupMenu(this, view);
    popup.getMenuInflater().inflate(R.menu.chat_menu, popup.getMenu());
    popup.setOnMenuItemClickListener(
        item -> {
          int itemId = item.getItemId();
          if (itemId == R.id.action_knowledge_base) {
            if (currentConversation != null) {
              try {
                Intent intent = new Intent(ChatActivity.this, KnowledgeActivity.class);
                intent.putExtra("conversation_id", currentConversation.getId());
                startActivity(intent);
              } catch (Exception e) {
                Toast.makeText(ChatActivity.this, "Error: Could not open Knowledge Base. Check AndroidManifest.xml", Toast.LENGTH_SHORT).show();
              }
            }
            return true;
          } else if (itemId == R.id.action_insights) {
            if (currentConversation != null) {
              try {
                Intent intent = new Intent(ChatActivity.this, InsightsActivity.class);
                intent.putExtra("conversation_id", currentConversation.getId());
                startActivity(intent);
              } catch (Exception e) {
                Toast.makeText(ChatActivity.this, "Error: Could not open Insights. Check AndroidManifest.xml", Toast.LENGTH_SHORT).show();
              }
            }
            return true;
          }
          return false;
        });
    popup.show();
  }

  private void sendMessage() {
    if (isAwaitingResponse) {
      Toast.makeText(this, "Please wait for the current response.", Toast.LENGTH_SHORT).show();
      return;
    }

    String messageContent = messageEditText.getText().toString().trim();
    if (messageContent.isEmpty()) {
      Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
      return;
    }

    setUiLoadingState(true);

    Message userMessage = new Message("user", messageContent);
    addMessageToChat(userMessage);
    messageEditText.setText("");

    addMessageToChat(new Message("thinking", "ai", "Thinking..."));

    int estimatedFullContextLength = calculateEstimatedContextLength(true);

    if (estimatedFullContextLength > SUMMARY_TRIGGER_CHARS) {
      summarizeAndThenSend(messageContent);
    } else {
      sendActualAiRequest(messageContent);
    }
  }

  private void sendActualAiRequest(String userMessageContent) {
    JSONArray messagesArray;
    try {
      messagesArray = buildMessagesForApi(userMessageContent);
    } catch (JSONException e) {
      Log.e("ChatActivity", "Error building messages for API: " + e.getMessage());
      updateLastMessageWithError("Failed to prepare message for AI.");
      setUiLoadingState(false);
      return;
    }

    apiService.getChatCompletion(
        currentAgent.getModel(),
        messagesArray,
        
        new BaseApiService.ApiResponseCallback<String>() {
          @Override
          public void onSuccess(String aiContent) {
            runOnUiThread(
                () -> {
                  removeLastMessage(); // Remove thinking message
                  addMessageToChat(new Message("ai", aiContent));

                  if (currentConversation.getTitle().startsWith("New Chat with")
                      && messageList.size()
                          == 2) { // First user message and first AI response
                    generateConversationTitle(userMessageContent, aiContent);
                  }

                  considerGeneratingKnowledge(userMessageContent, aiContent);
                  setUiLoadingState(false);
                });
          }

          @Override
          public void onError(String errorMessage) {
            runOnUiThread(
                () -> {
                  updateLastMessageWithError(errorMessage);
                  setUiLoadingState(false);
                });
          }
        });
  }

  private void summarizeAndThenSend(String pendingUserMessage) {
    updateThinkingMessage("Summarizing conversation for better context...");

    String history = getConversationHistoryForSummary(pendingUserMessage);

    apiService.summarizeConversation(
        currentConversation.getId(),
        history,
        new BaseApiService.ApiResponseCallback<Knowledge>() {
          @Override
          public void onSuccess(Knowledge summaryKnowledge) {
            runOnUiThread(
                () -> {
                  sharedPrefManager.addKnowledgeBase(summaryKnowledge);
                  Toast.makeText(
                          ChatActivity.this,
                          "Conversation summarized and saved to knowledge base.",
                          Toast.LENGTH_LONG)
                      .show();
                  Log.d(
                      "ChatActivity",
                      "Conversation Summary Saved: " + summaryKnowledge.getTitle());
                  updateThinkingMessage("Continuing conversation...");
                  sendActualAiRequest(pendingUserMessage);
                });
          }

          @Override
          public void onError(String errorMessage) {
            runOnUiThread(
                () -> {
                  updateLastMessageWithError(
                      "Failed to summarize: " + errorMessage + ". Proceeding anyway.");
                  sendActualAiRequest(pendingUserMessage);
                });
          }
        });
  }

  private void generateConversationTitle(String userMessage, String aiResponse) {
    apiService.generateConversationTitle(
        userMessage,
        aiResponse,
        new BaseApiService.ApiResponseCallback<String>() {
          @Override
          public void onSuccess(String generatedTitle) {
            runOnUiThread(
                () -> {
                  currentConversation.setTitle(generatedTitle);
                  chatTitle.setText(generatedTitle);
                  sharedPrefManager.updateConversation(currentConversation);
                });
          }

          @Override
          public void onError(String errorMessage) {
            Log.e("ChatActivity", "Could not generate title: " + errorMessage);
          }
        });
  }

  private void considerGeneratingKnowledge(String userMessage, String aiResponse) {
    apiService.considerGeneratingKnowledge(
        currentConversation.getId(),
        userMessage,
        aiResponse,
        new BaseApiService.KnowledgeCallback() {
          @Override
          public void onShouldGenerate(Knowledge knowledge) {
            runOnUiThread(
                () -> {
                  sharedPrefManager.addKnowledgeBase(knowledge);
                  Toast.makeText(
                          ChatActivity.this,
                          "New knowledge saved: " + knowledge.getTitle(),
                          Toast.LENGTH_SHORT)
                      .show();
                  Log.d("ChatActivity", "Knowledge Saved: " + knowledge.getTitle());
                });
          }

          @Override
          public void onShouldNotGenerate() {
            Log.d("ChatActivity", "AI decided not to generate knowledge for this turn.");
          }

          @Override
          public void onError(String errorMessage) {
            Log.e("ChatActivity", "Knowledge generation check failed: " + errorMessage);
          }
        });
  }

  private JSONArray buildMessagesForApi(String userMessageContent) throws JSONException {
    JSONArray messagesArray = new JSONArray();
    int currentContextLength = 0;

    String apiProvider = currentAgent.getApiProvider();

    if ("Alibaba".equals(apiProvider)) {
        // For Qwen, we only send the current message. History is managed server-side.
        JSONObject qwenMessage = new JSONObject();
        qwenMessage.put("fid", UUID.randomUUID().toString());
        qwenMessage.put("parentId", currentConversation.getQwenParentId());
        qwenMessage.put("childrenIds", new JSONArray().put(UUID.randomUUID().toString()));
        qwenMessage.put("role", "user");
        qwenMessage.put("content", userMessageContent);
        qwenMessage.put("user_action", "chat");
        qwenMessage.put("files", new JSONArray());
        qwenMessage.put("timestamp", System.currentTimeMillis());
        qwenMessage.put("models", new JSONArray().put(currentAgent.getModel()));
        qwenMessage.put("chat_type", "t2t");

        JSONObject featureConfig = new JSONObject();
        featureConfig.put("thinking_enabled", true); // This could be a setting
        featureConfig.put("output_schema", "phase");
        featureConfig.put("thinking_budget", 38912);
        qwenMessage.put("feature_config", featureConfig);

        JSONObject extra = new JSONObject();
        JSONObject meta = new JSONObject();
        meta.put("subChatType", "t2t");
        extra.put("meta", meta);
        qwenMessage.put("extra", extra);

        qwenMessage.put("sub_chat_type", "t2t");
        qwenMessage.put("parent_id", currentConversation.getQwenParentId());

        return new JSONArray().put(qwenMessage);
    }


    // Add agent prompt
    if ("Deepseek".equals(apiProvider)) {
      JSONObject agentPromptMessage = new JSONObject();
      agentPromptMessage.put("role", "user");
      agentPromptMessage.put("content", currentAgent.getPrompt());
      messagesArray.put(agentPromptMessage);
      currentContextLength += currentAgent.getPrompt().length();
    } else if ("Gemini".equals(apiProvider)) {
      JSONObject agentPromptPart = new JSONObject();
      agentPromptPart.put("text", currentAgent.getPrompt());
      JSONObject agentPromptContent = new JSONObject();
      agentPromptContent.put("role", "user");
      agentPromptContent.put("parts", new JSONArray().put(agentPromptPart));
      messagesArray.put(agentPromptContent);
      currentContextLength += currentAgent.getPrompt().length();
    }


    List<Knowledge> relevantKnowledge =
        sharedPrefManager.getKnowledgeBasesForConversation(currentConversation.getId());
    for (Knowledge kb : relevantKnowledge) {
      String knowledgeText = "Knowledge: " + kb.getTitle() + " - " + kb.getContent();
      if (currentContextLength + knowledgeText.length() < currentMaxContextChars) {
        if ("Deepseek".equals(apiProvider)) {
          JSONObject knowledgeMessage = new JSONObject();
          knowledgeMessage.put("role", "user");
          knowledgeMessage.put("content", knowledgeText);
          messagesArray.put(knowledgeMessage);
        } else if ("Gemini".equals(apiProvider)) {
          JSONObject knowledgePart = new JSONObject();
          knowledgePart.put("text", knowledgeText);
          JSONObject knowledgeContent = new JSONObject();
          knowledgeContent.put("role", "user");
          knowledgeContent.put("parts", new JSONArray().put(knowledgePart));
          messagesArray.put(knowledgeContent);
        }
        currentContextLength += knowledgeText.length();
      } else {
        Log.w(
            "ChatActivity",
            "Knowledge base entry too long for context or remaining space: " + kb.getTitle());
      }
    }

    List<Message> historyMessages = new ArrayList<>(messageList);
    if (!historyMessages.isEmpty()
        && historyMessages.get(historyMessages.size() - 1).getType().equals("thinking")) {
      historyMessages.remove(historyMessages.size() - 1);
    }
    if (!historyMessages.isEmpty()
        && historyMessages.get(historyMessages.size() - 1).getRole().equals("user")) {
      historyMessages.remove(historyMessages.size() - 1); // Remove current user message
    }

    Collections.reverse(historyMessages);

    for (Message msg : historyMessages) {
      if (!msg.getType().equals("thinking") && !msg.getType().equals("error")) {
        String msgContent = msg.getContent();
        if (currentContextLength + msgContent.length() < currentMaxContextChars) {
          if ("Deepseek".equals(apiProvider)) {
            JSONObject msgObject = new JSONObject();
            msgObject.put("role", msg.getRole());
            msgObject.put("content", msgContent);
            messagesArray.put(msgObject);
          } else if ("Gemini".equals(apiProvider)) {
            JSONObject msgPart = new JSONObject();
            msgPart.put("text", msgContent);
            JSONObject msgContentObject = new JSONObject();
            msgContentObject.put("role", msg.getRole().equals("ai") ? "model" : "user"); // Gemini uses "model" for AI
            msgContentObject.put("parts", new JSONArray().put(msgPart));
            messagesArray.put(msgContentObject);
          }
          currentContextLength += msgContent.length();
        } else {
          Log.w("ChatActivity", "Conversation history truncated due to context limit.");
          break;
        }
      }
    }

    // Add current user message
    if ("Deepseek".equals(apiProvider)) {
      JSONObject currentUserMsgObject = new JSONObject();
      currentUserMsgObject.put("role", "user");
      currentUserMsgObject.put("content", userMessageContent);
      messagesArray.put(currentUserMsgObject);
    } else if ("Gemini".equals(apiProvider)) {
      JSONObject currentUserPart = new JSONObject();
      currentUserPart.put("text", userMessageContent);
      JSONObject currentUserContent = new JSONObject();
      currentUserContent.put("role", "user");
      currentUserContent.put("parts", new JSONArray().put(currentUserPart));
      messagesArray.put(currentUserContent);
    } else if ("Alibaba".equals(apiProvider)) {
        // Qwen has a very specific message format
        JSONArray qwenMessages = new JSONArray();
        JSONObject qwenMessage = new JSONObject();
        qwenMessage.put("fid", UUID.randomUUID().toString());
        qwenMessage.put("parentId", currentConversation.getQwenParentId()); // This will be null for the first message
        qwenMessage.put("childrenIds", new JSONArray().put(UUID.randomUUID().toString()));
        qwenMessage.put("role", "user");
        qwenMessage.put("content", userMessageContent);
        qwenMessage.put("user_action", "chat");
        qwenMessage.put("files", new JSONArray());
        qwenMessage.put("timestamp", System.currentTimeMillis());
        qwenMessage.put("models", new JSONArray().put(currentAgent.getModel()));
        qwenMessage.put("chat_type", "t2t");

        JSONObject featureConfig = new JSONObject();
        featureConfig.put("thinking_enabled", true); // Or get from agent settings
        featureConfig.put("output_schema", "phase");
        featureConfig.put("thinking_budget", 38912);
        qwenMessage.put("feature_config", featureConfig);

        JSONObject extra = new JSONObject();
        JSONObject meta = new JSONObject();
        meta.put("subChatType", "t2t");
        extra.put("meta", meta);
        qwenMessage.put("extra", extra);

        qwenMessage.put("sub_chat_type", "t2t");
        qwenMessage.put("parent_id", currentConversation.getQwenParentId());

        qwenMessages.put(qwenMessage);
        return qwenMessages; // Return the new format
    }


    return messagesArray;
  }

  private int calculateEstimatedContextLength(boolean includeFullHistory) {
    int length = 0;
    length += currentAgent.getPrompt().length();

    List<Knowledge> allKnowledge =
        sharedPrefManager.getKnowledgeBasesForConversation(currentConversation.getId());
    for (Knowledge kb : allKnowledge) {
      length += ("Knowledge: " + kb.getTitle() + " - " + kb.getContent()).length();
    }

    if (includeFullHistory) {
      for (Message msg : messageList) {
        if (!msg.getType().equals("thinking") && !msg.getType().equals("error")) {
          length += msg.getContent().length();
        }
      }
    }
    return length;
  }

  private String getConversationHistoryForSummary(String pendingUserMessage) {
    StringBuilder historyBuilder = new StringBuilder();
    for (Message msg : messageList) {
      if (!msg.getType().equals("thinking")
          && !msg.getType().equals("error")
          && !msg.getContent().equals(pendingUserMessage)) {
        historyBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
      }
    }
    return historyBuilder.toString();
  }

  private void setUiLoadingState(boolean isLoading) {
    isAwaitingResponse = isLoading;
    messageEditText.setEnabled(!isLoading);
    sendMessageButton.setEnabled(!isLoading);
  }

  private void addMessageToChat(Message message) {
    messageList.add(message);
    chatAdapter.notifyItemInserted(messageList.size() - 1);
    chatRecyclerview.scrollToPosition(messageList.size() - 1);
  }

  private void removeLastMessage() {
    if (!messageList.isEmpty()) {
      messageList.remove(messageList.size() - 1);
      chatAdapter.notifyItemRemoved(messageList.size());
    }
  }

  private void updateThinkingMessage(String text) {
    if (!messageList.isEmpty()) {
      Message lastMessage = messageList.get(messageList.size() - 1);
      if (lastMessage.getType().equals("thinking")) {
        lastMessage.setContent(text);
        chatAdapter.notifyItemChanged(messageList.size() - 1);
      }
    }
  }

  private void updateLastMessageWithError(String errorMessage) {
    if (!messageList.isEmpty()) {
      Message lastMessage = messageList.get(messageList.size() - 1);
      if (lastMessage.getType().equals("thinking")) {
        lastMessage.setType("error");
        lastMessage.setContent(errorMessage);
        lastMessage.setStatus("error");
        lastMessage.setRetryAction(
            () -> {
              if (messageList.size() >= 2
                  && messageList.get(messageList.size() - 2).getRole().equals("user")) {
                Message originalUserMessage = messageList.get(messageList.size() - 2);
                removeLastMessage(); // remove error message
                setUiLoadingState(false);
                sendMessage(); // resend original message
              }
            });
        chatAdapter.notifyItemChanged(messageList.size() - 1);
      }
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    currentConversation.setMessages(messageList);
    sharedPrefManager.updateConversation(currentConversation);
  }

  public Bitmap textAsBitmap(String text, int textSizeDp, int textColor) {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextSize(textSizeDp * getResources().getDisplayMetrics().density);
    paint.setColor(textColor);
    paint.setTextAlign(Paint.Align.LEFT);
    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

    Rect bounds = new Rect();
    paint.getTextBounds(text, 0, text.length(), bounds);
    int width = (int) (bounds.width() + textSizeDp * getResources().getDisplayMetrics().density * 0.5);
    int height = (int) (bounds.height() + textSizeDp * getResources().getDisplayMetrics().density * 0.5);

    Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(image);
    canvas.drawText(text, (width - bounds.width()) / 2f, (height + bounds.height()) / 2f, paint);
    return image;
  }
}