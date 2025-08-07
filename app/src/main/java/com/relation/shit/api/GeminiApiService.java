package com.relation.shit.api;

import android.util.Log;
import androidx.annotation.NonNull;
import com.relation.shit.model.Knowledge;
import java.io.IOException;
import java.util.UUID;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GeminiApiService implements BaseApiService {

  private final GeminiApi geminiApi;

  public GeminiApiService(String apiKey) {
    this.geminiApi = new GeminiApi(apiKey);
  }

  @Override
  public void getChatCompletion(
      String model,
      JSONArray messages,
      BaseApiService.ApiResponseCallback<String> callback) {
    try {
      geminiApi.getChatCompletion(
          model,
          messages,
          new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
              Log.e("GeminiApiService", "API call failed: " + e.getMessage());
              callback.onError("Failed to get response: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
              if (!response.isSuccessful() || response.body() == null) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                Log.e("GeminiApiService", "API error: " + response.code() + " - " + errorBody);
                callback.onError("AI error: " + response.code());
                return;
              }

              try {
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                  JSONObject firstCandidate = candidates.getJSONObject(0);
                  JSONObject content = firstCandidate.getJSONObject("content");
                  JSONArray parts = content.getJSONArray("parts");
                  if (parts.length() > 0) {
                    String aiContent = parts.getJSONObject(0).getString("text");
                    callback.onSuccess(aiContent);
                  } else {
                    callback.onError("No parts in Gemini response.");
                  }
                } else {
                  callback.onError("No candidates in Gemini response.");
                }
              } catch (JSONException e) {
                Log.e("GeminiApiService", "JSON parsing error: " + e.getMessage());
                callback.onError("Failed to parse AI response.");
              }
            }
          });
    } catch (Exception e) {
      Log.e("GeminiApiService", "Error setting up API call: " + e.getMessage());
      callback.onError("Failed to initiate API call.");
    }
  }

  public void generateConversationTitle(
      String userMessage, String aiResponse, BaseApiService.ApiResponseCallback<String> callback) {
    String prompt =
        "Generate a short, concise, and descriptive title (max 5 words) for a conversation based on the following initial user message and AI response. The title should capture the main topic. Example: 'Relationship Advice', 'Career Guidance'.\n\nUser: \""
            + userMessage
            + "\"\nAI: \""
            + aiResponse
            + "\"";
    JSONArray contentsArray = new JSONArray();
    try {
      JSONObject userPart = new JSONObject();
      userPart.put("text", prompt);
      JSONObject userContent = new JSONObject();
      userContent.put("role", "user");
      userContent.put("parts", new JSONArray().put(userPart));
      contentsArray.put(userContent);
    } catch (JSONException e) {
      Log.e("GeminiApiService", "Title generation JSON error: " + e.getMessage());
      callback.onError("Failed to create title generation request.");
      return;
    }

    try {
      geminiApi.getChatCompletion(
          "gemini-pro", // Using gemini-pro for title generation
          contentsArray,
          new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
              Log.e("GeminiApiService", "API call failed: " + e.getMessage());
              callback.onError("Failed to get response: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
              if (!response.isSuccessful() || response.body() == null) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                Log.e("GeminiApiService", "API error: " + response.code() + " - " + errorBody);
                callback.onError("AI error: " + response.code());
                return;
              }

              try {
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                  JSONObject firstCandidate = candidates.getJSONObject(0);
                  JSONObject jsonContent = firstCandidate.getJSONObject("content");
                  JSONArray parts = jsonContent.getJSONArray("parts");
                  if (parts.length() > 0) {
                    String aiContent = parts.getJSONObject(0).getString("text");
                    String cleanedTitle = aiContent.replaceAll("[^a-zA-Z\\s]", "").trim();
                    if (cleanedTitle.isEmpty()) {
                        cleanedTitle = "Untitled Conversation";
                    }
                    if (cleanedTitle.length() > 50) {
                      cleanedTitle = cleanedTitle.substring(0, 47) + "...";
                    }
                    callback.onSuccess(cleanedTitle);
                  } else {
                    callback.onError("No parts in Gemini response.");
                  }
                } else {
                  callback.onError("No candidates in Gemini response.");
                }
              } catch (JSONException e) {
                Log.e("GeminiApiService", "JSON parsing error: " + e.getMessage());
                callback.onError("Failed to parse AI response.");
              }
            }
          });
    } catch (Exception e) {
      Log.e("GeminiApiService", "Error setting up API call: " + e.getMessage());
      callback.onError("Failed to initiate API call.");
    }
  }

  public void considerGeneratingKnowledge(
      String conversationId, String userMessage, String aiResponse, BaseApiService.KnowledgeCallback callback) {
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
            + "Example of a 'no' response:\n"
            + "```json\n"
            + "{\n"
            + "  \"shouldGenerate\": false\n"
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

    JSONArray contentsArray = new JSONArray();
    try {
      JSONObject userPart = new JSONObject();
      userPart.put("text", knowledgePrompt);
      JSONObject userContent = new JSONObject();
      userContent.put("role", "user");
      userContent.put("parts", new JSONArray().put(userPart));
      contentsArray.put(userContent);
    } catch (JSONException e) {
      Log.e("GeminiApiService", "Knowledge consideration JSON error: " + e.getMessage());
      callback.onError("Failed to create knowledge consideration request.");
      return;
    }

    try {
      geminiApi.getChatCompletion(
          "gemini-pro", // Using gemini-pro for knowledge generation
          contentsArray,
          new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
              Log.e("GeminiApiService", "API call failed: " + e.getMessage());
              callback.onError("Failed to get response: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
              if (!response.isSuccessful() || response.body() == null) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                Log.e("GeminiApiService", "API error: " + response.code() + " - " + errorBody);
                callback.onError("AI error: " + response.code());
                return;
              }

              try {
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                  JSONObject firstCandidate = candidates.getJSONObject(0);
                  JSONObject jsonContent = firstCandidate.getJSONObject("content");
                  JSONArray parts = jsonContent.getJSONArray("parts");
                  if (parts.length() > 0) {
                    String aiContent = parts.getJSONObject(0).getString("text");
                    String cleanedResult = aiContent.replace("```json", "").replace("```", "").trim();
                    JSONObject knowledgeJson = new JSONObject(cleanedResult);
                    boolean shouldGenerate = knowledgeJson.optBoolean("shouldGenerate", false);

                    if (shouldGenerate) {
                      String title =
                          knowledgeJson
                              .optString("title", "Untitled Knowledge")
                              .replace("\"", "")
                              .trim();
                      String knowledgeContent = // Corrected variable name
                          knowledgeJson
                              .optString("content", "No content provided.")
                              .replace("\"", "")
                              .trim();
                      Knowledge newKnowledge =
                          new Knowledge(
                              UUID.randomUUID().toString(),
                              conversationId,
                              title,
                              knowledgeContent, // Corrected variable name
                              System.currentTimeMillis());
                      callback.onShouldGenerate(newKnowledge);
                    } else {
                      callback.onShouldNotGenerate();
                    }
                  } else {
                    callback.onError("No parts in Gemini response.");
                  }
                } else {
                  callback.onError("No candidates in Gemini response.");
                }
              } catch (JSONException e) {
                Log.e("GeminiApiService", "JSON parsing error: " + e.getMessage());
                callback.onError("Failed to parse AI response.");
              }
            }
          });
    } catch (Exception e) {
      Log.e("GeminiApiService", "Error setting up API call: " + e.getMessage());
      callback.onError("Failed to initiate API call.");
    }
  }

  public void summarizeConversation(
      String conversationId, String history, BaseApiService.ApiResponseCallback<Knowledge> callback) {
    String summaryPrompt =
        "Summarize the following conversation history into a short (max 100 words), precise, and essence-capturing knowledge base entry. Focus on key personal information, recurring themes, or important context that would be vital for a psychologist, psychiatric, philosopher, consultant, or relationship expert to remember about this user. Respond in JSON format with a 'title' (max 10 words) and 'content' for the knowledge entry. Example: {\"title\": \"User's Core Conflict\", \"content\": \"User frequently struggles with self-worth due to childhood experiences, leading to trust issues in relationships.\"}\n\nConversation History:\n"
            + history
            + "\n\nYour JSON response:";

    JSONArray contentsArray = new JSONArray();
    try {
      JSONObject userPart = new JSONObject();
      userPart.put("text", summaryPrompt);
      JSONObject userContent = new JSONObject();
      userContent.put("role", "user");
      userContent.put("parts", new JSONArray().put(userPart));
      contentsArray.put(userContent);
    } catch (JSONException e) {
      Log.e("GeminiApiService", "Summarization JSON error: " + e.getMessage());
      callback.onError("Failed to create summarization request.");
      return;
    }

    try {
      geminiApi.getChatCompletion(
          "gemini-pro", // Using gemini-pro for summarization
          contentsArray,
          new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
              Log.e("GeminiApiService", "API call failed: " + e.getMessage());
              callback.onError("Failed to get response: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
              if (!response.isSuccessful() || response.body() == null) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                Log.e("GeminiApiService", "API error: " + response.code() + " - " + errorBody);
                callback.onError("AI error: " + response.code());
                return;
              }

              try {
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                  JSONObject firstCandidate = candidates.getJSONObject(0);
                  JSONObject jsonContent = firstCandidate.getJSONObject("content");
                  JSONArray parts = jsonContent.getJSONArray("parts");
                  if (parts.length() > 0) {
                    String aiContent = parts.getJSONObject(0).getString("text");
                    String cleanedResult = aiContent.replace("```json", "").replace("```", "").trim();
                    JSONObject summaryJson = new JSONObject(cleanedResult);
                    String title =
                        summaryJson.optString("title", "Conversation Summary").replace("\"", "").trim();
                    String summaryContent = // Removed duplicate declaration
                        summaryJson.optString("content", "Summary not available.").replace("\"", "").trim(); // Corrected replace method
                    Knowledge newSummaryKnowledge =
                        new Knowledge(
                            UUID.randomUUID().toString(),
                            conversationId,
                            title,
                            summaryContent, // Used summaryContent here
                            System.currentTimeMillis());
                    callback.onSuccess(newSummaryKnowledge);
                  } else {
                    callback.onError("No parts in Gemini response.");
                  }
                } else {
                  callback.onError("No candidates in Gemini response.");
                }
              } catch (JSONException e) {
                Log.e(
                    "GeminiApiService",
                    "Failed to parse summary JSON: " + response.body().string() + " Error: " + e.getMessage());
                callback.onError("Failed to parse summary response.");
              }
            }
          });
    } catch (Exception e) {
      Log.e("GeminiApiService", "Error setting up API call: " + e.getMessage());
      callback.onError("Failed to initiate API call.");
    }
  }

  public void generateInsights(
      String conversationHistory,
      BaseApiService.ApiResponseCallback<String> callback) {
    String insightsPrompt =
        "Based on the following conversation, generate a detailed psychological and relational analysis. The user is likely seeking advice on topics like relationships, psychology, and personal growth. If the conversation is too short, irrelevant, or lacks context, respond with {\"sufficientContext\": false}. Otherwise, provide a JSON object with the following structure:\n\n"
            + "```json\n"
            + "{\n"
            + "  \"sufficientContext\": true,\n"
            + "  \"insights\": [\n"
            + "    {\n"
            + "      \"type\": \"chart\",\n"
            + "      \"title\": \"Sentiment Trend\",\n"
            + "      \"chartType\": \"line\",\n"
            + "      \"data\": {\"labels\": [\"Start\", \"Mid\", \"End\"], \"values\": [0.2, -0.5, 0.8]}\n"
            + "    },\n"
            + "    {\n"
            + "      \"type\": \"progress\",\n"
            + "      \"title\": \"Goal Progress\",\n"
            + "      \"value\": 65,\n"
            + "      \"description\": \"User is making progress towards their stated goal.\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"type\": \"text\",\n"
            + "      \"title\": \"Key Themes\",\n"
            + "      \"content\": \"- Communication issues\\n- Fear of vulnerability\\n- Desire for connection\"\n"
            + "    }\n"
            + "  ]\n"
            + "}\n"
            + "```\n\n"
            + "Conversation History:\n"
            + conversationHistory
            + "\n\nYour JSON response:";

    JSONArray contentsArray = new JSONArray();
    try {
      JSONObject userPart = new JSONObject();
      userPart.put("text", insightsPrompt);
      JSONObject userContent = new JSONObject();
      userContent.put("role", "user");
      userContent.put("parts", new JSONArray().put(userPart));
      contentsArray.put(userContent);
    } catch (JSONException e) {
      Log.e("GeminiApiService", "Insights generation JSON error: " + e.getMessage());
      callback.onError("Failed to create insights generation request.");
      return;
    }

    getChatCompletion(
        "gemini-pro", // Using gemini-pro for insights generation
        contentsArray,
        new BaseApiService.ApiResponseCallback<String>() {
          @Override
          public void onSuccess(String result) {
            try {
              String cleanedResult = result.replace("```json", "").replace("```", "").trim();
              JSONObject insightsJson = new JSONObject(cleanedResult);
              boolean sufficientContext = insightsJson.optBoolean("sufficientContext", false);

              if (sufficientContext) {
                callback.onSuccess(insightsJson.toString());
              } else {
                callback.onError("Insufficient or irrelevant context to generate insights.");
              }
            } catch (JSONException e) {
              Log.e(
                  "GeminiApiService",
                  "Failed to parse insights JSON: " + result + " Error: " + e.getMessage());
              callback.onError("Failed to parse insights response.");
            }
          }

          @Override
          public void onError(String errorMessage) {
            Log.e("GeminiApiService", "Insights generation failed: " + errorMessage);
            callback.onError(errorMessage);
          }
        });
  }

  public void generatePsychologicalSurvey(
      String insightsJson,
      BaseApiService.ApiResponseCallback<String> callback) {
    String surveyPrompt =
        "Based on the following conversation insights, generate a personalized psychological survey with at least 10 questions. The user is likely seeking advice on topics like relationships, psychology, and personal growth. For each question, specify the type ('multiple_choice', 'single_choice', or 'text'), the question text, and a list of options if applicable. Respond with a JSON object with a 'questions' array.\n\n"
            + "Example Response:\n"
            + "```json\n"
            + "{\n"
            + "  \"questions\": [\n"
            + "    {\n"
            + "      \"type\": \"single_choice\",\n"
            + "      \"question\": \"How do you typically react to criticism?\",\n"
            + "      \"options\": [\"Defensively\", \"Openly\", \"Withdrawn\"]\n"
            + "    },\n"
            + "    {\n"
            + "      \"type\": \"text\",\n"
            + "      \"question\": \"Describe a recent situation where you felt misunderstood.\"\n"
            + "    }\n"
            + "  ]\n"
            + "}\n"
            + "```\n\n"
            + "Conversation Insights:\n"
            + insightsJson
            + "\n\nYour JSON response:";

    JSONArray contentsArray = new JSONArray();
    try {
      JSONObject userPart = new JSONObject();
      userPart.put("text", surveyPrompt);
      JSONObject userContent = new JSONObject();
      userContent.put("role", "user");
      userContent.put("parts", new JSONArray().put(userPart));
      contentsArray.put(userContent);
    } catch (JSONException e) {
      Log.e("GeminiApiService", "Survey generation JSON error: " + e.getMessage());
      callback.onError("Failed to create survey generation request.");
      return;
    }

    getChatCompletion(
        "gemini-pro", // Using gemini-pro for survey generation
        contentsArray,
        new BaseApiService.ApiResponseCallback<String>() {
          @Override
          public void onSuccess(String result) {
            try {
              String cleanedResult = result.replace("```json", "").replace("```", "").trim();
              JSONObject surveyJson = new JSONObject(cleanedResult);
              callback.onSuccess(surveyJson.toString());
            } catch (JSONException e) {
              Log.e(
                  "GeminiApiService",
                  "Failed to parse survey JSON: " + result + " Error: " + e.getMessage());
              callback.onError("Failed to parse survey response.");
            }
          }

          @Override
          public void onError(String errorMessage) {
            Log.e("GeminiApiService", "Survey generation failed: " + errorMessage);
            callback.onError(errorMessage);
          }
        });
  }

  public void analyzeSurveyAnswers(
      String insightsJson,
      String surveyAnswersJson,
      BaseApiService.ApiResponseCallback<String> callback) {
    String analysisPrompt =
        "Based on the following conversation insights and the user's answers to a psychological survey, generate an updated insights JSON and a detailed analysis report. The user is likely seeking advice on topics like relationships, psychology, and personal growth. Respond with a JSON object with 'updatedInsights' and 'analysisReport' fields.\n\n"
            + "Conversation Insights:\n"
            + insightsJson
            + "\n\nSurvey Answers:\n"
            + surveyAnswersJson
            + "\n\nYour JSON response:";

    JSONArray contentsArray = new JSONArray();
    try {
      JSONObject userPart = new JSONObject();
      userPart.put("text", analysisPrompt);
      JSONObject userContent = new JSONObject();
      userContent.put("role", "user");
      userContent.put("parts", new JSONArray().put(userPart));
      contentsArray.put(userContent);
    } catch (JSONException e) {
      Log.e("GeminiApiService", "Analysis generation JSON error: " + e.getMessage());
      callback.onError("Failed to create analysis generation request.");
      return;
    }

    getChatCompletion(
        "gemini-pro", // Using gemini-pro for analysis generation
        contentsArray,
        new BaseApiService.ApiResponseCallback<String>() {
          @Override
          public void onSuccess(String result) {
            try {
              String cleanedResult = result.replace("```json", "").replace("```", "").trim();
              JSONObject analysisJson = new JSONObject(cleanedResult);
              callback.onSuccess(analysisJson.toString());
            }
            catch (JSONException e) {
              Log.e(
                  "GeminiApiService",
                  "Failed to parse analysis JSON: " + result + " Error: " + e.getMessage());
              callback.onError("Failed to parse analysis response.");
            }
          }

          @Override
          public void onError(String errorMessage) {
            Log.e("GeminiApiService", "Analysis generation failed: " + errorMessage);
            callback.onError(errorMessage);
          }
        });
  }
}
