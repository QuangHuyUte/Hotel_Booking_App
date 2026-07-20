package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager sessionManager = new SessionManager(this);
        Class<?> nextScreen = sessionManager.isHostOrAdmin() ? HostHotelDashboardActivity.class : HotelSearchActivity.class;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, nextScreen));
            finish();
        }, 850L);
    }
}
