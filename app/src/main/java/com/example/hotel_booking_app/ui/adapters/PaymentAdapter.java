package com.example.hotel_booking_app.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_PAYMENT = 1;

    public interface OnPaymentClickListener {
        void onPaymentClick(Payment payment);
    }

    public interface OnPaymentAcceptListener {
        void onPaymentAccept(Payment payment);
    }

    public interface OnPaymentStatusFilterListener {
        void onPaymentStatusFilter(String status);
    }

    public interface OnCabinFilterListener {
        void onCabinFilter(String cabinId);
    }

    private final List<Payment> payments = new ArrayList<>();
    private final Map<String, Booking> bookingCache = new HashMap<>();
    private final Map<String, Cabin> cabinCache = new HashMap<>();
    private final Map<String, RoomType> roomTypeCache = new HashMap<>();
    private final Map<String, User> userCache = new HashMap<>();
    private final BookingService bookingService = new BookingService();
    private final CabinService cabinService = new CabinService();
    private final RoomTypeService roomTypeService = new RoomTypeService();
    private final AuthService authService = new AuthService();
    private final OnPaymentClickListener listener;
    private final OnPaymentAcceptListener acceptListener;
    private final OnPaymentStatusFilterListener statusFilterListener;
    private final OnCabinFilterListener cabinFilterListener;
    private final View.OnClickListener backClickListener;
    private final List<Cabin> managerCabins = new ArrayList<>();
    private boolean managerMode;
    private String headerTitle = "Lịch sử giao dịch";
    private String headerStatus = "";
    private String selectedStatus = AppConstants.PAYMENT_PENDING;
    private String selectedCabinId;
    private int pendingCount;
    private int finishedCount;

    public PaymentAdapter(
            OnPaymentClickListener listener,
            OnPaymentAcceptListener acceptListener,
            OnPaymentStatusFilterListener statusFilterListener,
            OnCabinFilterListener cabinFilterListener,
            View.OnClickListener backClickListener
    ) {
        this.listener = listener;
        this.acceptListener = acceptListener;
        this.statusFilterListener = statusFilterListener;
        this.cabinFilterListener = cabinFilterListener;
        this.backClickListener = backClickListener;
    }

    public void setManagerMode(boolean managerMode) {
        this.managerMode = managerMode;
        notifyDataSetChanged();
    }

    public void submitList(List<Payment> newPayments) {
        payments.clear();
        if (newPayments != null) {
            payments.addAll(newPayments);
        }
        notifyDataSetChanged();
    }

    public void setHeaderState(
            String title,
            String status,
            String selectedStatus,
            int pendingCount,
            int finishedCount,
            List<Cabin> cabins,
            String selectedCabinId
    ) {
        this.headerTitle = title;
        this.headerStatus = status;
        this.selectedStatus = selectedStatus;
        this.pendingCount = pendingCount;
        this.finishedCount = finishedCount;
        this.selectedCabinId = selectedCabinId;
        managerCabins.clear();
        if (cabins != null) {
            managerCabins.addAll(cabins);
        }
        notifyItemChanged(0);
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_history_header, parent, false);
            return new PaymentViewHolder(view, true);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment, parent, false);
        return new PaymentViewHolder(view, false);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            bindHeader(holder);
            return;
        }
        Payment payment = payments.get(position - 1);
        holder.boundBookingId = payment.getBookingId();
        holder.titleTextView.setText(PriceUtils.formatUsd(payment.getAmount()));
        holder.statusTextView.setText(statusLabel(payment, null));
        holder.statusTextView.setBackgroundResource(statusBackground(payment, null));
        holder.imageView.setImageResource(R.drawable.ic_launcher_background);
        holder.detailTextView.setText(basePaymentText(payment));
        bindAcceptButton(holder, payment, null);
        bindBooking(holder, payment);
        holder.itemView.setOnClickListener(view -> listener.onPaymentClick(payment));
        holder.invoiceButton.setOnClickListener(view -> listener.onPaymentClick(payment));
    }

    private void bindHeader(PaymentViewHolder holder) {
        holder.headerTitleTextView.setText(headerTitle);
        holder.headerStatusTextView.setText(headerStatus);
        holder.headerBackButton.setOnClickListener(backClickListener);
        holder.pendingButton.setText("Pending (" + pendingCount + ")");
        holder.finishedButton.setText("Finished (" + finishedCount + ")");
        boolean pendingSelected = AppConstants.PAYMENT_PENDING.equalsIgnoreCase(selectedStatus);
        holder.pendingButton.setTextColor(holder.itemView.getContext().getColor(pendingSelected ? R.color.black : R.color.ink));
        holder.finishedButton.setTextColor(holder.itemView.getContext().getColor(pendingSelected ? R.color.ink : R.color.black));
        holder.pendingButton.setBackgroundResource(pendingSelected ? R.drawable.bg_button_primary : R.drawable.bg_panel);
        holder.finishedButton.setBackgroundResource(pendingSelected ? R.drawable.bg_panel : R.drawable.bg_button_primary);
        holder.pendingButton.setOnClickListener(view -> statusFilterListener.onPaymentStatusFilter(AppConstants.PAYMENT_PENDING));
        holder.finishedButton.setOnClickListener(view -> statusFilterListener.onPaymentStatusFilter(AppConstants.PAYMENT_PAID));
        renderHotelTabs(holder);
    }

    private void renderHotelTabs(PaymentViewHolder holder) {
        if (!managerMode) {
            holder.hotelTabsScrollView.setVisibility(View.GONE);
            return;
        }
        holder.hotelTabsScrollView.setVisibility(View.VISIBLE);
        holder.hotelTabsContainer.removeAllViews();
        addHotelTab(holder.hotelTabsContainer, "Tất cả", null);
        for (Cabin cabin : managerCabins) {
            addHotelTab(holder.hotelTabsContainer, shortHotelName(cabin), cabin.getId());
        }
    }

    private void addHotelTab(LinearLayout container, String label, String cabinId) {
        Context context = container.getContext();
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        button.setTextSize(13);
        boolean selected = (selectedCabinId == null && cabinId == null)
                || (selectedCabinId != null && selectedCabinId.equals(cabinId));
        button.setTextColor(context.getColor(selected ? R.color.black : R.color.ink));
        button.setBackgroundResource(selected ? R.drawable.bg_button_primary : R.drawable.bg_manager_search);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(context, 42)
        );
        params.setMargins(0, 0, dp(context, 8), 0);
        button.setLayoutParams(params);
        button.setOnClickListener(view -> cabinFilterListener.onCabinFilter(cabinId));
        container.addView(button);
    }

    private void bindBooking(PaymentViewHolder holder, Payment payment) {
        if (payment.getBookingId() == null || payment.getBookingId().trim().isEmpty()) {
            return;
        }
        Booking cachedBooking = bookingCache.get(payment.getBookingId());
        if (cachedBooking != null) {
            renderBooking(holder, payment, cachedBooking);
            return;
        }
        bookingService.getBookingById(payment.getBookingId(), new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                bookingCache.put(payment.getBookingId(), booking);
                if (payment.getBookingId().equals(holder.boundBookingId)) {
                    renderBooking(holder, payment, booking);
                }
            }

            @Override
            public void onError(String message) {
                if (payment.getBookingId().equals(holder.boundBookingId)) {
                    holder.detailTextView.setText(basePaymentText(payment));
                    bindAcceptButton(holder, payment, null);
                }
            }
        });
    }

    private void renderBooking(PaymentViewHolder holder, Payment payment, Booking booking) {
        holder.statusTextView.setText(statusLabel(payment, booking));
        holder.statusTextView.setBackgroundResource(statusBackground(payment, booking));
        bindAcceptButton(holder, payment, booking);

        Cabin cachedCabin = cabinCache.get(booking.getCabinId());
        if (cachedCabin != null) {
            renderPaymentDetails(holder, payment, booking, cachedCabin);
            renderCabinImage(holder, cachedCabin);
            return;
        }
        cabinService.getCabinById(booking.getCabinId(), new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                cabinCache.put(booking.getCabinId(), cabin);
                if (payment.getBookingId().equals(holder.boundBookingId)) {
                    renderPaymentDetails(holder, payment, booking, cabin);
                    renderCabinImage(holder, cabin);
                }
            }

            @Override
            public void onError(String message) {
                if (payment.getBookingId().equals(holder.boundBookingId)) {
                    renderPaymentDetails(holder, payment, booking, null);
                }
            }
        });
    }

    private void renderPaymentDetails(PaymentViewHolder holder, Payment payment, Booking booking, Cabin cabin) {
        String cabinName = cabin == null ? "Đặt phòng khách sạn" : cabin.getName();
        String roomLabel = roomLabel(booking);
        String userLabel = userLabel(payment);
        holder.detailTextView.setText(paymentDetailText(payment, booking, cabinName, roomLabel, userLabel));
        loadRoomTypeIfNeeded(holder, payment, booking, cabinName, userLabel);
        loadUserIfNeeded(holder, payment, booking, cabinName, roomLabel);
    }

    private void loadRoomTypeIfNeeded(PaymentViewHolder holder, Payment payment, Booking booking, String cabinName, String userLabel) {
        String roomTypeId = booking.getRoomTypeId();
        if (roomTypeId == null || roomTypeId.trim().isEmpty() || roomTypeCache.containsKey(roomTypeId)) {
            return;
        }
        roomTypeService.getRoomTypeById(roomTypeId, new SupabaseCallback<RoomType>() {
            @Override
            public void onSuccess(RoomType roomType) {
                roomTypeCache.put(roomTypeId, roomType);
                if (payment.getBookingId().equals(holder.boundBookingId)) {
                    holder.detailTextView.setText(paymentDetailText(payment, booking, cabinName, roomLabel(booking), userLabel));
                }
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void loadUserIfNeeded(PaymentViewHolder holder, Payment payment, Booking booking, String cabinName, String roomLabel) {
        String userId = payment.getUserId();
        if (userId == null || userId.trim().isEmpty() || userCache.containsKey(userId)) {
            return;
        }
        authService.getUserById(userId, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                userCache.put(userId, user);
                if (payment.getBookingId().equals(holder.boundBookingId)) {
                    holder.detailTextView.setText(paymentDetailText(payment, booking, cabinName, roomLabel, userLabel(payment)));
                }
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private String paymentDetailText(Payment payment, Booking booking, String cabinName, String roomLabel, String userLabel) {
        return safe(cabinName)
                + "\n" + roomLabel
                + "\n" + safe(booking.getStartDate()) + " -> " + safe(booking.getEndDate())
                + " · " + Math.max(1, booking.getNumRooms()) + " phòng · " + booking.getNumGuests() + " khách"
                + "\n" + userLabel
                + "\nTạo lúc: " + safe(payment.getCreatedAt())
                + " · " + translatePaymentMethod(payment.getMethod());
    }

    private String basePaymentText(Payment payment) {
        return "Đang tải thông tin khách sạn..."
                + "\n" + userLabel(payment)
                + "\nTạo lúc: " + safe(payment.getCreatedAt())
                + " · " + translatePaymentMethod(payment.getMethod());
    }

    private String roomLabel(Booking booking) {
        String roomTypeId = booking.getRoomTypeId();
        if (roomTypeId == null || roomTypeId.trim().isEmpty()) {
            return "Chưa chọn loại phòng";
        }
        RoomType roomType = roomTypeCache.get(roomTypeId);
        if (roomType == null) {
            return "Đang tải loại phòng...";
        }
        String size = roomType.getSize() == null || roomType.getSize().trim().isEmpty()
                ? roomType.getSizeM2() + " m2"
                : roomType.getSize();
        String beds = roomType.getBedSummary() == null || roomType.getBedSummary().trim().isEmpty()
                ? roomType.getBeds()
                : roomType.getBedSummary();
        return roomType.getName() + " · " + size + " · " + safe(beds);
    }

    private String userLabel(Payment payment) {
        String userId = payment.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return "-";
        }
        User user = userCache.get(userId);
        if (user == null) {
            return shortId(userId);
        }
        String name = user.getFullName() == null || user.getFullName().trim().isEmpty()
                ? "Khách hàng"
                : user.getFullName().trim();
        String email = user.getEmail() == null || user.getEmail().trim().isEmpty()
                ? shortId(userId)
                : user.getEmail().trim();
        return name + " · " + email;
    }

    private void bindAcceptButton(PaymentViewHolder holder, Payment payment, Booking booking) {
        boolean paid = (booking != null && booking.isPaid())
                || AppConstants.PAYMENT_PAID.equalsIgnoreCase(payment.getStatus());
        boolean canAccept = managerMode
                && !paid
                && AppConstants.PAYMENT_PENDING.equalsIgnoreCase(payment.getStatus())
                && acceptListener != null;
        holder.acceptButton.setVisibility(canAccept ? View.VISIBLE : View.GONE);
        holder.acceptButton.setOnClickListener(canAccept ? view -> acceptListener.onPaymentAccept(payment) : null);
    }

    private void renderCabinImage(PaymentViewHolder holder, Cabin cabin) {
        Glide.with(holder.itemView.getContext())
                .load(cabin.getImage())
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imageView);
    }

    private String statusLabel(Payment payment, Booking booking) {
        if (booking != null && booking.isPaid()) {
            return "ĐÃ THANH TOÁN";
        }
        String status = safe(payment.getStatus());
        return status.equals("-") ? "ĐANG CHỜ" : translatePaymentStatus(status);
    }

    private int statusBackground(Payment payment, Booking booking) {
        if ((booking != null && booking.isPaid()) || AppConstants.PAYMENT_PAID.equalsIgnoreCase(payment.getStatus())) {
            return R.drawable.bg_booking_badge_green;
        }
        if (AppConstants.PAYMENT_FAILED.equalsIgnoreCase(payment.getStatus())
                || AppConstants.PAYMENT_REFUNDED.equalsIgnoreCase(payment.getStatus())) {
            return R.drawable.bg_booking_badge_red;
        }
        return R.drawable.bg_booking_badge_warm;
    }

    private String paidAtLabel(Payment payment) {
        String paidAt = payment.getPaidAt();
        return paidAt == null || paidAt.trim().isEmpty() ? "Chưa xác nhận" : paidAt;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String shortId(String value) {
        if (value == null || value.length() <= 8) {
            return safe(value);
        }
        return value.substring(0, 8) + "...";
    }

    private String translatePaymentStatus(String status) {
        if (AppConstants.PAYMENT_PAID.equalsIgnoreCase(status)) {
            return "ĐÃ THANH TOÁN";
        }
        if (AppConstants.PAYMENT_FAILED.equalsIgnoreCase(status)) {
            return "THẤT BẠI";
        }
        if (AppConstants.PAYMENT_REFUNDED.equalsIgnoreCase(status)) {
            return "ĐÃ HOÀN TIỀN";
        }
        if (AppConstants.PAYMENT_PENDING.equalsIgnoreCase(status)) {
            return "ĐANG CHỜ";
        }
        return status.toUpperCase(Locale.US);
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

    @Override
    public int getItemCount() {
        return payments.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_PAYMENT;
    }

    private String shortHotelName(Cabin cabin) {
        String name = safe(cabin.getName());
        return name.length() <= 22 ? name : name.substring(0, 21).trim() + "...";
    }

    private int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView statusTextView;
        private TextView detailTextView;
        private Button invoiceButton;
        private Button acceptButton;
        private ImageView imageView;
        private TextView headerTitleTextView;
        private TextView headerStatusTextView;
        private Button headerBackButton;
        private Button pendingButton;
        private Button finishedButton;
        private HorizontalScrollView hotelTabsScrollView;
        private LinearLayout hotelTabsContainer;
        private String boundBookingId;

        PaymentViewHolder(@NonNull View itemView, boolean header) {
            super(itemView);
            if (header) {
                headerBackButton = itemView.findViewById(R.id.button_back);
                headerTitleTextView = itemView.findViewById(R.id.text_payment_title);
                headerStatusTextView = itemView.findViewById(R.id.text_status);
                pendingButton = itemView.findViewById(R.id.button_pending_payments);
                finishedButton = itemView.findViewById(R.id.button_finished_payments);
                hotelTabsScrollView = itemView.findViewById(R.id.scroll_payment_hotel_tabs);
                hotelTabsContainer = itemView.findViewById(R.id.container_payment_hotel_tabs);
                return;
            }
            imageView = itemView.findViewById(R.id.image_payment_cabin);
            titleTextView = itemView.findViewById(R.id.text_payment_title);
            statusTextView = itemView.findViewById(R.id.text_payment_status);
            detailTextView = itemView.findViewById(R.id.text_payment_detail);
            invoiceButton = itemView.findViewById(R.id.button_invoice);
            acceptButton = itemView.findViewById(R.id.button_accept_payment);
        }
    }
}
