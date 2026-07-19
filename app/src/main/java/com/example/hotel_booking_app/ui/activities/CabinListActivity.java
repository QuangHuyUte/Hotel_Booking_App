package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.ui.adapters.CabinAdapter;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.PriceUtils;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CabinListActivity extends AppCompatActivity {
    private TextView statusTextView;
    private EditText searchEditText;
    private Spinner sortSpinner;
    private CabinAdapter adapter;
    private CabinService cabinService;
    private List<Cabin> loadedCabins = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isHostOrAdmin()) {
            startActivity(new Intent(this, HostDashboardActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_cabin_list);

        statusTextView = findViewById(R.id.text_status);
        searchEditText = findViewById(R.id.edit_search);
        sortSpinner = findViewById(R.id.spinner_sort);
        Button applyFiltersButton = findViewById(R.id.button_apply_filters);
        TextView filterLabel = findViewById(R.id.text_filter_label);
        LinearLayout aboutTab = findViewById(R.id.nav_about);
        LinearLayout personalTab = findViewById(R.id.nav_personal);
        RecyclerView recyclerView = findViewById(R.id.recycler_cabins);

        cabinService = new CabinService();
        adapter = new CabinAdapter(cabin -> {
            Intent intent = new Intent(this, CabinDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_CABIN_ID, cabin.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        setupSortSpinner();
        sortSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (!loadedCabins.isEmpty()) {
                    renderCabins();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        applyFiltersButton.setOnClickListener(view -> renderCabins());
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCabins();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        aboutTab.setOnClickListener(view -> startActivity(new Intent(this, HomeActivity.class)));
        personalTab.setOnClickListener(view -> startActivity(new Intent(this, PersonalActivity.class)));
        loadCabins();
    }

    private void setupSortSpinner() {
        String[] sortOptions = {
                "Name (A-Z)",
                "Discounted cabins",
                "Price: low to high",
                "Price: high to low",
                "Capacity: high to low",
                "Newest first"
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_on_primary, sortOptions);
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_serein_dropdown);
        sortSpinner.setAdapter(spinnerAdapter);
    }

    private void loadCabins() {
        String location = getIntent().getStringExtra("location");
        String amenity = getIntent().getStringExtra("amenity");
        int guests = getIntent().getIntExtra("guests", 0);
        statusTextView.setText("Loading cabins...");
        cabinService.searchCabins(location, guests, amenity, new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                loadedCabins = cabins;
                renderCabins();
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        });
    }

    private void renderCabins() {
        List<Cabin> result = new ArrayList<>(loadedCabins);
        SearchCriteria criteria = parseSearch(searchEditText.getText().toString());
        if (criteria.hasTextTerms()) {
            result.removeIf(cabin -> !matchesTextTerms(cabin, criteria.textTerms));
        }
        if (criteria.guests > 0) {
            result.removeIf(cabin -> cabin.getMaxCapacity() < criteria.guests);
        }
        if (criteria.maxPrice > 0) {
            result.removeIf(cabin -> PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount()) > criteria.maxPrice);
        }
        if (criteria.discountOnly) {
            result.removeIf(cabin -> cabin.getDiscount() <= 0);
        }

        int selected = sortSpinner.getSelectedItemPosition();
        if (selected == 0) {
            result.sort(Comparator.comparing(cabin -> safe(cabin.getName()), String.CASE_INSENSITIVE_ORDER));
        } else if (selected == 1) {
            result.removeIf(cabin -> cabin.getDiscount() <= 0);
            result.sort(Comparator.comparing(cabin -> safe(cabin.getName()), String.CASE_INSENSITIVE_ORDER));
        } else if (selected == 2) {
            result.sort(Comparator.comparingDouble(cabin -> PriceUtils.priceAfterDiscount(cabin.getRegularPrice(), cabin.getDiscount())));
        } else if (selected == 3) {
            result.sort((left, right) -> Double.compare(
                    PriceUtils.priceAfterDiscount(right.getRegularPrice(), right.getDiscount()),
                    PriceUtils.priceAfterDiscount(left.getRegularPrice(), left.getDiscount())
            ));
        } else if (selected == 4) {
            result.sort((left, right) -> Integer.compare(right.getMaxCapacity(), left.getMaxCapacity()));
        }

        adapter.submitList(result);
        statusTextView.setText("Showing " + result.size() + " cabin(s).");
    }

    private SearchCriteria parseSearch(String input) {
        String query = input == null ? "" : input.trim().toLowerCase(Locale.US);
        SearchCriteria criteria = new SearchCriteria();
        criteria.discountOnly = query.contains("discount") || query.contains("sale") || query.contains("deal");

        Matcher guestMatcher = Pattern.compile("(\\d+)\\s*(guest|guests|khach|people|person|persons)").matcher(query);
        if (guestMatcher.find()) {
            criteria.guests = parseIntOrZero(guestMatcher.group(1));
            query = query.replace(guestMatcher.group(0), " ");
        }

        Matcher maxPriceMatcher = Pattern.compile("(under|below|max|less than|<=|duoi)\\s*\\$?\\s*(\\d+(?:\\.\\d+)?)").matcher(query);
        if (maxPriceMatcher.find()) {
            criteria.maxPrice = parseDoubleOrZero(maxPriceMatcher.group(2));
            query = query.replace(maxPriceMatcher.group(0), " ");
        } else {
            Matcher dollarMatcher = Pattern.compile("\\$\\s*(\\d+(?:\\.\\d+)?)").matcher(query);
            if (dollarMatcher.find()) {
                criteria.maxPrice = parseDoubleOrZero(dollarMatcher.group(1));
                query = query.replace(dollarMatcher.group(0), " ");
            }
        }

        query = query.replace("discount", " ")
                .replace("sale", " ")
                .replace("deal", " ")
                .replace(",", " ")
                .replace(";", " ");
        for (String term : query.split("\\s+")) {
            if (term.trim().length() >= 2) {
                criteria.textTerms.add(term.trim());
            }
        }
        return criteria;
    }

    private int parseIntOrZero(String value) {
        try {
            return value == null || value.trim().isEmpty() ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleOrZero(String value) {
        try {
            return value == null || value.trim().isEmpty() ? 0 : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean matchesTextTerms(Cabin cabin, List<String> terms) {
        String haystack = (safe(cabin.getName()) + " "
                + safe(cabin.getLocation()) + " "
                + safe(cabin.getAmenities()) + " "
                + safe(cabin.getDescription())).toLowerCase(Locale.US);
        for (String term : terms) {
            if (!haystack.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class SearchCriteria {
        private int guests;
        private double maxPrice;
        private boolean discountOnly;
        private final List<String> textTerms = new ArrayList<>();

        private boolean hasTextTerms() {
            return !textTerms.isEmpty();
        }
    }
}
