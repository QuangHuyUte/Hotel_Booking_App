package com.example.hotel_booking_app.ui.activities;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.AppNotification;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.NotificationService;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.List;

public class NotificationCenterActivity extends AppCompatActivity {
    private TextView statusTextView;
    private LinearLayout container;
    private NotificationService notificationService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        notificationService = new NotificationService();
        sessionManager = new SessionManager(this);
        statusTextView = findViewById(R.id.text_status);
        container = findViewById(R.id.container_notifications);
        Button backButton = findViewById(R.id.button_back);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);
        backButton.setOnClickListener(view -> finish());
        bottomBackButton.setOnClickListener(view -> finish());
        loadNotifications();
    }

    private void loadNotifications() {
        statusTextView.setText("Đang tải thông báo...");
        notificationService.getNotifications(sessionManager.getUserId(), new SupabaseCallback<List<AppNotification>>() {
            @Override
            public void onSuccess(List<AppNotification> notifications) {
                renderNotifications(notifications);
                statusTextView.setText("Bạn có " + notifications.size() + " thông báo.");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void renderNotifications(List<AppNotification> notifications) {
        container.removeAllViews();
        if (notifications.isEmpty()) {
            TextView empty = makeText("Chưa có thông báo.");
            container.addView(empty);
            return;
        }
        for (AppNotification notification : notifications) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_panel);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 12);
            card.setLayoutParams(cardParams);

            TextView title = makeText((notification.isRead() ? "" : "Mới - ") + notification.getTitle());
            title.setTextColor(getColor(R.color.primary));
            title.setTextSize(18);
            TextView message = makeText(notification.getMessage());
            Button readButton = new Button(this);
            readButton.setText(notification.isRead() ? "Đã đọc" : "Đánh dấu đã đọc");
            readButton.setTextColor(getColor(R.color.primary_dark));
            readButton.setBackgroundResource(R.drawable.bg_button_secondary);
            readButton.setEnabled(!notification.isRead());
            readButton.setOnClickListener(view -> markRead(notification.getId()));
            card.addView(title);
            card.addView(message);
            card.addView(readButton);
            container.addView(card);
        }
    }

    private TextView makeText(String value) {
        TextView textView = new TextView(this);
        textView.setText(value == null ? "" : value);
        textView.setTextColor(getColor(R.color.ink));
        textView.setTextSize(15);
        textView.setPadding(8, 8, 8, 8);
        return textView;
    }

    private void markRead(String notificationId) {
        notificationService.markAsRead(notificationId, new SupabaseCallback<AppNotification>() {
            @Override
            public void onSuccess(AppNotification data) {
                loadNotifications();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }
}
