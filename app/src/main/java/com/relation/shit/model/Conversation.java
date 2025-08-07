package com.relation.shit.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Conversation implements Serializable {
    private String id;
    private String title;
    private Agent agent;
    private long timestamp;
    private List<Message> messages;

    public Conversation(String id, String title, Agent agent, long timestamp) {
        this.id = id;
        this.title = title;
        this.agent = agent;
        this.timestamp = timestamp;
        this.messages = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Agent getAgent() {
        return agent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
