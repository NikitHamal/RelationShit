package com.relation.shit.model;

import java.io.Serializable;
import java.util.List;

public class Survey implements Serializable {
    private String id;
    private String conversationId;
    private List<SurveyAnswer> answers;
    private String analysisReport;
    private long timestamp;

    public Survey(String id, String conversationId, List<SurveyAnswer> answers, String analysisReport, long timestamp) {
        this.id = id;
        this.conversationId = conversationId;
        this.answers = answers;
        this.analysisReport = analysisReport;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public List<SurveyAnswer> getAnswers() {
        return answers;
    }

    public String getAnalysisReport() {
        return analysisReport;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
