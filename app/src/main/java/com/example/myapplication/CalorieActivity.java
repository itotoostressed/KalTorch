package com.example.myapplication;

import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.util.ArrayList;

public class CalorieActivity extends BottomNavigationActivity {
    private User user;
    private ProgressBar progressBar;
    private ListView itemsListView;
    private ArrayList<String> calorieItems;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calorie);

        // Setup bottom navigation
        setupBottomNavigation();

        user = User.getInstance();
        user.loadUserData(this);

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        itemsListView = findViewById(R.id.resultListView);

        // Setup progress bar
        progressBar.setMax(user.getDailyGoal());
        animateProgressBar(user.getCurrentCalories());
        updateProgressColor();

        // Initialize list
        calorieItems = new ArrayList<>();
        calorieItems.add("Breakfast: 400 kcal");
        calorieItems.add("Lunch: 650 kcal");
        calorieItems.add("Snack: 200 kcal");

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, calorieItems);
        itemsListView.setAdapter(adapter);

        // Button listeners
        findViewById(R.id.addCalories).setOnClickListener(v -> addNewMeal());
        findViewById(R.id.addWorkout).setOnClickListener(v -> addNewWorkout());

        // List item click
        itemsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = calorieItems.get(position);
            try {
                String[] parts = selectedItem.split(":");
                String caloriePart = parts[1].trim().split(" ")[0];
                int calories = Integer.parseInt(caloriePart);

                if (selectedItem.contains("-")) {
                    user.addCalories(-calories);
                } else {
                    user.addCalories(calories);
                }

                user.saveUserData(this);
                updateProgress();
                Toast.makeText(this, selectedItem + " applied", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error processing item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addNewMeal() {
        int calories = 500;
        calorieItems.add("Meal: " + calories + " kcal");
        user.addCalories(calories);
        updateListAndProgress();
    }

    private void addNewWorkout() {
        int calories = 300;
        calorieItems.add("Workout: -" + calories + " kcal");
        user.addCalories(-calories);
        updateListAndProgress();
    }

    private void updateListAndProgress() {
        adapter.notifyDataSetChanged();
        user.saveUserData(this);
        updateProgress();
    }

    private void updateProgress() {
        animateProgressBar(user.getCurrentCalories());
        updateProgressColor();
    }

    private void animateProgressBar(int newProgress) {
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), newProgress)
                .setDuration(500)
                .start();
    }

    private void updateProgressColor() {
        try {
            float percentage = ((float)user.getCurrentCalories() / user.getDailyGoal()) * 100;

            Drawable progressDrawable = progressBar.getProgressDrawable();
            if (progressDrawable == null) return;

            // Safely get the progress layer
            Drawable progressLayer = null;
            if (progressDrawable instanceof LayerDrawable) {
                progressLayer = ((LayerDrawable) progressDrawable)
                        .findDrawableByLayerId(android.R.id.progress);
            }

            // Only proceed if we have a GradientDrawable
            if (progressLayer instanceof GradientDrawable) {
                GradientDrawable gradient = (GradientDrawable) progressLayer;

                // Wait until view has proper dimensions
                progressBar.post(() -> {
                    if (progressBar.getWidth() > 0) {
                        float gradientIntensity = percentage/100f;
                        gradient.setGradientRadius(progressBar.getWidth() * gradientIntensity);
                        progressBar.invalidate();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        user.saveUserData(this);
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.navigation_calorie;
    }
}