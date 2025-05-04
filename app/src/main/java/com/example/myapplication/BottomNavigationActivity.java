package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavigationActivity extends AppCompatActivity {

    protected BottomNavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupBottomNavigation() {
        // Initialize the bottom navigation view
        navigationView = findViewById(R.id.bottom_navigation);

        if (navigationView != null) {
            navigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                // Don't navigate if we're already on this screen
                if (itemId == getNavigationMenuItemId()) {
                    return true;
                }

                // Handle navigation item clicks
                if (itemId == R.id.navigation_menu) {
                    startActivity(new Intent(this, MenuActivity.class));
                    return true;
                } else if (itemId == R.id.navigation_calorie) {
                    startActivity(new Intent(this, CalorieActivity.class));
                    return true;
                } else if (itemId == R.id.navigation_workout) {
                    startActivity(new Intent(this, WorkoutActivity.class));
                    return true;
                } else if (itemId == R.id.navigation_diet) {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                }

                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (navigationView != null) {
            // Update the selected item based on current activity
            MenuItem menuItem = navigationView.getMenu().findItem(getNavigationMenuItemId());
            if (menuItem != null) {
                menuItem.setChecked(true);
            }
        }
    }

    // Override this in each activity to specify which navigation item should be selected
    protected int getNavigationMenuItemId() {
        return 0; // Override in subclasses
    }
}