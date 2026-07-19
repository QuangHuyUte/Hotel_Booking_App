package com.example.hotel_booking_app.ui.adapters;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.util.ArrayList;
import java.util.List;

public class CabinAdapter extends RecyclerView.Adapter<CabinAdapter.CabinViewHolder> {
    public interface OnCabinClickListener {
        void onCabinClick(Cabin cabin);
    }

    private final List<Cabin> cabins = new ArrayList<>();
    private final OnCabinClickListener listener;

    public CabinAdapter(OnCabinClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Cabin> newCabins) {
        cabins.clear();
        if (newCabins != null) {
            cabins.addAll(newCabins);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CabinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cabin, parent, false);
        return new CabinViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CabinViewHolder holder, int position) {
        Cabin cabin = cabins.get(position);
        holder.nameTextView.setText(cabin.getName());
        holder.locationTextView.setText("-> Tap to reserve");
        holder.priceTextView.setText(priceLabel(holder, cabin));
        holder.capacityTextView.setText("\uD83D\uDC65  " + cabin.getMaxCapacity() + " guests");
        Glide.with(holder.itemView.getContext())
                .load(cabin.getImage())
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imageView);
        holder.itemView.setOnClickListener(view -> listener.onCabinClick(cabin));
    }

    @Override
    public int getItemCount() {
        return cabins.size();
    }

    static class CabinViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView nameTextView;
        private final TextView locationTextView;
        private final TextView priceTextView;
        private final TextView capacityTextView;

        CabinViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_cabin);
            nameTextView = itemView.findViewById(R.id.text_cabin_name);
            locationTextView = itemView.findViewById(R.id.text_cabin_location);
            priceTextView = itemView.findViewById(R.id.text_cabin_price);
            capacityTextView = itemView.findViewById(R.id.text_cabin_capacity);
        }
    }

    private CharSequence priceLabel(CabinViewHolder holder, Cabin cabin) {
        double finalPrice = PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount());
        if (cabin.getDiscount() <= 0) {
            return PriceUtils.formatUsd(finalPrice) + " / night";
        }

        String discounted = PriceUtils.formatUsd(finalPrice);
        String original = PriceUtils.formatUsd(cabin.getRegularPrice());
        String full = discounted + "  " + original + " / night";
        SpannableString spannable = new SpannableString(full);
        int start = discounted.length() + 2;
        int end = start + original.length();
        spannable.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(0.82f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(holder.itemView.getContext().getColor(R.color.muted)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }
}
