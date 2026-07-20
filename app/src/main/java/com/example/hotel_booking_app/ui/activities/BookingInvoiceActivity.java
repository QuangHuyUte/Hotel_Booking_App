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

public class BookingInvoiceActivity extends AppCompatActivity {
    private TextView invoiceTextView;
    private TextView statusTextView;
    private PaymentService paymentService;
    private BookingService bookingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_invoice);

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
        statusTextView.setText("Đang chuẩn bị hóa đơn...");
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
                "Mã thanh toán\n" + safe(payment.getId())
                        + "\n\nĐặt phòng\n" + bookingText
                        + "\n\nSố tiền\n" + PriceUtils.formatUsd(payment.getAmount())
                        + "\n\nTrạng thái\n" + translatePaymentStatus(payment.getStatus())
                        + "\n\nHình thức\n" + safe(payment.getMethod())
                        + "\n\nNhà cung cấp\n" + safe(payment.getProvider())
                        + "\n\nGiao dịch\n" + safe(payment.getTransactionId())
                        + "\n\nThanh toán lúc\n" + safe(payment.getPaidAt())
        );
        statusTextView.setText("Hóa đơn đã sẵn sàng.");
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String translatePaymentStatus(String status) {
        if (AppConstants.PAYMENT_PAID.equalsIgnoreCase(status)) {
            return "Đã thanh toán";
        }
        if (AppConstants.PAYMENT_PENDING.equalsIgnoreCase(status)) {
            return "Đang chờ";
        }
        if (AppConstants.PAYMENT_FAILED.equalsIgnoreCase(status)) {
            return "Thất bại";
        }
        if (AppConstants.PAYMENT_REFUNDED.equalsIgnoreCase(status)) {
            return "Đã hoàn tiền";
        }
        return safe(status);
    }
}
