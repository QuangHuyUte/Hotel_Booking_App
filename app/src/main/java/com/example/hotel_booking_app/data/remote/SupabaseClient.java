package com.example.hotel_booking_app.data.remote;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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

public class SupabaseClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static SupabaseClient instance;

    public interface TokenRefresher {
        String refreshAccessToken() throws IOException;
    }

    private final Gson gson;
    private final Handler mainHandler;
    private final OkHttpClient httpClient;
    private String accessToken;
    private TokenRefresher tokenRefresher;

    private SupabaseClient() {
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setTokenRefresher(TokenRefresher tokenRefresher) {
        this.tokenRefresher = tokenRefresher;
    }

    public <T> void getList(String tableName, Class<T[]> responseType, SupabaseCallback<List<T>> callback) {
        getList(tableName, "*", null, responseType, callback);
    }

    public <T> void getList(
            String tableName,
            String select,
            Integer limit,
            Class<T[]> responseType,
            SupabaseCallback<List<T>> callback
    ) {
        getList(tableName, select, limit, null, null, responseType, callback);
    }

    public <T> void getList(
            String tableName,
            String select,
            Integer limit,
            String order,
            Map<String, String> filters,
            Class<T[]> responseType,
            SupabaseCallback<List<T>> callback
    ) {
        if (!SupabaseConfig.hasValidAnonKey()) {
            callback.onError("Bạn cần dán SUPABASE_ANON_KEY trong gradle.properties trước khi gọi Supabase.");
            return;
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(SupabaseConfig.BASE_URL + "/rest/v1/" + tableName).newBuilder()
                .addQueryParameter("select", select);
        if (limit != null) {
            urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        }
        if (order != null) {
            urlBuilder.addQueryParameter("order", order);
        }
        if (filters != null) {
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), "eq." + entry.getValue());
            }
        }

        executeList(baseRequest(urlBuilder.build()).get().build(), responseType, callback, true);
    }

    public <T> void getListIn(
            String tableName,
            String select,
            String order,
            String column,
            Collection<String> values,
            Class<T[]> responseType,
            SupabaseCallback<List<T>> callback
    ) {
        LinkedHashSet<String> cleanValues = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    cleanValues.add(value.trim());
                }
            }
        }
        if (cleanValues.isEmpty()) {
            callback.onSuccess(Collections.emptyList());
            return;
        }
        if (!SupabaseConfig.hasValidAnonKey()) {
            callback.onError("Bạn cần dán SUPABASE_ANON_KEY trong gradle.properties trước khi gọi Supabase.");
            return;
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(SupabaseConfig.BASE_URL + "/rest/v1/" + tableName).newBuilder()
                .addQueryParameter("select", select)
                .addQueryParameter(column, "in.(" + String.join(",", cleanValues) + ")");
        if (order != null) {
            urlBuilder.addQueryParameter("order", order);
        }

        executeList(baseRequest(urlBuilder.build()).get().build(), responseType, callback, true);
    }

    private <T> void executeList(
            Request request,
            Class<T[]> responseType,
            SupabaseCallback<List<T>> callback,
            boolean canRefresh
    ) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (canRefresh && shouldRefreshToken(response.code(), body)) {
                        Request retryRequest = refreshRequestToken(request, callback);
                        if (retryRequest != null) {
                            executeList(retryRequest, responseType, callback, false);
                        }
                    } else {
                        postError(callback, buildRestError(response.code(), body));
                    }
                    return;
                }

                T[] items = gson.fromJson(body, responseType);
                postSuccess(callback, Arrays.asList(items));
            }
        });
    }

    public <T> void getSingle(
            String tableName,
            Map<String, String> filters,
            Class<T[]> responseType,
            SupabaseCallback<T> callback
    ) {
        getList(tableName, "*", 1, null, filters, responseType, new SupabaseCallback<List<T>>() {
            @Override
            public void onSuccess(List<T> data) {
                if (data.isEmpty()) {
                    callback.onError("Không tìm thấy dữ liệu phù hợp.");
                } else {
                    callback.onSuccess(data.get(0));
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public <T> void insert(String tableName, Object payload, Class<T[]> responseType, SupabaseCallback<T> callback) {
        if (!SupabaseConfig.hasValidAnonKey()) {
            callback.onError("Bạn cần dán SUPABASE_ANON_KEY trong gradle.properties trước khi gọi Supabase.");
            return;
        }

        HttpUrl url = HttpUrl.parse(SupabaseConfig.BASE_URL + "/rest/v1/" + tableName).newBuilder().build();
        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        Request request = baseRequest(url)
                .post(body)
                .addHeader("Prefer", "return=representation")
                .build();
        executeSingle(request, responseType, callback);
    }

    public void insertNoReturn(String tableName, Object payload, SupabaseCallback<Boolean> callback) {
        if (!SupabaseConfig.hasValidAnonKey()) {
            callback.onError("Bạn cần dán SUPABASE_ANON_KEY trong gradle.properties trước khi gọi Supabase.");
            return;
        }

        HttpUrl url = HttpUrl.parse(SupabaseConfig.BASE_URL + "/rest/v1/" + tableName).newBuilder().build();
        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        Request request = baseRequest(url)
                .post(body)
                .addHeader("Prefer", "return=minimal")
                .build();
        executeSuccessOnly(request, callback, true);
    }

    public <T> void update(
            String tableName,
            Map<String, String> filters,
            Object payload,
            Class<T[]> responseType,
            SupabaseCallback<T> callback
    ) {
        if (!SupabaseConfig.hasValidAnonKey()) {
            callback.onError("Bạn cần dán SUPABASE_ANON_KEY trong gradle.properties trước khi gọi Supabase.");
            return;
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(SupabaseConfig.BASE_URL + "/rest/v1/" + tableName).newBuilder();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), "eq." + entry.getValue());
        }

        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        Request request = baseRequest(urlBuilder.build())
                .patch(body)
                .addHeader("Prefer", "return=representation")
                .build();
        executeSingle(request, responseType, callback);
    }

    public void updateNoReturn(
            String tableName,
            Map<String, String> filters,
            Object payload,
            SupabaseCallback<Boolean> callback
    ) {
        if (!SupabaseConfig.hasValidAnonKey()) {
            callback.onError("Bạn cần dán SUPABASE_ANON_KEY trong gradle.properties trước khi gọi Supabase.");
            return;
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(SupabaseConfig.BASE_URL + "/rest/v1/" + tableName).newBuilder();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), "eq." + entry.getValue());
        }

        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        Request request = baseRequest(urlBuilder.build())
                .patch(body)
                .addHeader("Prefer", "return=minimal")
                .build();
        executeSuccessOnly(request, callback, true);
    }

    public void delete(String tableName, Map<String, String> filters, SupabaseCallback<Boolean> callback) {
        if (!SupabaseConfig.hasValidAnonKey()) {
            callback.onError("Bạn cần dán SUPABASE_ANON_KEY trong gradle.properties trước khi gọi Supabase.");
            return;
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(SupabaseConfig.BASE_URL + "/rest/v1/" + tableName).newBuilder();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), "eq." + entry.getValue());
        }

        Request request = baseRequest(urlBuilder.build())
                .delete()
                .addHeader("Prefer", "return=representation")
                .build();
        executeDelete(request, callback, true);
    }

    private void executeDelete(Request request, SupabaseCallback<Boolean> callback, boolean canRefresh) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (canRefresh && shouldRefreshToken(response.code(), body)) {
                        Request retryRequest = refreshRequestToken(request, callback);
                        if (retryRequest != null) {
                            executeDelete(retryRequest, callback, false);
                        }
                    } else {
                        postError(callback, buildRestError(response.code(), body));
                    }
                    return;
                }
                if ("[]".equals(body.trim())) {
                    postError(callback, "Không có dòng nào được xóa. Hãy kiểm tra quyền sở hữu hoặc chính sách RLS của Supabase.");
                    return;
                }
                postSuccess(callback, true);
            }
        });
    }

    private Request.Builder baseRequest(HttpUrl url) {
        String bearerToken = accessToken != null && !accessToken.trim().isEmpty()
                ? accessToken
                : SupabaseConfig.ANON_KEY;
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer " + bearerToken)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json");
    }

    private <T> void executeSingle(Request request, Class<T[]> responseType, SupabaseCallback<T> callback) {
        executeSingle(request, responseType, callback, true);
    }

    private <T> void executeSingle(
            Request request,
            Class<T[]> responseType,
            SupabaseCallback<T> callback,
            boolean canRefresh
    ) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (canRefresh && shouldRefreshToken(response.code(), body)) {
                        Request retryRequest = refreshRequestToken(request, callback);
                        if (retryRequest != null) {
                            executeSingle(retryRequest, responseType, callback, false);
                        }
                    } else {
                        postError(callback, buildRestError(response.code(), body));
                    }
                    return;
                }

                T[] items = gson.fromJson(body, responseType);
                if (items == null || items.length == 0) {
                    postError(callback, "Supabase không trả dữ liệu sau thao tác.");
                } else {
                    postSuccess(callback, items[0]);
                }
            }
        });
    }

    private void executeSuccessOnly(Request request, SupabaseCallback<Boolean> callback, boolean canRefresh) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (canRefresh && shouldRefreshToken(response.code(), body)) {
                        Request retryRequest = refreshRequestToken(request, callback);
                        if (retryRequest != null) {
                            executeSuccessOnly(retryRequest, callback, false);
                        }
                    } else {
                        postError(callback, buildRestError(response.code(), body));
                    }
                    return;
                }
                postSuccess(callback, true);
            }
        });
    }

    private boolean shouldRefreshToken(int statusCode, String body) {
        String lower = body == null ? "" : body.toLowerCase();
        return statusCode == 401 && (lower.contains("jwt expired") || lower.contains("pgrst303"));
    }

    private Request refreshRequestToken(Request request, SupabaseCallback<?> callback) {
        if (tokenRefresher == null) {
            postError(callback, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return null;
        }

        try {
            String newToken = tokenRefresher.refreshAccessToken();
            if (newToken == null || newToken.trim().isEmpty()) {
                postError(callback, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
                return null;
            }
            accessToken = newToken;
            return request.newBuilder()
                    .header("Authorization", "Bearer " + newToken)
                    .build();
        } catch (IOException e) {
            postError(callback, e.getMessage());
            return null;
        }
    }

    private String buildRestError(int statusCode, String body) {
        if (shouldRefreshToken(statusCode, body)) {
            return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
        }
        String lowerBody = body == null ? "" : body.toLowerCase();
        if (lowerBody.contains("row-level security") || lowerBody.contains("\"code\":\"42501\"")) {
            return "Database chưa mở policy RLS cho thao tác demo này. Hãy chạy lại supabase/database.sql hoặc supabase/seed.sql để đồng bộ quyền bookings/payments.";
        }
        return "Lỗi Supabase " + statusCode + ": " + body;
    }

    private <T> void postSuccess(SupabaseCallback<T> callback, T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private void postError(SupabaseCallback<?> callback, String message) {
        mainHandler.post(() -> callback.onError(friendlyNetworkError(message)));
    }

    private String friendlyNetworkError(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Không thể kết nối Supabase. Vui lòng kiểm tra internet và Supabase URL.";
        }
        String lower = message.toLowerCase();
        if (lower.contains("unable to resolve host") || lower.contains("no address associated with hostname")) {
            return "Không thể truy cập máy chủ Supabase. Hãy kiểm tra internet giả lập, DNS và SUPABASE_URL trong gradle.properties.";
        }
        return message;
    }
}
