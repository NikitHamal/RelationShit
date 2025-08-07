package com.relation.shit.model;

import java.io.Serializable;

public class Insight implements Serializable {
    private String id;
    private String conversationId;
    private String content;
    private long timestamp;

    public Insight(String id, String conversationId, String content, long timestamp) {
        this.id = id;
        this.conversationId = conversationId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
