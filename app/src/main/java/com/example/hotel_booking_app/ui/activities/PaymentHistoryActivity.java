package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Payment;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.PaymentService;
import com.example.hotel_booking_app.ui.adapters.PaymentAdapter;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.List;
import java.util.UUID;

public class PaymentHistoryActivity extends AppCompatActivity {
    private TextView statusTextView;
    private PaymentAdapter adapter;
    private PaymentService paymentService;
    private BookingService bookingService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        statusTextView = findViewById(R.id.text_status);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        RecyclerView recyclerView = findViewById(R.id.recycler_payments);
        paymentService = new PaymentService();
        bookingService = new BookingService();
        sessionManager = new SessionManager(this);
        adapter = new PaymentAdapter(payment -> {
            Intent intent = new Intent(this, InvoiceActivity.class);
            intent.putExtra(AppConstants.EXTRA_PAYMENT_ID, payment.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        loadPayments();
    }

    private void loadPayments() {
        statusTextView.setText("Loading payment history...");
        paymentService.getPaymentsForUser(sessionManager.getUserId(), new SupabaseCallback<List<Payment>>() {
            @Override
            public void onSuccess(List<Payment> payments) {
                reconcilePaidPayments(payments);
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void reconcilePaidPayments(List<Payment> payments) {
        if (payments.isEmpty()) {
            adapter.submitList(payments);
            statusTextView.setText(buildStatusSummary(payments));
            return;
        }
        final int[] remaining = {payments.size()};
        for (Payment payment : payments) {
            if (!AppConstants.PAYMENT_PENDING.equalsIgnoreCase(payment.getStatus())) {
                finishOneReconcile(payments, remaining);
                continue;
            }
            bookingService.getBookingById(payment.getBookingId(), new SupabaseCallback<Booking>() {
                @Override
                public void onSuccess(Booking booking) {
                    if (booking.isPaid()) {
                        payment.setStatus(AppConstants.PAYMENT_PAID);
                        String transactionId = payment.getTransactionId();
                        if (transactionId == null || transactionId.trim().isEmpty()) {
                            transactionId = "MOCK-RECONCILED-" + UUID.randomUUID();
                        }
                        paymentService.markPaidNoReturn(payment, transactionId, new SupabaseCallback<Payment>() {
                            @Override
                            public void onSuccess(Payment paidPayment) {
                                payment.setStatus(AppConstants.PAYMENT_PAID);
                                payment.setTransactionId(paidPayment.getTransactionId());
                                payment.setPaidAt(paidPayment.getPaidAt());
                                finishOneReconcile(payments, remaining);
                            }

                            @Override
                            public void onError(String message) {
                                finishOneReconcile(payments, remaining);
                            }
                        });
                    } else {
                        finishOneReconcile(payments, remaining);
                    }
                }

                @Override
                public void onError(String message) {
                    finishOneReconcile(payments, remaining);
                }
            });
        }
    }

    private void finishOneReconcile(List<Payment> payments, int[] remaining) {
        remaining[0]--;
        if (remaining[0] == 0) {
            adapter.submitList(payments);
            statusTextView.setText(buildStatusSummary(payments));
        }
    }

    private String buildStatusSummary(List<Payment> payments) {
        int paid = 0;
        int pending = 0;
        int failed = 0;
        for (Payment payment : payments) {
            String status = payment.getStatus();
            if (AppConstants.PAYMENT_PAID.equalsIgnoreCase(status)) {
                paid++;
            } else if (AppConstants.PAYMENT_PENDING.equalsIgnoreCase(status)) {
                pending++;
            } else if (AppConstants.PAYMENT_FAILED.equalsIgnoreCase(status)) {
                failed++;
            }
        }
        return "Payments: " + payments.size()
                + " | Paid: " + paid
                + " | Pending: " + pending
                + " | Failed: " + failed;
    }
}
