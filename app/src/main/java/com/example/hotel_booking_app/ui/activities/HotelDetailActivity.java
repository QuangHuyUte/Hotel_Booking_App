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
import java.util.ArrayList;
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
    private TextView ratingMetaTextView;
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
    private RecyclerView reviewsRecyclerView;
    private Button prevReviewsButton;
    private Button nextReviewsButton;
    private TextView reviewPageTextView;
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
    private final List<Rate> allReviews = new ArrayList<>();
    private int reviewPage = 0;
    private static final int REVIEWS_PAGE_SIZE = 10;

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
        ratingMetaTextView = findViewById(R.id.text_rating_meta);
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
        reviewsRecyclerView = findViewById(R.id.recycler_reviews);
        prevReviewsButton = findViewById(R.id.button_reviews_prev);
        nextReviewsButton = findViewById(R.id.button_reviews_next);
        reviewPageTextView = findViewById(R.id.text_reviews_page);

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
        prevReviewsButton.setOnClickListener(view -> {
            if (reviewPage > 0) {
                reviewPage--;
                renderReviewPage(true);
            }
        });
        nextReviewsButton.setOnClickListener(view -> {
            int maxPage = Math.max(0, (allReviews.size() - 1) / REVIEWS_PAGE_SIZE);
            if (reviewPage < maxPage) {
                reviewPage++;
                renderReviewPage(true);
            }
        });

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
        updateDateViewsClean();
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
                bindCabinContent(cabin);
                Glide.with(HotelDetailActivity.this).load(cabin.getImage()).centerCrop().into(imageView);
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void bindCabinContent(Cabin cabin) {
        titleTextView.setText(cabin.getName());
        heroTitleTextView.setText(cabin.getName());
        priceTextView.setText(PriceUtils.formatUsd(PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount())) + " / đêm");
        ratingMetaTextView.setText(ratingLabel(cabin, 0, 0));
        locationTextView.setText("Vị trí\n" + safe(cabin.getAddress(), safe(cabin.getLocation(), "Serein Stay")));
        capacityTextView.setText("Sức chứa\nTối đa " + cabin.getMaxCapacity() + " khách");
        descriptionTextView.setText(displayDescription(cabin));
        guestSummaryTextView.setText("1 phòng · tối đa " + cabin.getMaxCapacity() + " khách");
        hostTextView.setText("Quản lý: đang tải...");
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
                renderAmenitiesClean(names == null || names.trim().isEmpty() ? cabin.getAmenities() : names);
            }

            @Override
            public void onError(String message) {
                renderAmenitiesClean(cabin.getAmenities());
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

    private void renderAmenitiesClean(String amenities) {
        amenitiesContainer.removeAllViews();
        amenitiesTitleTextView.setText("Tiện nghi nổi bật");
        String value = amenities == null || amenities.trim().isEmpty()
                ? "WiFi, Breakfast, Parking, Balcony"
                : amenities;
        String[] items = value.split(",");
        for (int i = 0; i < items.length; i++) {
            String rawLabel = items[i].trim();
            if (rawLabel.isEmpty()) {
                continue;
            }
            String label = translateAmenityLabel(rawLabel);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_detail_chip);
            row.setPadding(dp(12), dp(11), dp(12), dp(11));

            ImageView icon = iconTile(amenityIconRes(rawLabel), amenityColor(rawLabel));
            row.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

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
            hint.setText(amenityHintVi(rawLabel));
            hint.setTextColor(getColor(R.color.booking_muted));
            hint.setTextSize(12.5f);
            hint.setLineSpacing(dp(2), 1f);

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
                renderRoomTypesClean(roomTypes);
                updateSelectedRoomSummaryClean();
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

    private void renderRoomTypesClean(List<RoomType> roomTypes) {
        roomTypesContainer.removeAllViews();
        if (roomTypes == null || roomTypes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Chưa có loại phòng cho khách sạn này. Nếu bạn vừa đổi seed, hãy chạy lại seed.sql để nạp dữ liệu phòng.");
            empty.setTextColor(getColor(R.color.booking_muted));
            empty.setTextSize(13f);
            empty.setLineSpacing(dp(3), 1f);
            roomTypesContainer.addView(empty);
            return;
        }
        for (RoomType roomType : roomTypes) {
            boolean selected = roomType == selectedRoomType
                    || (selectedRoomType != null && selectedRoomType.getId() != null && selectedRoomType.getId().equals(roomType.getId()));

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(selected ? R.drawable.bg_detail_selected : R.drawable.bg_detail_chip);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);

            ImageView roomIcon = iconTile(roomIconRes(roomType), selected ? Color.parseColor("#B77A25") : Color.parseColor("#003580"));
            top.addView(roomIcon, new LinearLayout.LayoutParams(dp(38), dp(38)));

            LinearLayout titleBlock = new LinearLayout(this);
            titleBlock.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams titleBlockParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            titleBlockParams.setMargins(dp(10), 0, dp(8), 0);

            TextView title = new TextView(this);
            title.setText(roomType.displayName());
            title.setTextColor(getColor(R.color.booking_text));
            title.setTextSize(16f);
            title.setTypeface(null, Typeface.BOLD);

            TextView subTitle = new TextView(this);
            subTitle.setText(roomType.bedLabel());
            subTitle.setTextColor(getColor(R.color.booking_muted));
            subTitle.setTextSize(12.5f);
            subTitle.setMaxLines(2);

            titleBlock.addView(title);
            titleBlock.addView(subTitle);
            top.addView(titleBlock, titleBlockParams);

            TextView price = new TextView(this);
            price.setText(PriceUtils.formatUsd(roomType.getBasePrice()) + "\n/ đêm");
            price.setTextColor(getColor(R.color.booking_blue));
            price.setTextSize(14f);
            price.setTypeface(null, Typeface.BOLD);
            price.setGravity(Gravity.END);
            top.addView(price);
            card.addView(top);

            LinearLayout facts = new LinearLayout(this);
            facts.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams factParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            factParams.setMargins(0, dp(10), 0, 0);
            addRoomFact(facts, R.drawable.ic_room_size, roomType.sizeLabel());
            addRoomFact(facts, R.drawable.ic_room_users, "Tối đa " + roomType.effectiveMaxAdults() + " người lớn");
            addRoomFact(facts, R.drawable.ic_room_bed, roomType.effectiveBedCount() + " giường · còn " + roomType.getTotalRooms() + " phòng");
            if (roomType.hasLivingRoom()) {
                addRoomFact(facts, R.drawable.ic_room_size, "Có phòng khách riêng");
            }
            card.addView(facts, factParams);

            String desc = roomType.getDescription();
            if (desc != null && !desc.trim().isEmpty()) {
                TextView description = new TextView(this);
                description.setText(translateRoomDescription(desc));
                description.setTextColor(getColor(R.color.booking_muted));
                description.setTextSize(12f);
                description.setLineSpacing(dp(2), 1f);
                LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                descParams.setMargins(0, dp(6), 0, 0);
                card.addView(description, descParams);
            }

            card.setOnClickListener(view -> {
                selectedRoomType = roomType;
                renderRoomTypesClean(roomTypes);
                updateSelectedRoomSummaryClean();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(8);
            roomTypesContainer.addView(card, params);
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

    private void updateSelectedRoomSummaryClean() {
        if (selectedRoomType == null) {
            return;
        }
        priceTextView.setText(PriceUtils.formatUsd(selectedRoomType.getBasePrice()) + " / đêm");
        capacityTextView.setText("Loại phòng\n" + selectedRoomType.displayName()
                + " · tối đa " + selectedRoomType.effectiveMaxAdults()
                + " người lớn");
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

        mapHintTextView.setText("Xem bản đồ " + resolveCityLabel(cabin));
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
                + ".leaflet-control-container{display:none}.pin{position:relative;display:inline-flex;align-items:center;justify-content:center;"
                + "min-width:76px;height:30px;background:#064ea8;color:#fff;border:2px solid #fff;border-radius:8px;"
                + "padding:0 12px;font:800 14px Arial;box-sizing:border-box;box-shadow:0 4px 12px rgba(0,0,0,.32);white-space:nowrap}"
                + ".pin:after{content:'';position:absolute;left:50%;bottom:-8px;transform:translateX(-50%);"
                + "border-left:7px solid transparent;border-right:7px solid transparent;border-top:8px solid #fff}</style>"
                + "</head><body><div id='map'></div><script>"
                + "var map=L.map('map',{zoomControl:false,dragging:false,scrollWheelZoom:false,doubleClickZoom:false,touchZoom:false}).setView(["
                + lat + "," + lng + "],15);"
                + "L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{maxZoom:20}).addTo(map);"
                + "L.marker([" + lat + "," + lng + "],{icon:L.divIcon({className:'',html:'<div class=\"pin\">" + js(price) + "</div>',iconSize:[92,38],iconAnchor:[46,38]})}).addTo(map);"
                + "</script></body></html>";
    }

    private void loadReviews() {
        cabinService.getRates(cabinId, new SupabaseCallback<List<Rate>>() {
            @Override
            public void onSuccess(List<Rate> rates) {
                allReviews.clear();
                if (rates != null) {
                    allReviews.addAll(rates);
                }
                allReviews.sort((left, right) -> safe(right.getCreatedAt(), "").compareTo(safe(left.getCreatedAt(), "")));
                reviewPage = 0;
                renderReviewPage(false);
                double score = currentCabin == null ? 0 : currentCabin.getReviewScore();
                if (!allReviews.isEmpty()) {
                    int total = 0;
                    for (Rate rate : allReviews) {
                        total += rate.getRating();
                    }
                    score = Math.round(((total / (double) allReviews.size()) * 2.0) * 10.0) / 10.0;
                }
                int count = allReviews.size();
                if (ratingMetaTextView != null) {
                    ratingMetaTextView.setText(ratingLabel(currentCabin, score, count));
                }
                statusTextView.setText(count + " đánh giá · sẵn sàng đặt phòng");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Không tải được đánh giá: " + message);
            }
        });
    }

    private void renderReviewPage(boolean animated) {
        int total = allReviews.size();
        if (total == 0) {
            reviewAdapter.submitList(new ArrayList<>());
            reviewPageTextView.setText("Chưa có đánh giá");
            prevReviewsButton.setEnabled(false);
            nextReviewsButton.setEnabled(false);
            prevReviewsButton.setAlpha(0.45f);
            nextReviewsButton.setAlpha(0.45f);
            return;
        }
        int maxPage = Math.max(0, (total - 1) / REVIEWS_PAGE_SIZE);
        reviewPage = Math.max(0, Math.min(reviewPage, maxPage));
        int start = reviewPage * REVIEWS_PAGE_SIZE;
        int end = Math.min(total, start + REVIEWS_PAGE_SIZE);
        reviewAdapter.submitList(new ArrayList<>(allReviews.subList(start, end)));
        reviewPageTextView.setText("Trang " + (reviewPage + 1) + "/" + (maxPage + 1)
                + " · " + (start + 1) + "-" + end + "/" + total + " đánh giá");
        prevReviewsButton.setEnabled(reviewPage > 0);
        nextReviewsButton.setEnabled(reviewPage < maxPage);
        prevReviewsButton.setAlpha(reviewPage > 0 ? 1f : 0.45f);
        nextReviewsButton.setAlpha(reviewPage < maxPage ? 1f : 0.45f);
        if (animated && reviewsRecyclerView != null) {
            reviewsRecyclerView.setAlpha(0.35f);
            reviewsRecyclerView.setTranslationY(12f);
            reviewsRecyclerView.animate().alpha(1f).translationY(0f).setDuration(180).start();
        }
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
        updateDateViewsClean();
    }

    private void updateDateViews() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.US);
        checkInTextView.setText("Nhận phòng\n" + formatDate(selectedCheckIn, formatter));
        checkOutTextView.setText("Trả phòng\n" + formatDate(selectedCheckOut, formatter));
    }

    private void updateDateViewsClean() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.forLanguageTag("vi-VN"));
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
            statusTextView.setText("Đang mở cuộc trò chuyện hỗ trợ...");
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
        favoriteButton.setContentDescription(favorite ? "Bỏ lưu chỗ nghỉ" : "Lưu chỗ nghỉ");
    }

    private void loadWishlistCount() {
        wishlistService.getWishlistForCabin(cabinId, new SupabaseCallback<List<Wishlist>>() {
            @Override
            public void onSuccess(List<Wishlist> wishlists) {
                wishlistTextView.setText(wishlists.size() + " lưu");
            }

            @Override
            public void onError(String message) {
                wishlistTextView.setText("0 lưu");
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
        statusTextView.setText("Đang nhân bản khách sạn...");
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
        statusTextView.setText("Đang xóa khách sạn...");
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
            return "TP. Hồ Chí Minh";
        }
        if (location.toLowerCase(Locale.US).contains("vung tau")) {
            return "Vũng Tàu";
        }
        if (location.toLowerCase(Locale.US).contains("ha noi") || location.toLowerCase(Locale.US).contains("hanoi")) {
            return "Hà Nội";
        }
        if (location.toLowerCase(Locale.US).contains("da nang")) {
            return "Đà Nẵng";
        }
        if (location.toLowerCase(Locale.US).contains("da lat")) {
            return "Đà Lạt";
        }
        return "Việt Nam";
    }

    private double cityLat(Cabin cabin) {
        String city = resolveCityLabel(cabin);
        if (city.equals("Vũng Tàu")) {
            return 10.4114;
        }
        if (city.equals("Hà Nội")) {
            return 21.0278;
        }
        if (city.equals("Đà Nẵng")) {
            return 16.0471;
        }
        if (city.equals("Đà Lạt")) {
            return 11.9404;
        }
        return 10.7769;
    }

    private double cityLng(Cabin cabin) {
        String city = resolveCityLabel(cabin);
        if (city.equals("Vũng Tàu")) {
            return 107.1362;
        }
        if (city.equals("Hà Nội")) {
            return 105.8342;
        }
        if (city.equals("Đà Nẵng")) {
            return 108.2068;
        }
        if (city.equals("Đà Lạt")) {
            return 108.4583;
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

    private GradientDrawable lightSurface(boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(selected ? "#FFF3D6" : "#FFFDF8"));
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(1), Color.parseColor(selected ? "#D4A247" : "#E5D6BA"));
        return drawable;
    }

    private ImageView iconTile(int iconRes, int backgroundColor) {
        ImageView icon = new ImageView(this);
        GradientDrawable tile = new GradientDrawable();
        tile.setColor(backgroundColor);
        tile.setCornerRadius(dp(10));
        icon.setBackground(tile);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        return icon;
    }

    private void addRoomFact(LinearLayout parent, int iconRes, String text) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(3), 0, dp(3));

        ImageView icon = iconTile(iconRes, Color.parseColor("#355C7D"));
        row.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(getColor(R.color.booking_text));
        label.setTextSize(13f);
        label.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(9), 0, 0, 0);
        row.addView(label, params);
        parent.addView(row);
    }

    private int roomIconRes(RoomType roomType) {
        return roomType != null && roomType.effectiveMaxAdults() >= 3
                ? R.drawable.ic_room_users
                : R.drawable.ic_room_bed;
    }

    private int amenityIconRes(String label) {
        String value = label == null ? "" : label.toLowerCase(Locale.US);
        if (value.contains("wifi")) {
            return R.drawable.ic_amenity_wifi;
        }
        if (value.contains("breakfast") || value.contains("coffee")) {
            return R.drawable.ic_amenity_breakfast;
        }
        if (value.contains("parking")) {
            return R.drawable.ic_amenity_parking;
        }
        if (value.contains("pool")) {
            return R.drawable.ic_amenity_pool;
        }
        if (value.contains("air")) {
            return R.drawable.ic_amenity_ac;
        }
        if (value.contains("bath")) {
            return R.drawable.ic_amenity_bath;
        }
        if (value.contains("balcony")) {
            return R.drawable.ic_amenity_balcony;
        }
        if (value.contains("view")) {
            return R.drawable.ic_amenity_view;
        }
        return R.drawable.ic_room_size;
    }

    private int amenityColor(String label) {
        String value = label == null ? "" : label.toLowerCase(Locale.US);
        if (value.contains("wifi")) {
            return Color.parseColor("#2E6F95");
        }
        if (value.contains("breakfast") || value.contains("coffee")) {
            return Color.parseColor("#B77A25");
        }
        if (value.contains("parking")) {
            return Color.parseColor("#6F58A8");
        }
        if (value.contains("pool") || value.contains("view")) {
            return Color.parseColor("#287E75");
        }
        if (value.contains("air")) {
            return Color.parseColor("#3F7FA8");
        }
        if (value.contains("bath")) {
            return Color.parseColor("#9A6257");
        }
        if (value.contains("balcony")) {
            return Color.parseColor("#A96F23");
        }
        return Color.parseColor("#355C7D");
    }

    private String ratingLabel(Cabin cabin, double ratingFromRates, int countFromRates) {
        double score = ratingFromRates > 0 ? ratingFromRates : (cabin == null ? 0 : cabin.getReviewScore());
        int count = countFromRates > 0 ? countFromRates : (cabin == null ? 0 : cabin.getReviewCount());
        if (score <= 0) {
            return "Chưa có đánh giá";
        }
        return String.format(Locale.US, "%.1f/10 · %d đánh giá · %d sao", score, count, cabin == null ? 0 : cabin.getStarRating());
    }

    private String displayDescription(Cabin cabin) {
        String description = safe(cabin.getDescription(), "");
        String city = resolveCityLabel(cabin);
        String lower = description.toLowerCase(Locale.US);
        if (description.trim().isEmpty() || lower.contains("real-world hotel reference")) {
            return "Khách sạn tham khảo tại " + city + " với dữ liệu phòng, bản đồ và quản lý được chuẩn bị sẵn để bạn test đặt phòng.";
        }
        if (lower.contains("heritage riverside hotel")) {
            return "Khách sạn ven sông phong cách cổ điển, gần phố đi bộ Nguyễn Huệ, có phòng ban công và các lựa chọn phù hợp cho chuyến đi ngắn ngày.";
        }
        if (lower.contains("beachfront vung tau")) {
            return "Khách sạn gần Bãi Sau Vũng Tàu, có phòng rộng, hồ bơi và lựa chọn hướng biển cho kỳ nghỉ cuối tuần.";
        }
        if (lower.contains("landmark hoan kiem")) {
            return "Khách sạn nổi bật tại Hoàn Kiếm, gần Nhà hát Lớn và khu phố cổ, phù hợp cho lịch trình khám phá Hà Nội.";
        }
        if (lower.contains("beach resort")) {
            return "Khu nghỉ dưỡng gần biển với phòng rộng, hồ bơi, lựa chọn cho gia đình và các hạng phòng hướng biển.";
        }
        return description;
    }

    private String translateAmenityLabel(String label) {
        String value = label.toLowerCase(Locale.US);
        if (value.contains("wifi")) {
            return "WiFi";
        }
        if (value.contains("breakfast") || value.contains("coffee")) {
            return "Bữa sáng";
        }
        if (value.contains("parking")) {
            return "Bãi đỗ xe";
        }
        if (value.contains("pool")) {
            return "Hồ bơi";
        }
        if (value.contains("air")) {
            return "Điều hòa";
        }
        if (value.contains("bath")) {
            return "Phòng tắm riêng";
        }
        if (value.contains("balcony")) {
            return "Ban công";
        }
        if (value.contains("view")) {
            return "Tầm nhìn đẹp";
        }
        return label;
    }

    private String amenityHintVi(String label) {
        String value = label.toLowerCase(Locale.US);
        if (value.contains("wifi")) {
            return "Kết nối ổn định trong phòng và khu vực chung";
        }
        if (value.contains("breakfast") || value.contains("coffee")) {
            return "Có thể thêm bữa sáng khi đặt phòng";
        }
        if (value.contains("parking")) {
            return "Thuận tiện khi đi xe cá nhân";
        }
        if (value.contains("pool")) {
            return "Không gian thư giãn trong khuôn viên";
        }
        if (value.contains("air")) {
            return "Phòng mát và dễ chịu";
        }
        if (value.contains("bath")) {
            return "Riêng tư, sạch sẽ sau chuyến đi";
        }
        if (value.contains("balcony")) {
            return "Có thêm ánh sáng và không khí ngoài trời";
        }
        if (value.contains("view")) {
            return "Dễ ngắm cảnh và kiểm tra vị trí lưu trú";
        }
        return "Đã bao gồm trong chỗ nghỉ này";
    }

    private String translateRoomDescription(String description) {
        String value = description.toLowerCase(Locale.US);
        if (value.contains("20-25") || value.contains("quick stays") || value.contains("short city")) {
            return "Phòng gọn gàng cho chuyến đi ngắn, đủ tiện nghi cơ bản.";
        }
        if (value.contains("25-30") || value.contains("extra comfort") || value.contains("desk")) {
            return "Phòng rộng hơn, có thêm không gian sinh hoạt và làm việc.";
        }
        if (value.contains("30-45") || value.contains("balcony") || value.contains("couples")) {
            return "Phòng rộng, phù hợp cặp đôi hoặc nhóm nhỏ, có lựa chọn ban công.";
        }
        if (value.contains("suite") || value.contains("living room")) {
            return "Hạng suite rộng hơn, phù hợp gia đình hoặc nhóm cần không gian riêng.";
        }
        return description;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
