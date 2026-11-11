package com.example.load_app.activities;

import android.content.Intent;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RevenueActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_list);

        RecyclerView rv = findViewById(R.id.rvRevenue);
        rv.setLayoutManager(new LinearLayoutManager(this));

        TextView tvTotalProfit = findViewById(R.id.tvTotalRevProfit);
        TextView tvTotalVolume = findViewById(R.id.tvTotalRevVolume);

        List<Object> historyList = new ArrayList<>();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch delivered packages
        db.collection("packages").whereEqualTo("status", "DELIVERED").get().addOnSuccessListener(pkgSnapshots -> {
            for (DocumentSnapshot doc : pkgSnapshots.getDocuments()) {
                Package p = doc.toObject(Package.class);
                if (p != null) historyList.add(p);
            }

            // Fetch penalties
            db.collection("platform_earnings").get().addOnSuccessListener(penaltySnapshots -> {
                for (DocumentSnapshot doc : penaltySnapshots.getDocuments()) {
                    historyList.add(doc);
                }

                // Calculate Total
                double totalProfit = 0;
                for (Object obj : historyList) {
                    if (obj instanceof Package) {
                        totalProfit += ((Package) obj).getNetProfit();
                    } else if (obj instanceof DocumentSnapshot) {
                        Double amt = ((DocumentSnapshot) obj).getDouble("amount");
                        if (amt != null) totalProfit += amt;
                    }
                }

                tvTotalProfit.setText("৳" + String.format("%.0f", totalProfit));
                tvTotalVolume.setText("Across " + historyList.size() + " Transactions");

                // Sort by timestamp
                historyList.sort((o1, o2) -> {
                    long t1 = (o1 instanceof Package) ? ((Package)o1).getCreatedAt() : ((DocumentSnapshot)o1).getLong("timestamp");
                    long t2 = (o2 instanceof Package) ? ((Package)o2).getCreatedAt() : ((DocumentSnapshot)o2).getLong("timestamp");
                    return Long.compare(t2, t1);
                });

                rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_revenue, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        Object item = historyList.get(position);
                        TextView tvId = holder.itemView.findViewById(R.id.tvRevPkgId);
                        TextView tvDate = holder.itemView.findViewById(R.id.tvRevDate);
                        TextView tvProfit = holder.itemView.findViewById(R.id.tvRevProfit);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                        if (item instanceof Package) {
                            Package p = (Package) item;
                            tvId.setText(p.getPackageId());
                            tvDate.setText(sdf.format(new Date(p.getCreatedAt())));
                            tvProfit.setText("+ ৳" + String.format("%.0f", p.getNetProfit()));
                            tvProfit.setTextColor(getResources().getColor(R.color.successGreen));
                        } else if (item instanceof DocumentSnapshot) {
                            DocumentSnapshot doc = (DocumentSnapshot) item;
                            tvId.setText("Penalty: " + doc.getString("reason"));
                            tvDate.setText(sdf.format(new Date(doc.getLong("timestamp"))));
                            tvProfit.setText("+ ৳" + String.format("%.0f", doc.getDouble("amount")));
                            tvProfit.setTextColor(getResources().getColor(R.color.successGreen));
                        }
                    }

                    @Override
                    public int getItemCount() { return historyList.size(); }
                });
            });
        });
    }
}