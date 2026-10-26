package com.example.load.activities;

import android.app.Activity;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load.R;
import com.example.load.adapters.PackageAdapter;
import com.example.load.models.Package;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalCount, tvPendingCount;
    private RecyclerView recyclerViewPackages;
    private Button btnCreatePackage;
    private PackageAdapter adapter;
    private List<Package> dummyPackages;

    private ActivityResultLauncher<Intent> createPackageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        recyclerViewPackages = findViewById(R.id.recyclerViewPackages);
        btnCreatePackage = findViewById(R.id.btnCreatePackage);

        dummyPackages = generateDummyData();

        adapter = new PackageAdapter(dummyPackages);
        recyclerViewPackages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPackages.setAdapter(adapter);

        updateStats();

        createPackageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();

                            String id = data.getStringExtra("NEW_PACKAGE_ID");
                            String pickup = data.getStringExtra("NEW_PACKAGE_PICKUP");
                            String drop = data.getStringExtra("NEW_PACKAGE_DROP");
                            String phone = data.getStringExtra("NEW_PACKAGE_PHONE");
                            double weight = data.getDoubleExtra("NEW_PACKAGE_WEIGHT", 0.0);
                            String type = data.getStringExtra("NEW_PACKAGE_TYPE");
                            String priority = data.getStringExtra("NEW_PACKAGE_PRIORITY");

                            Package newPkg = new Package(id, pickup, drop, phone, weight, type, priority);
                            dummyPackages.add(newPkg);
                            adapter.notifyDataSetChanged();
                            updateStats();
                            Toast.makeText(AdminDashboardActivity.this, "Package Created", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        btnCreatePackage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminDashboardActivity.this, CreatePackageActivity.class);
                createPackageLauncher.launch(intent);
            }
        });
    }

    private void updateStats() {
        int pendingCount = 0;
        for (Package p : dummyPackages) {
            if (p.getStatus().equals(Package.STATUS_PENDING)) {
                pendingCount++;
            }
        }
        tvTotalCount.setText(String.valueOf(dummyPackages.size()));
        tvPendingCount.setText(String.valueOf(pendingCount));
    }

    private List<Package> generateDummyData() {
        List<Package> list = new ArrayList<>();

        Package p1 = new Package("P001", "Dhanmondi Road 5", "Gulshan Road 12", "01711111111", 5.0, "Standard", "NORMAL");
        Package p2 = new Package("P002", "Mirpur 10", "Uttara Sector 4", "01722222222", 2.5, "Fragile", "URGENT");
        Package p3 = new Package("P003", "Banani Road 11", "Motijheel", "01733333333", 12.0, "Document", "NORMAL");

        p2.setStatus(Package.STATUS_ASSIGNED);
        p3.setStatus(Package.STATUS_DELIVERED);

        list.add(p1);
        list.add(p2);
        list.add(p3);

        return list;
    }
}