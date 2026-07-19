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
    private TextView avatarTextView;
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private TextView nationalIdTextView;
    private TextView dateOfBirthTextView;
    private TextView roleTextView;
    private TextView addressTextView;
    private TextView nationalityTextView;
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
        avatarTextView = findViewById(R.id.text_profile_avatar);
        nameTextView = findViewById(R.id.text_profile_name);
        emailTextView = findViewById(R.id.text_profile_email);
        phoneTextView = findViewById(R.id.text_profile_phone);
        nationalIdTextView = findViewById(R.id.text_profile_national_id);
        dateOfBirthTextView = findViewById(R.id.text_profile_birth);
        roleTextView = findViewById(R.id.text_profile_role);
        addressTextView = findViewById(R.id.text_profile_address);
        nationalityTextView = findViewById(R.id.text_profile_nationality);
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
        if (nameTextView != null) {
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
        avatarTextView.setText(initials(user.getFullName()));
        nameTextView.setText(safe(user.getFullName()));
        emailTextView.setText(safe(user.getEmail()));
        phoneTextView.setText("Phone\n" + safe(user.getPhone()));
        nationalIdTextView.setText("National ID\n" + safe(user.getNationalId()));
        dateOfBirthTextView.setText("Date of birth\n" + safe(user.getDateOfBirth()));
        roleTextView.setText("Account type\n" + safe(user.getRole()));
        addressTextView.setText("Address\n" + safe(user.getAddress()));
        nationalityTextView.setText("Nationality\n" + safe(user.getNationality()));
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "S";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
