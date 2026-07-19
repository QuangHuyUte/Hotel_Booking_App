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

public class PersonalActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_personal);

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
        bookingsTab.setOnClickListener(view -> openLoginRequired(MyBookingsActivity.class));
        wishlistTab.setOnClickListener(view -> openLoginRequired(MyWishlistActivity.class));
        messagesTab.setOnClickListener(view -> openLoginRequired(MessagesActivity.class));
        loginButton.setOnClickListener(view -> startActivity(new Intent(this, LoginActivity.class)));
        signupButton.setOnClickListener(view -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotButton.setOnClickListener(view -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        bookingsButton.setOnClickListener(view -> {
            Class<?> target = sessionManager.isHostOrAdmin() ? AdminBookingsActivity.class : MyBookingsActivity.class;
            startActivity(new Intent(this, target));
        });
        wishlistButton.setOnClickListener(view -> startActivity(new Intent(this, MyWishlistActivity.class)));
        paymentsButton.setOnClickListener(view -> startActivity(new Intent(this, PaymentHistoryActivity.class)));
        messagesButton.setOnClickListener(view -> startActivity(new Intent(this, MessagesActivity.class)));
        notificationsButton.setOnClickListener(view -> startActivity(new Intent(this, NotificationsActivity.class)));
        profileButton.setOnClickListener(view -> startActivity(new Intent(this, ProfileActivity.class)));
        hostButton.setOnClickListener(view -> startActivity(new Intent(this, HostDashboardActivity.class)));
        adminSettingsButton.setOnClickListener(view -> startActivity(new Intent(this, AdminSettingsActivity.class)));
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
                ? (isAdmin ? "Admin Center" : "Chào, " + sessionManager.getFullName())
                : "Your account");

        Button bookingsButton = findViewById(R.id.button_my_bookings);
        Button wishlistButton = findViewById(R.id.button_my_wishlist);
        Button paymentsButton = findViewById(R.id.button_payment_history);
        Button messagesButton = findViewById(R.id.button_messages);
        Button notificationsButton = findViewById(R.id.button_notifications);
        Button hostButton = findViewById(R.id.button_host_dashboard);
        Button adminSettingsButton = findViewById(R.id.button_admin_settings);
        Button profileButton = findViewById(R.id.button_profile);

        bookingsButton.setVisibility(View.VISIBLE);
        bookingsButton.setText(isAdmin ? "Manage bookings" : "Trips");
        wishlistButton.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        paymentsButton.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        messagesButton.setText(isAdmin ? "Customer messages" : "Messages");
        notificationsButton.setText(isAdmin ? "System notifications" : "Notifications");
        profileButton.setText(isAdmin ? "Admin personal info" : "Personal info");
        hostButton.setText(isAdmin ? "Manage cabins" : "Host dashboard");
        hostButton.setVisibility(View.GONE);
        adminSettingsButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        updateGeniusState();
    }

    private void updateGeniusState() {
        if (!sessionManager.isLoggedIn()) {
            geniusLevelTextView.setText("Genius Level 1");
            geniusRewardsTextView.setText("You have 3 Genius rewards");
            geniusDescriptionTextView.setText("Log in to track bookings and unlock more savings.");
            geniusProgressTextView.setText("No completed stays yet.");
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
                geniusLevelTextView.setText("Genius Level " + geniusLevel);
                geniusRewardsTextView.setText("You have " + rewards + " Genius rewards");
                geniusDescriptionTextView.setText(geniusLevel == 1
                        ? "Save 10% and more on selected stays."
                        : geniusLevel == 2
                        ? "Save 10% to 15% and unlock better picks."
                        : "Save 15% and more on selected stays.");
                geniusProgressTextView.setText(buildGeniusProgress(completedBookings, geniusLevel));
            }

            @Override
            public void onError(String message) {
                geniusLevelTextView.setText("Genius Level 1");
                geniusRewardsTextView.setText("You have 3 Genius rewards");
                geniusDescriptionTextView.setText("Save 10% and more on selected stays.");
                geniusProgressTextView.setText("Tracking unavailable right now.");
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
            return "You are already on the highest Genius level.";
        }
        int target = geniusLevel == 1 ? 5 : 15;
        int remaining = Math.max(0, target - completedBookings);
        return remaining + " more bookings to reach Genius Level " + (geniusLevel + 1);
    }

    private void openCabinsForRole() {
        Class<?> target = sessionManager.isHostOrAdmin() ? HostDashboardActivity.class : CabinListActivity.class;
        startActivity(new Intent(this, target));
    }

    private void openLoginRequired(Class<?> target) {
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        Class<?> resolvedTarget = sessionManager.isHostOrAdmin() && target == MyBookingsActivity.class
                ? AdminBookingsActivity.class
                : target;
        startActivity(new Intent(this, resolvedTarget));
    }

    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log out", (dialog, which) -> {
                    sessionManager.logout();
                    renderState();
                })
                .show();
    }
}
