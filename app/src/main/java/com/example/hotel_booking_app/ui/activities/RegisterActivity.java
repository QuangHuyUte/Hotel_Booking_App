package com.example.hotel_booking_app.ui.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;
import com.example.hotel_booking_app.utils.ValidationUtils;

public class RegisterActivity extends AppCompatActivity {
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText passwordEditText;
    private EditText nationalIdEditText;
    private EditText birthEditText;
    private TextView statusTextView;
    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = new AuthService();
        sessionManager = new SessionManager(this);
        nameEditText = findViewById(R.id.edit_name);
        emailEditText = findViewById(R.id.edit_email);
        phoneEditText = findViewById(R.id.edit_phone);
        passwordEditText = findViewById(R.id.edit_password);
        nationalIdEditText = findViewById(R.id.edit_national_id);
        birthEditText = findViewById(R.id.edit_birth);
        statusTextView = findViewById(R.id.text_status);
        Button createButton = findViewById(R.id.button_create_account);
        TextView backButton = findViewById(R.id.button_back_login);
        TextView closeButton = findViewById(R.id.button_close);
        TextView guestButton = findViewById(R.id.text_guest_link);

        createButton.setOnClickListener(view -> register());
        backButton.setOnClickListener(view -> finish());
        closeButton.setOnClickListener(view -> openGuestSearch());
        guestButton.setOnClickListener(view -> openGuestSearch());
        setupBirthPicker();
    }

    private void openGuestSearch() {
        startActivity(new Intent(this, CabinListActivity.class));
        finish();
    }

    private void setupBirthPicker() {
        birthEditText.setFocusable(false);
        birthEditText.setCursorVisible(false);
        birthEditText.setKeyListener(null);
        birthEditText.setOnClickListener(view -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (picker, year, month, dayOfMonth) -> birthEditText.setText(String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                    2000,
                    0,
                    1
            );
            dialog.show();
        });
    }

    private void register() {
        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String phone = phoneEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String nationalId = nationalIdEditText.getText().toString().trim();
        String birth = birthEditText.getText().toString().trim();
        String role = AppConstants.ROLE_CUSTOMER;

        if (!ValidationUtils.isNotBlank(name) || !ValidationUtils.isValidEmail(email) || !ValidationUtils.isStrongEnoughPassword(password)) {
            setStatus("Name, valid email and a 6+ character password are required.");
            return;
        }

        setStatus("Creating customer account...");
        authService.register(name, email, password, phone, role, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                user.setNationalId(nationalId);
                user.setDateOfBirth(birth.isEmpty() ? null : birth);
                authService.updateProfile(user, new SupabaseCallback<User>() {
                    @Override
                    public void onSuccess(User updatedUser) {
                        updatedUser.setAuthUserId(user.getAuthUserId());
                        updatedUser.setAuthAccessToken(user.getAuthAccessToken());
                        updatedUser.setAuthRefreshToken(user.getAuthRefreshToken());
                        sessionManager.saveUser(updatedUser);
                        startActivity(new Intent(RegisterActivity.this, PersonalActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        sessionManager.saveUser(user);
                        startActivity(new Intent(RegisterActivity.this, PersonalActivity.class));
                        finish();
                    }
                });
            }

            @Override
            public void onError(String message) {
                setStatus(message);
            }
        });
    }

    private void setStatus(String message) {
        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText(message);
    }
}
