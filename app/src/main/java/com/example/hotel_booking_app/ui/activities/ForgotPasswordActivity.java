package com.example.hotel_booking_app.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.utils.ValidationUtils;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText emailEditText;
    private TextView statusTextView;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authService = new AuthService();
        emailEditText = findViewById(R.id.edit_email);
        statusTextView = findViewById(R.id.text_status);
        Button continueButton = findViewById(R.id.button_continue);
        TextView backLoginTextView = findViewById(R.id.text_back_login);

        continueButton.setOnClickListener(view -> sendResetEmail());
        backLoginTextView.setOnClickListener(view -> finish());
    }

    private void sendResetEmail() {
        String email = emailEditText.getText().toString().trim();
        if (!ValidationUtils.isValidEmail(email)) {
            setStatus("Please enter a valid email.");
            return;
        }
        setStatus("Sending reset email...");
        authService.sendPasswordResetEmail(email, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                Toast.makeText(ForgotPasswordActivity.this, "Reset email sent", Toast.LENGTH_SHORT).show();
                setStatus("Password reset email sent. Please check your inbox or spam folder.");
            }

            @Override
            public void onError(String message) {
                setStatus("Could not send reset email: " + message);
            }
        });
    }

    private void setStatus(String message) {
        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText(message);
    }
}
