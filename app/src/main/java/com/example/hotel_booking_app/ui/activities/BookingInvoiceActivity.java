package com.example.hotel_booking_app.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Payment;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.PaymentService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.UUID;

public class BookingInvoiceActivity extends AppCompatActivity {
    private TextView invoiceTextView;
    private TextView statusTextView;
    private PaymentService paymentService;
    private BookingService bookingService;
    private CabinService cabinService;
    private RoomTypeService roomTypeService;
    private AuthService authService;
    private SessionManager sessionManager;
    private LinearLayout managerActionsContainer;
    private Button acceptPaymentButton;
    private Button declinePaymentButton;
    private String paymentId;
    private String bookingId;
    private Payment currentPayment;
    private Booking currentBooking;
    private String cabinName = "-";
    private String roomLabel = "-";
    private String userLabel = "-";
    private boolean processingPaymentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_invoice);

        invoiceTextView = findViewById(R.id.text_invoice);
        statusTextView = findViewById(R.id.text_status);
        managerActionsContainer = findViewById(R.id.container_manager_payment_actions);
        acceptPaymentButton = findViewById(R.id.button_accept_payment);
        declinePaymentButton = findViewById(R.id.button_decline_payment);
        Button backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(view -> finish());
        acceptPaymentButton.setOnClickListener(view -> acceptPendingPayment());
        declinePaymentButton.setOnClickListener(view -> declinePendingPayment());
        paymentService = new PaymentService();
        bookingService = new BookingService();
        cabinService = new CabinService();
        roomTypeService = new RoomTypeService();
        authService = new AuthService();
        sessionManager = new SessionManager(this);
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
        currentPayment = payment;
        currentBooking = booking;
        renderInvoiceContent();
        updateManagerActions();
        loadRelatedDetails(payment, booking);
        statusTextView.setText("Chi tiết giao dịch đã sẵn sàng.");
    }

    private void renderInvoiceFromBooking(Booking booking) {
        Payment fallbackPayment = new Payment();
        fallbackPayment.setId(booking.getId());
        fallbackPayment.setBookingId(booking.getId());
        fallbackPayment.setUserId(booking.getUserId());
        fallbackPayment.setAmount(booking.getTotalPrice());
        fallbackPayment.setMethod(booking.isPaid() ? "card" : "app");
        fallbackPayment.setProvider(booking.isPaid() ? "stripe" : "app");
        fallbackPayment.setStatus(booking.isPaid() ? AppConstants.PAYMENT_PAID : AppConstants.PAYMENT_PENDING);
        fallbackPayment.setTransactionId(booking.isPaid() ? "BOOKING-" + booking.getId() : "-");
        fallbackPayment.setPaidAt(booking.isPaid() ? booking.getCreatedAt() : "");
        fallbackPayment.setCreatedAt(booking.getCreatedAt());
        renderInvoice(fallbackPayment, booking);
    }

    private void renderInvoiceContent() {
        Payment payment = currentPayment;
        Booking booking = currentBooking;
        if (payment == null) {
            return;
        }
        String stayText = booking == null
                ? safe(payment.getBookingId())
                : safe(booking.getStartDate()) + " -> " + safe(booking.getEndDate())
                + " · " + booking.getNumNights() + " đêm";
        String roomGuestText = booking == null
                ? "-"
                : Math.max(1, booking.getNumRooms()) + " phòng · " + booking.getNumGuests() + " khách";
        String priceBreakdown = booking == null
                ? PriceUtils.formatUsd(payment.getAmount())
                : "Giá phòng: " + PriceUtils.formatUsd(booking.getCabinPrice())
                + "\nDịch vụ thêm: " + PriceUtils.formatUsd(booking.getExtrasPrice())
                + "\nGiảm giá: -" + PriceUtils.formatUsd(booking.getDiscountAmount())
                + "\nTổng giao dịch: " + PriceUtils.formatUsd(payment.getAmount());

        invoiceTextView.setText(
                "Mã thanh toán\n" + safe(payment.getId())
                        + "\n\nMã booking\n" + safe(payment.getBookingId())
                        + "\n\nKhách sạn\n" + cabinName
                        + "\n\nLoại phòng\n" + roomLabel
                        + "\n\nThời gian ở\n" + stayText
                        + "\n\nSố phòng / khách\n" + roomGuestText
                        + "\n\nNgười giao dịch\n" + userLabel
                        + "\n\nChi phí\n" + priceBreakdown
                        + "\n\nTrạng thái thanh toán\n" + translatePaymentStatus(payment.getStatus())
                        + "\n\nTrạng thái booking\n" + (booking == null ? "-" : translateBookingStatus(booking.getStatus()))
                        + "\n\nHình thức\n" + translatePaymentMethod(payment.getMethod())
                        + "\n\nNhà cung cấp\n" + translatePaymentProvider(payment.getProvider())
                        + "\n\nGiao dịch\n" + safe(payment.getTransactionId())
                        + "\n\nTạo lúc\n" + safe(payment.getCreatedAt())
                        + "\n\nThanh toán lúc\n" + paidAtLabel(payment)
        );
        updateManagerActions();
    }

    private void updateManagerActions() {
        if (managerActionsContainer == null || currentPayment == null || sessionManager == null) {
            return;
        }
        boolean canHandle = sessionManager.isHostOrAdmin()
                && !processingPaymentAction
                && AppConstants.PAYMENT_PENDING.equalsIgnoreCase(currentPayment.getStatus())
                && currentPayment.getBookingId() != null
                && !currentPayment.getBookingId().trim().isEmpty()
                && (currentBooking == null || !currentBooking.isPaid());
        managerActionsContainer.setVisibility(canHandle ? View.VISIBLE : View.GONE);
        acceptPaymentButton.setEnabled(canHandle);
        declinePaymentButton.setEnabled(canHandle);
    }

    private void acceptPendingPayment() {
        if (!canProcessManagerAction()) {
            return;
        }
        setProcessingAction(true, "Đang xác nhận thanh toán...");
        String nextBookingStatus = acceptedBookingStatus(currentBooking);
        String bookingIdToUpdate = currentPayment.getBookingId();
        persistPaidPayment(currentPayment, "MGR-ACCEPT-" + UUID.randomUUID(), () ->
                bookingService.updateStatusNoReturn(bookingIdToUpdate, nextBookingStatus, true, new SupabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean updated) {
                        currentPayment.setStatus(AppConstants.PAYMENT_PAID);
                        if (currentBooking != null) {
                            currentBooking.setStatus(nextBookingStatus);
                            currentBooking.setPaid(true);
                        }
                        setProcessingAction(false, "Đã xác nhận thanh toán.");
                        Toast.makeText(BookingInvoiceActivity.this, "Đã accept payment.", Toast.LENGTH_SHORT).show();
                        renderInvoiceContent();
                    }

                    @Override
                    public void onError(String message) {
                        setProcessingAction(false, "Payment đã accept, nhưng booking chưa cập nhật: " + message);
                        renderInvoiceContent();
                    }
                })
        );
    }

    private void declinePendingPayment() {
        if (!canProcessManagerAction()) {
            return;
        }
        setProcessingAction(true, "Đang từ chối thanh toán...");
        String bookingIdToUpdate = currentPayment.getBookingId();
        persistFailedPayment(currentPayment, () ->
                bookingService.updateStatusNoReturn(bookingIdToUpdate, AppConstants.BOOKING_CANCELLED, false, new SupabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean updated) {
                        currentPayment.setStatus(AppConstants.PAYMENT_FAILED);
                        if (currentBooking != null) {
                            currentBooking.setStatus(AppConstants.BOOKING_CANCELLED);
                            currentBooking.setPaid(false);
                        }
                        setProcessingAction(false, "Đã decline payment và hủy booking.");
                        Toast.makeText(BookingInvoiceActivity.this, "Đã decline payment.", Toast.LENGTH_SHORT).show();
                        renderInvoiceContent();
                    }

                    @Override
                    public void onError(String message) {
                        setProcessingAction(false, "Payment đã decline, nhưng booking chưa cập nhật: " + message);
                        renderInvoiceContent();
                    }
                })
        );
    }

    private boolean canProcessManagerAction() {
        return sessionManager != null
                && sessionManager.isHostOrAdmin()
                && currentPayment != null
                && !processingPaymentAction
                && AppConstants.PAYMENT_PENDING.equalsIgnoreCase(currentPayment.getStatus());
    }

    private String acceptedBookingStatus(Booking booking) {
        String currentStatus = booking == null ? "" : safe(booking.getStatus());
        if (AppConstants.BOOKING_CHECKED_IN.equalsIgnoreCase(currentStatus)
                || AppConstants.BOOKING_CHECKED_OUT.equalsIgnoreCase(currentStatus)) {
            return currentStatus;
        }
        return AppConstants.BOOKING_CONFIRMED;
    }

    private void persistPaidPayment(Payment payment, String transactionId, Runnable onDone) {
        if (isPlaceholderPayment(payment)) {
            paymentService.createMockPayment(payment.getBookingId(), payment.getUserId(), payment.getAmount(), new SupabaseCallback<Payment>() {
                @Override
                public void onSuccess(Payment createdPayment) {
                    markCreatedPaymentPaid(createdPayment, transactionId, onDone);
                }

                @Override
                public void onError(String message) {
                    setProcessingAction(false, message);
                }
            });
            return;
        }
        markCreatedPaymentPaid(payment, transactionId, onDone);
    }

    private void markCreatedPaymentPaid(Payment payment, String transactionId, Runnable onDone) {
        paymentService.markPaidNoReturn(payment, transactionId, new SupabaseCallback<Payment>() {
            @Override
            public void onSuccess(Payment paidPayment) {
                currentPayment = paidPayment;
                onDone.run();
            }

            @Override
            public void onError(String message) {
                setProcessingAction(false, message);
            }
        });
    }

    private void persistFailedPayment(Payment payment, Runnable onDone) {
        if (isPlaceholderPayment(payment)) {
            paymentService.createMockPayment(payment.getBookingId(), payment.getUserId(), payment.getAmount(), new SupabaseCallback<Payment>() {
                @Override
                public void onSuccess(Payment createdPayment) {
                    markPaymentFailed(createdPayment, onDone);
                }

                @Override
                public void onError(String message) {
                    setProcessingAction(false, message);
                }
            });
            return;
        }
        markPaymentFailed(payment, onDone);
    }

    private void markPaymentFailed(Payment payment, Runnable onDone) {
        paymentService.markFailedNoReturn(payment, new SupabaseCallback<Payment>() {
            @Override
            public void onSuccess(Payment failedPayment) {
                currentPayment = failedPayment;
                onDone.run();
            }

            @Override
            public void onError(String message) {
                setProcessingAction(false, message);
            }
        });
    }

    private void setProcessingAction(boolean processing, String status) {
        processingPaymentAction = processing;
        statusTextView.setText(status);
        updateManagerActions();
    }

    private boolean isPlaceholderPayment(Payment payment) {
        String paymentId = safe(payment.getId());
        String bookingId = safe(payment.getBookingId());
        String transactionId = payment.getTransactionId() == null ? "" : payment.getTransactionId().trim();
        return !bookingId.equals("-") && paymentId.equals(bookingId) && (transactionId.isEmpty() || "-".equals(transactionId));
    }

    private void loadRelatedDetails(Payment payment, Booking booking) {
        if (booking == null) {
            loadUser(payment.getUserId());
            return;
        }
        loadCabin(booking.getCabinId());
        loadRoomType(booking.getRoomTypeId());
        loadUser(payment.getUserId());
    }

    private void loadCabin(String cabinId) {
        if (cabinId == null || cabinId.trim().isEmpty()) {
            return;
        }
        cabinService.getCabinById(cabinId, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                cabinName = safe(cabin.getName());
                renderInvoiceContent();
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void loadRoomType(String roomTypeId) {
        if (roomTypeId == null || roomTypeId.trim().isEmpty()) {
            return;
        }
        roomTypeService.getRoomTypeById(roomTypeId, new SupabaseCallback<RoomType>() {
            @Override
            public void onSuccess(RoomType roomType) {
                String size = roomType.getSize() == null || roomType.getSize().trim().isEmpty()
                        ? roomType.getSizeM2() + " m2"
                        : roomType.getSize();
                String beds = roomType.getBedSummary() == null || roomType.getBedSummary().trim().isEmpty()
                        ? roomType.getBeds()
                        : roomType.getBedSummary();
                roomLabel = safe(roomType.getName()) + " · " + size + " · " + safe(beds);
                renderInvoiceContent();
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void loadUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        authService.getUserById(userId, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                String name = user.getFullName() == null || user.getFullName().trim().isEmpty()
                        ? "Khách hàng"
                        : user.getFullName().trim();
                String email = user.getEmail() == null || user.getEmail().trim().isEmpty()
                        ? safe(userId)
                        : user.getEmail().trim();
                userLabel = name + " · " + email;
                renderInvoiceContent();
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String paidAtLabel(Payment payment) {
        return payment.getPaidAt() == null || payment.getPaidAt().trim().isEmpty()
                ? "Chưa xác nhận"
                : payment.getPaidAt();
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
