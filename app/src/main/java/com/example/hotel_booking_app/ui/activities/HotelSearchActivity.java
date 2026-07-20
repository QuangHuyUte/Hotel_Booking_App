package com.example.hotel_booking_app.ui.activities;

import android.app.Dialog;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.ui.adapters.CabinAdapter;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.text.Normalizer;

public class HotelSearchActivity extends AppCompatActivity {
    private static final String DEST_HCM = "TP. Hồ Chí Minh, Việt Nam";
    private static final String DEST_VUNG_TAU = "Vũng Tàu, Việt Nam";
    private static final String DEST_HANOI = "Hà Nội, Việt Nam";
    private static final String DEST_DA_NANG = "Đà Nẵng, Việt Nam";
    private static final String DEST_DA_LAT = "Đà Lạt, Việt Nam";

    private TextView statusTextView;
    private EditText searchEditText;
    private EditText destinationEditText;
    private EditText checkInEditText;
    private EditText checkOutEditText;
    private EditText guestsEditText;
    private Spinner sortSpinner;
    private LinearLayout searchPanel;
    private LinearLayout resultsToolbar;
    private LinearLayout mainContainer;
    private View homeContainer;
    private View bottomNav;
    private RecyclerView recyclerView;
    private TextView resultSummaryTextView;
    private View resultSearchContainer;
    private View continueSection;
    private TextView continueCityTextView;
    private TextView continueMetaTextView;
    private TextView hcmCountTextView;
    private TextView vungTauCountTextView;
    private TextView haNoiCountTextView;
    private TextView daNangCountTextView;
    private TextView daLatCountTextView;
    private TextView nearMeButton;
    private View discountDealsSection;
    private View[] dealCards = new View[0];
    private ImageView[] dealImages = new ImageView[0];
    private TextView[] dealTitles = new TextView[0];
    private TextView[] dealMetas = new TextView[0];
    private TextView[] dealPrices = new TextView[0];
    private CabinAdapter adapter;
    private CabinService cabinService;
    private BookingService bookingService;
    private RoomTypeService roomTypeService;
    private List<Cabin> loadedCabins = new ArrayList<>();
    private final List<FilterOption> filterOptions = new ArrayList<>();
    private final Set<String> selectedFilterKeys = new HashSet<>();
    private String selectedCheckIn = "";
    private String selectedCheckOut = "";
    private int availabilityRequestVersion = 0;
    private boolean resultsMode = false;
    private boolean hasCityExploration = false;
    private int currentSortMode = 0;
    private int selectedRooms = 1;
    private int selectedAdults = 2;
    private int selectedBeds = 0;
    private boolean filterOptionsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isHostOrAdmin()) {
            startActivity(new Intent(this, HostHotelDashboardActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_hotel_search);

        mainContainer = findViewById(R.id.container_main);
        statusTextView = findViewById(R.id.text_status);
        searchPanel = findViewById(R.id.panel_search);
        resultsToolbar = findViewById(R.id.panel_results_toolbar);
        homeContainer = findViewById(R.id.container_home);
        bottomNav = findViewById(R.id.bottom_nav);
        resultSummaryTextView = findViewById(R.id.text_result_summary);
        continueSection = findViewById(R.id.section_continue_exploration);
        continueCityTextView = findViewById(R.id.text_continue_city);
        continueMetaTextView = findViewById(R.id.text_continue_meta);
        hcmCountTextView = findViewById(R.id.text_count_hcm);
        vungTauCountTextView = findViewById(R.id.text_count_vung_tau);
        haNoiCountTextView = findViewById(R.id.text_count_ha_noi);
        daNangCountTextView = findViewById(R.id.text_count_da_nang);
        daLatCountTextView = findViewById(R.id.text_count_da_lat);
        nearMeButton = findViewById(R.id.button_near_me);
        discountDealsSection = findViewById(R.id.section_discount_deals);
        dealCards = new View[]{
                findViewById(R.id.card_deal_1),
                findViewById(R.id.card_deal_2),
                findViewById(R.id.card_deal_3)
        };
        dealImages = new ImageView[]{
                findViewById(R.id.image_deal_1),
                findViewById(R.id.image_deal_2),
                findViewById(R.id.image_deal_3)
        };
        dealTitles = new TextView[]{
                findViewById(R.id.text_deal_title_1),
                findViewById(R.id.text_deal_title_2),
                findViewById(R.id.text_deal_title_3)
        };
        dealMetas = new TextView[]{
                findViewById(R.id.text_deal_meta_1),
                findViewById(R.id.text_deal_meta_2),
                findViewById(R.id.text_deal_meta_3)
        };
        dealPrices = new TextView[]{
                findViewById(R.id.text_deal_price_1),
                findViewById(R.id.text_deal_price_2),
                findViewById(R.id.text_deal_price_3)
        };
        searchEditText = findViewById(R.id.edit_search);
        destinationEditText = findViewById(R.id.edit_destination);
        checkInEditText = findViewById(R.id.edit_check_in);
        checkOutEditText = findViewById(R.id.edit_check_out);
        guestsEditText = findViewById(R.id.edit_guests);
        sortSpinner = findViewById(R.id.spinner_sort);
        Button applyFiltersButton = findViewById(R.id.button_apply_filters);
        TextView resultBackButton = findViewById(R.id.button_result_back);
        resultSearchContainer = findViewById(R.id.container_result_search_summary);
        TextView sortResultsButton = findViewById(R.id.button_sort_results);
        TextView filterResultsButton = findViewById(R.id.button_filter_results);
        TextView mapResultsButton = findViewById(R.id.button_result_map);
        LinearLayout personalTab = findViewById(R.id.nav_personal);
        LinearLayout bookingsTab = findViewById(R.id.nav_bookings);
        LinearLayout wishlistTab = findViewById(R.id.nav_wishlist);
        LinearLayout messagesTab = findViewById(R.id.nav_messages);
        recyclerView = findViewById(R.id.recycler_cabins);

        cabinService = new CabinService();
        bookingService = new BookingService();
        roomTypeService = new RoomTypeService();
        adapter = new CabinAdapter(this::openHotelDetail);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        placeSearchPanelInsideHomeScroll();
        setupDefaultSearchState();
        setupInitialIntentFilters();
        setupSortSpinner();
        setupDateInputs();
        setupDestinationChips();
        setupRoomSizeChips();
        loadCityCardImages();
        startHeaderIconAnimations();
        sortSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                currentSortMode = position;
                if (resultsMode && !loadedCabins.isEmpty()) {
                    renderCabins();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        applyFiltersButton.setOnClickListener(view -> showResults());
        nearMeButton.setOnClickListener(view -> chooseNearbyHoChiMinh());
        destinationEditText.setOnClickListener(view -> showDestinationSheet());
        guestsEditText.setOnClickListener(view -> showRoomGuestSheet());
        resultBackButton.setOnClickListener(view -> showHome());
        resultSearchContainer.setOnClickListener(view -> showSearchMorphOverlay());
        resultSummaryTextView.setOnClickListener(view -> showSearchMorphOverlay());
        sortResultsButton.setOnClickListener(view -> showSortSheet());
        filterResultsButton.setOnClickListener(view -> showFilterOverlay());
        mapResultsButton.setOnClickListener(view -> openResultMap());
        destinationEditText.addTextChangedListener(renderingTextWatcher());
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateContinueCard();
                if (resultsMode) {
                    renderCabins();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        personalTab.setOnClickListener(view -> startActivity(new Intent(this, AccountHubActivity.class)));
        bookingsTab.setOnClickListener(view -> openLoginRequired(GuestBookingsActivity.class));
        wishlistTab.setOnClickListener(view -> openLoginRequired(SavedHotelsActivity.class));
        messagesTab.setOnClickListener(view -> openLoginRequired(ConversationListActivity.class));
        showHome();
        loadCabins();
    }

    private void setupDestinationChips() {
        bindDestinationCard(R.id.card_city_hcm, DEST_HCM);
        bindDestinationCard(R.id.card_city_vung_tau, DEST_VUNG_TAU);
        bindDestinationCard(R.id.card_city_ha_noi, DEST_HANOI);
        bindDestinationCard(R.id.card_city_da_nang, DEST_DA_NANG);
        bindDestinationCard(R.id.card_city_da_lat, DEST_DA_LAT);
        findViewById(R.id.card_continue_exploration).setOnClickListener(view -> showResults());
    }

    private void setupRoomSizeChips() {
        bindRoomSizeChip(R.id.chip_room_standard, "standard");
        bindRoomSizeChip(R.id.chip_room_superior, "superior");
        bindRoomSizeChip(R.id.chip_room_deluxe, "deluxe");
        bindRoomSizeChip(R.id.chip_room_suite, "suite");
    }

    private void bindRoomSizeChip(int viewId, String roomQuery) {
        View chip = findViewById(viewId);
        if (chip == null) {
            return;
        }
        chip.setOnClickListener(view -> {
            searchEditText.setText(roomQuery);
            searchEditText.setSelection(searchEditText.getText().length());
            showResults();
        });
    }

    private void placeSearchPanelInsideHomeScroll() {
        if (!(homeContainer instanceof ScrollView) || searchPanel == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) searchPanel.getParent();
        if (parent != null) {
            parent.removeView(searchPanel);
        }
        View child = ((ScrollView) homeContainer).getChildAt(0);
        if (child instanceof LinearLayout) {
            ((LinearLayout) child).addView(searchPanel, 0);
        }
    }

    private void loadCityCardImages() {
        loadCityImage(R.id.image_continue_city, "https://images.unsplash.com/photo-1583417319070-4a69db38a482?auto=format&fit=crop&w=1000&q=80");
        loadCityImage(R.id.image_city_hcm, "https://images.unsplash.com/photo-1583417319070-4a69db38a482?auto=format&fit=crop&w=1000&q=80");
        loadCityImage(R.id.image_city_vung_tau, "https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1000&q=80");
        loadCityImage(R.id.image_city_ha_noi, "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1000&q=80");
        loadCityImage(R.id.image_city_da_nang, "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=1000&q=80");
        loadCityImage(R.id.image_city_da_lat, "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1000&q=80");
    }

    private void loadCityImage(int viewId, String url) {
        ImageView imageView = findViewById(viewId);
        if (imageView == null) {
            return;
        }
        Glide.with(this)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.bg_dark_card)
                .into(imageView);
    }

    private void openHotelDetail(Cabin cabin) {
        Intent intent = new Intent(this, HotelDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabin.getId());
        if (cabin.getMatchedRoomType() != null) {
            intent.putExtra(AppConstants.EXTRA_ROOM_TYPE_ID, cabin.getMatchedRoomType().getId());
        }
        startActivity(intent);
    }

    private void startHeaderIconAnimations() {
    }

    private void animateFloatingHeaderIcon(ImageView icon, long duration, long delay, int startDp, int yDp) {
        if (icon == null) {
            return;
        }
        icon.post(() -> {
            View parent = (View) icon.getParent();
            int end = parent.getWidth() + dp(72);
            icon.setTranslationX(dp(startDp));
            icon.setTranslationY(dp(yDp));

            ObjectAnimator drift = ObjectAnimator.ofFloat(icon, "translationX", dp(startDp), end);
            drift.setDuration(duration);
            drift.setStartDelay(delay);
            drift.setRepeatCount(ValueAnimator.INFINITE);
            drift.setRepeatMode(ValueAnimator.RESTART);
            drift.setInterpolator(new LinearInterpolator());
            drift.start();

            ObjectAnimator pulse = ObjectAnimator.ofFloat(icon, "alpha", 0.08f, 0.18f, 0.08f);
            pulse.setDuration(4200L);
            pulse.setStartDelay(delay / 2);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setRepeatMode(ValueAnimator.RESTART);
            pulse.start();
        });
    }

    private void setupDefaultSearchState() {
        LocalDate today = LocalDate.now();
        selectedCheckIn = today.plusDays(5).toString();
        selectedCheckOut = today.plusDays(6).toString();
        destinationEditText.setText(DEST_HCM);
        destinationEditText.setSelection(destinationEditText.getText().length());
        selectedRooms = 1;
        selectedAdults = 2;
        selectedBeds = 0;
        guestsEditText.setText(defaultGuestSummary());
        updateDateTexts();
    }

    private void chooseNearbyHoChiMinh() {
        destinationEditText.setText(DEST_HCM);
        destinationEditText.setSelection(destinationEditText.getText().length());
        hasCityExploration = true;
        updateContinueCard();
    }

    private void bindDestinationCard(int viewId, String destination) {
        View card = findViewById(viewId);
        card.setOnClickListener(view -> {
            hasCityExploration = true;
            destinationEditText.setText(destination);
            destinationEditText.setSelection(destinationEditText.getText().length());
            showResults();
        });
    }

    private void openLoginRequired(Class<?> target) {
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            return;
        }
        Class<?> resolvedTarget = sessionManager.isHostOrAdmin() && target == GuestBookingsActivity.class
                ? AdminBookingManagementActivity.class
                : target;
        startActivity(new Intent(this, resolvedTarget));
    }

    private void showHome() {
        resultsMode = false;
        searchPanel.setVisibility(View.VISIBLE);
        resultsToolbar.setVisibility(View.GONE);
        homeContainer.setVisibility(View.VISIBLE);
        statusTextView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        bottomNav.setVisibility(View.VISIBLE);
        mainContainer.setPadding(0, 0, 0, dp(78));
        continueSection.setVisibility(hasCityExploration ? View.VISIBLE : View.GONE);
        updateContinueCard();
        updateCityCounts();
    }

    private void showResults() {
        resultsMode = true;
        hasCityExploration = true;
        if (destinationEditText.getText().toString().trim().isEmpty()) {
            destinationEditText.setText(DEST_HCM);
        }
        searchPanel.setVisibility(View.GONE);
        resultsToolbar.setVisibility(View.VISIBLE);
        homeContainer.setVisibility(View.GONE);
        statusTextView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        bottomNav.setVisibility(View.GONE);
        mainContainer.setPadding(0, 0, 0, 0);
        continueSection.setVisibility(hasCityExploration ? View.VISIBLE : View.GONE);
        updateContinueCard();
        updateResultSummary();
        renderCabins();
    }

    private void updateResultSummary() {
        String destination = destinationEditText.getText().toString().trim();
        if (destination.isEmpty()) {
            destination = DEST_HCM;
        }
        resultSummaryTextView.setText(destination + " - " + compactDateRange() + " - " + guestSummary());
    }

    private void updateContinueCard() {
        if (continueCityTextView == null) {
            return;
        }
        String destination = destinationEditText.getText().toString().trim();
        continueCityTextView.setText(destination.isEmpty() ? DEST_HCM : destination);
        continueMetaTextView.setText(compactDateRange() + " - " + guestSummary());
    }

    private String compactDateRange() {
        String checkIn = formatShortDate(selectedCheckIn);
        String checkOut = formatShortDate(selectedCheckOut);
        if (checkIn.isEmpty() || checkOut.isEmpty()) {
            return "Chọn ngày";
        }
        return checkIn + " - " + checkOut;
    }

    private String formatShortDate(String isoDate) {
        try {
            return formatVietnameseDate(LocalDate.parse(isoDate));
        } catch (Exception e) {
            return "";
        }
    }

    private String guestSummary() {
        syncGuestStateFromInput();
        return formatGuestSummary(selectedRooms, selectedAdults, selectedBeds);
    }

    private String defaultGuestSummary() {
        return formatGuestSummary(1, 2, 0);
    }

    private String formatGuestSummary(int rooms, int adults, int beds) {
        String summary = Math.max(1, rooms) + " phòng · " + Math.max(1, adults) + " người lớn";
        if (beds > 0) {
            summary += " · " + beds + " giường";
        }
        return summary;
    }

    private void updateCityCounts() {
        if (hcmCountTextView == null || loadedCabins == null) {
            return;
        }
        hcmCountTextView.setText(countCity("ho chi minh") + " chỗ nghỉ");
        vungTauCountTextView.setText(countCity("vung tau") + " chỗ nghỉ");
        haNoiCountTextView.setText(countCity("ha noi") + " chỗ nghỉ");
        if (daNangCountTextView != null) {
            daNangCountTextView.setText(countCity("da nang") + " chỗ nghỉ");
        }
        if (daLatCountTextView != null) {
            daLatCountTextView.setText(countCity("da lat") + " chỗ nghỉ");
        }
    }

    private int countCity(String city) {
        int count = 0;
        for (Cabin cabin : loadedCabins) {
            String location = normalizeSearchText(cabin.getLocation());
            String name = normalizeSearchText(cabin.getName());
            boolean hanoiAlias = city.equals("ha noi") && (location.contains("hanoi") || name.contains("hanoi"));
            if (location.contains(city) || name.contains(city) || hanoiAlias) {
                count++;
            }
        }
        return count;
    }

    private void showSortDialog() {
        String[] options = {
                "Phù hợp nhất",
                "Đang giảm giá",
                "Giá: thấp đến cao",
                "Giá: cao đến thấp",
                "Sức chứa: cao đến thấp",
                "Mới nhất trước"
        };
        new android.app.AlertDialog.Builder(this)
                .setTitle("Sắp xếp")
                .setSingleChoiceItems(options, sortSpinner.getSelectedItemPosition(), (dialog, which) -> {
                    sortSpinner.setSelection(which);
                    dialog.dismiss();
                    renderCabins();
                })
                .show();
    }

    private void showFilterDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundResource(R.drawable.bg_calendar_dialog);

        TextView title = new TextView(this);
        title.setText("Lọc chỗ nghỉ");
        title.setTextColor(getColor(R.color.booking_text));
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        EditText guestsInput = new EditText(this);
        guestsInput.setHint("Số khách");
        guestsInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        guestsInput.setText(guestsEditText.getText().toString());
        guestsInput.setBackgroundResource(R.drawable.bg_booking_field);
        guestsInput.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        inputParams.setMargins(0, dp(14), 0, 0);
        root.addView(guestsInput, inputParams);

        EditText keywordInput = new EditText(this);
        keywordInput.setHint("Amenities, area, max price");
        keywordInput.setText(searchEditText.getText().toString());
        keywordInput.setBackgroundResource(R.drawable.bg_booking_field);
        keywordInput.setPadding(dp(12), 0, dp(12), 0);
        root.addView(keywordInput, inputParams);

        Button applyButton = new Button(this);
        applyButton.setText("View results");
        applyButton.setAllCaps(false);
        applyButton.setTextColor(getColor(R.color.white));
        applyButton.setBackgroundResource(R.drawable.bg_button_primary);
        applyButton.setOnClickListener(view -> {
            guestsEditText.setText(guestsInput.getText().toString());
            searchEditText.setText(keywordInput.getText().toString());
            dialog.dismiss();
            renderCabins();
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        buttonParams.setMargins(0, dp(16), 0, 0);
        root.addView(applyButton, buttonParams);
        dialog.setContentView(root);
        dialog.show();
    }

    private void showSearchEditDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundResource(R.drawable.bg_calendar_dialog);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView close = new TextView(this);
        close.setText("X");
        close.setTextColor(getColor(R.color.booking_blue));
        close.setTextSize(30);
        close.setGravity(android.view.Gravity.CENTER);
        close.setOnClickListener(view -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = new TextView(this);
        title.setText("Sửa tìm kiếm");
        title.setTextColor(getColor(R.color.booking_text));
        title.setTextSize(23);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header);

        EditText destinationInput = dialogField(DEST_HCM);
        destinationInput.setText(destinationEditText.getText().toString());
        root.addView(destinationInput, dialogFieldParams(14));

        TextView dateInput = dialogTextRow(formatIsoDate(selectedCheckIn) + " - " + formatIsoDate(selectedCheckOut));
        dateInput.setOnClickListener(view -> {
            dialog.dismiss();
            showDatePicker(true);
        });
        root.addView(dateInput, dialogFieldParams(0));

        EditText guestsInput = dialogField(defaultGuestSummary());
        guestsInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        guestsInput.setText(guestsEditText.getText().toString());
        root.addView(guestsInput, dialogFieldParams(0));

        Button searchButton = new Button(this);
        searchButton.setText("Tìm");
        searchButton.setAllCaps(false);
        searchButton.setTextColor(getColor(R.color.white));
        searchButton.setTextSize(18);
        searchButton.setTypeface(null, android.graphics.Typeface.BOLD);
        searchButton.setBackgroundResource(R.drawable.bg_button_primary);
        searchButton.setOnClickListener(view -> {
            String destination = destinationInput.getText().toString().trim();
            if (!destination.isEmpty()) {
                destinationEditText.setText(destination);
            }
            String guests = guestsInput.getText().toString().trim();
            if (!guests.isEmpty()) {
                guestsEditText.setText(guests);
            }
            dialog.dismiss();
            showResults();
        });
        root.addView(searchButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        ));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.72f);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private EditText dialogField(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(getColor(R.color.booking_text));
        input.setHintTextColor(getColor(R.color.booking_muted));
        input.setTextSize(18);
        input.setBackgroundResource(R.drawable.bg_booking_field);
        input.setPadding(dp(14), 0, dp(14), 0);
        return input;
    }

    private TextView dialogTextRow(String text) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(getColor(R.color.booking_text));
        row.setTextSize(18);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_booking_field);
        row.setPadding(dp(14), 0, dp(14), 0);
        row.setClickable(true);
        row.setFocusable(true);
        return row;
    }

    private LinearLayout.LayoutParams dialogFieldParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        params.setMargins(0, dp(topMarginDp), 0, 0);
        return params;
    }

    private void showSearchMorphOverlay() {
        FrameLayout overlay = createOverlay(Color.TRANSPARENT);
        overlay.setClickable(true);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackground(roundedColor(0xFF202020, dp(10), getColor(R.color.booking_yellow), dp(3)));
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        panelParams.setMargins(dp(14), dp(48), dp(14), 0);
        overlay.addView(panel, panelParams);
        overlay.setOnClickListener(view -> dismissSearchOverlay(overlay, panel));
        panel.setOnClickListener(view -> { });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(header, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        TextView close = iconText("x", 34, 0xFF49A3FF);
        close.setOnClickListener(view -> dismissSearchOverlay(overlay, panel));
        header.addView(close, new LinearLayout.LayoutParams(dp(50), dp(48)));

        TextView title = darkTitle("Sửa tìm kiếm", 22);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(new View(this), new LinearLayout.LayoutParams(dp(50), dp(48)));

        TextView destinationInput = darkSearchRow(destinationEditText.getText().toString());
        destinationInput.setOnClickListener(view -> {
            dismissSearchOverlay(overlay, panel);
            showDestinationSheet();
        });
        panel.addView(destinationInput, fullHeightParams(62, 10));

        TextView scheduleInput = darkSearchRow("Ngày: " + formatIsoDate(selectedCheckIn) + " - " + formatIsoDate(selectedCheckOut));
        scheduleInput.setOnClickListener(view -> {
            dismissSearchOverlay(overlay, panel);
            showDatePicker(true);
        });
        panel.addView(scheduleInput, fullHeightParams(62, 0));

        TextView guestsInput = darkSearchRow(guestsEditText.getText().toString());
        guestsInput.setOnClickListener(view -> {
            dismissSearchOverlay(overlay, panel);
            showRoomGuestSheet();
        });
        panel.addView(guestsInput, fullHeightParams(62, 0));

        Button searchButton = new Button(this);
        searchButton.setText("Tìm");
        searchButton.setAllCaps(false);
        searchButton.setTextColor(Color.WHITE);
        searchButton.setTextSize(21);
        searchButton.setTypeface(null, Typeface.BOLD);
        searchButton.setBackground(roundedColor(0xFF087BEA, 0, 0, 0));
        searchButton.setOnClickListener(view -> {
            dismissSearchOverlay(overlay, panel);
            showResults();
        });
        panel.addView(searchButton, fullHeightParams(58, 0));

        panel.setPivotY(0);
        panel.setScaleX(0.92f);
        panel.setScaleY(0.28f);
        panel.setAlpha(0f);
        panel.setTranslationY(-dp(10));
        panel.animate().scaleX(1f).scaleY(1f).translationY(0).alpha(1f).setDuration(230).start();
    }

    private void showSortSheet() {
        FrameLayout overlay = createOverlay(Color.TRANSPARENT);
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(22), dp(12), dp(22), dp(26));
        sheet.setBackground(roundedColor(0xFF242424, dp(26), 0, 0));
        overlay.addView(sheet, bottomSheetParams());
        overlay.setOnClickListener(view -> dismissOverlay(overlay, sheet, true));
        sheet.setOnClickListener(view -> { });

        View handle = new View(this);
        handle.setBackground(roundedColor(0xFFB8B8B8, dp(4), 0, 0));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(64), dp(6));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        sheet.addView(handle, handleParams);

        TextView title = darkTitle("Sắp xếp theo", 31);
        sheet.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(78)));

        String[] labels = {
                "Homes and entire apartments first",
                "Our top picks",
                "Distance from city center",
                "Property rating (5 to 0)",
                "Property rating (0 to 5)",
                "Genius deals first",
                "Đánh giá cao trước",
                "Giá thấp trước",
                "Giá cao trước"
        };
        for (int i = 0; i < labels.length; i++) {
            final int mode = i;
            sheet.addView(sortRow(labels[i], mode, () -> {
                currentSortMode = mode;
                dismissOverlay(overlay, sheet, true);
                renderCabins();
            }));
        }
        attachSheetDrag(overlay, handle, sheet);
        animateSheetIn(sheet);
    }

    private void showDestinationSheet() {
        FrameLayout overlay = createOverlay(Color.argb(170, 0, 0, 0));
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(22), dp(12), dp(22), dp(22));
        sheet.setBackground(roundedColor(0xFF242424, dp(24), 0, 0));
        overlay.addView(sheet, bottomSheetParams());
        overlay.setOnClickListener(view -> dismissOverlay(overlay, sheet, true));
        sheet.setOnClickListener(view -> { });

        View handle = new View(this);
        handle.setBackground(roundedColor(0xFFB8B8B8, dp(4), 0, 0));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(64), dp(6));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        sheet.addView(handle, handleParams);

        TextView title = darkTitle("Chọn thành phố", 28);
        sheet.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(76)));
        sheet.addView(destinationRow(DEST_HCM, cityMeta("ho chi minh"), overlay, sheet));
        sheet.addView(destinationRow(DEST_VUNG_TAU, cityMeta("vung tau"), overlay, sheet));
        sheet.addView(destinationRow(DEST_HANOI, cityMeta("ha noi"), overlay, sheet));
        sheet.addView(destinationRow(DEST_DA_NANG, cityMeta("da nang"), overlay, sheet));
        sheet.addView(destinationRow(DEST_DA_LAT, cityMeta("da lat"), overlay, sheet));

        attachSheetDrag(overlay, handle, sheet);
        animateSheetIn(sheet);
    }

    private View destinationRow(String label, String meta, FrameLayout overlay, View sheet) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setBackground(roundedColor(0xFF2C2C2C, dp(8), 0xFF4B4B4B, dp(1)));
        row.setClickable(true);
        row.setFocusable(true);

        TextView text = darkBody(label + "\n" + meta, 18);
        text.setTypeface(null, Typeface.BOLD);
        row.addView(text, new LinearLayout.LayoutParams(0, dp(70), 1));

        TextView arrow = iconText(">", 24, 0xFF4DA3FF);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(40), dp(70)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(70)
        );
        params.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(params);
        row.setOnClickListener(view -> {
            destinationEditText.setText(label);
            destinationEditText.setSelection(destinationEditText.getText().length());
            hasCityExploration = true;
            updateContinueCard();
            if (resultsMode) {
                updateResultSummary();
                renderCabins();
            }
            dismissOverlay(overlay, sheet, true);
        });
        return row;
    }

    private String cityMeta(String cityKey) {
        int count = countCity(cityKey);
        return (count <= 0 ? 15 : count) + " chỗ nghỉ";
    }

    private void showRoomGuestSheet() {
        syncGuestStateFromInput();
        final int[] draftRooms = {Math.max(1, selectedRooms)};
        final int[] draftAdults = {Math.max(1, selectedAdults)};
        final int[] draftBeds = {Math.max(0, selectedBeds)};

        FrameLayout overlay = createOverlay(Color.argb(170, 0, 0, 0));
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(22), dp(12), dp(22), dp(22));
        sheet.setBackground(roundedColor(0xFF242424, dp(24), 0, 0));
        overlay.addView(sheet, bottomSheetParams());
        overlay.setOnClickListener(view -> dismissOverlay(overlay, sheet, true));
        sheet.setOnClickListener(view -> { });

        View handle = new View(this);
        handle.setBackground(roundedColor(0xFFB8B8B8, dp(4), 0, 0));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(64), dp(6));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        sheet.addView(handle, handleParams);

        TextView title = darkTitle("Chọn phòng và khách", 28);
        sheet.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(76)));

        sheet.addView(roomGuestCounterRow("Phòng", draftRooms, 1, 8));
        sheet.addView(roomGuestCounterRow("Người lớn", draftAdults, 1, 16));
        sheet.addView(roomGuestCounterRow("Giường", draftBeds, 0, 8));

        Button apply = new Button(this);
        apply.setText("Áp dụng");
        apply.setAllCaps(false);
        apply.setTextColor(Color.WHITE);
        apply.setTextSize(18);
        apply.setTypeface(null, Typeface.BOLD);
        apply.setBackground(roundedColor(0xFF087BEA, dp(6), 0, 0));
        apply.setOnClickListener(view -> {
            selectedRooms = draftRooms[0];
            selectedAdults = draftAdults[0];
            selectedBeds = draftBeds[0];
            guestsEditText.setText(formatGuestSummary(selectedRooms, selectedAdults, selectedBeds));
            updateContinueCard();
            if (resultsMode) {
                renderCabins();
            }
            dismissOverlay(overlay, sheet, true);
        });
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
        );
        applyParams.setMargins(0, dp(24), 0, 0);
        sheet.addView(apply, applyParams);

        attachSheetDrag(overlay, handle, sheet);
        animateSheetIn(sheet);
    }

    private View roomGuestCounterRow(String label, int[] value, int minValue, int maxValue) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView labelView = darkBody(label, 21);
        labelView.setTypeface(null, Typeface.BOLD);
        row.addView(labelView, new LinearLayout.LayoutParams(0, dp(64), 1));

        TextView minus = counterButton("-");
        TextView number = darkBody(String.valueOf(value[0]), 22);
        number.setGravity(Gravity.CENTER);
        TextView plus = counterButton("+");

        Runnable refresh = () -> {
            number.setText(value[0] <= 0 ? "Bất kỳ" : String.valueOf(value[0]));
            minus.setTextColor(value[0] <= minValue ? 0xFF777777 : 0xFF4DA3FF);
            plus.setTextColor(value[0] >= maxValue ? 0xFF777777 : 0xFF4DA3FF);
        };
        minus.setOnClickListener(view -> {
            if (value[0] > minValue) {
                value[0]--;
                refresh.run();
            }
        });
        plus.setOnClickListener(view -> {
            if (value[0] < maxValue) {
                value[0]++;
                refresh.run();
            }
        });

        row.addView(minus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        row.addView(number, new LinearLayout.LayoutParams(dp(78), dp(48)));
        row.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        refresh.run();
        return row;
    }

    private TextView counterButton(String text) {
        TextView button = iconText(text, 30, 0xFF4DA3FF);
        button.setBackground(roundedColor(0xFF242424, dp(6), 0xFF4DA3FF, dp(1)));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private FrameLayout createOverlay(int color) {
        FrameLayout host = findViewById(android.R.id.content);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(color);
        host.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        return overlay;
    }

    private FrameLayout.LayoutParams bottomSheetParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
    }

    private void animateSheetIn(View sheet) {
        sheet.post(() -> {
            sheet.setTranslationY(sheet.getHeight());
            sheet.animate().translationY(0).setDuration(240).start();
        });
    }

    private void dismissOverlay(FrameLayout overlay, View sheet, boolean slideSheet) {
        if (sheet != null && slideSheet) {
            sheet.animate().translationY(sheet.getHeight()).setDuration(180).withEndAction(() -> removeOverlay(overlay)).start();
        } else {
            overlay.animate().alpha(0f).setDuration(160).withEndAction(() -> removeOverlay(overlay)).start();
        }
    }

    private void dismissSearchOverlay(FrameLayout overlay, View panel) {
        panel.animate()
                .scaleX(0.94f)
                .scaleY(0.25f)
                .translationY(-dp(10))
                .alpha(0f)
                .setDuration(170)
                .withEndAction(() -> removeOverlay(overlay))
                .start();
    }

    private void removeOverlay(FrameLayout overlay) {
        if (overlay != null && overlay.getParent() instanceof FrameLayout) {
            ((FrameLayout) overlay.getParent()).removeView(overlay);
        }
    }

    private void attachSheetDrag(FrameLayout overlay, View dragHandle, View sheet) {
        final float[] startY = {0f};
        final float[] lastDy = {0f};
        dragHandle.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startY[0] = event.getRawY();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                lastDy[0] = Math.max(0, event.getRawY() - startY[0]);
                sheet.setTranslationY(lastDy[0]);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (lastDy[0] > dp(90)) {
                    dismissOverlay(overlay, sheet, true);
                } else {
                    sheet.animate().translationY(0).setDuration(160).start();
                }
                return true;
            }
            return false;
        });
    }

    private View sortRow(String label, int mode, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, 0);
        row.setOnClickListener(view -> onClick.run());
        TextView text = darkBody(label, 19);
        row.addView(text, new LinearLayout.LayoutParams(0, dp(64), 1));
        TextView radio = iconText(currentSortMode == mode ? "●" : "○", 28, currentSortMode == mode ? 0xFF1689FF : 0xFFE5E5E5);
        row.addView(radio, new LinearLayout.LayoutParams(dp(54), dp(64)));
        return row;
    }

    private EditText darkInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xFFBDBDBD);
        input.setTextSize(20);
        input.setBackground(roundedColor(0xFF242424, dp(8), 0xFF4B4B4B, dp(1)));
        input.setPadding(dp(18), 0, dp(18), 0);
        return input;
    }

    private TextView darkSearchRow(String text) {
        TextView row = darkBody(text, 20);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(roundedColor(0xFF242424, dp(8), 0xFF4B4B4B, dp(1)));
        row.setPadding(dp(18), 0, dp(18), 0);
        row.setClickable(true);
        return row;
    }

    private LinearLayout.LayoutParams fullHeightParams(int heightDp, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp));
        params.setMargins(0, dp(topDp), 0, 0);
        return params;
    }

    private TextView darkTitle(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(sp);
        view.setTypeface(null, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView darkBody(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(sp);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView iconText(String text, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private GradientDrawable roundedColor(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private void showFilterOverlay() {
        ensureFilterOptions();
        Set<String> draft = new HashSet<>(selectedFilterKeys);
        FrameLayout overlay = createOverlay(Color.BLACK);
        overlay.setOnClickListener(view -> { });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        overlay.addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(22), dp(40), dp(22), 0);
        root.addView(header, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(98)));

        TextView close = iconText("x", 38, Color.WHITE);
        close.setOnClickListener(view -> dismissOverlay(overlay, null, false));
        header.addView(close, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView title = darkTitle("Lọc theo", 22);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1));

        TextView reset = new TextView(this);
        reset.setText("Đặt lại");
        reset.setTextColor(0xFF4DA3FF);
        reset.setTextSize(19);
        reset.setGravity(Gravity.CENTER);
        reset.setTypeface(null, Typeface.BOLD);
        header.addView(reset, new LinearLayout.LayoutParams(dp(112), dp(54)));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(10), dp(22), dp(170));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        List<FacetRow> rows = new ArrayList<>();
        TextView resultTitle = darkTitle("", 21);
        TextView resultSub = darkBody("", 16);
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            for (FacetRow row : rows) {
                int count = countMatchingWith(row.option.key, draft);
                row.label.setText(row.option.label + " (" + count + ")");
                row.checkBox.setChecked(draft.contains(row.option.key));
            }
            int resultCount = countMatchingFilters(draft);
            resultTitle.setText(formatCount(resultCount) + " chỗ nghỉ phù hợp");
            resultSub.setText("+ " + Math.max(0, Math.round(resultCount * 0.1f)) + " chỗ nghỉ quanh " + normalizedDestinationName());
        };

        addBudgetSection(content, draft, rows, refresh);
        addFilterSection(content, "Bộ lọc phổ biến", "popular", 4, draft, rows, refresh);
        addFilterSection(content, "Khu vực", "area", 5, draft, rows, refresh);
        addFilterSection(content, "Loại chỗ nghỉ", "type", 3, draft, rows, refresh);
        addFilterSection(content, "Tiện nghi", "amenity", 4, draft, rows, refresh);
        addFilterSection(content, "Kiểu giường", "bed", 3, draft, rows, refresh);
        addFilterSection(content, "Điểm đánh giá", "review", 3, draft, rows, refresh);
        addFilterSection(content, "Hạng sao", "star", 5, draft, rows, refresh);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(22), dp(16), dp(22), dp(20));
        footer.setBackgroundColor(0xFF282828);
        overlay.addView(footer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(152),
                Gravity.BOTTOM
        ));
        footer.addView(resultTitle);
        footer.addView(resultSub);

        Button apply = new Button(this);
        apply.setText("Hiển thị kết quả");
        apply.setAllCaps(false);
        apply.setTextColor(Color.WHITE);
        apply.setTextSize(22);
        apply.setBackground(roundedColor(0xFF087BEA, dp(6), 0, 0));
        apply.setOnClickListener(view -> {
            selectedFilterKeys.clear();
            selectedFilterKeys.addAll(draft);
            dismissOverlay(overlay, null, false);
            renderCabins();
        });
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        applyParams.setMargins(0, dp(12), 0, 0);
        footer.addView(apply, applyParams);

        reset.setOnClickListener(view -> {
            draft.clear();
            refresh[0].run();
        });
        refresh[0].run();
        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(180).start();
    }

    private void ensureFilterOptions() {
        if (filterOptionsReady) {
            return;
        }
        filterOptionsReady = true;
        addFilter("budget", "budget_80", "US$0 - US$80", cabin -> price(cabin) <= 80);
        addFilter("budget", "budget_120", "US$80 - US$120", cabin -> price(cabin) > 80 && price(cabin) <= 120);
        addFilter("budget", "budget_200", "US$120 - US$200+", cabin -> price(cabin) > 120);

        addFilter("popular", "popular_9", "Tuyệt hảo: từ 9 điểm", cabin -> reviewScore(cabin) >= 9.0);
        addFilter("popular", "popular_hotel", "Khách sạn", cabin -> true);
        addFilter("popular", "popular_breakfast", "Bao gồm bữa sáng", cabin -> text(cabin).contains("breakfast"));
        addFilter("popular", "popular_d1", "Quận 1", cabin -> contains(cabin, "district 1"));
        addFilter("popular", "popular_pool", "Hồ bơi", cabin -> contains(cabin, "pool"));
        addFilter("popular", "popular_four", "4 sao", cabin -> cabin.getStarRating() >= 4);

        addFilter("area", "area_d1", "Quận 1", cabin -> contains(cabin, "district 1"));
        addFilter("area", "area_centre", "Trung tâm TP. Hồ Chí Minh", cabin -> contains(cabin, "ho chi minh"));
        addFilter("area", "area_d3", "Quận 3", cabin -> contains(cabin, "district 3"));
        addFilter("area", "area_favorite", "Khu vực được yêu thích", cabin -> containsAny(cabin, "ben thanh", "nguyen hue", "landmark", "hoan kiem", "front beach"));
        addFilter("area", "area_tan_binh", "Quận Tân Bình", cabin -> containsAny(cabin, "tan binh", "airport"));
        addFilter("area", "area_vung_tau", "Vũng Tàu", cabin -> contains(cabin, "vung tau"));
        addFilter("area", "area_ha_noi", "Hà Nội", cabin -> containsAny(cabin, "ha noi", "hanoi"));
        addFilter("area", "area_da_nang", "Đà Nẵng", cabin -> contains(cabin, "da nang"));
        addFilter("area", "area_da_lat", "Đà Lạt", cabin -> contains(cabin, "da lat"));

        addFilter("type", "type_hotel", "Khách sạn", cabin -> true);
        addFilter("type", "type_apartment", "Căn hộ", cabin -> containsAny(cabin, "apartment", "loft", "suite"));
        addFilter("type", "type_hotel_apartment", "Căn hộ khách sạn", cabin -> containsAny(cabin, "apartment", "suite"));
        addFilter("type", "type_guesthouse", "Nhà nghỉ", cabin -> cabin.getMaxCapacity() <= 2);
        addFilter("type", "type_resort", "Khu nghỉ dưỡng", cabin -> containsAny(cabin, "beach", "pool", "garden"));

        addFilter("amenity", "amenity_pool", "Hồ bơi", cabin -> contains(cabin, "pool"));
        addFilter("amenity", "amenity_free_parking", "Đỗ xe miễn phí", cabin -> contains(cabin, "parking"));
        addFilter("amenity", "amenity_parking", "Bãi đỗ xe", cabin -> contains(cabin, "parking"));
        addFilter("amenity", "amenity_pet", "Cho phép thú cưng", cabin -> containsAny(cabin, "garden", "beach"));
        addFilter("amenity", "amenity_wifi", "WiFi miễn phí", cabin -> contains(cabin, "wifi"));
        addFilter("amenity", "amenity_jacuzzi", "Bồn tắm/Jacuzzi", cabin -> contains(cabin, "bathtub"));
        addFilter("amenity", "amenity_fitness", "Phòng gym", cabin -> containsAny(cabin, "landmark", "central park"));

        addFilter("room", "room_ac", "Air conditioning", cabin -> contains(cabin, "air conditioning"));
        addFilter("room", "room_private_bath", "Private bathroom", cabin -> contains(cabin, "bathtub"));
        addFilter("room", "room_balcony", "Balcony", cabin -> contains(cabin, "balcony"));
        addFilter("room", "room_kitchen", "Kitchen area", cabin -> contains(cabin, "kitchen"));
        addFilter("room", "room_bath", "Bathtub", cabin -> contains(cabin, "bathtub"));
        addFilter("room", "room_view", "View", cabin -> containsAny(cabin, "view", "lake", "beach", "river"));

        addFilter("meal", "meal_breakfast", "Có bữa sáng", cabin -> contains(cabin, "breakfast"));
        addFilter("meal", "meal_breakfast_dinner", "Breakfast and dinner included", cabin -> contains(cabin, "breakfast") && cabin.getRegularPrice() > 120);
        addFilter("meal", "meal_self", "Tự nấu ăn", cabin -> contains(cabin, "kitchen"));
        addFilter("meal", "meal_all", "Bao gồm nhiều bữa", cabin -> contains(cabin, "breakfast") && contains(cabin, "kitchen"));
        addFilter("meal", "meal_breakfast_lunch", "Breakfast and lunch included", cabin -> contains(cabin, "breakfast") && cabin.getMaxCapacity() >= 4);

        addFilter("bed", "bed_double", "Giường đôi / Queen / King", cabin -> hasBedTerm(cabin, "double", "queen", "king"));
        addFilter("bed", "bed_twin", "2 giường đơn", cabin -> hasTwinSingle(cabin));
        addFilter("bed", "bed_sofa", "Giường sofa", cabin -> hasBedTerm(cabin, "sofa"));

        addFilter("brand", "brand_hilton", "Hilton Hotels & Resorts", cabin -> contains(cabin, "skyline"));
        addFilter("brand", "brand_marriott", "Marriott Hotels & Resorts", cabin -> contains(cabin, "landmark"));
        addFilter("brand", "brand_ibis", "ibis", cabin -> contains(cabin, "airport"));
        addFilter("brand", "brand_novotel", "Novotel", cabin -> contains(cabin, "central"));
        addFilter("brand", "brand_holiday", "Holiday Inn Hotels & Resorts", cabin -> contains(cabin, "family"));

        addFilter("review", "review_9", "Tuyệt hảo: từ 9 điểm", cabin -> reviewScore(cabin) >= 9.0);
        addFilter("review", "review_8", "Rất tốt: từ 8 điểm", cabin -> reviewScore(cabin) >= 8.0);
        addFilter("review", "review_7", "Tốt: từ 7 điểm", cabin -> reviewScore(cabin) >= 7.0);
        addFilter("review", "review_6", "Dễ chịu: từ 6 điểm", cabin -> reviewScore(cabin) >= 6.0);
        addFilter("review", "review_5", "Ổn: từ 5 điểm", cabin -> reviewScore(cabin) >= 5.0);

        addFilter("location", "location_1", "Dưới 1 km", cabin -> containsAny(cabin, "market", "walking", "lake"));
        addFilter("location", "location_3", "Dưới 3 km", cabin -> containsAny(cabin, "district 1", "hoan kiem", "front beach", "back beach"));
        addFilter("location", "location_5", "Dưới 5 km", cabin -> true);

        addFilter("star", "star_0", "Chưa xếp hạng", cabin -> cabin.getStarRating() <= 0);
        addFilter("star", "star_1", "1 sao", cabin -> cabin.getStarRating() == 1);
        addFilter("star", "star_2", "2 sao", cabin -> cabin.getStarRating() == 2);
        addFilter("star", "star_3", "3 sao", cabin -> cabin.getStarRating() == 3);
        addFilter("star", "star_4", "4 sao", cabin -> cabin.getStarRating() == 4);
        addFilter("star", "star_5", "5 sao", cabin -> cabin.getStarRating() >= 5);
    }

    private void addBudgetSection(LinearLayout content, Set<String> draft, List<FacetRow> rows, Runnable[] refresh) {
        addDivider(content);
        TextView title = darkTitle("Ngân sách của bạn (1 đêm)", 23);
        content.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        TextView range = darkBody("US$0 - US$200 +", 20);
        content.addView(range, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)));
        LinearLayout bars = new LinearLayout(this);
        bars.setGravity(Gravity.BOTTOM);
        bars.setPadding(0, 0, 0, dp(10));
        for (int i = 0; i < 44; i++) {
            View bar = new View(this);
            bar.setBackgroundColor(0xFF9B9B9B);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(16 + (i * 17 % 58)), 1);
            params.setMargins(dp(1), 0, dp(1), 0);
            bars.addView(bar, params);
        }
        content.addView(bars, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(116)));
        addFilterSection(content, null, "budget", 3, draft, rows, refresh);
    }

    private void addFilterSection(LinearLayout content, String title, String section, int collapsedCount,
                                  Set<String> draft, List<FacetRow> rows, Runnable[] refresh) {
        List<FilterOption> options = optionsFor(section);
        if (options.isEmpty()) {
            return;
        }
        if (title != null) {
            addDivider(content);
            content.addView(darkTitle(title, 23), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(62)));
        }
        LinearLayout optionBox = new LinearLayout(this);
        optionBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(optionBox);

        List<View> extraRows = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            View row = filterRow(options.get(i), draft, rows, refresh);
            optionBox.addView(row);
            if (i >= collapsedCount) {
                row.setVisibility(View.GONE);
                extraRows.add(row);
            }
        }
        if (!extraRows.isEmpty()) {
            TextView more = darkBody("Show more", 20);
            more.setTextColor(0xFF4DA3FF);
            more.setTypeface(null, Typeface.BOLD);
            more.setOnClickListener(view -> {
                boolean expanded = extraRows.get(0).getVisibility() == View.VISIBLE;
                for (View row : extraRows) {
                    row.setVisibility(expanded ? View.GONE : View.VISIBLE);
                }
                more.setText(expanded ? "Show more" : "Show less");
            });
            content.addView(more, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58)));
        }
    }

    private View filterRow(FilterOption option, Set<String> draft, List<FacetRow> rows, Runnable[] refresh) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = darkBody(option.label, 19);
        CheckBox checkBox = new CheckBox(this);
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
        rows.add(new FacetRow(option, label, checkBox));
        View.OnClickListener toggle = view -> {
            if (draft.contains(option.key)) {
                draft.remove(option.key);
            } else {
                draft.add(option.key);
            }
            refresh[0].run();
        };
        row.setOnClickListener(toggle);
        checkBox.setOnClickListener(toggle);
        row.addView(label, new LinearLayout.LayoutParams(0, dp(64), 1));
        row.addView(checkBox, new LinearLayout.LayoutParams(dp(54), dp(64)));
        return row;
    }

    private void addStepperSection(LinearLayout content) {
        addDivider(content);
        content.addView(darkTitle("Phòng và giường", 23), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(62)));
        addStepper(content, "Phòng ngủ");
        addStepper(content, "Beds");
        addStepper(content, "Phòng tắm");
    }

    private void addStepper(LinearLayout content, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(darkBody(label, 19), new LinearLayout.LayoutParams(0, dp(64), 1));
        LinearLayout control = new LinearLayout(this);
        control.setGravity(Gravity.CENTER);
        control.setBackground(roundedColor(0xFF242424, dp(5), 0xFF777777, dp(1)));
        TextView minus = iconText("−", 28, 0xFF8E8E8E);
        TextView value = darkBody("0", 22);
        value.setGravity(Gravity.CENTER);
        TextView plus = iconText("+", 28, 0xFF4DA3FF);
        final int[] count = {0};
        minus.setOnClickListener(view -> {
            count[0] = Math.max(0, count[0] - 1);
            value.setText(String.valueOf(count[0]));
        });
        plus.setOnClickListener(view -> {
            count[0]++;
            value.setText(String.valueOf(count[0]));
        });
        control.addView(minus, new LinearLayout.LayoutParams(dp(60), dp(50)));
        control.addView(value, new LinearLayout.LayoutParams(dp(60), dp(50)));
        control.addView(plus, new LinearLayout.LayoutParams(dp(60), dp(50)));
        row.addView(control, new LinearLayout.LayoutParams(dp(180), dp(54)));
        content.addView(row);
    }

    private void addLocationSection(LinearLayout content, Set<String> draft, List<FacetRow> rows, Runnable[] refresh) {
        addDivider(content);
        content.addView(darkTitle("Location", 23), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));
        TextView helper = darkBody("Landmark or airport", 17);
        helper.setTextColor(0xFFCFCFCF);
        content.addView(helper, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(32)));
        EditText place = darkInput("Ví dụ: bảo tàng, ga tàu");
        content.addView(place, fullHeightParams(58, 8));
        TextView distance = darkSearchRow("1 km");
        content.addView(distance, fullHeightParams(58, 8));
        addFilterSection(content, null, "location", 3, draft, rows, refresh);
    }

    private void addDivider(LinearLayout content) {
        View divider = new View(this);
        divider.setBackgroundColor(0xFF4A4A4A);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        params.setMargins(0, dp(10), 0, dp(8));
        content.addView(divider, params);
    }

    private void addFilter(String section, String key, String label, CabinMatcher matcher) {
        filterOptions.add(new FilterOption(section, key, label, matcher));
    }

    private List<FilterOption> optionsFor(String section) {
        List<FilterOption> result = new ArrayList<>();
        for (FilterOption option : filterOptions) {
            if (option.section.equals(section)) {
                result.add(option);
            }
        }
        return result;
    }

    private int countMatchingWith(String key, Set<String> draft) {
        Set<String> with = new HashSet<>(draft);
        with.add(key);
        return countMatchingFilters(with);
    }

    private int countMatchingFilters(Set<String> filters) {
        int count = 0;
        for (Cabin cabin : loadedCabins) {
            if (matchesCurrentSearchBase(cabin) && matchesSelectedFilters(cabin, filters)) {
                count++;
            }
        }
        return count;
    }

    private boolean matchesCurrentSearchBase(Cabin cabin) {
        SearchCriteria criteria = parseSearch(searchEditText.getText().toString());
        String destination = normalizeSearchText(destinationEditText.getText().toString().trim());
        if (!destination.isEmpty()) {
            addDestinationTerms(criteria, destination);
        }
        int structuredGuests = guestCountFromInput(guestsEditText.getText().toString());
        if (structuredGuests > 0) {
            criteria.guests = structuredGuests;
        }
        return (!criteria.hasTextTerms() || matchesTextTerms(cabin, criteria.textTerms))
                && (criteria.guests <= 0 || cabin.getMaxCapacity() >= criteria.guests)
                && (criteria.maxPrice <= 0 || price(cabin) <= criteria.maxPrice)
                && (!criteria.discountOnly || cabin.getDiscount() > 0);
    }

    private boolean matchesSelectedFilters(Cabin cabin, Set<String> filters) {
        Set<String> sections = new HashSet<>();
        for (String key : filters) {
            FilterOption option = findOption(key);
            if (option != null) {
                sections.add(option.section);
            }
        }
        for (String section : sections) {
            boolean sectionMatches = false;
            for (String key : filters) {
                FilterOption option = findOption(key);
                if (option != null && option.section.equals(section) && option.matcher.matches(cabin)) {
                    sectionMatches = true;
                    break;
                }
            }
            if (!sectionMatches) {
                return false;
            }
        }
        return true;
    }

    private FilterOption findOption(String key) {
        for (FilterOption option : filterOptions) {
            if (option.key.equals(key)) {
                return option;
            }
        }
        return null;
    }

    private double price(Cabin cabin) {
        return cabin.displayPrice();
    }

    private boolean contains(Cabin cabin, String term) {
        return text(cabin).contains(normalizeSearchText(term));
    }

    private boolean containsAny(Cabin cabin, String... terms) {
        for (String term : terms) {
            if (contains(cabin, term)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBedTerm(Cabin cabin, String... terms) {
        if (cabin.getRoomTypes() != null) {
            for (RoomType roomType : cabin.getRoomTypes()) {
                String bedText = normalizeSearchText(safe(roomType.getBedType()) + " "
                        + safe(roomType.getBeds()) + " "
                        + safe(roomType.getBedSummary()) + " "
                        + roomType.bedLabel());
                for (String term : terms) {
                    if (bedText.contains(normalizeSearchText(term))) {
                        return true;
                    }
                }
            }
        }
        return containsAny(cabin, terms);
    }

    private boolean hasTwinSingle(Cabin cabin) {
        if (cabin.getRoomTypes() != null) {
            for (RoomType roomType : cabin.getRoomTypes()) {
                String bedText = normalizeSearchText(safe(roomType.getBedType()) + " "
                        + safe(roomType.getBeds()) + " "
                        + safe(roomType.getBedSummary()) + " "
                        + roomType.bedLabel());
                if (bedText.contains("2 single") || bedText.contains("2 giuong don")
                        || (bedText.contains("single") && roomType.effectiveBedCount() >= 2)) {
                    return true;
                }
            }
        }
        return containsAny(cabin, "2 single", "single beds", "2 giuong don");
    }

    private String text(Cabin cabin) {
        StringBuilder builder = new StringBuilder();
        builder.append(safe(cabin.getName())).append(" ")
                .append(safe(cabin.getLocation())).append(" ")
                .append(safe(cabin.getAmenities())).append(" ")
                .append(safe(cabin.getDescription()));
        if (cabin.getRoomTypes() != null) {
            for (RoomType roomType : cabin.getRoomTypes()) {
                builder.append(" ")
                        .append(safe(roomType.getName())).append(" ")
                        .append(safe(roomType.getCategory())).append(" ")
                        .append(roomType.getSizeM2()).append("m2 ")
                        .append(roomType.sizeLabel()).append(" ")
                        .append(safe(roomType.getBedType())).append(" ")
                        .append(safe(roomType.getBeds())).append(" ")
                        .append(safe(roomType.getBedSummary())).append(" ")
                        .append(roomType.effectiveBedCount()).append(" beds ");
            }
        }
        return normalizeSearchText(builder.toString());
    }

    private String normalizedDestinationName() {
        String destination = destinationEditText.getText().toString().trim();
        return destination.isEmpty() ? DEST_HCM : destination;
    }

    private String formatCount(int value) {
        return String.format(Locale.US, "%,d", value).replace(",", ".");
    }

    private void openResultMap() {
        Intent intent = new Intent(this, HotelMapActivity.class);
        intent.putExtra("destination", destinationEditText.getText().toString());
        intent.putExtra("checkIn", selectedCheckIn);
        intent.putExtra("checkOut", selectedCheckOut);
        intent.putExtra("guests", guestCountFromInput(guestsEditText.getText().toString()));
        startActivity(intent);
    }

    private void setupInitialIntentFilters() {
        String location = getIntent().getStringExtra("location");
        int guests = getIntent().getIntExtra("guests", 0);
        if (location != null && !location.trim().isEmpty()) {
            destinationEditText.setText(location);
        }
        if (guests > 0) {
            selectedAdults = guests;
            guestsEditText.setText(formatGuestSummary(selectedRooms, selectedAdults, selectedBeds));
        }
    }

    private void setupSortSpinner() {
        String[] sortOptions = {
                "Phù hợp nhất",
                "Đang giảm giá",
                "Giá: thấp đến cao",
                "Giá: cao đến thấp",
                "Sức chứa: cao đến thấp",
                "Mới nhất trước"
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_on_primary, sortOptions);
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_serein_dropdown);
        sortSpinner.setAdapter(spinnerAdapter);
    }

    private void setupDateInputs() {
        checkInEditText.setOnClickListener(view -> showDatePicker(true));
        checkOutEditText.setOnClickListener(view -> showDatePicker(false));
    }

    private void showDatePicker(boolean checkIn) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_calendar_dialog);
        root.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView calendarTab = new TextView(this);
        calendarTab.setText("Lịch");
        calendarTab.setTextColor(getColor(R.color.booking_blue));
        calendarTab.setTextSize(15);
        calendarTab.setGravity(android.view.Gravity.CENTER);
        calendarTab.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(calendarTab, new LinearLayout.LayoutParams(0, dp(38), 1));
        TextView flexibleTab = new TextView(this);
        flexibleTab.setText("Linh hoạt");
        flexibleTab.setTextColor(getColor(R.color.booking_text));
        flexibleTab.setTextSize(15);
        flexibleTab.setGravity(android.view.Gravity.CENTER);
        header.addView(flexibleTab, new LinearLayout.LayoutParams(0, dp(38), 1));
        root.addView(header);

        LinearLayout months = new LinearLayout(this);
        months.setOrientation(LinearLayout.HORIZONTAL);
        YearMonth currentMonth = resolveInitialMonth(checkIn);
        addMonth(months, currentMonth, dialog);
        addMonth(months, currentMonth.plusMonths(1), dialog);
        root.addView(months, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView helper = new TextView(this);
        helper.setText("Chọn ngày nhận phòng, sau đó chọn ngày trả phòng. Khoảng ngày đã chọn sẽ được tô nổi bật.");
        helper.setTextColor(getColor(R.color.booking_muted));
        helper.setTextSize(12);
        helper.setPadding(0, dp(10), 0, dp(8));
        root.addView(helper);

        Button doneButton = new Button(this);
        doneButton.setText("Xong");
        doneButton.setAllCaps(false);
        doneButton.setTextColor(getColor(R.color.white));
        doneButton.setTextSize(15);
        doneButton.setTypeface(null, android.graphics.Typeface.BOLD);
        doneButton.setBackgroundResource(R.drawable.bg_button_primary);
        doneButton.setOnClickListener(view -> {
            updateDateTexts();
            if (resultsMode) {
                renderCabins();
            }
            dialog.dismiss();
        });
        root.addView(doneButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
        ));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private YearMonth resolveInitialMonth(boolean checkIn) {
        try {
            if (!checkIn && selectedCheckIn != null && !selectedCheckIn.trim().isEmpty()) {
                return YearMonth.from(LocalDate.parse(selectedCheckIn));
            }
            if (selectedCheckIn != null && !selectedCheckIn.trim().isEmpty()) {
                return YearMonth.from(LocalDate.parse(selectedCheckIn));
            }
        } catch (Exception ignored) {
        }
        return YearMonth.now();
    }

    private void addMonth(LinearLayout parent, YearMonth month, Dialog dialog) {
        LinearLayout monthBox = new LinearLayout(this);
        monthBox.setOrientation(LinearLayout.VERTICAL);
        monthBox.setPadding(dp(4), 0, dp(4), 0);

        TextView title = new TextView(this);
        title.setText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)));
        title.setTextColor(getColor(R.color.booking_text));
        title.setTextSize(16);
        title.setGravity(android.view.Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        monthBox.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(34)
        ));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : days) {
            TextView label = calendarCell(day);
            label.setTextColor(getColor(R.color.booking_muted));
            grid.addView(label);
        }

        int leadingBlanks = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leadingBlanks; i++) {
            grid.addView(calendarCell(""));
        }
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            TextView cell = calendarCell(String.valueOf(day));
            bindCalendarDate(cell, date, dialog);
            grid.addView(cell);
        }
        monthBox.addView(grid);
        parent.addView(monthBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
    }

    private TextView calendarCell(String text) {
        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setGravity(android.view.Gravity.CENTER);
        cell.setTextSize(13);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(38);
        params.height = dp(38);
        cell.setLayoutParams(params);
        return cell;
    }

    private void bindCalendarDate(TextView cell, LocalDate date, Dialog dialog) {
        boolean isPast = date.isBefore(LocalDate.now());
        cell.setTextColor(isPast ? getColor(R.color.booking_border) : getColor(R.color.booking_text));
        if (isSelectedBoundary(date)) {
            cell.setBackgroundResource(R.drawable.bg_calendar_selected);
            cell.setTextColor(getColor(R.color.white));
            cell.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (isInsideSelectedRange(date)) {
            cell.setBackgroundResource(R.drawable.bg_calendar_range);
            cell.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (isPast) {
            return;
        }
        cell.setOnClickListener(view -> {
            chooseCalendarDate(date);
            dialog.dismiss();
            showDatePicker(false);
        });
    }

    private boolean isSelectedBoundary(LocalDate date) {
        return date.toString().equals(selectedCheckIn) || date.toString().equals(selectedCheckOut);
    }

    private boolean isInsideSelectedRange(LocalDate date) {
        try {
            if (selectedCheckIn.trim().isEmpty() || selectedCheckOut.trim().isEmpty()) {
                return false;
            }
            LocalDate start = LocalDate.parse(selectedCheckIn);
            LocalDate end = LocalDate.parse(selectedCheckOut);
            return date.isAfter(start) && date.isBefore(end);
        } catch (Exception e) {
            return false;
        }
    }

    private void chooseCalendarDate(LocalDate date) {
        if (selectedCheckIn.trim().isEmpty() || !selectedCheckOut.trim().isEmpty()) {
            selectedCheckIn = date.toString();
            selectedCheckOut = "";
        } else {
            LocalDate start = LocalDate.parse(selectedCheckIn);
            if (date.isAfter(start)) {
                selectedCheckOut = date.toString();
            } else {
                selectedCheckIn = date.toString();
                selectedCheckOut = "";
            }
        }
        updateDateTexts();
    }

    private void updateDateTexts() {
        checkInEditText.setText(formatIsoDate(selectedCheckIn));
        checkOutEditText.setText(formatIsoDate(selectedCheckOut));
        updateContinueCard();
    }

    private String formatIsoDate(String isoDate) {
        try {
            return formatDisplayDate(LocalDate.parse(isoDate));
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDisplayDate(LocalDate date) {
        return formatVietnameseDate(date);
    }

    private String formatVietnameseDate(LocalDate date) {
        String[] weekdays = {"T2", "T3", "T4", "T5", "T6", "Thứ 7", "CN"};
        int dayIndex = Math.max(0, Math.min(6, date.getDayOfWeek().getValue() - 1));
        return weekdays[dayIndex] + ", " + date.getDayOfMonth() + " thg " + date.getMonthValue();
    }

    private TextWatcher renderingTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (resultsMode) {
                    renderCabins();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    private void loadCabins() {
        statusTextView.setText("Đang tải chỗ nghỉ...");
        cabinService.getCabins(new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                loadedCabins = cabins;
                if (roomTypeService != null) {
                    roomTypeService.attachRoomTypes(cabins, new SupabaseCallback<List<Cabin>>() {
                        @Override
                        public void onSuccess(List<Cabin> cabinsWithRooms) {
                            loadedCabins = cabinsWithRooms;
                            assignCheapestRoomTypes(loadedCabins);
                            updateCityCounts();
                            renderDiscountDeals();
                            if (resultsMode) {
                                renderCabins();
                            } else {
                                statusTextView.setText("Sẵn sàng tìm " + cabinsWithRooms.size() + " khách sạn.");
                            }
                        }

                        @Override
                        public void onError(String message) {
                            updateCityCounts();
                            renderDiscountDeals();
                            statusTextView.setText("Đã tải khách sạn, nhưng loại phòng chưa sẵn sàng: " + message);
                        }
                    });
                    return;
                }
                updateCityCounts();
                renderDiscountDeals();
                if (resultsMode) {
                    renderCabins();
                } else {
                    statusTextView.setText("Sẵn sàng tìm " + cabins.size() + " chỗ nghỉ.");
                }
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void renderDiscountDeals() {
        if (discountDealsSection == null || loadedCabins == null || loadedCabins.isEmpty()) {
            return;
        }

        List<Cabin> discountedCabins = new ArrayList<>();
        for (Cabin cabin : loadedCabins) {
            if (cabin.getDiscount() > 0) {
                discountedCabins.add(cabin);
            }
        }
        discountedCabins.sort((left, right) -> Double.compare(right.getDiscount(), left.getDiscount()));

        int visibleCount = Math.min(discountedCabins.size(), dealCards.length);
        discountDealsSection.setVisibility(visibleCount > 0 ? View.VISIBLE : View.GONE);
        for (int index = 0; index < dealCards.length; index++) {
            if (index < visibleCount) {
                bindDiscountDeal(index, discountedCabins.get(index));
            } else if (dealCards[index] != null) {
                dealCards[index].setVisibility(View.GONE);
            }
        }
    }

    private void bindDiscountDeal(int index, Cabin cabin) {
        View card = dealCards[index];
        if (card == null) {
            return;
        }
        card.setVisibility(View.VISIBLE);
        card.setOnClickListener(view -> openHotelDetail(cabin));

        if (dealTitles[index] != null) {
            dealTitles[index].setText(cabin.getName());
        }
        if (dealMetas[index] != null) {
            dealMetas[index].setText("Giảm " + PriceUtils.formatUsd(cabin.getDiscount()));
        }
        if (dealPrices[index] != null) {
            dealPrices[index].setText("Từ " + PriceUtils.formatUsd(price(cabin)) + " / đêm");
        }
        if (dealImages[index] != null) {
            Glide.with(this)
                    .load(cabin.getImage())
                    .centerCrop()
                    .placeholder(R.drawable.bg_dark_card)
                    .into(dealImages[index]);
        }
    }

    private void renderCabins() {
        if (!resultsMode) {
            updateCityCounts();
            return;
        }
        updateResultSummary();
        List<Cabin> result = new ArrayList<>(loadedCabins);
        SearchCriteria criteria = parseSearch(searchEditText.getText().toString());
        String destination = normalizeSearchText(destinationEditText.getText().toString().trim());
        if (!destination.isEmpty()) {
            addDestinationTerms(criteria, destination);
        }
        syncGuestStateFromInput();
        int structuredGuests = guestCountFromInput(guestsEditText.getText().toString());
        if (structuredGuests > 0) {
            criteria.guests = structuredGuests;
        }
        criteria.rooms = Math.max(1, selectedRooms);
        criteria.beds = Math.max(criteria.beds, selectedBeds);
        criteria.checkIn = selectedCheckIn;
        criteria.checkOut = selectedCheckOut;
        if (criteria.hasTextTerms()) {
            result.removeIf(cabin -> !matchesTextTerms(cabin, criteria.textTerms));
        }
        assignRoomMatches(result, criteria);
        result.removeIf(cabin -> cabin.getMatchedRoomType() == null && !canUseCabinFallback(cabin, criteria));
        if (criteria.maxPrice > 0) {
            result.removeIf(cabin -> cabin.displayPrice() > criteria.maxPrice);
        }
        if (criteria.discountOnly) {
            result.removeIf(cabin -> cabin.getDiscount() <= 0);
        }
        if (!selectedFilterKeys.isEmpty()) {
            ensureFilterOptions();
            result.removeIf(cabin -> !matchesSelectedFilters(cabin, selectedFilterKeys));
        }

        sortCabins(result);
        applyAvailabilityFilter(result, criteria);
    }

    private void sortCabins(List<Cabin> result) {
        int selected = currentSortMode;
        if (selected == 0) {
            result.sort((left, right) -> Boolean.compare(
                    !containsAny(left, "apartment", "loft", "suite"),
                    !containsAny(right, "apartment", "loft", "suite")
            ));
        } else if (selected == 1) {
            result.sort(Comparator
                    .comparingDouble((Cabin cabin) -> cabin.getDiscount() > 0 ? 0 : 1)
                    .thenComparing(cabin -> safe(cabin.getName()), String.CASE_INSENSITIVE_ORDER));
        } else if (selected == 2) {
            result.sort(Comparator.comparingDouble(cabin -> distanceScore(cabin)));
        } else if (selected == 3) {
            result.sort((left, right) -> Double.compare(reviewScore(right), reviewScore(left)));
        } else if (selected == 4) {
            result.sort(Comparator.comparingDouble(this::reviewScore));
        } else if (selected == 5) {
            result.sort((left, right) -> Double.compare(right.getDiscount(), left.getDiscount()));
        } else if (selected == 6) {
            result.sort((left, right) -> Double.compare(reviewScore(right) + right.getDiscount(), reviewScore(left) + left.getDiscount()));
        } else if (selected == 7) {
            result.sort(Comparator.comparingDouble(Cabin::displayPrice));
        } else if (selected == 8) {
            result.sort((left, right) -> Double.compare(right.displayPrice(), left.displayPrice()));
        }
    }

    private double distanceScore(Cabin cabin) {
        String haystack = text(cabin);
        if (haystack.contains("district 1") || haystack.contains("hoan kiem")) {
            return 1;
        }
        if (haystack.contains("district 3") || haystack.contains("front beach") || haystack.contains("back beach")) {
            return 2;
        }
        return 5;
    }

    private double reviewScore(Cabin cabin) {
        return cabin.getReviewScore() > 0 ? cabin.getReviewScore() : 6.0;
    }

    private void applyAvailabilityFilter(List<Cabin> result, SearchCriteria criteria) {
        availabilityRequestVersion++;
        int requestVersion = availabilityRequestVersion;
        if (!criteria.hasDateRange()) {
            List<Cabin> roomResults = expandRoomResults(result, criteria);
            adapter.submitList(roomResults);
            statusTextView.setText(formatCount(roomResults.size()) + " loại phòng phù hợp.");
            return;
        }
        if (!criteria.hasValidDateRange()) {
            adapter.submitList(result);
            statusTextView.setText("Chọn ngày trả phòng sau ngày nhận phòng.");
            return;
        }
        if (result.isEmpty()) {
            adapter.submitList(result);
            statusTextView.setText("Không có chỗ nghỉ phù hợp.");
            return;
        }

        statusTextView.setText("Đang kiểm tra phòng trống cho ngày đã chọn...");
        List<Cabin> availableCabins = new ArrayList<>();
        final int[] completed = {0};
        for (Cabin cabin : result) {
            List<RoomType> candidates = matchingRoomTypeCandidates(cabin, criteria);
            checkAvailableRoomCandidate(cabin, candidates, 0, criteria, requestVersion, result.size(), completed, availableCabins);
        }
    }

    private void checkAvailableRoomCandidate(
            Cabin cabin,
            List<RoomType> candidates,
            int index,
            SearchCriteria criteria,
            int requestVersion,
            int expectedCount,
            int[] completed,
            List<Cabin> availableCabins
    ) {
        if (requestVersion != availabilityRequestVersion) {
            return;
        }
        if (index >= candidates.size()) {
            if (canUseCabinFallback(cabin, criteria)) {
                bookingService.ensureRangeIsAvailable(cabin.getId(), criteria.checkIn, criteria.checkOut, new SupabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean available) {
                        if (requestVersion != availabilityRequestVersion) {
                            return;
                        }
                        if (Boolean.TRUE.equals(available)) {
                            availableCabins.add(cabin);
                        }
                        completeAvailabilityCheck(expectedCount, completed, availableCabins);
                    }

                    @Override
                    public void onError(String message) {
                        if (requestVersion != availabilityRequestVersion) {
                            return;
                        }
                        completeAvailabilityCheck(expectedCount, completed, availableCabins);
                    }
                });
                return;
            }
            completeAvailabilityCheck(expectedCount, completed, availableCabins);
            return;
        }
        RoomType roomType = candidates.get(index);
        bookingService.ensureRangeIsAvailable(cabin.getId(), roomType.getId(), criteria.checkIn, criteria.checkOut, criteria.rooms, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean available) {
                if (requestVersion != availabilityRequestVersion) {
                    return;
                }
                if (Boolean.TRUE.equals(available)) {
                    availableCabins.add(cabin.copyForMatchedRoom(roomType));
                    completeAvailabilityCheck(expectedCount, completed, availableCabins);
                    return;
                }
                checkAvailableRoomCandidate(cabin, candidates, index + 1, criteria, requestVersion, expectedCount, completed, availableCabins);
            }

            @Override
            public void onError(String message) {
                if (requestVersion != availabilityRequestVersion) {
                    return;
                }
                checkAvailableRoomCandidate(cabin, candidates, index + 1, criteria, requestVersion, expectedCount, completed, availableCabins);
            }
        });
    }

    private void completeAvailabilityCheck(int expectedCount, int[] completed, List<Cabin> availableCabins) {
        completed[0]++;
        if (completed[0] < expectedCount) {
            return;
        }
        sortCabins(availableCabins);
        adapter.submitList(availableCabins);
        statusTextView.setText(formatCount(availableCabins.size()) + " loại phòng còn trống cho ngày đã chọn.");
    }

    private List<Cabin> expandRoomResults(List<Cabin> cabins, SearchCriteria criteria) {
        List<Cabin> roomResults = new ArrayList<>();
        for (Cabin cabin : cabins) {
            List<RoomType> candidates = matchingRoomTypeCandidates(cabin, criteria);
            if (candidates.isEmpty()) {
                roomResults.add(cabin);
                continue;
            }
            int added = 0;
            for (RoomType roomType : candidates) {
                roomResults.add(cabin.copyForMatchedRoom(roomType));
                added++;
                if (added >= 4) {
                    break;
                }
            }
        }
        roomResults.sort(Comparator
                .comparing((Cabin cabin) -> safe(cabin.getName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparingDouble(Cabin::displayPrice));
        return roomResults;
    }

    private SearchCriteria parseSearch(String input) {
        String query = normalizeSearchText(input);
        SearchCriteria criteria = new SearchCriteria();
        criteria.discountOnly = query.contains("discount") || query.contains("sale") || query.contains("deal");

        Matcher guestMatcher = Pattern.compile("(\\d+)\\s*(nguoi lon|adult|adults|guest|guests|khach|nguoi|people|person|persons)").matcher(query);
        if (guestMatcher.find()) {
            criteria.guests = parseIntOrZero(guestMatcher.group(1));
            query = query.replace(guestMatcher.group(0), " ");
        }

        Matcher roomMatcher = Pattern.compile("(\\d+)\\s*(phong|room|rooms)").matcher(query);
        if (roomMatcher.find()) {
            criteria.rooms = Math.max(1, parseIntOrZero(roomMatcher.group(1)));
            query = query.replace(roomMatcher.group(0), " ");
        }

        Matcher bedMatcher = Pattern.compile("(\\d+)\\s*(giuong|bed|beds)").matcher(query);
        if (bedMatcher.find()) {
            criteria.beds = parseIntOrZero(bedMatcher.group(1));
            query = query.replace(bedMatcher.group(0), " ");
        }

        Matcher maxPriceMatcher = Pattern.compile("(under|below|max|less than|<=|duoi)\\s*\\$?\\s*(\\d+(?:\\.\\d+)?)").matcher(query);
        if (maxPriceMatcher.find()) {
            criteria.maxPrice = parseDoubleOrZero(maxPriceMatcher.group(2));
            query = query.replace(maxPriceMatcher.group(0), " ");
        } else {
            Matcher dollarMatcher = Pattern.compile("\\$\\s*(\\d+(?:\\.\\d+)?)").matcher(query);
            if (dollarMatcher.find()) {
                criteria.maxPrice = parseDoubleOrZero(dollarMatcher.group(1));
                query = query.replace(dollarMatcher.group(0), " ");
            }
        }

        Matcher sizeMatcher = Pattern.compile("(\\d+)\\s*(m2|sqm|met|meter|met vuong|m vuong)").matcher(query);
        if (sizeMatcher.find()) {
            criteria.roomQuery = sizeMatcher.group(1);
        }
        if (query.contains("standard")) {
            criteria.roomQuery = appendRoomQuery(criteria.roomQuery, "standard");
        }
        if (query.contains("superior")) {
            criteria.roomQuery = appendRoomQuery(criteria.roomQuery, "superior");
        }
        if (query.contains("deluxe")) {
            criteria.roomQuery = appendRoomQuery(criteria.roomQuery, "deluxe");
        }
        if (query.contains("suite")) {
            criteria.roomQuery = appendRoomQuery(criteria.roomQuery, "suite");
        }

        query = query.replace("discount", " ")
                .replace("sale", " ")
                .replace("deal", " ")
                .replace(",", " ")
                .replace(";", " ");
        for (String term : query.split("\\s+")) {
            if (isSearchKeyword(term)) {
                criteria.textTerms.add(term.trim());
            }
        }
        return criteria;
    }

    private boolean isSearchKeyword(String term) {
        String clean = normalizeSearchText(term).replaceAll("[^a-z0-9]", "").trim();
        if (clean.length() < 2 || clean.matches("\\d+")) {
            return false;
        }
        return !isSearchStopWord(clean);
    }

    private boolean isSearchStopWord(String term) {
        return term.equals("tp")
                || term.equals("thanh")
                || term.equals("pho")
                || term.equals("viet")
                || term.equals("nam")
                || term.equals("vietnam")
                || term.equals("thu")
                || term.equals("cn")
                || term.equals("thg")
                || term.equals("thang")
                || term.equals("ngay")
                || term.equals("phong")
                || term.equals("room")
                || term.equals("rooms")
                || term.equals("nguoi")
                || term.equals("lon")
                || term.equals("khach")
                || term.equals("guest")
                || term.equals("guests")
                || term.equals("adult")
                || term.equals("adults")
                || term.equals("giuong")
                || term.equals("bed")
                || term.equals("beds");
    }

    private int parseIntOrZero(String value) {
        try {
            return value == null || value.trim().isEmpty() ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int guestCountFromInput(String value) {
        String normalized = normalizeSearchText(value);
        Matcher adultMatcher = Pattern.compile("(\\d+)\\s*(nguoi lon|adult|adults|guest|guests|people|person|persons)").matcher(normalized);
        if (adultMatcher.find()) {
            return parseIntOrZero(adultMatcher.group(1));
        }
        return parseIntOrZero(value);
    }

    private void syncGuestStateFromInput() {
        String normalized = normalizeSearchText(guestsEditText == null ? "" : guestsEditText.getText().toString());
        Matcher roomMatcher = Pattern.compile("(\\d+)\\s*phong").matcher(normalized);
        if (roomMatcher.find()) {
            selectedRooms = Math.max(1, parseIntOrZero(roomMatcher.group(1)));
        }
        Matcher bedMatcher = Pattern.compile("(\\d+)\\s*(giuong|bed|beds)").matcher(normalized);
        if (bedMatcher.find()) {
            selectedBeds = Math.max(0, parseIntOrZero(bedMatcher.group(1)));
        }
        int adults = guestCountFromInput(normalized);
        if (adults > 0) {
            selectedAdults = adults;
        }
    }

    private double parseDoubleOrZero(String value) {
        try {
            return value == null || value.trim().isEmpty() ? 0 : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean matchesTextTerms(Cabin cabin, List<String> terms) {
        String haystack = text(cabin);
        for (String term : terms) {
            if (!haystack.contains(normalizeSearchText(term))) {
                return false;
            }
        }
        return true;
    }

    private void assignCheapestRoomTypes(List<Cabin> cabins) {
        for (Cabin cabin : cabins) {
            RoomType cheapest = null;
            if (cabin.getRoomTypes() != null) {
                for (RoomType roomType : cabin.getRoomTypes()) {
                    if (cheapest == null || roomType.getBasePrice() < cheapest.getBasePrice()) {
                        cheapest = roomType;
                    }
                }
            }
            cabin.setMatchedRoomType(cheapest);
        }
    }

    private void assignRoomMatches(List<Cabin> cabins, SearchCriteria criteria) {
        String query = criteria.roomQuery == null || criteria.roomQuery.trim().isEmpty()
                ? searchEditText.getText().toString()
                : criteria.roomQuery;
        for (Cabin cabin : cabins) {
            cabin.setMatchedRoomType(roomTypeService.findBestRoomType(cabin, criteria.guests, criteria.beds, query));
        }
    }

    private List<RoomType> matchingRoomTypeCandidates(Cabin cabin, SearchCriteria criteria) {
        List<RoomType> candidates = new ArrayList<>();
        if (cabin.getRoomTypes() != null) {
            String query = criteria.roomQuery == null || criteria.roomQuery.trim().isEmpty()
                    ? searchEditText.getText().toString()
                    : criteria.roomQuery;
            for (RoomType roomType : cabin.getRoomTypes()) {
                if (roomTypeMatchesSearch(roomType, criteria.guests, criteria.beds, query)) {
                    candidates.add(roomType);
                }
            }
        }
        if (candidates.isEmpty() && cabin.getMatchedRoomType() != null) {
            candidates.add(cabin.getMatchedRoomType());
        }
        candidates.sort(Comparator.comparingDouble(RoomType::getBasePrice));
        return candidates;
    }

    private boolean canUseCabinFallback(Cabin cabin, SearchCriteria criteria) {
        boolean hasRoomTypes = cabin.getRoomTypes() != null && !cabin.getRoomTypes().isEmpty();
        if (hasRoomTypes) {
            return false;
        }
        int guests = criteria == null ? 0 : criteria.guests;
        int rooms = criteria == null ? 1 : Math.max(1, criteria.rooms);
        int beds = criteria == null ? 0 : criteria.beds;
        return rooms <= 1
                && beds <= 1
                && (guests <= 0 || cabin.getMaxCapacity() >= guests);
    }

    private boolean roomTypeMatchesSearch(RoomType roomType, int guests, int beds, String queryValue) {
        if (roomType == null || !roomType.isActive()) {
            return false;
        }
        if (!roomTypeService.fitsGuestsAndBeds(roomType, guests, beds)) {
            return false;
        }
        String query = normalizeSearchText(queryValue)
                .replaceAll("(\\d+)\\s*(giuong|bed|beds)", " ");
        boolean asksStandard = query.contains("standard");
        boolean asksSuperior = query.contains("superior");
        boolean asksDeluxe = query.contains("deluxe");
        boolean asksSuite = query.contains("suite");
        boolean asksCategory = asksStandard || asksSuperior || asksDeluxe || asksSuite;
        String haystack = normalizeSearchText(roomType.getName() + " "
                + roomType.getCategory() + " "
                + roomType.getSize() + " "
                + roomType.getSizeM2() + " m2 "
                + roomType.getBedType() + " "
                + roomType.getBeds() + " "
                + roomType.getBedSummary() + " "
                + roomType.effectiveBedCount() + " beds");

        if (asksStandard && !haystack.contains("standard")) {
            return false;
        }
        if (asksSuperior && !haystack.contains("superior")) {
            return false;
        }
        if (asksDeluxe && !haystack.contains("deluxe")) {
            return false;
        }
        if (asksSuite && !haystack.contains("suite")) {
            return false;
        }

        int requestedSize = firstRoomSize(query);
        if (requestedSize > 0 && roomType.getSizeM2() > 0) {
            return Math.abs(roomType.getSizeM2() - requestedSize) <= 8 || roomType.getSizeM2() >= requestedSize;
        }
        return !asksCategory || haystack.contains(query) || query.trim().isEmpty();
    }

    private int firstRoomSize(String query) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*(m2|sqm|met|meter|met vuong|m vuong)").matcher(query);
        if (matcher.find()) {
            return parseIntOrZero(matcher.group(1));
        }
        return 0;
    }

    private String appendRoomQuery(String current, String addition) {
        if (current == null || current.trim().isEmpty()) {
            return addition;
        }
        return current + " " + addition;
    }

    private void addDestinationTerms(SearchCriteria criteria, String destination) {
        String normalizedDestination = normalizeSearchText(destination);
        if (normalizedDestination.contains("ho chi minh")
                || (normalizedDestination.contains("ho") && normalizedDestination.contains("chi") && normalizedDestination.contains("minh"))
                || normalizedDestination.contains("hcm")
                || normalizedDestination.contains("saigon")
                || normalizedDestination.contains("sai gon")
                || normalizedDestination.contains("tp.")
                || normalizedDestination.contains("tp ")
                || normalizedDestination.contains("vi tri hien tai")
                || normalizedDestination.contains("gan toi")
                || normalizedDestination.contains("near me")) {
            criteria.textTerms.add("ho chi minh");
            return;
        }
        if (normalizedDestination.contains("vung tau")) {
            criteria.textTerms.add("vung tau");
            return;
        }
        if (normalizedDestination.contains("ha noi") || normalizedDestination.contains("hanoi")) {
            criteria.textTerms.add("hanoi");
            return;
        }
        if (normalizedDestination.contains("da nang")) {
            criteria.textTerms.add("da nang");
            return;
        }
        if (normalizedDestination.contains("da lat")) {
            criteria.textTerms.add("da lat");
            return;
        }
        for (String term : normalizedDestination.replace(",", " ").split("\\s+")) {
            String cleanTerm = term.trim();
            if (cleanTerm.equals("tp") || cleanTerm.equals("vn") || cleanTerm.equals("viet")
                    || cleanTerm.equals("nam") || cleanTerm.equals("vietnam")) {
                continue;
            }
            if (cleanTerm.length() >= 2) {
                criteria.textTerms.add(cleanTerm);
            }
        }
    }

    private String normalizeSearchText(String value) {
        String lower = safe(value).trim().toLowerCase(Locale.US);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replace("đ", "d");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface CabinMatcher {
        boolean matches(Cabin cabin);
    }

    private static class FilterOption {
        private final String section;
        private final String key;
        private final String label;
        private final CabinMatcher matcher;

        private FilterOption(String section, String key, String label, CabinMatcher matcher) {
            this.section = section;
            this.key = key;
            this.label = label;
            this.matcher = matcher;
        }
    }

    private static class FacetRow {
        private final FilterOption option;
        private final TextView label;
        private final CheckBox checkBox;

        private FacetRow(FilterOption option, TextView label, CheckBox checkBox) {
            this.option = option;
            this.label = label;
            this.checkBox = checkBox;
        }
    }

    private static class SearchCriteria {
        private int guests;
        private int beds;
        private double maxPrice;
        private boolean discountOnly;
        private String checkIn = "";
        private String checkOut = "";
        private String roomQuery = "";
        private int rooms = 1;
        private final List<String> textTerms = new ArrayList<>();

        private boolean hasTextTerms() {
            return !textTerms.isEmpty();
        }

        private boolean hasDateRange() {
            return checkIn != null && !checkIn.trim().isEmpty()
                    || checkOut != null && !checkOut.trim().isEmpty();
        }

        private boolean hasValidDateRange() {
            try {
                return LocalDate.parse(checkOut).isAfter(LocalDate.parse(checkIn));
            } catch (Exception e) {
                return false;
            }
        }
    }
}

