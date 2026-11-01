package com.example.load_app.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.load_app.R;
import com.example.load_app.models.Package;
import com.example.load_app.models.ValidationUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Random;

public class CreatePackageActivity extends AppCompatActivity {
    private EditText etPickup, etDrop, etPhone, etWeight, etType;
    private Spinner spinnerPriority;
    private Button btnSave;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_package);

        db = FirebaseFirestore.getInstance();
        etPickup = findViewById(R.id.etPickup);
        etDrop = findViewById(R.id.etDrop);
        etPhone = findViewById(R.id.etPhone);
        etWeight = findViewById(R.id.etWeight);
        etType = findViewById(R.id.etType);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        btnSave = findViewById(R.id.btnSave);

        String[] priorities = {"NORMAL", "URGENT"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);

        btnSave.setOnClickListener(v -> savePackage());
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
        Package newPackage = new Package(packageId, pickup, drop, phone, weight, type, priority);

        btnSave.setEnabled(false);
        db.collection("packages").document(packageId).set(newPackage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        new AlertDialog.Builder(CreatePackageActivity.this)
                                .setTitle("Success")
                                .setMessage("Package has been added to the system!")
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