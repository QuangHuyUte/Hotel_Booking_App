package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Rate;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.models.Wishlist;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AmenityService;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.WishlistService;
import com.example.hotel_booking_app.ui.adapters.ReviewAdapter;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.List;

public class CabinDetailActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView titleTextView;
    private TextView detailsTextView;
    private TextView hostTextView;
    private TextView amenitiesTextView;
    private TextView wishlistTextView;
    private TextView availabilityTextView;
    private TextView statusTextView;
    private Button favoriteButton;
    private Button chatButton;
    private Button bookButton;
    private Button adminEditButton;
    private Button adminDuplicateButton;
    private Button adminDeleteButton;
    private LinearLayout adminActionsContainer;
    private ReviewAdapter reviewAdapter;
    private CabinService cabinService;
    private WishlistService wishlistService;
    private SessionManager sessionManager;
    private String cabinId;
    private String hostId;
    private Cabin currentCabin;
    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cabin_detail);

        cabinId = getIntent().getStringExtra(AppConstants.EXTRA_CABIN_ID);
        cabinService = new CabinService();
        wishlistService = new WishlistService();
        sessionManager = new SessionManager(this);
        imageView = findViewById(R.id.image_cabin);
        titleTextView = findViewById(R.id.text_title);
        detailsTextView = findViewById(R.id.text_details);
        hostTextView = findViewById(R.id.text_host);
        amenitiesTextView = findViewById(R.id.text_amenities);
        wishlistTextView = findViewById(R.id.text_wishlist_count);
        availabilityTextView = findViewById(R.id.text_availability);
        statusTextView = findViewById(R.id.text_status);
        favoriteButton = findViewById(R.id.button_favorite);
        chatButton = findViewById(R.id.button_chat);
        adminActionsContainer = findViewById(R.id.container_admin_actions);
        adminEditButton = findViewById(R.id.button_admin_edit);
        adminDuplicateButton = findViewById(R.id.button_admin_duplicate);
        adminDeleteButton = findViewById(R.id.button_admin_delete);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        bookButton = findViewById(R.id.button_book);
        RecyclerView reviewsRecyclerView = findViewById(R.id.recycler_reviews);

        reviewAdapter = new ReviewAdapter();
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(reviewAdapter);

        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        bookButton.setOnClickListener(view -> {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Please log in to reserve a cabin.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            Intent intent = new Intent(this, BookingActivity.class);
            intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabinId);
            startActivity(intent);
        });
        chatButton.setOnClickListener(view -> openChat());
        favoriteButton.setOnClickListener(view -> toggleFavorite());
        adminEditButton.setOnClickListener(view -> openEditCabin());
        adminDuplicateButton.setOnClickListener(view -> duplicateCabin());
        adminDeleteButton.setOnClickListener(view -> deleteCabin());
        renderRoleActions();

        loadCabin();
        loadAvailability();
        loadFavoriteState();
        loadReviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavoriteState();
    }

    private void loadCabin() {
        cabinService.getCabinById(cabinId, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                currentCabin = cabin;
                hostId = cabin.getHostId();
                titleTextView.setText(cabin.getName());
                detailsTextView.setText(
                        "Location\n" + safe(cabin.getLocation(), "Serein Stay retreat")
                                + "\n\nNightly price\n" + PriceUtils.formatUsd(PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount())) + " / night"
                                + "\n\nCapacity\nMax " + cabin.getMaxCapacity() + " guests"
                                + "\n\nDescription\n" + safe(cabin.getDescription(), "A quiet cabin surrounded by nature.")
                );
                loadHost(cabin.getHostId());
                loadAmenities(cabin);
                Glide.with(CabinDetailActivity.this).load(cabin.getImage()).centerCrop().into(imageView);
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void loadHost(String hostId) {
        if (hostId == null || hostId.trim().isEmpty()) {
            hostTextView.setText("Host: Serein Stay Support");
            chatButton.setEnabled(true);
            return;
        }
        new AuthService().getUserById(hostId, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                hostTextView.setText("Host: " + user.getFullName());
            }

            @Override
            public void onError(String message) {
                hostTextView.setText("Host: " + hostId);
            }
        });
    }

    private void loadAmenities(Cabin cabin) {
        new AmenityService().getAmenityNamesForCabin(cabin.getId(), new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String names) {
                amenitiesTextView.setText("Amenities: " + (names == null || names.trim().isEmpty() ? cabin.getAmenities() : names));
            }

            @Override
            public void onError(String message) {
                amenitiesTextView.setText("Amenities: " + cabin.getAmenities());
            }
        });
    }

    private void loadReviews() {
        cabinService.getRates(cabinId, new SupabaseCallback<List<Rate>>() {
            @Override
            public void onSuccess(List<Rate> rates) {
                reviewAdapter.submitList(rates);
                statusTextView.setText("Reviews: " + rates.size());
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Could not load reviews: " + message);
            }
        });
    }

    private void loadAvailability() {
        new BookingService().getAvailabilitySummary(cabinId, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String summary) {
                availabilityTextView.setText(summary);
            }

            @Override
            public void onError(String message) {
                availabilityTextView.setText("Could not load availability: " + message);
            }
        });
    }

    private void openChat() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in to message the host.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (hostId == null || hostId.trim().isEmpty()) {
            statusTextView.setText("Opening Serein Stay support chat...");
            new AuthService().getSupportUser(new SupabaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    hostId = user.getId();
                    Intent intent = new Intent(CabinDetailActivity.this, ChatActivity.class);
                    intent.putExtra(ChatActivity.EXTRA_HOST_ID, hostId);
                    intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabinId);
                    startActivity(intent);
                }

                @Override
                public void onError(String message) {
                    statusTextView.setText("No admin/support account is available for chat yet.");
                    Toast.makeText(CabinDetailActivity.this, "No support account found", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_HOST_ID, hostId);
        intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabinId);
        startActivity(intent);
    }

    private void loadFavoriteState() {
        wishlistService.isFavorite(sessionManager.getUserId(), cabinId, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean favorite) {
                isFavorite = favorite;
                favoriteButton.setText(isFavorite ? "Remove favorite" : "Add favorite");
                loadWishlistCount();
            }

            @Override
            public void onError(String message) {
                favoriteButton.setText("Add favorite");
                loadWishlistCount();
            }
        });
    }

    private void loadWishlistCount() {
        wishlistService.getWishlistForCabin(cabinId, new SupabaseCallback<List<Wishlist>>() {
            @Override
            public void onSuccess(List<Wishlist> wishlists) {
                wishlistTextView.setText("Favorites: " + wishlists.size());
            }

            @Override
            public void onError(String message) {
                wishlistTextView.setText("Favorites: not available");
            }
        });
    }

    private void toggleFavorite() {
        if (sessionManager.getUserId() == null || sessionManager.getAuthAccessToken() == null) {
            statusTextView.setText("Please log in again before using favorites.");
            return;
        }
        if (isFavorite) {
            wishlistService.removeFromWishlist(sessionManager.getUserId(), cabinId, new SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    Toast.makeText(CabinDetailActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                    isFavorite = false;
                    favoriteButton.setText("Add favorite");
                    loadWishlistCount();
                }

                @Override
                public void onError(String message) {
                    statusTextView.setText(message);
                    loadFavoriteState();
                }
            });
        } else {
            wishlistService.addToWishlist(sessionManager.getUserId(), cabinId, new SupabaseCallback<Wishlist>() {
                @Override
                public void onSuccess(Wishlist data) {
                    Toast.makeText(CabinDetailActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                    isFavorite = true;
                    favoriteButton.setText("Remove favorite");
                    loadWishlistCount();
                }

                @Override
                public void onError(String message) {
                    statusTextView.setText(message);
                    loadFavoriteState();
                }
            });
        }
    }

    private void renderRoleActions() {
        boolean adminMode = sessionManager.isHostOrAdmin();
        bookButton.setVisibility(adminMode ? View.GONE : View.VISIBLE);
        chatButton.setVisibility(adminMode ? View.GONE : View.VISIBLE);
        favoriteButton.setVisibility(adminMode ? View.GONE : View.VISIBLE);
        adminActionsContainer.setVisibility(adminMode ? View.VISIBLE : View.GONE);
    }

    private void openEditCabin() {
        Intent intent = new Intent(this, AdminCabinFormActivity.class);
        intent.putExtra(AdminCabinFormActivity.EXTRA_CABIN_ID, cabinId);
        startActivity(intent);
    }

    private void duplicateCabin() {
        if (currentCabin == null) {
            statusTextView.setText("Cabin is still loading.");
            return;
        }
        Cabin copy = new Cabin();
        copy.setName(currentCabin.getName() + " Copy");
        copy.setLocation(currentCabin.getLocation());
        copy.setRegularPrice(currentCabin.getRegularPrice());
        copy.setDiscount(currentCabin.getDiscount());
        copy.setMaxCapacity(currentCabin.getMaxCapacity());
        copy.setDescription(currentCabin.getDescription());
        copy.setImage(currentCabin.getImage());
        copy.setAmenities(currentCabin.getAmenities());
        copy.setHostId(currentCabin.getHostId() == null || currentCabin.getHostId().trim().isEmpty()
                ? sessionManager.getUserId()
                : currentCabin.getHostId());
        statusTextView.setText("Duplicating cabin...");
        cabinService.createCabin(copy, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin data) {
                Toast.makeText(CabinDetailActivity.this, "Cabin duplicated", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CabinDetailActivity.this, HostDashboardActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void deleteCabin() {
        statusTextView.setText("Deleting cabin...");
        cabinService.deleteCabin(cabinId, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                Toast.makeText(CabinDetailActivity.this, "Cabin deleted", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CabinDetailActivity.this, HostDashboardActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
