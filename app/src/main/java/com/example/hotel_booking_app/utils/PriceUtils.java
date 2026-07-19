package com.example.hotel_booking_app.utils;

import java.text.NumberFormat;
import java.util.Locale;

public final class PriceUtils {
    private PriceUtils() {
    }

    public static double priceAfterDiscount(double regularPrice, double discount) {
        return Math.max(0, regularPrice - discount);
    }

    public static String formatUsd(double price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        return format.format(price);
    }

    public static String formatVnd(double priceUsd) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(priceUsd * 25000);
    }
}
