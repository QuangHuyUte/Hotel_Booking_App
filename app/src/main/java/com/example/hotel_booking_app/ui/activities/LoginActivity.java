package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.hotel_booking_app.utils.ValidationUtils;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText;
    private EditText passwordEditText;
    private TextView statusTextView;
    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = new AuthService();
        sessionManager = new SessionManager(this);
        emailEditText = findViewById(R.id.edit_email);
        passwordEditText = findViewById(R.id.edit_password);
        statusTextView = findViewById(R.id.text_status);
        Button loginButton = findViewById(R.id.button_login);
        TextView registerButton = findViewById(R.id.text_signup_link);
        TextView forgotButton = findViewById(R.id.text_forgot_password);
        TextView guestButton = findViewById(R.id.text_guest_link);

        loginButton.setOnClickListener(view -> login());
        registerButton.setOnClickListener(view -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotButton.setOnClickListener(view -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        guestButton.setOnClickListener(view -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    private void login() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        if (!ValidationUtils.isValidEmail(email) || !ValidationUtils.isNotBlank(password)) {
            statusTextView.setText("Please enter a valid email and password.");
            return;
        }

        statusTextView.setText("Logging in...");
        authService.login(email, password, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                sessionManager.saveUser(user);
                Toast.makeText(LoginActivity.this, "Welcome " + user.getFullName(), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, PersonalActivity.class));
                finish();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }
}
