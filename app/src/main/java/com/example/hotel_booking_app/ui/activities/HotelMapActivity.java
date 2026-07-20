package com.example.hotel_booking_app.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Wishlist;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.WishlistService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HotelMapActivity extends AppCompatActivity {
    private WebView mapWebView;
    private TextView statusTextView;
    private TextView summaryTextView;
    private View selectedCard;
    private ImageView selectedImageView;
    private TextView selectedNameTextView;
    private TextView selectedLocationTextView;
    private TextView selectedPriceTextView;
    private TextView selectedMetaTextView;
    private TextView selectedDescriptionTextView;
    private Button favoriteButton;
    private String destination;
    private String focusCabinId;
    private List<Cabin> visibleCabins = new ArrayList<>();
    private final Set<String> favoriteCabinIds = new HashSet<>();
    private Cabin selectedCabin;
    private WishlistService wishlistService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_map);

        destination = getIntent().getStringExtra("destination");
        focusCabinId = getIntent().getStringExtra(AppConstants.EXTRA_CABIN_ID);
        mapWebView = findViewById(R.id.web_map);
        statusTextView = findViewById(R.id.text_map_status);
        summaryTextView = findViewById(R.id.text_map_summary);
        selectedCard = findViewById(R.id.card_selected_cabin);
        selectedImageView = findViewById(R.id.image_selected_cabin);
        selectedNameTextView = findViewById(R.id.text_selected_name);
        selectedLocationTextView = findViewById(R.id.text_selected_location);
        selectedPriceTextView = findViewById(R.id.text_selected_price);
        selectedMetaTextView = findViewById(R.id.text_selected_meta);
        selectedDescriptionTextView = findViewById(R.id.text_selected_description);
        favoriteButton = findViewById(R.id.button_map_favorite);
        wishlistService = new WishlistService();
        sessionManager = new SessionManager(this);

        Button backButton = findViewById(R.id.button_back);
        Button directionsButton = findViewById(R.id.button_directions);
        Button openDetailButton = findViewById(R.id.button_open_detail);

        backButton.setOnClickListener(view -> finish());
        directionsButton.setOnClickListener(view -> openSelectedInGoogleMaps());
        openDetailButton.setOnClickListener(view -> openSelectedDetail());
        favoriteButton.setOnClickListener(view -> toggleSelectedFavorite());
        selectedCard.setOnClickListener(view -> openSelectedDetail());

        summaryTextView.setText(safe(destination, "Ho Chi Minh City") + " - " + compactDateRange());
        setupWebView();
        loadMarkers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!visibleCabins.isEmpty()) {
            loadFavoriteState();
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebView() {
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        mapWebView.setWebViewClient(new WebViewClient());
        mapWebView.addJavascriptInterface(new MapBridge(), "HotelMap");
    }

    private void loadMarkers() {
        new CabinService().getCabins(new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                visibleCabins = filterCabins(cabins);
                if (visibleCabins.isEmpty()) {
                    statusTextView.setText("Chưa có chỗ nghỉ trên bản đồ");
                    selectedCard.setVisibility(View.GONE);
                    loadMapHtml();
                    return;
                }
                statusTextView.setText(formatCount(visibleCabins.size()) + " chỗ nghỉ trên bản đồ");
                selectedCabin = resolveInitialCabin();
                renderSelectedCabin();
                loadMapHtml();
                loadFavoriteState();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private List<Cabin> filterCabins(List<Cabin> cabins) {
        List<Cabin> filtered = new ArrayList<>();
        String city = normalizeCity(destination);
        for (Cabin cabin : cabins) {
            if (cabin.getLatitude() == 0 || cabin.getLongitude() == 0) {
                continue;
            }
            String haystack = (safe(cabin.getName(), "") + " " + safe(cabin.getLocation(), "")).toLowerCase(Locale.US);
            if (city.isEmpty() || haystack.contains(city)) {
                filtered.add(cabin);
            }
        }
        return filtered;
    }

    private Cabin resolveInitialCabin() {
        if (focusCabinId != null) {
            for (Cabin cabin : visibleCabins) {
                if (focusCabinId.equals(cabin.getId())) {
                    return cabin;
                }
            }
        }
        return visibleCabins.get(0);
    }

    private void renderSelectedCabin() {
        if (selectedCabin == null) {
            selectedCard.setVisibility(View.GONE);
            return;
        }
        selectedCard.setVisibility(View.VISIBLE);
        selectedNameTextView.setText(selectedCabin.getName());
        selectedLocationTextView.setText(safe(selectedCabin.getAddress(), selectedCabin.getLocation()));
        selectedPriceTextView.setText("Từ " + PriceUtils.formatUsd(price(selectedCabin)) + " / đêm");
        selectedMetaTextView.setText(buildSelectedMeta(selectedCabin));
        selectedDescriptionTextView.setText(safe(selectedCabin.getDescription(), "Chỗ nghỉ có vị trí thuận tiện, thông tin phòng rõ ràng và dễ đặt."));
        renderFavoriteButton();
        Glide.with(this)
                .load(selectedCabin.getImage())
                .centerCrop()
                .placeholder(R.drawable.bg_dark_card)
                .into(selectedImageView);
    }

    private void loadMapHtml() {
        mapWebView.loadDataWithBaseURL(
                "https://leafletjs.com/",
                buildMapHtml(),
                "text/html",
                "UTF-8",
                null
        );
    }

    private String buildMapHtml() {
        double centerLat = selectedCabin != null ? selectedCabin.getLatitude() : fallbackLat();
        double centerLng = selectedCabin != null ? selectedCabin.getLongitude() : fallbackLng();
        StringBuilder markers = new StringBuilder();
        for (Cabin cabin : visibleCabins) {
            markers.append("{")
                    .append("id:'").append(js(cabin.getId())).append("',")
                    .append("name:'").append(js(cabin.getName())).append("',")
                    .append("lat:").append(cabin.getLatitude()).append(",")
                    .append("lng:").append(cabin.getLongitude()).append(",")
                    .append("price:'").append(js(PriceUtils.formatUsd(price(cabin)))).append("'")
                    .append("},");
        }

        return "<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>"
                + "html,body,#map{height:100%;margin:0;background:#d8e4ed;font-family:Arial,sans-serif;}"
                + ".leaflet-control-attribution{font-size:10px;}"
                + ".leaflet-bottom.leaflet-right{bottom:258px;right:10px;}"
                + ".leaflet-control-zoom a{background:#202020;color:#fff;border-color:#444;}"
                + ".leaflet-control-layers{border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,.28);}"
                + ".price-marker{position:relative;background:#064ea8;color:#fff;border:2px solid #fff;border-radius:5px;"
                + "padding:6px 10px;font-weight:800;font-size:15px;box-shadow:0 3px 10px rgba(0,0,0,.32);white-space:nowrap;line-height:1;}"
                + ".price-marker.selected{background:#0b84ff;z-index:999;}"
                + ".price-marker:after{content:'';position:absolute;left:50%;bottom:-8px;transform:translateX(-50%);"
                + "border-left:7px solid transparent;border-right:7px solid transparent;border-top:8px solid #fff;}"
                + "</style></head><body><div id='map'></div><script>"
                + "var center=[" + centerLat + "," + centerLng + "];"
                + "var selectedZoom=" + selectedZoomForDestination() + ";"
                + "var cityZoom=" + zoomForDestination() + ";"
                + "var map=L.map('map',{zoomControl:false,preferCanvas:true,scrollWheelZoom:true,wheelPxPerZoomLevel:56}).setView(center,selectedZoom);"
                + "var light=L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{maxZoom:20,attribution:'&copy; OpenStreetMap &copy; CARTO'}).addTo(map);"
                + "var dark=L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',{maxZoom:20,attribution:'&copy; OpenStreetMap &copy; CARTO'});"
                + "L.control.zoom({position:'bottomright'}).addTo(map);"
                + "L.control.layers({'Clear map':light,'Night map':dark},null,{position:'bottomright'}).addTo(map);"
                + "var stays=[" + markers + "];var selected='" + js(selectedCabin == null ? "" : selectedCabin.getId()) + "';var bounds=[];"
                + "function markerHtml(stay){return '<div class=\"price-marker '+(stay.id===selected?'selected':'')+'\">'+stay.price+'</div>';}"
                + "function refreshMarkers(){stays.forEach(function(s){if(s._marker){s._marker.setIcon(L.divIcon({className:'',html:markerHtml(s),iconSize:null,iconAnchor:[40,34]}));}});}"
                + "function chooseStay(stay,animate){selected=stay.id;refreshMarkers();if(animate){map.setView([stay.lat,stay.lng],Math.max(map.getZoom(),selectedZoom),{animate:false});}HotelMap.selectCabin(stay.id);}"
                + "stays.forEach(function(stay){"
                + "var icon=L.divIcon({className:'',html:markerHtml(stay),iconSize:null,iconAnchor:[40,34]});"
                + "var marker=L.marker([stay.lat,stay.lng],{icon:icon}).addTo(map);"
                + "marker.on('click',function(){chooseStay(stay,true);});"
                + "stay._marker=marker;bounds.push([stay.lat,stay.lng]);"
                + "});"
                + "setTimeout(function(){map.invalidateSize();var active=stays.find(function(s){return s.id===selected;});"
                + "if(active){map.setView([active.lat,active.lng],selectedZoom);}else if(bounds.length>1){map.fitBounds(bounds,{padding:[58,58],maxZoom:cityZoom});}},150);"
                + "</script></body></html>";
    }

    private int zoomForDestination() {
        String city = normalizeCity(destination);
        if (city.contains("ho chi minh")) {
            return 13;
        }
        if (city.contains("vung tau")) {
            return 12;
        }
        if (city.contains("ha noi")) {
            return 12;
        }
        if (city.contains("da nang")) {
            return 12;
        }
        if (city.contains("da lat")) {
            return 13;
        }
        return 6;
    }

    private int selectedZoomForDestination() {
        String city = normalizeCity(destination);
        if (city.contains("ho chi minh") || city.contains("vung tau") || city.contains("ha noi")
                || city.contains("da nang") || city.contains("da lat")) {
            return 14;
        }
        return 13;
    }

    private double fallbackLat() {
        String city = normalizeCity(destination);
        if (city.contains("vung tau")) {
            return 10.4114;
        }
        if (city.contains("ha noi")) {
            return 21.0278;
        }
        if (city.contains("da nang")) {
            return 16.0471;
        }
        if (city.contains("da lat")) {
            return 11.9404;
        }
        return 10.7769;
    }

    private double fallbackLng() {
        String city = normalizeCity(destination);
        if (city.contains("vung tau")) {
            return 107.1362;
        }
        if (city.contains("ha noi")) {
            return 105.8342;
        }
        if (city.contains("da nang")) {
            return 108.2068;
        }
        if (city.contains("da lat")) {
            return 108.4583;
        }
        return 106.7009;
    }

    private void openSelectedDetail() {
        if (selectedCabin == null) {
            return;
        }
        Intent intent = new Intent(this, HotelDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_CABIN_ID, selectedCabin.getId());
        intent.putExtra("checkIn", getIntent().getStringExtra("checkIn"));
        intent.putExtra("checkOut", getIntent().getStringExtra("checkOut"));
        intent.putExtra("fromMap", true);
        startActivity(intent);
    }

    private void openSelectedInGoogleMaps() {
        if (selectedCabin == null) {
            return;
        }
        String query = selectedCabin.getLatitude() + "," + selectedCabin.getLongitude()
                + "(" + selectedCabin.getName() + ")";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) == null) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query="
                    + selectedCabin.getLatitude() + "," + selectedCabin.getLongitude()));
        }
        startActivity(intent);
    }

    private void loadFavoriteState() {
        favoriteCabinIds.clear();
        if (!sessionManager.isLoggedIn()) {
            renderFavoriteButton();
            return;
        }
        wishlistService.getWishlist(sessionManager.getUserId(), new SupabaseCallback<List<Wishlist>>() {
            @Override
            public void onSuccess(List<Wishlist> data) {
                favoriteCabinIds.clear();
                for (Wishlist wishlist : data) {
                    if (wishlist.getCabinId() != null) {
                        favoriteCabinIds.add(wishlist.getCabinId());
                    }
                }
                renderFavoriteButton();
            }

            @Override
            public void onError(String message) {
                renderFavoriteButton();
            }
        });
    }

    private void renderFavoriteButton() {
        if (favoriteButton == null || selectedCabin == null) {
            return;
        }
        boolean favorite = favoriteCabinIds.contains(selectedCabin.getId());
        favoriteButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline,
                0,
                0
        );
        favoriteButton.setContentDescription(favorite ? "Bỏ lưu chỗ nghỉ" : "Lưu chỗ nghỉ");
    }

    private void toggleSelectedFavorite() {
        if (selectedCabin == null) {
            return;
        }
        if (!sessionManager.isLoggedIn() || sessionManager.getAuthAccessToken() == null) {
            statusTextView.setText("Vui lòng đăng nhập lại để dùng mục yêu thích.");
            return;
        }
        String cabinId = selectedCabin.getId();
        boolean favorite = favoriteCabinIds.contains(cabinId);
        if (favorite) {
            favoriteCabinIds.remove(cabinId);
            renderFavoriteButton();
            wishlistService.removeFromWishlist(sessionManager.getUserId(), cabinId, new SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    statusTextView.setText("Đã bỏ lưu chỗ nghỉ");
                }

                @Override
                public void onError(String message) {
                    favoriteCabinIds.add(cabinId);
                    renderFavoriteButton();
                    statusTextView.setText(message);
                }
            });
            return;
        }
        favoriteCabinIds.add(cabinId);
        renderFavoriteButton();
        wishlistService.addToWishlist(sessionManager.getUserId(), cabinId, new SupabaseCallback<Wishlist>() {
            @Override
            public void onSuccess(Wishlist data) {
                statusTextView.setText("Đã lưu chỗ nghỉ");
            }

            @Override
            public void onError(String message) {
                favoriteCabinIds.remove(cabinId);
                renderFavoriteButton();
                statusTextView.setText(message);
            }
        });
    }

    private double price(Cabin cabin) {
        return PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount());
    }

    private String normalizeCity(String value) {
        String lower = safe(value, "").toLowerCase(Locale.US);
        if (lower.contains("ho chi minh") || lower.contains("hcm") || lower.contains("tp.")) {
            return "ho chi minh";
        }
        if (lower.contains("vung tau")) {
            return "vung tau";
        }
        if (lower.contains("ha noi") || lower.contains("hanoi")) {
            return "ha noi";
        }
        if (lower.contains("da nang") || lower.contains("danang")) {
            return "da nang";
        }
        if (lower.contains("da lat") || lower.contains("dalat")) {
            return "da lat";
        }
        return lower.trim();
    }

    private String buildSelectedMeta(Cabin cabin) {
        String type = translatePropertyType(safe(cabin.getPropertyType(), "Hotel"));
        StringBuilder builder = new StringBuilder(type);
        if (cabin.getStarRating() > 0) {
            builder.append(" - ").append(cabin.getStarRating()).append(" sao");
        }
        if (cabin.getReviewScore() > 0) {
            builder.append(" - ").append(String.format(Locale.US, "%.1f", cabin.getReviewScore()));
            if (cabin.getReviewCount() > 0) {
                builder.append(" (").append(formatCount(cabin.getReviewCount())).append(" đánh giá)");
            }
        }
        builder.append(" - Tối đa ").append(cabin.getMaxCapacity()).append(" khách");
        return builder.toString();
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
            return "";
        }
    }

    private String formatCount(int value) {
        return String.format(Locale.US, "%,d", value).replace(",", ".");
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String translatePropertyType(String value) {
        if ("Resort".equalsIgnoreCase(value)) {
            return "Khu nghỉ dưỡng";
        }
        if ("Hotel".equalsIgnoreCase(value)) {
            return "Khách sạn";
        }
        return value;
    }

    private String js(String value) {
        return safe(value, "")
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ");
    }

    private class MapBridge {
        @JavascriptInterface
        public void selectCabin(String id) {
            runOnUiThread(() -> {
                for (Cabin cabin : visibleCabins) {
                    if (cabin.getId().equals(id)) {
                        selectedCabin = cabin;
                        renderSelectedCabin();
                        return;
                    }
                }
            });
        }
    }
}
