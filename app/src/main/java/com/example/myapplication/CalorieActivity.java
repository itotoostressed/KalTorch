package com.example.myapplication;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;

public class CalorieActivity extends BottomNavigationActivity {
    private User user;
    private ProgressBar progressBar;
    private ListView itemsListView;
    private ArrayList<String> calorieItems;
    private ArrayAdapter<String> adapter;
    private ConstraintLayout mealInputLayout;
    private EditText editFoodName;
    private EditText editCalories;
    private ConstraintLayout workoutInputLayout;
    private EditText editWorkoutName;
    private EditText editWorkoutCalories;

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
        mealInputLayout = findViewById(R.id.mealInputLayout);
        editFoodName = findViewById(R.id.editFoodName);
        editCalories = findViewById(R.id.editCalories);
        workoutInputLayout = findViewById(R.id.workoutInputLayout);
        editWorkoutName = findViewById(R.id.editWorkoutName);
        editWorkoutCalories = findViewById(R.id.editWorkoutCalories);

        // Setup progress bar
        progressBar.setMax(user.getDailyGoal());
        animateProgressBar(user.getCurrentCalories());
        updateProgressColor();

        // Initialize list with both food and workout items
        calorieItems = new ArrayList<>();
        calorieItems.add("Breakfast: 400 kcal");
        calorieItems.add("Lunch: 650 kcal");
        calorieItems.add("Snack: 200 kcal");
        calorieItems.add("[Workout] Running: -300 kcal");  // Added base workout item

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, calorieItems);
        itemsListView.setAdapter(adapter);

        // Button listeners
        findViewById(R.id.addCalories).setOnClickListener(v -> showMealInputLayout());
        findViewById(R.id.addWorkout).setOnClickListener(v -> showWorkoutInputLayout());
        findViewById(R.id.btnSaveMeal).setOnClickListener(v -> saveMeal());
        findViewById(R.id.btnCancelMeal).setOnClickListener(v -> hideMealInputLayout());
        findViewById(R.id.btnSaveWorkout).setOnClickListener(v -> saveWorkout());
        findViewById(R.id.btnCancelWorkout).setOnClickListener(v -> hideWorkoutInputLayout());

        // List item click - Fixed to properly handle negative calories from workouts
        itemsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = calorieItems.get(position);
            try {
                String[] parts = selectedItem.split(":");
                String caloriePart = parts[1].trim().split(" ")[0];
                int calories;

                // Handle negative calories (for workouts)
                if (caloriePart.startsWith("-")) {
                    calories = Integer.parseInt(caloriePart);  // Already negative
                } else {
                    calories = Integer.parseInt(caloriePart);
                }

                // Apply calories based on item type
                user.addCalories(calories);  // Will add or subtract based on sign

                user.saveUserData(this);
                updateProgress();

                // Show appropriate message
                if (calories < 0) {
                    Toast.makeText(this, "Workout applied: " + (-calories) + " calories burned", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Food item applied: " + calories + " calories added", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Toast.makeText(this, "Error processing item", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private void showWorkoutInputLayout() {
        workoutInputLayout.setVisibility(View.VISIBLE);
        editWorkoutName.setText("");
        editWorkoutCalories.setText("");
    }

    private void hideWorkoutInputLayout() {
        workoutInputLayout.setVisibility(View.GONE);
    }

    private void saveWorkout() {
        try {
            String name = editWorkoutName.getText().toString().trim();
            String caloriesText = editWorkoutCalories.getText().toString().trim();

            if (name.isEmpty() || caloriesText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int calories = Integer.parseInt(caloriesText);
            if (calories <= 0) {
                Toast.makeText(this, "Calories must be positive", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add workout with negative calories (to subtract from total)
            String workoutItem = "[Workout] " + name + ": -" + calories + " kcal";
            calorieItems.add(workoutItem);

            // Subtract calories for workouts
            user.addCalories(-calories);

            updateListAndProgress();
            hideWorkoutInputLayout();
            Toast.makeText(this, "Workout added: " + calories + " calories burned", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMealInputLayout() {
        if (user.getCurrentCalories() > user.getDailyGoal()) {
            Toast.makeText(this, "Cannot add more calories - daily limit reached", Toast.LENGTH_SHORT).show();
            return;
        }
        mealInputLayout.setVisibility(View.VISIBLE);
        editFoodName.setText("");
        editCalories.setText("");
    }

    private void hideMealInputLayout() {
        mealInputLayout.setVisibility(View.GONE);
    }

    private void saveMeal() {
        try {
            String foodName = editFoodName.getText().toString().trim();
            String caloriesText = editCalories.getText().toString().trim();

            if (foodName.isEmpty() || caloriesText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int calories = Integer.parseInt(caloriesText);
            if (calories <= 0) {
                Toast.makeText(this, "Calories must be positive", Toast.LENGTH_SHORT).show();
                return;
            }

            calorieItems.add(foodName + ": " + calories + " kcal");
            user.addCalories(calories);
            updateListAndProgress();
            hideMealInputLayout();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for calories", Toast.LENGTH_SHORT).show();
        }
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
        if (user.getCurrentCalories() > user.getDailyGoal()) {
            new AlertDialog.Builder(CalorieActivity.this)
                    .setTitle("Calorie Limit Reached")
                    .setMessage("You've exceeded your daily calorie goal! Adding more calories will be disabled.")
                    .setPositiveButton("OK", null)
                    .setCancelable(false)
                    .show();
        }
        else {
            try {
                float percentage = ((float)user.getCurrentCalories() / user.getDailyGoal()) * 100;

                Drawable progressDrawable = progressBar.getProgressDrawable();
                if (progressDrawable == null) return;

                Drawable progressLayer = null;
                if (progressDrawable instanceof LayerDrawable) {
                    progressLayer = ((LayerDrawable) progressDrawable)
                            .findDrawableByLayerId(android.R.id.progress);
                }

                if (progressLayer instanceof GradientDrawable) {
                    GradientDrawable gradient = (GradientDrawable) progressLayer;
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