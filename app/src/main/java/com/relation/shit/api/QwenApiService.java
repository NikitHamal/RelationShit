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

    public QwenApiService() {
        this.qwenApi = new QwenApi();
    }

    // This is no longer needed as we send the full context every time
    public void setConversation(Conversation conversation) {
        // this.conversation = conversation;
    }

    @Override
    public void getChatCompletion(String model, JSONArray messages, ApiResponseCallback<String> callback) {
        try {
            qwenApi.getChatCompletion(model, messages, new Callback() {
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
                                    if (data.has("choices")) {
                                        JSONArray choices = data.getJSONArray("choices");
                                        if (choices.length() > 0) {
                                            JSONObject choice = choices.getJSONObject(0);
                                            JSONObject delta = choice.getJSONObject("delta");
                                            String content = delta.optString("content", "");
                                            String phase = delta.optString("phase", "");

                                            if ("answer".equals(phase)) {
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
        } catch (JSONException e) {
            callback.onError("Failed to build request: " + e.getMessage());
        }
    }


    @Override
    public void generateConversationTitle(String userMessage, String aiResponse, ApiResponseCallback<String> callback) {
        String prompt =
            "Generate a short, concise, and descriptive title (max 5 words) for a conversation based on the following initial user message and AI response. The title should capture the main topic. Example: 'Relationship Advice', 'Career Guidance'.\n\nUser: \""
                + userMessage
                + "\"\nAI: \""
                + aiResponse
                + "\"";
        JSONArray messagesArray = new JSONArray();
        try {
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messagesArray.put(userMsg);
        } catch (JSONException e) {
            callback.onError("Failed to create title generation request.");
            return;
        }

        getChatCompletion(
            "qwen-max-latest", // Use a fast model for this
            messagesArray,
            new BaseApiService.ApiResponseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    String cleanedTitle = result.replaceAll("[^a-zA-Z\\s]", "").trim();
                    if (cleanedTitle.isEmpty()) {
                        cleanedTitle = "Untitled Conversation";
                    }
                    if (cleanedTitle.length() > 50) {
                        cleanedTitle = cleanedTitle.substring(0, 47) + "...";
                    }
                    callback.onSuccess(cleanedTitle);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
    }

    @Override
    public void considerGeneratingKnowledge(String conversationId, String userMessage, String aiResponse, KnowledgeCallback callback) {
        String knowledgePrompt =
            "Analyze the following conversation turn. Does it contain any personal information, important context, or key insights that would be valuable to remember for future interactions with this user (e.g., specific preferences, past experiences, relationship dynamics, emotional states, recurring issues)? If yes, provide a concise title (max 10 words) and a brief content summary (max 100 words) in JSON format. If no, respond with {\"shouldGenerate\": false}.\n\n"
                + "Example of a 'yes' response:\n"
                + "```json\n"
                + "{\n"
                + "  \"shouldGenerate\": true,\n"
                + "  \"title\": \"User's Anxiety Triggers\",\n"
                + "  \"content\": \"User experiences anxiety when discussing career changes, stemming from a past job loss. Avoid direct pressure on this topic.\"\n"
                + "}\n"
                + "```\n\n"
                + "Conversation Turn:\n"
                + "User: \""
                + userMessage
                + "\"\n"
                + "AI: \""
                + aiResponse
                + "\"\n\n"
                + "Your JSON response:";

        JSONArray messagesArray = new JSONArray();
        try {
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", knowledgePrompt);
            messagesArray.put(userMsg);
        } catch (JSONException e) {
            callback.onError("Failed to create knowledge consideration request.");
            return;
        }

        getChatCompletion(
            "qwen-max-latest", // Use a fast model for this
            messagesArray,
            new BaseApiService.ApiResponseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    try {
                        String cleanedResult = result.replace("```json", "").replace("```", "").trim();
                        JSONObject knowledgeJson = new JSONObject(cleanedResult);
                        boolean shouldGenerate = knowledgeJson.optBoolean("shouldGenerate", false);

                        if (shouldGenerate) {
                            String title =
                                knowledgeJson
                                    .optString("title", "Untitled Knowledge")
                                    .replace("\"", "")
                                    .trim();
                            String content =
                                knowledgeJson
                                    .optString("content", "No content provided.")
                                    .replace("\"", "")
                                    .trim();
                            Knowledge newKnowledge =
                                new Knowledge(
                                    java.util.UUID.randomUUID().toString(),
                                    conversationId,
                                    title,
                                    content,
                                    System.currentTimeMillis());
                            callback.onShouldGenerate(newKnowledge);
                        } else {
                            callback.onShouldNotGenerate();
                        }
                    } catch (JSONException e) {
                        callback.onError("Failed to parse knowledge response.");
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
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
