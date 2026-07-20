package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchMapActivity extends AppCompatActivity {
    private FrameLayout mapCanvas;
    private TextView statusTextView;
    private TextView summaryTextView;
    private String destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_map);

        destination = getIntent().getStringExtra("destination");
        mapCanvas = findViewById(R.id.map_canvas);
        statusTextView = findViewById(R.id.text_map_status);
        summaryTextView = findViewById(R.id.text_map_summary);
        Button backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(view -> finish());

        summaryTextView.setText(safe(destination, "TP. Ho Chi Minh") + " · " + compactDateRange());
        loadMarkers();
    }

    private void loadMarkers() {
        new CabinService().getCabins(new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                List<Cabin> filtered = filterCabins(cabins);
                statusTextView.setText(filtered.size() + " chỗ nghỉ trên bản đồ");
                mapCanvas.post(() -> renderMarkers(filtered));
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private List<Cabin> filterCabins(List<Cabin> cabins) {
        List<Cabin> filtered = new ArrayList<>();
        String needle = normalizeCity(destination);
        for (Cabin cabin : cabins) {
            String haystack = (safe(cabin.getName(), "") + " " + safe(cabin.getLocation(), "")).toLowerCase(Locale.US);
            if (needle.isEmpty() || haystack.contains(needle)) {
                filtered.add(cabin);
            }
        }
        return filtered;
    }

    private void renderMarkers(List<Cabin> cabins) {
        int width = Math.max(1, mapCanvas.getWidth());
        int height = Math.max(1, mapCanvas.getHeight());
        mapCanvas.removeAllViews();

        ImageView mapImage = new ImageView(this);
        mapImage.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        mapImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mapImage.setImageResource(R.drawable.vietnam_official_map);
        mapCanvas.addView(mapImage);

        View overlay = new View(this);
        overlay.setBackgroundColor(0x1A0F2233);
        mapCanvas.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        addIslandLabel("Hoang Sa", width * 0.76f, height * 0.44f);
        addIslandLabel("Truong Sa", width * 0.70f, height * 0.70f);

        for (int i = 0; i < cabins.size(); i++) {
            Cabin cabin = cabins.get(i);
            TextView marker = new TextView(this);
            marker.setText(PriceUtils.formatVnd(PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount())));
            marker.setTextColor(getColor(R.color.white));
            marker.setTextSize(12);
            marker.setTypeface(null, android.graphics.Typeface.BOLD);
            marker.setGravity(Gravity.CENTER);
            marker.setBackgroundResource(R.drawable.bg_map_marker);
            marker.setSingleLine(true);
            marker.setOnClickListener(view -> {
                Intent intent = new Intent(this, CabinDetailActivity.class);
                intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabin.getId());
                startActivity(intent);
            });

            int left = Math.round(projectLongitude(cabin.getLongitude(), width));
            int top = Math.round(projectLatitude(cabin.getLatitude(), height));
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(34)
            );
            params.leftMargin = Math.max(8, Math.min(width - dp(116), left));
            params.topMargin = Math.max(dp(42), Math.min(height - dp(52), top));
            mapCanvas.addView(marker, params);
        }
    }

    private float projectLongitude(double longitude, int width) {
        if (longitude <= 0) {
            return width * 0.52f;
        }
        double minLon = 102.0;
        double maxLon = 110.6;
        double normalized = (longitude - minLon) / (maxLon - minLon);
        return (float) (width * (0.10 + clamp(normalized, 0, 1) * 0.78));
    }

    private float projectLatitude(double latitude, int height) {
        if (latitude <= 0) {
            return height * 0.62f;
        }
        double minLat = 8.0;
        double maxLat = 23.6;
        double normalized = (maxLat - latitude) / (maxLat - minLat);
        return (float) (height * (0.06 + clamp(normalized, 0, 1) * 0.88));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void addIslandLabel(String label, float x, float y) {
        TextView island = new TextView(this);
        island.setText(label);
        island.setTextColor(0xEEFFFFFF);
        island.setTextSize(11);
        island.setTypeface(null, android.graphics.Typeface.BOLD);
        island.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(92), dp(32));
        params.leftMargin = Math.round(x - dp(46));
        params.topMargin = Math.round(y);
        mapCanvas.addView(island, params);
    }

    private String normalizeCity(String value) {
        String lower = safe(value, "").toLowerCase(Locale.US);
        if (lower.contains("ho chi minh") || lower.contains("hcm")) {
            return "ho chi minh";
        }
        if (lower.contains("vung tau")) {
            return "vung tau";
        }
        if (lower.contains("ha noi")) {
            return "ha noi";
        }
        return lower.trim();
    }

    private String compactDateRange() {
        String checkIn = formatShortDate(getIntent().getStringExtra("checkIn"));
        String checkOut = formatShortDate(getIntent().getStringExtra("checkOut"));
        if (checkIn.isEmpty() || checkOut.isEmpty()) {
            return "Chọn ngày";
        }
        return checkIn + " - " + checkOut;
    }

    private String formatShortDate(String isoDate) {
        try {
            return LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("MMM d", Locale.US));
        } catch (Exception e) {
            return "Chọn ngày";
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
