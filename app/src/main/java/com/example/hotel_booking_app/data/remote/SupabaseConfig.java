package com.example.hotel_booking_app.data.remote;

import com.example.hotel_booking_app.BuildConfig;

public final class SupabaseConfig {
    public static final String BASE_URL = BuildConfig.SUPABASE_URL;
    public static final String ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private static final String PLACEHOLDER_KEY = "PASTE_YOUR_SUPABASE_ANON_KEY_HERE";

    private SupabaseConfig() {
    }

    public static boolean hasValidAnonKey() {
        return ANON_KEY != null && !ANON_KEY.trim().isEmpty() && !PLACEHOLDER_KEY.equals(ANON_KEY);
    }
}
