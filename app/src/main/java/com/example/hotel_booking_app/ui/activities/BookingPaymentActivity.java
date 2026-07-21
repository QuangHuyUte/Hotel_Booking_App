package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Payment;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.PaymentService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.List;

public class BookingPaymentActivity extends AppCompatActivity {
    private static final double CLEANING_FEE = 20.0;

    private TextView summaryTextView;
    private TextView statusTextView;
    private BookingService bookingService;
    private PaymentService paymentService;
    private SessionManager sessionManager;
    private Button payButton;
    private Booking booking;
    private Payment payment;
    private String cabinName;
    private boolean processingPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_payment);

        summaryTextView = findViewById(R.id.text_checkout_summary);
        statusTextView = findViewById(R.id.text_status);
        payButton = findViewById(R.id.button_pay);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        Button payLaterButton = findViewById(R.id.button_pay_later);
        Button invoiceButton = findViewById(R.id.button_invoice);
        Button historyButton = findViewById(R.id.button_payment_history);

        bookingService = new BookingService();
        paymentService = new PaymentService();
        sessionManager = new SessionManager(this);

        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        payButton.setOnClickListener(view -> payNow());
        payLaterButton.setOnClickListener(view -> payOnArrival());
        invoiceButton.setOnClickListener(view -> openInvoice());
        historyButton.setOnClickListener(view -> startActivity(new Intent(this, PaymentHistoryActivity.class)));
        loadBooking();
    }

    private void loadBooking() {
        String bookingId = getIntent().getStringExtra(AppConstants.EXTRA_BOOKING_ID);
        statusTextView.setText("Đang tải thanh toán...");
        bookingService.getBookingById(bookingId, new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking data) {
                booking = data;
                loadCabinName();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void loadCabinName() {
        new CabinService().getCabinById(booking.getCabinId(), new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                cabinName = cabin.getName();
                loadExistingPayment();
            }

            @Override
            public void onError(String message) {
                cabinName = booking.getCabinId();
                loadExistingPayment();
            }
        });
    }

    private void loadExistingPayment() {
        paymentService.getPaymentsForBooking(booking.getId(), new SupabaseCallback<List<Payment>>() {
            @Override
            public void onSuccess(List<Payment> payments) {
                payment = findBestPayment(payments);
                renderSummary();
            }

            @Override
            public void onError(String message) {
                payment = null;
                renderSummary();
                statusTextView.setText("Không thể tải thanh toán hiện có: " + message);
            }
        });
    }

    private Payment findBestPayment(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return null;
        }
        for (Payment item : payments) {
            if (isPaid(item)) {
                return item;
            }
        }
        return payments.get(0);
    }

    private void renderSummary() {
        double totalDue = booking.getTotalPrice() + CLEANING_FEE;
        String paymentStatus = payment == null ? "Chưa thanh toán / có thể trả khi nhận phòng" : translatePaymentStatus(payment.getStatus());
        summaryTextView.setText(
                "Khách sạn: " + cabinName
                        + "\nNgày: " + booking.getStartDate() + " -> " + booking.getEndDate()
                        + "\nGiá phòng: " + PriceUtils.formatUsd(booking.getCabinPrice())
                        + "\nDịch vụ thêm: " + PriceUtils.formatUsd(booking.getExtrasPrice())
                        + "\nPhí dọn phòng: " + PriceUtils.formatUsd(CLEANING_FEE)
                        + "\nGiảm giá: -" + PriceUtils.formatUsd(booking.getDiscountAmount())
                        + "\nTổng tiền: " + PriceUtils.formatUsd(totalDue)
                        + "\nĐặt phòng: " + translateBookingStatus(booking.getStatus())
                        + "\nThanh toán: " + paymentStatus
        );
        if (isPaid(payment)) {
            payButton.setText("Đã thanh toán");
            payButton.setEnabled(true);
            statusTextView.setText("Đơn này đã thanh toán. Bạn có thể xem hóa đơn.");
        } else if (isPending(payment)) {
            payButton.setText("Đang chờ xác nhận");
            payButton.setEnabled(false);
            statusTextView.setText("Yêu cầu thanh toán đã gửi. Khách sạn sẽ xác nhận trong tab Pending của manager.");
        } else {
            payButton.setText("Gửi yêu cầu thanh toán");
            payButton.setEnabled(true);
            statusTextView.setText("Gửi yêu cầu thanh toán trong app hoặc chọn trả khi nhận phòng.");
        }
    }

    private void payOnArrival() {
        if (booking == null) {
            statusTextView.setText("Đặt phòng vẫn đang tải.");
            return;
        }
        bookingService.updateStatusNoReturn(booking.getId(), AppConstants.BOOKING_CONFIRMED, false, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean updated) {
                booking.setStatus(AppConstants.BOOKING_CONFIRMED);
                booking.setPaid(false);
                Toast.makeText(BookingPaymentActivity.this, "Đã xác nhận, trả khi nhận phòng", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Đã xác nhận đặt phòng. Thanh toán sẽ thu khi nhận phòng.");
                renderSummary();
                Intent intent = new Intent(BookingPaymentActivity.this, GuestBookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Không thể chọn trả khi nhận phòng: " + message);
            }
        });
    }

    private void payNow() {
        if (processingPayment) {
            return;
        }
        if (booking == null) {
            statusTextView.setText("Đặt phòng vẫn đang tải.");
            return;
        }
        if (isPaid(payment)) {
            statusTextView.setText("Đặt phòng này đã thanh toán. Đang mở hóa đơn.");
            openInvoice();
            return;
        }
        if (isPending(payment)) {
            statusTextView.setText("Yêu cầu thanh toán đang chờ khách sạn xác nhận.");
            return;
        }
        double amount = booking.getTotalPrice() + CLEANING_FEE;
        setProcessingUi(true);
        paymentService.createMockPayment(booking.getId(), sessionManager.getUserId(), amount, new SupabaseCallback<Payment>() {
            @Override
            public void onSuccess(Payment pendingPayment) {
                payment = pendingPayment;
                setProcessingUi(false);
                Toast.makeText(BookingPaymentActivity.this, "Đã gửi yêu cầu thanh toán", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Đã gửi yêu cầu thanh toán. Khách sạn sẽ xác nhận trong tab Pending.");
                renderSummary();
            }

            @Override
            public void onError(String message) {
                setProcessingUi(false);
                statusTextView.setText(message);
            }
        });
    }

    private void setProcessingUi(boolean processing) {
        processingPayment = processing;
        payButton.setEnabled(!processing);
        if (processing) {
            statusTextView.setText("Đang xử lý thanh toán...");
        }
    }

    private void openInvoice() {
        if (payment == null) {
            loadPaidPaymentThenOpenInvoice();
            return;
        }
        if (!isPaid(payment)) {
            statusTextView.setText("Vui lòng thanh toán trước khi xem hóa đơn.");
            return;
        }
        Intent intent = new Intent(this, BookingInvoiceActivity.class);
        intent.putExtra(AppConstants.EXTRA_PAYMENT_ID, payment.getId());
        startActivity(intent);
    }

    private void loadPaidPaymentThenOpenInvoice() {
        if (booking == null) {
            statusTextView.setText("Đặt phòng vẫn đang tải.");
            return;
        }
        statusTextView.setText("Đang kiểm tra hóa đơn...");
        paymentService.getPaymentsForBooking(booking.getId(), new SupabaseCallback<List<Payment>>() {
            @Override
            public void onSuccess(List<Payment> payments) {
                payment = findBestPayment(payments);
                if (isPaid(payment)) {
                    openInvoice();
                } else {
                    statusTextView.setText("Vui lòng thanh toán trước khi xem hóa đơn.");
                }
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private boolean isPaid(Payment item) {
        return item != null && AppConstants.PAYMENT_PAID.equalsIgnoreCase(item.getStatus());
    }

    private boolean isPending(Payment item) {
        return item != null && AppConstants.PAYMENT_PENDING.equalsIgnoreCase(item.getStatus());
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
        return status == null || status.trim().isEmpty() ? "-" : status;
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
        return status == null || status.trim().isEmpty() ? "-" : status;
    }
}
