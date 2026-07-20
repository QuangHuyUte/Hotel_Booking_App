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

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Amenity;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.CabinAmenity;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AmenityService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class AdminHotelFormActivity extends AppCompatActivity {
    public static final String EXTRA_CABIN_ID = "extra_admin_cabin_id";
    private static final int REQUEST_PICK_IMAGE = 2001;

    private TextView formTitleTextView;
    private TextView statusTextView;
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
    private Spinner roomCategorySpinner;
    private Spinner bedTypeSpinner;
    private LinearLayout amenitiesContainer;
    private LinearLayout roomTypesContainer;
    private Button saveButton;
    private Button saveRoomTypeButton;
    private CabinService cabinService;
    private AmenityService amenityService;
    private RoomTypeService roomTypeService;
    private SessionManager sessionManager;
    private Cabin editingCabin;
    private RoomType editingRoomType;
    private String cabinId;
    private final List<Amenity> allAmenities = new ArrayList<>();
    private final List<String> selectedAmenityIds = new ArrayList<>();
    private final List<String> selectedAmenityNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_hotel_form);

        cabinService = new CabinService();
        amenityService = new AmenityService();
        roomTypeService = new RoomTypeService();
        sessionManager = new SessionManager(this);
        cabinId = getIntent().getStringExtra(EXTRA_CABIN_ID);

        formTitleTextView = findViewById(R.id.text_form_title);
        statusTextView = findViewById(R.id.text_status);
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
        roomCategorySpinner = findViewById(R.id.spinner_room_category);
        bedTypeSpinner = findViewById(R.id.spinner_bed_type);
        amenitiesContainer = findViewById(R.id.container_amenities);
        roomTypesContainer = findViewById(R.id.container_room_types);
        saveButton = findViewById(R.id.button_save_cabin);
        saveRoomTypeButton = findViewById(R.id.button_save_room_type);
        Button pickImageButton = findViewById(R.id.button_pick_image);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);

        boolean editMode = cabinId != null && !cabinId.trim().isEmpty();
        formTitleTextView.setText(editMode ? "Sửa khách sạn" : "Tạo khách sạn mới");
        saveButton.setText(editMode ? "Update Hotel" : "Create Hotel");

        pickImageButton.setOnClickListener(view -> openImagePicker());
        saveButton.setOnClickListener(view -> saveCabin());
        saveRoomTypeButton.setOnClickListener(view -> saveRoomType());
        bottomBackButton.setOnClickListener(view -> finish());
        setupRoomSpinners();

        loadAmenities();
        if (editMode) {
            loadCabin();
        } else {
            statusTextView.setText("Create a hotel for Serein Stay.");
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
                editingCabin = cabin;
                nameEditText.setText(cabin.getName());
                locationEditText.setText(cabin.getLocation());
                capacityEditText.setText(String.valueOf(cabin.getMaxCapacity()));
                priceEditText.setText(String.valueOf(cabin.getRegularPrice()));
                discountEditText.setText(String.valueOf(cabin.getDiscount()));
                descriptionEditText.setText(cabin.getDescription());
                imageEditText.setText(cabin.getImage());
                syncSelectedAmenitiesFromCabin();
                renderAmenityCheckboxes();
                loadRoomTypes();
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
        roomTypesContainer.removeAllViews();
        if (editingCabin == null || editingCabin.getId() == null) {
            TextView hint = roomHint("Lưu khách sạn trước, sau đó thêm loại phòng.");
            roomTypesContainer.addView(hint);
            return;
        }
        if (roomTypes == null || roomTypes.isEmpty()) {
            roomTypesContainer.addView(roomHint("Chưa có loại phòng. Thêm Standard, Superior, Deluxe hoặc Suite bên dưới."));
            return;
        }
        for (RoomType roomType : roomTypes) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(R.drawable.bg_status);
            row.setPadding(12, 12, 12, 12);

        TextView summary = roomHint(roomType.displayName()
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
            actions.addView(edit, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
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
        setSpinnerSelection(roomCategorySpinner, roomType.displayName());
        setSpinnerSelection(bedTypeSpinner, roomType.getBedType());
        roomNameEditText.setText(roomType.getName());
        roomTotalEditText.setText(String.valueOf(roomType.getTotalRooms()));
        roomGuestsEditText.setText(String.valueOf(roomType.effectiveMaxAdults()));
        roomSizeEditText.setText(String.valueOf(roomType.getSizeM2()));
        roomBedCountEditText.setText(String.valueOf(roomType.effectiveBedCount()));
        roomPriceEditText.setText(String.valueOf(roomType.getBasePrice()));
        saveRoomTypeButton.setText("Cập nhật loại phòng");
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
            roomType.setCabinId(editingCabin.getId());
            roomType.setCategory(category);
            roomType.setName(roomNameEditText.getText().toString().trim().isEmpty()
                    ? category + " Room"
                    : roomNameEditText.getText().toString().trim());
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
                statusTextView.setText("Đã xóa loại phòng.");
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
        return count + " " + bedType + " giường";
    }

    private void saveCabin() {
        Cabin cabin = editingCabin == null ? new Cabin() : editingCabin;
        try {
            cabin.setName(nameEditText.getText().toString().trim());
            cabin.setLocation(locationEditText.getText().toString().trim());
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
