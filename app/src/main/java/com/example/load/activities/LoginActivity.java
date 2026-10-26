package com.example.load.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.load.MainActivity;
import com.example.load.R;
import com.example.load.models.ValidationUtils;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        tvGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        String emailError = ValidationUtils.getEmailError(email);
        if (emailError != null) {
            etEmail.setError(emailError);
            etEmail.requestFocus();
            return;
        }

        String passwordError = ValidationUtils.getPasswordError(password);
        if (passwordError != null) {
            etPassword.setError(passwordError);
            etPassword.requestFocus();
            return;
        }

        Toast.makeText(this, "Login valid", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
        finish();
    }
}