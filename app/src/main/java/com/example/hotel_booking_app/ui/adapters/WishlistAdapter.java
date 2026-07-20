package com.example.hotel_booking_app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Wishlist;

import java.util.ArrayList;
import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {
    public interface OnWishlistClickListener {
        void onWishlistClick(Wishlist wishlist);
    }

    private final List<Wishlist> wishlists = new ArrayList<>();
    private final OnWishlistClickListener listener;

    public WishlistAdapter(OnWishlistClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Wishlist> newWishlists) {
        wishlists.clear();
        if (newWishlists != null) {
            wishlists.addAll(newWishlists);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wishlist, parent, false);
        return new WishlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        Wishlist wishlist = wishlists.get(position);
        holder.titleTextView.setText("Favorite cabin");
        holder.subtitleTextView.setText(wishlist.getCabinId());
        holder.itemView.setOnClickListener(view -> listener.onWishlistClick(wishlist));
    }

    @Override
    public int getItemCount() {
        return wishlists.size();
    }

    static class WishlistViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView subtitleTextView;

        WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_wishlist_title);
            subtitleTextView = itemView.findViewById(R.id.text_wishlist_subtitle);
        }
    }
}
