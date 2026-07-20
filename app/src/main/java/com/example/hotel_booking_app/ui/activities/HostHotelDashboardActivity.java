package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.hotel_booking_app.ui.helpers.ManagerNavigationHelper;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HostHotelDashboardActivity extends AppCompatActivity {
    public static final String EXTRA_EDIT_CABIN_ID = "extra_edit_cabin_id";
    private static final int REQUEST_PICK_IMAGE = 1001;

    private TextView statusTextView;
    private TextView dashboardTitleTextView;
    private TextView formTitleTextView;
    private TextView bookingTitleTextView;
    private TextView emptyBookingsTextView;
    private TextView hotelMetricTextView;
    private TextView roomMetricTextView;
    private TextView priceMetricTextView;
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
    private LinearLayout hotelTabsContainer;
    private LinearLayout selectedRoomsPanel;
    private LinearLayout selectedRoomsContainer;
    private TextView selectedRoomsTitleTextView;
    private TextView selectedRoomsHintTextView;
    private Button manageSelectedHotelButton;
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
    private String selectedManagerCabinId;
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
        hotelMetricTextView = findViewById(R.id.text_metric_hotels);
        roomMetricTextView = findViewById(R.id.text_metric_rooms);
        priceMetricTextView = findViewById(R.id.text_metric_price);
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
        hotelTabsContainer = findViewById(R.id.container_hotel_tabs);
        selectedRoomsPanel = findViewById(R.id.panel_selected_rooms);
        selectedRoomsContainer = findViewById(R.id.container_selected_rooms);
        selectedRoomsTitleTextView = findViewById(R.id.text_selected_rooms_title);
        selectedRoomsHintTextView = findViewById(R.id.text_selected_rooms_hint);
        manageSelectedHotelButton = findViewById(R.id.button_manage_selected_hotel);
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
                Intent intent = new Intent(HostHotelDashboardActivity.this, AdminHotelFormActivity.class);
                intent.putExtra(AdminHotelFormActivity.EXTRA_CABIN_ID, cabin.getId());
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
        bookingAdapter = new BookingAdapter("Xác nhận", this::confirmBooking);
        cabinsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cabinsRecyclerView.setAdapter(cabinAdapter);
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookingsRecyclerView.setAdapter(bookingAdapter);

        String[] filters = {"Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đã hủy", "Đã thanh toán"};
        ArrayAdapter<String> bookingFilterAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_on_primary, filters);
        bookingFilterAdapter.setDropDownViewResource(R.layout.item_spinner_serein_dropdown);
        bookingFilterSpinner.setAdapter(bookingFilterAdapter);
    }

    private void setupActions() {
        Button backButton = findViewById(R.id.button_back);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);
        Button newCabinButton = findViewById(R.id.button_new_cabin);
        Button messagesButton = findViewById(R.id.button_open_messages);
        Button pickImageButton = findViewById(R.id.button_pick_image);
        Button saveCabinButton = findViewById(R.id.button_create_cabin);
        Button clearFormButton = findViewById(R.id.button_clear_form);
        ManagerNavigationHelper.bind(this, ManagerNavigationHelper.TAB_DASHBOARD);
        backButton.setOnClickListener(view -> finish());
        bottomBackButton.setOnClickListener(view -> finish());
        newCabinButton.setOnClickListener(view -> startActivity(new Intent(this, AdminHotelFormActivity.class)));
        messagesButton.setOnClickListener(view -> startActivity(new Intent(this, ConversationListActivity.class)));
        manageSelectedHotelButton.setOnClickListener(view -> openSelectedHotelManager());
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
            statusTextView.setText("Đã chọn ảnh.");
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
                        finishCabinLoad(cabinsWithRooms);
                    }

                    @Override
                    public void onError(String message) {
                        finishCabinLoad(cabins);
                        statusTextView.setText("Đã tải khách sạn, nhưng loại phòng chưa sẵn sàng: " + message);
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

    private void finishCabinLoad(List<Cabin> cabins) {
        List<Cabin> loadedCabins = cabins == null ? new ArrayList<>() : cabins;
        hydrateMissingRoomTypes(loadedCabins, () -> {
            allManagedCabins.clear();
            allManagedCabins.addAll(loadedCabins);
            assignCheapestRoomTypes(allManagedCabins);
            renderHotelTabs();
            renderCabins();
            openPendingEditIfReady();
        });
    }

    private void hydrateMissingRoomTypes(List<Cabin> cabins, Runnable done) {
        List<Cabin> missing = new ArrayList<>();
        for (Cabin cabin : cabins) {
            if (cabin.getId() != null && (cabin.getRoomTypes() == null || cabin.getRoomTypes().isEmpty())) {
                missing.add(cabin);
            }
        }
        if (missing.isEmpty()) {
            done.run();
            return;
        }
        AtomicInteger completed = new AtomicInteger(0);
        for (Cabin cabin : missing) {
            roomTypeService.getRoomTypesForCabin(cabin.getId(), new SupabaseCallback<List<RoomType>>() {
                @Override
                public void onSuccess(List<RoomType> roomTypes) {
                    cabin.setRoomTypes(roomTypes == null ? new ArrayList<>() : roomTypes);
                    completeRoomHydration(missing.size(), completed, done);
                }

                @Override
                public void onError(String message) {
                    cabin.setRoomTypes(new ArrayList<>());
                    completeRoomHydration(missing.size(), completed, done);
                }
            });
        }
    }

    private void completeRoomHydration(int total, AtomicInteger completed, Runnable done) {
        if (completed.incrementAndGet() >= total) {
            done.run();
        }
    }

    private String displayName() {
        String name = sessionManager.getFullName();
        return name == null || name.trim().isEmpty() ? "Quản lý" : name.trim();
    }

    private void renderCabins() {
        String query = cabinSearchEditText.getText().toString().trim().toLowerCase(java.util.Locale.US);
        List<Cabin> filtered = allManagedCabins.stream()
                .filter(cabin -> selectedManagerCabinId == null || selectedManagerCabinId.equals(cabin.getId()))
                .filter(cabin -> matchesCabinSearch(cabin, query))
                .collect(Collectors.toList());
        filtered.sort((left, right) -> safe(left.getName()).compareToIgnoreCase(safe(right.getName())));
        cabinAdapter.submitList(selectedManagerCabinId == null ? filtered : new ArrayList<>());
        renderSelectedHotelRooms();
        updateDashboardMetrics(filtered);
        String scope = selectedManagerCabinId == null ? "tất cả khách sạn" : "khách sạn đang chọn";
        statusTextView.setText("Đang hiển thị " + filtered.size() + "/" + allManagedCabins.size()
                + " " + scope + ". Tab Tất cả dùng để sửa thông tin hotel; bấm từng tab hotel để quản lý room.");
    }

    private void renderHotelTabs() {
        hotelTabsContainer.removeAllViews();
        addHotelTab("Tất cả", null, selectedManagerCabinId == null);
        for (Cabin cabin : allManagedCabins) {
            addHotelTab(shortHotelName(cabin), cabin.getId(), cabin.getId().equals(selectedManagerCabinId));
        }
    }

    private void addHotelTab(String label, String cabinId, boolean selected) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setTextColor(getColor(selected ? R.color.black : R.color.ink));
        button.setTextSize(13);
        button.setBackgroundResource(selected ? R.drawable.bg_button_primary : R.drawable.bg_manager_search);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
        );
        params.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(params);
        button.setOnClickListener(view -> {
            selectedManagerCabinId = cabinId;
            renderHotelTabs();
            renderCabins();
        });
        hotelTabsContainer.addView(button);
    }

    private void renderSelectedHotelRooms() {
        selectedRoomsContainer.removeAllViews();
        Cabin selectedCabin = findCabinById(selectedManagerCabinId);
        if (selectedCabin == null) {
            selectedRoomsPanel.setVisibility(View.GONE);
            return;
        }
        selectedRoomsPanel.setVisibility(View.VISIBLE);
        selectedRoomsTitleTextView.setText("Loại phòng · " + selectedCabin.getName());
        List<RoomType> roomTypes = selectedCabin.getRoomTypes();
        int count = roomTypes == null ? 0 : roomTypes.size();
        selectedRoomsHintTextView.setText(count + " loại phòng đang hoạt động. Chạm một room để mở đúng form chỉnh loại phòng đó.");
        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("Khách sạn này chưa có loại phòng. Bấm nút bên dưới để thêm Solo, Twin, Family, Suite...");
            empty.setTextColor(getColor(R.color.muted));
            empty.setTextSize(13f);
            empty.setLineSpacing(dp(3), 1f);
            selectedRoomsContainer.addView(empty);
            return;
        }
        for (RoomType roomType : roomTypes) {
            selectedRoomsContainer.addView(roomTypeCard(selectedCabin, roomType));
        }
    }

    private View roomTypeCard(Cabin cabin, RoomType roomType) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_manager_search);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(roomType.displayName() + " · " + PriceUtils.formatUsd(roomType.getBasePrice()) + " / đêm");
        title.setTextColor(getColor(R.color.ink));
        title.setTextSize(16f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView meta = new TextView(this);
        meta.setText(roomType.effectiveMaxAdults() + " khách · " + roomType.effectiveBedCount()
                + " giường · " + roomType.getTotalRooms() + " phòng · " + roomType.sizeLabel());
        meta.setTextColor(getColor(R.color.muted));
        meta.setTextSize(13f);
        meta.setPadding(0, dp(5), 0, 0);
        card.addView(meta);

        TextView beds = new TextView(this);
        beds.setText(roomType.bedLabel());
        beds.setTextColor(getColor(R.color.primary));
        beds.setTextSize(13f);
        beds.setTypeface(null, android.graphics.Typeface.BOLD);
        beds.setPadding(0, dp(6), 0, 0);
        card.addView(beds);

        card.setOnClickListener(view -> openRoomManager(cabin.getId(), roomType.getId()));
        return card;
    }

    private Cabin findCabinById(String cabinId) {
        if (cabinId == null) {
            return null;
        }
        for (Cabin cabin : allManagedCabins) {
            if (cabinId.equals(cabin.getId())) {
                return cabin;
            }
        }
        return null;
    }

    private void openSelectedHotelManager() {
        if (selectedManagerCabinId != null) {
            openHotelManager(selectedManagerCabinId);
        }
    }

    private void openHotelManager(String cabinId) {
        Intent intent = new Intent(this, AdminHotelFormActivity.class);
        intent.putExtra(AdminHotelFormActivity.EXTRA_CABIN_ID, cabinId);
        startActivity(intent);
    }

    private void openRoomManager(String cabinId, String roomTypeId) {
        Intent intent = new Intent(this, AdminHotelFormActivity.class);
        intent.putExtra(AdminHotelFormActivity.EXTRA_CABIN_ID, cabinId);
        intent.putExtra(AdminHotelFormActivity.EXTRA_ROOM_TYPE_ID, roomTypeId);
        startActivity(intent);
    }

    private void updateDashboardMetrics(List<Cabin> visibleCabins) {
        int hotelCount = allManagedCabins.size();
        int roomTypeCount = 0;
        int roomUnitCount = 0;
        double minPrice = Double.MAX_VALUE;
        for (Cabin cabin : allManagedCabins) {
            if (cabin.getRoomTypes() == null || cabin.getRoomTypes().isEmpty()) {
                minPrice = Math.min(minPrice, cabin.displayPrice());
                continue;
            }
            roomTypeCount += cabin.getRoomTypes().size();
            for (RoomType roomType : cabin.getRoomTypes()) {
                roomUnitCount += Math.max(0, roomType.getTotalRooms());
                if (roomType.getBasePrice() > 0) {
                    minPrice = Math.min(minPrice, roomType.getBasePrice());
                }
            }
        }
        hotelMetricTextView.setText(hotelCount + "\nKhách sạn");
        roomMetricTextView.setText(roomTypeCount + " loại\n" + roomUnitCount + " phòng");
        priceMetricTextView.setText((minPrice == Double.MAX_VALUE ? "$0" : PriceUtils.formatUsd(minPrice)) + "\nGiá từ");
        if (visibleCabins.isEmpty() && !allManagedCabins.isEmpty()) {
            statusTextView.animate().alpha(0.55f).setDuration(90)
                    .withEndAction(() -> statusTextView.animate().alpha(1f).setDuration(140).start())
                    .start();
        }
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
                statusTextView.setText("Đang sửa " + cabin.getName());
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
            statusTextView.setText("Giá, giảm giá và sức chứa phải là số hợp lệ.");
            return;
        }

        if (editingCabin == null) {
            createCabin(cabin);
        } else {
            updateCabin(cabin);
        }
    }

    private void createCabin(Cabin cabin) {
        statusTextView.setText("Đang tạo khách sạn...");
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
        statusTextView.setText("Đang cập nhật khách sạn...");
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
        statusTextView.setText("Đang xóa khách sạn...");
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
                Toast.makeText(HostHotelDashboardActivity.this, "Đã xóa khách sạn", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Đã xóa khách sạn.");
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
                    statusTextView.setText("Đã lưu khách sạn, nhưng một tiện nghi chưa gắn được: " + message);
                }
            });
        }
    }

    private void loadBookingsForCabin(Cabin cabin) {
        selectedCabinId = cabin.getId();
        bookingTitleTextView.setText("Đặt phòng của " + cabin.getName());
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

    private double priceAfterDiscount(Cabin cabin) {
        return Math.max(0, cabin.getRegularPrice() - cabin.getDiscount());
    }

    private String shortHotelName(Cabin cabin) {
        String name = safe(cabin.getName());
        return name.length() <= 22 ? name : name.substring(0, 21).trim() + "...";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
