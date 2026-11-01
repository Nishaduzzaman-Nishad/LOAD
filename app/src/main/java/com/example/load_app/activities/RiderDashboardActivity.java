package com.example.load_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load_app.R;
import com.example.load_app.adapters.PackageAdapter;
import com.example.load_app.models.Package;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class RiderDashboardActivity extends AppCompatActivity {
    private RecyclerView rvRiderPackages;
    private TextView tvRiderStats, tvEmptyState;
    private PackageAdapter adapter;
    private List<Package> riderPackages;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_dashboard);

        db = FirebaseFirestore.getInstance();
        rvRiderPackages = findViewById(R.id.rvRiderPackages);
        tvRiderStats = findViewById(R.id.tvRiderStats);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        riderPackages = new ArrayList<>();
        adapter = new PackageAdapter(riderPackages, null);
        adapter.setOnPackageClickListener(pkg -> {
            Intent intent = new Intent(RiderDashboardActivity.this, DeliveryActivity.class);
            intent.putExtra("PACKAGE_ID", pkg.getPackageId());
            startActivity(intent);
        });

        rvRiderPackages.setLayoutManager(new LinearLayoutManager(this));
        rvRiderPackages.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        db.collection("packages").addSnapshotListener((value, error) -> {
            if (value != null) {
                riderPackages.clear();
                int assigned = 0, pickedUp = 0;
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Package p = doc.toObject(Package.class);
                    if (p != null && (p.getStatus().equals(Package.STATUS_ASSIGNED) || p.getStatus().equals(Package.STATUS_PICKED_UP))) {
                        riderPackages.add(p);
                        if (p.getStatus().equals(Package.STATUS_ASSIGNED)) assigned++;
                        if (p.getStatus().equals(Package.STATUS_PICKED_UP)) pickedUp++;
                    }
                }
                tvRiderStats.setText("Assigned: " + assigned + " | Picked Up: " + pickedUp);
                tvEmptyState.setVisibility(riderPackages.isEmpty() ? View.VISIBLE : View.GONE);
                rvRiderPackages.setVisibility(riderPackages.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        });
    }
}