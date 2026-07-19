package com.example.hotel_booking_app.ui.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.Locale;

public class UpdateProfileActivity extends AppCompatActivity {
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText nationalIdEditText;
    private EditText birthEditText;
    private EditText addressEditText;
    private EditText newPasswordEditText;
    private TextView statusTextView;
    private AuthService authService;
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        authService = new AuthService();
        sessionManager = new SessionManager(this);
        nameEditText = findViewById(R.id.edit_name);
        emailEditText = findViewById(R.id.edit_email);
        phoneEditText = findViewById(R.id.edit_phone);
        nationalIdEditText = findViewById(R.id.edit_national_id);
        birthEditText = findViewById(R.id.edit_birth);
        addressEditText = findViewById(R.id.edit_address);
        newPasswordEditText = findViewById(R.id.edit_new_password);
        statusTextView = findViewById(R.id.text_status);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        Button saveButton = findViewById(R.id.button_save_profile);
        Button passwordButton = findViewById(R.id.button_change_password);

        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        saveButton.setOnClickListener(view -> saveProfile());
        passwordButton.setOnClickListener(view -> changePassword());
        setupEmailDisplay();
        setupBirthPicker();
        if (getIntent().getBooleanExtra("show_password", false)) {
            newPasswordEditText.setVisibility(View.VISIBLE);
        }
        loadProfile();
    }

    private void setupEmailDisplay() {
        emailEditText.setFocusable(false);
        emailEditText.setCursorVisible(false);
        emailEditText.setKeyListener(null);
    }

    private void setupBirthPicker() {
        birthEditText.setFocusable(false);
        birthEditText.setCursorVisible(false);
        birthEditText.setKeyListener(null);
        birthEditText.setOnClickListener(view -> {
            int year = 2000;
            int month = 0;
            int day = 1;
            String currentValue = birthEditText.getText().toString().trim();
            if (currentValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                year = Integer.parseInt(currentValue.substring(0, 4));
                month = Integer.parseInt(currentValue.substring(5, 7)) - 1;
                day = Integer.parseInt(currentValue.substring(8, 10));
            }
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (picker, selectedYear, selectedMonth, dayOfMonth) ->
                            birthEditText.setText(String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, dayOfMonth)),
                    year,
                    month,
                    day
            );
            dialog.show();
        });
    }

    private void loadProfile() {
        statusTextView.setText("Loading profile...");
        authService.getUserById(sessionManager.getUserId(), new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                nameEditText.setText(user.getFullName());
                emailEditText.setText(user.getEmail());
                phoneEditText.setText(user.getPhone());
                nationalIdEditText.setText(user.getNationalId());
                birthEditText.setText(user.getDateOfBirth());
                addressEditText.setText(user.getAddress());
                statusTextView.setText("Profile ready.");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(UpdateProfileActivity.this, "Could not load profile", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Could not load profile: " + message);
            }
        });
    }

    private void saveProfile() {
        if (currentUser == null) {
            statusTextView.setText("Profile is not loaded yet.");
            return;
        }
        currentUser.setFullName(nameEditText.getText().toString().trim());
        currentUser.setPhone(phoneEditText.getText().toString().trim());
        currentUser.setNationalId(nationalIdEditText.getText().toString().trim());
        currentUser.setDateOfBirth(birthEditText.getText().toString().trim());
        currentUser.setAddress(addressEditText.getText().toString().trim());
        statusTextView.setText("Saving profile...");
        authService.updateProfile(currentUser, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                user.setAuthUserId(sessionManager.getAuthUserId());
                user.setAuthAccessToken(sessionManager.getAuthAccessToken());
                user.setAuthRefreshToken(sessionManager.getAuthRefreshToken());
                sessionManager.saveUser(user);
                currentUser = user;
                Toast.makeText(UpdateProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Profile saved.");
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(UpdateProfileActivity.this, "Could not save profile", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Could not save profile: " + message);
            }
        });
    }

    private void changePassword() {
        if (newPasswordEditText.getVisibility() != View.VISIBLE) {
            newPasswordEditText.setVisibility(View.VISIBLE);
            statusTextView.setText("Enter a new password, then tap Change Password again.");
            return;
        }
        String password = newPasswordEditText.getText().toString();
        authService.updatePassword(sessionManager.getUserId(), password, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                newPasswordEditText.setText("");
                Toast.makeText(UpdateProfileActivity.this, "Password updated", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Password changed for public profile login fallback.");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(UpdateProfileActivity.this, "Could not change password", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Could not change password: " + message);
            }
        });
    }
}
