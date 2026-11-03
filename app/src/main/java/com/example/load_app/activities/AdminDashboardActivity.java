package com.example.load_app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load_app.R;
import com.example.load_app.adapters.PackageAdapter;
import com.example.load_app.models.Package;
import com.example.load_app.utils.KnapsackAlgorithm;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity implements PackageAdapter.OnAssignClickListener {

    private TextView tvTotalCount, tvPendingCount, tvNetworkStatus, tvLogout;
    private RecyclerView recyclerViewPackages;
    private ListView listViewRiders;
    private Switch switchTheme;
    private Button btnCreatePackage;
    private PackageAdapter adapter;
    private List<Package> packageList;
    private ActivityResultLauncher<Intent> createPackageLauncher;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            tvNetworkStatus.setText(isConnected ? "Online" : "Offline");
            tvNetworkStatus.setTextColor(getResources().getColor(isConnected ? R.color.successGreen : R.color.errorRed));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey("AIzaSyBzGU3NJV8-y5wDwmn3jOTw95e88HKxZeM")
                    .setApplicationId("1:853658382959:android:b6b6c6ce5c5caa928e139a")
                    .setProjectId("load-2420e")
                    .build();
            FirebaseApp.initializeApp(this, options);
        }

        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        tvLogout = findViewById(R.id.tvLogout);
        switchTheme = findViewById(R.id.switchTheme);
        recyclerViewPackages = findViewById(R.id.recyclerViewPackages);
        listViewRiders = findViewById(R.id.listViewRiders);
        btnCreatePackage = findViewById(R.id.btnCreatePackage);

        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        switchTheme.setChecked(currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        tvLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(AdminDashboardActivity.this, LoginActivity.class));
            finish();
        });

        packageList = new ArrayList<>();
        // Passing "ADMIN" as the userRole
        adapter = new PackageAdapter(packageList, this, "ADMIN");
        recyclerViewPackages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPackages.setAdapter(adapter);

        loadPackagesFromFirestore();
        loadRidersListView();

        createPackageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { }
        );

        btnCreatePackage.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, CreatePackageActivity.class);
            createPackageLauncher.launch(intent);
        });
    }

    private void loadRidersListView() {
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> riderNames = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                String role = doc.getString("role");
                String name = doc.getString("fullName");
                if (role != null && role.equalsIgnoreCase("RIDER")) {
                    riderNames.add(name);
                }
            }
            ArrayAdapter<String> riderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, riderNames);
            listViewRiders.setAdapter(riderAdapter);
        });
    }

    private void loadPackagesFromFirestore() {
        db.collection("packages").addSnapshotListener((value, error) -> {
            if (value != null) {
                packageList.clear();
                int pendingCount = 0;
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Package pkg = doc.toObject(Package.class);
                    if (pkg != null) {
                        packageList.add(pkg);
                        if (pkg.getStatus().equals(Package.STATUS_PENDING)) pendingCount++;
                    }
                }
                adapter.notifyDataSetChanged();
                tvTotalCount.setText(String.valueOf(packageList.size()));
                tvPendingCount.setText(String.valueOf(pendingCount));
            }
        });
    }

    @Override
    public void onAssignClick(Package selectedPackage) {
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> riderNames = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                String role = doc.getString("role");
                String name = doc.getString("fullName");
                if (role != null && role.equalsIgnoreCase("RIDER")) {
                    riderNames.add(name);
                }
            }

            if (riderNames.isEmpty()) {
                Toast.makeText(this, "No riders found in database.", Toast.LENGTH_LONG).show();
                return;
            }

            String[] ridersArray = riderNames.toArray(new String[0]);
            final double capacity = 20.0;

            new AlertDialog.Builder(this)
                    .setTitle("Select Rider")
                    .setItems(ridersArray, (dialog, which) -> {
                        String riderName = ridersArray[which];
                        List<Package> pendingPackages = new ArrayList<>();
                        for (Package p : packageList) {
                            if (p.getStatus().equals(Package.STATUS_PENDING)) pendingPackages.add(p);
                        }

                        List<Package> optimalPackages = KnapsackAlgorithm.getOptimalPackages(pendingPackages, capacity);

                        if (optimalPackages.contains(selectedPackage)) {
                            selectedPackage.setStatus(Package.STATUS_ASSIGNED);
                            selectedPackage.setAssignedRiderId(riderName);

                            db.collection("packages").document(selectedPackage.getPackageId()).set(selectedPackage)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(this, "Assigned to " + riderName, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Cannot assign! Capacity exceeded.", Toast.LENGTH_LONG).show();
                        }
                    }).show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkReceiver);
    }
}