package com.relation.shit.model;

import java.io.Serializable;

public class Message implements Serializable {
    private String role;
    private String content;
    private String type; // "user", "ai", "thinking", "error"
    private String status; // "success", "error", "thinking"
    private transient Runnable retryAction; // Not serialized

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
        this.type = role; // Default type is the role
        this.status = "success";
    }

    public Message(String type, String role, String content) {
        this.type = type;
        this.role = role;
        this.content = content;
        this.status = type; // For thinking/error messages, status is initially their type
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Runnable getRetryAction() {
        return retryAction;
    }

    public void setRetryAction(Runnable retryAction) {
        this.retryAction = retryAction;
    }
}
