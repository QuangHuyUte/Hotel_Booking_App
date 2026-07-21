package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Conversation;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.ChatService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.ui.helpers.CustomerNavigationHelper;
import com.example.hotel_booking_app.ui.helpers.ManagerNavigationHelper;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.List;

public class ConversationListActivity extends AppCompatActivity {
    private TextView statusTextView;
    private LinearLayout conversationsContainer;
    private ChatService chatService;
    private CabinService cabinService;
    private BookingService bookingService;
    private RoomTypeService roomTypeService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);

        statusTextView = findViewById(R.id.text_status);
        conversationsContainer = findViewById(R.id.container_conversations);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        chatService = new ChatService();
        cabinService = new CabinService();
        bookingService = new BookingService();
        roomTypeService = new RoomTypeService();
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        backButton.setVisibility(View.GONE);
        backBottomButton.setVisibility(View.GONE);
        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        if (sessionManager.isHostOrAdmin()) {
            View customerNav = findViewById(R.id.nav_customer_root);
            if (customerNav != null) {
                customerNav.setVisibility(View.GONE);
            }
            ManagerNavigationHelper.bind(this, ManagerNavigationHelper.TAB_MESSAGES);
        } else {
            CustomerNavigationHelper.bind(this, CustomerNavigationHelper.TAB_MESSAGES);
        }
        loadConversations();
    }

    private void loadConversations() {
        statusTextView.setText("Đang tải cuộc trò chuyện...");
        SupabaseCallback<List<Conversation>> callback = new SupabaseCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                conversationsContainer.removeAllViews();
                addAiConversationCard();
                int conversationCount = conversations == null ? 0 : conversations.size();
                if (conversationCount == 0) {
                    statusTextView.setText("Trợ lý AI luôn sẵn sàng. Bạn chưa có chat khách sạn nào.");
                    return;
                }
                statusTextView.setText("Trợ lý AI và " + conversationCount + " cuộc trò chuyện với khách.");
                for (Conversation conversation : conversations) {
                    addConversationCard(conversation, "Đang tải khách sạn...");
                    loadConversationDetails(conversation);
                }
            }

            @Override
            public void onError(String message) {
                conversationsContainer.removeAllViews();
                addAiConversationCard();
                statusTextView.setText(message);
            }
        };

        if (sessionManager.isHostOrAdmin()) {
            chatService.getHostConversations(sessionManager.getUserId(), callback);
        } else {
            chatService.getGuestConversations(sessionManager.getUserId(), callback);
        }
    }

    private void addAiConversationCard() {
        LinearLayout card = conversationCardBase(true);
        card.setTag("ai-assistant");
        TextView avatar = avatarView("AI", true);
        LinearLayout content = contentColumn();
        content.addView(line("Trợ lý Serein", R.color.black, 16, true));
        content.addView(line("Tìm khách sạn, loại phòng và giá bằng mô tả tự nhiên", R.color.black, 13, false));
        content.addView(line("Luôn sẵn sàng hỗ trợ khách đặt phòng", R.color.booking_blue, 12, true));
        card.addView(avatar);
        card.addView(content);
        card.setOnClickListener(view -> startActivity(new Intent(this, AiChatActivity.class)));
        conversationsContainer.addView(card);
    }

    private void addConversationCard(Conversation conversation, String hotelName) {
        boolean manager = sessionManager.isHostOrAdmin();
        String guestName = userDisplayName(conversation.getGuestId());
        LinearLayout card = conversationCardBase(false);
        card.setTag(conversation.getId());
        TextView avatar = avatarView(initials(manager ? guestName : hotelName), false);
        avatar.setTag("avatar_" + conversation.getId());
        LinearLayout content = contentColumn();
        TextView title = line(manager ? guestName : hotelName, R.color.booking_text, 16, true);
        title.setTag("title_" + conversation.getId());
        TextView subtitle = line((manager ? "Hotel: " + hotelName : "Khách sạn: " + hotelName), R.color.booking_muted, 13, false);
        subtitle.setTag("subtitle_" + conversation.getId());
        TextView roomLine = line("Room: đang tải loại phòng...", R.color.booking_muted, 12, false);
        roomLine.setTag("room_" + conversation.getId());
        TextView bookingLine = line(bookingStatusLabel(conversation), R.color.booking_blue, 12, true);
        bookingLine.setTag("booking_" + conversation.getId());
        content.addView(title);
        content.addView(subtitle);
        content.addView(roomLine);
        content.addView(bookingLine);
        content.addView(line("Chạm để mở nội dung trò chuyện", R.color.booking_muted, 12, false));
        card.addView(avatar);
        card.addView(content);
        card.setOnClickListener(view -> {
            Intent intent = new Intent(this, ChatThreadActivity.class);
            intent.putExtra(ChatThreadActivity.EXTRA_CONVERSATION_ID, conversation.getId());
            startActivity(intent);
        });
        conversationsContainer.addView(card);
    }

    private LinearLayout conversationCardBase(boolean ai) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(ai ? R.drawable.bg_button_primary : R.drawable.bg_review_card);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);
        return card;
    }

    private TextView avatarView(String text, boolean ai) {
        TextView avatar = new TextView(this);
        avatar.setText(text);
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setTextColor(getColor(ai ? R.color.booking_blue : R.color.white));
        avatar.setTextSize(14f);
        avatar.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.setBackgroundResource(ai ? R.drawable.bg_icon_warm_circle : R.drawable.bg_review_avatar);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dp(46),
                dp(46)
        );
        params.setMargins(0, 0, dp(12), 0);
        avatar.setLayoutParams(params);
        return avatar;
    }

    private LinearLayout contentColumn() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return content;
    }

    private TextView line(String text, int colorRes, int sizeSp, boolean bold) {
        TextView line = new TextView(this);
        line.setText(text);
        line.setTextColor(getColor(colorRes));
        line.setTextSize(sizeSp);
        line.setMaxLines(2);
        line.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (bold) {
            line.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        return line;
    }

    private void loadConversationDetails(Conversation conversation) {
        if (conversation.getCabinId() == null || conversation.getCabinId().trim().isEmpty()) {
            updateConversationText(conversation, null, "Room: khách đang hỏi thông tin chung", bookingStatusLabel(conversation));
            return;
        }
        cabinService.getCabinById(conversation.getCabinId(), new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                String roomText = "Room: khách đang hỏi tư vấn trước khi đặt";
                updateConversationText(conversation, cabin, roomText, bookingStatusLabel(conversation));
                if (conversation.hasBooking()) {
                    loadBookingRoom(conversation, cabin);
                } else {
                    loadSuggestedRoom(conversation, cabin);
                }
            }

            @Override
            public void onError(String message) {
                updateConversationText(conversation, null, "Room: chưa tải được thông tin phòng", bookingStatusLabel(conversation));
            }
        });
    }

    private void loadBookingRoom(Conversation conversation, Cabin cabin) {
        bookingService.getBookingById(conversation.getBookingId(), new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                String bookingLine = "Đã booking · mã " + shortId(conversation.getBookingId())
                        + " · " + booking.getStartDate() + " đến " + booking.getEndDate();
                if (!hasText(booking.getRoomTypeId())) {
                    updateConversationText(conversation, cabin, "Room: chưa gắn loại phòng cho booking này", bookingLine);
                    return;
                }
                roomTypeService.getRoomTypeById(booking.getRoomTypeId(), new SupabaseCallback<RoomType>() {
                    @Override
                    public void onSuccess(RoomType roomType) {
                        String roomText = "Room: " + roomType.displayName() + " · "
                                + roomType.effectiveMaxAdults() + " khách · "
                                + roomType.bedLabel();
                        updateConversationText(conversation, cabin, roomText, bookingLine);
                    }

                    @Override
                    public void onError(String message) {
                        updateConversationText(conversation, cabin, "Room: booking đã có phòng nhưng chưa tải được tên", bookingLine);
                    }
                });
            }

            @Override
            public void onError(String message) {
                updateConversationText(conversation, cabin, "Room: chưa tải được booking", bookingStatusLabel(conversation));
            }
        });
    }

    private void loadSuggestedRoom(Conversation conversation, Cabin cabin) {
        roomTypeService.getRoomTypesForCabin(cabin.getId(), new SupabaseCallback<List<RoomType>>() {
            @Override
            public void onSuccess(List<RoomType> roomTypes) {
                if (roomTypes == null || roomTypes.isEmpty()) {
                    updateConversationText(conversation, cabin, "Room: chưa có loại phòng để tư vấn", bookingStatusLabel(conversation));
                    return;
                }
                RoomType roomType = roomTypes.get(0);
                updateConversationText(conversation, cabin,
                        "Room đang hỏi: " + roomType.displayName() + " · từ $" + roomType.getBasePrice(),
                        bookingStatusLabel(conversation));
            }

            @Override
            public void onError(String message) {
                updateConversationText(conversation, cabin, "Room: khách đang hỏi tư vấn trước khi đặt", bookingStatusLabel(conversation));
            }
        });
    }

    private void updateConversationText(Conversation conversation, Cabin cabin, String roomText, String bookingText) {
        for (int i = 0; i < conversationsContainer.getChildCount(); i++) {
            View card = conversationsContainer.getChildAt(i);
            if (conversation.getId().equals(card.getTag())) {
                TextView title = card.findViewWithTag("title_" + conversation.getId());
                TextView subtitle = card.findViewWithTag("subtitle_" + conversation.getId());
                TextView avatar = card.findViewWithTag("avatar_" + conversation.getId());
                TextView room = card.findViewWithTag("room_" + conversation.getId());
                TextView booking = card.findViewWithTag("booking_" + conversation.getId());
                String guestName = userDisplayName(conversation.getGuestId());
                String hotelName = cabin == null ? "Khách sạn chưa rõ" : cabin.getName();
                if (sessionManager.isHostOrAdmin()) {
                    if (title != null) {
                        title.setText(guestName);
                    }
                    if (subtitle != null) {
                        subtitle.setText("Hotel: " + hotelName);
                    }
                } else if (title != null) {
                    title.setText(hotelName);
                }
                if (avatar != null) {
                    avatar.setText(initials(sessionManager.isHostOrAdmin() ? guestName : hotelName));
                }
                if (room != null) {
                    room.setText(roomText);
                }
                if (booking != null) {
                    booking.setText(bookingText);
                }
                return;
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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

    private String initials(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "S";
        }
        String[] parts = value.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    private String bookingStatusLabel(Conversation conversation) {
        if (conversation == null || !conversation.hasBooking()) {
            return "Chưa booking · tư vấn trước đặt phòng";
        }
        return "Đã booking · mã " + shortId(conversation.getBookingId());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String shortId(String value) {
        if (value == null || value.length() < 8) {
            return "-";
        }
        return value.substring(value.length() - 8);
    }
}
