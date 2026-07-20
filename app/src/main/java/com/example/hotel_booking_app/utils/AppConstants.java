package com.example.hotel_booking_app.utils;

public final class AppConstants {
    public static final String TABLE_CABINS = "cabins";
    public static final String TABLE_USERS = "users";
    public static final String TABLE_IMAGES = "images";
    public static final String TABLE_BOOKINGS = "bookings";
    public static final String TABLE_RATES = "rates";
    public static final String TABLE_PROMOTIONS = "promotions";
    public static final String TABLE_BOOKING_POLICIES = "booking_policies";
    public static final String TABLE_SETTINGS = "settings";
    public static final String TABLE_OTPS = "otps";
    public static final String TABLE_WISHLISTS = "wishlists";
    public static final String TABLE_PAYMENTS = "payments";
    public static final String TABLE_BLOCKED_DATES = "blocked_dates";
    public static final String TABLE_NOTIFICATIONS = "notifications";
    public static final String TABLE_CONVERSATIONS = "conversations";
    public static final String TABLE_MESSAGES = "messages";
    public static final String TABLE_AMENITIES = "amenities";
    public static final String TABLE_CABIN_AMENITIES = "cabin_amenities";
    public static final String TABLE_COUPONS = "coupons";
    public static final String TABLE_ROOM_TYPES = "room_types";
    public static final String TABLE_ROOM_INVENTORY = "room_inventory";

    public static final String ROLE_CUSTOMER = "customer";
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_HOST = ROLE_MANAGER;
    public static final String ROLE_ADMIN = ROLE_MANAGER;

    public static final String BOOKING_PENDING = "pending";
    public static final String BOOKING_CONFIRMED = "confirmed";
    public static final String BOOKING_CANCELLED = "cancelled";
    public static final String BOOKING_CHECKED_IN = "checked-in";
    public static final String BOOKING_CHECKED_OUT = "checked-out";

    public static final String PAYMENT_PENDING = "pending";
    public static final String PAYMENT_PAID = "paid";
    public static final String PAYMENT_FAILED = "failed";
    public static final String PAYMENT_REFUNDED = "refunded";

    public static final String COUPON_PERCENT = "percent";
    public static final String COUPON_FIXED = "fixed";

    public static final String EXTRA_CABIN_ID = "extra_cabin_id";
    public static final String EXTRA_ROOM_TYPE_ID = "extra_room_type_id";
    public static final String EXTRA_BOOKING_ID = "extra_booking_id";
    public static final String EXTRA_PAYMENT_ID = "extra_payment_id";

    private AppConstants() {
    }
}
