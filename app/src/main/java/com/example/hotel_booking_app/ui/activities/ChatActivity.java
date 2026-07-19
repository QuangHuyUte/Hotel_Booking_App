package com.example.hotel_booking_app.ui.activities;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Conversation;
import com.example.hotel_booking_app.data.models.Message;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.services.ChatService;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.List;

public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_HOST_ID = "extra_host_id";
    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";

    private TextView statusTextView;
    private LinearLayout messagesContainer;
    private EditText messageEditText;
    private ChatService chatService;
    private SessionManager sessionManager;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatService = new ChatService();
        sessionManager = new SessionManager(this);
        statusTextView = findViewById(R.id.text_status);
        messagesContainer = findViewById(R.id.container_messages);
        messageEditText = findViewById(R.id.edit_message);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        Button sendButton = findViewById(R.id.button_send);
        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        sendButton.setOnClickListener(view -> sendMessage());

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        if (conversationId == null) {
            createConversation();
        } else {
            loadMessages();
        }
    }

    private void createConversation() {
        String hostId = getIntent().getStringExtra(EXTRA_HOST_ID);
        String cabinId = getIntent().getStringExtra(com.example.hotel_booking_app.utils.AppConstants.EXTRA_CABIN_ID);
        statusTextView.setText("Opening chat...");
        if (hostId == null || hostId.trim().isEmpty()) {
            new AuthService().getSupportUser(new SupabaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    createConversationWithHost(user.getId(), cabinId);
                }

                @Override
                public void onError(String message) {
                    showChatError("No admin/support account is available for chat yet.");
                }
            });
            return;
        }
        createConversationWithHost(hostId, cabinId);
    }

    private void createConversationWithHost(String hostId, String cabinId) {
        chatService.createConversation(sessionManager.getUserId(), hostId, cabinId, null, new SupabaseCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation conversation) {
                conversationId = conversation.getId();
                loadMessages();
            }

            @Override
            public void onError(String message) {
                showChatError(message);
            }
        });
    }

    private void loadMessages() {
        statusTextView.setText("Loading messages...");
        chatService.getMessages(conversationId, new SupabaseCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                renderMessages(messages);
                statusTextView.setText("Conversation ready.");
            }

            @Override
            public void onError(String message) {
                showChatError(message);
            }
        });
    }

    private void renderMessages(List<Message> messages) {
        messagesContainer.removeAllViews();
        for (Message message : messages) {
            TextView bubble = new TextView(this);
            bubble.setText(message.getMessage());
            bubble.setTextColor(getColor(R.color.ink));
            bubble.setTextSize(15);
            bubble.setPadding(16, 12, 16, 12);
            bubble.setBackgroundResource(R.drawable.bg_panel);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 10);
            if (sessionManager.getUserId().equals(message.getSenderId())) {
                params.gravity = Gravity.END;
            }
            bubble.setLayoutParams(params);
            messagesContainer.addView(bubble);
        }
    }

    private void sendMessage() {
        String message = messageEditText.getText().toString().trim();
        if (message.isEmpty() || conversationId == null) {
            return;
        }
        chatService.sendMessage(conversationId, sessionManager.getUserId(), message, new SupabaseCallback<Message>() {
            @Override
            public void onSuccess(Message data) {
                messageEditText.setText("");
                loadMessages();
            }

            @Override
            public void onError(String message) {
                showChatError(message);
            }
        });
    }

    private void showChatError(String message) {
        String friendly = message != null && message.contains("row-level security")
                ? "Chat is blocked by Supabase security policy. Please add RLS policies for conversations/messages."
                : message;
        statusTextView.setText(friendly);
        Toast.makeText(this, "Chat is not available yet", Toast.LENGTH_SHORT).show();
    }
}
