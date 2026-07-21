package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

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
import java.util.Locale;
import java.util.UUID;

public class PaymentHistoryActivity extends AppCompatActivity {
    private PaymentAdapter adapter;
    private PaymentService paymentService;
    private BookingService bookingService;
    private HostService hostService;
    private SessionManager sessionManager;
    private final List<Cabin> managerCabins = new ArrayList<>();
    private final List<Payment> allLoadedPayments = new ArrayList<>();
    private String selectedPaymentCabinId;
    private String selectedPaymentStatus = AppConstants.PAYMENT_PENDING;
    private String headerStatusMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

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
        }, this::acceptPayment, this::setPaymentStatusFilter, this::setCabinFilter, view -> finish());
        adapter.setManagerMode(sessionManager.isHostOrAdmin());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        if (sessionManager.isHostOrAdmin()) {
            ManagerNavigationHelper.bind(this, ManagerNavigationHelper.TAB_TRANSACTIONS);
        }
        refreshHeader();
        loadPayments();
    }

    private void loadPayments() {
        if (sessionManager.isHostOrAdmin()) {
            loadManagerPayments();
            return;
        }
        setHeaderStatusMessage("Đang tải lịch sử thanh toán của bạn...");
        bookingService.getBookingsForUser(sessionManager.getUserId(), new SupabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                collectPaymentsForBookings(bookings == null ? new ArrayList<>() : bookings);
            }

            @Override
            public void onError(String message) {
                setHeaderStatusMessage(message);
            }
        });
    }

    private void loadManagerPayments() {
        setHeaderStatusMessage("Đang tải giao dịch từ các khách sạn bạn quản lý...");
        hostService.getCabinsForHost(sessionManager.getUserId(), new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                if (cabins == null || cabins.isEmpty()) {
                    showLoadedPayments(new ArrayList<>());
                    setHeaderStatusMessage("Bạn chưa có khách sạn nào để kiểm tra giao dịch.");
                    return;
                }
                managerCabins.clear();
                managerCabins.addAll(cabins);
                collectBookingsForCabins(filteredPaymentCabins());
            }

            @Override
            public void onError(String message) {
                setHeaderStatusMessage(message);
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

    private void collectBookingsForCabins(List<Cabin> cabins) {
        if (cabins.isEmpty()) {
            showLoadedPayments(new ArrayList<>());
            setHeaderStatusMessage("Chưa có khách sạn phù hợp để kiểm tra giao dịch.");
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
        if (bookings == null || bookings.isEmpty()) {
            showLoadedPayments(new ArrayList<>());
            setHeaderStatusMessage(sessionManager.isHostOrAdmin()
                    ? "Chưa có giao dịch nào từ khách sạn bạn quản lý."
                    : "Bạn chưa có giao dịch đặt phòng nào.");
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
            showLoadedPayments(payments);
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
                            transactionId = "MGR-RECONCILED-" + UUID.randomUUID();
                        }
                        persistPaidPayment(payment, transactionId, false, new Runnable() {
                            @Override
                            public void run() {
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
            showLoadedPayments(payments);
        }
    }

    private void showLoadedPayments(List<Payment> payments) {
        allLoadedPayments.clear();
        if (payments != null) {
            allLoadedPayments.addAll(payments);
        }
        applyPaymentStatusFilter();
    }

    private void setPaymentStatusFilter(String status) {
        selectedPaymentStatus = AppConstants.PAYMENT_PENDING.equalsIgnoreCase(status)
                ? AppConstants.PAYMENT_PENDING
                : AppConstants.PAYMENT_PAID;
        applyPaymentStatusFilter();
    }

    private void setCabinFilter(String cabinId) {
        selectedPaymentCabinId = cabinId;
        collectBookingsForCabins(filteredPaymentCabins());
    }

    private void applyPaymentStatusFilter() {
        List<Payment> filtered = new ArrayList<>();
        for (Payment payment : allLoadedPayments) {
            if (matchesSelectedStatus(payment)) {
                filtered.add(payment);
            }
        }
        adapter.submitList(filtered);
        setHeaderStatusMessage(buildStatusSummary(allLoadedPayments, filtered.size()));
    }

    private boolean matchesSelectedStatus(Payment payment) {
        boolean pending = AppConstants.PAYMENT_PENDING.equalsIgnoreCase(payment.getStatus());
        if (AppConstants.PAYMENT_PENDING.equalsIgnoreCase(selectedPaymentStatus)) {
            return pending;
        }
        return !pending;
    }

    private int countPendingPayments() {
        int pending = 0;
        for (Payment payment : allLoadedPayments) {
            if (AppConstants.PAYMENT_PENDING.equalsIgnoreCase(payment.getStatus())) {
                pending++;
            }
        }
        return pending;
    }

    private int countFinishedPayments() {
        return Math.max(0, allLoadedPayments.size() - countPendingPayments());
    }

    private void setHeaderStatusMessage(String message) {
        headerStatusMessage = message == null ? "" : message;
        refreshHeader();
    }

    private void refreshHeader() {
        if (adapter == null) {
            return;
        }
        adapter.setHeaderState(
                sessionManager != null && sessionManager.isHostOrAdmin() ? "Giao dịch khách sạn" : "Lịch sử giao dịch",
                headerStatusMessage,
                selectedPaymentStatus,
                countPendingPayments(),
                countFinishedPayments(),
                managerCabins,
                selectedPaymentCabinId
        );
    }

    private void acceptPayment(Payment payment) {
        if (!sessionManager.isHostOrAdmin()) {
            return;
        }
        if (!AppConstants.PAYMENT_PENDING.equalsIgnoreCase(payment.getStatus())) {
            Toast.makeText(this, "Giao dịch này đã xử lý.", Toast.LENGTH_SHORT).show();
            return;
        }
        setHeaderStatusMessage("Đang xác nhận thanh toán...");
        bookingService.getBookingById(payment.getBookingId(), new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                String nextStatus = AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus())
                        ? AppConstants.BOOKING_CONFIRMED
                        : booking.getStatus();
                persistAcceptedPayment(payment, nextStatus);
            }

            @Override
            public void onError(String message) {
                persistAcceptedPayment(payment, AppConstants.BOOKING_CONFIRMED);
            }
        });
    }

    private void persistAcceptedPayment(Payment payment, String bookingStatus) {
        String transactionId = "MGR-ACCEPT-" + UUID.randomUUID();
        persistPaidPayment(payment, transactionId, true, new Runnable() {
            @Override
            public void run() {
                bookingService.updateStatusNoReturn(payment.getBookingId(), bookingStatus, true, new SupabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean updated) {
                        Toast.makeText(PaymentHistoryActivity.this, "Đã xác nhận thanh toán.", Toast.LENGTH_SHORT).show();
                        loadPayments();
                    }

                    @Override
                    public void onError(String message) {
                        setHeaderStatusMessage("Payment đã xác nhận, nhưng booking chưa cập nhật: " + message);
                        loadPayments();
                    }
                });
            }
        });
    }

    private void persistPaidPayment(Payment payment, String transactionId, boolean showErrors, Runnable onDone) {
        if (isPlaceholderPayment(payment)) {
            paymentService.createMockPayment(payment.getBookingId(), payment.getUserId(), payment.getAmount(), new SupabaseCallback<Payment>() {
                @Override
                public void onSuccess(Payment createdPayment) {
                    markPaymentPaid(createdPayment, transactionId, showErrors, onDone);
                }

                @Override
                public void onError(String message) {
                    if (showErrors) {
                        setHeaderStatusMessage(message);
                        return;
                    }
                    onDone.run();
                }
            });
            return;
        }
        markPaymentPaid(payment, transactionId, showErrors, onDone);
    }

    private void markPaymentPaid(Payment payment, String transactionId, boolean showErrors, Runnable onDone) {
        paymentService.markPaidNoReturn(payment, transactionId, new SupabaseCallback<Payment>() {
            @Override
            public void onSuccess(Payment paidPayment) {
                payment.setStatus(AppConstants.PAYMENT_PAID);
                payment.setTransactionId(paidPayment.getTransactionId());
                payment.setPaidAt(paidPayment.getPaidAt());
                onDone.run();
            }

            @Override
            public void onError(String message) {
                if (showErrors) {
                    setHeaderStatusMessage(message);
                    return;
                }
                onDone.run();
            }
        });
    }

    private boolean isPlaceholderPayment(Payment payment) {
        String paymentId = safe(payment.getId());
        String bookingId = safe(payment.getBookingId());
        String transactionId = safe(payment.getTransactionId());
        return !bookingId.isEmpty() && paymentId.equals(bookingId) && transactionId.isEmpty();
    }

    private String buildStatusSummary(List<Payment> payments, int visibleCount) {
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
        String tabLabel = AppConstants.PAYMENT_PENDING.equalsIgnoreCase(selectedPaymentStatus) ? "Pending" : "Finished";
        String roleHint = sessionManager.isHostOrAdmin()
                ? "Pending là giao dịch cần bấm Xác nhận."
                : "Pending là khoản đang chờ khách sạn xác nhận.";
        return tabLabel + ": " + visibleCount
                + " | Tổng: " + payments.size()
                + " | Đã trả: " + paid
                + " | Đang chờ: " + pending
                + " | Thất bại: " + failed
                + " | Doanh thu đã nhận: $" + String.format(Locale.US, "%.2f", paidAmount)
                + "\n" + roleHint;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
