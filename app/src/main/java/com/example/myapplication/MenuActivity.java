package com.example.myapplication;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;

public class MenuActivity extends BottomNavigationActivity {
    private User user;
    private SpeechUtility speechUtility;

    private void updateProgressAndColor(ProgressBar progressBar, int currentCalories, int dailyGoal) {
        try {
            // Set max and animate progress
            progressBar.setMax(dailyGoal);
            animateProgressBar(progressBar, currentCalories);

            // Calculate percentage
            float percentage = ((float)currentCalories / dailyGoal) * 100;
            percentage = Math.min(percentage, 100);

            // Get drawable and update gradient
            Drawable progressDrawable = progressBar.getProgressDrawable();
            if (progressDrawable == null) return;

            if (progressDrawable instanceof LayerDrawable) {
                LayerDrawable layerDrawable = (LayerDrawable) progressDrawable;
                Drawable progressLayer = layerDrawable.findDrawableByLayerId(android.R.id.progress);

                if (progressLayer instanceof ScaleDrawable) {
                    progressLayer = ((ScaleDrawable) progressLayer).getDrawable();
                }

                if (progressLayer instanceof GradientDrawable) {
                    final GradientDrawable gradientDrawable = (GradientDrawable) progressLayer;
                    final float finalPercentage = percentage;

                    // Values for #4CAF50 (Material Design Green)
                    final int startRed = 76;       // R value of #4CAF50
                    final int startGreen = 175;    // G value of #4CAF50
                    final int startBlue = 80;      // B value of #4CAF50

                    progressBar.post(() -> {
                        if (progressBar.getWidth() > 0) {
                            // Intensity increases with percentage
                            float gradientIntensity = finalPercentage / 100f;

                            // Update gradient radius based on progress percentage
                            gradientDrawable.setGradientRadius(progressBar.getWidth() * gradientIntensity);

                            // If approaching goal, enhance green color
                            if (finalPercentage > 75) {
                                // Make greener when approaching goal
                                int[] colors = {
                                        android.graphics.Color.rgb(startRed, startGreen, startBlue),
                                        android.graphics.Color.rgb(
                                                255 - (int)((255 - startRed) * gradientIntensity),
                                                255 - (int)((255 - startGreen) * gradientIntensity),
                                                255 - (int)((255 - startBlue) * gradientIntensity))
                                };
                                gradientDrawable.setColors(colors);
                            }

                            progressBar.invalidate();
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Setup bottom navigation
        setupBottomNavigation();

        speechUtility = new SpeechUtility(this, BuildConfig.OPENAI_API_KEY);

        speechUtility.speak("For help on voice commands, say help");


        try {
            user = User.getInstance();
            if (user != null) {
                user.loadUserData(this);

                // Update UI
                ProgressBar progressBar = findViewById(R.id.progressBar);
                TextView calorieText = findViewById(R.id.calorieText);

                if (progressBar != null && calorieText != null) {
                    updateProgressAndColor(progressBar, user.getCurrentCalories(), user.getDailyGoal());
                    calorieText.setText(user.getCurrentCalories() + "/" + user.getDailyGoal() + " kcal");
                }
            }

            // Set up navigation (with null checks)
            if (findViewById(R.id.menuLoginButton) != null) {
                findViewById(R.id.menuLoginButton).setOnClickListener(v -> {
                    startActivity(new Intent(this, LoginActivity.class));
                });
            }

            if (findViewById(R.id.menuCalculatorButton) != null) {
                findViewById(R.id.menuCalculatorButton).setOnClickListener(v -> {
                    startActivity(new Intent(this, MainActivity.class));
                });
            }

            if (findViewById(R.id.calorieButton) != null || findViewById(R.id.caloriesContainer) != null) {
                View.OnClickListener calorieClickListener = v -> {
                    navigationView.setSelectedItemId(R.id.navigation_calorie);
                };

                if (findViewById(R.id.calorieButton) != null) {
                    findViewById(R.id.calorieButton).setOnClickListener(calorieClickListener);
                }

                if (findViewById(R.id.caloriesContainer) != null) {
                    findViewById(R.id.caloriesContainer).setOnClickListener(calorieClickListener);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Refresh data when returning to this activity
            if (user != null) {
                user.loadUserData(this);
                ProgressBar progressBar = findViewById(R.id.progressBar);
                TextView calorieText = findViewById(R.id.calorieText);

                if (progressBar != null && calorieText != null) {
                    updateProgressAndColor(progressBar, user.getCurrentCalories(), user.getDailyGoal());
                    calorieText.setText(user.getCurrentCalories() + "/" + user.getDailyGoal() + " kcal");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void animateProgressBar(ProgressBar progressBar, int newProgress) {
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), newProgress)
                .setDuration(500)
                .start();
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.navigation_menu;
    }
}