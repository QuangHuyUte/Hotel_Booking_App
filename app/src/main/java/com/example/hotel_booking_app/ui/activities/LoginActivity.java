package com.example.hotel_booking_app.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseConfig;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.utils.SessionManager;
import com.example.hotel_booking_app.utils.ValidationUtils;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {
    private static final int REQUEST_GMAIL_LOGIN = 2407;
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
        TextView closeButton = findViewById(R.id.button_close);
        TextView gmailButton = findViewById(R.id.button_gmail_login);

        loginButton.setOnClickListener(view -> login());
        closeButton.setOnClickListener(view -> openGuestSearch());
        gmailButton.setOnClickListener(view -> openGmailLogin());
        registerButton.setOnClickListener(view -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotButton.setOnClickListener(view -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        guestButton.setOnClickListener(view -> openGuestSearch());
        handleOAuthRedirect(getIntent() == null ? null : getIntent().getData());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOAuthRedirect(intent == null ? null : intent.getData());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_GMAIL_LOGIN) {
            return;
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        String callbackUrl = data.getStringExtra(OAuthWebActivity.EXTRA_CALLBACK_URL);
        if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
            handleOAuthRedirect(data.getData());
            return;
        }
        handleOAuthRedirect(Uri.parse(callbackUrl));
    }

    private void login() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        if (!ValidationUtils.isValidEmail(email) || !ValidationUtils.isNotBlank(password)) {
            setStatus("Please enter a valid email and password.");
            return;
        }

        setStatus("Logging in...");
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
                setStatus(message);
            }
        });
    }

    private void openGmailLogin() {
        if (!SupabaseConfig.hasValidAnonKey()) {
            setStatus("Add SUPABASE_ANON_KEY and enable Google provider in Supabase Auth before using Gmail login.");
            return;
        }

        Uri uri = Uri.parse(SupabaseConfig.BASE_URL + "/auth/v1/authorize")
                .buildUpon()
                .appendQueryParameter("provider", "google")
                .appendQueryParameter("redirect_to", "hotelbookingapp://auth/callback")
                .build();
        Intent intent = new Intent(this, OAuthWebActivity.class);
        intent.putExtra(OAuthWebActivity.EXTRA_AUTH_URL, uri.toString());
        startActivityForResult(intent, REQUEST_GMAIL_LOGIN);
        setStatus("Open Gmail sign in inside the app.");
    }

    private void openGuestSearch() {
        startActivity(new Intent(this, CabinListActivity.class));
        finish();
    }

    private void handleOAuthRedirect(Uri data) {
        if (data == null || !"hotelbookingapp".equals(data.getScheme())) {
            return;
        }

        String error = getCallbackValue(data, "error_description");
        if (error == null) {
            error = getCallbackValue(data, "error");
        }
        if (error != null) {
            setStatus(error);
            return;
        }

        String accessToken = getCallbackValue(data, "access_token");
        String refreshToken = getCallbackValue(data, "refresh_token");
        String code = getCallbackValue(data, "code");
        if (accessToken == null && code != null) {
            setStatus("Google returned an auth code. In Supabase Auth, use an implicit/mobile redirect flow or add code exchange before using Gmail login.");
            return;
        }
        if (accessToken == null) {
            return;
        }

        try {
            JSONObject claims = decodeJwtClaims(accessToken);
            String email = claims.optString("email", "");
            String name = claims.optString("name", email);
            String subject = claims.optString("sub", "");
            setStatus("Finishing Gmail login...");
            authService.loginWithOAuthSession(name, email, subject, accessToken, refreshToken, new SupabaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    sessionManager.saveUser(user);
                    Toast.makeText(LoginActivity.this, "Welcome " + user.getFullName(), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, PersonalActivity.class));
                    finish();
                }

                @Override
                public void onError(String message) {
                    setStatus(message);
                }
            });
        } catch (Exception e) {
            setStatus("Could not finish Gmail login: " + e.getMessage());
        }
    }

    private JSONObject decodeJwtClaims(String accessToken) throws Exception {
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("invalid access token");
        }
        byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return new JSONObject(new String(decoded, StandardCharsets.UTF_8));
    }

    private String getCallbackValue(Uri uri, String key) {
        String value = uri.getQueryParameter(key);
        if (value != null) {
            return value;
        }
        String fragment = uri.getFragment();
        if (fragment == null) {
            return null;
        }
        for (String part : fragment.split("&")) {
            String[] pieces = part.split("=", 2);
            if (pieces.length == 2 && key.equals(pieces[0])) {
                return Uri.decode(pieces[1]);
            }
        }
        return null;
    }

    private void setStatus(String message) {
        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText(message);
    }
}
