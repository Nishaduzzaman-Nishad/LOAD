package com.example.load.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load.R;
import com.example.load.adapters.PackageAdapter;
import com.example.load.database.AppDatabase;
import com.example.load.models.Package;
import com.example.load.utils.KnapsackAlgorithm;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity implements PackageAdapter.OnAssignClickListener {

    private TextView tvTotalCount, tvPendingCount;
    private RecyclerView recyclerViewPackages;
    private Button btnCreatePackage;
    private PackageAdapter adapter;
    private List<Package> packageList;
    private ActivityResultLauncher<Intent> createPackageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        recyclerViewPackages = findViewById(R.id.recyclerViewPackages);
        btnCreatePackage = findViewById(R.id.btnCreatePackage);

        packageList = new ArrayList<>();
        adapter = new PackageAdapter(packageList, this);
        recyclerViewPackages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPackages.setAdapter(adapter);

        loadDataFromDatabase();

        createPackageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            loadDataFromDatabase();
                        }
                    }
                }
        );

        btnCreatePackage.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, CreatePackageActivity.class);
            createPackageLauncher.launch(intent);
        });
    }

    private void loadDataFromDatabase() {
        packageList.clear();
        packageList.addAll(AppDatabase.getInstance(this).packageDao().getAllPackages());
        adapter.notifyDataSetChanged();
        updateStats();
    }

    private void updateStats() {
        int pendingCount = 0;
        for (Package p : packageList) {
            if (p.getStatus().equals(Package.STATUS_PENDING)) {
                pendingCount++;
            }
        }
        tvTotalCount.setText(String.valueOf(packageList.size()));
        tvPendingCount.setText(String.valueOf(pendingCount));
    }

    @Override
    public void onAssignClick(Package selectedPackage) {
        final String[] riders = {"Rakib (15 kg capacity)", "Karim (25 kg capacity)"};
        final double[] capacities = {15.0, 25.0};

        new AlertDialog.Builder(this)
                .setTitle("Select Rider")
                .setItems(riders, (dialog, which) -> {
                    double selectedRiderCapacity = capacities[which];
                    String riderName = riders[which].split(" ")[0];

                    List<Package> pendingPackages = new ArrayList<>();
                    for (Package p : packageList) {
                        if (p.getStatus().equals(Package.STATUS_PENDING)) {
                            pendingPackages.add(p);
                        }
                    }

                    List<Package> optimalPackages = KnapsackAlgorithm.getOptimalPackages(pendingPackages, selectedRiderCapacity);

                    if (optimalPackages.contains(selectedPackage)) {
                        selectedPackage.setStatus(Package.STATUS_ASSIGNED);
                        selectedPackage.setAssignedRiderId(riderName);
                        AppDatabase.getInstance(this).packageDao().insert(selectedPackage);

                        loadDataFromDatabase();
                        Toast.makeText(this, "Assigned to " + riderName, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Cannot assign! " + riderName + " does not have enough capacity.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }
}