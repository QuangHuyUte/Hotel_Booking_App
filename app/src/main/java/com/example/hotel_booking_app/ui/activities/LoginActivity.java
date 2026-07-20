package com.example.hotel_booking_app.ui.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
        showGmailAccountDialog();
    }

    private void showGmailAccountDialog() {
        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(18));
        panel.setBackgroundResource(R.drawable.bg_calendar_dialog);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView close = new TextView(this);
        close.setText("X");
        close.setTextColor(Color.parseColor("#4DA3FF"));
        close.setTextSize(30);
        close.setGravity(Gravity.CENTER);
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView title = new TextView(this);
        title.setText("Dang nhap bang Gmail");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView spacer = new TextView(this);
        header.addView(spacer, new LinearLayout.LayoutParams(dp(44), dp(44)));
        panel.addView(header);

        TextView helper = new TextView(this);
        helper.setText("Chon tai khoan Gmail de dang nhap hoac tao tai khoan moi trong app.");
        helper.setTextColor(Color.parseColor("#D8DEE9"));
        helper.setTextSize(13);
        helper.setPadding(0, dp(8), 0, dp(12));
        panel.addView(helper);

        TextView status = new TextView(this);
        status.setTextColor(Color.parseColor("#F5C542"));
        status.setTextSize(12);
        status.setVisibility(View.GONE);

        panel.addView(gmailAccountRow("Alice Booking", "alice.booking@gmail.com", dialog, status));
        panel.addView(gmailAccountRow("Bao Travel", "bao.travel@gmail.com", dialog, status));
        panel.addView(gmailAccountRow("New Google account", "serein.guest@gmail.com", dialog, status));

        EditText customEmail = new EditText(this);
        customEmail.setHint("nhap gmail khac");
        customEmail.setSingleLine(true);
        customEmail.setTextColor(Color.WHITE);
        customEmail.setHintTextColor(Color.parseColor("#9CA3AF"));
        customEmail.setPadding(dp(14), 0, dp(14), 0);
        customEmail.setBackgroundResource(R.drawable.bg_input);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        inputParams.setMargins(0, dp(12), 0, 0);
        panel.addView(customEmail, inputParams);

        Button continueButton = new Button(this);
        continueButton.setText("Tiep tuc");
        continueButton.setTextColor(Color.parseColor("#052F5F"));
        continueButton.setAllCaps(false);
        continueButton.setBackgroundResource(R.drawable.bg_button_secondary);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        buttonParams.setMargins(0, dp(10), 0, 0);
        panel.addView(continueButton, buttonParams);

        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, dp(10), 0, 0);
        panel.addView(status, statusParams);

        close.setOnClickListener(view -> dialog.dismiss());
        continueButton.setOnClickListener(view -> {
            String email = customEmail.getText().toString().trim();
            String name = email.contains("@") ? email.substring(0, email.indexOf("@")) : "Gmail guest";
            continueWithGmailAccount(name, email, dialog, status);
        });

        dialog.setContentView(panel);
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setDimAmount(0.72f);
                window.setLayout(dp(332), LinearLayout.LayoutParams.WRAP_CONTENT);
            }
        });
        dialog.show();
    }

    private View gmailAccountRow(String name, String email, Dialog dialog, TextView status) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setBackgroundResource(R.drawable.bg_profile_dark_row);

        TextView avatar = new TextView(this);
        avatar.setText(name.substring(0, 1));
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(18);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_icon_warm_circle);
        row.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView label = new TextView(this);
        label.setText(name + "\n" + email);
        label.setTextColor(Color.WHITE);
        label.setTextSize(14);
        label.setPadding(dp(12), 0, 0, 0);
        row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(Color.parseColor("#D8B684"));
        arrow.setTextSize(24);
        row.addView(arrow);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);
        row.setOnClickListener(view -> continueWithGmailAccount(name, email, dialog, status));
        return row;
    }

    private void continueWithGmailAccount(String name, String email, Dialog dialog, TextView dialogStatus) {
        if (email == null || !email.trim().toLowerCase().endsWith("@gmail.com")) {
            dialogStatus.setText("Hay chon hoac nhap dung dia chi @gmail.com.");
            dialogStatus.setVisibility(View.VISIBLE);
            return;
        }
        dialogStatus.setText("Dang kiem tra tai khoan...");
        dialogStatus.setVisibility(View.VISIBLE);
        setStatus("Dang dang nhap bang Gmail...");
        authService.loginOrCreateGoogleAccount(name, email, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                sessionManager.saveUser(user);
                dialog.dismiss();
                Toast.makeText(LoginActivity.this, "Da dang nhap bang Gmail", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, PersonalActivity.class));
                finish();
            }

            @Override
            public void onError(String message) {
                dialogStatus.setText(message);
                dialogStatus.setVisibility(View.VISIBLE);
                setStatus(message);
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
