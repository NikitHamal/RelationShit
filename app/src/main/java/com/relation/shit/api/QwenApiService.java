package com.relation.shit.api;

import android.util.Log;
import androidx.annotation.NonNull;
import com.relation.shit.model.Conversation;
import com.relation.shit.model.Knowledge;
import com.relation.shit.model.Message;

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
    public void getChatCompletion(String model, JSONArray messages, ApiResponseCallback<String> finalCallback) {
        // The callback needs to handle streaming updates, so we'll wrap the final callback.
        // We need to know if the model supports thinking. We'll assume it does for now.
        boolean supportsThinking = true;

        try {
            if (conversation.getQwenChatId() == null) {
                // First message in a new conversation, so create a new chat first
                qwenApi.newChat(model, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        finalCallback.onError("Failed to create new chat: " + e.getMessage());
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
                                    // Now that we have a chat ID, we can send the message
                                    performCompletion(model, messages, supportsThinking, false, finalCallback);
                                } else {
                                    finalCallback.onError("Failed to create new Qwen chat.");
                                }
                            } catch (JSONException e) {
                                finalCallback.onError("Error parsing new chat response.");
                            }
                        } else {
                            finalCallback.onError("Failed to create new Qwen chat: " + response.message());
                        }
                    }
                });
            } else {
                // Existing conversation, just send the message
                performCompletion(model, messages, supportsThinking, false, finalCallback);
            }
        } catch (JSONException e) {
            finalCallback.onError("Failed to build request: " + e.getMessage());
        }
    }

    private void performCompletion(String model, JSONArray messages, boolean thinking, boolean webSearch, ApiResponseCallback<String> callback) throws JSONException {
        qwenApi.getChatCompletion(conversation.getQwenChatId(), conversation.getQwenParentId(), model, messages, thinking, webSearch, new Callback() {
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

                                        if ("answer".equals(phase)) {
                                            fullContent.append(content);
                                        }
                                        // Here you could add logic to update the UI with the thinking phase content
                                        // For now, we just accumulate the answer.
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


    // Other methods from BaseApiService are not implemented yet
    @Override
    public void generateConversationTitle(String userMessage, String aiResponse, ApiResponseCallback<String> callback) {
        callback.onSuccess("New Qwen Chat");
    }

    @Override
    public void considerGeneratingKnowledge(String conversationId, String userMessage, String aiResponse, KnowledgeCallback callback) {
        callback.onShouldNotGenerate();
    }

    @Override
    public void summarizeConversation(String conversationId, String history, ApiResponseCallback<Knowledge> callback) {
        callback.onError("Not implemented");
    }

    @Override
    public void generateInsights(String conversationHistory, ApiResponseCallback<String> callback) {
        callback.onError("Not implemented");
    }

    @Override
    public void generatePsychologicalSurvey(String insightsJson, ApiResponseCallback<String> callback) {
        callback.onError("Not implemented");
    }

    @Override
    public void analyzeSurveyAnswers(String insightsJson, String surveyAnswersJson, ApiResponseCallback<String> callback) {
        callback.onError("Not implemented");
    }
}
