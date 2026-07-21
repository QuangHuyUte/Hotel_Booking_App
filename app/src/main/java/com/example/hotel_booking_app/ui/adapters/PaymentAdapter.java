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
import com.example.hotel_booking_app.data.models.Payment;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {
    public interface OnPaymentClickListener {
        void onPaymentClick(Payment payment);
    }

    private final List<Payment> payments = new ArrayList<>();
    private final Map<String, Booking> bookingCache = new HashMap<>();
    private final Map<String, Cabin> cabinCache = new HashMap<>();
    private final BookingService bookingService = new BookingService();
    private final CabinService cabinService = new CabinService();
    private final OnPaymentClickListener listener;

    public PaymentAdapter(OnPaymentClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Payment> newPayments) {
        payments.clear();
        if (newPayments != null) {
            payments.addAll(newPayments);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment payment = payments.get(position);
        holder.boundBookingId = payment.getBookingId();
        holder.titleTextView.setText(PriceUtils.formatUsd(payment.getAmount()));
        holder.statusTextView.setText(statusLabel(payment, null));
        holder.statusTextView.setBackgroundResource(statusBackground(payment, null));
        holder.imageView.setImageResource(R.drawable.ic_launcher_background);
        holder.detailTextView.setText(
                "Đặt phòng\nĐang tải thông tin chỗ nghỉ..."
                        + "\n\nHình thức\n" + translatePaymentMethod(payment.getMethod())
                        + "\n\nNhà cung cấp\n" + translatePaymentProvider(payment.getProvider())
                        + "\n\nThanh toán lúc\n" + safe(payment.getPaidAt())
        );
        bindBooking(holder, payment);
        holder.itemView.setOnClickListener(view -> listener.onPaymentClick(payment));
        holder.invoiceButton.setOnClickListener(view -> listener.onPaymentClick(payment));
    }

    private void bindBooking(PaymentViewHolder holder, Payment payment) {
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
                    holder.detailTextView.setText(
                            "Đặt phòng\nChưa có thông tin chỗ nghỉ"
                                    + "\n\nHình thức\n" + translatePaymentMethod(payment.getMethod())
                                    + "\n\nNhà cung cấp\n" + translatePaymentProvider(payment.getProvider())
                                    + "\n\nThanh toán lúc\n" + safe(payment.getPaidAt())
                    );
                }
            }
        });
    }

    private void renderBooking(PaymentViewHolder holder, Payment payment, Booking booking) {
        holder.statusTextView.setText(statusLabel(payment, booking));
        holder.statusTextView.setBackgroundResource(statusBackground(payment, booking));
        Cabin cachedCabin = cabinCache.get(booking.getCabinId());
        if (cachedCabin != null) {
            renderPaymentDetails(holder, payment, booking, cachedCabin.getName());
            renderCabinImage(holder, cachedCabin);
            return;
        }
        cabinService.getCabinById(booking.getCabinId(), new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                cabinCache.put(booking.getCabinId(), cabin);
                if (payment.getBookingId().equals(holder.boundBookingId)) {
                    renderPaymentDetails(holder, payment, booking, cabin.getName());
                    renderCabinImage(holder, cabin);
                }
            }

            @Override
            public void onError(String message) {
                renderPaymentDetails(holder, payment, booking, "Đặt phòng khách sạn");
            }
        });
    }

    private void renderPaymentDetails(PaymentViewHolder holder, Payment payment, Booking booking, String cabinName) {
        holder.detailTextView.setText(
                "Đặt phòng\n" + cabinName + "\n" + booking.getStartDate() + " -> " + booking.getEndDate()
                        + "\n\nHình thức\n" + translatePaymentMethod(payment.getMethod())
                        + "\n\nNhà cung cấp\n" + translatePaymentProvider(payment.getProvider())
                        + "\n\nThanh toán lúc\n" + safe(payment.getPaidAt())
        );
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

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
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
        if ("pending".equalsIgnoreCase(status)) {
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
        return payments.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView statusTextView;
        private final TextView detailTextView;
        private final Button invoiceButton;
        private final ImageView imageView;
        private String boundBookingId;

        PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_payment_cabin);
            titleTextView = itemView.findViewById(R.id.text_payment_title);
            statusTextView = itemView.findViewById(R.id.text_payment_status);
            detailTextView = itemView.findViewById(R.id.text_payment_detail);
            invoiceButton = itemView.findViewById(R.id.button_invoice);
        }
    }
}
