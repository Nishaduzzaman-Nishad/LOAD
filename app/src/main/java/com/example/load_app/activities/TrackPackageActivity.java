package com.example.load_app.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.load_app.R;
import com.example.load_app.models.Package;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TrackPackageActivity extends AppCompatActivity {

    private String currentPkgId;
    private String currentRiderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_package);

        currentPkgId = getIntent().getStringExtra("PKG_ID");

        TextView tvPkgId = findViewById(R.id.tvTrackPkgId);
        TextView tvDate = findViewById(R.id.tvTrackDate);
        TextView tvRiderName = findViewById(R.id.tvTrackRiderName);
        TextView tvRiderPhone = findViewById(R.id.tvTrackRiderPhone);
        TextView tvPickup = findViewById(R.id.tvTrackPickup);
        TextView tvDrop = findViewById(R.id.tvTrackDrop);
        TextView tvPickupOtp = findViewById(R.id.tvTrackPickupOtp);
        TextView tvDeliveryOtp = findViewById(R.id.tvTrackOtp);

        TextView stepPlaced = findViewById(R.id.stepPlaced);
        TextView stepAssigned = findViewById(R.id.stepAssigned);
        TextView stepPickedUp = findViewById(R.id.stepPickedUp);
        TextView stepDelivered = findViewById(R.id.stepDelivered);

        Button btnRateRider = findViewById(R.id.btnRateRider);

        tvPkgId.setText("Package: " + currentPkgId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("packages").document(currentPkgId).get().addOnSuccessListener(doc -> {
            Package pkg = doc.toObject(Package.class);
            if (pkg != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                tvDate.setText("Created: " + sdf.format(new Date(pkg.getCreatedAt())));

                tvPickup.setText("Pickup: " + pkg.getPickupAddress());
                tvDrop.setText("Drop: " + pkg.getDropAddress());

                if (pkg.getPickupOtp() != null) tvPickupOtp.setText("Pickup OTP: " + pkg.getPickupOtp());
                if (pkg.getDeliveryOtp() != null) tvDeliveryOtp.setText("Delivery OTP: " + pkg.getDeliveryOtp());

                if (pkg.getAssignedRiderId() != null && !pkg.getAssignedRiderId().isEmpty()) {
                    currentRiderName = pkg.getAssignedRiderId();
                    tvRiderName.setText("Rider: " + currentRiderName);
                    db.collection("users").whereEqualTo("fullName", currentRiderName).get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    String phone = queryDocumentSnapshots.getDocuments().get(0).getString("phone");
                                    tvRiderPhone.setText("Phone: " + (phone != null ? phone : "N/A"));
                                }
                            });
                }

                String status = pkg.getStatus();
                if (status != null) {
                    if (status.equals(Package.STATUS_PENDING)) {
                        stepPlaced.setTextColor(getResources().getColor(R.color.successGreen));
                    } else if (status.equals(Package.STATUS_ASSIGNED)) {
                        stepPlaced.setTextColor(getResources().getColor(R.color.successGreen));
                        stepAssigned.setTextColor(getResources().getColor(R.color.successGreen));
                        stepAssigned.setText("● Rider Assigned");
                        stepAssigned.setTypeface(null, Typeface.BOLD);
                    } else if (status.equals(Package.STATUS_PICKED_UP)) {
                        stepPlaced.setTextColor(getResources().getColor(R.color.successGreen));
                        stepAssigned.setTextColor(getResources().getColor(R.color.successGreen));
                        stepPickedUp.setTextColor(getResources().getColor(R.color.successGreen));
                        stepAssigned.setText("● Rider Assigned");
                        stepPickedUp.setText("● Picked Up");
                        stepPickedUp.setTypeface(null, Typeface.BOLD);
                    } else if (status.equals(Package.STATUS_DELIVERED)) {
                        stepPlaced.setTextColor(getResources().getColor(R.color.successGreen));
                        stepAssigned.setTextColor(getResources().getColor(R.color.successGreen));
                        stepPickedUp.setTextColor(getResources().getColor(R.color.successGreen));
                        stepDelivered.setTextColor(getResources().getColor(R.color.successGreen));
                        stepAssigned.setText("● Rider Assigned");
                        stepPickedUp.setText("● Picked Up");
                        stepDelivered.setText("● Delivered");
                        stepDelivered.setTypeface(null, Typeface.BOLD);

                        btnRateRider.setVisibility(View.VISIBLE);
                        btnRateRider.setOnClickListener(v -> showRatingDialog());
                    }
                }
            }
        });
    }

    private void showRatingDialog() {
        String[] ratings = {"⭐⭐⭐⭐⭐ Excellent", "⭐⭐⭐⭐ Good", "⭐⭐⭐ Average", "⭐⭐ Poor", "⭐ Terrible"};

        new AlertDialog.Builder(this)
                .setTitle("Rate Your Rider")
                .setItems(ratings, (dialog, which) -> {
                    Map<String, Object> ratingData = new HashMap<>();
                    ratingData.put("packageId", currentPkgId);
                    ratingData.put("riderName", currentRiderName);
                    ratingData.put("stars", 5 - which);
                    ratingData.put("timestamp", System.currentTimeMillis());

                    FirebaseFirestore.getInstance().collection("ratings").document(currentPkgId).set(ratingData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_LONG).show();
                                finish();
                            });
                }).show();
    }
}