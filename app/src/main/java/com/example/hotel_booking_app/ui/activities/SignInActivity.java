package com.example.hotel_booking_app.ui.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
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
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;
import com.example.hotel_booking_app.utils.ValidationUtils;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class SignInActivity extends AppCompatActivity {
    private static final int REQUEST_GMAIL_LOGIN = 2407;
    private EditText emailEditText;
    private EditText passwordEditText;
    private TextView statusTextView;
    private TextView customerModeButton;
    private TextView managerModeButton;
    private TextView gmailButton;
    private TextView registerButton;
    private boolean managerLoginMode;
    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        authService = new AuthService();
        sessionManager = new SessionManager(this);
        emailEditText = findViewById(R.id.edit_email);
        passwordEditText = findViewById(R.id.edit_password);
        statusTextView = findViewById(R.id.text_status);
        customerModeButton = findViewById(R.id.button_customer_mode);
        managerModeButton = findViewById(R.id.button_manager_mode);
        Button loginButton = findViewById(R.id.button_login);
        registerButton = findViewById(R.id.text_signup_link);
        TextView forgotButton = findViewById(R.id.text_forgot_password);
        TextView guestButton = findViewById(R.id.text_guest_link);
        TextView closeButton = findViewById(R.id.button_close);
        gmailButton = findViewById(R.id.button_gmail_login);

        setLoginMode(false);
        customerModeButton.setOnClickListener(view -> setLoginMode(false));
        managerModeButton.setOnClickListener(view -> setLoginMode(true));
        loginButton.setOnClickListener(view -> login());
        closeButton.setOnClickListener(view -> openGuestSearch());
        gmailButton.setOnClickListener(view -> openGmailLogin());
        registerButton.setOnClickListener(view -> startActivity(new Intent(this, SignUpActivity.class)));
        forgotButton.setOnClickListener(view -> startActivity(new Intent(this, PasswordResetActivity.class)));
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
        String callbackUrl = data.getStringExtra(OAuthBrowserActivity.EXTRA_CALLBACK_URL);
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
            setStatus("Vui lòng nhập email và mật khẩu hợp lệ.");
            return;
        }

        setStatus("Đang đăng nhập...");
        authService.login(email, password, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isExpectedLoginRole(user)) {
                    return;
                }
                completeLogin(user, welcomeMessage(user));
            }

            @Override
            public void onError(String message) {
                setStatus(message);
            }
        });
    }

    private void setLoginMode(boolean managerMode) {
        managerLoginMode = managerMode;
        customerModeButton.setBackgroundResource(managerMode ? R.drawable.bg_google_button : R.drawable.bg_button_primary);
        customerModeButton.setTextColor(getColor(managerMode ? R.color.booking_text : R.color.black));
        managerModeButton.setBackgroundResource(managerMode ? R.drawable.bg_button_primary : R.drawable.bg_google_button);
        managerModeButton.setTextColor(getColor(managerMode ? R.color.black : R.color.booking_text));
        emailEditText.setHint(managerMode ? "Email quản lý" : "Email");
        gmailButton.setVisibility(managerMode ? View.GONE : View.VISIBLE);
        registerButton.setVisibility(managerMode ? View.GONE : View.VISIBLE);
        setStatus(managerMode
                ? "Tài khoản quản lý chỉ xem và quản lý các khách sạn được phân quyền."
                : "Tài khoản khách hàng có thể tìm, đặt phòng, thanh toán và xem chuyến đi.");
    }

    private boolean isExpectedLoginRole(User user) {
        String role = user == null ? "" : user.getRole();
        boolean isManager = AppConstants.ROLE_MANAGER.equals(role);
        if (managerLoginMode && !isManager) {
            setStatus("Đây là tài khoản khách hàng. Vui lòng chọn chế độ Khách hàng.");
            return false;
        }
        if (!managerLoginMode && isManager) {
            setStatus("Đây là tài khoản quản lý. Vui lòng chọn chế độ Quản lý.");
            return false;
        }
        return true;
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
        close.setTextColor(getColor(R.color.booking_blue));
        close.setTextSize(30);
        close.setGravity(Gravity.CENTER);
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView title = new TextView(this);
        title.setText("Đăng nhập bằng Gmail");
        title.setTextColor(getColor(R.color.booking_text));
        title.setTextSize(21);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView spacer = new TextView(this);
        header.addView(spacer, new LinearLayout.LayoutParams(dp(44), dp(44)));
        panel.addView(header);

        TextView helper = new TextView(this);
        helper.setText("Nhập Gmail của bạn. Chúng tôi sẽ gửi mã đăng nhập vào hộp thư này.");
        helper.setTextColor(getColor(R.color.booking_muted));
        helper.setTextSize(13);
        helper.setGravity(Gravity.CENTER);
        helper.setPadding(0, dp(8), 0, dp(14));
        panel.addView(helper);

        TextView status = new TextView(this);
        status.setTextColor(getColor(R.color.danger));
        status.setTextSize(12);
        status.setVisibility(View.GONE);

        EditText customEmail = new EditText(this);
        customEmail.setHint("yourname@gmail.com");
        customEmail.setSingleLine(true);
        customEmail.setTextColor(getColor(R.color.booking_text));
        customEmail.setHintTextColor(getColor(R.color.booking_muted));
        customEmail.setPadding(dp(14), 0, dp(14), 0);
        customEmail.setBackgroundResource(R.drawable.bg_booking_field);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        panel.addView(customEmail, inputParams);

        EditText otpInput = new EditText(this);
        otpInput.setHint("Mã OTP 6 số");
        otpInput.setSingleLine(true);
        otpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        otpInput.setTextColor(getColor(R.color.booking_text));
        otpInput.setHintTextColor(getColor(R.color.booking_muted));
        otpInput.setPadding(dp(14), 0, dp(14), 0);
        otpInput.setBackgroundResource(R.drawable.bg_booking_field);
        otpInput.setVisibility(View.GONE);
        LinearLayout.LayoutParams otpParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        otpParams.setMargins(0, dp(12), 0, 0);
        panel.addView(otpInput, otpParams);

        Button continueButton = new Button(this);
        continueButton.setText("Tiếp tục");
        continueButton.setTextColor(getColor(R.color.booking_text));
        continueButton.setAllCaps(false);
        continueButton.setBackgroundResource(R.drawable.bg_booking_cta);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        buttonParams.setMargins(0, dp(12), 0, 0);
        panel.addView(continueButton, buttonParams);

        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, dp(10), 0, 0);
        panel.addView(status, statusParams);

        close.setOnClickListener(view -> dialog.dismiss());
        final boolean[] otpSent = {false};
        final String[] pendingEmail = {""};
        final String[] pendingName = {""};
        continueButton.setOnClickListener(view -> {
            if (!otpSent[0]) {
                String email = customEmail.getText().toString().trim();
                String name = email.contains("@") ? email.substring(0, email.indexOf("@")) : "Gmail guest";
                requestGmailOtp(name, email, customEmail, otpInput, continueButton, status, otpSent, pendingEmail, pendingName);
                return;
            }
            verifyGmailOtp(pendingName[0], pendingEmail[0], otpInput.getText().toString(), dialog, continueButton, status);
        });

        dialog.setContentView(panel);
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setDimAmount(0.72f);
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                window.setLayout(Math.min(screenWidth - dp(32), dp(360)), LinearLayout.LayoutParams.WRAP_CONTENT);
            }
        });
        dialog.show();
        customEmail.requestFocus();
    }

    private void requestGmailOtp(
            String name,
            String email,
            EditText emailInput,
            EditText otpInput,
            Button continueButton,
            TextView dialogStatus,
            boolean[] otpSent,
            String[] pendingEmail,
            String[] pendingName
    ) {
        if (email == null || !email.trim().toLowerCase().endsWith("@gmail.com")) {
            dialogStatus.setText("Vui lòng nhập địa chỉ @gmail.com hợp lệ.");
            dialogStatus.setVisibility(View.VISIBLE);
            return;
        }
        String cleanEmail = email.trim();
        String cleanName = name == null || name.trim().isEmpty()
                ? cleanEmail.substring(0, cleanEmail.indexOf("@"))
                : name.trim();

        dialogStatus.setText("Đang gửi OTP tới Gmail...");
        dialogStatus.setVisibility(View.VISIBLE);
        setStatus("Đang gửi OTP tới Gmail...");
        if (continueButton != null) {
            continueButton.setEnabled(false);
        }
        authService.requestGmailOtp(cleanEmail, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                pendingEmail[0] = cleanEmail;
                pendingName[0] = cleanName;
                otpSent[0] = true;
                if (emailInput != null) {
                    emailInput.setEnabled(false);
                }
                if (otpInput != null) {
                    otpInput.setVisibility(View.VISIBLE);
                    otpInput.requestFocus();
                }
                if (continueButton != null) {
                    continueButton.setText("Xác nhận OTP");
                    continueButton.setEnabled(true);
                }
                dialogStatus.setText("Đã gửi OTP. Kiểm tra Gmail và nhập mã 6 số.");
                setStatus("Đã gửi OTP tới " + cleanEmail + ".");
            }

            @Override
            public void onError(String message) {
                if (continueButton != null) {
                    continueButton.setEnabled(true);
                }
                dialogStatus.setText(message);
                dialogStatus.setVisibility(View.VISIBLE);
                setStatus(message);
            }
        });
    }

    private void verifyGmailOtp(String name, String email, String otp, Dialog dialog, Button continueButton, TextView dialogStatus) {
        dialogStatus.setText("Đang xác nhận OTP...");
        dialogStatus.setVisibility(View.VISIBLE);
        setStatus("Đang xác nhận OTP Gmail...");
        continueButton.setEnabled(false);
        authService.verifyGmailOtp(name, email, otp, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                dialog.dismiss();
                if (!isExpectedLoginRole(user)) {
                    return;
                }
                completeLogin(user, welcomeMessage(user));
            }

            @Override
            public void onError(String message) {
                continueButton.setEnabled(true);
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
        if (getIntent().getBooleanExtra("returnToBooking", false)) {
            finish();
            return;
        }
        startActivity(new Intent(this, HotelSearchActivity.class));
        finish();
    }

    private void completeLogin(User user, String toastMessage) {
        sessionManager.saveUser(user);
        Toast.makeText(SignInActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
        if (getIntent().getBooleanExtra("returnToBooking", false)) {
            Intent intent = new Intent(SignInActivity.this, BookingCreateActivity.class);
            intent.putExtra(AppConstants.EXTRA_CABIN_ID, getIntent().getStringExtra(AppConstants.EXTRA_CABIN_ID));
            intent.putExtra("checkIn", getIntent().getStringExtra("checkIn"));
            intent.putExtra("checkOut", getIntent().getStringExtra("checkOut"));
            startActivity(intent);
            finish();
            return;
        }
        startActivity(new Intent(SignInActivity.this, AccountHubActivity.class));
        finish();
    }

    private String welcomeMessage(User user) {
        String name = user == null ? "" : user.getFullName();
        if (name == null || name.trim().isEmpty()) {
            name = user == null ? "" : user.getEmail();
        }
        if (name == null || name.trim().isEmpty()) {
            name = "bạn";
        }
        return "Xin chào, " + name.trim();
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
            setStatus("Google đã trả về mã xác thực. Vui lòng kiểm tra lại cấu hình đăng nhập Gmail trong Supabase.");
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
            setStatus("Đang hoàn tất đăng nhập Gmail...");
            authService.loginWithOAuthSession(name, email, subject, accessToken, refreshToken, new SupabaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (!isExpectedLoginRole(user)) {
                        return;
                    }
                    completeLogin(user, welcomeMessage(user));
                }

                @Override
                public void onError(String message) {
                    setStatus(message);
                }
            });
        } catch (Exception e) {
            setStatus("Không thể hoàn tất đăng nhập Gmail: " + e.getMessage());
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
