package com.example.hotel_booking_app.ui.activities;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

public class ChatThreadActivity extends AppCompatActivity {
    public static final String EXTRA_HOST_ID = "extra_host_id";
    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";

    private TextView statusTextView;
    private LinearLayout messagesContainer;
    private ScrollView messagesScroll;
    private EditText messageEditText;
    private ChatService chatService;
    private SessionManager sessionManager;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_thread);

        chatService = new ChatService();
        sessionManager = new SessionManager(this);
        statusTextView = findViewById(R.id.text_status);
        messagesContainer = findViewById(R.id.container_messages);
        messagesScroll = findViewById(R.id.scroll_messages);
        messageEditText = findViewById(R.id.edit_message);
        Button backButton = findViewById(R.id.button_back);
        Button sendButton = findViewById(R.id.button_send);
        backButton.setOnClickListener(view -> finish());
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
                    showChatError("Chưa có tài khoản quản lý/hỗ trợ để bắt đầu trò chuyện.");
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
        statusTextView.setText("Đang tải tin nhắn...");
        chatService.getMessages(conversationId, new SupabaseCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                renderMessages(messages);
                statusTextView.setText("Đang hoạt động");
            }

            @Override
            public void onError(String message) {
                showChatError(message);
            }
        });
    }

    private void renderMessages(List<Message> messages) {
        messagesContainer.removeAllViews();
        if (messages == null || messages.isEmpty()) {
            addSystemHint("Start the conversation with your host.");
            return;
        }
        for (Message message : messages) {
            boolean isMine = sessionManager.getUserId().equals(message.getSenderId());
            String receipt = isMine ? (message.isRead() ? "Seen" : "Sent") : "";
            addMessageRow(message.getMessage(), isMine, formatTime(message.getCreatedAt()), receipt);
        }
        scrollToBottom();
    }

    private void sendMessage() {
        String message = messageEditText.getText().toString().trim();
        if (message.isEmpty() || conversationId == null) {
            return;
        }
        messageEditText.setText("");
        addMessageRow(message, true, "Bây giờ", "Đang gửi...");
        scrollToBottom();
        chatService.sendMessage(conversationId, sessionManager.getUserId(), message, new SupabaseCallback<Message>() {
            @Override
            public void onSuccess(Message data) {
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
                ? "Tin nhắn đang bị chặn bởi chính sách bảo mật Supabase. Vui lòng thêm RLS cho conversations/messages."
                : message;
        statusTextView.setText(friendly);
        Toast.makeText(this, "Tin nhắn hiện chưa sẵn sàng", Toast.LENGTH_SHORT).show();
    }

    private void addMessageRow(String body, boolean isMine, String time, String receipt) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(isMine ? Gravity.END : Gravity.START);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(8));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(rowParams);

        TextView avatar = makeAvatar(isMine);
        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setGravity(isMine ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams stackParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        stackParams.setMargins(dp(8), 0, dp(8), 0);
        stack.setLayoutParams(stackParams);

        TextView bubble = new TextView(this);
        bubble.setText(body);
        bubble.setTextColor(getColor(isMine ? R.color.white : R.color.booking_text));
        bubble.setTextSize(15);
        bubble.setLineSpacing(0, 1.08f);
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.68f));
        bubble.setBackgroundResource(isMine ? R.drawable.bg_chat_bubble_mine : R.drawable.bg_chat_bubble_theirs);
        bubble.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView meta = new TextView(this);
        String metaText = receipt == null || receipt.isEmpty() ? time : time + " - " + receipt;
        meta.setText(metaText);
        meta.setTextColor(getColor(R.color.booking_muted));
        meta.setTextSize(11);
        meta.setGravity(isMine ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        metaParams.setMargins(0, dp(4), 0, 0);
        meta.setLayoutParams(metaParams);

        stack.addView(bubble);
        stack.addView(meta);

        if (isMine) {
            row.addView(stack);
            row.addView(avatar);
        } else {
            row.addView(avatar);
            row.addView(stack);
        }
        messagesContainer.addView(row);
    }

    private TextView makeAvatar(boolean isMine) {
        TextView avatar = new TextView(this);
        avatar.setWidth(dp(34));
        avatar.setHeight(dp(34));
        avatar.setGravity(Gravity.CENTER);
        avatar.setText(isMine ? "Me" : "H");
        avatar.setTextColor(getColor(isMine ? R.color.black : R.color.white));
        avatar.setTextSize(isMine ? 11 : 14);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setBackgroundResource(isMine ? R.drawable.bg_chat_avatar_mine : R.drawable.bg_chat_avatar_theirs);
        return avatar;
    }

    private void addSystemHint(String message) {
        TextView hint = new TextView(this);
        hint.setText(message);
        hint.setTextColor(getColor(R.color.booking_muted));
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(16), dp(24), dp(16), dp(24));
        messagesContainer.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
    }

    private String formatTime(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return "Bây giờ";
        }
        String cleaned = createdAt.replace('T', ' ');
        int spaceIndex = cleaned.indexOf(' ');
        if (spaceIndex >= 0 && cleaned.length() >= spaceIndex + 6) {
            return cleaned.substring(spaceIndex + 1, spaceIndex + 6);
        }
        return cleaned.length() >= 5 ? cleaned.substring(0, 5) : cleaned;
    }

    private void scrollToBottom() {
        messagesScroll.post(() -> messagesScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
