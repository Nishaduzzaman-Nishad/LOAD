package com.example.load_app.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class DeliveryActivity extends AppCompatActivity {
    private TextView tvPackageId, tvSenderInfo, tvReceiverInfo, tvPickupAddress, tvDropAddress, tvWeight, tvPhaseStatus;
    private Button btnNavigate, btnCall, btnTakePhoto, btnCompleteDelivery, btnUnassign;
    private ImageView ivProofPhoto;
    private Package currentPackage;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private boolean isPickupPhase = true;

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
                    .build();
            FirebaseApp.initializeApp(this, options);
        }

        setContentView(R.layout.activity_delivery);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving...");
        progressDialog.setCancelable(false);

        String packageId = getIntent().getStringExtra("PACKAGE_ID");
        if (packageId == null) { finish(); return; }

        tvPackageId = findViewById(R.id.tvPackageId);
        tvSenderInfo = findViewById(R.id.tvSenderInfo);
        tvReceiverInfo = findViewById(R.id.tvReceiverInfo);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDropAddress = findViewById(R.id.tvDropAddress);
        tvWeight = findViewById(R.id.tvWeight);
        tvPhaseStatus = findViewById(R.id.tvPhaseStatus);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnCall = findViewById(R.id.btnCall);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnCompleteDelivery = findViewById(R.id.btnCompleteDelivery);
        btnUnassign = findViewById(R.id.btnUnassign);
        ivProofPhoto = findViewById(R.id.ivProofPhoto);

        loadPackageData(packageId);

        btnNavigate.setOnClickListener(v -> {
            if (currentPackage != null) {
                String target = isPickupPhase ? currentPackage.getPickupAddress() : currentPackage.getDropAddress();
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(target));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        btnCall.setOnClickListener(v -> {
            if (currentPackage != null) {
                String phoneToCall = isPickupPhase ? currentPackage.getCustomerPhone() : currentPackage.getReceiverPhone();
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phoneToCall));
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

        btnCompleteDelivery.setOnClickListener(v -> {
            if (isPickupPhase) showOtpDialog(true);
            else showOtpDialog(false);
        });

        btnUnassign.setOnClickListener(v -> {
            if (currentPackage == null) return;
            new AlertDialog.Builder(this)
                    .setTitle("Unassign Package?")
                    .setMessage("Are you sure? A 20% penalty (৳" + (currentPackage.getRiderFee() * 0.2) + ") will be deducted from your earnings.")
                    .setPositiveButton("Yes, Unassign", (dialog, which) -> {
                        double penalty = currentPackage.getRiderFee() * 0.2;

                        currentPackage.setStatus(Package.STATUS_PENDING);
                        currentPackage.setAssignedRiderId(null);
                        db.collection("packages").document(currentPackage.getPackageId()).set(currentPackage);

                        String uid = mAuth.getCurrentUser().getUid();
                        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                            double currentPenalty = doc.getDouble("totalPenalty") != null ? doc.getDouble("totalPenalty") : 0.0;
                            db.collection("users").document(uid).update("totalPenalty", currentPenalty + penalty);
                        });

                        Map<String, Object> platformEarning = new HashMap<>();
                        platformEarning.put("amount", penalty);
                        platformEarning.put("reason", "Unassign Penalty - " + currentPackage.getPackageId());
                        platformEarning.put("timestamp", System.currentTimeMillis());
                        db.collection("platform_earnings").add(platformEarning);

                        Map<String, Object> alert = new HashMap<>();
                        alert.put("message", "Rider unassigned Package " + currentPackage.getPackageId() + ". Penalty applied: ৳" + penalty);
                        alert.put("timestamp", System.currentTimeMillis());
                        db.collection("admin_notifications").add(alert);

                        Toast.makeText(this, "Unassigned. Penalty applied.", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadPackageData(String packageId) {
        db.collection("packages").document(packageId).get().addOnSuccessListener(doc -> {
            currentPackage = doc.toObject(Package.class);
            if (currentPackage != null) {
                tvPackageId.setText("Package: " + currentPackage.getPackageId());
                tvSenderInfo.setText("Sender: " + currentPackage.getCustomerName() + " (" + currentPackage.getCustomerPhone() + ")");
                tvReceiverInfo.setText("Receiver: " + (currentPackage.getReceiverName() != null ? currentPackage.getReceiverName() : "Customer") + " (" + currentPackage.getReceiverPhone() + ")");
                tvPickupAddress.setText("Pickup: " + currentPackage.getPickupAddress());
                tvDropAddress.setText("Drop: " + currentPackage.getDropAddress());
                tvWeight.setText("Weight: " + currentPackage.getWeightKg() + " kg");

                isPickupPhase = Package.STATUS_ASSIGNED.equals(currentPackage.getStatus());

                if (isPickupPhase) {
                    tvPhaseStatus.setText("PHASE 1: ARRIVED AT PICKUP");
                    tvPhaseStatus.setBackgroundColor(getResources().getColor(R.color.primary));
                    btnCompleteDelivery.setText("VERIFY PICKUP OTP");
                    btnNavigate.setText("NAVIGATE TO PICKUP");
                    btnCall.setText("CALL SENDER");
                    ivProofPhoto.setVisibility(View.GONE);
                } else {
                    tvPhaseStatus.setText("PHASE 2: DELIVER TO CUSTOMER");
                    tvPhaseStatus.setBackgroundColor(getResources().getColor(R.color.successGreen));
                    btnCompleteDelivery.setText("VERIFY DELIVERY OTP");
                    btnNavigate.setText("NAVIGATE TO DROP");
                    btnCall.setText("CALL RECEIVER");

                    if (currentPackage.getPickupPhotoUrl() != null) {
                        ivProofPhoto.setImageBitmap(decodeBase64ToBitmap(currentPackage.getPickupPhotoUrl()));
                        ivProofPhoto.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void showOtpDialog(boolean isPickup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isPickup ? "Enter Pickup OTP" : "Enter Delivery OTP");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String enteredOtp = input.getText().toString().trim();
            String correctOtp = isPickup ? currentPackage.getPickupOtp() : currentPackage.getDeliveryOtp();

            if (correctOtp != null && enteredOtp.equals(correctOtp)) {
                if (isPickup) {
                    if (ivProofPhoto.getVisibility() != View.VISIBLE) {
                        Toast.makeText(this, "Please take a pickup photo first!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currentPackage.setStatus(Package.STATUS_PICKED_UP);
                    db.collection("packages").document(currentPackage.getPackageId()).set(currentPackage)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Pickup Verified!", Toast.LENGTH_LONG).show();
                                loadPackageData(currentPackage.getPackageId());
                            });
                } else {
                    currentPackage.setStatus(Package.STATUS_DELIVERED);
                    db.collection("packages").document(currentPackage.getPackageId()).set(currentPackage)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Delivery Completed!", Toast.LENGTH_LONG).show();
                                finish();
                            });
                }
            } else {
                Toast.makeText(this, "Invalid OTP!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getExtras() != null) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            if (photo != null) {
                Bitmap resizedPhoto = getResizedBitmap(photo, 800);
                ivProofPhoto.setImageBitmap(resizedPhoto);
                ivProofPhoto.setVisibility(View.VISIBLE);
                saveImageToFirestore(resizedPhoto);
            }
        }
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize; height = (int) (width / bitmapRatio);
        } else {
            height = maxSize; width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void saveImageToFirestore(Bitmap bitmap) {
        if (currentPackage == null) return;
        progressDialog.show();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        if (isPickupPhase) currentPackage.setPickupPhotoUrl(base64Image);
        else currentPackage.setDeliveryPhotoUrl(base64Image);

        db.collection("packages").document(currentPackage.getPackageId()).set(currentPackage)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) Toast.makeText(DeliveryActivity.this, "Photo Saved!", Toast.LENGTH_SHORT).show();
                });
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) { return null; }
    }
}