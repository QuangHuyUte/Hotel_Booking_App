package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.ui.helpers.ManagerNavigationHelper;
import com.example.hotel_booking_app.utils.SessionManager;

public class ProfileDetailsActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_profile_details);

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
        Button logoutButton = findViewById(R.id.button_logout);

        backButton.setVisibility(View.GONE);
        backBottomButton.setVisibility(View.GONE);
        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        updateButton.setOnClickListener(view -> startActivity(new Intent(this, EditProfileActivity.class)));
        passwordButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("show_password", true);
            startActivity(intent);
        });
        logoutButton.setOnClickListener(view -> showLogoutConfirmation());
        if (sessionManager.isHostOrAdmin()) {
            ManagerNavigationHelper.bind(this, ManagerNavigationHelper.TAB_PROFILE);
        }
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
        statusTextView.setText("Đang tải hồ sơ...");
        authService.getUserById(sessionManager.getUserId(), new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                renderProfile(user);
                statusTextView.setText("Hồ sơ đã sẵn sàng.");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileDetailsActivity.this, "Không thể tải hồ sơ", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Không thể tải hồ sơ: " + message);
            }
        });
    }

    private void renderProfile(User user) {
        avatarTextView.setText(initials(user.getFullName()));
        nameTextView.setText(safe(user.getFullName()));
        emailTextView.setText(safe(user.getEmail()));
        phoneTextView.setText("Số điện thoại\n" + safe(user.getPhone()));
        nationalIdTextView.setText("CCCD / CMND\n" + safe(user.getNationalId()));
        dateOfBirthTextView.setText("Ngày sinh\n" + safe(user.getDateOfBirth()));
        roleTextView.setText("Loại tài khoản\n" + roleLabel(user.getRole()));
        addressTextView.setText("Địa chỉ\n" + safe(user.getAddress()));
        nationalityTextView.setText("Quốc tịch\n" + safe(user.getNationality()));
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String roleLabel(String role) {
        if ("manager".equalsIgnoreCase(role)) {
            return "Quản lý";
        }
        if ("customer".equalsIgnoreCase(role)) {
            return "Khách hàng";
        }
        return safe(role);
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

    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đăng xuất?")
                .setMessage("Bạn có chắc muốn đăng xuất khỏi tài khoản này không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(this, SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}
