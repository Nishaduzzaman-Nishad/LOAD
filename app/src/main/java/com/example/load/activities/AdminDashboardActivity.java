package com.example.load.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load.R;
import com.example.load.adapters.PackageAdapter;
import com.example.load.database.AppDatabase;
import com.example.load.models.Package;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

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
        adapter = new PackageAdapter(packageList);
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

        btnCreatePackage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminDashboardActivity.this, CreatePackageActivity.class);
                createPackageLauncher.launch(intent);
            }
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
}