package com.example.hotel_booking_app.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Payment;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.PaymentService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.util.Locale;

public class InvoiceActivity extends AppCompatActivity {
    private TextView invoiceTextView;
    private TextView statusTextView;
    private PaymentService paymentService;
    private BookingService bookingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice);

        invoiceTextView = findViewById(R.id.text_invoice);
        statusTextView = findViewById(R.id.text_status);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        paymentService = new PaymentService();
        bookingService = new BookingService();
        loadInvoice();
    }

    private void loadInvoice() {
        String paymentId = getIntent().getStringExtra(AppConstants.EXTRA_PAYMENT_ID);
        statusTextView.setText("Preparing invoice...");
        paymentService.getPaymentById(paymentId, new SupabaseCallback<Payment>() {
            @Override
            public void onSuccess(Payment payment) {
                bookingService.getBookingById(payment.getBookingId(), new SupabaseCallback<Booking>() {
                    @Override
                    public void onSuccess(Booking booking) {
                        renderInvoice(payment, booking);
                    }

                    @Override
                    public void onError(String message) {
                        renderInvoice(payment, null);
                    }
                });
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void renderInvoice(Payment payment, Booking booking) {
        String bookingText = booking == null
                ? safe(payment.getBookingId())
                : booking.getStartDate() + " -> " + booking.getEndDate();
        invoiceTextView.setText(
                "Payment ID\n" + safe(payment.getId())
                        + "\n\nBooking\n" + bookingText
                        + "\n\nAmount\n" + PriceUtils.formatUsd(payment.getAmount())
                        + "\n\nStatus\n" + safe(payment.getStatus()).toUpperCase(Locale.US)
                        + "\n\nMethod\n" + safe(payment.getMethod())
                        + "\n\nProvider\n" + safe(payment.getProvider())
                        + "\n\nTransaction\n" + safe(payment.getTransactionId())
                        + "\n\nPaid At\n" + safe(payment.getPaidAt())
        );
        statusTextView.setText("Invoice ready.");
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
