package com.example.hotel_booking_app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Rate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private final List<Rate> rates = new ArrayList<>();

    public void submitList(List<Rate> newRates) {
        rates.clear();
        if (newRates != null) {
            rates.addAll(newRates);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Rate rate = rates.get(position);
        String userName = userDisplayName(rate.getUserId());
        holder.avatarTextView.setText(initials(userName));
        holder.userTextView.setText(userName);
        holder.timeTextView.setText(formatTime(rate.getCreatedAt()));
        holder.ratingTextView.setText(rate.getRating() + "/5");
        holder.commentTextView.setText(rate.getComment() == null || rate.getComment().trim().isEmpty()
                ? "Khách đã để lại đánh giá tích cực cho kỳ nghỉ này."
                : rate.getComment().trim());

        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(18f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(Math.min(position, 6) * 35L)
                .setDuration(220)
                .start();
    }

    @Override
    public int getItemCount() {
        return rates.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatarTextView;
        private final TextView userTextView;
        private final TextView timeTextView;
        private final TextView ratingTextView;
        private final TextView commentTextView;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarTextView = itemView.findViewById(R.id.text_review_avatar);
            userTextView = itemView.findViewById(R.id.text_review_user);
            timeTextView = itemView.findViewById(R.id.text_review_time);
            ratingTextView = itemView.findViewById(R.id.text_review_rating);
            commentTextView = itemView.findViewById(R.id.text_review_comment);
        }
    }

    private String userDisplayName(String userId) {
        if (userId == null) {
            return "Khách lưu trú";
        }
        if (userId.endsWith("000000000101")) {
            return "Alice Nguyen";
        }
        if (userId.endsWith("000000000102")) {
            return "Bao Tran";
        }
        if (userId.endsWith("000000000103")) {
            return "Chi Pham";
        }
        if (userId.endsWith("000000000104")) {
            return "David Le";
        }
        if (userId.endsWith("000000000105")) {
            return "Eve Hoang";
        }
        return "Khách lưu trú";
    }

    private String initials(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            return "K";
        }
        String[] parts = clean.split("\\s+");
        if (parts.length == 1) {
            return clean.substring(0, 1).toUpperCase(Locale.US);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.US);
    }

    private String formatTime(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return "Vừa đánh giá";
        }
        String normalized = createdAt.trim().replace("Z", "").replace('T', ' ');
        int dotIndex = normalized.indexOf('.');
        if (dotIndex > 0) {
            normalized = normalized.substring(0, dotIndex);
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return "Đã gửi " + dateTime.format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy"));
        } catch (Exception ignored) {
            return "Đã gửi " + normalized;
        }
    }
}
