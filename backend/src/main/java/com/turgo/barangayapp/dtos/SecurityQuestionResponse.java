package com.turgo.barangayapp.dtos;

public class SecurityQuestionResponse {
    private String securityQuestion;

    public SecurityQuestionResponse() {
    }

    public SecurityQuestionResponse(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }
}
