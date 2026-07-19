package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Amenity;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.CabinAmenity;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.AmenityService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class AdminCabinFormActivity extends AppCompatActivity {
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
    private LinearLayout amenitiesContainer;
    private Button saveButton;
    private CabinService cabinService;
    private AmenityService amenityService;
    private SessionManager sessionManager;
    private Cabin editingCabin;
    private String cabinId;
    private final List<Amenity> allAmenities = new ArrayList<>();
    private final List<String> selectedAmenityIds = new ArrayList<>();
    private final List<String> selectedAmenityNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_cabin_form);

        cabinService = new CabinService();
        amenityService = new AmenityService();
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
        amenitiesContainer = findViewById(R.id.container_amenities);
        saveButton = findViewById(R.id.button_save_cabin);
        Button pickImageButton = findViewById(R.id.button_pick_image);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);

        boolean editMode = cabinId != null && !cabinId.trim().isEmpty();
        formTitleTextView.setText(editMode ? "Edit Cabin" : "Create New Cabin");
        saveButton.setText(editMode ? "Update Cabin" : "Create Cabin");

        pickImageButton.setOnClickListener(view -> openImagePicker());
        saveButton.setOnClickListener(view -> saveCabin());
        bottomBackButton.setOnClickListener(view -> finish());

        loadAmenities();
        if (editMode) {
            loadCabin();
        } else {
            statusTextView.setText("Create a cabin for Serein Stay.");
        }
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
                statusTextView.setText("Could not load amenities: " + message);
            }
        });
    }

    private void loadCabin() {
        statusTextView.setText("Loading cabin...");
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
                statusTextView.setText("Cabin ready.");
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
            cabin.setAmenities(selectedAmenityNames.isEmpty() ? "WiFi, Kitchen, Private Bathroom" : String.join(", ", selectedAmenityNames));
            if (cabin.getHostId() == null || cabin.getHostId().trim().isEmpty()) {
                cabin.setHostId(sessionManager.getUserId());
            }
        } catch (NumberFormatException exception) {
            statusTextView.setText("Price, discount and capacity must be valid numbers.");
            return;
        }

        if (cabin.getName() == null || cabin.getName().trim().isEmpty()) {
            statusTextView.setText("Cabin name is required.");
            return;
        }

        statusTextView.setText(editingCabin == null ? "Creating cabin..." : "Updating cabin...");
        if (editingCabin == null) {
            cabinService.createCabin(cabin, new SupabaseCallback<Cabin>() {
                @Override
                public void onSuccess(Cabin data) {
                    attachSelectedAmenities(data.getId());
                    returnToManageCabins("Cabin created");
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
                    returnToManageCabins("Cabin updated");
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
                    statusTextView.setText("Cabin saved, but one amenity could not be attached: " + message);
                }
            });
        }
    }

    private void returnToManageCabins(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, HostDashboardActivity.class);
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
