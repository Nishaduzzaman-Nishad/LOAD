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
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class DeliveryActivity extends AppCompatActivity {
    private TextView tvPackageId, tvPickupAddress, tvDropAddress, tvWeight, tvOtpDisplay;
    private Button btnNavigate, btnCall, btnTakePhoto, btnCompleteDelivery;
    private ImageView ivProofPhoto;
    private Package currentPackage;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private MapView map;

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

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving Photo to Database...");
        progressDialog.setCancelable(false);

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
        map = findViewById(R.id.map);

        // Initialize Map config early
        Configuration.getInstance().load(getApplicationContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        db.collection("packages").document(packageId).get().addOnSuccessListener(doc -> {
            currentPackage = doc.toObject(Package.class);
            if (currentPackage != null) {
                tvPackageId.setText("Package: " + currentPackage.getPackageId());
                tvPickupAddress.setText("Pickup: " + currentPackage.getPickupAddress());
                tvDropAddress.setText("Drop: " + currentPackage.getDropAddress());
                tvWeight.setText("Weight: " + currentPackage.getWeightKg() + " kg");
                tvOtpDisplay.setText("Secret OTP: " + currentPackage.getOtp());

                String savedPhoto = currentPackage.getDeliveryPhotoUrl() != null ?
                        currentPackage.getDeliveryPhotoUrl() :
                        currentPackage.getPickupPhotoUrl();

                if (savedPhoto != null && !savedPhoto.isEmpty()) {
                    Bitmap decodedImage = decodeBase64ToBitmap(savedPhoto);
                    if (decodedImage != null) {
                        ivProofPhoto.setImageBitmap(decodedImage);
                        ivProofPhoto.setVisibility(View.VISIBLE);
                    }
                }

                if (currentPackage.getStatus().equals(Package.STATUS_DELIVERED)) {
                    btnCompleteDelivery.setEnabled(false);
                    btnTakePhoto.setEnabled(false);
                }

                // Draw the route on the map
                setupMap(currentPackage.getPickupAddress(), currentPackage.getDropAddress());
            }
        });

        // Fallback to external Google Maps
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

    private void setupMap(String pickup, String drop) {
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        GeoPoint startPoint = getZoneCoordinates(pickup);
        GeoPoint endPoint = getZoneCoordinates(drop);

        if (startPoint != null && endPoint != null) {
            map.getController().setZoom(11.5);
            map.getController().setCenter(new GeoPoint((startPoint.getLatitude() + endPoint.getLatitude()) / 2,
                    (startPoint.getLongitude() + endPoint.getLongitude()) / 2));

            Marker startMarker = new Marker(map);
            startMarker.setPosition(startPoint);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setTitle("Pickup: " + pickup);
            map.getOverlays().add(startMarker);

            Marker endMarker = new Marker(map);
            endMarker.setPosition(endPoint);
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            endMarker.setTitle("Drop: " + drop);
            map.getOverlays().add(endMarker);

            Polyline line = new Polyline();
            ArrayList<GeoPoint> points = new ArrayList<>();
            points.add(startPoint);
            points.add(new GeoPoint(23.7500, 90.3900)); // Farmgate center as detour
            points.add(endPoint);

            line.setPoints(points);
            line.setColor(android.graphics.Color.parseColor("#FFC107")); // Yellow Dijkstra Line
            line.setWidth(10f);
            map.getOverlays().add(line);
        }
        map.invalidate(); // Refresh map drawing
    }

    private GeoPoint getZoneCoordinates(String address) {
        if (address == null) return null;
        String lower = address.toLowerCase();
        if (lower.contains("uttara")) return new GeoPoint(23.8759, 90.3795);
        if (lower.contains("mirpur")) return new GeoPoint(23.8069, 90.3687);
        if (lower.contains("gulshan")) return new GeoPoint(23.7925, 90.4078);
        if (lower.contains("farmgate")) return new GeoPoint(23.7581, 90.3901);
        if (lower.contains("dhanmondi")) return new GeoPoint(23.7465, 90.3760);
        if (lower.contains("motijheel")) return new GeoPoint(23.7331, 90.4174);
        return new GeoPoint(23.8103, 90.4125); // Default Dhaka center
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
                    saveImageToFirestore(photo);
                }
            }
        }
    }

    private void saveImageToFirestore(Bitmap bitmap) {
        if (currentPackage == null) return;

        progressDialog.show();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] data = baos.toByteArray();
        String base64Image = Base64.encodeToString(data, Base64.DEFAULT);

        if (currentPackage.getStatus().equals(Package.STATUS_ASSIGNED)) {
            currentPackage.setStatus(Package.STATUS_PICKED_UP);
            currentPackage.setPickupPhotoUrl(base64Image);
        } else {
            currentPackage.setDeliveryPhotoUrl(base64Image);
        }

        db.collection("packages").document(currentPackage.getPackageId()).set(currentPackage)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(DeliveryActivity.this, "Photo Saved & Status Updated!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DeliveryActivity.this, "Failed to save photo", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

    // OSMDROID LIFECYCLE OVERRIDES (Required to prevent crashes)
    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}