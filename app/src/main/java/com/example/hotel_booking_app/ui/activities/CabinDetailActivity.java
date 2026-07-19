package com.example.hotel_booking_app.ui.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CabinDetailActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView titleTextView;
    private TextView heroTitleTextView;
    private TextView priceTextView;
    private TextView locationTextView;
    private TextView capacityTextView;
    private TextView descriptionTextView;
    private TextView checkInTextView;
    private TextView checkOutTextView;
    private TextView guestSummaryTextView;
    private TextView amenitiesTitleTextView;
    private TextView wishlistTextView;
    private TextView availabilityTextView;
    private TextView statusTextView;
    private TextView hostTextView;
    private TextView mapHintTextView;
    private Button favoriteButton;
    private Button chatButton;
    private Button mapButton;
    private Button bookButton;
    private Button adminEditButton;
    private Button adminDuplicateButton;
    private Button adminDeleteButton;
    private LinearLayout adminActionsContainer;
    private LinearLayout amenitiesContainer;
    private LinearLayout bookingPanel;
    private FrameLayout mapPreviewContainer;
    private ReviewAdapter reviewAdapter;
    private CabinService cabinService;
    private WishlistService wishlistService;
    private BookingService bookingService;
    private SessionManager sessionManager;
    private String cabinId;
    private String hostId;
    private Cabin currentCabin;
    private boolean isFavorite;
    private String selectedCheckIn = "";
    private String selectedCheckOut = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cabin_detail);

        cabinId = getIntent().getStringExtra(AppConstants.EXTRA_CABIN_ID);
        cabinService = new CabinService();
        wishlistService = new WishlistService();
        bookingService = new BookingService();
        sessionManager = new SessionManager(this);

        imageView = findViewById(R.id.image_cabin);
        titleTextView = findViewById(R.id.text_title);
        heroTitleTextView = findViewById(R.id.text_details);
        priceTextView = findViewById(R.id.text_price);
        locationTextView = findViewById(R.id.text_location);
        capacityTextView = findViewById(R.id.text_capacity);
        descriptionTextView = findViewById(R.id.text_description);
        checkInTextView = findViewById(R.id.text_check_in);
        checkOutTextView = findViewById(R.id.text_check_out);
        guestSummaryTextView = findViewById(R.id.text_guest_summary);
        amenitiesTitleTextView = findViewById(R.id.text_amenities);
        wishlistTextView = findViewById(R.id.text_wishlist_count);
        availabilityTextView = findViewById(R.id.text_availability);
        statusTextView = findViewById(R.id.text_status);
        hostTextView = findViewById(R.id.text_host);
        mapHintTextView = findViewById(R.id.text_map_hint);
        favoriteButton = findViewById(R.id.button_favorite);
        chatButton = findViewById(R.id.button_chat);
        mapButton = findViewById(R.id.button_map);
        bookingPanel = findViewById(R.id.panel_booking_cta);
        adminActionsContainer = findViewById(R.id.container_admin_actions);
        adminEditButton = findViewById(R.id.button_admin_edit);
        adminDuplicateButton = findViewById(R.id.button_admin_duplicate);
        adminDeleteButton = findViewById(R.id.button_admin_delete);
        mapPreviewContainer = findViewById(R.id.map_preview);
        Button backButton = findViewById(R.id.button_back);
        bookButton = findViewById(R.id.button_book);
        RecyclerView reviewsRecyclerView = findViewById(R.id.recycler_reviews);

        reviewAdapter = new ReviewAdapter();
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(reviewAdapter);

        backButton.setOnClickListener(view -> finish());
        bookButton.setOnClickListener(view -> openBooking());
        chatButton.setOnClickListener(view -> openChat());
        mapButton.setOnClickListener(view -> openMap());
        mapPreviewContainer.setOnClickListener(view -> openMap());
        favoriteButton.setOnClickListener(view -> toggleFavorite());
        favoriteButton.setText("");
        chatButton.setText("");
        chatButton.setContentDescription("Nhắn chủ nhà");
        adminEditButton.setOnClickListener(view -> openEditCabin());
        adminDuplicateButton.setOnClickListener(view -> duplicateCabin());
        adminDeleteButton.setOnClickListener(view -> deleteCabin());
        checkInTextView.setOnClickListener(view -> showDatePicker(true));
        checkOutTextView.setOnClickListener(view -> showDatePicker(false));

        initDefaultDates();
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

    private void initDefaultDates() {
        LocalDate today = LocalDate.now();
        selectedCheckIn = today.plusDays(1).toString();
        selectedCheckOut = today.plusDays(2).toString();
        updateDateViews();
    }

    private void loadCabin() {
        cabinService.getCabinById(cabinId, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                currentCabin = cabin;
                hostId = cabin.getHostId();
                titleTextView.setText(cabin.getName());
                heroTitleTextView.setText(cabin.getName());
                priceTextView.setText(PriceUtils.formatUsd(PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount())) + " / night");
                locationTextView.setText("Địa điểm: " + safe(cabin.getLocation(), "Hotel Booking App"));
                capacityTextView.setText("Sức chứa: tối đa " + cabin.getMaxCapacity() + " khách");
                descriptionTextView.setText(safe(cabin.getDescription(), "A quiet stay with a calm booking flow."));
                guestSummaryTextView.setText("1 phòng · tối đa " + cabin.getMaxCapacity() + " khách");
                hostTextView.setText("Chủ nhà: đang tải...");
                renderMapPreview(cabin);
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
            hostTextView.setText("Chủ nhà: Hotel Booking App Support");
            return;
        }
        new AuthService().getUserById(hostId, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                hostTextView.setText("Chủ nhà: " + user.getFullName());
            }

            @Override
            public void onError(String message) {
                hostTextView.setText("Chủ nhà: " + hostId);
            }
        });
    }

    private void loadAmenities(Cabin cabin) {
        new AmenityService().getAmenityNamesForCabin(cabin.getId(), new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String names) {
                renderAmenities(names == null || names.trim().isEmpty() ? cabin.getAmenities() : names);
            }

            @Override
            public void onError(String message) {
                renderAmenities(cabin.getAmenities());
            }
        });
    }

    private void renderAmenities(String amenities) {
        amenitiesContainer.removeAllViews();
        amenitiesTitleTextView.setText("Tiện nghi");
        String value = amenities == null || amenities.trim().isEmpty()
                ? "WiFi, Breakfast, Parking, Balcony"
                : amenities;
        String[] items = value.split(",");
        for (int i = 0; i < items.length; i++) {
            String label = items[i].trim();
            if (label.isEmpty()) {
                continue;
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_profile_dark_row);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));

            TextView icon = new TextView(this);
            int[] colors = {
                    Color.parseColor("#284A68"),
                    Color.parseColor("#8D5C20"),
                    Color.parseColor("#1F5E5A"),
                    Color.parseColor("#6A4C93"),
                    Color.parseColor("#82544A"),
                    Color.parseColor("#3F6F8F")
            };
            GradientDrawable circle = new GradientDrawable();
            circle.setColor(colors[i % colors.length]);
            circle.setCornerRadius(dp(18));
            icon.setBackground(circle);
            icon.setGravity(Gravity.CENTER);
            icon.setText(amenityBadge(label));
            icon.setTextColor(Color.WHITE);
            icon.setTypeface(null, Typeface.BOLD);
            icon.setTextSize(11f);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(36), dp(36));
            row.addView(icon, iconParams);

            LinearLayout textBlock = new LinearLayout(this);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textParams.setMargins(dp(12), 0, 0, 0);

            TextView name = new TextView(this);
            name.setText(label);
            name.setTextColor(getColor(R.color.booking_text));
            name.setTextSize(15f);
            name.setTypeface(null, Typeface.BOLD);

            TextView hint = new TextView(this);
            hint.setText(amenityHint(label));
            hint.setTextColor(getColor(R.color.booking_muted));
            hint.setTextSize(12f);

            textBlock.addView(name);
            textBlock.addView(hint);
            row.addView(textBlock, textParams);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = dp(8);
            amenitiesContainer.addView(row, rowParams);
        }
    }

    private void renderMapPreview(Cabin cabin) {
        mapPreviewContainer.removeAllViews();

        ImageView mapImage = new ImageView(this);
        mapImage.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        mapImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mapImage.setImageResource(R.drawable.vietnam_official_map);
        mapPreviewContainer.addView(mapImage);

        View overlay = new View(this);
        overlay.setBackgroundColor(0x140F2233);
        mapPreviewContainer.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        hintParams.topMargin = dp(16);
        mapPreviewContainer.addView(mapHintTextView, hintParams);

        TextView cityBadge = new TextView(this);
        cityBadge.setBackgroundResource(R.drawable.bg_map_marker);
        cityBadge.setTextColor(Color.WHITE);
        cityBadge.setTypeface(null, Typeface.BOLD);
        cityBadge.setTextSize(12f);
        cityBadge.setText(resolveCityLabel(cabin));
        cityBadge.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cityParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(34)
        );
        cityParams.leftMargin = dp(18);
        cityParams.topMargin = dp(18);
        mapPreviewContainer.addView(cityBadge, cityParams);

        View dot = new View(this);
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setColor(Color.WHITE);
        dotDrawable.setCornerRadius(dp(7));
        dot.setBackground(dotDrawable);
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(14), dp(14));
        dotParams.leftMargin = dp(112);
        dotParams.topMargin = dp(92);
        mapPreviewContainer.addView(dot, dotParams);

        TextView locationBadge = new TextView(this);
        locationBadge.setBackgroundResource(R.drawable.bg_booking_badge_warm);
        locationBadge.setTextColor(Color.WHITE);
        locationBadge.setTypeface(null, Typeface.BOLD);
        locationBadge.setTextSize(11f);
        locationBadge.setText(safe(cabin.getLocation(), cabin.getName()));
        locationBadge.setGravity(Gravity.CENTER);
        locationBadge.setSingleLine(true);
        FrameLayout.LayoutParams locationParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(34)
        );
        locationParams.leftMargin = dp(54);
        locationParams.topMargin = dp(74);
        mapPreviewContainer.addView(locationBadge, locationParams);

        mapHintTextView.setText("Xem bản đồ " + resolveCityLabel(cabin));
    }

    private void loadReviews() {
        cabinService.getRates(cabinId, new SupabaseCallback<List<Rate>>() {
            @Override
            public void onSuccess(List<Rate> rates) {
                reviewAdapter.submitList(rates);
                statusTextView.setText("Đánh giá: " + rates.size() + " · sẵn sàng đặt");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Could not load reviews: " + message);
            }
        });
    }

    private void loadAvailability() {
        bookingService.getAvailabilitySummary(cabinId, new SupabaseCallback<String>() {
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

    private void openBooking() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in to reserve a cabin.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabinId);
        intent.putExtra("checkIn", selectedCheckIn);
        intent.putExtra("checkOut", selectedCheckOut);
        startActivity(intent);
    }

    private void showDatePicker(boolean checkIn) {
        LocalDate today = LocalDate.now();
        LocalDate initial = checkIn
                ? parseDate(selectedCheckIn, today.plusDays(1))
                : parseDate(selectedCheckOut, parseDate(selectedCheckIn, today.plusDays(1)).plusDays(1));
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (picker, year, month, dayOfMonth) -> handleDateSelected(checkIn, LocalDate.of(year, month + 1, dayOfMonth)),
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        );
        dialog.getDatePicker().setMinDate(today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.show();
    }

    private void handleDateSelected(boolean checkIn, LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            statusTextView.setText("Choose a future date.");
            return;
        }
        if (checkIn) {
            selectedCheckIn = date.toString();
            LocalDate checkout = parseDate(selectedCheckOut, date.plusDays(1));
            if (!checkout.isAfter(date)) {
                checkout = date.plusDays(1);
            }
            selectedCheckOut = checkout.toString();
        } else {
            LocalDate start = parseDate(selectedCheckIn, today.plusDays(1));
            if (!date.isAfter(start)) {
                statusTextView.setText("Check-out must be after check-in.");
                return;
            }
            selectedCheckOut = date.toString();
        }
        updateDateViews();
    }

    private void updateDateViews() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM", new Locale("vi", "VN"));
        checkInTextView.setText("Check-in\n" + formatDate(selectedCheckIn, formatter));
        checkOutTextView.setText("Check-out\n" + formatDate(selectedCheckOut, formatter));
    }

    private String formatDate(String isoDate, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(isoDate).format(formatter);
        } catch (Exception e) {
            return isoDate == null ? "-" : isoDate;
        }
    }

    private LocalDate parseDate(String isoDate, LocalDate fallback) {
        try {
            if (isoDate == null || isoDate.trim().isEmpty()) {
                return fallback;
            }
            return LocalDate.parse(isoDate);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void openChat() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in to message the host.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (hostId == null || hostId.trim().isEmpty()) {
            statusTextView.setText("Opening support chat...");
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
                    statusTextView.setText("No support account found.");
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

    private void openMap() {
        if (currentCabin == null) {
            statusTextView.setText("Cabin location is still loading.");
            return;
        }
        Intent intent = new Intent(this, SearchMapActivity.class);
        intent.putExtra("destination", resolveCityLabel(currentCabin));
        intent.putExtra("checkIn", selectedCheckIn);
        intent.putExtra("checkOut", selectedCheckOut);
        intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabinId);
        startActivity(intent);
    }

    private void loadFavoriteState() {
        if (!sessionManager.isLoggedIn()) {
            setFavoriteButtonState(false);
            loadWishlistCount();
            return;
        }
        wishlistService.isFavorite(sessionManager.getUserId(), cabinId, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean favorite) {
                isFavorite = favorite;
                setFavoriteButtonState(isFavorite);
                loadWishlistCount();
            }

            @Override
            public void onError(String message) {
                setFavoriteButtonState(false);
                loadWishlistCount();
            }
        });
    }

    private void setFavoriteButtonState(boolean favorite) {
        favoriteButton.setText("");
        favoriteButton.setContentDescription(favorite ? "Bỏ lưu chỗ nghỉ" : "Lưu chỗ nghỉ");
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
        if (!sessionManager.isLoggedIn() || sessionManager.getAuthAccessToken() == null) {
            statusTextView.setText("Please log in again before using favorites.");
            return;
        }
        if (isFavorite) {
            wishlistService.removeFromWishlist(sessionManager.getUserId(), cabinId, new SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    Toast.makeText(CabinDetailActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                    isFavorite = false;
                    setFavoriteButtonState(false);
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
                    setFavoriteButtonState(true);
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
        bookingPanel.setVisibility(adminMode ? View.GONE : View.VISIBLE);
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

    private String resolveCityLabel(Cabin cabin) {
        String location = safe(cabin.getLocation(), "");
        if (location.toLowerCase(Locale.US).contains("ho chi minh")) {
            return "TP. Ho Chi Minh";
        }
        if (location.toLowerCase(Locale.US).contains("vung tau")) {
            return "Vung Tau";
        }
        if (location.toLowerCase(Locale.US).contains("ha noi")) {
            return "Ha Noi";
        }
        return "Vietnam";
    }

    private String amenityBadge(String label) {
        String value = label.toLowerCase(Locale.US);
        if (value.contains("wifi")) {
            return "Wi";
        }
        if (value.contains("breakfast") || value.contains("coffee")) {
            return "Br";
        }
        if (value.contains("parking")) {
            return "P";
        }
        if (value.contains("kitchen")) {
            return "K";
        }
        if (value.contains("air")) {
            return "AC";
        }
        if (value.contains("pool")) {
            return "Po";
        }
        if (value.contains("bath")) {
            return "Ba";
        }
        if (value.contains("balcony")) {
            return "Bl";
        }
        if (value.contains("view")) {
            return "V";
        }
        return value.isEmpty() ? "A" : value.substring(0, 1).toUpperCase(Locale.US);
    }

    private String amenityHint(String label) {
        String value = label.toLowerCase(Locale.US);
        if (value.contains("wifi")) {
            return "Fast and stable connection";
        }
        if (value.contains("breakfast") || value.contains("coffee")) {
            return "Morning service can be added";
        }
        if (value.contains("parking")) {
            return "Easy access for your car";
        }
        if (value.contains("kitchen")) {
            return "Cook and store snacks";
        }
        if (value.contains("air")) {
            return "Cool comfort in every room";
        }
        if (value.contains("pool")) {
            return "Relax and recharge";
        }
        if (value.contains("bath")) {
            return "Private soak after the trip";
        }
        if (value.contains("balcony")) {
            return "Fresh air and a wider view";
        }
        if (value.contains("view")) {
            return "A cleaner look at the area";
        }
        return "Included in this stay";
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
