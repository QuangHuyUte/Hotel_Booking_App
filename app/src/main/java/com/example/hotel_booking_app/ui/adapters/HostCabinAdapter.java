package com.example.hotel_booking_app.ui.adapters;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
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
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.util.ArrayList;
import java.util.List;

public class HostCabinAdapter extends RecyclerView.Adapter<HostCabinAdapter.HostCabinViewHolder> {
    public interface HostCabinListener {
        void onSelect(Cabin cabin);

        void onEdit(Cabin cabin);

        void onDuplicate(Cabin cabin);

        void onDelete(Cabin cabin);
    }

    private final List<Cabin> cabins = new ArrayList<>();
    private final HostCabinListener listener;

    public HostCabinAdapter(HostCabinListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Cabin> newCabins) {
        cabins.clear();
        if (newCabins != null) {
            cabins.addAll(newCabins);
        }
        notifyDataSetChanged();
    }

    public void upsert(Cabin cabin) {
        if (cabin == null || cabin.getId() == null) {
            return;
        }
        for (int i = 0; i < cabins.size(); i++) {
            if (cabin.getId().equals(cabins.get(i).getId())) {
                cabins.set(i, cabin);
                notifyItemChanged(i);
                return;
            }
        }
        cabins.add(0, cabin);
        notifyItemInserted(0);
    }

    public void removeById(String cabinId) {
        if (cabinId == null) {
            return;
        }
        for (int i = 0; i < cabins.size(); i++) {
            if (cabinId.equals(cabins.get(i).getId())) {
                cabins.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public HostCabinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_host_cabin, parent, false);
        return new HostCabinViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HostCabinViewHolder holder, int position) {
        Cabin cabin = cabins.get(position);
        holder.nameTextView.setText(cabin.getName());
        holder.locationTextView.setText(cabin.getLocation() == null || cabin.getLocation().trim().isEmpty()
                ? "Chưa cập nhật vị trí"
                : cabin.getLocation());
        holder.detailTextView.setText(managerRoomSummary(cabin));
        double finalPrice = cabin.displayPrice();
        holder.priceTextView.setText(priceLabel(holder, cabin, finalPrice));
        if (cabin.getDiscount() > 0) {
            holder.discountBadgeTextView.setVisibility(View.VISIBLE);
            holder.discountBadgeTextView.setText("-" + PriceUtils.formatUsd(cabin.getDiscount()));
        } else {
            holder.discountBadgeTextView.setVisibility(View.GONE);
        }
        Glide.with(holder.itemView.getContext()).load(cabin.getImage()).centerCrop().into(holder.imageView);
        holder.itemView.setOnClickListener(view -> listener.onSelect(cabin));
        holder.editButton.setOnClickListener(view -> listener.onEdit(cabin));
        holder.duplicateButton.setOnClickListener(view -> listener.onDuplicate(cabin));
        holder.deleteButton.setOnClickListener(view -> listener.onDelete(cabin));
    }

    @Override
    public int getItemCount() {
        return cabins.size();
    }

    static class HostCabinViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView nameTextView;
        private final TextView locationTextView;
        private final TextView detailTextView;
        private final TextView priceTextView;
        private final TextView discountBadgeTextView;
        private final Button editButton;
        private final Button duplicateButton;
        private final Button deleteButton;

        HostCabinViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_cabin);
            nameTextView = itemView.findViewById(R.id.text_cabin_name);
            locationTextView = itemView.findViewById(R.id.text_cabin_location);
            detailTextView = itemView.findViewById(R.id.text_cabin_detail);
            priceTextView = itemView.findViewById(R.id.text_cabin_price);
            discountBadgeTextView = itemView.findViewById(R.id.text_discount_badge);
            editButton = itemView.findViewById(R.id.button_edit);
            duplicateButton = itemView.findViewById(R.id.button_duplicate);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }
    }

    private CharSequence priceLabel(HostCabinViewHolder holder, Cabin cabin, double finalPrice) {
        if (cabin.getMatchedRoomType() != null || (cabin.getRoomTypes() != null && !cabin.getRoomTypes().isEmpty())) {
            return "Từ " + PriceUtils.formatUsd(finalPrice) + " / đêm";
        }
        if (cabin.getDiscount() <= 0) {
            return PriceUtils.formatUsd(finalPrice) + " / đêm";
        }

        String discounted = PriceUtils.formatUsd(finalPrice);
        String original = PriceUtils.formatUsd(cabin.getRegularPrice());
        String full = discounted + "  " + original + " / đêm";
        SpannableString spannable = new SpannableString(full);
        int start = discounted.length() + 2;
        int end = start + original.length();
        spannable.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(0.82f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(holder.itemView.getContext().getColor(R.color.muted)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private String managerRoomSummary(Cabin cabin) {
        if (cabin.getRoomTypes() == null || cabin.getRoomTypes().isEmpty()) {
            return "Chưa tải được loại phòng. Bấm tab hotel này để xem room, hoặc Sửa hotel để chỉnh thông tin tổng quan.";
        }
        int totalRooms = 0;
        int maxGuests = 0;
        for (RoomType roomType : cabin.getRoomTypes()) {
            totalRooms += Math.max(0, roomType.getTotalRooms());
            maxGuests = Math.max(maxGuests, roomType.effectiveMaxAdults());
        }
        RoomType cheapest = cabin.getMatchedRoomType();
        String base = cabin.getRoomTypes().size() + " loại phòng · " + totalRooms + " phòng";
        if (cheapest != null) {
            base += " · từ " + cheapest.displayName();
        }
        if (maxGuests > 0) {
            base += " · tối đa " + maxGuests + " người lớn";
        }
        return base;
    }
}
