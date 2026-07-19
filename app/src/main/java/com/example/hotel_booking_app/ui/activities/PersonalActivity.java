package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.utils.SessionManager;

public class PersonalActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private LinearLayout guestActions;
    private LinearLayout customerActions;
    private TextView profileTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal);

        sessionManager = new SessionManager(this);
        guestActions = findViewById(R.id.container_guest_actions);
        customerActions = findViewById(R.id.container_customer_actions);
        profileTitle = findViewById(R.id.text_profile_title);

        LinearLayout aboutTab = findViewById(R.id.nav_about);
        LinearLayout cabinsTab = findViewById(R.id.nav_cabins);
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

        aboutTab.setOnClickListener(view -> startActivity(new Intent(this, HomeActivity.class)));
        cabinsTab.setOnClickListener(view -> openCabinsForRole());
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
        logoutButton.setOnClickListener(view -> {
            sessionManager.logout();
            renderState();
        });

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
        profileTitle.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        profileTitle.setText(loggedIn
                ? (isAdmin ? "Admin Center" : "Welcome, " + sessionManager.getFullName())
                : "Guest Profile");

        Button bookingsButton = findViewById(R.id.button_my_bookings);
        Button wishlistButton = findViewById(R.id.button_my_wishlist);
        Button paymentsButton = findViewById(R.id.button_payment_history);
        Button messagesButton = findViewById(R.id.button_messages);
        Button notificationsButton = findViewById(R.id.button_notifications);
        Button hostButton = findViewById(R.id.button_host_dashboard);
        Button adminSettingsButton = findViewById(R.id.button_admin_settings);
        Button profileButton = findViewById(R.id.button_profile);

        bookingsButton.setVisibility(View.VISIBLE);
        bookingsButton.setText(isAdmin ? "Manage Bookings" : "Manage Booking");
        wishlistButton.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        paymentsButton.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        messagesButton.setText(isAdmin ? "Customer Messages" : "Messages");
        notificationsButton.setText(isAdmin ? "System Notifications" : "Notifications");
        profileButton.setText(isAdmin ? "Admin Personal Info" : "Manage Personal Info");
        hostButton.setText(isAdmin ? "Manage Cabins" : "Host Dashboard");
        hostButton.setVisibility(View.GONE);
        adminSettingsButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
    }

    private void openCabinsForRole() {
        Class<?> target = sessionManager.isHostOrAdmin() ? HostDashboardActivity.class : CabinListActivity.class;
        startActivity(new Intent(this, target));
    }
}
