package com.example.hotel_booking_app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.AdminBookingViewHolder> {
    public interface AdminBookingListener {
        void onOpen(Booking booking);
        void onPrimaryAction(Booking booking);
        void onCancel(Booking booking);
    }

    private final List<Booking> bookings = new ArrayList<>();
    private final Map<String, Cabin> cabinById;
    private final Map<String, User> userById;
    private final AdminBookingListener listener;

    public AdminBookingAdapter(Map<String, Cabin> cabinById, Map<String, User> userById, AdminBookingListener listener) {
        this.cabinById = cabinById;
        this.userById = userById;
        this.listener = listener;
    }

    public void submitList(List<Booking> newBookings) {
        bookings.clear();
        if (newBookings != null) {
            bookings.addAll(newBookings);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminBookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_booking, parent, false);
        return new AdminBookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminBookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        Cabin cabin = cabinById.get(booking.getCabinId());
        holder.titleTextView.setText(cabin == null ? "Đặt phòng khách sạn" : cabin.getName());
        holder.nightsTextView.setText(booking.getNumNights() + " đêm");
        holder.statusTextView.setText(statusLabel(booking));
        holder.statusTextView.setBackgroundResource(statusBackground(booking));
        holder.dateTextView.setText(formatDate(booking.getStartDate()) + "  ->  " + formatDate(booking.getEndDate()));
        holder.customerTextView.setText(customerLabel(booking));
        holder.priceTextView.setText("Khách: " + booking.getNumGuests() + "   |   "
                + PriceUtils.formatUsd(booking.getTotalPrice()) + " tổng cộng");
        holder.createdTextView.setText("Đã đặt: " + formatCreatedDate(booking.getCreatedAt()));
        bindImage(holder, cabin);
        bindActions(holder, booking);
        holder.itemView.setOnClickListener(view -> listener.onOpen(booking));
    }

    private void bindImage(AdminBookingViewHolder holder, Cabin cabin) {
        if (cabin == null) {
            holder.imageView.setImageResource(R.drawable.ic_launcher_background);
            return;
        }
        Glide.with(holder.itemView.getContext())
                .load(cabin.getImage())
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imageView);
    }

    private void bindActions(AdminBookingViewHolder holder, Booking booking) {
        boolean cancelled = AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus());
        if (cancelled || booking.isPaid()) {
            holder.primaryButton.setText("Mở");
            holder.primaryButton.setOnClickListener(view -> listener.onOpen(booking));
        } else if (AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus())) {
            holder.primaryButton.setText("Xác nhận");
            holder.primaryButton.setOnClickListener(view -> listener.onPrimaryAction(booking));
        } else {
            holder.primaryButton.setText("Đánh dấu đã trả");
            holder.primaryButton.setOnClickListener(view -> listener.onPrimaryAction(booking));
        }
        holder.cancelButton.setVisibility(cancelled ? View.GONE : View.VISIBLE);
        holder.cancelButton.setOnClickListener(view -> listener.onCancel(booking));
    }

    private String customerLabel(Booking booking) {
        User user = userById.get(booking.getUserId());
        if (user == null) {
            return "Khách hàng: đang tải...";
        }
        String name = user.getFullName() == null || user.getFullName().trim().isEmpty()
                ? "Khách chưa đặt tên"
                : user.getFullName().trim();
        String phone = user.getPhone() == null || user.getPhone().trim().isEmpty()
                ? ""
                : " | " + user.getPhone().trim();
        return "Khách hàng: " + name + phone;
    }

    private String statusLabel(Booking booking) {
        if (booking.isPaid()) {
            return "ĐÃ THANH TOÁN";
        }
        if (AppConstants.BOOKING_CONFIRMED.equalsIgnoreCase(booking.getStatus())) {
            return "TRẢ SAU";
        }
        String status = booking.getStatus() == null ? "" : booking.getStatus().toUpperCase(Locale.US);
        return status.isEmpty() ? "ĐANG CHỜ" : status;
    }

    private int statusBackground(Booking booking) {
        if (AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus())) {
            return R.drawable.bg_booking_badge_red;
        }
        if (booking.isPaid()) {
            return R.drawable.bg_booking_badge_green;
        }
        return R.drawable.bg_booking_badge_warm;
    }

    private String formatDate(String isoDate) {
        try {
            LocalDate date = LocalDate.parse(isoDate);
            return date.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US));
        } catch (Exception e) {
            return isoDate == null ? "-" : isoDate;
        }
    }

    private String formatCreatedDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        try {
            return OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));
            } catch (Exception secondIgnored) {
                try {
                    return LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));
                } catch (Exception e) {
                    return value;
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class AdminBookingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleTextView;
        private final TextView nightsTextView;
        private final TextView statusTextView;
        private final TextView dateTextView;
        private final TextView customerTextView;
        private final TextView priceTextView;
        private final TextView createdTextView;
        private final Button primaryButton;
        private final Button cancelButton;

        AdminBookingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_booking_cabin);
            titleTextView = itemView.findViewById(R.id.text_booking_title);
            nightsTextView = itemView.findViewById(R.id.text_booking_nights);
            statusTextView = itemView.findViewById(R.id.text_booking_status);
            dateTextView = itemView.findViewById(R.id.text_booking_date);
            customerTextView = itemView.findViewById(R.id.text_booking_customer);
            priceTextView = itemView.findViewById(R.id.text_booking_price);
            createdTextView = itemView.findViewById(R.id.text_booking_created);
            primaryButton = itemView.findViewById(R.id.button_booking_primary);
            cancelButton = itemView.findViewById(R.id.button_booking_cancel);
        }
    }
}
