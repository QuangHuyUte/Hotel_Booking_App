package com.example.hotel_booking_app.data.remote;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseAuthClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    public void signInWithPassword(String email, String password, SupabaseCallback<SupabaseAuthSession> callback) {
        HttpUrl url = HttpUrl.parse(SupabaseConfig.BASE_URL + "/auth/v1/token")
                .newBuilder()
                .addQueryParameter("grant_type", "password")
                .build();

        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        executeAuthRequest(url, payload, callback);
    }

    public void signUp(String email, String password, SupabaseCallback<SupabaseAuthSession> callback) {
        HttpUrl url = HttpUrl.parse(SupabaseConfig.BASE_URL + "/auth/v1/signup").newBuilder().build();

        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        executeAuthRequest(url, payload, callback);
    }

    public void sendEmailOtp(String email, SupabaseCallback<Boolean> callback) {
        HttpUrl url = HttpUrl.parse(SupabaseConfig.BASE_URL + "/auth/v1/otp").newBuilder().build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email.trim());
        payload.put("create_user", true);

        Request request = buildAuthRequest(url, payload);
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    postError(callback, buildFriendlyAuthError(response.code(), body));
                    return;
                }
                postSuccess(callback, true);
            }
        });
    }

    public void verifyEmailOtp(String email, String otp, SupabaseCallback<SupabaseAuthSession> callback) {
        HttpUrl url = HttpUrl.parse(SupabaseConfig.BASE_URL + "/auth/v1/verify").newBuilder().build();

        Map<String, String> payload = new HashMap<>();
        payload.put("email", email.trim());
        payload.put("token", otp.trim());
        payload.put("type", "email");
        executeAuthRequest(url, payload, callback);
    }

    public void recoverPassword(String email, SupabaseCallback<Boolean> callback) {
        HttpUrl url = HttpUrl.parse(SupabaseConfig.BASE_URL + "/auth/v1/recover").newBuilder().build();

        Map<String, String> payload = new HashMap<>();
        payload.put("email", email.trim());
        Request request = buildAuthRequest(url, payload);
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    postError(callback, buildFriendlyAuthError(response.code(), body));
                    return;
                }
                postSuccess(callback, true);
            }
        });
    }

    public SupabaseAuthSession refreshSessionBlocking(String refreshToken) throws IOException {
        HttpUrl url = HttpUrl.parse(SupabaseConfig.BASE_URL + "/auth/v1/token")
                .newBuilder()
                .addQueryParameter("grant_type", "refresh_token")
                .build();

        Map<String, String> payload = new HashMap<>();
        payload.put("refresh_token", refreshToken);

        Request request = buildAuthRequest(url, payload);
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException(buildFriendlyAuthError(response.code(), body));
            }
            return parseAuthSession(body);
        }
    }

    private void executeAuthRequest(HttpUrl url, Object payload, SupabaseCallback<SupabaseAuthSession> callback) {
        Request request = buildAuthRequest(url, payload);

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    postError(callback, buildFriendlyAuthError(response.code(), body));
                    return;
                }

                try {
                    postSuccess(callback, parseAuthSession(body));
                } catch (Exception e) {
                    postError(callback, "Không đọc được phản hồi đăng nhập từ Supabase: " + e.getMessage());
                }
            }
        });
    }

    private Request buildAuthRequest(HttpUrl url, Object payload) {
        return new Request.Builder()
                .url(url)
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.ANON_KEY)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();
    }

    private SupabaseAuthSession parseAuthSession(String body) {
        JsonObject json = gson.fromJson(body, JsonObject.class);
        String accessToken = getString(json, "access_token");
        String refreshToken = getString(json, "refresh_token");
        JsonObject user = json.has("user") && json.get("user").isJsonObject()
                ? json.getAsJsonObject("user")
                : null;
        String userId = user != null ? getString(user, "id") : null;
        String email = user != null ? getString(user, "email") : null;

        if (userId == null) {
            throw new IllegalStateException("Supabase Auth không trả về user.id.");
        }

        return new SupabaseAuthSession(userId, email, accessToken, refreshToken);
    }

    private String getString(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return json.get(key).getAsString();
    }

    private String buildFriendlyAuthError(int statusCode, String body) {
        String lower = body == null ? "" : body.toLowerCase();
        if (statusCode == 429 || lower.contains("over_email_send_rate_limit") || lower.contains("email rate limit")) {
            return "Supabase Auth đang giới hạn số email xác nhận. Hãy tắt email confirmation trong Supabase Auth Settings cho demo hoặc chờ vài phút rồi thử lại.";
        }
        if (lower.contains("user_already_exists") || lower.contains("already registered") || lower.contains("already exists")) {
            return "Email này đã tồn tại trong Supabase Auth. Hãy đăng nhập lại.";
        }
        if (lower.contains("invalid_credentials") || lower.contains("invalid login credentials")) {
            return "Email hoặc mật khẩu Supabase Auth không đúng.";
        }
        if (lower.contains("otp") || lower.contains("token")) {
            return "OTP không đúng hoặc đã hết hạn. Vui lòng kiểm tra Gmail rồi thử lại.";
        }
        return "Lỗi Supabase Auth " + statusCode + ": " + body;
    }

    private <T> void postSuccess(SupabaseCallback<T> callback, T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private void postError(SupabaseCallback<?> callback, String message) {
        mainHandler.post(() -> callback.onError(friendlyNetworkError(message)));
    }

    private String friendlyNetworkError(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Không thể kết nối Supabase Auth. Vui lòng kiểm tra internet và Supabase URL.";
        }
        String lower = message.toLowerCase();
        if (lower.contains("unable to resolve host") || lower.contains("no address associated with hostname")) {
            return "Không thể truy cập máy chủ Supabase Auth. Hãy kiểm tra internet giả lập, DNS và SUPABASE_URL trong gradle.properties.";
        }
        return message;
    }
}
