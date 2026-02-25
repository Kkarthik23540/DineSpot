package com.example.dinespot;
public class Feedback {
    private String feedback;
    private String name;
    private String email;
    private String timestamp;

    public Feedback(String feedback, String name, String email, String timestamp) {
        this.feedback = feedback;
        this.name = name;
        this.email = email;
        this.timestamp = timestamp;
    }

    public String getFeedback() {
        return feedback;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
