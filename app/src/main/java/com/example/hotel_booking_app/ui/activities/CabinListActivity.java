package com.example.hotel_booking_app.ui.activities;

import android.app.Dialog;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.ui.adapters.CabinAdapter;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;

public class CabinListActivity extends AppCompatActivity {
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
    private View continueSection;
    private TextView continueCityTextView;
    private TextView continueMetaTextView;
    private TextView hcmCountTextView;
    private TextView vungTauCountTextView;
    private TextView haNoiCountTextView;
    private CabinAdapter adapter;
    private CabinService cabinService;
    private BookingService bookingService;
    private List<Cabin> loadedCabins = new ArrayList<>();
    private String selectedCheckIn = "";
    private String selectedCheckOut = "";
    private int availabilityRequestVersion = 0;
    private boolean resultsMode = false;
    private boolean hasCityExploration = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isHostOrAdmin()) {
            startActivity(new Intent(this, HostDashboardActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_cabin_list);

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
        searchEditText = findViewById(R.id.edit_search);
        destinationEditText = findViewById(R.id.edit_destination);
        checkInEditText = findViewById(R.id.edit_check_in);
        checkOutEditText = findViewById(R.id.edit_check_out);
        guestsEditText = findViewById(R.id.edit_guests);
        sortSpinner = findViewById(R.id.spinner_sort);
        Button applyFiltersButton = findViewById(R.id.button_apply_filters);
        Button resultBackButton = findViewById(R.id.button_result_back);
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
        adapter = new CabinAdapter(cabin -> {
            Intent intent = new Intent(this, CabinDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabin.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        setupDefaultSearchState();
        setupInitialIntentFilters();
        setupSortSpinner();
        setupDateInputs();
        setupDestinationChips();
        loadCityCardImages();
        startHeaderIconAnimations();
        sortSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (resultsMode && !loadedCabins.isEmpty()) {
                    renderCabins();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        applyFiltersButton.setOnClickListener(view -> showResults());
        resultBackButton.setOnClickListener(view -> showHome());
        sortResultsButton.setOnClickListener(view -> showSortDialog());
        filterResultsButton.setOnClickListener(view -> showFilterDialog());
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
        personalTab.setOnClickListener(view -> startActivity(new Intent(this, PersonalActivity.class)));
        bookingsTab.setOnClickListener(view -> openLoginRequired(MyBookingsActivity.class));
        wishlistTab.setOnClickListener(view -> openLoginRequired(MyWishlistActivity.class));
        messagesTab.setOnClickListener(view -> openLoginRequired(MessagesActivity.class));
        showHome();
        loadCabins();
    }

    private void setupDestinationChips() {
        bindDestinationCard(R.id.card_city_hcm, "TP. Ho Chi Minh");
        bindDestinationCard(R.id.card_city_vung_tau, "Vung Tau");
        bindDestinationCard(R.id.card_city_ha_noi, "Ha Noi");
        findViewById(R.id.card_continue_exploration).setOnClickListener(view -> showResults());
    }

    private void loadCityCardImages() {
        loadCityImage(R.id.image_city_hcm, "https://images.unsplash.com/photo-1583417319070-4a69db38a482?auto=format&fit=crop&w=1000&q=80");
        loadCityImage(R.id.image_city_vung_tau, "https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1000&q=80");
        loadCityImage(R.id.image_city_ha_noi, "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1000&q=80");
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
        selectedCheckIn = today.toString();
        selectedCheckOut = today.plusDays(1).toString();
        guestsEditText.setText("2");
        updateDateTexts();
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
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        Class<?> resolvedTarget = sessionManager.isHostOrAdmin() && target == MyBookingsActivity.class
                ? AdminBookingsActivity.class
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
        if (destinationEditText.getText().toString().trim().isEmpty()) {
            destinationEditText.setText("TP. Ho Chi Minh");
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
            destination = "TP. Ho Chi Minh";
        }
        resultSummaryTextView.setText(destination + " - " + compactDateRange() + " - " + guestSummary());
    }

    private void updateContinueCard() {
        if (continueCityTextView == null) {
            return;
        }
        String destination = destinationEditText.getText().toString().trim();
        continueCityTextView.setText(destination.isEmpty() ? "TP. Ho Chi Minh" : destination);
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
            return LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("MMM d", Locale.US));
        } catch (Exception e) {
            return "";
        }
    }

    private String guestSummary() {
        int guests = parseIntOrZero(guestsEditText.getText().toString());
        if (guests <= 0) {
            guests = 2;
        }
        return "1 phòng - " + guests + " người lớn";
    }

    private void updateCityCounts() {
        if (hcmCountTextView == null || loadedCabins == null) {
            return;
        }
        hcmCountTextView.setText(countCity("ho chi minh") + " chỗ nghỉ");
        vungTauCountTextView.setText(countCity("vung tau") + " chỗ nghỉ");
        haNoiCountTextView.setText(countCity("ha noi") + " chỗ nghỉ");
    }

    private int countCity(String city) {
        int count = 0;
        for (Cabin cabin : loadedCabins) {
            if (safe(cabin.getLocation()).toLowerCase(Locale.US).contains(city)
                    || safe(cabin.getName()).toLowerCase(Locale.US).contains(city)) {
                count++;
            }
        }
        return count;
    }

    private void showSortDialog() {
        String[] options = {
                "Phù hợp nhất",
                "Có giảm giá",
                "Giá: thấp đến cao",
                "Giá: cao đến thấp",
                "Sức chứa: cao đến thấp",
                "Mới nhất"
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
        keywordInput.setHint("Tiện nghi, khu vực, giá tối đa");
        keywordInput.setText(searchEditText.getText().toString());
        keywordInput.setBackgroundResource(R.drawable.bg_booking_field);
        keywordInput.setPadding(dp(12), 0, dp(12), 0);
        root.addView(keywordInput, inputParams);

        Button applyButton = new Button(this);
        applyButton.setText("Xem kết quả");
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

    private void openResultMap() {
        Intent intent = new Intent(this, SearchMapActivity.class);
        intent.putExtra("destination", destinationEditText.getText().toString());
        intent.putExtra("checkIn", selectedCheckIn);
        intent.putExtra("checkOut", selectedCheckOut);
        intent.putExtra("guests", parseIntOrZero(guestsEditText.getText().toString()));
        startActivity(intent);
    }

    private void setupInitialIntentFilters() {
        String location = getIntent().getStringExtra("location");
        int guests = getIntent().getIntExtra("guests", 0);
        if (location != null && !location.trim().isEmpty()) {
            destinationEditText.setText(location);
        }
        if (guests > 0) {
            guestsEditText.setText(String.valueOf(guests));
        }
    }

    private void setupSortSpinner() {
        String[] sortOptions = {
                "Best match",
                "Discounted cabins",
                "Price: low to high",
                "Price: high to low",
                "Capacity: high to low",
                "Newest first"
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
        helper.setText("Chọn ngày nhận phòng rồi chọn ngày trả phòng. Khoảng ngày đã chọn sẽ được tô xanh.");
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
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));
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
                updateCityCounts();
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

    private void renderCabins() {
        if (!resultsMode) {
            updateCityCounts();
            return;
        }
        updateResultSummary();
        List<Cabin> result = new ArrayList<>(loadedCabins);
        SearchCriteria criteria = parseSearch(searchEditText.getText().toString());
        String destination = destinationEditText.getText().toString().trim().toLowerCase(Locale.US);
        if (!destination.isEmpty()) {
            addDestinationTerms(criteria, destination);
        }
        int structuredGuests = parseIntOrZero(guestsEditText.getText().toString());
        if (structuredGuests > 0) {
            criteria.guests = structuredGuests;
        }
        criteria.checkIn = selectedCheckIn;
        criteria.checkOut = selectedCheckOut;
        if (criteria.hasTextTerms()) {
            result.removeIf(cabin -> !matchesTextTerms(cabin, criteria.textTerms));
        }
        if (criteria.guests > 0) {
            result.removeIf(cabin -> cabin.getMaxCapacity() < criteria.guests);
        }
        if (criteria.maxPrice > 0) {
            result.removeIf(cabin -> PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount()) > criteria.maxPrice);
        }
        if (criteria.discountOnly) {
            result.removeIf(cabin -> cabin.getDiscount() <= 0);
        }

        sortCabins(result);
        applyAvailabilityFilter(result, criteria);
    }

    private void sortCabins(List<Cabin> result) {
        int selected = sortSpinner.getSelectedItemPosition();
        if (selected == 0) {
            result.sort(Comparator
                    .comparingDouble((Cabin cabin) -> cabin.getDiscount() > 0 ? 0 : 1)
                    .thenComparing(cabin -> safe(cabin.getName()), String.CASE_INSENSITIVE_ORDER));
        } else if (selected == 1) {
            result.removeIf(cabin -> cabin.getDiscount() <= 0);
            result.sort(Comparator.comparing(cabin -> safe(cabin.getName()), String.CASE_INSENSITIVE_ORDER));
        } else if (selected == 2) {
            result.sort(Comparator.comparingDouble(cabin -> PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount())));
        } else if (selected == 3) {
            result.sort((left, right) -> Double.compare(
                    PriceUtils.priceAfterDiscount(right.getRegularPrice(), right.getDiscount()),
                    PriceUtils.priceAfterDiscount(left.getRegularPrice(), left.getDiscount())
            ));
        } else if (selected == 4) {
            result.sort((left, right) -> Integer.compare(right.getMaxCapacity(), left.getMaxCapacity()));
        }
    }

    private void applyAvailabilityFilter(List<Cabin> result, SearchCriteria criteria) {
        availabilityRequestVersion++;
        int requestVersion = availabilityRequestVersion;
        if (!criteria.hasDateRange()) {
            adapter.submitList(result);
            statusTextView.setText("Showing " + result.size() + " cabin(s).");
            return;
        }
        if (!criteria.hasValidDateRange()) {
            adapter.submitList(result);
            statusTextView.setText("Choose a check-out date after check-in.");
            return;
        }
        if (result.isEmpty()) {
            adapter.submitList(result);
            statusTextView.setText("No cabins match this search.");
            return;
        }

        statusTextView.setText("Checking availability for selected dates...");
        List<Cabin> availableCabins = new ArrayList<>();
        final int[] completed = {0};
        for (Cabin cabin : result) {
            bookingService.ensureRangeIsAvailable(cabin.getId(), criteria.checkIn, criteria.checkOut, new SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean available) {
                    if (requestVersion != availabilityRequestVersion) {
                        return;
                    }
                    if (Boolean.TRUE.equals(available)) {
                        availableCabins.add(cabin);
                    }
                    completeAvailabilityCheck(result.size(), completed, availableCabins);
                }

                @Override
                public void onError(String message) {
                    if (requestVersion != availabilityRequestVersion) {
                        return;
                    }
                    completeAvailabilityCheck(result.size(), completed, availableCabins);
                }
            });
        }
    }

    private void completeAvailabilityCheck(int expectedCount, int[] completed, List<Cabin> availableCabins) {
        completed[0]++;
        if (completed[0] < expectedCount) {
            return;
        }
        sortCabins(availableCabins);
        adapter.submitList(availableCabins);
        statusTextView.setText(availableCabins.size() + " cabin(s) available for your dates.");
    }

    private SearchCriteria parseSearch(String input) {
        String query = input == null ? "" : input.trim().toLowerCase(Locale.US);
        SearchCriteria criteria = new SearchCriteria();
        criteria.discountOnly = query.contains("discount") || query.contains("sale") || query.contains("deal");

        Matcher guestMatcher = Pattern.compile("(\\d+)\\s*(guest|guests|khach|people|person|persons)").matcher(query);
        if (guestMatcher.find()) {
            criteria.guests = parseIntOrZero(guestMatcher.group(1));
            query = query.replace(guestMatcher.group(0), " ");
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

        query = query.replace("discount", " ")
                .replace("sale", " ")
                .replace("deal", " ")
                .replace(",", " ")
                .replace(";", " ");
        for (String term : query.split("\\s+")) {
            if (term.trim().length() >= 2) {
                criteria.textTerms.add(term.trim());
            }
        }
        return criteria;
    }

    private int parseIntOrZero(String value) {
        try {
            return value == null || value.trim().isEmpty() ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
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
        String haystack = (safe(cabin.getName()) + " "
                + safe(cabin.getLocation()) + " "
                + safe(cabin.getAmenities()) + " "
                + safe(cabin.getDescription())).toLowerCase(Locale.US);
        for (String term : terms) {
            if (!haystack.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private void addDestinationTerms(SearchCriteria criteria, String destination) {
        if (destination.contains("ho chi minh") || destination.contains("hcm") || destination.contains("tp.")) {
            criteria.textTerms.add("ho chi minh");
            return;
        }
        if (destination.contains("vung tau")) {
            criteria.textTerms.add("vung tau");
            return;
        }
        if (destination.contains("ha noi") || destination.contains("hanoi")) {
            criteria.textTerms.add("ha noi");
            return;
        }
        for (String term : destination.replace(",", " ").split("\\s+")) {
            if (term.trim().length() >= 2) {
                criteria.textTerms.add(term.trim());
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class SearchCriteria {
        private int guests;
        private double maxPrice;
        private boolean discountOnly;
        private String checkIn = "";
        private String checkOut = "";
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
