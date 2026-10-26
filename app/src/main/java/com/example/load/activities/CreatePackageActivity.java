package com.example.load.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.load.R;
import com.example.load.models.Package;
import com.example.load.models.ValidationUtils;

import java.util.Random;

public class CreatePackageActivity extends AppCompatActivity {

    private EditText etPickup, etDrop, etPhone, etWeight, etType;
    private Spinner spinnerPriority;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_package);

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

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePackage();
            }
        });
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

        Intent returnIntent = new Intent();
        returnIntent.putExtra("NEW_PACKAGE_ID", newPackage.getPackageId());
        returnIntent.putExtra("NEW_PACKAGE_PICKUP", newPackage.getPickupAddress());
        returnIntent.putExtra("NEW_PACKAGE_DROP", newPackage.getDropAddress());
        returnIntent.putExtra("NEW_PACKAGE_PHONE", newPackage.getReceiverPhone());
        returnIntent.putExtra("NEW_PACKAGE_WEIGHT", newPackage.getWeightKg());
        returnIntent.putExtra("NEW_PACKAGE_TYPE", newPackage.getPackageType());
        returnIntent.putExtra("NEW_PACKAGE_PRIORITY", newPackage.getPriority());

        setResult(RESULT_OK, returnIntent);
        finish();
    }
}