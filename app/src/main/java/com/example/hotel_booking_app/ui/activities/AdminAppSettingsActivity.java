package com.example.hotel_booking_app.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Setting;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.SettingsService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.Locale;

public class AdminAppSettingsActivity extends AppCompatActivity {
    private EditText minBookingEditText;
    private EditText maxBookingEditText;
    private EditText maxGuestsEditText;
    private EditText breakfastPriceEditText;
    private TextView statusTextView;
    private SettingsService settingsService;
    private SessionManager sessionManager;
    private Setting currentSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_app_settings);

        settingsService = new SettingsService();
        sessionManager = new SessionManager(this);
        minBookingEditText = findViewById(R.id.edit_min_booking);
        maxBookingEditText = findViewById(R.id.edit_max_booking);
        maxGuestsEditText = findViewById(R.id.edit_max_guests);
        breakfastPriceEditText = findViewById(R.id.edit_breakfast_price);
        statusTextView = findViewById(R.id.text_status);

        Button backButton = findViewById(R.id.button_back);
        Button bottomBackButton = findViewById(R.id.button_back_bottom);
        Button saveButton = findViewById(R.id.button_save_settings);
        backButton.setOnClickListener(view -> finish());
        bottomBackButton.setOnClickListener(view -> finish());
        saveButton.setOnClickListener(view -> saveSettings());

        if (!sessionManager.isHostOrAdmin()) {
            statusTextView.setText("Chỉ tài khoản quản lý mới có thể cập nhật cài đặt hệ thống.");
            saveButton.setEnabled(false);
        }
        loadSettings();
    }

    private void loadSettings() {
        statusTextView.setText("Đang tải cài đặt...");
        settingsService.getSettings(new SupabaseCallback<Setting>() {
            @Override
            public void onSuccess(Setting setting) {
                currentSetting = setting;
                minBookingEditText.setText(String.valueOf(setting.getMiniBookingLength()));
                maxBookingEditText.setText(String.valueOf(setting.getMaxBookingLength()));
                maxGuestsEditText.setText(String.valueOf(setting.getMaxNumberOfGuests()));
                breakfastPriceEditText.setText(String.format(Locale.US, "%.2f", setting.getBreakfastPrice()));
                statusTextView.setText(setting.getId() == null
                        ? "Đã tải cài đặt mặc định. Hãy lưu một lần để ghi vào Supabase."
                        : "Cài đặt đã sẵn sàng.");
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void saveSettings() {
        try {
            int minBooking = Integer.parseInt(minBookingEditText.getText().toString().trim());
            int maxBooking = Integer.parseInt(maxBookingEditText.getText().toString().trim());
            int maxGuests = Integer.parseInt(maxGuestsEditText.getText().toString().trim());
            double breakfastPrice = Double.parseDouble(breakfastPriceEditText.getText().toString().trim());

            if (minBooking < 1 || maxBooking < minBooking || maxGuests < 1 || breakfastPrice < 0) {
                statusTextView.setText("Vui lòng kiểm tra số liệu: tối thiểu >= 1, tối đa >= tối thiểu, khách >= 1, bữa sáng >= 0.");
                return;
            }

            Setting setting = currentSetting == null ? new Setting() : currentSetting;
            setting.setMiniBookingLength(minBooking);
            setting.setMaxBookingLength(maxBooking);
            setting.setMaxNumberOfGuests(maxGuests);
            setting.setBreakfastPrice(breakfastPrice);

            statusTextView.setText("Đang lưu cài đặt...");
            settingsService.saveSettings(setting, new SupabaseCallback<Setting>() {
                @Override
                public void onSuccess(Setting data) {
                    currentSetting = data;
                    statusTextView.setText("Đã lưu cài đặt.");
                    Toast.makeText(AdminAppSettingsActivity.this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    statusTextView.setText(message);
                }
            });
        } catch (NumberFormatException exception) {
            statusTextView.setText("Vui lòng nhập số hợp lệ.");
        }
    }
}
