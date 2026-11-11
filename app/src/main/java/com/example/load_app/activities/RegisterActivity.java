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

        String sanitizedPhone = sanitizePhoneNumber(phoneInput);
        if (sanitizedPhone == null) { etPhone.setError("Invalid BD Phone Number"); return; }

        // FIX: Create a final variable for the lambda
        final String finalFirebaseEmail;

        if (!emailInput.contains("@")) {
            String cleanPhoneEmail = sanitizePhoneNumber(emailInput);
            if (cleanPhoneEmail == null) { etEmail.setError("Invalid Email or Phone"); return; }
            finalFirebaseEmail = cleanPhoneEmail + "@load-app.com";
        } else {
            if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                etEmail.setError("Invalid Email format"); return;
            }
            finalFirebaseEmail = emailInput;
        }

        if (password.length() < 6) { etPassword.setError("Min 6 chars"); return; }
        if (!password.equals(confirmPassword)) { etConfirmPassword.setError("Mismatch"); return; }

        btnRegister.setEnabled(false);

        // Check for duplicate phone in Firestore
        db.collection("users").whereEqualTo("phone", sanitizedPhone).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Phone number already registered!", Toast.LENGTH_LONG).show();
                btnRegister.setEnabled(true);
                return;
            }

            // Create Auth User
            mAuth.createUserWithEmailAndPassword(finalFirebaseEmail, password).addOnCompleteListener(this, authTask -> {
                if (authTask.isSuccessful()) {
                    String userId = mAuth.getCurrentUser().getUid();
                    Map<String, Object> user = new HashMap<>();
                    user.put("userId", userId);
                    user.put("fullName", fullName);
                    user.put("email", emailInput);
                    user.put("phone", sanitizedPhone);
                    user.put("role", role);
                    user.put("isOnDuty", true); // Set default duty status

                    db.collection("users").document(userId).set(user).addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        }
                    });
                } else {
                    Toast.makeText(RegisterActivity.this, "Error: Account already exists!", Toast.LENGTH_LONG).show();
                    btnRegister.setEnabled(true);
                }
            });
        });
    }

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