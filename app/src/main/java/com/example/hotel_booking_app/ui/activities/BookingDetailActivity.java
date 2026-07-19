package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Rate;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class BookingDetailActivity extends AppCompatActivity {
    private TextView statusTextView;
    private TextView durationDatesTextView;
    private TextView durationNightsTextView;
    private TextView guestCountTextView;
    private TextView breakfastTextView;
    private TextView cabinPriceTextView;
    private TextView extrasPriceTextView;
    private TextView totalAmountTextView;
    private TextView bookedOnTextView;
    private TextView paymentStatusTextView;
    private LinearLayout ratingPanel;
    private RatingBar ratingBar;
    private EditText reviewEditText;
    private Button payNowButton;
    private Button cancelButton;
    private Button editButton;
    private BookingService bookingService;
    private CabinService cabinService;
    private SessionManager sessionManager;
    private Booking currentBooking;
    private String currentCabinName = "Cabin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        statusTextView = findViewById(R.id.text_status);
        durationDatesTextView = findViewById(R.id.text_duration_dates);
        durationNightsTextView = findViewById(R.id.text_duration_nights);
        guestCountTextView = findViewById(R.id.text_guest_count);
        breakfastTextView = findViewById(R.id.text_breakfast);
        cabinPriceTextView = findViewById(R.id.text_cabin_price);
        extrasPriceTextView = findViewById(R.id.text_extras_price);
        totalAmountTextView = findViewById(R.id.text_total_amount);
        bookedOnTextView = findViewById(R.id.text_booked_on);
        paymentStatusTextView = findViewById(R.id.text_payment_status);
        ratingPanel = findViewById(R.id.panel_rating);
        ratingBar = findViewById(R.id.rating_bar);
        reviewEditText = findViewById(R.id.edit_review);
        payNowButton = findViewById(R.id.button_pay_now);
        cancelButton = findViewById(R.id.button_cancel_booking);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        editButton = findViewById(R.id.button_edit_booking);
        Button reviewButton = findViewById(R.id.button_submit_review);

        bookingService = new BookingService();
        cabinService = new CabinService();
        sessionManager = new SessionManager(this);

        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        editButton.setOnClickListener(view -> openEditBooking());
        cancelButton.setOnClickListener(view -> cancelBooking());
        payNowButton.setOnClickListener(view -> openCheckout());
        reviewButton.setOnClickListener(view -> submitReview());
        loadBooking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentBooking != null) {
            loadBooking();
        }
    }

    private void loadBooking() {
        String bookingId = getIntent().getStringExtra(AppConstants.EXTRA_BOOKING_ID);
        statusTextView.setText("Loading booking detail...");
        bookingService.getBookingById(bookingId, new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                currentBooking = booking;
                renderBooking();
                loadCabinName(booking.getCabinId());
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void loadCabinName(String cabinId) {
        cabinService.getCabinById(cabinId, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                currentCabinName = cabin.getName();
                renderBooking();
            }

            @Override
            public void onError(String message) {
                currentCabinName = cabinId;
                renderBooking();
            }
        });
    }

    private void renderBooking() {
        if (currentBooking == null) {
            return;
        }
        durationDatesTextView.setText(formatDate(currentBooking.getStartDate()) + "  ->  " + formatDate(currentBooking.getEndDate()));
        durationNightsTextView.setText(currentBooking.getNumNights() + " night" + (currentBooking.getNumNights() == 1 ? "" : "s"));
        guestCountTextView.setText("Guests                                                        " + currentBooking.getNumGuests());
        breakfastTextView.setText("Breakfast                                             " + (currentBooking.hasBreakfast() ? "Included" : "Not included"));
        cabinPriceTextView.setText("Cabin Price (" + currentBooking.getNumNights() + " nights)                 " + PriceUtils.formatUsd(currentBooking.getCabinPrice()));
        extrasPriceTextView.setText("Breakfast & Extras                         " + PriceUtils.formatUsd(currentBooking.getExtrasPrice()));
        totalAmountTextView.setText("Total Amount                              " + PriceUtils.formatUsd(currentBooking.getTotalPrice()));
        bookedOnTextView.setText("Booked: " + formatCreatedDate(currentBooking.getCreatedAt()));
        renderPaymentState();
        boolean isAdmin = sessionManager.isHostOrAdmin();
        ratingPanel.setVisibility(!isAdmin && canRateStay() ? View.VISIBLE : View.GONE);
        editButton.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        cancelButton.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        cancelButton.setEnabled(!AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(currentBooking.getStatus()));
        if (isAdmin) {
            payNowButton.setVisibility(View.GONE);
        }
        statusTextView.setText(isAdmin
                ? currentCabinName + " booking detail is read-only for admin."
                : currentCabinName + " booking detail ready.");
    }

    private void renderPaymentState() {
        if (AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(currentBooking.getStatus())) {
            paymentStatusTextView.setText("CANCELLED");
            paymentStatusTextView.setBackgroundResource(R.drawable.bg_booking_badge_red);
            payNowButton.setVisibility(View.GONE);
            return;
        }
        if (currentBooking.isPaid()) {
            paymentStatusTextView.setText("PAID");
            paymentStatusTextView.setBackgroundResource(R.drawable.bg_booking_badge_green);
            payNowButton.setVisibility(View.GONE);
            return;
        }
        paymentStatusTextView.setText("PAYMENT PENDING");
        paymentStatusTextView.setBackgroundResource(R.drawable.bg_booking_badge_red);
        payNowButton.setVisibility(View.VISIBLE);
    }

    private boolean canRateStay() {
        if (currentBooking == null || AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(currentBooking.getStatus())) {
            return false;
        }
        if (AppConstants.BOOKING_CHECKED_OUT.equalsIgnoreCase(currentBooking.getStatus())) {
            return true;
        }
        try {
            return LocalDate.parse(currentBooking.getEndDate()).isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void openCheckout() {
        if (currentBooking == null) {
            statusTextView.setText("Booking is not loaded yet.");
            return;
        }
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra(AppConstants.EXTRA_BOOKING_ID, currentBooking.getId());
        startActivity(intent);
    }

    private void openEditBooking() {
        if (currentBooking == null) {
            statusTextView.setText("Booking is not loaded yet.");
            return;
        }
        Intent intent = new Intent(this, EditBookingActivity.class);
        intent.putExtra(AppConstants.EXTRA_BOOKING_ID, currentBooking.getId());
        startActivity(intent);
    }

    private void cancelBooking() {
        if (currentBooking == null) {
            statusTextView.setText("Booking is not loaded yet.");
            return;
        }
        statusTextView.setText("Cancelling booking...");
        bookingService.cancelBooking(currentBooking.getId(), new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                Toast.makeText(BookingDetailActivity.this, "Booking deleted", Toast.LENGTH_SHORT).show();
                currentBooking = booking;
                renderBooking();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void submitReview() {
        if (currentBooking == null) {
            statusTextView.setText("Booking is not loaded yet.");
            return;
        }
        int rating = Math.round(ratingBar.getRating());
        if (rating < 1 || rating > 5) {
            statusTextView.setText("Please choose a rating from 1 to 5 stars.");
            return;
        }
        statusTextView.setText("Submitting rating...");
        Rate rate = new Rate();
        rate.setUserId(sessionManager.getUserId());
        rate.setCabinId(currentBooking.getCabinId());
        rate.setBookingId(currentBooking.getId());
        rate.setRating(rating);
        rate.setComment(reviewEditText.getText().toString().trim());
        cabinService.createRate(rate, new SupabaseCallback<Rate>() {
            @Override
            public void onSuccess(Rate data) {
                Toast.makeText(BookingDetailActivity.this, "Review submitted", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Thanks for rating your stay.");
                ratingBar.setRating(0);
                reviewEditText.setText("");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(BookingDetailActivity.this, "Could not submit rating", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Could not submit rating: " + message);
            }
        });
    }

    private String formatDate(String isoDate) {
        try {
            return LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("MMM dd", Locale.US));
        } catch (Exception e) {
            return isoDate == null ? "-" : isoDate;
        }
    }

    private String formatCreatedDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        try {
            return OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));
            } catch (Exception secondIgnored) {
                try {
                    return LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US));
                } catch (Exception e) {
                    return value;
                }
            }
        }
    }
}
