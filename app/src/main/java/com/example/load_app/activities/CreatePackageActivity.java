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
import com.example.load_app.utils.PricingCalculator;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Random;

public class CreatePackageActivity extends AppCompatActivity {
    private EditText etCustName, etCustPhone, etRecvName, etRecvPhone, etPickup, etDrop, etWeight, etType;
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

        etCustName = findViewById(R.id.etCustName);
        etCustPhone = findViewById(R.id.etCustPhone);
        etPickup = findViewById(R.id.etPickup);
        etDrop = findViewById(R.id.etDrop);
        etRecvName = findViewById(R.id.etRecvName);
        etRecvPhone = findViewById(R.id.etPhone);
        etWeight = findViewById(R.id.etWeight);
        etType = findViewById(R.id.etType);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        btnSave = findViewById(R.id.btnSave);
        btnPickContact = findViewById(R.id.btnPickContact);

        String[] priorities = {"NORMAL", "URGENT"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);

        btnPickContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, 1001);
        });

        btnSave.setOnClickListener(v -> validateAndCalculate());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                etRecvPhone.setText(cursor.getString(numberIndex));
                cursor.close();
            }
        }
    }

    private void validateAndCalculate() {
        String custName = etCustName.getText().toString().trim();
        String custPhone = etCustPhone.getText().toString().trim();
        String recvName = etRecvName.getText().toString().trim();
        String recvPhone = etRecvPhone.getText().toString().trim();
        String pickup = etPickup.getText().toString().trim();
        String drop = etDrop.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String type = etType.getText().toString().trim();
        String priority = spinnerPriority.getSelectedItem().toString();

        if (custName.isEmpty() || custPhone.isEmpty() || recvName.isEmpty() || recvPhone.isEmpty() || pickup.isEmpty() || drop.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(weightStr);
        double price = PricingCalculator.calculateCustomerPrice(weight, priority);
        double riderFee = PricingCalculator.calculateRiderFee(price);
        double netProfit = PricingCalculator.calculateNetProfit(price);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delivery Cost");
        builder.setMessage("Package Weight: " + weight + " kg\nPriority: " + priority +
                "\n\nTotal Delivery Cost: ৳" + String.format("%.2f", price) +
                "\n\nDoes the customer agree to this price?");

        builder.setPositiveButton("Yes, Confirm & Save", (dialog, which) -> {
            savePackageToFirestore(custName, custPhone, recvName, recvPhone, pickup, drop, weight, type, priority, price, riderFee, netProfit);
        });

        builder.setNegativeButton("No, Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void savePackageToFirestore(String custName, String custPhone, String recvName, String recvPhone,
                                        String pickup, String drop, double weight, String type, String priority,
                                        double price, double riderFee, double netProfit) {

        int randomNum = new Random().nextInt(900) + 100;
        String packageId = "PKG-" + randomNum;

        String pickupOtp = String.valueOf(new Random().nextInt(9000) + 1000);
        String deliveryOtp = String.valueOf(new Random().nextInt(9000) + 1000);

        Package newPackage = new Package(packageId, pickup, drop, recvPhone, weight, type, priority, "WALK_IN");
        newPackage.setCustomerName(custName);
        newPackage.setCustomerPhone(custPhone);
        newPackage.setReceiverName(recvName);
        newPackage.setPickupOtp(pickupOtp);
        newPackage.setDeliveryOtp(deliveryOtp);
        newPackage.setCustomerPrice(price);
        newPackage.setRiderFee(riderFee);
        newPackage.setNetProfit(netProfit);

        btnSave.setEnabled(false);
        db.collection("packages").document(packageId).set(newPackage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        new AlertDialog.Builder(CreatePackageActivity.this)
                                .setTitle("Package Created Successfully")
                                .setMessage("Pickup OTP (Give to Sender): " + pickupOtp + "\n\nDelivery OTP (Give to Receiver): " + deliveryOtp)
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