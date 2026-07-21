package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Amenity;
import com.example.hotel_booking_app.data.models.BlockedDate;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.CabinAmenity;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AmenityService;
import com.example.hotel_booking_app.services.BlockedDateService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminHotelFormActivity extends AppCompatActivity {
    public static final String EXTRA_CABIN_ID = "extra_admin_cabin_id";
    public static final String EXTRA_ROOM_TYPE_ID = "extra_admin_room_type_id";
    public static final String EXTRA_ROOM_MODE = "extra_admin_room_mode";
    private static final int REQUEST_PICK_IMAGE = 2001;
    private static final int REQUEST_PICK_LOCATION = 2002;

    private TextView formTitleTextView;
    private TextView statusTextView;
    private TextView coordinatesTextView;
    private EditText nameEditText;
    private EditText locationEditText;
    private EditText capacityEditText;
    private EditText priceEditText;
    private EditText discountEditText;
    private EditText descriptionEditText;
    private EditText imageEditText;
    private EditText roomNameEditText;
    private EditText roomTotalEditText;
    private EditText roomGuestsEditText;
    private EditText roomSizeEditText;
    private EditText roomBedCountEditText;
    private EditText roomPriceEditText;
    private EditText blockStartEditText;
    private EditText blockEndEditText;
    private EditText blockRoomsEditText;
    private EditText blockReasonEditText;
    private Spinner roomCategorySpinner;
    private Spinner bedTypeSpinner;
    private LinearLayout amenitiesContainer;
    private LinearLayout roomTypesContainer;
    private LinearLayout roomFiltersContainer;
    private final List<RoomType> loadedRoomTypes = new ArrayList<>();
    private String selectedRoomCategoryFilter = "Tất cả";
    private String selectedBedTypeFilter = "Tất cả";
    private Button saveButton;
    private Button saveRoomTypeButton;
    private Button pickLocationButton;
    private Button blockRoomButton;
    private CabinService cabinService;
    private AmenityService amenityService;
    private RoomTypeService roomTypeService;
    private BlockedDateService blockedDateService;
    private SessionManager sessionManager;
    private Cabin editingCabin;
    private RoomType editingRoomType;
    private String cabinId;
    private String pendingRoomTypeId;
    private boolean roomMode;
    private final List<Amenity> allAmenities = new ArrayList<>();
    private final List<String> selectedAmenityIds = new ArrayList<>();
    private final List<String> selectedAmenityNames = new ArrayList<>();
    private double pickedLatitude;
    private double pickedLongitude;
    private String pickedGoogleMapsUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_hotel_form);

        cabinService = new CabinService();
        amenityService = new AmenityService();
        roomTypeService = new RoomTypeService();
        blockedDateService = new BlockedDateService();
        sessionManager = new SessionManager(this);
        cabinId = getIntent().getStringExtra(EXTRA_CABIN_ID);
        pendingRoomTypeId = getIntent().getStringExtra(EXTRA_ROOM_TYPE_ID);
        roomMode = getIntent().getBooleanExtra(EXTRA_ROOM_MODE, false);

        formTitleTextView = findViewById(R.id.text_form_title);
        statusTextView = findViewById(R.id.text_status);
        coordinatesTextView = findViewById(R.id.text_location_coordinates);
        nameEditText = findViewById(R.id.edit_cabin_name);
        locationEditText = findViewById(R.id.edit_location);
        capacityEditText = findViewById(R.id.edit_capacity);
        priceEditText = findViewById(R.id.edit_price);
        discountEditText = findViewById(R.id.edit_discount);
        descriptionEditText = findViewById(R.id.edit_description);
        imageEditText = findViewById(R.id.edit_image);
        roomNameEditText = findViewById(R.id.edit_room_name);
        roomTotalEditText = findViewById(R.id.edit_room_total);
        roomGuestsEditText = findViewById(R.id.edit_room_guests);
        roomSizeEditText = findViewById(R.id.edit_room_size);
        roomBedCountEditText = findViewById(R.id.edit_room_bed_count);
        roomPriceEditText = findViewById(R.id.edit_room_price);
        blockStartEditText = findViewById(R.id.edit_block_start);
        blockEndEditText = findViewById(R.id.edit_block_end);
        blockRoomsEditText = findViewById(R.id.edit_block_rooms);
        blockReasonEditText = findViewById(R.id.edit_block_reason);
        roomCategorySpinner = findViewById(R.id.spinner_room_category);
        bedTypeSpinner = findViewById(R.id.spinner_bed_type);
        amenitiesContainer = findViewById(R.id.container_amenities);
        roomFiltersContainer = findViewById(R.id.container_room_filters);
        roomTypesContainer = findViewById(R.id.container_room_types);
        saveButton = findViewById(R.id.button_save_cabin);
        saveRoomTypeButton = findViewById(R.id.button_save_room_type);
        pickLocationButton = findViewById(R.id.button_pick_location);
        blockRoomButton = findViewById(R.id.button_block_room);
        Button pickImageButton = findViewById(R.id.button_pick_image);
        Button topBackButton = findViewById(R.id.button_back);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);

        boolean editMode = cabinId != null && !cabinId.trim().isEmpty();
        formTitleTextView.setText(editMode ? "Sửa khách sạn" : "Tạo khách sạn mới");
        saveButton.setText(editMode ? "Cập nhật khách sạn" : "Tạo khách sạn");

        pickImageButton.setOnClickListener(view -> openImagePicker());
        pickLocationButton.setOnClickListener(view -> openLocationPicker());
        saveButton.setOnClickListener(view -> saveCabin());
        saveRoomTypeButton.setOnClickListener(view -> saveRoomType());
        blockRoomButton.setOnClickListener(view -> blockSelectedRoomDates());
        topBackButton.setOnClickListener(view -> finish());
        bottomBackButton.setOnClickListener(view -> finish());
        setupRoomSpinners();
        applyMode();

        loadAmenities();
        if (editMode) {
            loadCabin();
        } else {
            statusTextView.setText("Tạo khách sạn mới cho Serein Stay.");
            renderRoomTypes(new ArrayList<>());
        }
    }

    private void setupRoomSpinners() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Standard", "Superior", "Deluxe", "Suite"});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roomCategorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<String> bedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Single", "Double", "Queen", "King"});
        bedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bedTypeSpinner.setAdapter(bedAdapter);
    }

    private void applyMode() {
        if (!isRoomMode()) {
            return;
        }
        formTitleTextView.setText("Sửa loại phòng");
        saveButton.setVisibility(View.GONE);
        nameEditText.setEnabled(false);
        locationEditText.setEnabled(false);
        capacityEditText.setEnabled(false);
        priceEditText.setEnabled(false);
        discountEditText.setEnabled(false);
        descriptionEditText.setEnabled(false);
        if (pickLocationButton != null) {
            pickLocationButton.setEnabled(false);
        }
        amenitiesContainer.setVisibility(View.GONE);
        statusTextView.setText("Đang sửa loại phòng. Phần khách sạn đã khóa.");
    }

    private boolean isRoomMode() {
        return roomMode || (pendingRoomTypeId != null && !pendingRoomTypeId.trim().isEmpty());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void openLocationPicker() {
        Intent intent = new Intent(this, HotelMapActivity.class);
        intent.putExtra(HotelMapActivity.EXTRA_PICK_LOCATION, true);
        intent.putExtra(HotelMapActivity.EXTRA_PICK_LOCATION_LABEL, locationEditText.getText().toString().trim());
        if (pickedLatitude != 0 && pickedLongitude != 0) {
            intent.putExtra(HotelMapActivity.EXTRA_PICK_LATITUDE, pickedLatitude);
            intent.putExtra(HotelMapActivity.EXTRA_PICK_LONGITUDE, pickedLongitude);
        }
        startActivityForResult(intent, REQUEST_PICK_LOCATION);
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
        if (requestCode == REQUEST_PICK_LOCATION && resultCode == RESULT_OK && data != null) {
            pickedLatitude = data.getDoubleExtra(HotelMapActivity.EXTRA_PICK_LATITUDE, 0);
            pickedLongitude = data.getDoubleExtra(HotelMapActivity.EXTRA_PICK_LONGITUDE, 0);
            pickedGoogleMapsUrl = data.getStringExtra(HotelMapActivity.EXTRA_PICK_GOOGLE_MAPS_URL);
            renderPickedLocation();
            statusTextView.setText("Da chon vi tri moi. Bam cap nhat khach san de luu vao database.");
        }
    }

    private void renderPickedLocation() {
        if (coordinatesTextView == null) {
            return;
        }
        if (pickedLatitude == 0 || pickedLongitude == 0) {
            coordinatesTextView.setText("Chua chon toa do ban do.");
            return;
        }
        coordinatesTextView.setText(String.format(Locale.US,
                "Toa do: %.6f, %.6f",
                pickedLatitude,
                pickedLongitude));
    }

    private void loadAmenities() {
        amenityService.getAmenities(new SupabaseCallback<List<Amenity>>() {
            @Override
            public void onSuccess(List<Amenity> amenities) {
                allAmenities.clear();
                allAmenities.addAll(amenities);
                syncSelectedAmenitiesFromCabin();
                renderAmenityCheckboxes();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Không tải được tiện nghi: " + message);
            }
        });
    }

    private void loadCabin() {
        statusTextView.setText("Đang tải khách sạn...");
        cabinService.getCabinById(cabinId, new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                if (cabin == null) {
                    statusTextView.setText("Không tìm thấy khách sạn để sửa. Quay lại danh sách và thử lại.");
                    return;
                }
                editingCabin = cabin;
                nameEditText.setText(safe(cabin.getName()));
                locationEditText.setText(safe(cabin.getLocation()));
                pickedLatitude = cabin.getLatitude();
                pickedLongitude = cabin.getLongitude();
                pickedGoogleMapsUrl = cabin.getGoogleMapsUrl();
                renderPickedLocation();
                capacityEditText.setText(String.valueOf(cabin.getMaxCapacity()));
                priceEditText.setText(String.valueOf(cabin.getRegularPrice()));
                discountEditText.setText(String.valueOf(cabin.getDiscount()));
                descriptionEditText.setText(safe(cabin.getDescription()));
                imageEditText.setText(safe(cabin.getImage()));
                syncSelectedAmenitiesFromCabin();
                renderAmenityCheckboxes();
                if (isRoomMode()) {
                    loadRoomTypes();
                } else {
                    renderHotelOnlyRoomNotice();
                }
                statusTextView.setText("Khách sạn đã sẵn sàng.");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
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

    private void syncSelectedAmenitiesFromCabin() {
        if (editingCabin == null || editingCabin.getAmenities() == null || allAmenities.isEmpty()) {
            return;
        }
        selectedAmenityNames.clear();
        selectedAmenityIds.clear();
        for (Amenity amenity : allAmenities) {
            if (editingCabin.getAmenities().contains(amenity.getName())) {
                selectedAmenityIds.add(amenity.getId());
                selectedAmenityNames.add(amenity.getName());
            }
        }
    }

    private void loadRoomTypes() {
        if (editingCabin == null || editingCabin.getId() == null) {
            renderRoomTypes(new ArrayList<>());
            return;
        }
        roomTypeService.getRoomTypesForCabin(editingCabin.getId(), new SupabaseCallback<List<RoomType>>() {
            @Override
            public void onSuccess(List<RoomType> roomTypes) {
                renderRoomTypes(roomTypes);
            }

            @Override
            public void onError(String message) {
                statusTextView.setText("Không tải được loại phòng: " + message);
            }
        });
    }

    private void renderRoomTypes(List<RoomType> roomTypes) {
        loadedRoomTypes.clear();
        if (roomTypes != null) {
            loadedRoomTypes.addAll(roomTypes);
        }
        renderRoomFilters();
        roomTypesContainer.removeAllViews();
        if (editingCabin == null || editingCabin.getId() == null) {
            roomTypesContainer.addView(roomHint("Lưu khách sạn trước, sau đó thêm loại phòng."));
            return;
        }

        List<RoomType> visibleRoomTypes = applyRoomFilters(loadedRoomTypes);
        if (loadedRoomTypes.isEmpty()) {
            roomTypesContainer.addView(roomHint("Chưa có loại phòng. Thêm Standard, Superior, Deluxe hoặc Suite bên dưới."));
            return;
        }
        if (visibleRoomTypes.isEmpty()) {
            roomTypesContainer.addView(roomHint("Không có loại phòng phù hợp bộ lọc hiện tại."));
            return;
        }

        for (RoomType roomType : visibleRoomTypes) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(R.drawable.bg_status);
            row.setPadding(12, 12, 12, 12);

            TextView summary = roomHint(roomType.titleLabel()
                    + " · " + roomType.categoryLabel()
                    + " · " + roomType.sizeLabel()
                    + " · " + roomType.bedLabel()
                    + "\n" + roomType.getTotalRooms() + " phòng · tối đa "
                    + roomType.effectiveMaxAdults() + " người lớn · "
                    + roomType.effectiveBedCount() + " giường · $" + roomType.getBasePrice());
            row.addView(summary);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button edit = new Button(this);
            edit.setText("Sửa");
            edit.setAllCaps(false);
            edit.setOnClickListener(view -> fillRoomForm(roomType));
            Button delete = new Button(this);
            delete.setText("Xóa");
            delete.setAllCaps(false);
            delete.setOnClickListener(view -> deleteRoomType(roomType));
            Button block = new Button(this);
            block.setText("Chan lich");
            block.setAllCaps(false);
            block.setOnClickListener(view -> {
                fillRoomForm(roomType);
                blockReasonEditText.setText("");
                statusTextView.setText("Nhap khoang ngay ban cho: " + roomType.titleLabel());
            });
            actions.addView(edit, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            actions.addView(delete, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            actions.addView(block, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(actions);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = 10;
            roomTypesContainer.addView(row, params);
        }
        selectPendingRoomType(loadedRoomTypes);
    }

    private void renderHotelOnlyRoomNotice() {
        loadedRoomTypes.clear();
        renderRoomFilters();
        roomTypesContainer.removeAllViews();
        roomTypesContainer.addView(roomHint("Đang sửa thông tin khách sạn. Muốn thêm hoặc sửa loại phòng, quay lại dashboard, chọn hotel rồi bấm + Phòng hoặc chạm vào một room."));
    }

    private void renderRoomFilters() {
        if (roomFiltersContainer == null) {
            return;
        }
        roomFiltersContainer.removeAllViews();
        if (loadedRoomTypes.isEmpty()) {
            roomFiltersContainer.setVisibility(View.GONE);
            return;
        }
        roomFiltersContainer.setVisibility(View.VISIBLE);

        TextView categoryTitle = roomHint("Lọc loại phòng");
        categoryTitle.setTextColor(getColor(R.color.primary));
        categoryTitle.setTextSize(13f);
        categoryTitle.setPadding(0, 0, 0, 6);
        roomFiltersContainer.addView(categoryTitle);
        roomFiltersContainer.addView(buildFilterScroll(buildCategoryFilters(), true));

        TextView bedTitle = roomHint("Lọc theo giường");
        bedTitle.setTextColor(getColor(R.color.primary));
        bedTitle.setTextSize(13f);
        bedTitle.setPadding(0, 10, 0, 6);
        roomFiltersContainer.addView(bedTitle);
        roomFiltersContainer.addView(buildFilterScroll(buildBedFilters(), false));
    }

    private void selectPendingRoomType(List<RoomType> roomTypes) {
        if (pendingRoomTypeId == null || pendingRoomTypeId.trim().isEmpty() || roomTypes == null) {
            return;
        }
        for (RoomType roomType : roomTypes) {
            if (roomType != null && pendingRoomTypeId.equals(roomType.getId())) {
                fillRoomForm(roomType);
                statusTextView.setText("Đang sửa loại phòng: " + roomType.titleLabel());
                pendingRoomTypeId = null;
                return;
            }
        }
    }

    private View buildFilterScroll(List<String> filters, boolean categoryFilter) {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, 0);

        for (String value : filters) {
            String label = categoryFilter ? roomCategoryFilterLabel(value) : bedFilterLabel(value);
            boolean selected = categoryFilter
                    ? selectedRoomCategoryFilter.equalsIgnoreCase(value)
                    : selectedBedTypeFilter.equalsIgnoreCase(value);
            Button chip = new Button(this);
            chip.setText(label);
            chip.setAllCaps(false);
            chip.setTextSize(12f);
            chip.setTextColor(getColor(selected ? R.color.black : R.color.ink));
            chip.setBackgroundResource(selected ? R.drawable.bg_button_primary : R.drawable.bg_manager_search);
            chip.setPadding(18, 0, 18, 0);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(36)
            );
            chipParams.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(chipParams);
            chip.setOnClickListener(view -> {
                if (categoryFilter) {
                    selectedRoomCategoryFilter = value;
                } else {
                    selectedBedTypeFilter = value;
                }
                renderRoomTypes(new ArrayList<>(loadedRoomTypes));
            });
            row.addView(chip);
        }

        scrollView.addView(row);
        return scrollView;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private List<RoomType> applyRoomFilters(List<RoomType> roomTypes) {
        List<RoomType> filtered = new ArrayList<>();
        if (roomTypes == null) {
            return filtered;
        }
        for (RoomType roomType : roomTypes) {
            if (roomType == null) {
                continue;
            }
            boolean matchesCategory = "Tất cả".equalsIgnoreCase(selectedRoomCategoryFilter)
                    || selectedRoomCategoryFilter.equalsIgnoreCase(safe(roomType.getCategory()));
            boolean matchesBedType = "Tất cả".equalsIgnoreCase(selectedBedTypeFilter)
                    || selectedBedTypeFilter.equalsIgnoreCase(safe(roomType.getBedType()));
            if (matchesCategory && matchesBedType) {
                filtered.add(roomType);
            }
        }
        return filtered;
    }

    private List<String> buildCategoryFilters() {
        java.util.LinkedHashSet<String> filters = new java.util.LinkedHashSet<>();
        filters.add("Tất cả");
        String[] preferred = {"Standard", "Solo", "Twin", "Superior", "Deluxe", "Family", "Suite"};
        for (String candidate : preferred) {
            if (hasRoomTypeCategory(candidate)) {
                filters.add(candidate);
            }
        }
        for (RoomType roomType : loadedRoomTypes) {
            String category = safe(roomType.getCategory());
            if (!category.isEmpty()) {
                filters.add(category);
            }
        }
        return new ArrayList<>(filters);
    }

    private List<String> buildBedFilters() {
        java.util.LinkedHashSet<String> filters = new java.util.LinkedHashSet<>();
        filters.add("Tất cả");
        String[] preferred = {"Single", "Double", "Queen", "King"};
        for (String candidate : preferred) {
            if (hasBedType(candidate)) {
                filters.add(candidate);
            }
        }
        for (RoomType roomType : loadedRoomTypes) {
            String bedType = safe(roomType.getBedType());
            if (!bedType.isEmpty()) {
                filters.add(bedType);
            }
        }
        return new ArrayList<>(filters);
    }

    private boolean hasRoomTypeCategory(String category) {
        for (RoomType roomType : loadedRoomTypes) {
            if (category.equalsIgnoreCase(safe(roomType.getCategory()))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBedType(String bedType) {
        for (RoomType roomType : loadedRoomTypes) {
            if (bedType.equalsIgnoreCase(safe(roomType.getBedType()))) {
                return true;
            }
        }
        return false;
    }

    private String roomCategoryFilterLabel(String value) {
        if (value == null || value.trim().isEmpty() || "Tất cả".equalsIgnoreCase(value)) {
            return "Tất cả";
        }
        if ("Standard".equalsIgnoreCase(value)) {
            return "Tiêu chuẩn";
        }
        if ("Solo".equalsIgnoreCase(value)) {
            return "Phòng đơn";
        }
        if ("Twin".equalsIgnoreCase(value)) {
            return "Phòng 2 giường";
        }
        if ("Superior".equalsIgnoreCase(value)) {
            return "Cao cấp";
        }
        if ("Deluxe".equalsIgnoreCase(value)) {
            return "Deluxe";
        }
        if ("Family".equalsIgnoreCase(value)) {
            return "Gia đình";
        }
        if ("Suite".equalsIgnoreCase(value)) {
            return "Suite";
        }
        return value;
    }

    private String bedFilterLabel(String value) {
        if (value == null || value.trim().isEmpty() || "Tất cả".equalsIgnoreCase(value)) {
            return "Tất cả";
        }
        if ("Single".equalsIgnoreCase(value)) {
            return "Đơn";
        }
        if ("Double".equalsIgnoreCase(value)) {
            return "Đôi";
        }
        if ("Queen".equalsIgnoreCase(value)) {
            return "Queen";
        }
        if ("King".equalsIgnoreCase(value)) {
            return "King";
        }
        return value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private TextView roomHint(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(getColor(R.color.ink));
        view.setTextSize(14f);
        view.setPadding(10, 8, 10, 8);
        return view;
    }

    private void fillRoomForm(RoomType roomType) {
        editingRoomType = roomType;
        setSpinnerSelection(roomCategorySpinner, roomType.getCategory());
        setSpinnerSelection(bedTypeSpinner, roomType.getBedType());
        roomNameEditText.setText(roomType.getName());
        roomTotalEditText.setText(String.valueOf(roomType.getTotalRooms()));
        roomGuestsEditText.setText(String.valueOf(roomType.effectiveMaxAdults()));
        roomSizeEditText.setText(String.valueOf(roomType.getSizeM2()));
        roomBedCountEditText.setText(String.valueOf(roomType.effectiveBedCount()));
        roomPriceEditText.setText(String.valueOf(roomType.getBasePrice()));
        saveRoomTypeButton.setText("Cập nhật loại phòng");
    }

    private void blockSelectedRoomDates() {
        if (editingCabin == null || editingCabin.getId() == null) {
            statusTextView.setText("Luu khach san truoc khi chan lich phong.");
            return;
        }
        if (editingRoomType == null || editingRoomType.getId() == null) {
            statusTextView.setText("Chon mot loai phong truoc khi chan lich.");
            return;
        }

        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(blockStartEditText.getText().toString().trim());
            endDate = LocalDate.parse(blockEndEditText.getText().toString().trim());
        } catch (Exception exception) {
            statusTextView.setText("Ngay ban phai dung dinh dang YYYY-MM-DD.");
            return;
        }
        if (!endDate.isAfter(startDate)) {
            statusTextView.setText("Ngay ket thuc phai sau ngay bat dau.");
            return;
        }
        int blockedRooms;
        try {
            String roomsValue = blockRoomsEditText.getText().toString().trim();
            blockedRooms = roomsValue.isEmpty() ? 1 : Integer.parseInt(roomsValue);
        } catch (NumberFormatException exception) {
            statusTextView.setText("So phong bi giu phai la so hop le.");
            return;
        }
        if (blockedRooms <= 0 || blockedRooms > Math.max(1, editingRoomType.getTotalRooms())) {
            statusTextView.setText("So phong bi giu phai tu 1 den " + Math.max(1, editingRoomType.getTotalRooms()) + ".");
            return;
        }

        String reason = blockReasonEditText.getText().toString().trim();
        if (reason.isEmpty()) {
            reason = "External booking or manager block";
        }
        statusTextView.setText("Dang chan lich cho loai phong...");
        blockedDateService.blockDates(
                editingCabin.getId(),
                editingRoomType.getId(),
                sessionManager.getUserId(),
                startDate.toString(),
                endDate.toString(),
                blockedRooms,
                reason,
                new SupabaseCallback<BlockedDate>() {
                    @Override
                    public void onSuccess(BlockedDate data) {
                        blockStartEditText.setText("");
                        blockEndEditText.setText("");
                        blockRoomsEditText.setText("");
                        blockReasonEditText.setText("");
                        statusTextView.setText("Da chan lich. Booking qua app se thay phong nay khong kha dung trong khoang ngay do.");
                    }

                    @Override
                    public void onError(String message) {
                        statusTextView.setText(message);
                    }
                }
        );
    }

    private void saveRoomType() {
        if (editingCabin == null || editingCabin.getId() == null) {
            statusTextView.setText("Lưu khách sạn trước khi thêm loại phòng.");
            return;
        }
        RoomType roomType = editingRoomType == null ? new RoomType() : editingRoomType;
        try {
            String category = String.valueOf(roomCategorySpinner.getSelectedItem());
            int sizeM2 = Integer.parseInt(roomSizeEditText.getText().toString().trim());
            String bedType = String.valueOf(bedTypeSpinner.getSelectedItem());
            int maxAdults = Integer.parseInt(roomGuestsEditText.getText().toString().trim());
            int bedCount = Integer.parseInt(roomBedCountEditText.getText().toString().trim());
            String roomName = roomNameEditText.getText().toString().trim();
            roomType.setCabinId(editingCabin.getId());
            roomType.setCategory(category);
            roomType.setName(roomName.isEmpty()
                    ? category + " " + bedType + " Room"
                    : roomName);
            roomType.setDescription(category + " tại " + editingCabin.getName());
            roomType.setTotalRooms(Integer.parseInt(roomTotalEditText.getText().toString().trim()));
            roomType.setMaxGuests(maxAdults);
            roomType.setMaxAdults(maxAdults);
            roomType.setBedCount(bedCount);
            roomType.setSleepingCapacity(Math.max(maxAdults, bedAdultCapacity(bedType) * bedCount));
            roomType.setSizeM2(sizeM2);
            roomType.setSize(sizeM2 + " m2");
            roomType.setBasePrice(Double.parseDouble(roomPriceEditText.getText().toString().trim()));
            roomType.setBedType(bedType);
            roomType.setBeds(bedSummary(bedType, bedCount));
            roomType.setBedSummary(bedSummary(bedType, bedCount));
            roomType.setBedWidthM(bedWidth(bedType));
            roomType.setBedLengthM(2.0);
            roomType.setHasLivingRoom("Suite".equalsIgnoreCase(category));
            roomType.setAmenities(editingCabin.getAmenities());
            roomType.setImage(editingCabin.getImage());
            roomType.setActive(true);
            if (!roomType.fitsRoomSizeForGuests(maxAdults)) {
                statusTextView.setText("Diện tích phòng chưa đủ cho số khách này.");
                return;
            }
        } catch (NumberFormatException exception) {
            statusTextView.setText("Số phòng, người lớn, giường, diện tích và giá phải là số hợp lệ.");
            return;
        }

        SupabaseCallback<RoomType> callback = new SupabaseCallback<RoomType>() {
            @Override
            public void onSuccess(RoomType data) {
                clearRoomForm();
                loadRoomTypes();
                statusTextView.setText("Đã lưu loại phòng.");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        };
        if (editingRoomType == null) {
            roomTypeService.createRoomType(roomType, callback);
        } else {
            roomTypeService.updateRoomType(roomType, callback);
        }
    }

    private void deleteRoomType(RoomType roomType) {
        roomTypeService.deleteRoomType(roomType.getId(), new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                clearRoomForm();
                loadRoomTypes();
                statusTextView.setText("Đã ẩn loại phòng. Dữ liệu vẫn còn trong database.");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void clearRoomForm() {
        editingRoomType = null;
        roomNameEditText.setText("");
        roomTotalEditText.setText("");
        roomGuestsEditText.setText("");
        roomSizeEditText.setText("");
        roomBedCountEditText.setText("");
        roomPriceEditText.setText("");
        saveRoomTypeButton.setText("Lưu loại phòng");
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null || spinner.getAdapter() == null) {
            return;
        }
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            if (value.equalsIgnoreCase(String.valueOf(spinner.getAdapter().getItem(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private double bedWidth(String bedType) {
        if ("Single".equalsIgnoreCase(bedType)) {
            return 1.0;
        }
        if ("Double".equalsIgnoreCase(bedType)) {
            return 1.5;
        }
        if ("Queen".equalsIgnoreCase(bedType)) {
            return 1.6;
        }
        return 1.8;
    }

    private int bedAdultCapacity(String bedType) {
        if ("Single".equalsIgnoreCase(bedType)) {
            return 1;
        }
        return 2;
    }

    private String bedSummary(String bedType, int bedCount) {
        int count = Math.max(1, bedCount);
        String label;
        if ("Single".equalsIgnoreCase(bedType)) {
            label = "giường đơn";
        } else if ("Double".equalsIgnoreCase(bedType)) {
            label = "giường đôi";
        } else if ("Queen".equalsIgnoreCase(bedType)) {
            label = "giường Queen";
        } else if ("King".equalsIgnoreCase(bedType)) {
            label = "giường King";
        } else {
            label = "giường " + bedType;
        }
        return count + " " + label;
    }

    private void saveCabin() {
        if (isRoomMode()) {
            statusTextView.setText("Chỉ sửa loại phòng ở chế độ này.");
            return;
        }
        Cabin cabin = editingCabin == null ? new Cabin() : editingCabin;
        try {
            cabin.setName(nameEditText.getText().toString().trim());
            cabin.setLocation(locationEditText.getText().toString().trim());
            cabin.setAddress(locationEditText.getText().toString().trim());
            if (pickedLatitude != 0 && pickedLongitude != 0) {
                cabin.setLatitude(pickedLatitude);
                cabin.setLongitude(pickedLongitude);
                cabin.setGoogleMapsUrl(pickedGoogleMapsUrl == null || pickedGoogleMapsUrl.trim().isEmpty()
                        ? "https://www.google.com/maps/search/?api=1&query=" + pickedLatitude + "," + pickedLongitude
                        : pickedGoogleMapsUrl);
            }
            cabin.setMaxCapacity(Integer.parseInt(capacityEditText.getText().toString().trim()));
            cabin.setRegularPrice(Double.parseDouble(priceEditText.getText().toString().trim()));
            cabin.setDiscount(parseDoubleOrZero(discountEditText.getText().toString().trim()));
            cabin.setDescription(descriptionEditText.getText().toString().trim());
            cabin.setImage(imageEditText.getText().toString().trim());
                cabin.setAmenities(selectedAmenityNames.isEmpty() ? "WiFi, Bếp, Phòng tắm riêng" : String.join(", ", selectedAmenityNames));
            if (cabin.getHostId() == null || cabin.getHostId().trim().isEmpty()) {
                cabin.setHostId(sessionManager.getUserId());
            }
        } catch (NumberFormatException exception) {
            statusTextView.setText("Giá, giảm giá và sức chứa phải là số hợp lệ.");
            return;
        }

        if (cabin.getName() == null || cabin.getName().trim().isEmpty()) {
            statusTextView.setText("Vui lòng nhập tên khách sạn.");
            return;
        }

        statusTextView.setText(editingCabin == null ? "Đang tạo khách sạn..." : "Đang cập nhật khách sạn...");
        if (editingCabin == null) {
            cabinService.createCabin(cabin, new SupabaseCallback<Cabin>() {
                @Override
                public void onSuccess(Cabin data) {
                    attachSelectedAmenities(data.getId());
                    returnToManageCabins("Đã tạo khách sạn");
                }

                @Override
                public void onError(String message) {
                    statusTextView.setText(message);
                }
            });
        } else {
            cabinService.updateCabin(cabin, new SupabaseCallback<Cabin>() {
                @Override
                public void onSuccess(Cabin data) {
                    returnToManageCabins("Đã cập nhật khách sạn");
                }

                @Override
                public void onError(String message) {
                    statusTextView.setText(message);
                }
            });
        }
    }

    private void attachSelectedAmenities(String createdCabinId) {
        for (String amenityId : selectedAmenityIds) {
            amenityService.addAmenityToCabin(createdCabinId, amenityId, new SupabaseCallback<CabinAmenity>() {
                @Override
                public void onSuccess(CabinAmenity data) {
                }

                @Override
                public void onError(String message) {
                    statusTextView.setText("Đã lưu khách sạn, nhưng có một tiện nghi chưa gắn được: " + message);
                }
            });
        }
    }

    private void returnToManageCabins(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, HostHotelDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private double parseDoubleOrZero(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        return Double.parseDouble(value);
    }
}



