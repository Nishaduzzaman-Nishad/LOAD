package com.example.load_app.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.load_app.R;
import com.example.load_app.adapters.PackageAdapter;
import com.example.load_app.models.Package;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardActivity extends AppCompatActivity {
    private RecyclerView rvCustomerPackages;
    private TextView tvListLabel, tvCustomerWelcome, tvEmptyText, tvCustTotalOrders, tvCustTotalSpent;
    private LinearLayout llEmptyState;
    private EditText etSearch;
    private SwipeRefreshLayout swipeRefresh;
    private PackageAdapter adapter;
    private List<Package> displayPackages;
    private List<Package> allMyPackages;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentTab = "ACTIVE";
    private String searchQuery = "";

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

        setContentView(R.layout.activity_customer_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvCustomerPackages = findViewById(R.id.rvCustomerPackages);
        tvListLabel = findViewById(R.id.tvListLabel);
        tvCustomerWelcome = findViewById(R.id.tvCustomerWelcome);
        tvEmptyText = findViewById(R.id.tvEmptyText);
        tvCustTotalOrders = findViewById(R.id.tvCustTotalOrders);
        tvCustTotalSpent = findViewById(R.id.tvCustTotalSpent);
        llEmptyState = findViewById(R.id.llCustomerEmptyState);
        etSearch = findViewById(R.id.etCustomerSearch);
        swipeRefresh = findViewById(R.id.swipeRefreshCustomer);
        Button btnCreateRequest = findViewById(R.id.btnCreateRequest);

        SwitchMaterial switchTheme = findViewById(R.id.switchCustomerTheme);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switchTheme.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
                    tvCustomerWelcome.setText("Hello, " + (name != null ? name : "Customer") + "!");
                }
            });
        }

        swipeRefresh.setOnRefreshListener(() -> {
            loadCustomerPackages();
            swipeRefresh.setRefreshing(false);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                filterList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                currentTab = "ACTIVE";
                tvListLabel.setText("Active Packages");
                filterList();
                return true;
            } else if (item.getItemId() == R.id.nav_deliveries) {
                currentTab = "COMPLETED";
                tvListLabel.setText("Delivery History");
                filterList();
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(CustomerDashboardActivity.this, ProfileActivity.class));
                return true;
            }
            return true;
        });

        btnCreateRequest.setOnClickListener(v -> {
            startActivity(new Intent(CustomerDashboardActivity.this, CreatePackageActivity.class));
        });

        displayPackages = new ArrayList<>();
        allMyPackages = new ArrayList<>();

        adapter = new PackageAdapter(displayPackages, pkg -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, TrackPackageActivity.class);
            intent.putExtra("PKG_ID", pkg.getPackageId());
            startActivity(intent);
        });

        rvCustomerPackages.setLayoutManager(new LinearLayoutManager(this));
        rvCustomerPackages.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerPackages();
    }

    private void loadCustomerPackages() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        db.collection("packages").get().addOnSuccessListener(value -> {
            if (value != null) {
                allMyPackages.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Package p = doc.toObject(Package.class);
                    if (p != null) {
                        boolean isMine = myId.equals(p.getCustomerId());
                        boolean isWalkIn = "WALK_IN".equals(p.getCustomerId());
                        if (isMine || isWalkIn) {
                            allMyPackages.add(p);
                        }
                    }
                }
                filterList();
            }
        });
    }

    private void filterList() {
        displayPackages.clear();
        double totalSpent = 0.0;

        // Calculate total spent across ALL packages
        for (Package p : allMyPackages) {
            totalSpent += p.getCustomerPrice();
        }

        tvCustTotalOrders.setText(String.valueOf(allMyPackages.size()));
        tvCustTotalSpent.setText("৳" + String.format("%.0f", totalSpent));

        for (Package p : allMyPackages) {
            boolean matchesTab = false;

            if (currentTab.equals("ACTIVE")) {
                matchesTab = Package.STATUS_PENDING.equals(p.getStatus()) ||
                        Package.STATUS_ASSIGNED.equals(p.getStatus()) ||
                        Package.STATUS_PICKED_UP.equals(p.getStatus());
            } else {
                matchesTab = Package.STATUS_DELIVERED.equals(p.getStatus());
            }

            boolean matchesSearch = p.getPackageId().toLowerCase().contains(searchQuery);

            if (matchesTab && matchesSearch) {
                displayPackages.add(p);
            }
        }

        adapter.notifyDataSetChanged();

        if (displayPackages.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvCustomerPackages.setVisibility(View.GONE);
            if (currentTab.equals("ACTIVE")) {
                tvEmptyText.setText("You have no active deliveries.\nTap 'Order Now' to create one.");
            } else {
                tvEmptyText.setText("No completed deliveries yet.");
            }
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvCustomerPackages.setVisibility(View.VISIBLE);
        }
    }
}