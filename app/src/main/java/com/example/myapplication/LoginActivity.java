package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        user = User.getInstance();
        user.initialize(this);

        findViewById(R.id.loginButton).setOnClickListener(v -> {
            String username = ((EditText)findViewById(R.id.usernameField)).getText().toString();
            String password = ((EditText)findViewById(R.id.passwordField)).getText().toString();

            if (user.checkUP(username, password)) {
                startActivity(new Intent(this, MenuActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.createAccountButton).setOnClickListener(v -> {
            String username = ((EditText)findViewById(R.id.usernameField)).getText().toString();
            String password = ((EditText)findViewById(R.id.passwordField)).getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            } else {
                user.setUsername(username);
                user.setPassword(password);
                user.saveUserData(this);
                Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}