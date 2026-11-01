package com.example.load_app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.load_app.R;
import com.example.load_app.models.Package;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeliveryActivity extends AppCompatActivity {
    private TextView tvPackageId, tvPickupAddress, tvDropAddress, tvWeight;
    private Button btnNavigate, btnCall, btnTakePhoto, btnCompleteDelivery;
    private Package currentPackage;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery);

        db = FirebaseFirestore.getInstance();
        String packageId = getIntent().getStringExtra("PACKAGE_ID");
        if (packageId == null) { finish(); return; }

        tvPackageId = findViewById(R.id.tvPackageId);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDropAddress = findViewById(R.id.tvDropAddress);
        tvWeight = findViewById(R.id.tvWeight);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnCall = findViewById(R.id.btnCall);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnCompleteDelivery = findViewById(R.id.btnCompleteDelivery);

        db.collection("packages").document(packageId).get().addOnSuccessListener(doc -> {
            currentPackage = doc.toObject(Package.class);
            if (currentPackage != null) {
                tvPackageId.setText("Package: " + currentPackage.getPackageId());
                tvPickupAddress.setText("Pickup: " + currentPackage.getPickupAddress());
                tvDropAddress.setText("Drop: " + currentPackage.getDropAddress());
                tvWeight.setText("Weight: " + currentPackage.getWeightKg() + " kg");
                if (currentPackage.getStatus().equals(Package.STATUS_DELIVERED)) {
                    btnCompleteDelivery.setEnabled(false);
                    btnTakePhoto.setEnabled(false);
                }
            }
        });

        btnNavigate.setOnClickListener(v -> {
            if (currentPackage != null) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(currentPackage.getDropAddress()));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        btnCall.setOnClickListener(v -> {
            if (currentPackage != null) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + currentPackage.getReceiverPhone()));
                startActivity(callIntent);
            }
        });

        btnTakePhoto.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, 101);
        });

        btnCompleteDelivery.setOnClickListener(v -> showOtpDialog());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
            if (currentPackage != null && currentPackage.getStatus().equals(Package.STATUS_ASSIGNED)) {
                currentPackage.setStatus(Package.STATUS_PICKED_UP);
                db.collection("packages").document(currentPackage.getPackageId()).set(currentPackage);
                Toast.makeText(this, "Status: PICKED UP", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showOtpDialog() {
        if (currentPackage == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Receiver OTP");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            if (input.getText().toString().trim().equals("1234")) {
                currentPackage.setStatus(Package.STATUS_DELIVERED);
                db.collection("packages").document(currentPackage.getPackageId()).set(currentPackage)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Delivery Completed!", Toast.LENGTH_LONG).show();
                            finish();
                        });
            } else {
                Toast.makeText(this, "Invalid OTP! Try 1234", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}