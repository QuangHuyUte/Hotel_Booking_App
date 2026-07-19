package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Wishlist;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.WishlistService;
import com.example.hotel_booking_app.ui.adapters.CabinAdapter;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyWishlistActivity extends AppCompatActivity {
    private TextView statusTextView;
    private CabinAdapter adapter;
    private WishlistService wishlistService;
    private CabinService cabinService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_wishlist);

        statusTextView = findViewById(R.id.text_status);
        Button backButton = findViewById(R.id.button_back);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);
        LinearLayout searchTab = findViewById(R.id.nav_cabins);
        LinearLayout bookingsTab = findViewById(R.id.nav_bookings);
        LinearLayout messagesTab = findViewById(R.id.nav_messages);
        LinearLayout profileTab = findViewById(R.id.nav_personal);
        RecyclerView recyclerView = findViewById(R.id.recycler_wishlist);
        wishlistService = new WishlistService();
        cabinService = new CabinService();
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        adapter = new CabinAdapter(cabin -> {
            Intent intent = new Intent(this, CabinDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabin.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        backButton.setOnClickListener(view -> finish());
        bottomBackButton.setOnClickListener(view -> finish());
        searchTab.setOnClickListener(view -> startActivity(new Intent(this, CabinListActivity.class)));
        bookingsTab.setOnClickListener(view -> startActivity(new Intent(this, MyBookingsActivity.class)));
        messagesTab.setOnClickListener(view -> startActivity(new Intent(this, MessagesActivity.class)));
        profileTab.setOnClickListener(view -> startActivity(new Intent(this, PersonalActivity.class)));
        loadWishlist();
    }

    private void loadWishlist() {
        statusTextView.setText("Đang tải wishlist...");
        wishlistService.getWishlist(sessionManager.getUserId(), new SupabaseCallback<List<Wishlist>>() {
            @Override
            public void onSuccess(List<Wishlist> wishlists) {
                loadWishlistCabins(wishlists);
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void loadWishlistCabins(List<Wishlist> wishlists) {
        if (wishlists.isEmpty()) {
            adapter.submitList(new ArrayList<>());
            statusTextView.setText("Bạn chưa favorite cabin nào.");
            return;
        }

        cabinService.getCabins(new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                List<String> cabinIds = wishlists.stream().map(Wishlist::getCabinId).collect(Collectors.toList());
                List<Cabin> favoriteCabins = cabins.stream()
                        .filter(cabin -> cabinIds.contains(cabin.getId()))
                        .collect(Collectors.toList());
                adapter.submitList(favoriteCabins);
                statusTextView.setText("Bạn có " + favoriteCabins.size() + " cabin yêu thích.");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }
}
