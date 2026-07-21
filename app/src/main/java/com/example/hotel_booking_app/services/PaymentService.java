package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Payment;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentService {
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

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", AppConstants.PAYMENT_PAID);
        payload.put("transactionId", transactionId);
        payload.put("paidAt", LocalDateTime.now().toString());
        supabaseClient.update(AppConstants.TABLE_PAYMENTS, filters, payload, Payment[].class, callback);
    }

    public void markPaidNoReturn(Payment payment, String transactionId, SupabaseCallback<Payment> callback) {
        markPaid(payment.getId(), transactionId, new SupabaseCallback<Payment>() {
            @Override
            public void onSuccess(Payment updatedPayment) {
                callback.onSuccess(updatedPayment);
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
        supabaseClient.update(AppConstants.TABLE_PAYMENTS, filters, payload, Payment[].class, callback);
    }
}
