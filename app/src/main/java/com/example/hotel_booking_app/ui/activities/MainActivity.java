package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.utils.SessionManager;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessionManager = new SessionManager(this);
        Class<?> nextScreen = sessionManager.isLoggedIn() ? PersonalActivity.class : HomeActivity.class;
        startActivity(new Intent(this, nextScreen));
        finish();
    }
}
