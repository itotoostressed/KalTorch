package com.example.myapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class WorkoutActivity extends BottomNavigationActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        // Setup bottom navigation
        setupBottomNavigation();

        // Initialize workout activity UI elements and functionality here
        try {
            // Your workout activity initialization code here
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.navigation_workout;
    }
}