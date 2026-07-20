package com.example.hotel_booking_app.ui.adapters;

import android.text.TextUtils;
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
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {
    public interface OnBookingActionListener {
        void onPrimaryAction(Booking booking);
    }

    private final List<Booking> bookings = new ArrayList<>();
    private final Map<String, Cabin> cabinCache = new HashMap<>();
    private final CabinService cabinService = new CabinService();
    private final String actionText;
    private final OnBookingActionListener listener;

    public BookingAdapter(String actionText, OnBookingActionListener listener) {
        this.actionText = actionText;
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
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.boundCabinId = booking.getCabinId();
        holder.titleTextView.setText("Đặt phòng khách sạn");
        holder.nightsTextView.setText(booking.getNumNights() + " đêm");
        holder.dateTextView.setText(formatDate(booking.getStartDate()) + "  ->  " + formatDate(booking.getEndDate()));
        holder.priceTextView.setText("Khách: " + booking.getNumGuests() + "   |   " + PriceUtils.formatUsd(booking.getTotalPrice()) + " tổng cộng");
        holder.createdTextView.setText("Đã đặt: " + formatCreatedDate(booking.getCreatedAt()));
        holder.statusTextView.setText(statusLabel(booking));
        holder.statusTextView.setBackgroundResource(statusBackground(booking));
        holder.imageView.setImageResource(R.drawable.ic_launcher_background);

        bindCabin(holder, booking.getCabinId());

        if (TextUtils.isEmpty(actionText)) {
            String contextualAction = contextualActionText(booking);
            if (TextUtils.isEmpty(contextualAction)) {
                holder.actionButton.setVisibility(View.GONE);
            } else {
                holder.actionButton.setVisibility(View.VISIBLE);
                holder.actionButton.setText(contextualAction);
                holder.actionButton.setOnClickListener(view -> listener.onPrimaryAction(booking));
            }
        } else {
            holder.actionButton.setVisibility(View.VISIBLE);
            holder.actionButton.setText(actionText);
            holder.actionButton.setOnClickListener(view -> listener.onPrimaryAction(booking));
        }
        holder.itemView.setOnClickListener(view -> listener.onPrimaryAction(booking));
    }

    private String contextualActionText(Booking booking) {
        if (AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus()) || booking.isPaid()) {
            return "";
        }
        if (AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus())) {
            return "Xem đặt phòng đang chờ";
        }
        if (AppConstants.BOOKING_CONFIRMED.equalsIgnoreCase(booking.getStatus())) {
            return "Thanh toán";
        }
        return "";
    }

    private void bindCabin(BookingViewHolder holder, String cabinId) {
        Cabin cachedCabin = cabinCache.get(cabinId);
        if (cachedCabin != null) {
            renderCabin(holder, cachedCabin);
            return;
        }
        cabinService.getCabinById(cabinId, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                cabinCache.put(cabinId, cabin);
                if (cabinId.equals(holder.boundCabinId)) {
                    renderCabin(holder, cabin);
                }
            }

            @Override
            public void onError(String message) {
                if (cabinId.equals(holder.boundCabinId)) {
                    holder.titleTextView.setText("Khách sạn: " + cabinId);
                }
            }
        });
    }

    private void renderCabin(BookingViewHolder holder, Cabin cabin) {
        holder.titleTextView.setText(cabin.getName());
        Glide.with(holder.itemView.getContext())
                .load(cabin.getImage())
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imageView);
    }

    private String statusLabel(Booking booking) {
        if (booking.isPaid()) {
            return "ĐÃ THANH TOÁN";
        }
        String status = booking.getStatus() == null ? "" : booking.getStatus().toUpperCase(Locale.US);
        if (AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus())) {
            return "ĐANG CHỜ";
        }
        if (AppConstants.BOOKING_CONFIRMED.equalsIgnoreCase(booking.getStatus())) {
            return "TRẢ SAU";
        }
        return status.isEmpty() ? "ĐANG CHỜ" : status;
    }

    private int statusBackground(Booking booking) {
        if (AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus())) {
            return R.drawable.bg_booking_badge_red;
        }
        if (booking.isPaid()) {
            return R.drawable.bg_booking_badge_green;
        }
        if (AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus())) {
            return R.drawable.bg_booking_badge_warm;
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
        if (TextUtils.isEmpty(value)) {
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

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleTextView;
        private final TextView nightsTextView;
        private final TextView statusTextView;
        private final TextView dateTextView;
        private final TextView priceTextView;
        private final TextView createdTextView;
        private final Button actionButton;
        private String boundCabinId;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_booking_cabin);
            titleTextView = itemView.findViewById(R.id.text_booking_title);
            nightsTextView = itemView.findViewById(R.id.text_booking_nights);
            statusTextView = itemView.findViewById(R.id.text_booking_status);
            dateTextView = itemView.findViewById(R.id.text_booking_date);
            priceTextView = itemView.findViewById(R.id.text_booking_price);
            createdTextView = itemView.findViewById(R.id.text_booking_created);
            actionButton = itemView.findViewById(R.id.button_booking_action);
        }
    }
}
