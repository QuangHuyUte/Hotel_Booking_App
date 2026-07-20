package com.example.hotel_booking_app.ui.activities;

import android.graphics.Typeface;
import android.os.Bundle;
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
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Conversation;
import com.example.hotel_booking_app.data.models.Message;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.ChatService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.List;

public class ChatThreadActivity extends AppCompatActivity {
    public static final String EXTRA_HOST_ID = "extra_host_id";
    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";

    private TextView titleTextView;
    private TextView statusTextView;
    private TextView contextTextView;
    private LinearLayout messagesContainer;
    private ScrollView messagesScroll;
    private EditText messageEditText;
    private ChatService chatService;
    private CabinService cabinService;
    private BookingService bookingService;
    private RoomTypeService roomTypeService;
    private SessionManager sessionManager;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_thread);

        chatService = new ChatService();
        cabinService = new CabinService();
        bookingService = new BookingService();
        roomTypeService = new RoomTypeService();
        sessionManager = new SessionManager(this);
        titleTextView = findViewById(R.id.text_title);
        statusTextView = findViewById(R.id.text_status);
        contextTextView = findViewById(R.id.text_conversation_context);
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
        String cabinId = getIntent().getStringExtra(AppConstants.EXTRA_CABIN_ID);
        statusTextView.setText("Đang mở cuộc trò chuyện...");
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
                renderConversationContext(conversation);
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
        loadConversationContext();
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

    private void loadConversationContext() {
        if (conversationId == null) {
            return;
        }
        chatService.getConversationById(conversationId, new SupabaseCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation conversation) {
                renderConversationContext(conversation);
            }

            @Override
            public void onError(String message) {
                contextTextView.setText("Chưa tải được thông tin hotel và room cho cuộc trò chuyện này.");
            }
        });
    }

    private void renderConversationContext(Conversation conversation) {
        if (conversation == null) {
            return;
        }
        String guestName = userDisplayName(conversation.getGuestId());
        titleTextView.setText(sessionManager.isHostOrAdmin() ? guestName : "Tin nhắn khách sạn");
        contextTextView.setText("Đang tải hotel và room...");
        if (conversation.getCabinId() == null || conversation.getCabinId().trim().isEmpty()) {
            contextTextView.setText(guestName + " · hỏi thông tin chung · chưa booking");
            return;
        }
        cabinService.getCabinById(conversation.getCabinId(), new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                if (!sessionManager.isHostOrAdmin()) {
                    titleTextView.setText(cabin.getName());
                }
                String base = guestName + " · " + cabin.getName();
                if (hasText(conversation.getBookingId())) {
                    loadBookedRoomContext(conversation, base);
                } else {
                    loadSuggestedRoomContext(cabin, base);
                }
            }

            @Override
            public void onError(String message) {
                contextTextView.setText(guestName + " · chưa tải được hotel · "
                        + (hasText(conversation.getBookingId()) ? "đã booking" : "chưa booking"));
            }
        });
    }

    private void loadBookedRoomContext(Conversation conversation, String base) {
        bookingService.getBookingById(conversation.getBookingId(), new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                String bookingText = " · " + booking.getStartDate() + " đến " + booking.getEndDate()
                        + " · mã " + shortId(conversation.getBookingId());
                if (!hasText(booking.getRoomTypeId())) {
                    contextTextView.setText(base + " · room chưa gắn loại phòng" + bookingText);
                    return;
                }
                roomTypeService.getRoomTypeById(booking.getRoomTypeId(), new SupabaseCallback<RoomType>() {
                    @Override
                    public void onSuccess(RoomType roomType) {
                        contextTextView.setText(base + " · " + roomType.displayName() + bookingText);
                    }

                    @Override
                    public void onError(String message) {
                        contextTextView.setText(base + " · đã booking" + bookingText);
                    }
                });
            }

            @Override
            public void onError(String message) {
                contextTextView.setText(base + " · đã booking · mã " + shortId(conversation.getBookingId()));
            }
        });
    }

    private void loadSuggestedRoomContext(Cabin cabin, String base) {
        roomTypeService.getRoomTypesForCabin(cabin.getId(), new SupabaseCallback<List<RoomType>>() {
            @Override
            public void onSuccess(List<RoomType> roomTypes) {
                if (roomTypes == null || roomTypes.isEmpty()) {
                    contextTextView.setText(base + " · đang hỏi phòng · chưa booking");
                    return;
                }
                RoomType roomType = roomTypes.get(0);
                contextTextView.setText(base + " · hỏi trước đặt phòng · gợi ý " + roomType.displayName());
            }

            @Override
            public void onError(String message) {
                contextTextView.setText(base + " · hỏi trước đặt phòng · chưa booking");
            }
        });
    }

    private void renderMessages(List<Message> messages) {
        messagesContainer.removeAllViews();
        if (messages == null || messages.isEmpty()) {
            addSystemHint("Bắt đầu cuộc trò chuyện với quản lý khách sạn.");
            return;
        }
        for (Message message : messages) {
            boolean isMine = sessionManager.getUserId().equals(message.getSenderId());
            String receipt = isMine ? (message.isRead() ? "Đã xem" : "Đã gửi") : "";
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
        messagesContainer.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView avatar = makeAvatar(isMine);
        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setGravity(isMine ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams stackParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        stackParams.setMargins(dp(8), 0, dp(8), 0);

        TextView bubble = new TextView(this);
        bubble.setText(body);
        bubble.setTextColor(getColor(isMine ? R.color.white : R.color.booking_text));
        bubble.setTextSize(15);
        bubble.setLineSpacing(0, 1.08f);
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.68f));
        bubble.setBackgroundResource(isMine ? R.drawable.bg_chat_bubble_mine : R.drawable.bg_chat_bubble_theirs);

        TextView meta = new TextView(this);
        String metaText = receipt == null || receipt.isEmpty() ? time : time + " - " + receipt;
        meta.setText(metaText);
        meta.setTextColor(getColor(R.color.booking_muted));
        meta.setTextSize(11);
        meta.setGravity(isMine ? Gravity.END : Gravity.START);
        meta.setPadding(0, dp(4), 0, 0);

        stack.addView(bubble);
        stack.addView(meta);

        if (isMine) {
            row.addView(stack, stackParams);
            row.addView(avatar);
        } else {
            row.addView(avatar);
            row.addView(stack, stackParams);
        }
    }

    private TextView makeAvatar(boolean isMine) {
        TextView avatar = new TextView(this);
        avatar.setWidth(dp(34));
        avatar.setHeight(dp(34));
        avatar.setGravity(Gravity.CENTER);
        avatar.setText(isMine ? "Tôi" : "QL");
        avatar.setTextColor(getColor(isMine ? R.color.black : R.color.white));
        avatar.setTextSize(11);
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

    private String userDisplayName(String userId) {
        if (userId == null) {
            return "Khách lưu trú";
        }
        if (userId.endsWith("000000000101")) {
            return "Alice Nguyen";
        }
        if (userId.endsWith("000000000102")) {
            return "Bao Tran";
        }
        if (userId.endsWith("000000000103")) {
            return "Chi Pham";
        }
        if (userId.endsWith("000000000104")) {
            return "David Le";
        }
        if (userId.endsWith("000000000105")) {
            return "Eve Hoang";
        }
        return "Khách lưu trú";
    }

    private String shortId(String value) {
        if (value == null || value.length() < 8) {
            return "-";
        }
        return value.substring(value.length() - 8);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
