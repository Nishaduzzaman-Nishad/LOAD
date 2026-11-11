package com.example.load_app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.example.load_app.utils.PriorityQueueAlgorithm;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class AdminDashboardActivity extends AppCompatActivity implements PackageAdapter.OnAssignClickListener {

    private TextView tvTotalCount, tvPendingCount, tvRevenue, tvNetworkStatus, tvRiderCount, tvWelcomeMsg, tvSuccessRate, tvEmptyState;
    private ProgressBar pbSuccessRate;
    private RecyclerView recyclerViewPackages;
    private Button btnCreatePackage, btnAutoDispatch, btnBroadcast;
    private EditText etSearch;
    private PackageAdapter adapter;
    private List<Package> allPackages;
    private List<Package> displayPackages;
    private ActivityResultLauncher<Intent> createPackageLauncher;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentFilter = "ALL";
    private String searchQuery = "";
    private Handler timerHandler;
    private Runnable timerRunnable;
    private TabLayout tabFilter;

    // NEW: Keep track of penalty revenue
    private double totalPenaltyRevenue = 0.0;

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                tvNetworkStatus.setText("● Admin Online");
                tvNetworkStatus.setTextColor(getResources().getColor(R.color.successGreen));
            } else {
                tvNetworkStatus.setText("● Offline");
                tvNetworkStatus.setTextColor(getResources().getColor(R.color.errorRed));
            }
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
        tvRevenue = findViewById(R.id.tvRevenue);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        tvRiderCount = findViewById(R.id.tvRiderCount);
        tvWelcomeMsg = findViewById(R.id.tvWelcomeMsg);
        tvSuccessRate = findViewById(R.id.tvSuccessRate);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        pbSuccessRate = findViewById(R.id.pbSuccessRate);
        recyclerViewPackages = findViewById(R.id.recyclerViewPackages);
        btnCreatePackage = findViewById(R.id.btnCreatePackage);
        btnAutoDispatch = findViewById(R.id.btnAutoDispatch);
        btnBroadcast = findViewById(R.id.btnBroadcast);
        etSearch = findViewById(R.id.etSearch);
        tabFilter = findViewById(R.id.tabFilter);

        SwitchMaterial switchAdminTheme = findViewById(R.id.switchAdminTheme);

        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        switchAdminTheme.setChecked(currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        switchAdminTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("fullName");
                    tvWelcomeMsg.setText("Welcome back, " + (name != null ? name : "Admin") + " 👋");
                }
            });
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(AdminDashboardActivity.this, ProfileActivity.class));
                return true;
            }
            return true;
        });

        findViewById(R.id.cardRiders).setOnClickListener(v -> startActivity(new Intent(AdminDashboardActivity.this, RiderListActivity.class)));
        findViewById(R.id.cardRevenue).setOnClickListener(v -> startActivity(new Intent(AdminDashboardActivity.this, RevenueActivity.class)));
        findViewById(R.id.cardTotal).setOnClickListener(v -> { if (tabFilter.getTabAt(0) != null) tabFilter.getTabAt(0).select(); });
        findViewById(R.id.cardPending).setOnClickListener(v -> { if (tabFilter.getTabAt(1) != null) tabFilter.getTabAt(1).select(); });
        findViewById(R.id.ivAdminNotifications).setOnClickListener(v -> startActivity(new Intent(AdminDashboardActivity.this, AdminNotificationsActivity.class)));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        allPackages = new ArrayList<>();
        displayPackages = new ArrayList<>();
        adapter = new PackageAdapter(displayPackages, this, "ADMIN");
        recyclerViewPackages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPackages.setAdapter(adapter);

        tabFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getText().toString();
                applyFilter();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadPackagesFromFirestore();
        loadRiderCount();

        // NEW: Load total penalty earnings
        loadPlatformEarnings();

        createPackageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> { });
        btnCreatePackage.setOnClickListener(v -> createPackageLauncher.launch(new Intent(AdminDashboardActivity.this, CreatePackageActivity.class)));

        btnAutoDispatch.setOnClickListener(v -> runAutoDispatch());
        btnBroadcast.setOnClickListener(v -> showBroadcastDialog());
    }

    private void loadPlatformEarnings() {
        db.collection("platform_earnings").addSnapshotListener((value, error) -> {
            if (value != null) {
                totalPenaltyRevenue = 0.0;
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Double amount = doc.getDouble("amount");
                    if (amount != null) totalPenaltyRevenue += amount;
                }
                applyFilter(); // Refresh the UI with the new total
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (adapter != null) adapter.notifyDataSetChanged();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void runAutoDispatch() {
        List<Package> pendingPkgs = new ArrayList<>();
        for (Package p : allPackages) {
            if (Package.STATUS_PENDING.equals(p.getStatus())) pendingPkgs.add(p);
        }

        if (pendingPkgs.isEmpty()) {
            Toast.makeText(this, "No pending packages to dispatch.", Toast.LENGTH_SHORT).show();
            return;
        }

        PriorityQueue<Package> sortedQueue = PriorityQueueAlgorithm.createDeliveryQueue();
        sortedQueue.addAll(pendingPkgs);
        List<Package> sortedList = new ArrayList<>();
        while(!sortedQueue.isEmpty()) sortedList.add(sortedQueue.poll());

        db.collection("users").whereEqualTo("role", "RIDER").get().addOnSuccessListener(riderSnapshots -> {
            List<String> activeRiders = new ArrayList<>();
            for (DocumentSnapshot doc : riderSnapshots.getDocuments()) {
                Boolean isOnDuty = doc.getBoolean("isOnDuty");
                if (isOnDuty == null || isOnDuty) {
                    activeRiders.add(doc.getString("fullName"));
                }
            }

            if (activeRiders.isEmpty()) {
                Toast.makeText(this, "No On-Duty riders available.", Toast.LENGTH_SHORT).show();
                return;
            }

            String bestRiderName = activeRiders.get(0);
            List<Package> optimalToAssign = KnapsackAlgorithm.getOptimalPackages(sortedList, 20.0);

            if (optimalToAssign.isEmpty()) {
                Toast.makeText(this, "No packages fit within capacity.", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Package p : optimalToAssign) {
                p.setStatus(Package.STATUS_ASSIGNED);
                p.setAssignedRiderId(bestRiderName);
                db.collection("packages").document(p.getPackageId()).set(p);
            }
            Toast.makeText(this, "Auto-Dispatched " + optimalToAssign.size() + " loads to " + bestRiderName, Toast.LENGTH_LONG).show();
        });
    }

    private void showBroadcastDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Broadcast to All Riders");
        final EditText input = new EditText(this);
        input.setHint("e.g. Heavy rain, drive safe!");
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String msg = input.getText().toString().trim();
            if (!msg.isEmpty()) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("message", msg);
                alert.put("timestamp", System.currentTimeMillis());
                db.collection("system_alerts").document("current_alert").set(alert)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Broadcast Sent!", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadRiderCount() {
        db.collection("users").whereEqualTo("role", "RIDER").addSnapshotListener((value, error) -> {
            if (value != null) tvRiderCount.setText(String.valueOf(value.size()));
        });
    }

    private void applyFilter() {
        displayPackages.clear();
        int pendingCount = 0;
        int deliveredCount = 0;
        double packageRevenue = 0.0;

        for (Package p : allPackages) {
            if (Package.STATUS_PENDING.equals(p.getStatus())) pendingCount++;
            if (Package.STATUS_DELIVERED.equals(p.getStatus())) {
                deliveredCount++;
                packageRevenue += p.getNetProfit();
            }

            boolean matchesFilter = currentFilter.equals("ALL") || currentFilter.equals(p.getStatus());
            boolean matchesSearch = p.getPackageId().toLowerCase().contains(searchQuery) ||
                    (p.getAssignedRiderId() != null && p.getAssignedRiderId().toLowerCase().contains(searchQuery));

            if (matchesFilter && matchesSearch) displayPackages.add(p);
        }

        Collections.sort(displayPackages, (p1, p2) -> {
            boolean p1Urgent = "URGENT".equalsIgnoreCase(p1.getPriority());
            boolean p2Urgent = "URGENT".equalsIgnoreCase(p2.getPriority());
            if (p1Urgent && !p2Urgent) return -1;
            if (!p1Urgent && p2Urgent) return 1;
            return Long.compare(p2.getCreatedAt(), p1.getCreatedAt());
        });

        adapter.notifyDataSetChanged();
        tvTotalCount.setText(String.valueOf(allPackages.size()));
        tvPendingCount.setText(String.valueOf(pendingCount));

        // NEW: Add penalty revenue to total
        double finalTotalRevenue = packageRevenue + totalPenaltyRevenue;
        tvRevenue.setText("৳" + String.format("%.0f", finalTotalRevenue));

        int totalPackages = allPackages.size();
        int successRate = totalPackages == 0 ? 0 : (deliveredCount * 100) / totalPackages;
        tvSuccessRate.setText(successRate + "%");
        pbSuccessRate.setProgress(successRate);

        tvEmptyState.setVisibility(displayPackages.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerViewPackages.setVisibility(displayPackages.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadPackagesFromFirestore() {
        db.collection("packages").addSnapshotListener((value, error) -> {
            if (value != null) {
                allPackages.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Package pkg = doc.toObject(Package.class);
                    if (pkg != null) allPackages.add(pkg);
                }
                applyFilter();
            }
        });
    }

    @Override
    public void onAssignClick(Package selectedPackage) {
        db.collection("users").whereEqualTo("role", "RIDER").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> riderNames = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Boolean isOnDuty = doc.getBoolean("isOnDuty");
                    if (isOnDuty == null || isOnDuty) {
                        riderNames.add(doc.getString("fullName"));
                    }
                }

                if (riderNames.isEmpty()) {
                    Toast.makeText(this, "No On-Duty Riders available.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] ridersArray = riderNames.toArray(new String[0]);
                new AlertDialog.Builder(this)
                        .setTitle("Select Rider")
                        .setItems(ridersArray, (dialog, which) -> {
                            selectedPackage.setStatus(Package.STATUS_ASSIGNED);
                            selectedPackage.setAssignedRiderId(ridersArray[which]);
                            db.collection("packages").document(selectedPackage.getPackageId()).set(selectedPackage)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Assigned!", Toast.LENGTH_SHORT).show());
                        }).show();
            }
        });
    }

    @Override
    protected void onStart() { super.onStart(); registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)); }
    @Override
    protected void onStop() { super.onStop(); unregisterReceiver(networkReceiver); }
}