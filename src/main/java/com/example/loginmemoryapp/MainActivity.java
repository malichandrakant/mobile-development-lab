package com.example.loginmemoryapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText editTextName;
    Button btnLogin, btnLogout;
    TextView textStatus;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // STEP 1: connect UI
        editTextName = findViewById(R.id.editTextName);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogout = findViewById(R.id.btnLogout);
        textStatus = findViewById(R.id.textStatus);

        // STEP 2: create SharedPreferences
        prefs = getSharedPreferences("my_app", MODE_PRIVATE);

        // STEP 3: check login state on app start
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            String name = prefs.getString("username", "");
            textStatus.setText("Welcome " + name);
        } else {
            textStatus.setText("Not Logged In");
        }

        // STEP 4: LOGIN BUTTON
        btnLogin.setOnClickListener(v -> {

            String name = editTextName.getText().toString();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", name);
            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            textStatus.setText("Welcome " + name);
        });

        // STEP 5: LOGOUT BUTTON
        btnLogout.setOnClickListener(v -> {

            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            textStatus.setText("Logged Out");
        });
    }
}