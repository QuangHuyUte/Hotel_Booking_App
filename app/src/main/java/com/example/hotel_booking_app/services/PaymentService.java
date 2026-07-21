package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Payment;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PaymentService {
    private static final DateTimeFormatter SUPABASE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final SupabaseClient supabaseClient;

    public PaymentService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getPaymentsForUser(String userId, SupabaseCallback<List<Payment>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("userId", userId);
        supabaseClient.getList(AppConstants.TABLE_PAYMENTS, "*", null, "createdAt.desc", filters, Payment[].class, callback);
    }

    public void getPaymentById(String paymentId, SupabaseCallback<Payment> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", paymentId);
        supabaseClient.getSingle(AppConstants.TABLE_PAYMENTS, filters, Payment[].class, callback);
    }

    public void getPaymentsForBooking(String bookingId, SupabaseCallback<List<Payment>> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("bookingId", bookingId);
        supabaseClient.getList(AppConstants.TABLE_PAYMENTS, "*", null, "createdAt.desc", filters, Payment[].class, callback);
    }

    public void createMockPayment(String bookingId, String userId, double amount, SupabaseCallback<Payment> callback) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setMethod("app");
        payment.setProvider("app");
        payment.setStatus(AppConstants.PAYMENT_PENDING);
        supabaseClient.insertNoReturn(AppConstants.TABLE_PAYMENTS, payment, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                callback.onSuccess(payment);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void markPaid(String paymentId, String transactionId, SupabaseCallback<Payment> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", paymentId);

        String paidAt = currentTimestamp();
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", AppConstants.PAYMENT_PAID);
        payload.put("transactionId", transactionId);
        payload.put("paidAt", paidAt);
        payload.put("updatedAt", paidAt);
        supabaseClient.update(AppConstants.TABLE_PAYMENTS, filters, payload, Payment[].class, callback);
    }

    public void markPaidNoReturn(Payment payment, String transactionId, SupabaseCallback<Payment> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", payment.getId());

        String paidAt = currentTimestamp();
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", AppConstants.PAYMENT_PAID);
        payload.put("transactionId", transactionId);
        payload.put("paidAt", paidAt);
        payload.put("updatedAt", paidAt);
        supabaseClient.updateNoReturn(AppConstants.TABLE_PAYMENTS, filters, payload, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean updated) {
                payment.setStatus(AppConstants.PAYMENT_PAID);
                payment.setTransactionId(transactionId);
                payment.setPaidAt(paidAt);
                payment.setUpdatedAt(paidAt);
                callback.onSuccess(payment);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void markFailed(String paymentId, SupabaseCallback<Payment> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", paymentId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", AppConstants.PAYMENT_FAILED);
        payload.put("updatedAt", currentTimestamp());
        supabaseClient.update(AppConstants.TABLE_PAYMENTS, filters, payload, Payment[].class, callback);
    }

    public void markFailedNoReturn(Payment payment, SupabaseCallback<Payment> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", payment.getId());

        String updatedAt = currentTimestamp();
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", AppConstants.PAYMENT_FAILED);
        payload.put("updatedAt", updatedAt);
        supabaseClient.updateNoReturn(AppConstants.TABLE_PAYMENTS, filters, payload, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean updated) {
                payment.setStatus(AppConstants.PAYMENT_FAILED);
                payment.setUpdatedAt(updatedAt);
                callback.onSuccess(payment);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private String currentTimestamp() {
        return LocalDateTime.now().format(SUPABASE_TIMESTAMP_FORMATTER);
    }
}
