package com.example.load_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.load_app.R;
import com.example.load_app.models.Package;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RiderRevenueActivity extends AppCompatActivity {

    private double availableBalance = 0.0;
    private String currentRiderName = "";
    private String currentRiderId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_revenue);

        RecyclerView rv = findViewById(R.id.rvRiderRevenue);
        rv.setLayoutManager(new LinearLayoutManager(this));

        TextView tvTotalProfit = findViewById(R.id.tvTotalRiderIncome);
        TextView tvTotalVolume = findViewById(R.id.tvTotalRiderVolume);
        Button btnWithdraw = findViewById(R.id.btnWithdraw);

        List<Package> deliveredPackages = new ArrayList<>();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) { finish(); return; }

        currentRiderId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(currentRiderId).get().addOnSuccessListener(userDoc -> {
            currentRiderName = userDoc.getString("fullName");
            double penalty = userDoc.getDouble("totalPenalty") != null ? userDoc.getDouble("totalPenalty") : 0.0;
            double withdrawn = userDoc.getDouble("totalWithdrawn") != null ? userDoc.getDouble("totalWithdrawn") : 0.0;

            db.collection("packages").whereEqualTo("status", "DELIVERED").whereEqualTo("assignedRiderId", currentRiderName).get().addOnSuccessListener(snapshots -> {
                double grossEarned = 0;
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Package p = doc.toObject(Package.class);
                    if (p != null) {
                        deliveredPackages.add(p);
                        grossEarned += p.getRiderFee();
                    }
                }

                availableBalance = grossEarned - withdrawn - penalty;

                tvTotalProfit.setText("৳" + String.format("%.0f", availableBalance));
                tvTotalVolume.setText("Gross: ৳" + String.format("%.0f", grossEarned) + " | Penalty: ৳" + String.format("%.0f", penalty));

                deliveredPackages.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));

                rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_revenue, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        Package p = deliveredPackages.get(position);
                        TextView tvId = holder.itemView.findViewById(R.id.tvRevPkgId);
                        TextView tvDate = holder.itemView.findViewById(R.id.tvRevDate);
                        TextView tvProfit = holder.itemView.findViewById(R.id.tvRevProfit);

                        tvId.setText(p.getPackageId());
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        tvDate.setText(sdf.format(new Date(p.getCreatedAt())));
                        tvProfit.setText("+ ৳" + String.format("%.0f", p.getRiderFee()));
                        tvProfit.setTextColor(getResources().getColor(R.color.successGreen));
                    }

                    @Override
                    public int getItemCount() { return deliveredPackages.size(); }
                });
            });
        });

        btnWithdraw.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Withdraw Balance");
            builder.setMessage("Available: ৳" + availableBalance + "\nEnter amount to withdraw:");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            builder.setView(input);

            builder.setPositiveButton("Request", (dialog, which) -> {
                String amountStr = input.getText().toString();
                if (amountStr.isEmpty()) return;
                double amount = Double.parseDouble(amountStr);

                if (amount > availableBalance) {
                    Toast.makeText(this, "Insufficient balance!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> request = new HashMap<>();
                request.put("riderId", currentRiderId);
                request.put("riderName", currentRiderName);
                request.put("amount", amount);
                request.put("status", "PENDING");
                request.put("timestamp", System.currentTimeMillis());

                db.collection("withdraw_requests").add(request).addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Withdrawal requested! Waiting for Admin approval.", Toast.LENGTH_LONG).show();
                });
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });
    }
}