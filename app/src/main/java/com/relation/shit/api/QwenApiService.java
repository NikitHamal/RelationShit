package com.relation.shit.api;

import android.util.Log;
import androidx.annotation.NonNull;
import com.relation.shit.model.Conversation;
import com.relation.shit.model.Knowledge;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QwenApiService implements BaseApiService {

    private final QwenApi qwenApi;
    private Conversation conversation;

    public QwenApiService() {
        this.qwenApi = new QwenApi();
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    @Override
    public void getChatCompletion(String model, JSONArray messages, ApiResponseCallback<String> callback) {
        try {
            // This is the actual user message payload
            final JSONArray userMessages = messages;

            if (conversation.getQwenChatId() == null) {
                // New conversation: Chain of calls -> newChat -> primeWithSystemPrompt -> sendUserMessage
                qwenApi.newChat(model, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        callback.onError("Failed to create new chat: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String responseBody = response.body().string();
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                if (jsonResponse.getBoolean("success")) {
                                    String chatId = jsonResponse.getJSONObject("data").getString("id");
                                    conversation.setQwenChatId(chatId);
                                    primeWithSystemPrompt(model, userMessages, callback);
                                } else {
                                    callback.onError("Failed to create new Qwen chat.");
                                }
                            } catch (JSONException e) {
                                callback.onError("Error parsing new chat response.");
                            }
                        } else {
                            callback.onError("Failed to create new Qwen chat: " + response.message());
                        }
                    }
                });
            } else {
                // Existing conversation, just send the user message
                performCompletion(model, userMessages, callback);
            }
        } catch (JSONException e) {
            callback.onError("Failed to build request: " + e.getMessage());
        }
    }

    private void primeWithSystemPrompt(String model, JSONArray userMessages, ApiResponseCallback<String> callback) throws JSONException {
        // Create a payload for the system prompt
        JSONArray systemPromptPayload = new JSONArray().put(new JSONObject()
            .put("role", "user") // Qwen doesn't have a system role, send as user
            .put("content", conversation.getAgent().getPrompt()));

        qwenApi.getChatCompletion(conversation.getQwenChatId(), null, model, systemPromptPayload, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError("Failed to send system prompt: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("AI error on system prompt: " + response.code());
                    return;
                }
                // We need to consume the response body to find the new parentId
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String jsonData = line.substring(5).trim();
                            if (jsonData.isEmpty()) continue;
                            try {
                                JSONObject data = new JSONObject(jsonData);
                                if (data.has("response.created")) {
                                    JSONObject created = data.getJSONObject("response.created");
                                    conversation.setQwenParentId(created.getString("response_id"));
                                    // Now that we have the parentId from the system prompt response, send the actual user message
                                    try {
                                        performCompletion(model, userMessages, callback);
                                    } catch (JSONException jsonException) {
                                        callback.onError("Failed to build user message request: " + jsonException.getMessage());
                                    }
                                    return; // Exit after we found what we need
                                }
                            } catch (JSONException e) {
                                // Ignore parsing errors in other chunks
                            }
                        }
                    }
                    // If we get here, we didn't find the response.created chunk
                    callback.onError("Could not get parentId from system prompt response.");
                }
            }
        });
    }

    private void performCompletion(String model, JSONArray messages, ApiResponseCallback<String> callback) throws JSONException {
        qwenApi.getChatCompletion(conversation.getQwenChatId(), conversation.getQwenParentId(), model, messages, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError("Failed to get response: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("AI error: " + response.code());
                    return;
                }

                StringBuilder fullContent = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String jsonData = line.substring(5).trim();
                            if (jsonData.isEmpty()) continue;

                            try {
                                JSONObject data = new JSONObject(jsonData);
                                if (data.has("response.created")) {
                                    JSONObject created = data.getJSONObject("response.created");
                                    conversation.setQwenParentId(created.getString("response_id"));
                                    continue;
                                }

                                if (data.has("choices")) {
                                    JSONArray choices = data.getJSONArray("choices");
                                    if (choices.length() > 0) {
                                        JSONObject choice = choices.getJSONObject(0);
                                        JSONObject delta = choice.getJSONObject("delta");
                                        String content = delta.optString("content", "");
                                        String phase = delta.optString("phase", "");

                                        if ("answer".equals(phase) && content != null && !content.isEmpty()) {
                                            fullContent.append(content);
                                            callback.onUpdate(content);
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e("QwenApiService", "Error parsing stream chunk: " + jsonData, e);
                            }
                        }
                    }
                }
                callback.onSuccess(fullContent.toString());
            }
        });
    }

    // Stubs for other methods
    @Override
    public void generateConversationTitle(String userMessage, String aiResponse, ApiResponseCallback<String> callback) { callback.onSuccess("New Qwen Chat"); }
    @Override
    public void considerGeneratingKnowledge(String conversationId, String userMessage, String aiResponse, KnowledgeCallback callback) { callback.onShouldNotGenerate(); }
    @Override
    public void summarizeConversation(String conversationId, String history, ApiResponseCallback<Knowledge> callback) { callback.onError("Not implemented"); }
    @Override
    public void generateInsights(String conversationHistory, ApiResponseCallback<String> callback) { callback.onError("Not implemented"); }
    @Override
    public void generatePsychologicalSurvey(String insightsJson, ApiResponseCallback<String> callback) { callback.onError("Not implemented"); }
    @Override
    public void analyzeSurveyAnswers(String insightsJson, String surveyAnswersJson, ApiResponseCallback<String> callback) { callback.onError("Not implemented"); }
}
