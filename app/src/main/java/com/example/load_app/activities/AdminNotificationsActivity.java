package com.example.load_app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.load_app.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminNotificationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notifications);

        RecyclerView rv = findViewById(R.id.rvAdminNotifications);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<DocumentSnapshot> notifications = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("admin_notifications").get().addOnSuccessListener(alerts -> {
            db.collection("withdraw_requests").whereEqualTo("status", "PENDING").get().addOnSuccessListener(withdraws -> {
                notifications.addAll(alerts.getDocuments());
                notifications.addAll(withdraws.getDocuments());

                notifications.sort((d1, d2) -> {
                    Long t1 = d1.getLong("timestamp");
                    Long t2 = d2.getLong("timestamp");
                    if (t1 == null) t1 = 0L; if (t2 == null) t2 = 0L;
                    return Long.compare(t2, t1);
                });

                rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        DocumentSnapshot doc = notifications.get(position);
                        TextView tv1 = holder.itemView.findViewById(android.R.id.text1);
                        TextView tv2 = holder.itemView.findViewById(android.R.id.text2);

                        if (doc.contains("amount")) {
                            String rider = doc.getString("riderName");
                            Double amt = doc.getDouble("amount");
                            String riderId = doc.getString("riderId");

                            tv1.setText("Withdraw Request: ৳" + amt + " from " + rider);
                            tv1.setTextColor(getResources().getColor(R.color.successGreen));
                            tv2.setText("Tap to Approve");

                            holder.itemView.setOnClickListener(v -> {
                                new AlertDialog.Builder(AdminNotificationsActivity.this)
                                        .setTitle("Approve Withdrawal")
                                        .setMessage("Approve ৳" + amt + " for " + rider + "?")
                                        .setPositiveButton("Approve", (d, w) -> {
                                            Map<String, Object> update = new HashMap<>();
                                            update.put("status", "APPROVED");
                                            db.collection("withdraw_requests").document(doc.getId()).update(update);

                                            // Deduct from Rider's Balance
                                            db.collection("users").document(riderId).get().addOnSuccessListener(userDoc -> {
                                                double withdrawn = userDoc.getDouble("totalWithdrawn") != null ? userDoc.getDouble("totalWithdrawn") : 0.0;
                                                db.collection("users").document(riderId).update("totalWithdrawn", withdrawn + amt);
                                            });

                                            Toast.makeText(AdminNotificationsActivity.this, "Approved & Balance Deducted!", Toast.LENGTH_SHORT).show();
                                            recreate();
                                        }).show();
                            });
                        } else {
                            tv1.setText("⚠️ Rider Alert");
                            tv1.setTextColor(getResources().getColor(R.color.errorRed));
                            tv2.setText(doc.getString("message"));
                        }
                    }

                    @Override
                    public int getItemCount() { return notifications.size(); }
                });
            });
        });
    }
}