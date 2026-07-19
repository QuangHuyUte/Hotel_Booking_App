package com.example.hotel_booking_app.ui.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Coupon;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.CouponService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.time.LocalDate;
import java.time.ZoneId;

public class BookingActivity extends AppCompatActivity {
    private TextView cabinTextView;
    private TextView cabinCapacityTextView;
    private TextView heroTitleTextView;
    private TextView heroDescriptionTextView;
    private TextView heroCapacityTextView;
    private TextView heroPriceTextView;
    private TextView reserveTitleTextView;
    private TextView welcomeNameTextView;
    private TextView nightlyPriceTextView;
    private TextView availabilityTextView;
    private TextView selectedDatesTextView;
    private TextView statusTextView;
    private ImageView cabinImageView;
    private EditText startDateEditText;
    private EditText endDateEditText;
    private EditText guestsEditText;
    private EditText couponEditText;
    private EditText observationsEditText;
    private CheckBox breakfastCheckBox;
    private Cabin selectedCabin;
    private Coupon selectedCoupon;
    private double discountAmount;
    private BookingService bookingService;
    private CouponService couponService;
    private SessionManager sessionManager;
    private LocalDate selectedStartDate;
    private LocalDate selectedEndDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        bookingService = new BookingService();
        couponService = new CouponService();
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in before booking.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        cabinImageView = findViewById(R.id.image_cabin);
        cabinTextView = findViewById(R.id.text_cabin);
        cabinCapacityTextView = findViewById(R.id.text_cabin_capacity);
        heroTitleTextView = findViewById(R.id.text_hero_title);
        heroDescriptionTextView = findViewById(R.id.text_hero_description);
        heroCapacityTextView = findViewById(R.id.text_hero_capacity);
        heroPriceTextView = findViewById(R.id.text_hero_price);
        reserveTitleTextView = findViewById(R.id.text_reserve_title);
        welcomeNameTextView = findViewById(R.id.text_welcome_name);
        nightlyPriceTextView = findViewById(R.id.text_nightly_price);
        availabilityTextView = findViewById(R.id.text_availability);
        selectedDatesTextView = findViewById(R.id.text_selected_dates);
        statusTextView = findViewById(R.id.text_status);
        startDateEditText = findViewById(R.id.edit_start_date);
        endDateEditText = findViewById(R.id.edit_end_date);
        guestsEditText = findViewById(R.id.edit_guests);
        couponEditText = findViewById(R.id.edit_coupon);
        observationsEditText = findViewById(R.id.edit_observations);
        breakfastCheckBox = findViewById(R.id.check_breakfast);
        Button applyCouponButton = findViewById(R.id.button_apply_coupon);
        Button confirmButton = findViewById(R.id.button_confirm_booking);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);

        setupDatePickers();
        applyInitialDatesFromIntent();
        welcomeNameTextView.setText(sessionManager.getFullName().isEmpty() ? "Guest" : sessionManager.getFullName());
        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        applyCouponButton.setOnClickListener(view -> applyCoupon());
        confirmButton.setOnClickListener(view -> createBooking());
        loadCabin();
    }

    private void setupDatePickers() {
        startDateEditText.setFocusable(false);
        startDateEditText.setCursorVisible(false);
        startDateEditText.setKeyListener(null);
        startDateEditText.setOnClickListener(view -> showStartDatePicker());

        endDateEditText.setFocusable(false);
        endDateEditText.setCursorVisible(false);
        endDateEditText.setKeyListener(null);
        endDateEditText.setOnClickListener(view -> {
            if (selectedStartDate == null) {
                statusTextView.setText("Please choose a check-in date first.");
                showStartDatePicker();
                return;
            }
            showEndDatePicker();
        });
    }

    private void applyInitialDatesFromIntent() {
        try {
            String checkIn = getIntent().getStringExtra("checkIn");
            String checkOut = getIntent().getStringExtra("checkOut");
            if (checkIn != null && !checkIn.trim().isEmpty()) {
                selectedStartDate = LocalDate.parse(checkIn);
                startDateEditText.setText(formatDisplayDate(selectedStartDate));
            }
            if (checkOut != null && !checkOut.trim().isEmpty()) {
                selectedEndDate = LocalDate.parse(checkOut);
                endDateEditText.setText(formatDisplayDate(selectedEndDate));
            }
        } catch (Exception ignored) {
        }
    }

    private void showStartDatePicker() {
        LocalDate today = LocalDate.now();
        LocalDate initialDate = selectedStartDate != null ? selectedStartDate : today;
        DatePickerDialog dialog = createDatePicker(initialDate, (view, year, month, dayOfMonth) -> {
            LocalDate chosenDate = LocalDate.of(year, month + 1, dayOfMonth);
            if (chosenDate.isBefore(today)) {
                statusTextView.setText("Check-in date cannot be in the past.");
                return;
            }

            if (selectedCabin == null) {
                statusTextView.setText("Cabin is still loading.");
                return;
            }
            statusTextView.setText("Checking this check-in date...");
            bookingService.ensureDateIsAvailable(selectedCabin.getId(), chosenDate.toString(), new SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean available) {
                    selectedStartDate = chosenDate;
                    startDateEditText.setText(formatDisplayDate(chosenDate));
                    if (selectedEndDate != null && !selectedEndDate.isAfter(selectedStartDate)) {
                        selectedEndDate = null;
                        endDateEditText.setText("");
                        selectedDatesTextView.setText("Choose a check-out date after check-in.");
                        statusTextView.setText("Please choose a new check-out date after check-in.");
                    } else {
                        selectedDatesTextView.setText("Check-in: " + formatDisplayDate(selectedStartDate) + " | Check-out: Chọn ngày");
                        statusTextView.setText("Check-in selected. Continue choosing check-out.");
                    }
                }

                @Override
                public void onError(String message) {
                    selectedStartDate = null;
                    startDateEditText.setText("");
                    selectedDatesTextView.setText("Choose another check-in date.");
                    statusTextView.setText(message);
                }
            });
        });
        dialog.getDatePicker().setMinDate(toMillis(today));
        dialog.show();
    }

    private void showEndDatePicker() {
        LocalDate minEndDate = selectedStartDate.plusDays(1);
        LocalDate initialDate = selectedEndDate != null && selectedEndDate.isAfter(selectedStartDate)
                ? selectedEndDate
                : minEndDate;
        DatePickerDialog dialog = createDatePicker(initialDate, (view, year, month, dayOfMonth) -> {
            LocalDate chosenDate = LocalDate.of(year, month + 1, dayOfMonth);
            if (!chosenDate.isAfter(selectedStartDate)) {
                statusTextView.setText("Check-out date must be after check-in.");
                return;
            }

            if (selectedCabin == null) {
                statusTextView.setText("Cabin is still loading.");
                return;
            }
            statusTextView.setText("Checking this stay range...");
            bookingService.ensureRangeIsAvailable(selectedCabin.getId(), selectedStartDate.toString(), chosenDate.toString(), new SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean available) {
                    selectedEndDate = chosenDate;
                    endDateEditText.setText(formatDisplayDate(chosenDate));
                    selectedDatesTextView.setText("Selected stay: " + formatDisplayDate(selectedStartDate) + " -> " + formatDisplayDate(selectedEndDate));
                    statusTextView.setText("Dates selected. You can now reserve this cabin.");
                }

                @Override
                public void onError(String message) {
                    selectedEndDate = null;
                    endDateEditText.setText("");
                    selectedDatesTextView.setText("Choose another check-out date.");
                    statusTextView.setText(message);
                }
            });
        });
        dialog.getDatePicker().setMinDate(toMillis(minEndDate));
        dialog.show();
    }

    private DatePickerDialog createDatePicker(LocalDate initialDate, DatePickerDialog.OnDateSetListener listener) {
        return new DatePickerDialog(
                this,
                listener,
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );
    }

    private long toMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String formatDisplayDate(LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", new java.util.Locale("vi", "VN")));
    }

    private void loadCabin() {
        String cabinId = getIntent().getStringExtra(AppConstants.EXTRA_CABIN_ID);
        new CabinService().getCabinById(cabinId, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                selectedCabin = cabin;
                renderCabin(cabin);
                loadAvailability(cabin.getId());
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void renderCabin(Cabin cabin) {
        double nightlyPrice = PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount());
        String price = PriceUtils.formatUsd(nightlyPrice);
        String capacity = cabin.getMaxCapacity() + " guests";
        String description = cabin.getDescription() == null || cabin.getDescription().trim().isEmpty()
                ? "Cozy cabin surrounded by quiet nature."
                : cabin.getDescription();

        heroTitleTextView.setText(cabin.getName());
        heroDescriptionTextView.setText(description);
        heroCapacityTextView.setText(capacity);
        heroPriceTextView.setText(price + " / night");
        reserveTitleTextView.setText("Reserve " + cabin.getName() + " today.\nPay on arrival.");
        cabinTextView.setText(cabin.getName());
        cabinCapacityTextView.setText("Max " + capacity);
        nightlyPriceTextView.setText(price + " per night");
        Glide.with(this)
                .load(cabin.getImage())
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(cabinImageView);
    }

    private void loadAvailability(String cabinId) {
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

    private void createBooking() {
        if (selectedCabin == null) {
            statusTextView.setText("Cabin is still loading.");
            return;
        }
        if (selectedStartDate == null || selectedEndDate == null) {
            statusTextView.setText("Please choose check-in and check-out dates.");
            return;
        }
        if (!selectedEndDate.isAfter(selectedStartDate)) {
            statusTextView.setText("Check-out date must be after check-in.");
            return;
        }

        int guests;
        try {
            String guestsValue = guestsEditText.getText().toString();
            guests = guestsValue.isEmpty() ? 0 : Integer.parseInt(guestsValue);
        } catch (NumberFormatException e) {
            statusTextView.setText("Guest count is not valid.");
            return;
        }

        statusTextView.setText("Checking availability and creating booking...");
        bookingService.createBooking(
                selectedCabin,
                sessionManager.getUserId(),
                selectedStartDate.toString(),
                selectedEndDate.toString(),
                guests,
                breakfastCheckBox.isChecked(),
                observationsEditText.getText().toString(),
                selectedCoupon,
                discountAmount,
                new SupabaseCallback<Booking>() {
                    @Override
                    public void onSuccess(Booking booking) {
                        incrementCouponUsageIfNeeded();
                        Toast.makeText(BookingActivity.this, "Booking created", Toast.LENGTH_SHORT).show();
                        statusTextView.setText("Booking pending. Moving to checkout...");
                        Intent intent = new Intent(BookingActivity.this, CheckoutActivity.class);
                        intent.putExtra(AppConstants.EXTRA_BOOKING_ID, booking.getId());
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String message) {
                        statusTextView.setText(message);
                    }
                }
        );
    }

    private void applyCoupon() {
        String code = couponEditText.getText().toString().trim();
        if (code.isEmpty()) {
            selectedCoupon = null;
            discountAmount = 0;
            statusTextView.setText("Enter a coupon code first.");
            return;
        }

        double subtotal = calculateCurrentSubtotal();
        if (subtotal <= 0) {
            statusTextView.setText("Choose dates, guests and cabin before applying a coupon.");
            return;
        }

        couponService.getCouponByCode(code, new SupabaseCallback<Coupon>() {
            @Override
            public void onSuccess(Coupon coupon) {
                couponService.validateCoupon(coupon, subtotal, new SupabaseCallback<Double>() {
                    @Override
                    public void onSuccess(Double discount) {
                        selectedCoupon = coupon;
                        discountAmount = discount;
                        statusTextView.setText(
                                "Coupon " + coupon.getCode()
                                        + " applied. Subtotal: "
                                        + PriceUtils.formatUsd(subtotal)
                                        + " | Discount: "
                                        + PriceUtils.formatUsd(discountAmount)
                                        + " | Total: "
                                        + PriceUtils.formatUsd(subtotal - discountAmount)
                        );
                    }

                    @Override
                    public void onError(String message) {
                        selectedCoupon = null;
                        discountAmount = 0;
                        statusTextView.setText("Coupon is not valid: " + message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                selectedCoupon = null;
                discountAmount = 0;
                statusTextView.setText("Coupon not found: " + message);
            }
        });
    }

    private void incrementCouponUsageIfNeeded() {
        if (selectedCoupon == null) {
            return;
        }
        couponService.incrementUsedCount(selectedCoupon, new SupabaseCallback<Coupon>() {
            @Override
            public void onSuccess(Coupon coupon) {
                selectedCoupon = coupon;
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(statusTextView.getText() + "\nNote: coupon usage was not updated.");
            }
        });
    }

    private double calculateCurrentSubtotal() {
        if (selectedCabin == null || selectedStartDate == null || selectedEndDate == null) {
            return 0;
        }
        int guests;
        try {
            guests = Integer.parseInt(guestsEditText.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(selectedStartDate, selectedEndDate);
        if (nights <= 0 || guests <= 0) {
            return 0;
        }
        double cabinPrice = PriceUtils.priceAfterDiscount(selectedCabin.getRegularPrice(), selectedCabin.getDiscount()) * nights;
        double estimatedBreakfast = breakfastCheckBox.isChecked() ? 15 * guests * nights : 0;
        return cabinPrice + estimatedBreakfast;
    }
}
