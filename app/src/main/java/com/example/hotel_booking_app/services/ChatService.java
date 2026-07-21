package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Conversation;
import com.example.hotel_booking_app.data.models.Message;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatService {
    private final SupabaseClient supabaseClient;

    public ChatService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getGuestConversations(String guestId, SupabaseCallback<List<Conversation>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("guestId", guestId);
        supabaseClient.getList(AppConstants.TABLE_CONVERSATIONS, "*", null, "updatedAt.desc", filters, Conversation[].class, callback);
    }

    public void getHostConversations(String hostId, SupabaseCallback<List<Conversation>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("hostId", hostId);
        supabaseClient.getList(AppConstants.TABLE_CONVERSATIONS, "*", null, "updatedAt.desc", filters, Conversation[].class, callback);
    }

    public void getAllConversations(SupabaseCallback<List<Conversation>> callback) {
        supabaseClient.getList(AppConstants.TABLE_CONVERSATIONS, "*", null, "updatedAt.desc", null, Conversation[].class, callback);
    }

    public void getConversationById(String conversationId, SupabaseCallback<Conversation> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", conversationId);
        supabaseClient.getSingle(AppConstants.TABLE_CONVERSATIONS, filters, Conversation[].class, callback);
    }

    public void createInquiryConversation(String guestId, String hostId, String cabinId, SupabaseCallback<Conversation> callback) {
        createConversation(guestId, hostId, cabinId, null, callback);
    }

    public void createBookingConversation(String guestId, String hostId, String cabinId, String bookingId, SupabaseCallback<Conversation> callback) {
        createConversation(guestId, hostId, cabinId, bookingId, callback);
    }

    public void createConversation(String guestId, String hostId, String cabinId, String bookingId, SupabaseCallback<Conversation> callback) {
        Conversation conversation = new Conversation();
        conversation.setGuestId(guestId);
        conversation.setHostId(hostId);
        conversation.setCabinId(cabinId);
        conversation.setBookingId(bookingId);
        supabaseClient.insert(AppConstants.TABLE_CONVERSATIONS, conversation, Conversation[].class, callback);
    }

    public void getMessages(String conversationId, SupabaseCallback<List<Message>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("conversationId", conversationId);
        supabaseClient.getList(AppConstants.TABLE_MESSAGES, "*", null, "createdAt.asc", filters, Message[].class, callback);
    }

    public void sendMessage(String conversationId, String senderId, String messageText, SupabaseCallback<Message> callback) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setMessage(messageText);
        supabaseClient.insert(AppConstants.TABLE_MESSAGES, message, Message[].class, callback);
    }
}
