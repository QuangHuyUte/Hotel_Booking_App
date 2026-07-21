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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Amenity;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.CabinAmenity;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AmenityService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.utils.SessionManager;

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
    private TextView hotelRoomSummaryTextView;
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
    private EditText roomImageEditText;
    private Spinner roomCategorySpinner;
    private Spinner bedTypeSpinner;
    private LinearLayout hotelFieldsSection;
    private LinearLayout roomFieldsSection;
    private LinearLayout roomEditorSection;
    private LinearLayout amenitiesContainer;
    private LinearLayout roomAmenitiesContainer;
    private LinearLayout roomTypesContainer;
    private LinearLayout roomFiltersContainer;
    private ScrollView formScrollView;
    private TextView roomEditorTitleTextView;
    private TextView roomEditorHintTextView;
    private final List<RoomType> loadedRoomTypes = new ArrayList<>();
    private Button saveButton;
    private Button saveRoomTypeButton;
    private Button pickLocationButton;
    private Button manageRoomUnitsButton;
    private CabinService cabinService;
    private AmenityService amenityService;
    private RoomTypeService roomTypeService;
    private SessionManager sessionManager;
    private Cabin editingCabin;
    private RoomType editingRoomType;
    private String cabinId;
    private String pendingRoomTypeId;
    private boolean roomMode;
    private final List<Amenity> allAmenities = new ArrayList<>();
    private final List<String> selectedAmenityIds = new ArrayList<>();
    private final List<String> selectedAmenityNames = new ArrayList<>();
    private final List<String> selectedRoomAmenityNames = new ArrayList<>();
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
        sessionManager = new SessionManager(this);
        cabinId = getIntent().getStringExtra(EXTRA_CABIN_ID);
        pendingRoomTypeId = getIntent().getStringExtra(EXTRA_ROOM_TYPE_ID);
        roomMode = getIntent().getBooleanExtra(EXTRA_ROOM_MODE, false);

        formTitleTextView = findViewById(R.id.text_form_title);
        statusTextView = findViewById(R.id.text_status);
        coordinatesTextView = findViewById(R.id.text_location_coordinates);
        hotelRoomSummaryTextView = findViewById(R.id.text_hotel_room_summary);
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
        roomImageEditText = findViewById(R.id.edit_room_image);
        roomCategorySpinner = findViewById(R.id.spinner_room_category);
        bedTypeSpinner = findViewById(R.id.spinner_bed_type);
        hotelFieldsSection = findViewById(R.id.section_hotel_fields);
        roomFieldsSection = findViewById(R.id.section_room_fields);
        roomEditorSection = findViewById(R.id.section_room_editor);
        amenitiesContainer = findViewById(R.id.container_amenities);
        roomAmenitiesContainer = findViewById(R.id.container_room_amenities);
        roomFiltersContainer = findViewById(R.id.container_room_filters);
        roomTypesContainer = findViewById(R.id.container_room_types);
        formScrollView = findViewById(R.id.scroll_form);
        roomEditorTitleTextView = findViewById(R.id.text_room_editor_title);
        roomEditorHintTextView = findViewById(R.id.text_room_editor_hint);
        saveButton = findViewById(R.id.button_save_cabin);
        saveRoomTypeButton = findViewById(R.id.button_save_room_type);
        pickLocationButton = findViewById(R.id.button_pick_location);
        manageRoomUnitsButton = findViewById(R.id.button_manage_room_units);
        Button pickImageButton = findViewById(R.id.button_pick_image);
        View topBackButton = findViewById(R.id.button_back);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);

        boolean editMode = cabinId != null && !cabinId.trim().isEmpty();
        formTitleTextView.setText(editMode ? "Sửa khách sạn" : "Tạo khách sạn mới");
        saveButton.setText(editMode ? "Cập nhật khách sạn" : "Tạo khách sạn");

        pickImageButton.setOnClickListener(view -> openImagePicker());
        pickLocationButton.setOnClickListener(view -> openLocationPicker());
        saveButton.setOnClickListener(view -> saveCabin());
        saveRoomTypeButton.setOnClickListener(view -> saveRoomType());
        manageRoomUnitsButton.setOnClickListener(view -> openRoomUnitManagement());
        manageRoomUnitsButton.setTextColor(getColor(R.color.ink));
        saveRoomTypeButton.setTextColor(getColor(R.color.black));
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
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                new String[]{"Standard", "Superior", "Deluxe", "Suite"}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getColor(R.color.ink));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getColor(R.color.black));
                }
                return view;
            }
        };
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roomCategorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<String> bedAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                new String[]{"Single", "Double", "Queen", "King"}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getColor(R.color.ink));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getColor(R.color.black));
                }
                return view;
            }
        };
        bedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bedTypeSpinner.setAdapter(bedAdapter);
    }

    private void applyMode() {
        boolean roomEditing = isRoomMode();
        hotelFieldsSection.setVisibility(roomEditing ? View.GONE : View.VISIBLE);
        roomFieldsSection.setVisibility(roomEditing ? View.VISIBLE : View.GONE);
        saveButton.setVisibility(roomEditing ? View.GONE : View.VISIBLE);
        if (hotelRoomSummaryTextView != null) {
            hotelRoomSummaryTextView.setVisibility(!roomEditing && cabinId != null && !cabinId.trim().isEmpty()
                    ? View.VISIBLE
                    : View.GONE);
        }
        if (!roomEditing) {
            formTitleTextView.setText(cabinId == null || cabinId.trim().isEmpty()
                    ? "Tạo khách sạn mới"
                    : "Sửa khách sạn");
            saveButton.setText(cabinId == null || cabinId.trim().isEmpty()
                    ? "Tạo khách sạn"
                    : "Cập nhật khách sạn");
            roomEditorSection.setVisibility(View.GONE);
            return;
        }
        formTitleTextView.setText("Quản lý loại phòng");
        roomEditorSection.setVisibility(View.VISIBLE);
        roomEditorTitleTextView.setText(pendingRoomTypeId == null || pendingRoomTypeId.trim().isEmpty()
                ? "Thêm loại phòng mới"
                : "Đang mở loại phòng");
        roomEditorHintTextView.setText(pendingRoomTypeId == null || pendingRoomTypeId.trim().isEmpty()
                ? "Loại phòng mới cần đủ cấu hình phòng, tiện nghi, ảnh và giá."
                : "Loại phòng đã có chỉ sửa giá, tiện nghi và ảnh. Thông số cấu trúc được khóa để giữ đúng tồn kho.");
        statusTextView.setText("Chọn một loại phòng bên dưới để sửa, hoặc nhập cấu hình để thêm loại phòng mới.");
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
            coordinatesTextView.setText("Chưa chọn tọa độ bản đồ.");
            return;
        }
        coordinatesTextView.setText(String.format(Locale.US,
                "Tọa độ: %.6f, %.6f",
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
                syncSelectedRoomAmenitiesFromRoomType();
                renderRoomAmenityCheckboxes();
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
                    loadHotelRoomSummary();
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

    private void renderRoomAmenityCheckboxes() {
        if (roomAmenitiesContainer == null) {
            return;
        }
        roomAmenitiesContainer.removeAllViews();
        if (allAmenities.isEmpty()) {
            roomAmenitiesContainer.addView(roomHint("Đang tải tiện nghi..."));
            return;
        }
        for (Amenity amenity : allAmenities) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(amenity.getName());
            checkBox.setTextColor(getColor(R.color.ink));
            checkBox.setChecked(selectedRoomAmenityNames.contains(amenity.getName()));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedRoomAmenityNames.contains(amenity.getName())) {
                        selectedRoomAmenityNames.add(amenity.getName());
                    }
                } else {
                    selectedRoomAmenityNames.remove(amenity.getName());
                }
            });
            roomAmenitiesContainer.addView(checkBox);
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

    private void syncSelectedRoomAmenitiesFromRoomType() {
        selectedRoomAmenityNames.clear();
        String amenitiesSource = editingRoomType != null && editingRoomType.getAmenities() != null
                ? editingRoomType.getAmenities()
                : editingCabin != null ? editingCabin.getAmenities() : "";
        if (amenitiesSource == null || amenitiesSource.trim().isEmpty() || allAmenities.isEmpty()) {
            return;
        }
        for (Amenity amenity : allAmenities) {
            if (amenitiesSource.toLowerCase(Locale.US).contains(amenity.getName().toLowerCase(Locale.US))) {
                selectedRoomAmenityNames.add(amenity.getName());
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

    private void loadHotelRoomSummary() {
        if (hotelRoomSummaryTextView == null || editingCabin == null || editingCabin.getId() == null) {
            return;
        }
        hotelRoomSummaryTextView.setVisibility(View.VISIBLE);
        hotelRoomSummaryTextView.setText("Đang tải tổng quan loại phòng...");
        roomTypeService.getRoomTypesForCabin(editingCabin.getId(), new SupabaseCallback<List<RoomType>>() {
            @Override
            public void onSuccess(List<RoomType> roomTypes) {
                int activeTypes = 0;
                int totalRooms = 0;
                double totalBasePrice = 0;
                if (roomTypes != null) {
                    for (RoomType roomType : roomTypes) {
                        if (roomType == null || !roomType.isActive()) {
                            continue;
                        }
                        activeTypes++;
                        totalRooms += Math.max(0, roomType.getTotalRooms());
                        totalBasePrice += roomType.getBasePrice();
                    }
                }
                if (activeTypes == 0) {
                    hotelRoomSummaryTextView.setText("Chưa có loại phòng. Vào tab hotel rồi bấm + Phòng để thêm.");
                    return;
                }
                double averagePrice = totalBasePrice / activeTypes;
                hotelRoomSummaryTextView.setText(String.format(Locale.US,
                        "%d loại phòng đang hoạt động · %d phòng theo loại · giá phòng trung bình $%.2f",
                        activeTypes,
                        totalRooms,
                        averagePrice));
            }

            @Override
            public void onError(String message) {
                hotelRoomSummaryTextView.setText("Không tải được tổng quan phòng: " + message);
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
        if (isRoomMode()) {
            roomTypesContainer.setVisibility(View.GONE);
            if (!selectPendingRoomType(loadedRoomTypes)) {
                clearRoomForm();
                roomEditorSection.setVisibility(View.VISIBLE);
                statusTextView.setText("Nhập thông tin để thêm loại phòng mới cho khách sạn này.");
            }
            return;
        }

        List<RoomType> visibleRoomTypes = new ArrayList<>(loadedRoomTypes);
        if (loadedRoomTypes.isEmpty()) {
            roomTypesContainer.addView(roomHint("Chưa có loại phòng. Thêm Standard, Superior, Deluxe hoặc Suite bên dưới."));
            return;
        }
        for (RoomType roomType : visibleRoomTypes) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(R.drawable.bg_manager_search);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));

            TextView summary = roomHint(roomType.titleLabel()
                    + " · " + roomType.categoryLabel()
                    + " · " + roomType.sizeLabel()
                    + " · " + roomType.bedLabel()
                    + "\n" + roomType.getTotalRooms() + " phòng · tối đa "
                    + roomType.effectiveMaxAdults() + " người lớn · "
                    + roomType.effectiveBedCount() + " giường · $" + roomType.getBasePrice());
            row.addView(summary);
            summary.setText(roomType.titleLabel()
                    + " · " + roomType.categoryLabel()
                    + "\n" + roomType.getTotalRooms() + " phòng cùng loại · tối đa "
                    + roomType.effectiveMaxAdults() + " người lớn · "
                    + roomType.effectiveBedCount() + " giường · " + roomType.sizeLabel()
                    + "\nGiá cơ bản: $" + roomType.getBasePrice());
            summary.setTextColor(getColor(R.color.ink));
            row.setOnClickListener(view -> fillRoomForm(roomType));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button edit = new Button(this);
            edit.setText("Sửa");
            edit.setAllCaps(false);
            edit.setBackgroundResource(R.drawable.bg_button_secondary);
            edit.setTextColor(getColor(R.color.black));
            edit.setOnClickListener(view -> fillRoomForm(roomType));
            Button delete = new Button(this);
            delete.setAllCaps(false);
            delete.setText("Ẩn loại");
            delete.setBackgroundResource(R.drawable.bg_manager_search);
            delete.setTextColor(getColor(R.color.ink));
            delete.setOnClickListener(view -> deleteRoomType(roomType));
            Button manage = new Button(this);
            manage.setAllCaps(false);
            manage.setText("Phòng");
            manage.setBackgroundResource(R.drawable.bg_button_secondary);
            manage.setTextColor(getColor(R.color.black));
            manage.setOnClickListener(view -> {
                editingRoomType = roomType;
                openRoomUnitManagement();
            });
            actions.addView(edit, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            actions.addView(manage, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            actions.addView(delete, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(actions);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = 10;
            roomTypesContainer.addView(row, params);
        }
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
        roomFiltersContainer.setVisibility(View.GONE);
    }

    private boolean selectPendingRoomType(List<RoomType> roomTypes) {
        if (pendingRoomTypeId == null || pendingRoomTypeId.trim().isEmpty() || roomTypes == null) {
            return false;
        }
        for (RoomType roomType : roomTypes) {
            if (roomType != null && pendingRoomTypeId.equals(roomType.getId())) {
                fillRoomForm(roomType);
                statusTextView.setText("Đang sửa loại phòng: " + roomType.titleLabel());
                pendingRoomTypeId = null;
                return true;
            }
        }
        return false;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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
        roomImageEditText.setText(safe(roomType.getImage()));
        syncSelectedRoomAmenitiesFromRoomType();
        renderRoomAmenityCheckboxes();
        roomEditorSection.setVisibility(View.VISIBLE);
        roomEditorTitleTextView.setText("Sửa " + roomType.titleLabel());
        roomEditorHintTextView.setText("Đang quản lý " + roomType.getTotalRooms()
                + " phòng thuộc loại này. Chỉ sửa giá, tiện nghi và ảnh; bấm quản lý phòng để xem lịch từng phòng.");
        saveRoomTypeButton.setText("Cập nhật loại phòng");
        setRoomStructureEditable(false);
        if (formScrollView != null) {
            formScrollView.post(() -> formScrollView.smoothScrollTo(0, roomEditorSection.getTop()));
        }
    }

    private void openRoomUnitManagement() {
        if (editingCabin == null || editingCabin.getId() == null || editingRoomType == null || editingRoomType.getId() == null) {
            statusTextView.setText("Chọn và lưu loại phòng trước khi quản lý từng phòng.");
            return;
        }
        Intent intent = new Intent(this, RoomUnitManagementActivity.class);
        intent.putExtra(RoomUnitManagementActivity.EXTRA_CABIN_ID, editingCabin.getId());
        intent.putExtra(RoomUnitManagementActivity.EXTRA_ROOM_TYPE_ID, editingRoomType.getId());
        startActivity(intent);
    }

    private void saveRoomType() {
        if (editingCabin == null || editingCabin.getId() == null) {
            statusTextView.setText("Lưu khách sạn trước khi thêm loại phòng.");
            return;
        }
        RoomType roomType = editingRoomType == null ? new RoomType() : editingRoomType;
        try {
            boolean editingExisting = editingRoomType != null;
            String category = editingExisting ? roomType.getCategory() : String.valueOf(roomCategorySpinner.getSelectedItem());
            int sizeM2 = editingExisting ? roomType.getSizeM2() : Integer.parseInt(roomSizeEditText.getText().toString().trim());
            String bedType = editingExisting ? roomType.getBedType() : String.valueOf(bedTypeSpinner.getSelectedItem());
            int maxAdults = editingExisting ? roomType.effectiveMaxAdults() : Integer.parseInt(roomGuestsEditText.getText().toString().trim());
            int bedCount = editingExisting ? roomType.effectiveBedCount() : Integer.parseInt(roomBedCountEditText.getText().toString().trim());
            int totalRooms = editingExisting ? roomType.getTotalRooms() : Integer.parseInt(roomTotalEditText.getText().toString().trim());
            int roomLimit = maxRoomsForCategory(category);
            int adultLimit = maxAdultsForCategory(category);
            if (totalRooms < 1 || totalRooms > roomLimit) {
                statusTextView.setText("Số phòng " + category + " phải từ 1 đến " + roomLimit + ".");
                return;
            }
            if (maxAdults < 1 || maxAdults > adultLimit) {
                statusTextView.setText("Sức chứa " + category + " tối đa " + adultLimit + " người lớn.");
                return;
            }
            if (bedCount < 1 || bedCount > Math.max(1, adultLimit)) {
                statusTextView.setText("Số giường phải từ 1 đến " + Math.max(1, adultLimit) + ".");
                return;
            }
            String roomName = editingExisting ? roomType.getName() : roomNameEditText.getText().toString().trim();
            roomType.setCabinId(editingCabin.getId());
            roomType.setCategory(category);
            roomType.setName(roomName.isEmpty()
                    ? category + " " + bedType + " Room"
                    : roomName);
            roomType.setDescription(category + " tại " + editingCabin.getName());
            roomType.setTotalRooms(totalRooms);
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
            String roomImage = roomImageEditText.getText().toString().trim();
            String roomAmenities = selectedRoomAmenityNames.isEmpty()
                    ? editingCabin.getAmenities()
                    : String.join(", ", selectedRoomAmenityNames);
            roomType.setAmenities(roomAmenities);
            roomType.setImage(roomImage.isEmpty() ? editingCabin.getImage() : roomImage);
            roomType.setActive(true);
            if (!roomType.fitsRoomSizeForGuests(maxAdults)) {
                statusTextView.setText("Diện tích phòng chưa đủ cho số khách này.");
                return;
            }
        } catch (NumberFormatException exception) {
            statusTextView.setText(editingRoomType != null
                    ? "Giá mỗi đêm phải là số hợp lệ."
                    : "Số phòng, người lớn, giường, diện tích và giá phải là số hợp lệ.");
            return;
        }

        SupabaseCallback<RoomType> callback = new SupabaseCallback<RoomType>() {
            @Override
            public void onSuccess(RoomType data) {
                editingRoomType = data;
                pendingRoomTypeId = data == null ? null : data.getId();
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

    private void setRoomStructureEditable(boolean editable) {
        setReadOnly(roomNameEditText, editable);
        setReadOnly(roomTotalEditText, editable);
        setReadOnly(roomGuestsEditText, editable);
        setReadOnly(roomSizeEditText, editable);
        setReadOnly(roomBedCountEditText, editable);
        roomCategorySpinner.setEnabled(editable);
        roomCategorySpinner.setAlpha(1f);
        bedTypeSpinner.setEnabled(editable);
        bedTypeSpinner.setAlpha(1f);
    }

    private void setReadOnly(EditText editText, boolean editable) {
        editText.setFocusable(editable);
        editText.setFocusableInTouchMode(editable);
        editText.setCursorVisible(editable);
        editText.setLongClickable(editable);
        editText.setTextColor(getColor(R.color.ink));
        editText.setAlpha(1f);
    }

    private void clearRoomForm() {
        editingRoomType = null;
        setRoomStructureEditable(true);
        roomNameEditText.setText("");
        roomTotalEditText.setText("");
        roomGuestsEditText.setText("");
        roomSizeEditText.setText("");
        roomBedCountEditText.setText("");
        roomPriceEditText.setText("");
        roomImageEditText.setText("");
        syncSelectedRoomAmenitiesFromRoomType();
        renderRoomAmenityCheckboxes();
        roomEditorTitleTextView.setText("Thêm loại phòng mới");
        roomEditorHintTextView.setText("Loại phòng kế thừa địa chỉ của khách sạn. Chỉ chỉnh số phòng cùng loại, sức chứa, giường, ảnh, tiện nghi và giá.");
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

    private int maxRoomsForCategory(String category) {
        if ("Standard".equalsIgnoreCase(category)) {
            return 10;
        }
        if ("Superior".equalsIgnoreCase(category)) {
            return 8;
        }
        if ("Deluxe".equalsIgnoreCase(category)) {
            return 6;
        }
        if ("Suite".equalsIgnoreCase(category)) {
            return 4;
        }
        return 2;
    }

    private int maxAdultsForCategory(String category) {
        if ("Standard".equalsIgnoreCase(category) || "Superior".equalsIgnoreCase(category)) {
            return 2;
        }
        if ("Deluxe".equalsIgnoreCase(category)) {
            return 3;
        }
        if ("Suite".equalsIgnoreCase(category)) {
            return 4;
        }
        return 2;
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



