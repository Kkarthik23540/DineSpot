package com.example.dinespot;
public class CommonFeedback {
    private String id;
    private String feedback;
    private String name;
    private String email;
    private String userId;
    private String restaurantId;
    private String timestamp;
    private String adminReply;
    private int rating;
    private String ratingLabel;

    public CommonFeedback() {} // Required for Firestore

    public CommonFeedback(String id, String feedback, String name, String email, String userId, String restaurantId, String timestamp, String adminReply, int rating, String ratingLabel) {
        this.id = id;
        this.feedback = feedback;
        this.name = name;
        this.email = email;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.timestamp = timestamp;
        this.adminReply = adminReply;
        this.rating = rating;
        this.ratingLabel = ratingLabel;
    }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getRatingLabel() { return ratingLabel; }
    public void setRatingLabel(String ratingLabel) { this.ratingLabel = ratingLabel; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }

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
