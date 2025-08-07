package com.relation.shit.model;

import java.io.Serializable;

public class JournalEntry implements Serializable {
    private String id;
    private String title;
    private String content;
    private long timestamp;

    public JournalEntry(String id, String title, String content, long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
