package com.skillconnect.app.models;

import com.google.firebase.firestore.Exclude;

/** One chat message. Stored at chats/{chatId}/messages/{messageId}. */
public class Message {

    private String messageId;
    private String senderId;
    private String senderName;
    private String text;
    private String imageUrl;
    private long createdAt;

    public Message() {
        // Required empty constructor for Firestore
    }

    @Exclude
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    /** Short preview used in the chat list ("📷 Photo" for images). */
    @Exclude
    public String preview() {
        if (text != null && !text.trim().isEmpty()) return text.trim();
        return hasImage() ? "📷 Photo" : "";
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
