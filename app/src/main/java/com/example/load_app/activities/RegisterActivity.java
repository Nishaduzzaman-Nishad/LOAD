package com.example.load_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.load_app.R;
import com.example.load_app.models.ValidationUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Spinner spinnerRole;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey("AIzaSyBzGU3NJV8-y5wDwmn3jOTw95e88HKxZeM")
                    .setApplicationId("1:853658382959:android:b6b6c6ce5c5caa928e139a")
                    .setProjectId("load-2420e")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }

        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnRegister = findViewById(R.id.btnRegister);

        String[] roles = {"ADMIN", "RIDER", "CUSTOMER", "P2P"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        btnRegister.setOnClickListener(v -> handleRegister());
    }

    private void handleRegister() {
        String fullName = etFullName.getText().toString().trim();
        String emailInput = etEmail.getText().toString().trim();
        String phoneInput = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (fullName.isEmpty()) { etFullName.setError("Required"); return; }

        // 1. Sanitize the Phone field (allows +880, 880, dashes)
        String sanitizedPhone = sanitizePhoneNumber(phoneInput);
        if (sanitizedPhone == null) {
            etPhone.setError("Invalid BD Phone Number");
            return;
        }

        // 2. Handle Email/Phone logic for Firebase Auth
        String firebaseEmail = emailInput;
        if (!emailInput.contains("@")) {
            // If they typed a phone number in the email field, sanitize it
            String cleanPhoneEmail = sanitizePhoneNumber(emailInput);
            if (cleanPhoneEmail == null) {
                etEmail.setError("Invalid Email or Phone");
                return;
            }
            firebaseEmail = cleanPhoneEmail + "@load-app.com";
        } else {
            // Standard Email Validation
            if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                etEmail.setError("Invalid Email format");
                return;
            }
        }

        // 3. Password Validation
        String passwordError = ValidationUtils.getPasswordError(password);
        if (passwordError != null) { etPassword.setError(passwordError); return; }
        if (!password.equals(confirmPassword)) { etConfirmPassword.setError("Mismatch"); return; }

        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(firebaseEmail, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                String userId = mAuth.getCurrentUser().getUid();

                Map<String, Object> user = new HashMap<>();
                user.put("userId", userId);
                user.put("fullName", fullName);
                user.put("email", emailInput); // Store what they originally typed
                user.put("phone", sanitizedPhone); // Store the clean phone
                user.put("role", role);

                db.collection("users").document(userId).set(user).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Firestore Error: " + task1.getException().getMessage(), Toast.LENGTH_LONG).show();
                        btnRegister.setEnabled(true);
                    }
                });
            } else {
                Toast.makeText(RegisterActivity.this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                btnRegister.setEnabled(true);
            }
        });
    }

    // Helper to clean +880, dashes, and spaces
    private String sanitizePhoneNumber(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("880") && digits.length() == 13) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("88") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        }
        if (digits.startsWith("01") && digits.length() == 11) {
            return digits;
        }
        return null;
    }
}