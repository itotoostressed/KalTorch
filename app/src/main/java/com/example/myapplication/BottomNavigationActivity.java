package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Base activity that handles both bottom navigation and voice commands
 * Extends VoiceCommandActivity to inherit all voice command functionality
 */
public abstract class BottomNavigationActivity extends VoiceCommandActivity implements VoiceCommandActivity.VoiceCommandListener {
    private static final String TAG = "BottomNavActivity";

    protected BottomNavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // This calls VoiceCommandActivity's onCreate
        // Don't set content view here - let subclasses do it
    }

    /**
     * Setup bottom navigation with menu items and click listeners
     */
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

    /**
     * Set up activity-specific voice commands related to navigation
     * This implements the abstract method from VoiceCommandActivity
     */
    @Override
    protected void setupActivityCommands() {
        // Register navigation commands
        registerCommand("go to menu", "nav_to_menu");
        registerCommand("open menu", "nav_to_menu");

        registerCommand("go to calories", "nav_to_calorie");
        registerCommand("show calories", "nav_to_calorie");
        registerCommand("calorie screen", "nav_to_calorie");

        registerCommand("go to workout", "nav_to_workout");
        registerCommand("go workout", "nav_to_workout");
        registerCommand("show workout", "nav_to_workout");

        registerCommand("go to diet", "nav_to_diet");
        registerCommand("show diet", "nav_to_diet");

        // Add any additional navigation commands here
    }

    /**
     * Handle voice commands by navigating to the appropriate activity
     * Implements VoiceCommandListener interface method
     */
    @Override
    public void onVoiceCommand(String commandId) {
        Log.d(TAG, "Voice command received: " + commandId);

        // Handle navigation commands
        switch (commandId) {
            case "nav_to_menu":
                startActivity(new Intent(this, MenuActivity.class));
                break;
            case "nav_to_calorie":
                startActivity(new Intent(this, CalorieActivity.class));
                break;
            case "nav_to_workout":
                startActivity(new Intent(this, WorkoutActivity.class));
                break;
            case "nav_to_diet":
                startActivity(new Intent(this, MainActivity.class));
                break;
            // Global navigation commands from VoiceCommandActivity parent class
            case "action_home":
                startActivity(new Intent(this, MainActivity.class));
                break;
            case "action_diet":
                startActivity(new Intent(this, MainActivity.class));
                break;
            case "action_workout":
                startActivity(new Intent(this, WorkoutActivity.class));
                break;
            case "action_settings":
                // Implement settings navigation if you have a settings activity
                break;
            default:
                // For commands not related to navigation, let subclasses handle them
                handleActivitySpecificCommands(commandId);
                break;
        }
    }

    /**
     * Allow subclasses to handle their own specific voice commands
     * @param commandId The command identifier to handle
     */
    protected void handleActivitySpecificCommands(String commandId) {
        // Default implementation does nothing
        // Subclasses can override this to handle their specific commands
    }

    /**
     * Override this in each activity to specify which navigation item should be selected
     */
    protected abstract int getNavigationMenuItemId();
}