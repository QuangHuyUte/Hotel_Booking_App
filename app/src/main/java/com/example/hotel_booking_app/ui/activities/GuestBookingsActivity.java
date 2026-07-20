package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.ui.adapters.BookingAdapter;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GuestBookingsActivity extends AppCompatActivity {
    private static final String FILTER_ALL = "Tất cả";
    private static final String FILTER_UPCOMING = "Sắp tới";
    private static final String FILTER_CURRENT = "Đang lưu trú";
    private static final String FILTER_PAST = "Đã qua";
    private static final String FILTER_PENDING = "Chờ xác nhận";
    private static final String FILTER_CONFIRMED = "Đã xác nhận";
    private static final String FILTER_CANCELLED = "Đã hủy";

    private TextView statusTextView;
    private EditText searchEditText;
    private Spinner filterSpinner;
    private BookingAdapter adapter;
    private BookingService bookingService;
    private CabinService cabinService;
    private SessionManager sessionManager;
    private final List<Booking> allBookings = new ArrayList<>();
    private final Map<String, Cabin> cabinById = new HashMap<>();
    private final Set<String> loadingCabinIds = new HashSet<>();
    private String selectedFilter = FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_bookings);

        statusTextView = findViewById(R.id.text_status);
        searchEditText = findViewById(R.id.edit_search_bookings);
        filterSpinner = findViewById(R.id.spinner_filter);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        LinearLayout searchTab = findViewById(R.id.nav_cabins);
        LinearLayout wishlistTab = findViewById(R.id.nav_wishlist);
        LinearLayout messagesTab = findViewById(R.id.nav_messages);
        LinearLayout profileTab = findViewById(R.id.nav_personal);
        RecyclerView recyclerView = findViewById(R.id.recycler_bookings);
        bookingService = new BookingService();
        cabinService = new CabinService();
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        adapter = new BookingAdapter("", this::openBookingDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        searchTab.setOnClickListener(view -> startActivity(new Intent(this, HotelSearchActivity.class)));
        wishlistTab.setOnClickListener(view -> startActivity(new Intent(this, SavedHotelsActivity.class)));
        messagesTab.setOnClickListener(view -> startActivity(new Intent(this, ConversationListActivity.class)));
        profileTab.setOnClickListener(view -> startActivity(new Intent(this, AccountHubActivity.class)));
        setupSearch();
        setupFilter();
        loadBookings();
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderBookings();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilter() {
        String[] filters = {
                FILTER_ALL,
                FILTER_UPCOMING,
                FILTER_CURRENT,
                FILTER_PAST,
                FILTER_PENDING,
                FILTER_CONFIRMED,
                FILTER_CANCELLED
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_on_primary,
                filters
        );
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_serein_dropdown);
        filterSpinner.setAdapter(spinnerAdapter);
        filterSpinner.setSelection(0);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilter = filters[position];
                renderBookings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadBookings() {
        statusTextView.setText("Đang tải chuyến đi của bạn...");
        bookingService.getBookingsForUser(sessionManager.getUserId(), new SupabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                allBookings.clear();
                if (bookings != null) {
                    allBookings.addAll(bookings);
                }
                loadCabinMetadata();
                renderBookings();
            }

            @Override
            public void onError(String message) {
                allBookings.clear();
                adapter.submitList(allBookings);
                statusTextView.setText(message);
            }
        });
    }

    private void loadCabinMetadata() {
        for (Booking booking : allBookings) {
            String cabinId = booking.getCabinId();
            if (cabinId == null || cabinById.containsKey(cabinId) || loadingCabinIds.contains(cabinId)) {
                continue;
            }
            loadingCabinIds.add(cabinId);
            cabinService.getCabinById(cabinId, new SupabaseCallback<Cabin>() {
                @Override
                public void onSuccess(Cabin cabin) {
                    loadingCabinIds.remove(cabinId);
                    cabinById.put(cabinId, cabin);
                    renderBookings();
                }

                @Override
                public void onError(String message) {
                    loadingCabinIds.remove(cabinId);
                }
            });
        }
    }

    private void renderBookings() {
        List<Booking> filtered = new ArrayList<>();
        for (Booking booking : allBookings) {
            if (matchesFilter(booking) && matchesSearch(booking)) {
                filtered.add(booking);
            }
        }
        filtered.sort((left, right) -> Integer.compare(statusPriority(left), statusPriority(right)));
        adapter.submitList(filtered);
        if (filtered.isEmpty()) {
            statusTextView.setText("Không có đặt phòng phù hợp.");
        } else {
            int pendingCount = pendingCount(filtered);
            String pendingText = pendingCount > 0
                    ? " " + pendingCount + " pending booking" + (pendingCount == 1 ? "" : "s") + " need attention."
                    : "";
            statusTextView.setText("Đang hiển thị " + filtered.size() + " đặt phòng." + pendingText);
        }
    }

    private int pendingCount(List<Booking> bookings) {
        int count = 0;
        for (Booking booking : bookings) {
            if (AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int statusPriority(Booking booking) {
        if (AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus())) {
            return 0;
        }
        if (AppConstants.BOOKING_CONFIRMED.equalsIgnoreCase(booking.getStatus()) && !booking.isPaid()) {
            return 1;
        }
        if (!AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus()) && isUpcoming(booking)) {
            return 2;
        }
        if (AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus())) {
            return 4;
        }
        return 3;
    }

    private boolean matchesFilter(Booking booking) {
        switch (selectedFilter) {
            case FILTER_UPCOMING:
                return !AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus()) && isUpcoming(booking);
            case FILTER_CURRENT:
                return !AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus()) && isCurrent(booking);
            case FILTER_PAST:
                return isPast(booking);
            case FILTER_PENDING:
                return AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus());
            case FILTER_CONFIRMED:
                return AppConstants.BOOKING_CONFIRMED.equalsIgnoreCase(booking.getStatus());
            case FILTER_CANCELLED:
                return AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus());
            case FILTER_ALL:
            default:
                return true;
        }
    }

    private boolean matchesSearch(Booking booking) {
        String query = searchEditText.getText().toString().trim().toLowerCase(Locale.US);
        if (query.isEmpty()) {
            return true;
        }
        Cabin cabin = cabinById.get(booking.getCabinId());
        String cabinName = cabin == null ? "" : cabin.getName();
        String haystack = (
                booking.getId() + " "
                        + booking.getCabinId() + " "
                        + cabinName + " "
                        + booking.getStartDate() + " "
                        + booking.getEndDate() + " "
                        + booking.getStatus()
        ).toLowerCase(Locale.US);
        return haystack.contains(query);
    }

    private boolean isUpcoming(Booking booking) {
        try {
            return LocalDate.parse(booking.getStartDate()).isAfter(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCurrent(Booking booking) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate start = LocalDate.parse(booking.getStartDate());
            LocalDate end = LocalDate.parse(booking.getEndDate());
            return !today.isBefore(start) && today.isBefore(end);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPast(Booking booking) {
        try {
            return LocalDate.parse(booking.getEndDate()).isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void openBookingDetail(Booking booking) {
        Intent intent = new Intent(this, BookingDetailsActivity.class);
        intent.putExtra(AppConstants.EXTRA_BOOKING_ID, booking.getId());
        startActivity(intent);
    }
}
