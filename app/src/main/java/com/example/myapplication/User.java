package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class User {
    private static User instance;
    private String username;
    private String password;
    private int currentCalories = 0;
    private static final int DAILY_GOAL = 2000;
    private static final String PREFS_NAME = "UserData";

    // Singleton pattern
    public static synchronized User getInstance() {
        if (instance == null) {
            instance = new User();
        }
        return instance;
    }

    private User() {}

    public void initialize(Context context) {
        loadUserData(context);
        if (username == null || username.isEmpty()) {
            username = "test";
            password = "test123";
            currentCalories = 0;
            saveUserData(context);
        }
    }

    public void addCalories(int calories) {
        this.currentCalories += calories;
    }

    public void setCurrentCalories(int calories) {
        this.currentCalories = calories;
    }

    public int getCurrentCalories() {
        return currentCalories;
    }

    public int getDailyGoal() {
        return DAILY_GOAL;
    }

    public void saveUserData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("username", username)
                .putString("password", password)
                .putInt("currentCalories", currentCalories)
                .apply();
    }

    public void loadUserData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        username = prefs.getString("username", "");
        password = prefs.getString("password", "");
        currentCalories = prefs.getInt("currentCalories", 0);
    }

    public boolean checkUP(String inputUsername, String inputPassword) {
        return username.equals(inputUsername) && password.equals(inputPassword);
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}