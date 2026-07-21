package com.example.hotel_booking_app.ui.activities;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.BlockedDate;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BlockedDateService;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoomUnitManagementActivity extends AppCompatActivity {
    public static final String EXTRA_CABIN_ID = "extra_room_unit_cabin_id";
    public static final String EXTRA_ROOM_TYPE_ID = "extra_room_unit_room_type_id";

    private TextView titleTextView;
    private TextView summaryTextView;
    private TextView calendarTitleTextView;
    private TextView blockTitleTextView;
    private TextView statusTextView;
    private EditText rangeStartEditText;
    private EditText rangeEndEditText;
    private EditText blockStartEditText;
    private EditText blockEndEditText;
    private EditText blockReasonEditText;
    private LinearLayout roomUnitsContainer;
    private LinearLayout roomCalendarContainer;
    private Button refreshButton;
    private Button blockSelectedButton;
    private RoomTypeService roomTypeService;
    private BookingService bookingService;
    private BlockedDateService blockedDateService;
    private SessionManager sessionManager;
    private RoomType roomType;
    private String cabinId;
    private String roomTypeId;
    private int selectedUnit = 1;
    private List<RoomUnitEvent> currentEvents = new ArrayList<>();
    private LocalDate currentStart;
    private LocalDate currentEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_unit_management);

        roomTypeService = new RoomTypeService();
        bookingService = new BookingService();
        blockedDateService = new BlockedDateService();
        sessionManager = new SessionManager(this);
        cabinId = getIntent().getStringExtra(EXTRA_CABIN_ID);
        roomTypeId = getIntent().getStringExtra(EXTRA_ROOM_TYPE_ID);

        titleTextView = findViewById(R.id.text_room_unit_title);
        summaryTextView = findViewById(R.id.text_room_unit_summary);
        calendarTitleTextView = findViewById(R.id.text_selected_calendar_title);
        blockTitleTextView = findViewById(R.id.text_block_title);
        statusTextView = findViewById(R.id.text_status);
        rangeStartEditText = findViewById(R.id.edit_range_start);
        rangeEndEditText = findViewById(R.id.edit_range_end);
        blockStartEditText = findViewById(R.id.edit_block_start);
        blockEndEditText = findViewById(R.id.edit_block_end);
        blockReasonEditText = findViewById(R.id.edit_block_reason);
        roomUnitsContainer = findViewById(R.id.container_room_units);
        roomCalendarContainer = findViewById(R.id.container_room_calendar);
        refreshButton = findViewById(R.id.button_refresh_units);
        blockSelectedButton = findViewById(R.id.button_block_selected_room);
        Button backButton = findViewById(R.id.button_back);

        LocalDate today = LocalDate.now();
        rangeStartEditText.setText(today.toString());
        rangeEndEditText.setText(today.plusDays(14).toString());
        blockStartEditText.setText(today.toString());
        blockEndEditText.setText(today.plusDays(1).toString());

        backButton.setOnClickListener(view -> finish());
        refreshButton.setOnClickListener(view -> loadRoomSchedule());
        blockSelectedButton.setOnClickListener(view -> blockSelectedRoom());

        loadRoomType();
    }

    private void loadRoomType() {
        if (roomTypeId == null || roomTypeId.trim().isEmpty() || cabinId == null || cabinId.trim().isEmpty()) {
            statusTextView.setText("Thiếu dữ liệu loại phòng.");
            return;
        }
        statusTextView.setText("Đang tải loại phòng...");
        roomTypeService.getRoomTypeById(roomTypeId, new SupabaseCallback<RoomType>() {
            @Override
            public void onSuccess(RoomType data) {
                roomType = data;
                selectedUnit = 1;
                titleTextView.setText("Quản lý " + roomType.titleLabel());
                blockTitleTextView.setText("Chặn lịch Phòng 01");
                loadRoomSchedule();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Không tải được loại phòng: " + message);
            }
        });
    }

    private void loadRoomSchedule() {
        if (roomType == null) {
            return;
        }
        LocalDate[] range = resolveRange();
        if (range == null) {
            return;
        }
        currentStart = range[0];
        currentEnd = range[1];
        statusTextView.setText("Đang tải lịch phòng...");
        bookingService.getBookingsForCabin(cabinId, new SupabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                blockedDateService.getBlockedDates(cabinId, new SupabaseCallback<List<BlockedDate>>() {
                    @Override
                    public void onSuccess(List<BlockedDate> blockedDates) {
                        currentEvents = buildRoomUnitEvents(bookings, blockedDates, Math.max(1, roomType.getTotalRooms()));
                        renderRoomTabs();
                        renderSelectedRoomCalendarV2();
                        statusTextView.setText("Đã cập nhật lịch phòng.");
                    }

                    @Override
                    public void onError(String message) {
                        statusTextView.setText("Không tải được lịch chặn: " + message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Không tải được booking: " + message);
            }
        });
    }

    private LocalDate[] resolveRange() {
        try {
            LocalDate start = LocalDate.parse(rangeStartEditText.getText().toString().trim());
            LocalDate end = LocalDate.parse(rangeEndEditText.getText().toString().trim());
            if (!end.isAfter(start)) {
                statusTextView.setText("Ngày kết thúc phải sau ngày bắt đầu.");
                return null;
            }
            if (ChronoUnit.DAYS.between(start, end) > 31) {
                statusTextView.setText("Khoảng xem lịch tối đa 31 ngày để dễ đọc.");
                return null;
            }
            return new LocalDate[]{start, end};
        } catch (Exception exception) {
            statusTextView.setText("Ngày phải đúng định dạng YYYY-MM-DD.");
            return null;
        }
    }

    private void renderRoomTabs() {
        int totalRooms = Math.max(1, roomType.getTotalRooms());
        int availableRooms = 0;
        roomUnitsContainer.removeAllViews();
        LinearLayout row = null;
        for (int unit = 1; unit <= totalRooms; unit++) {
            if ((unit - 1) % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowParams.bottomMargin = dp(8);
                roomUnitsContainer.addView(row, rowParams);
            }

            List<RoomUnitEvent> events = eventsForUnit(currentEvents, unit, currentStart, currentEnd);
            boolean available = events.isEmpty();
            if (available) {
                availableRooms++;
            }
            TextView card = label(String.format(Locale.US,
                    "Phòng %02d\n%s",
                    unit,
                    (unit == selectedUnit ? "Đang chọn · " : "")
                            + (available ? "Trống" : events.size() + " lịch bận")));
            card.setBackgroundResource(available ? R.drawable.bg_room_available : R.drawable.bg_room_booked);
            card.setTextColor(getColor(R.color.black));
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setMinHeight(dp(76));
            card.setPadding(dp(14), dp(10), dp(14), dp(10));
            int clickedUnit = unit;
            card.setOnClickListener(view -> {
                selectedUnit = clickedUnit;
                renderRoomTabs();
                renderSelectedRoomCalendarV2();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (unit % 2 == 1) {
                params.rightMargin = dp(5);
            } else {
                params.leftMargin = dp(5);
            }
            if (row != null) {
                row.addView(card, params);
            }
            if (unit == totalRooms && totalRooms % 2 == 1 && row != null) {
                TextView spacer = new TextView(this);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 1, 1f);
                spacerParams.leftMargin = dp(5);
                row.addView(spacer, spacerParams);
            }
        }
        summaryTextView.setText(String.format(Locale.US,
                "%s · %s · %d phòng\nKhoảng %s đến %s: còn %d/%d phòng trống.",
                roomType.titleLabel(),
                roomType.categoryLabel(),
                totalRooms,
                currentStart,
                currentEnd,
                availableRooms,
                totalRooms));
    }

    private void renderRoomUnits() {
        int totalRooms = Math.max(1, roomType.getTotalRooms());
        int availableRooms = 0;
        roomUnitsContainer.removeAllViews();
        for (int unit = 1; unit <= totalRooms; unit++) {
            List<RoomUnitEvent> events = eventsForUnit(currentEvents, unit, currentStart, currentEnd);
            boolean available = events.isEmpty();
            if (available) {
                availableRooms++;
            }
            TextView card = label(String.format(Locale.US,
                    "Phòng %02d · %s\n%s",
                    unit,
                    available ? "Trống" : "Có lịch",
                    available ? "Sẵn sàng nhận booking trong khoảng ngày này."
                            : events.size() + " lịch trùng khoảng ngày đang xem."));
            card.setBackgroundResource(unit == selectedUnit ? R.drawable.bg_button_secondary : R.drawable.bg_manager_search);
            card.setTextColor(getColor(unit == selectedUnit ? R.color.black : R.color.ink));
            card.setPadding(dp(14), dp(10), dp(14), dp(10));
            int clickedUnit = unit;
            card.setOnClickListener(view -> {
                selectedUnit = clickedUnit;
                renderRoomTabs();
                renderSelectedRoomCalendarV2();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(8);
            roomUnitsContainer.addView(card, params);
        }
        summaryTextView.setText(String.format(Locale.US,
                "%s · %s · %d phòng\nKhoảng %s đến %s: còn %d/%d phòng trống.",
                roomType.titleLabel(),
                roomType.categoryLabel(),
                totalRooms,
                currentStart,
                currentEnd,
                availableRooms,
                totalRooms));
    }

    private void renderSelectedRoomCalendarV2() {
        calendarTitleTextView.setText(String.format(Locale.US,
                "Lịch Phòng %02d · %s đến %s",
                selectedUnit,
                currentStart,
                currentEnd));
        blockTitleTextView.setText(String.format(Locale.US, "Chặn lịch Phòng %02d", selectedUnit));
        roomCalendarContainer.removeAllViews();
        LocalDate visualEnd = currentEnd;
        if (ChronoUnit.DAYS.between(currentStart, currentEnd) > 21) {
            visualEnd = currentStart.plusDays(21);
        }
        for (LocalDate day = currentStart; day.isBefore(visualEnd); day = day.plusDays(1)) {
            List<RoomUnitEvent> events = eventsForUnit(currentEvents, selectedUnit, day, day.plusDays(1));
            boolean free = events.isEmpty();
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(free ? R.drawable.bg_room_available : R.drawable.bg_room_booked);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));

            if (!free) {
                RoomUnitEvent event = events.get(0);
                TextView avatar = label(event.avatarLabel);
                avatar.setGravity(Gravity.CENTER);
                avatar.setTextColor(getColor(R.color.white));
                avatar.setTextSize(11f);
                avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                avatar.setBackgroundResource(R.drawable.bg_review_avatar);
                LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(28), dp(28));
                avatarParams.rightMargin = dp(10);
                row.addView(avatar, avatarParams);
            }

            String detail;
            if (free) {
                detail = "Trống";
            } else {
                RoomUnitEvent event = events.get(0);
                detail = event.title + " · " + event.detail;
            }
            TextView text = label(day + " · " + (free ? "Trống" : "Đã book") + "\n" + detail);
            text.setTextColor(getColor(R.color.black));
            row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(7);
            roomCalendarContainer.addView(row, params);
        }
        if (visualEnd.isBefore(currentEnd)) {
            TextView note = label("Chỉ hiển thị 21 ngày đầu để lịch không quá dài.");
            note.setTextColor(getColor(R.color.muted));
            roomCalendarContainer.addView(note);
        }
    }

    private void renderSelectedRoomCalendar() {
        calendarTitleTextView.setText(String.format(Locale.US,
                "Lịch Phòng %02d · %s đến %s",
                selectedUnit,
                currentStart,
                currentEnd));
        blockTitleTextView.setText(String.format(Locale.US, "Chặn lịch Phòng %02d", selectedUnit));
        roomCalendarContainer.removeAllViews();
        LocalDate visualEnd = currentEnd;
        if (ChronoUnit.DAYS.between(currentStart, currentEnd) > 21) {
            visualEnd = currentStart.plusDays(21);
        }
        for (LocalDate day = currentStart; day.isBefore(visualEnd); day = day.plusDays(1)) {
            List<RoomUnitEvent> events = eventsForUnit(currentEvents, selectedUnit, day, day.plusDays(1));
            boolean free = events.isEmpty();
            String detail;
            if (free) {
                detail = "Trống · có thể nhận booking hoặc khách walk-in.";
            } else {
                RoomUnitEvent event = events.get(0);
                detail = event.title + " · " + event.detail;
            }
            TextView row = label(day + " · " + (free ? "Trống" : "Bận") + "\n" + detail);
            row.setBackgroundResource(R.drawable.bg_manager_search);
            row.setTextColor(getColor(free ? R.color.primary : R.color.ink));
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(6);
            roomCalendarContainer.addView(row, params);
        }
        if (visualEnd.isBefore(currentEnd)) {
            TextView note = label("Chỉ hiển thị 21 ngày đầu để lịch không quá dài.");
            note.setTextColor(getColor(R.color.muted));
            roomCalendarContainer.addView(note);
        }
    }

    private void blockSelectedRoom() {
        if (roomType == null) {
            return;
        }
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(blockStartEditText.getText().toString().trim());
            end = LocalDate.parse(blockEndEditText.getText().toString().trim());
        } catch (Exception exception) {
            statusTextView.setText("Ngày chặn phải đúng định dạng YYYY-MM-DD.");
            return;
        }
        if (!end.isAfter(start)) {
            statusTextView.setText("Ngày kết thúc phải sau ngày bắt đầu.");
            return;
        }
        if (!eventsForUnit(currentEvents, selectedUnit, start, end).isEmpty()) {
            statusTextView.setText(String.format(Locale.US,
                    "Phòng %02d đã có lịch trong khoảng này.",
                    selectedUnit));
            return;
        }
        String reason = blockReasonEditText.getText().toString().trim();
        if (reason.isEmpty()) {
            reason = "External booking or manager block";
        }
        statusTextView.setText("Đang chặn lịch phòng...");
        blockedDateService.blockDates(
                cabinId,
                roomType.getId(),
                sessionManager.getUserId(),
                start.toString(),
                end.toString(),
                1,
                selectedUnit,
                reason,
                new SupabaseCallback<BlockedDate>() {
                    @Override
                    public void onSuccess(BlockedDate data) {
                        blockReasonEditText.setText("");
                        rangeStartEditText.setText(start.toString());
                        rangeEndEditText.setText(end.toString());
                        statusTextView.setText(String.format(Locale.US,
                                "Đã chặn lịch Phòng %02d.",
                                selectedUnit));
                        loadRoomSchedule();
                    }

                    @Override
                    public void onError(String message) {
                        statusTextView.setText("Không chặn được lịch: " + message);
                    }
                }
        );
    }

    private List<RoomUnitEvent> buildRoomUnitEvents(List<Booking> bookings, List<BlockedDate> blockedDates, int totalRooms) {
        List<RoomUnitEvent> events = new ArrayList<>();
        if (bookings != null) {
            for (Booking booking : bookings) {
                if (booking == null
                        || !sameId(roomType.getId(), booking.getRoomTypeId())
                        || AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(safe(booking.getStatus()))) {
                    continue;
                }
                LocalDate start = parseDateOrNull(booking.getStartDate());
                LocalDate end = parseDateOrNull(booking.getEndDate());
                if (start == null || end == null || !end.isAfter(start)) {
                    continue;
                }
                int rooms = Math.max(1, booking.getNumRooms());
                for (int i = 0; i < rooms; i++) {
                    int unit = nextAvailableUnit(events, totalRooms, start, end);
                    events.add(new RoomUnitEvent(unit, start, end,
                            "Booking " + shortId(booking.getId()),
                            bookingStatusLabel(booking.getStatus()) + " · " + booking.getNumGuests()
                                    + " khách · $" + booking.getTotalPrice(),
                            guestInitials(booking.getUserId())));
                }
            }
        }
        if (blockedDates != null) {
            for (BlockedDate blockedDate : blockedDates) {
                if (blockedDate == null) {
                    continue;
                }
                String blockedRoomTypeId = blockedDate.getRoomTypeId();
                if (blockedRoomTypeId != null && !blockedRoomTypeId.trim().isEmpty()
                        && !sameId(roomType.getId(), blockedRoomTypeId)) {
                    continue;
                }
                LocalDate start = parseDateOrNull(blockedDate.getStartDate());
                LocalDate end = parseDateOrNull(blockedDate.getEndDate());
                if (start == null || end == null || !end.isAfter(start)) {
                    continue;
                }
                String reason = safe(blockedDate.getReason()).isEmpty() ? "Manager block" : blockedDate.getReason();
                int fixedUnit = blockedDate.getRoomUnitNumber();
                if (fixedUnit > 0 && fixedUnit <= totalRooms) {
                    events.add(new RoomUnitEvent(fixedUnit, start, end,
                            "Chặn lịch " + shortId(blockedDate.getId()),
                            reason,
                            "QL"));
                    continue;
                }
                int rooms = Math.max(1, blockedDate.getNumRooms());
                for (int i = 0; i < rooms; i++) {
                    int unit = nextAvailableUnit(events, totalRooms, start, end);
                    events.add(new RoomUnitEvent(unit, start, end,
                            "Chặn lịch " + shortId(blockedDate.getId()),
                            reason,
                            "QL"));
                }
            }
        }
        return events;
    }

    private int nextAvailableUnit(List<RoomUnitEvent> events, int totalRooms, LocalDate start, LocalDate end) {
        for (int unit = 1; unit <= totalRooms; unit++) {
            if (eventsForUnit(events, unit, start, end).isEmpty()) {
                return unit;
            }
        }
        return 1;
    }

    private List<RoomUnitEvent> eventsForUnit(List<RoomUnitEvent> events, int unit, LocalDate start, LocalDate end) {
        List<RoomUnitEvent> result = new ArrayList<>();
        for (RoomUnitEvent event : events) {
            if (event.unitNumber == unit && rangesOverlap(start, end, event.startDate, event.endDate)) {
                result.add(event);
            }
        }
        return result;
    }

    private boolean rangesOverlap(LocalDate leftStart, LocalDate leftEnd, LocalDate rightStart, LocalDate rightEnd) {
        return leftStart.isBefore(rightEnd) && leftEnd.isAfter(rightStart);
    }

    private LocalDate parseDateOrNull(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean sameId(String left, String right) {
        return left != null && right != null && left.trim().equals(right.trim());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String shortId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return "";
        }
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private String bookingStatusLabel(String status) {
        if (AppConstants.BOOKING_PENDING.equals(status)) {
            return "Chờ manager duyệt";
        }
        if (AppConstants.BOOKING_CONFIRMED.equals(status)) {
            return "Đã xác nhận";
        }
        if (AppConstants.BOOKING_CHECKED_IN.equals(status)) {
            return "Đang lưu trú";
        }
        if (AppConstants.BOOKING_CHECKED_OUT.equals(status)) {
            return "Đã trả phòng";
        }
        return safe(status).isEmpty() ? "Đã book" : status;
    }

    private String guestInitials(String userId) {
        if (userId == null) {
            return "KH";
        }
        if (userId.endsWith("101")) {
            return "AN";
        }
        if (userId.endsWith("102")) {
            return "BT";
        }
        if (userId.endsWith("103")) {
            return "CP";
        }
        if (userId.endsWith("104")) {
            return "DL";
        }
        if (userId.endsWith("105")) {
            return "EH";
        }
        return "KH";
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14f);
        view.setTextColor(getColor(R.color.ink));
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class RoomUnitEvent {
        private final int unitNumber;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String title;
        private final String detail;
        private final String avatarLabel;

        private RoomUnitEvent(int unitNumber, LocalDate startDate, LocalDate endDate, String title, String detail, String avatarLabel) {
            this.unitNumber = unitNumber;
            this.startDate = startDate;
            this.endDate = endDate;
            this.title = title;
            this.detail = detail;
            this.avatarLabel = avatarLabel;
        }
    }
}
