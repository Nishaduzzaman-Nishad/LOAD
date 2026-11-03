package com.example.load_app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.load_app.R;
import com.example.load_app.models.Package;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

public class DeliveryActivity extends AppCompatActivity {
    private TextView tvPackageId, tvPickupAddress, tvDropAddress, tvWeight, tvOtpDisplay;
    private Button btnNavigate, btnCall, btnTakePhoto, btnCompleteDelivery;
    private ImageView ivProofPhoto;
    private Package currentPackage;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey("AIzaSyBzGU3NJV8-y5wDwmn3jOTw95e88HKxZeM")
                    .setApplicationId("1:853658382959:android:b6b6c6ce5c5caa928e139a")
                    .setProjectId("load-2420e")
                    .setStorageBucket("load-2420e.firebasestorage.app")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }

        setContentView(R.layout.activity_delivery);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        String packageId = getIntent().getStringExtra("PACKAGE_ID");

        if (packageId == null) { finish(); return; }

        tvPackageId = findViewById(R.id.tvPackageId);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDropAddress = findViewById(R.id.tvDropAddress);
        tvWeight = findViewById(R.id.tvWeight);
        tvOtpDisplay = findViewById(R.id.tvOtpDisplay);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnCall = findViewById(R.id.btnCall);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnCompleteDelivery = findViewById(R.id.btnCompleteDelivery);
        ivProofPhoto = findViewById(R.id.ivProofPhoto);

        db.collection("packages").document(packageId).get().addOnSuccessListener(doc -> {
            currentPackage = doc.toObject(Package.class);
            if (currentPackage != null) {
                tvPackageId.setText("Package: " + currentPackage.getPackageId());
                tvPickupAddress.setText("Pickup: " + currentPackage.getPickupAddress());
                tvDropAddress.setText("Drop: " + currentPackage.getDropAddress());
                tvWeight.setText("Weight: " + currentPackage.getWeightKg() + " kg");
                tvOtpDisplay.setText("Secret OTP: " + currentPackage.getOtp());

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        });

        btnCompleteDelivery.setOnClickListener(v -> showOtpDialog());
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No camera app found on device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera Permission is required to take photo", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                if (photo != null) {
                    ivProofPhoto.setImageBitmap(photo);
                    ivProofPhoto.setVisibility(View.VISIBLE);
                    uploadImageToFirebase(photo);
                }
            }
        }
    }

    private void uploadImageToFirebase(Bitmap bitmap) {
        if (currentPackage == null) return;

        Toast.makeText(this, "Uploading Photo...", Toast.LENGTH_SHORT).show();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] data = baos.toByteArray();

        String path = "proofs/" + currentPackage.getPackageId() + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                String url = uri.toString();
                if (currentPackage.getStatus().equals(Package.STATUS_ASSIGNED)) {
                    currentPackage.setStatus(Package.STATUS_PICKED_UP);
                    currentPackage.setPickupPhotoUrl(url);
                } else {
                    currentPackage.setDeliveryPhotoUrl(url);
                }
                db.collection("packages").document(currentPackage.getPackageId()).set(currentPackage);
                Toast.makeText(this, "Photo Uploaded & Status Updated!", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void showOtpDialog() {
        if (currentPackage == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Receiver OTP");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String enteredOtp = input.getText().toString().trim();
            if (enteredOtp.equals(currentPackage.getOtp())) {
                currentPackage.setStatus(Package.STATUS_DELIVERED);
                db.collection("packages").document(currentPackage.getPackageId()).set(currentPackage)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Delivery Completed Successfully!", Toast.LENGTH_LONG).show();
                            finish();
                        });
            } else {
                Toast.makeText(this, "Invalid OTP! Please ask the customer.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}