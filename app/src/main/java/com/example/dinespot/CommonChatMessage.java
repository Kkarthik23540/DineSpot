package com.example.dinespot;

public class CommonChatMessage {
    private String message;
    private String senderId;
    private long timestamp;
    private String messageId;
    private String type; // "text", "image"
    private String imageUrl;
    private String replyToId;
    private String replyToText;
    private String replyToSenderName;
    private String status; // "sent", "delivered", "read"

    public CommonChatMessage() {
        // Required for Firestore
    }

    public CommonChatMessage(String message, String senderId, long timestamp) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.type = "text";
        this.status = "sent";
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getReplyToId() { return replyToId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }

    public String getReplyToText() { return replyToText; }
    public void setReplyToText(String replyToText) { this.replyToText = replyToText; }

    public String getReplyToSenderName() { return replyToSenderName; }
    public void setReplyToSenderName(String replyToSenderName) { this.replyToSenderName = replyToSenderName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
