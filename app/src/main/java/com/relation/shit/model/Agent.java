package com.relation.shit.model;

import java.io.Serializable;

public class Agent implements Serializable {
    private String id;
    private String name;
    private String prompt;
    private String emoji;
    private String model;
    private String apiProvider;

    public Agent(String id, String name, String prompt, String emoji, String model, String apiProvider) {
        this.id = id;
        this.name = name;
        this.prompt = prompt;
        this.emoji = emoji;
        this.model = model;
        this.apiProvider = apiProvider;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getModel() {
        return model;
    }

    public String getApiProvider() {
        return apiProvider;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setApiProvider(String apiProvider) {
        this.apiProvider = apiProvider;
    }
}