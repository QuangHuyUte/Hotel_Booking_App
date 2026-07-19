package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.utils.SessionManager;

public class HomeActivity extends AppCompatActivity {
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        LinearLayout cabinsTab = findViewById(R.id.nav_cabins);
        LinearLayout personalTab = findViewById(R.id.nav_personal);

        cabinsTab.setOnClickListener(view -> openCabinsForRole());
        personalTab.setOnClickListener(view -> startActivity(new Intent(this, PersonalActivity.class)));
    }

    private void openCabinsForRole() {
        Class<?> target = sessionManager.isHostOrAdmin() ? HostDashboardActivity.class : CabinListActivity.class;
        startActivity(new Intent(this, target));
    }
}
