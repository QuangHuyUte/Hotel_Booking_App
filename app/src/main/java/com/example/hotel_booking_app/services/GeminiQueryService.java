package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.BuildConfig;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiQueryService {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Set<String> DESTINATIONS = new HashSet<>(Arrays.asList(
            "Ho Chi Minh City", "Vung Tau", "Hanoi", "Da Nang", "Da Lat"
    ));
    private static final Set<String> ROOM_QUERIES = new HashSet<>(Arrays.asList(
            "Standard", "Superior", "Deluxe", "Suite"
    ));
    private static final Set<String> AMENITIES = new HashSet<>(Arrays.asList(
            "WiFi", "Breakfast", "Parking", "Pool", "Sea view", "Balcony", "Air conditioning", "Private bathroom"
    ));

    private final Gson gson = new Gson();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(16, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    public void refineQuery(String userMessage, AiAssistantService.AiSearchQuery fallback, SupabaseCallback<AiAssistantService.AiSearchQuery> callback) {
        String apiKey = BuildConfig.GEMINI_API_KEY == null ? "" : BuildConfig.GEMINI_API_KEY.trim();
        String model = BuildConfig.GEMINI_MODEL == null || BuildConfig.GEMINI_MODEL.trim().isEmpty()
                ? "gemini-3.1-flash-lite"
                : BuildConfig.GEMINI_MODEL.trim();
        if (apiKey.isEmpty()) {
            callback.onSuccess(fallback);
            return;
        }

        HttpUrl url = HttpUrl.parse("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                .newBuilder()
                .addQueryParameter("key", apiKey)
                .build();

        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", buildPrompt(userMessage));
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        payload.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.05);
        generationConfig.addProperty("maxOutputTokens", 512);
        generationConfig.addProperty("responseMimeType", "application/json");
        payload.add("generationConfig", generationConfig);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onSuccess(fallback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    callback.onSuccess(fallback);
                    return;
                }
                try {
                    callback.onSuccess(mergeWithFallback(fallback, extractJsonText(body)));
                } catch (Exception ignored) {
                    callback.onSuccess(fallback);
                }
            }
        });
    }

    private String buildPrompt(String userMessage) {
        return "You extract hotel search filters from a Vietnamese or English user message. "
                + "Return ONLY JSON. Do not recommend hotels. Do not invent unavailable destinations. "
                + "Allowed destinations: Ho Chi Minh City, Vung Tau, Hanoi, Da Nang, Da Lat. "
                + "Allowed roomQuery: Standard, Superior, Deluxe, Suite, or empty string. "
                + "Allowed amenities: WiFi, Breakfast, Parking, Pool, Sea view, Balcony, Air conditioning, Private bathroom. "
                + "Use today's date " + LocalDate.now() + " to resolve relative dates. "
                + "Schema: {\"destination\":\"\",\"adults\":2,\"rooms\":1,\"requestedBeds\":0,\"maxPricePerNight\":0,\"roomQuery\":\"\",\"amenities\":[],\"checkIn\":\"YYYY-MM-DD or empty\",\"checkOut\":\"YYYY-MM-DD or empty\"}. "
                + "User message: " + userMessage;
    }

    private String extractJsonText(String body) {
        JsonObject root = gson.fromJson(body, JsonObject.class);
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.size() == 0) {
            return "{}";
        }
        JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
        JsonArray parts = content == null ? null : content.getAsJsonArray("parts");
        if (parts == null || parts.size() == 0) {
            return "{}";
        }
        String text = stringValue(parts.get(0).getAsJsonObject(), "text", "{}");
        return text.replace("```json", "").replace("```", "").trim();
    }

    private AiAssistantService.AiSearchQuery mergeWithFallback(AiAssistantService.AiSearchQuery fallback, String jsonText) {
        JsonObject json = gson.fromJson(jsonText, JsonObject.class);
        AiAssistantService.AiSearchQuery query = fallback.copy();

        String destination = stringValue(json, "destination", "").trim();
        if (DESTINATIONS.contains(destination)) {
            query.setDestination(destination);
        }

        query.setAdults(clamp(intValue(json, "adults", query.getAdults()), 1, 8));
        query.setRooms(clamp(intValue(json, "rooms", query.getRooms()), 1, 4));
        query.setRequestedBeds(clamp(intValue(json, "requestedBeds", query.getRequestedBeds()), 0, 8));
        query.setMaxPricePerNight(Math.max(0, doubleValue(json, "maxPricePerNight", query.getMaxPricePerNight())));

        String roomQuery = stringValue(json, "roomQuery", "").trim();
        if (ROOM_QUERIES.contains(roomQuery)) {
            query.setRoomQuery(roomQuery);
        }

        List<String> amenities = new ArrayList<>();
        JsonArray rawAmenities = json.getAsJsonArray("amenities");
        if (rawAmenities != null) {
            for (JsonElement element : rawAmenities) {
                String amenity = element.getAsString();
                if (AMENITIES.contains(amenity) && !amenities.contains(amenity)) {
                    amenities.add(amenity);
                }
            }
        }
        if (!amenities.isEmpty()) {
            query.setAmenities(amenities);
        }

        String checkIn = stringValue(json, "checkIn", "");
        String checkOut = stringValue(json, "checkOut", "");
        if (isValidDateRange(checkIn, checkOut)) {
            query.setCheckIn(checkIn);
            query.setCheckOut(checkOut);
        }
        return query;
    }

    private boolean isValidDateRange(String checkIn, String checkOut) {
        try {
            LocalDate start = LocalDate.parse(checkIn);
            LocalDate end = LocalDate.parse(checkOut);
            return end.isAfter(start);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String stringValue(JsonObject json, String key, String fallback) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return fallback;
        }
        return json.get(key).getAsString();
    }

    private int intValue(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double doubleValue(JsonObject json, String key, double fallback) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
