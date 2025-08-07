package com.relation.shit.model;

import java.io.Serializable;

public class Knowledge implements Serializable {
    private String id;
    private String conversationId; // Link to the conversation this knowledge belongs to
    private String title;
    private String content;
    private long timestamp;

    public Knowledge(String id, String conversationId, String title, String content, long timestamp) {
        this.id = id;
        this.conversationId = conversationId;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
