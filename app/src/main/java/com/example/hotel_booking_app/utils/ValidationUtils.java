package com.example.hotel_booking_app.utils;

import android.text.TextUtils;
import android.util.Patterns;

public final class ValidationUtils {
    private ValidationUtils() {
    }

    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isStrongEnoughPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
