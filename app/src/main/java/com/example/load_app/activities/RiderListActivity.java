package com.example.load_app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.load_app.R;
import com.example.load_app.models.Package;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiderListActivity extends AppCompatActivity {

    public static class RiderStats {
        String name, phone;
        double load = 0, earned = 0;
        int assigned = 0, delivered = 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_list);

        RecyclerView rv = findViewById(R.id.rvRiders);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<RiderStats> riderStatsList = new ArrayList<>();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").whereEqualTo("role", "RIDER").get().addOnSuccessListener(userSnapshots -> {
            Map<String, RiderStats> statsMap = new HashMap<>();
            for (DocumentSnapshot doc : userSnapshots.getDocuments()) {
                RiderStats stats = new RiderStats();
                stats.name = doc.getString("fullName");
                stats.phone = doc.getString("phone");
                if (stats.name != null) statsMap.put(stats.name, stats);
            }

            db.collection("packages").get().addOnSuccessListener(pkgSnapshots -> {
                for (DocumentSnapshot doc : pkgSnapshots.getDocuments()) {
                    Package p = doc.toObject(Package.class);
                    if (p != null && p.getAssignedRiderId() != null && statsMap.containsKey(p.getAssignedRiderId())) {
                        RiderStats stats = statsMap.get(p.getAssignedRiderId());
                        if (Package.STATUS_ASSIGNED.equals(p.getStatus()) || Package.STATUS_PICKED_UP.equals(p.getStatus())) {
                            stats.load += p.getWeightKg();
                            stats.assigned++;
                        } else if (Package.STATUS_DELIVERED.equals(p.getStatus())) {
                            stats.delivered++;
                            stats.earned += p.getRiderFee();
                        }
                    }
                }

                riderStatsList.addAll(statsMap.values());

                // Issue 2 Fix: Ascending order (lowest load first)
                riderStatsList.sort((r1, r2) -> Double.compare(r1.load, r2.load));

                rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rider, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        RiderStats r = riderStatsList.get(position);
                        TextView tvName = holder.itemView.findViewById(R.id.tvRiderName);
                        TextView tvPhone = holder.itemView.findViewById(R.id.tvRiderPhone);
                        TextView tvLoad = holder.itemView.findViewById(R.id.tvRiderLoad);
                        TextView tvAssigned = holder.itemView.findViewById(R.id.tvRiderAssigned);
                        TextView tvEarned = holder.itemView.findViewById(R.id.tvRiderEarned);

                        tvName.setText(r.name);
                        tvPhone.setText(r.phone);
                        tvLoad.setText(String.format("%.1f kg", r.load));
                        tvAssigned.setText(String.valueOf(r.assigned + r.delivered));
                        tvEarned.setText("৳" + String.format("%.0f", r.earned));
                    }

                    @Override
                    public int getItemCount() { return riderStatsList.size(); }
                });
            });
        });
    }
}