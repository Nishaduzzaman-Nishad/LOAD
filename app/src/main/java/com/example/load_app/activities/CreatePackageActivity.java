package com.example.load_app.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.load_app.R;
import com.example.load_app.models.Package;
import com.example.load_app.models.ValidationUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Random;

public class CreatePackageActivity extends AppCompatActivity {
    private EditText etPickup, etDrop, etPhone, etWeight, etType;
    private Spinner spinnerPriority;
    private Button btnSave, btnPickContact;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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

        setContentView(R.layout.activity_create_package);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etPickup = findViewById(R.id.etPickup);
        etDrop = findViewById(R.id.etDrop);
        etPhone = findViewById(R.id.etPhone);
        etWeight = findViewById(R.id.etWeight);
        etType = findViewById(R.id.etType);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        btnSave = findViewById(R.id.btnSave);
        btnPickContact = findViewById(R.id.btnPickContact);

        String[] priorities = {"NORMAL", "URGENT"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);

        // Trigger System Content Provider for Contacts
        btnPickContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, 1001);
        });

        btnSave.setOnClickListener(v -> savePackage());
    }

    // Handle the result from the System Content Provider
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
            // Query the provider
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(numberIndex);
                etPhone.setText(number);
                cursor.close();
            }
        }
    }

    private void savePackage() {
        String pickup = etPickup.getText().toString().trim();
        String drop = etDrop.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String type = etType.getText().toString().trim();
        String priority = spinnerPriority.getSelectedItem().toString();

        if (pickup.isEmpty()) { etPickup.setError("Required"); return; }
        if (drop.isEmpty()) { etDrop.setError("Required"); return; }
        String phoneError = ValidationUtils.getPhoneError(phone);
        if (phoneError != null) { etPhone.setError(phoneError); return; }
        if (weightStr.isEmpty()) { etWeight.setError("Required"); return; }

        double weight = Double.parseDouble(weightStr);
        String weightError = ValidationUtils.getWeightError(weight);
        if (weightError != null) { etWeight.setError(weightError); return; }
        if (type.isEmpty()) { etType.setError("Required"); return; }

        int randomNum = new Random().nextInt(900) + 100;
        String packageId = "P" + randomNum;

        // GENERATE 4-DIGIT OTP
        int otpNum = new Random().nextInt(9000) + 1000;
        String generatedOtp = String.valueOf(otpNum);

        String customerId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "UNKNOWN";

        Package newPackage = new Package(packageId, pickup, drop, phone, weight, type, priority, customerId);
        newPackage.setOtp(generatedOtp);

        btnSave.setEnabled(false);
        db.collection("packages").document(packageId).set(newPackage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        new AlertDialog.Builder(CreatePackageActivity.this)
                                .setTitle("Success")
                                .setMessage("Package Added!\nOTP for Delivery: " + generatedOtp)
                                .setPositiveButton("OK", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    } else {
                        Toast.makeText(CreatePackageActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                    }
                });
    }
}