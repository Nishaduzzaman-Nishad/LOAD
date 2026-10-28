package com.example.load.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load.R;
import com.example.load.adapters.PackageAdapter;
import com.example.load.database.AppDatabase;
import com.example.load.models.Package;

import java.util.ArrayList;
import java.util.List;

public class RiderDashboardActivity extends AppCompatActivity {

    private RecyclerView rvRiderPackages;
    private TextView tvRiderStats, tvEmptyState;
    private PackageAdapter adapter;
    private List<Package> riderPackages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_dashboard);

        rvRiderPackages = findViewById(R.id.rvRiderPackages);
        tvRiderStats = findViewById(R.id.tvRiderStats);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        riderPackages = new ArrayList<>();
        adapter = new PackageAdapter(riderPackages, null);
        rvRiderPackages.setLayoutManager(new LinearLayoutManager(this));
        rvRiderPackages.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRiderPackages();
    }

    private void loadRiderPackages() {
        riderPackages.clear();

        List<Package> allPackages = AppDatabase.getInstance(this).packageDao().getAllPackages();
        int assignedCount = 0;
        int pickedUpCount = 0;

        for (Package p : allPackages) {
            if (p.getStatus().equals(Package.STATUS_ASSIGNED) || p.getStatus().equals(Package.STATUS_PICKED_UP)) {
                riderPackages.add(p);
                if (p.getStatus().equals(Package.STATUS_ASSIGNED)) assignedCount++;
                if (p.getStatus().equals(Package.STATUS_PICKED_UP)) pickedUpCount++;
            }
        }

        tvRiderStats.setText("Assigned: " + assignedCount + " | Picked Up: " + pickedUpCount);

        if (riderPackages.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvRiderPackages.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvRiderPackages.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }
}