package com.example.hotel_booking_app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Rate;

import java.util.ArrayList;
import java.util.List;

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
        holder.ratingTextView.setText(rate.getRating() + "/5");
        holder.commentTextView.setText(rate.getComment());
    }

    @Override
    public int getItemCount() {
        return rates.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView ratingTextView;
        private final TextView commentTextView;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ratingTextView = itemView.findViewById(R.id.text_review_rating);
            commentTextView = itemView.findViewById(R.id.text_review_comment);
        }
    }
}
