package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.AppNotification;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Payment;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.NotificationService;
import com.example.hotel_booking_app.services.PaymentService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.List;
import java.util.UUID;

public class CheckoutActivity extends AppCompatActivity {
    private static final double CLEANING_FEE = 20.0;
    private static final long MOCK_PAYMENT_DELAY_MS = 2500;

    private TextView summaryTextView;
    private TextView statusTextView;
    private BookingService bookingService;
    private PaymentService paymentService;
    private NotificationService notificationService;
    private SessionManager sessionManager;
    private Handler handler;
    private Button payButton;
    private Booking booking;
    private Payment payment;
    private String cabinName;
    private boolean processingPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

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
        notificationService = new NotificationService();
        sessionManager = new SessionManager(this);
        handler = new Handler(Looper.getMainLooper());

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
        statusTextView.setText("Loading checkout...");
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
                statusTextView.setText("Could not load existing payments: " + message);
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
        String paymentStatus = payment == null ? "Unpaid / pay on arrival available" : payment.getStatus();
        summaryTextView.setText(
                "Cabin: " + cabinName
                        + "\nDates: " + booking.getStartDate() + " -> " + booking.getEndDate()
                        + "\nCabin price: " + PriceUtils.formatUsd(booking.getCabinPrice())
                        + "\nExtras: " + PriceUtils.formatUsd(booking.getExtrasPrice())
                        + "\nCleaning fee: " + PriceUtils.formatUsd(CLEANING_FEE)
                        + "\nDiscount: -" + PriceUtils.formatUsd(booking.getDiscountAmount())
                        + "\nTotal: " + PriceUtils.formatUsd(totalDue)
                        + "\nBooking: " + booking.getStatus()
                        + "\nPayment: " + paymentStatus
        );
        if (isPaid(payment)) {
            payButton.setText("Paid");
            payButton.setEnabled(true);
            statusTextView.setText("Payment is paid. You can view the invoice.");
        } else if (isPending(payment)) {
            payButton.setText("Complete pending payment");
            payButton.setEnabled(true);
            statusTextView.setText("A pending payment exists. Tap Pay now to mark it paid.");
        } else {
            payButton.setText("Pay now (mock)");
            payButton.setEnabled(true);
            statusTextView.setText("Choose Pay now or Pay on arrival. Pay on arrival keeps this booking unpaid.");
        }
    }

    private void payOnArrival() {
        if (booking == null) {
            statusTextView.setText("Booking is still loading.");
            return;
        }
        bookingService.updateStatusNoReturn(booking.getId(), AppConstants.BOOKING_CONFIRMED, false, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean updated) {
                booking.setStatus(AppConstants.BOOKING_CONFIRMED);
                booking.setPaid(false);
                Toast.makeText(CheckoutActivity.this, "Booking confirmed. Pay on arrival.", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Booking confirmed. Payment will be collected on arrival.");
                renderSummary();
                Intent intent = new Intent(CheckoutActivity.this, MyBookingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Could not choose pay on arrival: " + message);
            }
        });
    }

    private void payNow() {
        if (processingPayment) {
            return;
        }
        if (booking == null) {
            statusTextView.setText("Booking is still loading.");
            return;
        }
        if (isPaid(payment)) {
            statusTextView.setText("This booking is already paid. Opening invoice.");
            openInvoice();
            return;
        }
        if (isPending(payment)) {
            startMockProcessing(payment);
            return;
        }
        double amount = booking.getTotalPrice() + CLEANING_FEE;
        setProcessingUi(true);
        paymentService.createMockPayment(booking.getId(), sessionManager.getUserId(), amount, new SupabaseCallback<Payment>() {
            @Override
            public void onSuccess(Payment pendingPayment) {
                payment = pendingPayment;
                startMockProcessing(pendingPayment);
            }

            @Override
            public void onError(String message) {
                setProcessingUi(false);
                statusTextView.setText(message);
            }
        });
    }

    private void startMockProcessing(Payment pendingPayment) {
        setProcessingUi(true);
        handler.postDelayed(() -> markPaymentPaid(pendingPayment), MOCK_PAYMENT_DELAY_MS);
    }

    private void setProcessingUi(boolean processing) {
        processingPayment = processing;
        payButton.setEnabled(!processing);
        if (processing) {
            statusTextView.setText("Processing payment...");
        }
    }

    private void markPaymentPaid(Payment pendingPayment) {
        String transactionId = "MOCK-" + UUID.randomUUID();
        paymentService.markPaidNoReturn(pendingPayment, transactionId, new SupabaseCallback<Payment>() {
            @Override
            public void onSuccess(Payment paidPayment) {
                payment = paidPayment;
                bookingService.updateStatusNoReturn(booking.getId(), AppConstants.BOOKING_CONFIRMED, true, new SupabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean updated) {
                        booking.setStatus(AppConstants.BOOKING_CONFIRMED);
                        booking.setPaid(true);
                        createPaymentNotifications();
                        Toast.makeText(CheckoutActivity.this, "Payment successful", Toast.LENGTH_SHORT).show();
                        setProcessingUi(false);
                        renderSummary();
                        openBookingDetailAndFinish();
                    }

                    @Override
                    public void onError(String message) {
                        setProcessingUi(false);
                        statusTextView.setText("Payment paid, but booking status was not updated: " + message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                setProcessingUi(false);
                statusTextView.setText(message);
            }
        });
    }

    private void createPaymentNotifications() {
        String userId = sessionManager.getUserId();
        notificationService.createNotification(
                userId,
                "Payment Successful",
                "Payment for booking " + booking.getId() + " was successful.",
                "payment",
                new SupabaseCallback<AppNotification>() {
                    @Override
                    public void onSuccess(AppNotification data) {
                        createBookingConfirmedNotification(userId);
                    }

                    @Override
                    public void onError(String message) {
                        createBookingConfirmedNotification(userId);
                    }
                }
        );
    }

    private void openBookingDetailAndFinish() {
        Intent intent = new Intent(this, BookingDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_BOOKING_ID, booking.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void createBookingConfirmedNotification(String userId) {
        notificationService.createNotification(
                userId,
                "Booking Confirmed",
                "Booking " + booking.getId() + " has been confirmed.",
                "booking",
                new SupabaseCallback<AppNotification>() {
                    @Override
                    public void onSuccess(AppNotification data) {
                    }

                    @Override
                    public void onError(String message) {
                    }
                }
        );
    }

    private void openInvoice() {
        if (payment == null) {
            loadPaidPaymentThenOpenInvoice();
            return;
        }
        if (!isPaid(payment)) {
            statusTextView.setText("Please pay first before viewing the invoice.");
            return;
        }
        Intent intent = new Intent(this, InvoiceActivity.class);
        intent.putExtra(AppConstants.EXTRA_PAYMENT_ID, payment.getId());
        startActivity(intent);
    }

    private void loadPaidPaymentThenOpenInvoice() {
        if (booking == null) {
            statusTextView.setText("Booking is still loading.");
            return;
        }
        statusTextView.setText("Checking invoice...");
        paymentService.getPaymentsForBooking(booking.getId(), new SupabaseCallback<List<Payment>>() {
            @Override
            public void onSuccess(List<Payment> payments) {
                payment = findBestPayment(payments);
                if (isPaid(payment)) {
                    openInvoice();
                } else {
                    statusTextView.setText("Please pay first before viewing the invoice.");
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
}
