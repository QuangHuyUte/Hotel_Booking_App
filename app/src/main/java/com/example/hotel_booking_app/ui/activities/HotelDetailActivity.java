package com.example.hotel_booking_app.ui.activities;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.models.Wishlist;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AmenityService;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.services.WishlistService;
import com.example.hotel_booking_app.ui.adapters.ReviewAdapter;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class HotelDetailActivity extends AppCompatActivity {
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
    private LinearLayout roomTypesContainer;
    private LinearLayout bookingPanel;
    private FrameLayout mapPreviewContainer;
    private ReviewAdapter reviewAdapter;
    private CabinService cabinService;
    private WishlistService wishlistService;
    private BookingService bookingService;
    private RoomTypeService roomTypeService;
    private SessionManager sessionManager;
    private String cabinId;
    private String hostId;
    private Cabin currentCabin;
    private RoomType selectedRoomType;
    private boolean isFavorite;
    private String selectedCheckIn = "";
    private String selectedCheckOut = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_detail);

        cabinId = getIntent().getStringExtra(AppConstants.EXTRA_CABIN_ID);
        cabinService = new CabinService();
        wishlistService = new WishlistService();
        bookingService = new BookingService();
        roomTypeService = new RoomTypeService();
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
        amenitiesContainer = findViewById(R.id.container_amenities);
        roomTypesContainer = findViewById(R.id.container_room_types);
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
        mapButton.setOnClickListener(view -> openGoogleMaps());
        mapPreviewContainer.setOnClickListener(view -> openFullMap());
        favoriteButton.setOnClickListener(view -> toggleFavorite());
        favoriteButton.setText("");
        chatButton.setText("");
        chatButton.setContentDescription("Nhắn tin cho khách sạn");
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
        selectedCheckIn = safe(getIntent().getStringExtra("checkIn"), today.plusDays(1).toString());
        selectedCheckOut = safe(getIntent().getStringExtra("checkOut"), today.plusDays(2).toString());
        LocalDate checkIn = parseDate(selectedCheckIn, today.plusDays(1));
        LocalDate checkOut = parseDate(selectedCheckOut, checkIn.plusDays(1));
        if (!checkOut.isAfter(checkIn)) {
            checkOut = checkIn.plusDays(1);
        }
        selectedCheckIn = checkIn.toString();
        selectedCheckOut = checkOut.toString();
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
                priceTextView.setText(PriceUtils.formatUsd(PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount())) + " / đêm");
                locationTextView.setText("Vị trí: " + safe(cabin.getLocation(), "Serein Stay"));
                capacityTextView.setText("Sức chứa: tối đa " + cabin.getMaxCapacity() + " khách");
                descriptionTextView.setText(safe(cabin.getDescription(), "Một kỳ nghỉ yên tĩnh với trải nghiệm đặt phòng nhẹ nhàng."));
                guestSummaryTextView.setText("1 phòng - tối đa " + cabin.getMaxCapacity() + " khách");
                hostTextView.setText("Quản lý: đang tải...");
                renderMapPreview(cabin);
                loadHost(cabin.getHostId());
                loadAmenities(cabin);
                loadRoomTypes(cabin);
                Glide.with(HotelDetailActivity.this).load(cabin.getImage()).centerCrop().into(imageView);
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void loadHost(String hostId) {
        if (hostId == null || hostId.trim().isEmpty()) {
            hostTextView.setText("Quản lý: Serein Stay Support");
            return;
        }
        new AuthService().getUserById(hostId, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                hostTextView.setText("Quản lý: " + user.getFullName());
            }

            @Override
            public void onError(String message) {
                hostTextView.setText("Quản lý: " + hostId);
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
        amenitiesTitleTextView.setText("Amenities");
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

    private void loadRoomTypes(Cabin cabin) {
        roomTypeService.getRoomTypesForCabin(cabin.getId(), new SupabaseCallback<List<RoomType>>() {
            @Override
            public void onSuccess(List<RoomType> roomTypes) {
                cabin.setRoomTypes(roomTypes);
                String preferredRoomTypeId = getIntent().getStringExtra(AppConstants.EXTRA_ROOM_TYPE_ID);
                selectedRoomType = null;
                for (RoomType roomType : roomTypes) {
                    if (preferredRoomTypeId != null && preferredRoomTypeId.equals(roomType.getId())) {
                        selectedRoomType = roomType;
                        break;
                    }
                }
                if (selectedRoomType == null && !roomTypes.isEmpty()) {
                    selectedRoomType = roomTypes.get(0);
                }
                renderRoomTypes(roomTypes);
                updateSelectedRoomSummary();
            }

            @Override
            public void onError(String message) {
                roomTypesContainer.removeAllViews();
                TextView fallback = new TextView(HotelDetailActivity.this);
                fallback.setText("Chưa có loại phòng để hiển thị.");
                fallback.setTextColor(getColor(R.color.booking_muted));
                roomTypesContainer.addView(fallback);
            }
        });
    }

    private void renderRoomTypes(List<RoomType> roomTypes) {
        roomTypesContainer.removeAllViews();
        if (roomTypes == null || roomTypes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Khách sạn này chưa cấu hình loại phòng.");
            empty.setTextColor(getColor(R.color.booking_muted));
            roomTypesContainer.addView(empty);
            return;
        }
        for (RoomType roomType : roomTypes) {
            TextView row = new TextView(this);
            row.setText(roomType.displayName()
                    + " · " + roomType.sizeLabel()
                    + " · " + roomType.bedLabel()
                    + "\nTối đa " + roomType.effectiveMaxAdults()
                    + " người lớn · " + roomType.effectiveBedCount()
                    + " giường · " + roomType.getTotalRooms()
                    + " phòng · " + PriceUtils.formatUsd(roomType.getBasePrice()) + " / đêm"
                    + (roomType.hasLivingRoom() ? "\nCó phòng khách riêng" : ""));
            row.setTextColor(getColor(R.color.booking_text));
            row.setTextSize(14f);
            row.setLineSpacing(dp(2), 1f);
            row.setBackgroundResource(roomType == selectedRoomType ? R.drawable.bg_booking_secondary : R.drawable.bg_profile_dark_row);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setOnClickListener(view -> {
                selectedRoomType = roomType;
                renderRoomTypes(roomTypes);
                updateSelectedRoomSummary();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(8);
            roomTypesContainer.addView(row, params);
        }
    }

    private void updateSelectedRoomSummary() {
        if (selectedRoomType == null) {
            return;
        }
        priceTextView.setText(PriceUtils.formatUsd(selectedRoomType.getBasePrice()) + " / đêm");
        capacityTextView.setText("Phòng: " + selectedRoomType.displayName()
                + " · tối đa " + selectedRoomType.effectiveMaxAdults()
                + " người lớn · " + selectedRoomType.effectiveBedCount() + " giường");
        guestSummaryTextView.setText("1 phòng · " + selectedRoomType.sizeLabel() + " · " + selectedRoomType.bedLabel());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void renderMapPreview(Cabin cabin) {
        mapPreviewContainer.removeAllViews();
        if (cabin != null) {
            WebView mapView = new WebView(this);
            mapView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            WebSettings settings = mapView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            mapView.setWebViewClient(new WebViewClient());
            mapView.setOnTouchListener((view, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    openFullMap();
                }
                return true;
            });
            mapPreviewContainer.addView(mapView);
            mapView.loadDataWithBaseURL(
                    "https://leafletjs.com/",
                    singleHotelMapHtml(cabin),
                    "text/html",
                    "UTF-8",
                    null
            );

            FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            hintParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            hintParams.topMargin = dp(12);
            mapPreviewContainer.addView(mapHintTextView, hintParams);
            mapHintTextView.setText("Chạm để xem chỗ nghỉ gần đây - " + resolveCityLabel(cabin));
            return;
        }

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

        mapHintTextView.setText("View map " + resolveCityLabel(cabin));
    }

    private String singleHotelMapHtml(Cabin cabin) {
        double lat = cabin.getLatitude() == 0 ? cityLat(cabin) : cabin.getLatitude();
        double lng = cabin.getLongitude() == 0 ? cityLng(cabin) : cabin.getLongitude();
        String price = PriceUtils.formatUsd(PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount()));
        return "<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>html,body,#map{height:100%;margin:0;background:#d8e4ed;}"
                + ".leaflet-control-container{display:none}.pin{position:relative;background:#064ea8;color:#fff;border:2px solid #fff;border-radius:5px;"
                + "padding:6px 10px;font:800 15px Arial;box-shadow:0 2px 8px rgba(0,0,0,.35);white-space:nowrap}"
                + ".pin:after{content:'';position:absolute;left:50%;bottom:-8px;transform:translateX(-50%);"
                + "border-left:7px solid transparent;border-right:7px solid transparent;border-top:8px solid #fff}</style>"
                + "</head><body><div id='map'></div><script>"
                + "var map=L.map('map',{zoomControl:false,dragging:false,scrollWheelZoom:false,doubleClickZoom:false,touchZoom:false}).setView(["
                + lat + "," + lng + "],15);"
                + "L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{maxZoom:20}).addTo(map);"
                + "L.marker([" + lat + "," + lng + "],{icon:L.divIcon({className:'',html:'<div class=\"pin\">" + js(price) + "</div>',iconAnchor:[34,34]})}).addTo(map);"
                + "</script></body></html>";
    }

    private void loadReviews() {
        cabinService.getRates(cabinId, new SupabaseCallback<List<Rate>>() {
            @Override
            public void onSuccess(List<Rate> rates) {
                reviewAdapter.submitList(rates);
                statusTextView.setText("Đánh giá: " + rates.size() + " - sẵn sàng đặt phòng");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Không tải được đánh giá: " + message);
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
                availabilityTextView.setText("Không tải được tình trạng phòng: " + message);
            }
        });
    }

    private void openBooking() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt phòng.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SignInActivity.class);
            intent.putExtra("returnToBooking", true);
            intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabinId);
            intent.putExtra("checkIn", selectedCheckIn);
            intent.putExtra("checkOut", selectedCheckOut);
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(this, BookingCreateActivity.class);
        intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabinId);
        if (selectedRoomType != null) {
            intent.putExtra(AppConstants.EXTRA_ROOM_TYPE_ID, selectedRoomType.getId());
        }
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
            statusTextView.setText("Vui lòng chọn ngày trong tương lai.");
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
                statusTextView.setText("Ngày trả phòng phải sau ngày nhận phòng.");
                return;
            }
            selectedCheckOut = date.toString();
        }
        updateDateViews();
    }

    private void updateDateViews() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.US);
        checkInTextView.setText("Nhận phòng\n" + formatDate(selectedCheckIn, formatter));
        checkOutTextView.setText("Trả phòng\n" + formatDate(selectedCheckOut, formatter));
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
            Toast.makeText(this, "Vui lòng đăng nhập để nhắn tin cho khách sạn.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SignInActivity.class));
            return;
        }
        if (hostId == null || hostId.trim().isEmpty()) {
            statusTextView.setText("Opening support chat...");
            new AuthService().getSupportUser(new SupabaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    hostId = user.getId();
                    Intent intent = new Intent(HotelDetailActivity.this, ChatThreadActivity.class);
                    intent.putExtra(ChatThreadActivity.EXTRA_HOST_ID, hostId);
                    intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabinId);
                    startActivity(intent);
                }

                @Override
                public void onError(String message) {
                    statusTextView.setText("Chưa tìm thấy tài khoản hỗ trợ.");
                    Toast.makeText(HotelDetailActivity.this, "Chưa tìm thấy tài khoản hỗ trợ", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        Intent intent = new Intent(this, ChatThreadActivity.class);
        intent.putExtra(ChatThreadActivity.EXTRA_HOST_ID, hostId);
        intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabinId);
        startActivity(intent);
    }

    private void openFullMap() {
        if (currentCabin == null) {
            statusTextView.setText("Vị trí khách sạn vẫn đang tải.");
            return;
        }
        Intent intent = new Intent(this, HotelMapActivity.class);
        intent.putExtra(AppConstants.EXTRA_CABIN_ID, currentCabin.getId());
        intent.putExtra("destination", resolveCityLabel(currentCabin));
        intent.putExtra("checkIn", selectedCheckIn);
        intent.putExtra("checkOut", selectedCheckOut);
        startActivity(intent);
    }

    private void openGoogleMaps() {
        if (currentCabin == null) {
            statusTextView.setText("Vị trí khách sạn vẫn đang tải.");
            return;
        }
        double lat = currentCabin.getLatitude() == 0 ? cityLat(currentCabin) : currentCabin.getLatitude();
        double lng = currentCabin.getLongitude() == 0 ? cityLng(currentCabin) : currentCabin.getLongitude();
        String query = lat + "," + lng + "(" + currentCabin.getName() + ")";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) == null) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng));
        }
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
        favoriteButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline,
                0,
                0
        );
        favoriteButton.setContentDescription(favorite ? "Remove saved stay" : "Save stay");
    }

    private void loadWishlistCount() {
        wishlistService.getWishlistForCabin(cabinId, new SupabaseCallback<List<Wishlist>>() {
            @Override
            public void onSuccess(List<Wishlist> wishlists) {
                wishlistTextView.setText("Đã lưu: " + wishlists.size());
            }

            @Override
            public void onError(String message) {
                wishlistTextView.setText("Chưa tải được lượt lưu");
            }
        });
    }

    private void toggleFavorite() {
        if (!sessionManager.isLoggedIn() || sessionManager.getAuthAccessToken() == null) {
            statusTextView.setText("Vui lòng đăng nhập lại trước khi lưu chỗ nghỉ.");
            return;
        }
        if (isFavorite) {
            wishlistService.removeFromWishlist(sessionManager.getUserId(), cabinId, new SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    Toast.makeText(HotelDetailActivity.this, "Đã bỏ khỏi danh sách lưu", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(HotelDetailActivity.this, "Đã lưu chỗ nghỉ", Toast.LENGTH_SHORT).show();
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
        Intent intent = new Intent(this, AdminHotelFormActivity.class);
        intent.putExtra(AdminHotelFormActivity.EXTRA_CABIN_ID, cabinId);
        startActivity(intent);
    }

    private void duplicateCabin() {
        if (currentCabin == null) {
            statusTextView.setText("Khách sạn vẫn đang tải.");
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
                Toast.makeText(HotelDetailActivity.this, "Đã nhân bản khách sạn", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HotelDetailActivity.this, HostHotelDashboardActivity.class);
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
                Toast.makeText(HotelDetailActivity.this, "Đã xóa khách sạn", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HotelDetailActivity.this, HostHotelDashboardActivity.class);
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
            return "Ho Chi Minh City";
        }
        if (location.toLowerCase(Locale.US).contains("vung tau")) {
            return "Vung Tau";
        }
        if (location.toLowerCase(Locale.US).contains("ha noi") || location.toLowerCase(Locale.US).contains("hanoi")) {
            return "Hanoi";
        }
        return "Vietnam";
    }

    private double cityLat(Cabin cabin) {
        String city = resolveCityLabel(cabin);
        if (city.equals("Vung Tau")) {
            return 10.4114;
        }
        if (city.equals("Hanoi")) {
            return 21.0278;
        }
        return 10.7769;
    }

    private double cityLng(Cabin cabin) {
        String city = resolveCityLabel(cabin);
        if (city.equals("Vung Tau")) {
            return 107.1362;
        }
        if (city.equals("Hanoi")) {
            return 105.8342;
        }
        return 106.7009;
    }

    private String js(String value) {
        return safe(value, "")
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ");
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
            return "Phòng nào cũng mát mẻ dễ chịu";
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
