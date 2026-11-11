package com.example.load_app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.load_app.R;
import com.example.load_app.models.Package;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PackageDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_details);

        String pkgId = getIntent().getStringExtra("PACKAGE_ID");
        if (pkgId == null) { finish(); return; }

        TextView tvId = findViewById(R.id.tvDetailPkgId);
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        TextView tvPriority = findViewById(R.id.tvDetailPriority);
        TextView tvSenderName = findViewById(R.id.tvDetailSenderName);
        TextView tvSenderPhone = findViewById(R.id.tvDetailSenderPhone);
        TextView tvRecvName = findViewById(R.id.tvDetailRecvName);
        TextView tvRecvPhone = findViewById(R.id.tvDetailRecvPhone);
        TextView tvRoute = findViewById(R.id.tvDetailRoute);
        TextView tvRider = findViewById(R.id.tvDetailRider);
        TextView tvFinancials = findViewById(R.id.tvDetailFinancials);
        TextView tvOtps = findViewById(R.id.tvDetailOtps);
        ImageView ivProof = findViewById(R.id.ivDetailProof);
        View cardFin = findViewById(R.id.cardFin);
        View cardOtp = findViewById(R.id.cardOtp);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(userDoc -> {
                String role = userDoc.getString("role");

                db.collection("packages").document(pkgId).get().addOnSuccessListener(doc -> {
                    Package p = doc.toObject(Package.class);
                    if (p != null) {
                        tvId.setText(p.getPackageId());
                        tvStatus.setText(p.getStatus());
                        tvPriority.setText(p.getPriority());

                        if ("NORMAL".equals(p.getPriority())) tvPriority.setBackgroundColor(getResources().getColor(R.color.textSecondary));

                        tvSenderName.setText(p.getCustomerName() != null ? p.getCustomerName() : "Walk-in");
                        tvSenderPhone.setText(p.getCustomerPhone() != null ? p.getCustomerPhone() : "N/A");
                        tvRecvName.setText(p.getReceiverName() != null ? p.getReceiverName() : "Customer");
                        tvRecvPhone.setText(p.getReceiverPhone());

                        tvRoute.setText("Pickup: " + p.getPickupAddress() + "\nDrop: " + p.getDropAddress() + "\nWeight: " + p.getWeightKg() + " kg");

                        if (p.getAssignedRiderId() != null) {
                            db.collection("users").whereEqualTo("fullName", p.getAssignedRiderId()).get().addOnSuccessListener(snapshots -> {
                                if (!snapshots.isEmpty()) {
                                    String phone = snapshots.getDocuments().get(0).getString("phone");
                                    tvRider.setText(p.getAssignedRiderId() + "\n" + (phone != null ? phone : "N/A"));
                                } else {
                                    tvRider.setText(p.getAssignedRiderId() + "\nPhone N/A");
                                }
                            });
                        } else {
                            tvRider.setText("Not Assigned Yet");
                        }

                        if ("RIDER".equals(role)) {
                            tvFinancials.setText("Your Income: ৳" + p.getRiderFee());
                        } else if ("CUSTOMER".equals(role)) {
                            tvFinancials.setText("Delivery Cost: ৳" + p.getCustomerPrice());
                        } else {
                            tvFinancials.setText("Customer Paid: ৳" + p.getCustomerPrice() + "\nRider Fee: ৳" + p.getRiderFee() + "\nNet Profit: ৳" + p.getNetProfit());
                        }

                        tvOtps.setText("Pickup OTP: " + p.getPickupOtp() + "\nDelivery OTP: " + p.getDeliveryOtp());

                        if ("RIDER".equals(role)) cardOtp.setVisibility(View.GONE);

                        String b64 = p.getDeliveryPhotoUrl() != null ? p.getDeliveryPhotoUrl() : p.getPickupPhotoUrl();
                        if (b64 != null && !b64.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(b64, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                ivProof.setImageBitmap(decodedByte);
                                ivProof.setVisibility(View.VISIBLE);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                });
            });
        }
    }
}