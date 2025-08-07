package com.relation.shit.api;

import org.json.JSONArray;
import com.relation.shit.model.Knowledge;

public interface BaseApiService {

    interface ApiResponseCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    interface KnowledgeCallback {
        void onShouldGenerate(Knowledge knowledge);
        void onShouldNotGenerate();
        void onError(String errorMessage);
    }

    void getChatCompletion(String model, JSONArray messages, ApiResponseCallback<String> callback);

    void generateConversationTitle(String userMessage, String aiResponse, ApiResponseCallback<String> callback);

    void considerGeneratingKnowledge(String conversationId, String userMessage, String aiResponse, KnowledgeCallback callback);

    void summarizeConversation(String conversationId, String history, ApiResponseCallback<Knowledge> callback);

    void generateInsights(String conversationHistory, ApiResponseCallback<String> callback);

    void generatePsychologicalSurvey(String insightsJson, ApiResponseCallback<String> callback);

    void analyzeSurveyAnswers(String insightsJson, String surveyAnswersJson, ApiResponseCallback<String> callback);
}
