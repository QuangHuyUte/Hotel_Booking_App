package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Amenity;
import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.CabinAmenity;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AmenityService;
import com.example.hotel_booking_app.services.HostService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.ui.adapters.BookingAdapter;
import com.example.hotel_booking_app.ui.adapters.HostCabinAdapter;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HostHotelDashboardActivity extends AppCompatActivity {
    public static final String EXTRA_EDIT_CABIN_ID = "extra_edit_cabin_id";
    private static final int REQUEST_PICK_IMAGE = 1001;

    private TextView statusTextView;
    private TextView dashboardTitleTextView;
    private TextView formTitleTextView;
    private TextView bookingTitleTextView;
    private TextView emptyBookingsTextView;
    private EditText cabinSearchEditText;
    private EditText nameEditText;
    private EditText locationEditText;
    private EditText priceEditText;
    private EditText discountEditText;
    private EditText capacityEditText;
    private EditText descriptionEditText;
    private EditText imageEditText;
    private LinearLayout cabinFormContainer;
    private LinearLayout amenitiesContainer;
    private Spinner cabinFilterSpinner;
    private Spinner bookingFilterSpinner;
    private HostCabinAdapter cabinAdapter;
    private BookingAdapter bookingAdapter;
    private HostService hostService;
    private AmenityService amenityService;
    private RoomTypeService roomTypeService;
    private SessionManager sessionManager;
    private Cabin editingCabin;
    private String selectedCabinId;
    private String pendingEditCabinId;
    private boolean pendingEditOpened;
    private boolean amenitiesLoaded;
    private final List<Cabin> allManagedCabins = new ArrayList<>();
    private List<Booking> selectedCabinBookings = new ArrayList<>();
    private final List<Amenity> allAmenities = new ArrayList<>();
    private final List<String> selectedAmenityIds = new ArrayList<>();
    private final List<String> selectedAmenityNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_hotel_dashboard);

        hostService = new HostService();
        amenityService = new AmenityService();
        roomTypeService = new RoomTypeService();
        sessionManager = new SessionManager(this);
        pendingEditCabinId = getIntent().getStringExtra(EXTRA_EDIT_CABIN_ID);
        bindViews();
        setupLists();
        setupActions();
        loadAmenities();
        loadCabins();
    }

    private void bindViews() {
        statusTextView = findViewById(R.id.text_status);
        dashboardTitleTextView = findViewById(R.id.text_dashboard_title);
        formTitleTextView = findViewById(R.id.text_form_title);
        bookingTitleTextView = findViewById(R.id.text_booking_title);
        emptyBookingsTextView = findViewById(R.id.text_empty_bookings);
        cabinSearchEditText = findViewById(R.id.edit_cabin_search);
        nameEditText = findViewById(R.id.edit_cabin_name);
        locationEditText = findViewById(R.id.edit_location);
        priceEditText = findViewById(R.id.edit_price);
        discountEditText = findViewById(R.id.edit_discount);
        capacityEditText = findViewById(R.id.edit_capacity);
        descriptionEditText = findViewById(R.id.edit_description);
        imageEditText = findViewById(R.id.edit_image);
        cabinFormContainer = findViewById(R.id.container_cabin_form);
        amenitiesContainer = findViewById(R.id.container_amenities);
        cabinFilterSpinner = findViewById(R.id.spinner_cabin_filter);
        bookingFilterSpinner = findViewById(R.id.spinner_booking_filter);
        dashboardTitleTextView.setText("Xin chào, " + displayName());
    }

    private void setupLists() {
        RecyclerView cabinsRecyclerView = findViewById(R.id.recycler_host_cabins);
        RecyclerView bookingsRecyclerView = findViewById(R.id.recycler_host_bookings);
        cabinAdapter = new HostCabinAdapter(new HostCabinAdapter.HostCabinListener() {
            @Override
            public void onSelect(Cabin cabin) {
                Intent intent = new Intent(HostHotelDashboardActivity.this, HotelDetailActivity.class);
                intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabin.getId());
                startActivity(intent);
            }

            @Override
            public void onEdit(Cabin cabin) {
                Intent intent = new Intent(HostHotelDashboardActivity.this, AdminHotelFormActivity.class);
                intent.putExtra(AdminHotelFormActivity.EXTRA_CABIN_ID, cabin.getId());
                startActivity(intent);
            }

            @Override
            public void onDuplicate(Cabin cabin) {
                Cabin copy = copyCabin(cabin);
                copy.setName(cabin.getName() + " Copy");
                createCabin(copy);
            }

            @Override
            public void onDelete(Cabin cabin) {
                deleteCabin(cabin);
            }
        });
        bookingAdapter = new BookingAdapter("Confirm", this::confirmBooking);
        cabinsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cabinsRecyclerView.setAdapter(cabinAdapter);
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookingsRecyclerView.setAdapter(bookingAdapter);

        String[] cabinFilters = {
                "Tất cả khách sạn",
                "Đang giảm giá",
                "Không giảm giá",
                "TP. Hồ Chí Minh",
                "Vũng Tàu",
                "Hà Nội",
                "Đà Nẵng",
                "Đà Lạt",
                "Có phòng Suite",
                "Chưa có loại phòng",
                "Sức chứa: cao đến thấp",
                "Giá: thấp đến cao",
                "Giá: cao đến thấp",
                "Mới nhất trước"
        };
        ArrayAdapter<String> cabinFilterAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_on_primary, cabinFilters);
        cabinFilterAdapter.setDropDownViewResource(R.layout.item_spinner_serein_dropdown);
        cabinFilterSpinner.setAdapter(cabinFilterAdapter);

        String[] filters = {"Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đã hủy", "Đã thanh toán"};
        ArrayAdapter<String> bookingFilterAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_on_primary, filters);
        bookingFilterAdapter.setDropDownViewResource(R.layout.item_spinner_serein_dropdown);
        bookingFilterSpinner.setAdapter(bookingFilterAdapter);
    }

    private void setupActions() {
        Button backButton = findViewById(R.id.button_back);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);
        Button newCabinButton = findViewById(R.id.button_new_cabin);
        Button pickImageButton = findViewById(R.id.button_pick_image);
        Button saveCabinButton = findViewById(R.id.button_create_cabin);
        Button clearFormButton = findViewById(R.id.button_clear_form);
        LinearLayout personalTab = findViewById(R.id.nav_personal);
        LinearLayout searchTab = findViewById(R.id.nav_cabins);
        LinearLayout bookingsTab = findViewById(R.id.nav_bookings);
        LinearLayout wishlistTab = findViewById(R.id.nav_wishlist);
        LinearLayout messagesTab = findViewById(R.id.nav_messages);
        backButton.setOnClickListener(view -> finish());
        bottomBackButton.setOnClickListener(view -> finish());
        searchTab.setOnClickListener(view -> startActivity(new Intent(this, HostHotelDashboardActivity.class)));
        bookingsTab.setOnClickListener(view -> startActivity(new Intent(this, AdminBookingManagementActivity.class)));
        wishlistTab.setOnClickListener(view -> startActivity(new Intent(this, HostHotelDashboardActivity.class)));
        messagesTab.setOnClickListener(view -> startActivity(new Intent(this, ConversationListActivity.class)));
        personalTab.setOnClickListener(view -> startActivity(new Intent(this, AccountHubActivity.class)));
        newCabinButton.setOnClickListener(view -> startActivity(new Intent(this, AdminHotelFormActivity.class)));
        pickImageButton.setOnClickListener(view -> openImagePicker());
        saveCabinButton.setOnClickListener(view -> saveCabin());
        clearFormButton.setOnClickListener(view -> clearForm());
        cabinSearchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCabins();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
        cabinFilterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                renderCabins();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            imageEditText.setText(imageUri.toString());
            statusTextView.setText("Image selected.");
        }
    }

    private void loadAmenities() {
        amenityService.getAmenities(new SupabaseCallback<List<Amenity>>() {
            @Override
            public void onSuccess(List<Amenity> amenities) {
                allAmenities.clear();
                allAmenities.addAll(amenities);
                amenitiesLoaded = true;
                renderAmenityCheckboxes();
                openPendingEditIfReady();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Không tải được tiện nghi: " + message);
            }
        });
    }

    private void renderAmenityCheckboxes() {
        amenitiesContainer.removeAllViews();
        for (Amenity amenity : allAmenities) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(amenity.getName());
            checkBox.setTextColor(getColor(R.color.ink));
            checkBox.setChecked(selectedAmenityIds.contains(amenity.getId()));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedAmenityIds.contains(amenity.getId())) {
                        selectedAmenityIds.add(amenity.getId());
                        selectedAmenityNames.add(amenity.getName());
                    }
                } else {
                    selectedAmenityIds.remove(amenity.getId());
                    selectedAmenityNames.remove(amenity.getName());
                }
            });
            amenitiesContainer.addView(checkBox);
        }
    }

    private void loadCabins() {
        statusTextView.setText("Đang tải khách sạn bạn quản lý...");
        SupabaseCallback<List<Cabin>> callback = new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                roomTypeService.attachRoomTypes(cabins, new SupabaseCallback<List<Cabin>>() {
                    @Override
                    public void onSuccess(List<Cabin> cabinsWithRooms) {
                        allManagedCabins.clear();
                        allManagedCabins.addAll(cabinsWithRooms);
                        assignCheapestRoomTypes(allManagedCabins);
                        renderCabins();
                        openPendingEditIfReady();
                    }

                    @Override
                    public void onError(String message) {
                        allManagedCabins.clear();
                        allManagedCabins.addAll(cabins);
                        renderCabins();
                        openPendingEditIfReady();
                        statusTextView.setText("Hotels loaded, but room types are not ready: " + message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        };
        hostService.getCabinsForHost(sessionManager.getUserId(), callback);
    }

    private String displayName() {
        String name = sessionManager.getFullName();
        return name == null || name.trim().isEmpty() ? "Quản lý" : name.trim();
    }

    private void renderCabins() {
        String query = cabinSearchEditText.getText().toString().trim().toLowerCase(java.util.Locale.US);
        String filter = cabinFilterSpinner.getSelectedItem() == null
                ? "Tất cả khách sạn"
                : String.valueOf(cabinFilterSpinner.getSelectedItem());
        List<Cabin> filtered = allManagedCabins.stream()
                .filter(cabin -> matchesCabinSearch(cabin, query))
                .filter(cabin -> matchesCabinFilter(cabin, filter))
                .collect(Collectors.toList());
        if ("Sức chứa: cao đến thấp".equals(filter)) {
            filtered.sort((left, right) -> Integer.compare(right.getMaxCapacity(), left.getMaxCapacity()));
        } else if ("Giá: thấp đến cao".equals(filter)) {
            filtered.sort((left, right) -> Double.compare(priceAfterDiscount(left), priceAfterDiscount(right)));
        } else if ("Giá: cao đến thấp".equals(filter)) {
            filtered.sort((left, right) -> Double.compare(priceAfterDiscount(right), priceAfterDiscount(left)));
        }
        cabinAdapter.submitList(filtered);
        statusTextView.setText("Đang hiển thị " + filtered.size() + "/" + allManagedCabins.size()
                + " khách sạn. Bấm Sửa để quản lý các loại phòng Tiêu chuẩn, Superior, Deluxe và Suite.");
    }

    private void assignCheapestRoomTypes(List<Cabin> cabins) {
        for (Cabin cabin : cabins) {
            if (cabin.getRoomTypes() == null || cabin.getRoomTypes().isEmpty()) {
                cabin.setMatchedRoomType(null);
                continue;
            }
            cabin.setMatchedRoomType(cabin.getRoomTypes().stream()
                    .min((left, right) -> Double.compare(left.getBasePrice(), right.getBasePrice()))
                    .orElse(null));
        }
    }

    private void openPendingEditIfReady() {
        if (pendingEditOpened || pendingEditCabinId == null || !amenitiesLoaded || allManagedCabins.isEmpty()) {
            return;
        }
        for (Cabin cabin : allManagedCabins) {
            if (pendingEditCabinId.equals(cabin.getId())) {
                pendingEditOpened = true;
                fillForm(cabin);
                statusTextView.setText("Editing " + cabin.getName());
                return;
            }
        }
    }

    private void saveCabin() {
        Cabin cabin = editingCabin == null ? new Cabin() : editingCabin;
        try {
            cabin.setName(nameEditText.getText().toString().trim());
            cabin.setLocation(locationEditText.getText().toString().trim());
            cabin.setRegularPrice(Double.parseDouble(priceEditText.getText().toString().trim()));
            cabin.setDiscount(parseDoubleOrZero(discountEditText.getText().toString().trim()));
            cabin.setMaxCapacity(Integer.parseInt(capacityEditText.getText().toString().trim()));
            cabin.setDescription(descriptionEditText.getText().toString().trim());
            cabin.setImage(imageEditText.getText().toString().trim());
            cabin.setAmenities(selectedAmenityNames.isEmpty() ? "WiFi, Kitchen, Private Bathroom" : String.join(", ", selectedAmenityNames));
            cabin.setHostId(sessionManager.getUserId());
        } catch (NumberFormatException e) {
            statusTextView.setText("Price, discount and capacity must be valid numbers.");
            return;
        }

        if (editingCabin == null) {
            createCabin(cabin);
        } else {
            updateCabin(cabin);
        }
    }

    private void createCabin(Cabin cabin) {
        statusTextView.setText("Creating hotel...");
        hostService.createCabin(cabin, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin data) {
                cabinAdapter.upsert(data);
                attachSelectedAmenities(data.getId());
                Toast.makeText(HostHotelDashboardActivity.this, "Đã tạo khách sạn", Toast.LENGTH_SHORT).show();
                clearForm();
                loadCabins();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void updateCabin(Cabin cabin) {
        statusTextView.setText("Updating hotel...");
        hostService.updateCabin(cabin, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin data) {
                cabinAdapter.upsert(cabin);
                Toast.makeText(HostHotelDashboardActivity.this, "Đã cập nhật khách sạn", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Đã cập nhật khách sạn. Danh sách đã được làm mới.");
                clearForm();
                loadCabins();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void deleteCabin(Cabin cabin) {
        statusTextView.setText("Deleting hotel...");
        hostService.deleteCabin(cabin.getId(), new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                cabinAdapter.removeById(cabin.getId());
                if (cabin.getId().equals(selectedCabinId)) {
                    selectedCabinId = null;
                    selectedCabinBookings = new ArrayList<>();
                    bookingTitleTextView.setText("Quản lý đặt phòng");
                    renderBookings();
                }
                Toast.makeText(HostHotelDashboardActivity.this, "Hotel deleted", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Hotel deleted.");
                loadCabins();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void fillForm(Cabin cabin) {
        editingCabin = cabin;
        cabinFormContainer.setVisibility(View.VISIBLE);
        formTitleTextView.setText("Sửa khách sạn");
        nameEditText.setText(cabin.getName());
        locationEditText.setText(cabin.getLocation());
        priceEditText.setText(String.valueOf(cabin.getRegularPrice()));
        discountEditText.setText(String.valueOf(cabin.getDiscount()));
        capacityEditText.setText(String.valueOf(cabin.getMaxCapacity()));
        descriptionEditText.setText(cabin.getDescription());
        imageEditText.setText(cabin.getImage());
        selectedAmenityNames.clear();
        selectedAmenityIds.clear();
        if (cabin.getAmenities() != null) {
            for (Amenity amenity : allAmenities) {
                if (cabin.getAmenities().contains(amenity.getName())) {
                    selectedAmenityIds.add(amenity.getId());
                    selectedAmenityNames.add(amenity.getName());
                }
            }
        }
        renderAmenityCheckboxes();
    }

    private Cabin copyCabin(Cabin source) {
        Cabin cabin = new Cabin();
        cabin.setName(source.getName());
        cabin.setLocation(source.getLocation());
        cabin.setRegularPrice(source.getRegularPrice());
        cabin.setDiscount(source.getDiscount());
        cabin.setMaxCapacity(source.getMaxCapacity());
        cabin.setDescription(source.getDescription());
        cabin.setImage(source.getImage());
        cabin.setAmenities(source.getAmenities());
        cabin.setHostId(sessionManager.getUserId());
        return cabin;
    }

    private void clearForm() {
        editingCabin = null;
        formTitleTextView.setText("Tạo khách sạn mới");
        nameEditText.setText("");
        locationEditText.setText("");
        priceEditText.setText("");
        discountEditText.setText("");
        capacityEditText.setText("");
        descriptionEditText.setText("");
        imageEditText.setText("");
        selectedAmenityIds.clear();
        selectedAmenityNames.clear();
        renderAmenityCheckboxes();
        cabinFormContainer.setVisibility(View.GONE);
    }

    private void attachSelectedAmenities(String cabinId) {
        for (String amenityId : selectedAmenityIds) {
            amenityService.addAmenityToCabin(cabinId, amenityId, new SupabaseCallback<CabinAmenity>() {
                @Override
                public void onSuccess(CabinAmenity data) {
                }

                @Override
                public void onError(String message) {
                    statusTextView.setText("Hotel saved, but one amenity could not be attached: " + message);
                }
            });
        }
    }

    private void loadBookingsForCabin(Cabin cabin) {
        selectedCabinId = cabin.getId();
        bookingTitleTextView.setText("Bookings for " + cabin.getName());
        hostService.getBookingsForCabin(cabin.getId(), new SupabaseCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                selectedCabinBookings = bookings;
                renderBookings();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void renderBookings() {
        if (selectedCabinId == null) {
            bookingAdapter.submitList(new ArrayList<>());
            emptyBookingsTextView.setVisibility(View.VISIBLE);
            emptyBookingsTextView.setText("Chọn một khách sạn phía trên để xem đặt phòng.");
            return;
        }
        String filter = String.valueOf(bookingFilterSpinner.getSelectedItem());
        List<Booking> filtered = selectedCabinBookings.stream()
                .filter(booking -> "Tất cả".equals(filter)
                        || ("Đã thanh toán".equals(filter) && booking.isPaid())
                        || matchesBookingStatusFilter(booking, filter))
                .collect(Collectors.toList());
        bookingAdapter.submitList(filtered);
        emptyBookingsTextView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        emptyBookingsTextView.setText("Chưa có đặt phòng phù hợp với bộ lọc này.");
        statusTextView.setText("Đang hiển thị " + filtered.size() + " đặt phòng.");
    }

    private void confirmBooking(Booking bookingToConfirm) {
        hostService.updateBookingStatus(bookingToConfirm.getId(), AppConstants.BOOKING_CONFIRMED, bookingToConfirm.isPaid(), new SupabaseCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                Toast.makeText(HostHotelDashboardActivity.this, "Đã xác nhận đặt phòng", Toast.LENGTH_SHORT).show();
                if (selectedCabinId != null) {
                    hostService.getBookingsForCabin(selectedCabinId, new SupabaseCallback<List<Booking>>() {
                        @Override
                        public void onSuccess(List<Booking> bookings) {
                            selectedCabinBookings = bookings;
                            renderBookings();
                        }

                        @Override
                        public void onError(String message) {
                            statusTextView.setText(message);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private double parseDoubleOrZero(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        return Double.parseDouble(value);
    }

    private boolean matchesCabinSearch(Cabin cabin, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return safe(cabin.getName()).toLowerCase(java.util.Locale.US).contains(query)
                || safe(cabin.getLocation()).toLowerCase(java.util.Locale.US).contains(query)
                || safe(cabin.getAmenities()).toLowerCase(java.util.Locale.US).contains(query)
                || matchesRoomTypeSearch(cabin, query);
    }

    private boolean matchesCabinFilter(Cabin cabin, String filter) {
        if ("Đang giảm giá".equals(filter)) {
            return cabin.getDiscount() > 0;
        }
        if ("Không giảm giá".equals(filter)) {
            return cabin.getDiscount() <= 0;
        }
        if ("TP. Hồ Chí Minh".equals(filter)) {
            return containsCabinText(cabin, "ho chi minh");
        }
        if ("Vũng Tàu".equals(filter)) {
            return containsCabinText(cabin, "vung tau");
        }
        if ("Hà Nội".equals(filter)) {
            return containsCabinText(cabin, "hanoi") || containsCabinText(cabin, "ha noi");
        }
        if ("Đà Nẵng".equals(filter)) {
            return containsCabinText(cabin, "da nang");
        }
        if ("Đà Lạt".equals(filter)) {
            return containsCabinText(cabin, "da lat");
        }
        if ("Có phòng Suite".equals(filter)) {
            return matchesRoomTypeSearch(cabin, "suite");
        }
        if ("Chưa có loại phòng".equals(filter)) {
            return cabin.getRoomTypes() == null || cabin.getRoomTypes().isEmpty();
        }
        return true;
    }

    private boolean matchesBookingStatusFilter(Booking booking, String filter) {
        if ("Chờ xác nhận".equals(filter)) {
            return AppConstants.BOOKING_PENDING.equalsIgnoreCase(booking.getStatus());
        }
        if ("Đã xác nhận".equals(filter)) {
            return AppConstants.BOOKING_CONFIRMED.equalsIgnoreCase(booking.getStatus());
        }
        if ("Đã hủy".equals(filter)) {
            return AppConstants.BOOKING_CANCELLED.equalsIgnoreCase(booking.getStatus());
        }
        return booking.getStatus().equalsIgnoreCase(filter);
    }

    private boolean matchesRoomTypeSearch(Cabin cabin, String query) {
        if (cabin.getRoomTypes() == null || cabin.getRoomTypes().isEmpty()) {
            return false;
        }
        for (RoomType roomType : cabin.getRoomTypes()) {
            String text = (safe(roomType.getName()) + " "
                    + safe(roomType.getCategory()) + " "
                    + safe(roomType.getBeds()) + " "
                    + safe(roomType.getSize())).toLowerCase(java.util.Locale.US);
            if (text.contains(query)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCabinText(Cabin cabin, String query) {
        String text = (safe(cabin.getName()) + " "
                + safe(cabin.getLocation()) + " "
                + safe(cabin.getAmenities())).toLowerCase(java.util.Locale.US);
        return text.contains(query);
    }

    private double priceAfterDiscount(Cabin cabin) {
        return Math.max(0, cabin.getRegularPrice() - cabin.getDiscount());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
