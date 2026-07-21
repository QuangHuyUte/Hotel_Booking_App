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
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.models.Wishlist;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.BookingService;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.RoomTypeService;
import com.example.hotel_booking_app.services.WishlistService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HotelMapActivity extends AppCompatActivity {
    public static final String EXTRA_PICK_LOCATION = "extra_pick_location";
    public static final String EXTRA_PICK_LATITUDE = "extra_pick_latitude";
    public static final String EXTRA_PICK_LONGITUDE = "extra_pick_longitude";
    public static final String EXTRA_PICK_LOCATION_LABEL = "extra_pick_location_label";
    public static final String EXTRA_PICK_GOOGLE_MAPS_URL = "extra_pick_google_maps_url";

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
    private String focusRoomTypeId;
    private String checkIn;
    private String checkOut;
    private String roomQuery;
    private int guests;
    private int rooms;
    private int beds;
    private List<Cabin> visibleCabins = new ArrayList<>();
    private final Set<String> favoriteCabinIds = new HashSet<>();
    private Cabin selectedCabin;
    private WishlistService wishlistService;
    private RoomTypeService roomTypeService;
    private BookingService bookingService;
    private SessionManager sessionManager;
    private boolean pickLocationMode;
    private double pickedLatitude;
    private double pickedLongitude;
    private Button directionsButton;
    private long lastPickerUpdateAt;
    private static final long PICKER_UPDATE_INTERVAL_MS = 80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_map);

        destination = getIntent().getStringExtra("destination");
        focusCabinId = getIntent().getStringExtra(AppConstants.EXTRA_CABIN_ID);
        focusRoomTypeId = getIntent().getStringExtra(AppConstants.EXTRA_ROOM_TYPE_ID);
        checkIn = safe(getIntent().getStringExtra("checkIn"), "");
        checkOut = safe(getIntent().getStringExtra("checkOut"), "");
        roomQuery = safe(getIntent().getStringExtra("roomQuery"), "");
        guests = Math.max(0, getIntent().getIntExtra("guests", 0));
        rooms = Math.max(1, getIntent().getIntExtra("rooms", 1));
        beds = Math.max(0, getIntent().getIntExtra("beds", 0));
        pickLocationMode = getIntent().getBooleanExtra(EXTRA_PICK_LOCATION, false);
        pickedLatitude = getIntent().getDoubleExtra(EXTRA_PICK_LATITUDE, 0);
        pickedLongitude = getIntent().getDoubleExtra(EXTRA_PICK_LONGITUDE, 0);
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
        roomTypeService = new RoomTypeService();
        bookingService = new BookingService();
        sessionManager = new SessionManager(this);

        Button backButton = findViewById(R.id.button_back);
        directionsButton = findViewById(R.id.button_directions);
        Button openDetailButton = findViewById(R.id.button_open_detail);

        backButton.setOnClickListener(view -> finish());
        directionsButton.setOnClickListener(view -> {
            if (pickLocationMode) {
                acceptPickedLocation();
            } else {
                openSelectedInGoogleMaps();
            }
        });
        openDetailButton.setOnClickListener(view -> openSelectedDetail());
        favoriteButton.setOnClickListener(view -> toggleSelectedFavorite());
        selectedCard.setOnClickListener(view -> openSelectedDetail());

        setupWebView();
        if (pickLocationMode) {
            setupLocationPicker();
        } else {
            summaryTextView.setText(buildMapSummary());
            loadMarkers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pickLocationMode) {
            return;
        }
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
        if (pickLocationMode) {
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        mapWebView.setWebViewClient(new WebViewClient());
        mapWebView.addJavascriptInterface(new MapBridge(), "HotelMap");
    }

    private void loadMarkers() {
        statusTextView.setText("Đang tải loại phòng phù hợp trên bản đồ...");
        new CabinService().getCabins(new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                List<Cabin> cityCabins = filterCabins(cabins);
                if (cityCabins.isEmpty()) {
                    statusTextView.setText("Chưa có chỗ nghỉ trên bản đồ");
                    selectedCard.setVisibility(View.GONE);
                    loadMapHtml();
                    return;
                }
                roomTypeService.attachRoomTypes(cityCabins, new SupabaseCallback<List<Cabin>>() {
                    @Override
                    public void onSuccess(List<Cabin> cabinsWithRooms) {
                        List<Cabin> matchedCabins = matchRoomTypesForMap(cabinsWithRooms);
                        if (hasComparableDateRange()) {
                            filterAvailableRoomMatches(matchedCabins);
                            return;
                        }
                        showMatchedCabins(matchedCabins);
                    }

                    @Override
                    public void onError(String message) {
                        showMatchedCabins(matchRoomTypesForMap(cityCabins));
                    }
                });
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void showMatchedCabins(List<Cabin> cabins) {
        visibleCabins = cabins;
        if (visibleCabins.isEmpty()) {
            statusTextView.setText("Không có loại phòng phù hợp để so sánh trên bản đồ.");
            selectedCabin = null;
            selectedCard.setVisibility(View.GONE);
            loadMapHtml();
            return;
        }
        visibleCabins.sort(Comparator.comparingDouble(Cabin::displayPrice));
        statusTextView.setText(formatCount(visibleCabins.size()) + " loại phòng phù hợp trên bản đồ");
        selectedCabin = resolveInitialCabin();
        renderSelectedCabin();
        loadMapHtml();
        loadFavoriteState();
    }

    private List<Cabin> matchRoomTypesForMap(List<Cabin> cabins) {
        List<Cabin> matched = new ArrayList<>();
        for (Cabin cabin : cabins) {
            RoomType preferred = focusCabinId != null && focusCabinId.equals(cabin.getId())
                    ? roomTypeById(cabin, focusRoomTypeId)
                    : null;
            RoomType roomType = preferred == null
                    ? roomTypeService.findBestRoomType(cabin, guests, beds, roomQuery)
                    : preferred;
            if (roomType != null) {
                matched.add(cabin.copyForMatchedRoom(roomType));
            } else if (cabin.getRoomTypes() == null || cabin.getRoomTypes().isEmpty()) {
                matched.add(cabin);
            }
        }
        return matched;
    }

    private void filterAvailableRoomMatches(List<Cabin> matchedCabins) {
        if (matchedCabins.isEmpty()) {
            showMatchedCabins(matchedCabins);
            return;
        }
        statusTextView.setText("Đang kiểm tra phòng trống theo ngày đã chọn...");
        List<Cabin> available = new ArrayList<>();
        final int[] completed = {0};
        final int expected = matchedCabins.size();
        for (Cabin cabin : matchedCabins) {
            RoomType roomType = cabin.getMatchedRoomType();
            if (roomType == null) {
                completeAvailabilityCheck(available, completed, expected, cabin);
                continue;
            }
            bookingService.ensureRangeIsAvailable(cabin.getId(), roomType.getId(), checkIn, checkOut, rooms, new SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    completeAvailabilityCheck(available, completed, expected, Boolean.TRUE.equals(data) ? cabin : null);
                }

                @Override
                public void onError(String message) {
                    completeAvailabilityCheck(available, completed, expected, null);
                }
            });
        }
    }

    private void completeAvailabilityCheck(List<Cabin> available, int[] completed, int expected, Cabin cabin) {
        if (cabin != null) {
            available.add(cabin);
        }
        completed[0]++;
        if (completed[0] >= expected) {
            showMatchedCabins(available);
        }
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
        selectedPriceTextView.setText(PriceUtils.formatUsd(price(selectedCabin)) + " / đêm");
        selectedMetaTextView.setText(buildSelectedMeta(selectedCabin));
        RoomType matchedRoomType = selectedCabin.getMatchedRoomType();
        selectedDescriptionTextView.setText(matchedRoomType == null
                ? safe(selectedCabin.getDescription(), "Chỗ nghỉ có vị trí thuận tiện, thông tin phòng rõ ràng và dễ đặt.")
                : matchedRoomType.displayName() + " · " + matchedRoomType.sizeLabel() + " · " + matchedRoomType.bedLabel());
        renderFavoriteButton();
        Glide.with(this)
                .load(selectedCabin.getImage())
                .centerCrop()
                .placeholder(R.drawable.bg_dark_card)
                .into(selectedImageView);
    }

    private void loadMapHtml() {
        mapWebView.loadDataWithBaseURL(
                pickLocationMode ? "https://serein.local/" : "https://leafletjs.com/",
                buildMapHtml(),
                "text/html",
                "UTF-8",
                null
        );
    }

    private void setupLocationPicker() {
        selectedCard.setVisibility(View.GONE);
        statusTextView.setVisibility(View.GONE);
        directionsButton.setText("Lưu");
        destination = safe(getIntent().getStringExtra(EXTRA_PICK_LOCATION_LABEL), destination);
        if (pickedLatitude == 0 || pickedLongitude == 0) {
            pickedLatitude = fallbackLat();
            pickedLongitude = fallbackLng();
        }
        summaryTextView.setText("Chọn vị trí khách sạn");
        updatePickerStatus();
        loadMapHtml();
    }

    private String buildMapHtml() {
        if (pickLocationMode) {
            return buildPickerMapHtml();
        }
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

    private String buildPickerMapHtml() {
        double centerLat = pickedLatitude == 0 ? fallbackLat() : pickedLatitude;
        double centerLng = pickedLongitude == 0 ? fallbackLng() : pickedLongitude;
        return "<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>"
                + "html,body,#map{height:100%;margin:0;background:#d8e4ed;font-family:Arial,sans-serif;overflow:hidden;touch-action:none;}"
                + "#tiles{position:absolute;inset:0;overflow:hidden;background:#d8e4ed;}"
                + "#tiles img{position:absolute;width:256px;height:256px;user-select:none;-webkit-user-drag:none;}"
                + ".map-shade{position:absolute;inset:0;pointer-events:none;background:linear-gradient(180deg,rgba(10,24,44,.08),rgba(10,24,44,0) 22%,rgba(10,24,44,.08));}"
                + ".pick-pin{position:absolute;z-index:50;width:42px;height:42px;margin-left:-21px;margin-top:-54px;border-radius:50% 50% 50% 0;"
                + "background:#0b84ff;border:4px solid #fff;transform:rotate(-45deg);box-shadow:0 8px 20px rgba(0,0,0,.32);}"
                + ".pick-pin:after{content:'';position:absolute;width:12px;height:12px;left:11px;top:11px;background:#fff;border-radius:50%;}"
                + ".coord{position:absolute;left:12px;bottom:12px;z-index:999;padding:8px 11px;border-radius:9px;background:rgba(255,255,255,.9);"
                + "font-size:11px;font-weight:800;color:#102033;box-shadow:0 6px 18px rgba(0,0,0,.15);}"
                + "</style></head><body><div id='map'><div id='tiles'></div><div class='map-shade'></div>"
                + "<div id='pin' class='pick-pin'></div><div id='coord' class='coord'></div></div><script>"
                + "var zoom=16,tileSize=256,tileCount=Math.pow(2,zoom),centerLat=" + centerLat + ",centerLng=" + centerLng + ";"
                + "var map=document.getElementById('map'),tiles=document.getElementById('tiles'),pin=document.getElementById('pin'),coord=document.getElementById('coord');"
                + "var centerWorld=project(centerLat,centerLng),picked={lat:centerLat,lng:centerLng},tileHost=0;"
                + "function project(lat,lng){var sin=Math.sin(lat*Math.PI/180);sin=Math.min(Math.max(sin,-.9999),.9999);"
                + "return{x:(lng+180)/360*tileCount*tileSize,y:(.5-Math.log((1+sin)/(1-sin))/(4*Math.PI))*tileCount*tileSize};}"
                + "function unproject(x,y){var lng=x/(tileCount*tileSize)*360-180;var n=Math.PI-2*Math.PI*y/(tileCount*tileSize);"
                + "var lat=180/Math.PI*Math.atan(.5*(Math.exp(n)-Math.exp(-n)));return{lat:lat,lng:lng};}"
                + "function renderTiles(){var w=map.clientWidth,h=map.clientHeight,r=2,baseX=Math.floor(centerWorld.x/tileSize),baseY=Math.floor(centerWorld.y/tileSize);"
                + "tiles.innerHTML='';for(var dx=-r;dx<=r;dx++){for(var dy=-r;dy<=r;dy++){var tx=baseX+dx,ty=baseY+dy;if(ty<0||ty>=tileCount)continue;"
                + "var img=document.createElement('img');img.loading='eager';img.decoding='async';img.style.left=(tx*tileSize-centerWorld.x+w/2)+'px';"
                + "img.style.top=(ty*tileSize-centerWorld.y+h/2)+'px';img.src='https://'+(['a','b','c'][tileHost++%3])+'.basemaps.cartocdn.com/rastertiles/voyager/'+zoom+'/'+tx+'/'+ty+'.png';tiles.appendChild(img);}}}"
                + "function setPinFromLatLng(latlng,send){picked=latlng;var p=project(latlng.lat,latlng.lng);pin.style.left=(p.x-centerWorld.x+map.clientWidth/2)+'px';"
                + "pin.style.top=(p.y-centerWorld.y+map.clientHeight/2)+'px';coord.textContent=latlng.lat.toFixed(6)+', '+latlng.lng.toFixed(6);"
                + "if(send&&window.HotelMap){HotelMap.pickLocation(latlng.lat,latlng.lng);}}"
                + "function choose(clientX,clientY,send){var r=map.getBoundingClientRect();var x=centerWorld.x+(clientX-r.left)-r.width/2;var y=centerWorld.y+(clientY-r.top)-r.height/2;setPinFromLatLng(unproject(x,y),send);}"
                + "var dragging=false;map.addEventListener('touchstart',function(e){dragging=true;if(e.touches.length){choose(e.touches[0].clientX,e.touches[0].clientY,true);}e.preventDefault();},{passive:false});"
                + "map.addEventListener('touchmove',function(e){if(dragging&&e.touches.length){choose(e.touches[0].clientX,e.touches[0].clientY,true);}e.preventDefault();},{passive:false});"
                + "map.addEventListener('touchend',function(){dragging=false;});map.addEventListener('click',function(e){choose(e.clientX,e.clientY,true);});"
                + "window.addEventListener('resize',function(){renderTiles();setPinFromLatLng(picked,false);});"
                + "setTimeout(function(){renderTiles();setPinFromLatLng(picked,true);},30);"
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
        if (selectedCabin.getMatchedRoomType() != null) {
            intent.putExtra(AppConstants.EXTRA_ROOM_TYPE_ID, selectedCabin.getMatchedRoomType().getId());
        }
        intent.putExtra("destination", destination);
        intent.putExtra("checkIn", checkIn);
        intent.putExtra("checkOut", checkOut);
        intent.putExtra("guests", Math.max(1, guests));
        intent.putExtra("rooms", Math.max(1, rooms));
        intent.putExtra("beds", Math.max(0, beds));
        intent.putExtra("roomQuery", roomQuery);
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

    private void acceptPickedLocation() {
        if (!pickLocationMode) {
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_PICK_LATITUDE, pickedLatitude);
        result.putExtra(EXTRA_PICK_LONGITUDE, pickedLongitude);
        result.putExtra(EXTRA_PICK_GOOGLE_MAPS_URL, googleMapsUrl(pickedLatitude, pickedLongitude));
        setResult(RESULT_OK, result);
        finish();
    }

    private void updatePickerStatus() {
        statusTextView.setText(String.format(Locale.US,
                "Đã chọn: %.6f, %.6f. Chạm hoặc kéo pin để đổi vị trí.",
                pickedLatitude,
                pickedLongitude));
    }

    private String googleMapsUrl(double latitude, double longitude) {
        return "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
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
        return cabin.getMatchedRoomType() == null
                ? PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount())
                : cabin.getMatchedRoomType().getBasePrice();
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
        RoomType roomType = cabin.getMatchedRoomType();
        if (roomType != null) {
            return roomType.displayName()
                    + " - " + roomType.effectiveMaxAdults() + " người lớn"
                    + " - " + roomType.effectiveBedCount() + " giường"
                    + " - " + roomType.sizeLabel();
        }
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

    private String buildMapSummary() {
        StringBuilder builder = new StringBuilder(safe(destination, "Ho Chi Minh City"));
        builder.append(" - ").append(compactDateRange());
        if (guests > 0) {
            builder.append(" - ").append(guests).append(" người lớn");
        }
        if (rooms > 1) {
            builder.append(" - ").append(rooms).append(" phòng");
        }
        if (beds > 0) {
            builder.append(" - ").append(beds).append(" giường");
        }
        return builder.toString();
    }

    private boolean hasComparableDateRange() {
        try {
            LocalDate start = LocalDate.parse(checkIn);
            LocalDate end = LocalDate.parse(checkOut);
            return end.isAfter(start);
        } catch (Exception exception) {
            return false;
        }
    }

    private RoomType roomTypeById(Cabin cabin, String roomTypeId) {
        if (roomTypeId == null || cabin.getRoomTypes() == null) {
            return null;
        }
        for (RoomType roomType : cabin.getRoomTypes()) {
            if (roomTypeId.equals(roomType.getId())) {
                return roomType;
            }
        }
        return null;
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

        @JavascriptInterface
        public void pickLocation(double latitude, double longitude) {
            long now = System.currentTimeMillis();
            if (now - lastPickerUpdateAt < PICKER_UPDATE_INTERVAL_MS) {
                return;
            }
            lastPickerUpdateAt = now;
            runOnUiThread(() -> {
                pickedLatitude = latitude;
                pickedLongitude = longitude;
                updatePickerStatus();
            });
        }
    }
}
