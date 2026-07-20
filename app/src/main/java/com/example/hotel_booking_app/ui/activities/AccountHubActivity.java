package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.utils.SessionManager;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.List;

public class AccountHubActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private BookingService bookingService;
    private LinearLayout guestActions;
    private LinearLayout customerActions;
    private TextView profileTitle;
    private TextView geniusLevelTextView;
    private TextView geniusRewardsTextView;
    private TextView geniusDescriptionTextView;
    private TextView geniusProgressTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_hub);

        sessionManager = new SessionManager(this);
        bookingService = new BookingService();
        guestActions = findViewById(R.id.container_guest_actions);
        customerActions = findViewById(R.id.container_customer_actions);
        profileTitle = findViewById(R.id.text_profile_title);
        geniusLevelTextView = findViewById(R.id.text_genius_level);
        geniusRewardsTextView = findViewById(R.id.text_genius_rewards);
        geniusDescriptionTextView = findViewById(R.id.text_genius_description);
        geniusProgressTextView = findViewById(R.id.text_genius_progress);

        LinearLayout cabinsTab = findViewById(R.id.nav_cabins);
        LinearLayout bookingsTab = findViewById(R.id.nav_bookings);
        LinearLayout wishlistTab = findViewById(R.id.nav_wishlist);
        LinearLayout messagesTab = findViewById(R.id.nav_messages);
        Button loginButton = findViewById(R.id.button_login);
        Button signupButton = findViewById(R.id.button_signup);
        TextView forgotButton = findViewById(R.id.text_forgot_password);
        Button bookingsButton = findViewById(R.id.button_my_bookings);
        Button wishlistButton = findViewById(R.id.button_my_wishlist);
        Button paymentsButton = findViewById(R.id.button_payment_history);
        Button messagesButton = findViewById(R.id.button_messages);
        Button notificationsButton = findViewById(R.id.button_notifications);
        Button profileButton = findViewById(R.id.button_profile);
        Button hostButton = findViewById(R.id.button_host_dashboard);
        Button adminSettingsButton = findViewById(R.id.button_admin_settings);
        Button logoutButton = findViewById(R.id.button_logout);

        cabinsTab.setOnClickListener(view -> openCabinsForRole());
        bookingsTab.setOnClickListener(view -> openLoginRequired(GuestBookingsActivity.class));
        wishlistTab.setOnClickListener(view -> openLoginRequired(SavedHotelsActivity.class));
        messagesTab.setOnClickListener(view -> openLoginRequired(ConversationListActivity.class));
        loginButton.setOnClickListener(view -> startActivity(new Intent(this, SignInActivity.class)));
        signupButton.setOnClickListener(view -> startActivity(new Intent(this, SignUpActivity.class)));
        forgotButton.setOnClickListener(view -> startActivity(new Intent(this, PasswordResetActivity.class)));
        bookingsButton.setOnClickListener(view -> {
            Class<?> target = sessionManager.isHostOrAdmin() ? AdminBookingManagementActivity.class : GuestBookingsActivity.class;
            startActivity(new Intent(this, target));
        });
        wishlistButton.setOnClickListener(view -> startActivity(new Intent(this, SavedHotelsActivity.class)));
        paymentsButton.setOnClickListener(view -> startActivity(new Intent(this, PaymentHistoryActivity.class)));
        messagesButton.setOnClickListener(view -> startActivity(new Intent(this, ConversationListActivity.class)));
        notificationsButton.setOnClickListener(view -> startActivity(new Intent(this, NotificationCenterActivity.class)));
        profileButton.setOnClickListener(view -> startActivity(new Intent(this, ProfileDetailsActivity.class)));
        hostButton.setOnClickListener(view -> startActivity(new Intent(this, HostHotelDashboardActivity.class)));
        adminSettingsButton.setOnClickListener(view -> startActivity(new Intent(this, AdminAppSettingsActivity.class)));
        logoutButton.setOnClickListener(view -> showLogoutConfirmation());

        renderState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderState();
    }

    private void renderState() {
        boolean loggedIn = sessionManager.isLoggedIn();
        boolean isAdmin = sessionManager.isHostOrAdmin();
        guestActions.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        customerActions.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        profileTitle.setVisibility(View.VISIBLE);
        profileTitle.setText(loggedIn
                ? "Xin chào, " + displayName()
                : "Tài khoản của bạn");

        Button bookingsButton = findViewById(R.id.button_my_bookings);
        Button wishlistButton = findViewById(R.id.button_my_wishlist);
        Button paymentsButton = findViewById(R.id.button_payment_history);
        Button messagesButton = findViewById(R.id.button_messages);
        Button notificationsButton = findViewById(R.id.button_notifications);
        Button hostButton = findViewById(R.id.button_host_dashboard);
        Button adminSettingsButton = findViewById(R.id.button_admin_settings);
        Button profileButton = findViewById(R.id.button_profile);

        bookingsButton.setVisibility(View.VISIBLE);
        bookingsButton.setText(isAdmin ? "Quản lý đặt phòng" : "Chuyến đi");
        wishlistButton.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        paymentsButton.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        messagesButton.setText(isAdmin ? "Tin nhắn khách hàng" : "Tin nhắn");
        notificationsButton.setText(isAdmin ? "Thông báo hệ thống" : "Thông báo");
        profileButton.setText(isAdmin ? "Thông tin quản lý" : "Thông tin cá nhân");
        hostButton.setText(isAdmin ? "Quản lý khách sạn" : "Bảng quản lý khách sạn");
        hostButton.setVisibility(View.GONE);
        adminSettingsButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        updateGeniusState();
    }

    private void updateGeniusState() {
        if (!sessionManager.isLoggedIn()) {
            geniusLevelTextView.setText("Genius cấp 1");
            geniusRewardsTextView.setText("Bạn có 3 ưu đãi Genius");
            geniusDescriptionTextView.setText("Đăng nhập để theo dõi đặt phòng và mở thêm ưu đãi.");
            geniusProgressTextView.setText("Chưa có kỳ nghỉ hoàn tất.");
            return;
        }

        bookingService.getBookingsForUser(sessionManager.getUserId(), new SupabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                int completedBookings = 0;
                for (Booking booking : bookings) {
                    if (AppConstants.BOOKING_CHECKED_OUT.equalsIgnoreCase(booking.getStatus())) {
                        completedBookings++;
                    }
                }
                int geniusLevel = resolveGeniusLevel(completedBookings);
                int rewards = geniusLevel == 1 ? 3 : geniusLevel == 2 ? 5 : 7;
                geniusLevelTextView.setText("Genius cấp " + geniusLevel);
                geniusRewardsTextView.setText("Bạn có " + rewards + " ưu đãi Genius");
                geniusDescriptionTextView.setText(geniusLevel == 1
                        ? "Tiết kiệm từ 10% cho một số chỗ nghỉ."
                        : geniusLevel == 2
                        ? "Tiết kiệm 10% đến 15% và mở thêm lựa chọn tốt hơn."
                        : "Tiết kiệm từ 15% cho một số chỗ nghỉ.");
                geniusProgressTextView.setText(buildGeniusProgress(completedBookings, geniusLevel));
            }

            @Override
            public void onError(String message) {
                geniusLevelTextView.setText("Genius cấp 1");
                geniusRewardsTextView.setText("Bạn có 3 ưu đãi Genius");
                geniusDescriptionTextView.setText("Tiết kiệm từ 10% cho một số chỗ nghỉ.");
                geniusProgressTextView.setText("Hiện chưa thể cập nhật tiến độ.");
            }
        });
    }

    private int resolveGeniusLevel(int completedBookings) {
        if (completedBookings >= 15) {
            return 3;
        }
        if (completedBookings >= 5) {
            return 2;
        }
        return 1;
    }

    private String buildGeniusProgress(int completedBookings, int geniusLevel) {
        if (geniusLevel >= 3) {
            return "Bạn đã ở cấp Genius cao nhất.";
        }
        int target = geniusLevel == 1 ? 5 : 15;
        int remaining = Math.max(0, target - completedBookings);
        return "Còn " + remaining + " đặt phòng để lên Genius cấp " + (geniusLevel + 1);
    }

    private void openCabinsForRole() {
        Class<?> target = sessionManager.isHostOrAdmin() ? HostHotelDashboardActivity.class : HotelSearchActivity.class;
        startActivity(new Intent(this, target));
    }

    private String displayName() {
        String name = sessionManager.getFullName();
        if (name == null || name.trim().isEmpty()) {
            return sessionManager.isHostOrAdmin() ? "Quản lý" : "bạn";
        }
        return name.trim();
    }

    private void openLoginRequired(Class<?> target) {
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            return;
        }
        Class<?> resolvedTarget = sessionManager.isHostOrAdmin() && target == GuestBookingsActivity.class
                ? AdminBookingManagementActivity.class
                : target;
        startActivity(new Intent(this, resolvedTarget));
    }

    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đăng xuất?")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    sessionManager.logout();
                    renderState();
                })
                .show();
    }
}
