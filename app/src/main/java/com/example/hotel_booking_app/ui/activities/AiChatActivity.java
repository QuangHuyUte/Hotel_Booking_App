package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AiAssistantService;
import com.example.hotel_booking_app.services.AiAssistantService.AiRecommendation;
import com.example.hotel_booking_app.services.AiAssistantService.AiSearchQuery;
import com.example.hotel_booking_app.services.AiAssistantService.AiSearchResult;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AiChatActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AiAssistantService aiAssistantService;
    private LinearLayout messagesContainer;
    private ScrollView messagesScroll;
    private EditText messageEditText;
    private boolean isSearching;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_thread);

        aiAssistantService = new AiAssistantService();
        TextView titleTextView = findViewById(R.id.text_title);
        TextView statusTextView = findViewById(R.id.text_status);
        TextView avatarTextView = findViewById(R.id.avatar_host);
        Button backButton = findViewById(R.id.button_back);
        Button sendButton = findViewById(R.id.button_send);
        messagesContainer = findViewById(R.id.container_messages);
        messagesScroll = findViewById(R.id.scroll_messages);
        messageEditText = findViewById(R.id.edit_message);

        titleTextView.setText("Trợ lý Serein");
        statusTextView.setText("Tìm khách sạn bằng dữ liệu thật");
        avatarTextView.setText("AI");
        messageEditText.setHint("Bạn muốn tìm phòng như thế nào?");
        backButton.setOnClickListener(view -> finish());
        sendButton.setOnClickListener(view -> submitCurrentMessage());

        renderWelcome();
    }

    private void renderWelcome() {
        messagesContainer.removeAllViews();
        addMessageRow(
                "Xin chào, mình là Trợ lý Serein. Cuộc trò chuyện này giúp bạn tìm khách sạn và loại phòng phù hợp từ dữ liệu thật trong app. Bạn có thể nói điểm đến, ngày ở, số người, giá hoặc tiện nghi mong muốn.",
                false,
                "Sẵn sàng"
        );
        addSuggestions();
        scrollToBottom();
    }

    private void addSuggestions() {
        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setPadding(dp(42), dp(2), dp(12), dp(8));
        messagesContainer.addView(stack, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        addSuggestion(stack, "Tìm khách sạn ở Đà Nẵng cho 2 người có hồ bơi");
        addSuggestion(stack, "Phòng ở TP. Hồ Chí Minh 25-26/7 cho 2 người");
        addSuggestion(stack, "Tôi muốn phòng suite ở Vũng Tàu cho 4 người");
    }

    private void addSuggestion(LinearLayout parent, String text) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(getColor(R.color.booking_blue));
        button.setTextSize(13);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setBackgroundResource(R.drawable.bg_booking_secondary);
        button.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        params.setMargins(0, dp(6), 0, 0);
        parent.addView(button, params);
        button.setOnClickListener(view -> submitMessage(text));
    }

    private void submitCurrentMessage() {
        String message = messageEditText.getText().toString().trim();
        if (!message.isEmpty()) {
            submitMessage(message);
        }
    }

    private void submitMessage(String message) {
        if (isSearching) {
            Toast.makeText(this, "Trợ lý đang xử lý yêu cầu trước đó", Toast.LENGTH_SHORT).show();
            return;
        }
        isSearching = true;
        messageEditText.setText("");
        addMessageRow(message, true, "Bây giờ");
        TextView progress = addMessageRow("Đang phân tích nhu cầu...", false, "");
        scrollToBottom();

        handler.postDelayed(() -> progress.setText("Đang kiểm tra khách sạn phù hợp..."), 450);
        aiAssistantService.searchHotels(message, new SupabaseCallback<AiSearchResult>() {
            @Override
            public void onSuccess(AiSearchResult result) {
                runOnUiThread(() -> renderResult(progress, result));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    isSearching = false;
                    progress.setText("Mình chưa lấy được dữ liệu khách sạn. Bạn thử lại sau nhé.");
                    Toast.makeText(AiChatActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderResult(TextView progress, AiSearchResult result) {
        isSearching = false;
        List<AiRecommendation> recommendations = result.getRecommendations();
        int count = recommendations == null ? 0 : recommendations.size();
        progress.setText(count == 0
                ? "Mình chưa tìm thấy khách sạn khớp đúng yêu cầu. Bạn thử nới bớt giá, tiện nghi hoặc đổi khu vực nhé."
                : "Đã tìm thấy " + count + " lựa chọn phù hợp. Đây là các khách sạn lấy trực tiếp từ dữ liệu phòng hiện có:");
        if (count == 0) {
            addMessageRow(buildQuerySummary(result.getQuery()), false, "");
            scrollToBottom();
            return;
        }

        addMessageRow(buildQuerySummary(result.getQuery()), false, "");
        for (int i = 0; i < recommendations.size(); i++) {
            addRecommendationCard(recommendations.get(i), result.getQuery(), i + 1);
        }
        scrollToBottom();
    }

    private String buildQuerySummary(AiSearchQuery query) {
        String destination = query.getDestination().isEmpty() ? "mọi điểm đến" : query.getDestination();
        String price = query.getMaxPricePerNight() > 0
                ? " · tối đa " + PriceUtils.formatUsd(query.getMaxPricePerNight()) + " / đêm"
                : "";
        return "Bộ lọc: " + destination
                + " · " + formatDate(query.getCheckIn()) + " - " + formatDate(query.getCheckOut())
                + " · " + query.getRooms() + " phòng"
                + " · " + query.getAdults() + " người lớn"
                + price;
    }

    private void addRecommendationCard(AiRecommendation recommendation, AiSearchQuery query, int index) {
        Cabin cabin = recommendation.getCabin();
        RoomType roomType = recommendation.getRoomType();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_booking_card);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dp(42), dp(8), dp(10), dp(10));
        messagesContainer.addView(card, cardParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(imageView, new LinearLayout.LayoutParams(dp(86), dp(86)));
        Glide.with(this).load(cabin.getImage()).centerCrop().into(imageView);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        contentParams.setMargins(dp(10), 0, 0, 0);
        row.addView(content, contentParams);

        TextView name = makeText(index + ". " + cabin.getName(), 15, R.color.booking_text, true);
        name.setMaxLines(2);
        content.addView(name);

        TextView meta = makeText(safe(cabin.getDistrict(), cabin.getLocation()), 12, R.color.booking_muted, false);
        meta.setMaxLines(2);
        content.addView(meta);

        TextView room = makeText(roomType.displayName()
                + " · " + roomType.sizeLabel()
                + " · " + roomType.bedLabel(), 12, R.color.booking_text, false);
        room.setMaxLines(2);
        content.addView(room);

        TextView price = makeText(PriceUtils.formatUsd(roomType.getBasePrice()) + " / đêm", 16, R.color.booking_blue, true);
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        priceParams.setMargins(0, dp(4), 0, 0);
        content.addView(price, priceParams);

        TextView reasons = makeText(join(recommendation.getReasons(), " · "), 12, R.color.booking_muted, false);
        reasons.setPadding(0, dp(8), 0, 0);
        card.addView(reasons);

        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText("Xem chi tiết và chọn phòng");
        button.setTextColor(getColor(R.color.white));
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_booking_cta);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        buttonParams.setMargins(0, dp(10), 0, 0);
        card.addView(button, buttonParams);
        button.setOnClickListener(view -> openHotel(cabin, roomType, query));
        card.setOnClickListener(view -> openHotel(cabin, roomType, query));
    }

    private void openHotel(Cabin cabin, RoomType roomType, AiSearchQuery query) {
        Intent intent = new Intent(this, HotelDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabin.getId());
        intent.putExtra(AppConstants.EXTRA_ROOM_TYPE_ID, roomType.getId());
        intent.putExtra("checkIn", query.getCheckIn());
        intent.putExtra("checkOut", query.getCheckOut());
        startActivity(intent);
    }

    private TextView addMessageRow(String body, boolean isMine, String metaText) {
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
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.72f));
        bubble.setBackgroundResource(isMine ? R.drawable.bg_chat_bubble_mine : R.drawable.bg_chat_bubble_theirs);
        stack.addView(bubble);

        if (metaText != null && !metaText.trim().isEmpty()) {
            TextView meta = makeText(metaText, 11, R.color.booking_muted, false);
            meta.setGravity(isMine ? Gravity.END : Gravity.START);
            meta.setPadding(0, dp(4), 0, 0);
            stack.addView(meta);
        }

        if (isMine) {
            row.addView(stack, stackParams);
            row.addView(avatar);
        } else {
            row.addView(avatar);
            row.addView(stack, stackParams);
        }
        return bubble;
    }

    private TextView makeAvatar(boolean isMine) {
        TextView avatar = new TextView(this);
        avatar.setWidth(dp(34));
        avatar.setHeight(dp(34));
        avatar.setGravity(Gravity.CENTER);
        avatar.setText(isMine ? "Tôi" : "AI");
        avatar.setTextColor(getColor(isMine ? R.color.black : R.color.white));
        avatar.setTextSize(11);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setBackgroundResource(isMine ? R.drawable.bg_chat_avatar_mine : R.drawable.bg_chat_avatar_theirs);
        return avatar;
    }

    private TextView makeText(String value, int sp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(getColor(color));
        textView.setLineSpacing(0, 1.08f);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return textView;
    }

    private String formatDate(String value) {
        try {
            return LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM"));
        } catch (Exception ignored) {
            return value;
        }
    }

    private String safe(String primary, String fallback) {
        return primary == null || primary.trim().isEmpty() ? fallback : primary.trim();
    }

    private String join(List<String> values, String separator) {
        if (values == null || values.isEmpty()) {
            return "Đề xuất theo dữ liệu khách sạn và loại phòng hiện có";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private void scrollToBottom() {
        messagesScroll.post(() -> messagesScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
