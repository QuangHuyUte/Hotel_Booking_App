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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AuthService;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.ui.adapters.AdminBookingAdapter;
import com.example.hotel_booking_app.utils.AppConstants;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminBookingsActivity extends AppCompatActivity {
    private static final String FILTER_ALL = "All bookings";
    private static final String FILTER_PENDING = "Pending confirmation";
    private static final String FILTER_CONFIRMED = "Confirmed stays";
    private static final String FILTER_PAID = "Paid";
    private static final String FILTER_UNPAID = "Unpaid / pay later";
    private static final String FILTER_UPCOMING = "Upcoming stays";
    private static final String FILTER_CURRENT = "Current stays";
    private static final String FILTER_PAST = "Past stays";
    private static final String FILTER_CANCELLED = "Cancelled stays";

    private TextView statusTextView;
    private EditText searchEditText;
    private Spinner filterSpinner;
    private AdminBookingAdapter adapter;
    private BookingService bookingService;
    private CabinService cabinService;
    private AuthService authService;
    private final List<Booking> allBookings = new ArrayList<>();
    private final Map<String, Cabin> cabinById = new HashMap<>();
    private final Map<String, User> userById = new HashMap<>();
    private final Set<String> loadingCabinIds = new HashSet<>();
    private final Set<String> loadingUserIds = new HashSet<>();
    private String selectedFilter = FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_bookings);

        statusTextView = findViewById(R.id.text_status);
        searchEditText = findViewById(R.id.edit_search_bookings);
        filterSpinner = findViewById(R.id.spinner_filter);
        Button backButton = findViewById(R.id.button_back);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);
        RecyclerView recyclerView = findViewById(R.id.recycler_bookings);

        bookingService = new BookingService();
        cabinService = new CabinService();
        authService = new AuthService();
        adapter = new AdminBookingAdapter(cabinById, userById, new AdminBookingAdapter.AdminBookingListener() {
            @Override
            public void onOpen(Booking booking) {
                openBookingDetail(booking);
            }

            @Override
            public void onPrimaryAction(Booking booking) {
                handlePrimaryAction(booking);
            }

            @Override
            public void onCancel(Booking booking) {
                cancelBooking(booking);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        backButton.setOnClickListener(view -> finish());
        bottomBackButton.setOnClickListener(view -> finish());
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
                FILTER_PENDING,
                FILTER_CONFIRMED,
                FILTER_PAID,
                FILTER_UNPAID,
                FILTER_UPCOMING,
                FILTER_CURRENT,
                FILTER_PAST,
                FILTER_CANCELLED
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_on_primary, filters);
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
        statusTextView.setText("Loading all bookings...");
        bookingService.getAllBookings(new SupabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                allBookings.clear();
                if (bookings != null) {
                    allBookings.addAll(bookings);
                }
                loadCabinMetadata();
                loadUserMetadata();
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

    private void loadUserMetadata() {
        for (Booking booking : allBookings) {
            String userId = booking.getUserId();
            if (userId == null || userById.containsKey(userId) || loadingUserIds.contains(userId)) {
                continue;
            }
            loadingUserIds.add(userId);
            authService.getUserById(userId, new SupabaseCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    loadingUserIds.remove(userId);
                    userById.put(userId, user);
                    renderBookings();
                }

                @Override
                public void onError(String message) {
                    loadingUserIds.remove(userId);
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
        adapter.submitList(filtered);
        statusTextView.setText(filtered.size() + " of " + allBookings.size() + " booking(s) shown.");
    }

    private boolean matchesFilter(Booking booking) {
        switch (selectedFilter) {
            case FILTER_PENDING:
                return AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus());
            case FILTER_CONFIRMED:
                return AppConstants.BOOKING_CONFIRMED.equalsIgnoreCase(booking.getStatus());
            case FILTER_PAID:
                return booking.isPaid();
            case FILTER_UNPAID:
                return !booking.isPaid() && !AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus());
            case FILTER_UPCOMING:
                return !AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus()) && isUpcoming(booking);
            case FILTER_CURRENT:
                return !AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus()) && isCurrent(booking);
            case FILTER_PAST:
                return isPast(booking);
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
        User user = userById.get(booking.getUserId());
        String cabinName = cabin == null ? "" : cabin.getName();
        String location = cabin == null ? "" : cabin.getLocation();
        String customerName = user == null ? "" : user.getFullName();
        String customerPhone = user == null ? "" : user.getPhone();
        String customerEmail = user == null ? "" : user.getEmail();
        String payment = booking.isPaid() ? "paid" : "unpaid pay later";
        String haystack = (
                booking.getId() + " "
                        + booking.getUserId() + " "
                        + customerName + " "
                        + customerPhone + " "
                        + customerEmail + " "
                        + cabinName + " "
                        + location + " "
                        + booking.getStartDate() + " "
                        + booking.getEndDate() + " "
                        + booking.getStatus() + " "
                        + payment
        ).toLowerCase(Locale.US);
        return haystack.contains(query);
    }

    private void handlePrimaryAction(Booking booking) {
        if (AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus())) {
            updateBooking(booking, AppConstants.BOOKING_CONFIRMED, booking.isPaid(), "Booking confirmed");
            return;
        }
        if (!booking.isPaid() && !AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus())) {
            updateBooking(booking, AppConstants.BOOKING_CONFIRMED, true, "Booking marked as paid");
            return;
        }
        openBookingDetail(booking);
    }

    private void cancelBooking(Booking booking) {
        updateBooking(booking, AppConstants.BOOKING_CANCELLED, false, "Booking cancelled");
    }

    private void updateBooking(Booking booking, String status, boolean isPaid, String toast) {
        statusTextView.setText("Updating booking...");
        bookingService.updateStatus(booking.getId(), status, isPaid, new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking data) {
                Toast.makeText(AdminBookingsActivity.this, toast, Toast.LENGTH_SHORT).show();
                loadBookings();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
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
        Intent intent = new Intent(this, BookingDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_BOOKING_ID, booking.getId());
        startActivity(intent);
    }
}
