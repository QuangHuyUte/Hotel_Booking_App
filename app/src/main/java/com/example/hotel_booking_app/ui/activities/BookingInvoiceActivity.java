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
    private String paymentId;
    private String bookingId;

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
        paymentId = getIntent().getStringExtra(AppConstants.EXTRA_PAYMENT_ID);
        bookingId = getIntent().getStringExtra(AppConstants.EXTRA_BOOKING_ID);
        statusTextView.setText("Đang chuẩn bị hóa đơn...");
        if (paymentId != null && !paymentId.trim().isEmpty()) {
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
                    loadBookingFallback(message);
                }
            });
            return;
        }
        loadBookingFallback(null);
    }

    private void loadBookingFallback(String fallbackMessage) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            statusTextView.setText(fallbackMessage == null ? "Không thể mở hóa đơn." : fallbackMessage);
            return;
        }
        bookingService.getBookingById(bookingId, new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                renderInvoiceFromBooking(booking);
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
                        + "\n\nHình thức\n" + translatePaymentMethod(payment.getMethod())
                        + "\n\nNhà cung cấp\n" + translatePaymentProvider(payment.getProvider())
                        + "\n\nGiao dịch\n" + safe(payment.getTransactionId())
                        + "\n\nThanh toán lúc\n" + safe(payment.getPaidAt())
        );
        statusTextView.setText("Hóa đơn đã sẵn sàng.");
    }

    private void renderInvoiceFromBooking(Booking booking) {
        invoiceTextView.setText(
                "Mã thanh toán\n" + safe(booking.getId())
                        + "\n\nĐặt phòng\n" + booking.getStartDate() + " -> " + booking.getEndDate()
                        + "\n\nSố tiền\n" + PriceUtils.formatUsd(booking.getTotalPrice())
                        + "\n\nTrạng thái\n" + translateBookingStatus(booking.getStatus())
                        + "\n\nHình thức\n" + translatePaymentMethod(booking.isPaid() ? "card" : "app")
                        + "\n\nNhà cung cấp\n" + translatePaymentProvider(booking.isPaid() ? "stripe" : "app")
                        + "\n\nGiao dịch\n" + (booking.isPaid() ? "BOOKING-" + booking.getId() : "-")
                        + "\n\nThanh toán lúc\n" + safe(booking.getCreatedAt())
        );
        statusTextView.setText("Hóa đơn đặt phòng đã sẵn sàng.");
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

    private String translateBookingStatus(String status) {
        if (AppConstants.BOOKING_CONFIRMED.equalsIgnoreCase(status)) {
            return "Đã xác nhận";
        }
        if (AppConstants.BOOKING_PENDING.equalsIgnoreCase(status)) {
            return "Đang chờ";
        }
        if (AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(status)) {
            return "Đã hủy";
        }
        if (AppConstants.BOOKING_CHECKED_IN.equalsIgnoreCase(status)) {
            return "Đã nhận phòng";
        }
        if (AppConstants.BOOKING_CHECKED_OUT.equalsIgnoreCase(status)) {
            return "Đã trả phòng";
        }
        return safe(status);
    }

    private String translatePaymentMethod(String method) {
        if (method == null || method.trim().isEmpty() || "-".equals(method)) {
            return "-";
        }
        if ("app".equalsIgnoreCase(method)) {
            return "Thanh toán trong app";
        }
        if ("card".equalsIgnoreCase(method)) {
            return "Thẻ";
        }
        if ("bank_transfer".equalsIgnoreCase(method)) {
            return "Chuyển khoản";
        }
        return method;
    }

    private String translatePaymentProvider(String provider) {
        if (provider == null || provider.trim().isEmpty() || "-".equals(provider)) {
            return "-";
        }
        if ("app".equalsIgnoreCase(provider) || "mock".equalsIgnoreCase(provider)) {
            return "Hệ thống";
        }
        if ("stripe".equalsIgnoreCase(provider)) {
            return "Stripe";
        }
        if ("manual".equalsIgnoreCase(provider)) {
            return "Thủ công";
        }
        return provider;
    }
}
