package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {
    private TextView summaryTextView;
    private TextView statusTextView;
    private AuthService authService;
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authService = new AuthService();
        sessionManager = new SessionManager(this);
        summaryTextView = findViewById(R.id.text_profile_summary);
        statusTextView = findViewById(R.id.text_status);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        Button updateButton = findViewById(R.id.button_save_profile);
        Button passwordButton = findViewById(R.id.button_change_password);

        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        updateButton.setOnClickListener(view -> startActivity(new Intent(this, UpdateProfileActivity.class)));
        passwordButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, UpdateProfileActivity.class);
            intent.putExtra("show_password", true);
            startActivity(intent);
        });
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (summaryTextView != null) {
            loadProfile();
        }
    }

    private void loadProfile() {
        statusTextView.setText("Loading profile...");
        authService.getUserById(sessionManager.getUserId(), new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                renderProfile(user);
                statusTextView.setText("Profile ready.");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, "Could not load profile", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Could not load profile: " + message);
            }
        });
    }

    private void renderProfile(User user) {
        summaryTextView.setText(
                "Full name\n" + safe(user.getFullName())
                        + "\n\nEmail\n" + safe(user.getEmail())
                        + "\n\nPhone\n" + safe(user.getPhone())
                        + "\n\nNational ID\n" + safe(user.getNationalId())
                        + "\n\nDate of Birth\n" + safe(user.getDateOfBirth())
        );
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
