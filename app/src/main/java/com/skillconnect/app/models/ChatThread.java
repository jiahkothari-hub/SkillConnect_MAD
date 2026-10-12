package com.skillconnect.app.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * A conversation between one customer and one provider.
 * Stored at chats/{customerId_providerId}; messages live in the sub-collection
 * chats/{chatId}/messages.
 */
public class ChatThread {

    private String chatId;
    private String customerId;
    private String customerName;
    private String providerId;
    private String providerName;
    private String providerTitle;
    private List<String> participants = new ArrayList<>();
    private String lastMessage;
    private String lastSenderId;
    private long updatedAt;
    private int unreadCustomer;
    private int unreadProvider;

    public ChatThread() {
        // Required empty constructor for Firestore
    }

    public static String idFor(String customerId, String providerId) {
        return customerId + "_" + providerId;
    }

    @Exclude
    public boolean isCustomer(String userId) {
        return userId != null && userId.equals(customerId);
    }

    /** Name of the other person in the chat. */
    @Exclude
    public String otherName(String myId) {
        return isCustomer(myId) ? providerName : customerName;
    }

    @Exclude
    public String otherId(String myId) {
        return isCustomer(myId) ? providerId : customerId;
    }

    @Exclude
    public int unreadFor(String myId) {
        return isCustomer(myId) ? unreadCustomer : unreadProvider;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getProviderTitle() { return providerTitle; }
    public void setProviderTitle(String providerTitle) { this.providerTitle = providerTitle; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants != null ? participants : new ArrayList<>(); }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public int getUnreadCustomer() { return unreadCustomer; }
    public void setUnreadCustomer(int unreadCustomer) { this.unreadCustomer = unreadCustomer; }
    public int getUnreadProvider() { return unreadProvider; }
    public void setUnreadProvider(int unreadProvider) { this.unreadProvider = unreadProvider; }
}
