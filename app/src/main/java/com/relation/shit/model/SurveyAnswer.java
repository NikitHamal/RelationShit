package com.relation.shit.model;

import java.io.Serializable;

public class SurveyAnswer implements Serializable {
    private String question;
    private String answer;

    public SurveyAnswer(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }
}
