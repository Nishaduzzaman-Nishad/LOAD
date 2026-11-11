package com.example.load_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.load_app.R;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnLogin = findViewById(R.id.btnGoToLogin);
        Button btnRegister = findViewById(R.id.btnGoToRegister);

        btnLogin.setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, LoginActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class)));
    }
}
