package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Payment;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.HostService;
import com.example.hotel_booking_app.services.PaymentService;
import com.example.hotel_booking_app.ui.adapters.PaymentAdapter;
import com.example.hotel_booking_app.ui.helpers.ManagerNavigationHelper;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PaymentHistoryActivity extends AppCompatActivity {
    private TextView statusTextView;
    private LinearLayout hotelTabsContainer;
    private PaymentAdapter adapter;
    private PaymentService paymentService;
    private BookingService bookingService;
    private HostService hostService;
    private SessionManager sessionManager;
    private final List<Cabin> managerCabins = new ArrayList<>();
    private final List<Button> paymentHotelTabButtons = new ArrayList<>();
    private final List<String> paymentHotelTabCabinIds = new ArrayList<>();
    private String selectedPaymentCabinId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        statusTextView = findViewById(R.id.text_status);
        hotelTabsContainer = findViewById(R.id.container_payment_hotel_tabs);
        TextView titleTextView = findViewById(R.id.text_payment_title);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        RecyclerView recyclerView = findViewById(R.id.recycler_payments);
        paymentService = new PaymentService();
        bookingService = new BookingService();
        hostService = new HostService();
        sessionManager = new SessionManager(this);
        adapter = new PaymentAdapter(payment -> {
            Intent intent = new Intent(this, BookingInvoiceActivity.class);
            intent.putExtra(AppConstants.EXTRA_PAYMENT_ID, payment.getId());
            intent.putExtra(AppConstants.EXTRA_BOOKING_ID, payment.getBookingId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        if (sessionManager.isHostOrAdmin()) {
            titleTextView.setText("Giao dịch khách sạn");
            ManagerNavigationHelper.bind(this, ManagerNavigationHelper.TAB_TRANSACTIONS);
        }
        loadPayments();
    }

    private void loadPayments() {
        if (sessionManager.isHostOrAdmin()) {
            loadManagerPayments();
            return;
        }
        statusTextView.setText("Đang tải lịch sử thanh toán...");
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

    private void loadManagerPayments() {
        statusTextView.setText("Đang tải giao dịch từ các khách sạn bạn quản lý...");
        hostService.getCabinsForHost(sessionManager.getUserId(), new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                if (cabins == null || cabins.isEmpty()) {
                    adapter.submitList(new ArrayList<>());
                    statusTextView.setText("Bạn chưa có khách sạn nào để kiểm tra giao dịch.");
                    return;
                }
                managerCabins.clear();
                managerCabins.addAll(cabins);
                renderPaymentHotelTabs();
                collectBookingsForCabins(filteredPaymentCabins());
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private List<Cabin> filteredPaymentCabins() {
        if (selectedPaymentCabinId == null) {
            return new ArrayList<>(managerCabins);
        }
        List<Cabin> filtered = new ArrayList<>();
        for (Cabin cabin : managerCabins) {
            if (selectedPaymentCabinId.equals(cabin.getId())) {
                filtered.add(cabin);
                break;
            }
        }
        return filtered;
    }

    private void renderPaymentHotelTabs() {
        int expectedTabs = managerCabins.size() + 1;
        if (paymentHotelTabButtons.size() != expectedTabs) {
            hotelTabsContainer.removeAllViews();
            paymentHotelTabButtons.clear();
            paymentHotelTabCabinIds.clear();
            addPaymentHotelTab("Tất cả", null);
            for (Cabin cabin : managerCabins) {
                addPaymentHotelTab(shortHotelName(cabin), cabin.getId());
            }
        }
        updatePaymentHotelTabStyles();
    }

    private void addPaymentHotelTab(String label, String cabinId) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setTextSize(13);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
        );
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);
        button.setOnClickListener(view -> {
            selectedPaymentCabinId = cabinId;
            updatePaymentHotelTabStyles();
            collectBookingsForCabins(filteredPaymentCabins());
        });
        paymentHotelTabButtons.add(button);
        paymentHotelTabCabinIds.add(cabinId);
        hotelTabsContainer.addView(button);
    }

    private void updatePaymentHotelTabStyles() {
        for (int i = 0; i < paymentHotelTabButtons.size(); i++) {
            Button button = paymentHotelTabButtons.get(i);
            boolean selected = Objects.equals(paymentHotelTabCabinIds.get(i), selectedPaymentCabinId);
            button.setTextColor(getColor(selected ? R.color.black : R.color.ink));
            button.setBackgroundResource(selected ? R.drawable.bg_button_primary : R.drawable.bg_manager_search);
        }
    }
    private void collectBookingsForCabins(List<Cabin> cabins) {
        if (cabins.isEmpty()) {
            adapter.submitList(new ArrayList<>());
            statusTextView.setText("Chưa có khách sạn phù hợp để kiểm tra giao dịch.");
            return;
        }
        List<Booking> allBookings = new ArrayList<>();
        final int[] remaining = {cabins.size()};
        for (Cabin cabin : cabins) {
            hostService.getBookingsForCabin(cabin.getId(), new SupabaseCallback<List<Booking>>() {
                @Override
                public void onSuccess(List<Booking> bookings) {
                    if (bookings != null) {
                        allBookings.addAll(bookings);
                    }
                    finishOneCabinBookingLoad(allBookings, remaining);
                }

                @Override
                public void onError(String message) {
                    finishOneCabinBookingLoad(allBookings, remaining);
                }
            });
        }
    }

    private void finishOneCabinBookingLoad(List<Booking> bookings, int[] remaining) {
        remaining[0]--;
        if (remaining[0] == 0) {
            collectPaymentsForBookings(bookings);
        }
    }

    private void collectPaymentsForBookings(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            adapter.submitList(new ArrayList<>());
            statusTextView.setText("Chưa có giao dịch nào từ khách sạn bạn quản lý.");
            return;
        }
        List<Payment> payments = new ArrayList<>();
        final int[] remaining = {bookings.size()};
        for (Booking booking : bookings) {
            paymentService.getPaymentsForBooking(booking.getId(), new SupabaseCallback<List<Payment>>() {
                @Override
                public void onSuccess(List<Payment> bookingPayments) {
                    if (bookingPayments != null && !bookingPayments.isEmpty()) {
                        payments.addAll(bookingPayments);
                    } else {
                        payments.add(buildPlaceholderPayment(booking));
                    }
                    finishOneBookingPaymentLoad(payments, remaining);
                }

                @Override
                public void onError(String message) {
                    payments.add(buildPlaceholderPayment(booking));
                    finishOneBookingPaymentLoad(payments, remaining);
                }
            });
        }
    }

    private Payment buildPlaceholderPayment(Booking booking) {
        Payment payment = new Payment();
        payment.setId(booking.getId());
        payment.setBookingId(booking.getId());
        payment.setUserId(booking.getUserId());
        payment.setAmount(booking.getTotalPrice());
        payment.setMethod(booking.isPaid() ? "card" : "app");
        payment.setProvider(booking.isPaid() ? "stripe" : "app");
        payment.setStatus(booking.isPaid() ? AppConstants.PAYMENT_PAID : AppConstants.PAYMENT_PENDING);
        payment.setTransactionId(booking.isPaid() ? "BOOKING-" + booking.getId() : "");
        payment.setPaidAt(booking.isPaid() ? booking.getCreatedAt() : "");
        payment.setCreatedAt(booking.getCreatedAt());
        payment.setUpdatedAt(booking.getUpdatedAt());
        return payment;
    }

    private void finishOneBookingPaymentLoad(List<Payment> payments, int[] remaining) {
        remaining[0]--;
        if (remaining[0] == 0) {
            payments.sort(Comparator.comparing(payment -> safe(payment.getCreatedAt()), Comparator.reverseOrder()));
            reconcilePaidPayments(payments);
        }
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
        double paidAmount = 0;
        for (Payment payment : payments) {
            String status = payment.getStatus();
            if (AppConstants.PAYMENT_PAID.equalsIgnoreCase(status)) {
                paid++;
                paidAmount += payment.getAmount();
            } else if (AppConstants.PAYMENT_PENDING.equalsIgnoreCase(status)) {
                pending++;
            } else if (AppConstants.PAYMENT_FAILED.equalsIgnoreCase(status)) {
                failed++;
            }
        }
        return "Giao dịch: " + payments.size()
                + " | Đã trả: " + paid
                + " | Đang chờ: " + pending
                + " | Thất bại: " + failed
                + " | Doanh thu đã nhận: $" + String.format(java.util.Locale.US, "%.2f", paidAmount);
    }

    private String shortHotelName(Cabin cabin) {
        String name = safe(cabin.getName());
        return name.length() <= 22 ? name : name.substring(0, 21).trim() + "...";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
