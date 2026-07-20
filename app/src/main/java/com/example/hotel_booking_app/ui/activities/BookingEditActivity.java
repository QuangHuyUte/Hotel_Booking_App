package com.example.hotel_booking_app.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.utils.AppConstants;

public class BookingEditActivity extends AppCompatActivity {
    private static final double DEFAULT_BREAKFAST_PRICE = 15.0;

    private EditText guestsEditText;
    private EditText observationsEditText;
    private CheckBox breakfastCheckBox;
    private TextView statusTextView;
    private BookingService bookingService;
    private CabinService cabinService;
    private Booking currentBooking;
    private Cabin currentCabin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_edit);

        guestsEditText = findViewById(R.id.edit_guests);
        observationsEditText = findViewById(R.id.edit_observations);
        breakfastCheckBox = findViewById(R.id.check_breakfast);
        statusTextView = findViewById(R.id.text_status);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        Button updateButton = findViewById(R.id.button_update_booking);
        bookingService = new BookingService();
        cabinService = new CabinService();

        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        updateButton.setOnClickListener(view -> updateBooking());
        loadBooking();
    }

    private void loadBooking() {
        String bookingId = getIntent().getStringExtra(AppConstants.EXTRA_BOOKING_ID);
        statusTextView.setText("Đang tải đặt phòng...");
        bookingService.getBookingById(bookingId, new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                currentBooking = booking;
                guestsEditText.setText(String.valueOf(booking.getNumGuests()));
                breakfastCheckBox.setChecked(booking.hasBreakfast());
                observationsEditText.setText(booking.getObservations());
                loadCabin(booking.getCabinId());
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void loadCabin(String cabinId) {
        cabinService.getCabinById(cabinId, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                currentCabin = cabin;
                statusTextView.setText("Đặt phòng đã sẵn sàng để chỉnh sửa.");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Đã tải đặt phòng, nhưng chưa tải được sức chứa khách sạn.");
            }
        });
    }

    private void updateBooking() {
        if (currentBooking == null) {
            statusTextView.setText("Đặt phòng chưa được tải xong.");
            return;
        }
        int guests;
        try {
            guests = Integer.parseInt(guestsEditText.getText().toString().trim());
        } catch (NumberFormatException e) {
            statusTextView.setText("Số khách phải là số.");
            return;
        }
        if (guests <= 0) {
            statusTextView.setText("Số khách phải lớn hơn 0.");
            return;
        }
        if (currentCabin != null && guests > currentCabin.getMaxCapacity()) {
            statusTextView.setText("This cabin only supports up to " + currentCabin.getMaxCapacity() + " guests.");
            return;
        }

        statusTextView.setText("Đang cập nhật đặt phòng...");
        double extrasPrice = breakfastCheckBox.isChecked()
                ? DEFAULT_BREAKFAST_PRICE * guests * currentBooking.getNumNights()
                : 0;
        double totalPrice = currentBooking.getCabinPrice() + extrasPrice - currentBooking.getDiscountAmount();
        bookingService.updateBookingDetails(
                currentBooking.getId(),
                guests,
                breakfastCheckBox.isChecked(),
                observationsEditText.getText().toString().trim(),
                extrasPrice,
                totalPrice,
                new SupabaseCallback<Booking>() {
                    @Override
                    public void onSuccess(Booking booking) {
                        Toast.makeText(BookingEditActivity.this, "Đã cập nhật đặt phòng", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        statusTextView.setText(message);
                    }
                }
        );
    }
}
