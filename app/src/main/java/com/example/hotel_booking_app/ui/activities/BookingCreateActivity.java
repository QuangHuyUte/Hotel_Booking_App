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
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.CouponService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.time.LocalDate;
import java.time.ZoneId;

public class BookingCreateActivity extends AppCompatActivity {
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
    private RoomType selectedRoomType;
    private Coupon selectedCoupon;
    private double discountAmount;
    private BookingService bookingService;
    private RoomTypeService roomTypeService;
    private CouponService couponService;
    private SessionManager sessionManager;
    private LocalDate selectedStartDate;
    private LocalDate selectedEndDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_create);

        bookingService = new BookingService();
        roomTypeService = new RoomTypeService();
        couponService = new CouponService();
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập trước khi đặt phòng.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SignInActivity.class));
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
        welcomeNameTextView.setText(sessionManager.getFullName().isEmpty() ? "Khách" : sessionManager.getFullName());
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
                statusTextView.setText("Vui lòng chọn ngày nhận phòng trước.");
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
            statusTextView.setText("Ngày nhận phòng không được ở quá khứ.");
                return;
            }

            if (selectedCabin == null) {
                statusTextView.setText("Khách sạn vẫn đang tải.");
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
                        selectedDatesTextView.setText("Chọn ngày trả phòng sau ngày nhận phòng.");
                        statusTextView.setText("Vui lòng chọn lại ngày trả phòng sau ngày nhận phòng.");
                    } else {
                        selectedDatesTextView.setText("Nhận phòng: " + formatDisplayDate(selectedStartDate) + " | Trả phòng: Chọn ngày");
                        statusTextView.setText("Đã chọn ngày nhận phòng. Tiếp tục chọn ngày trả phòng.");
                    }
                }

                @Override
                public void onError(String message) {
                    selectedStartDate = null;
                    startDateEditText.setText("");
                    selectedDatesTextView.setText("Chọn ngày nhận phòng khác.");
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
                statusTextView.setText("Ngày trả phòng phải sau ngày nhận phòng.");
                return;
            }

            if (selectedCabin == null) {
                statusTextView.setText("Khách sạn vẫn đang tải.");
                return;
            }
            statusTextView.setText("Đang kiểm tra khoảng ngày lưu trú...");
            String roomTypeId = selectedRoomType == null ? null : selectedRoomType.getId();
            bookingService.ensureRangeIsAvailable(selectedCabin.getId(), roomTypeId, selectedStartDate.toString(), chosenDate.toString(), 1, new SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean available) {
                    selectedEndDate = chosenDate;
                    endDateEditText.setText(formatDisplayDate(chosenDate));
                    selectedDatesTextView.setText("Kỳ lưu trú: " + formatDisplayDate(selectedStartDate) + " -> " + formatDisplayDate(selectedEndDate));
                    statusTextView.setText("Đã chọn ngày. Bạn có thể đặt phòng ngay.");
                }

                @Override
                public void onError(String message) {
                    selectedEndDate = null;
                    endDateEditText.setText("");
                    selectedDatesTextView.setText("Chọn ngày trả phòng khác.");
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
                loadRoomType();
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
        String capacity = cabin.getMaxCapacity() + " khách";
        String description = cabin.getDescription() == null || cabin.getDescription().trim().isEmpty()
                ? "Một chỗ nghỉ ấm cúng, yên tĩnh và dễ chịu."
                : cabin.getDescription();

        heroTitleTextView.setText(cabin.getName());
        heroDescriptionTextView.setText(description);
        heroCapacityTextView.setText(capacity);
        heroPriceTextView.setText(price + " / đêm");
        reserveTitleTextView.setText("Đặt " + cabin.getName() + " hôm nay.\nThanh toán khi nhận phòng.");
        cabinTextView.setText(cabin.getName());
        cabinCapacityTextView.setText("Tối đa " + capacity);
        nightlyPriceTextView.setText(price + " mỗi đêm");
        Glide.with(this)
                .load(cabin.getImage())
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(cabinImageView);
    }

    private void loadRoomType() {
        String roomTypeId = getIntent().getStringExtra(AppConstants.EXTRA_ROOM_TYPE_ID);
        if (roomTypeId == null || roomTypeId.trim().isEmpty()) {
            return;
        }
        roomTypeService.getRoomTypeById(roomTypeId, new SupabaseCallback<RoomType>() {
            @Override
            public void onSuccess(RoomType roomType) {
                selectedRoomType = roomType;
                renderSelectedRoomType(roomType);
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Không tải được loại phòng đã chọn: " + message);
            }
        });
    }

    private void renderSelectedRoomType(RoomType roomType) {
        String summary = roomType.displayName()
                + " · " + roomType.sizeLabel()
                + " · " + roomType.bedLabel()
                + " · tối đa " + roomType.effectiveMaxAdults() + " người lớn"
                + " · " + roomType.effectiveBedCount() + " giường";
        heroCapacityTextView.setText(roomType.effectiveMaxAdults() + " người lớn");
        heroPriceTextView.setText(PriceUtils.formatUsd(roomType.getBasePrice()) + " / đêm");
        cabinCapacityTextView.setText(summary);
        nightlyPriceTextView.setText(PriceUtils.formatUsd(roomType.getBasePrice()) + " mỗi đêm");
        reserveTitleTextView.setText("Đặt " + roomType.displayName() + " tại " + selectedCabin.getName() + ".\nThanh toán khi nhận phòng.");
    }

    private void loadAvailability(String cabinId) {
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

    private void createBooking() {
        if (selectedCabin == null) {
            statusTextView.setText("Khách sạn vẫn đang tải.");
            return;
        }
        if (selectedStartDate == null || selectedEndDate == null) {
            statusTextView.setText("Vui lòng chọn ngày nhận phòng và trả phòng.");
            return;
        }
        if (!selectedEndDate.isAfter(selectedStartDate)) {
            statusTextView.setText("Ngày trả phòng phải sau ngày nhận phòng.");
            return;
        }

        int guests;
        try {
            String guestsValue = guestsEditText.getText().toString();
            guests = guestsValue.isEmpty() ? 0 : Integer.parseInt(guestsValue);
        } catch (NumberFormatException e) {
            statusTextView.setText("Số khách chưa hợp lệ.");
            return;
        }

        statusTextView.setText("Đang kiểm tra phòng trống và tạo đặt phòng...");
        SupabaseCallback<Booking> callback = new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                incrementCouponUsageIfNeeded();
                Toast.makeText(BookingCreateActivity.this, "Đã tạo đặt phòng", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Đặt phòng đang chờ xác nhận. Đang chuyển sang thanh toán...");
                Intent intent = new Intent(BookingCreateActivity.this, BookingPaymentActivity.class);
                intent.putExtra(AppConstants.EXTRA_BOOKING_ID, booking.getId());
                startActivity(intent);
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        };
        if (selectedRoomType != null) {
            bookingService.createBooking(
                    selectedCabin,
                    selectedRoomType,
                    sessionManager.getUserId(),
                    selectedStartDate.toString(),
                    selectedEndDate.toString(),
                    guests,
                    1,
                    breakfastCheckBox.isChecked(),
                    observationsEditText.getText().toString(),
                    selectedCoupon,
                    discountAmount,
                    callback
            );
        } else {
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
                    callback
            );
        }
    }

    private void applyCoupon() {
        String code = couponEditText.getText().toString().trim();
        if (code.isEmpty()) {
            selectedCoupon = null;
            discountAmount = 0;
            statusTextView.setText("Nhập mã giảm giá trước.");
            return;
        }

        double subtotal = calculateCurrentSubtotal();
        if (subtotal <= 0) {
            statusTextView.setText("Chọn ngày, số khách và khách sạn trước khi áp dụng mã giảm giá.");
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
                                "Mã " + coupon.getCode()
                                        + " đã áp dụng. Tạm tính: "
                                        + PriceUtils.formatUsd(subtotal)
                                        + " | Giảm: "
                                        + PriceUtils.formatUsd(discountAmount)
                                        + " | Tổng: "
                                        + PriceUtils.formatUsd(subtotal - discountAmount)
                        );
                    }

                    @Override
                    public void onError(String message) {
                        selectedCoupon = null;
                        discountAmount = 0;
                        statusTextView.setText("Mã giảm giá không hợp lệ: " + message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                selectedCoupon = null;
                discountAmount = 0;
                statusTextView.setText("Không tìm thấy mã giảm giá: " + message);
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
                statusTextView.setText(statusTextView.getText() + "\nLưu ý: chưa cập nhật lượt dùng mã giảm giá.");
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
        double nightly = selectedRoomType != null
                ? selectedRoomType.getBasePrice()
                : PriceUtils.priceAfterDiscount(selectedCabin.getRegularPrice(), selectedCabin.getDiscount());
        double cabinPrice = nightly * nights;
        double estimatedBreakfast = breakfastCheckBox.isChecked() ? 15 * guests * nights : 0;
        return cabinPrice + estimatedBreakfast;
    }
}
