package com.example.load_app.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.load_app.R;
import com.example.load_app.adapters.PackageAdapter;
import com.example.load_app.models.Package;
import com.example.load_app.utils.DijkstraAlgorithm;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class P2PDashboardActivity extends AppCompatActivity {
    private RecyclerView rvP2PPackages;
    private TextView tvP2PWelcome;
    private AutoCompleteTextView etP2PStart, etP2PEnd;
    private EditText etP2PSearch;
    private LinearLayout llP2PEmptyState;
    private SwipeRefreshLayout swipeRefresh;
    private PackageAdapter adapter;
    private List<Package> displayPackages;
    private List<Package> allPendingPackages;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Map<String, List<DijkstraAlgorithm.Edge>> cityGraph;

    private String searchQuery = "";
    private String currentTravelRoute = "";

    // 50+ Dhaka City Locations
    private final String[] dhakaCities = {
            "Uttara", "Airport", "Nikunja", "Khilkhet", "Kuril", "Bashundhara", "Notun Bazar",
            "Baridhara", "Gulshan", "Banani", "Mohakhali", "Tejgaon", "Kawran Bazar", "Farmgate",
            "Dhanmondi", "Jhigatola", "Shahbagh", "Mohammadpur", "Shyamoli", "Agargaon", "Mirpur",
            "Kazipara", "Paltan", "Motijheel", "Jatrabari", "Sayedabad", "Demra", "Gendaria",
            "Kotwali", "Lalbagh", "Azimpur", "Newmarket", "Panthapath", "Green Road", "Kalabagan",
            "Gabtoli", "Kallyanpur", "Moghbazar", "Malibagh", "Khilgaon", "Rampura", "Badda", "Merul Badda",
            "Dhanmondi 27", "Mohammadpur Bus Stand", "Mirpur 10", "Mirpur 1", "Gulshan 1", "Gulshan 2",
            "Banani 11", "Uttara Sector 7", "Bashundhara R/A", "Motijheel Shapla Chattar"
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

        setContentView(R.layout.activity_p2p_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        cityGraph = DijkstraAlgorithm.buildDhakaGraph();

        rvP2PPackages = findViewById(R.id.rvP2PPackages);
        tvP2PWelcome = findViewById(R.id.tvP2PWelcome);
        etP2PStart = findViewById(R.id.etP2PStart);
        etP2PEnd = findViewById(R.id.etP2PEnd);
        etP2PSearch = findViewById(R.id.etP2PSearch);
        llP2PEmptyState = findViewById(R.id.llP2PEmptyState);
        swipeRefresh = findViewById(R.id.swipeRefreshP2P);
        Button btnSearchRoute = findViewById(R.id.btnSearchRoute);

        SwitchMaterial switchTheme = findViewById(R.id.switchP2PTheme);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Set up AutoCompleteTextViews with city array
        ArrayAdapter<String> adapterCities = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, dhakaCities);
        etP2PStart.setAdapter(adapterCities);
        etP2PEnd.setAdapter(adapterCities);

        // Dark Mode Setup
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
                    tvP2PWelcome.setText("Hello, " + (name != null ? name : "Traveler") + "!");
                }
            });
        }

        swipeRefresh.setOnRefreshListener(() -> {
            loadAvailablePackages();
            swipeRefresh.setRefreshing(false);
        });

        etP2PSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                return true;
            } else if (item.getItemId() == R.id.nav_deliveries) {
                startActivity(new Intent(P2PDashboardActivity.this, RiderDashboardActivity.class));
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(P2PDashboardActivity.this, ProfileActivity.class));
                return true;
            }
            return true;
        });

        btnSearchRoute.setOnClickListener(v -> {
            String start = etP2PStart.getText().toString().trim();
            String end = etP2PEnd.getText().toString().trim();

            if (start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Please select both start and end locations", Toast.LENGTH_SHORT).show();
                return;
            }

            if (start.equalsIgnoreCase(end)) {
                Toast.makeText(this, "Start and End locations cannot be the same", Toast.LENGTH_SHORT).show();
                return;
            }

            // Since they pick from the list, we use the exact string
            currentTravelRoute = DijkstraAlgorithm.getShortestPath(cityGraph, start, end);

            if (currentTravelRoute.contains("No route found") || currentTravelRoute.contains("unavailable")) {
                Toast.makeText(this, "No route found between these locations.", Toast.LENGTH_LONG).show();
                currentTravelRoute = "";
                return;
            }

            Toast.makeText(this, "Route Found! Filtering packages...", Toast.LENGTH_SHORT).show();
            applyFilter();
        });

        displayPackages = new ArrayList<>();
        allPendingPackages = new ArrayList<>();

        adapter = new PackageAdapter(displayPackages, pkg -> {
            showDijkstraAcceptDialog(pkg);
        });

        rvP2PPackages.setLayoutManager(new LinearLayoutManager(this));
        rvP2PPackages.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvailablePackages();
    }

    private void loadAvailablePackages() {
        db.collection("packages").whereEqualTo("status", Package.STATUS_PENDING).get().addOnSuccessListener(value -> {
            if (value != null) {
                allPendingPackages.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Package p = doc.toObject(Package.class);
                    if (p != null) {
                        allPendingPackages.add(p);
                    }
                }
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        displayPackages.clear();

        if (currentTravelRoute.isEmpty()) {
            llP2PEmptyState.setVisibility(View.VISIBLE);
            rvP2PPackages.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
            return;
        }

        for (Package p : allPendingPackages) {
            // Extract exact match from package address
            String pPickup = extractCity(p.getPickupAddress());
            String pDrop = extractCity(p.getDropAddress());

            // Check if both pickup and drop exist in the calculated route string
            boolean isOnRoute = pPickup != null && pDrop != null &&
                    currentTravelRoute.contains(pPickup) &&
                    currentTravelRoute.contains(pDrop);

            boolean matchesSearch = p.getPackageId().toLowerCase().contains(searchQuery);

            if (isOnRoute && matchesSearch) {
                displayPackages.add(p);
            }
        }

        adapter.notifyDataSetChanged();

        if (displayPackages.isEmpty()) {
            llP2PEmptyState.setVisibility(View.VISIBLE);
            rvP2PPackages.setVisibility(View.GONE);
        } else {
            llP2PEmptyState.setVisibility(View.GONE);
            rvP2PPackages.setVisibility(View.VISIBLE);
        }
    }

    private void showDijkstraAcceptDialog(Package pkg) {
        String pickupNode = extractCity(pkg.getPickupAddress());
        String dropNode = extractCity(pkg.getDropAddress());

        if (pickupNode == null || dropNode == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Accept Delivery?")
                    .setMessage("Route calculation unavailable for this area. Accept anyway?")
                    .setPositiveButton("Accept", (d, w) -> assignPackageToMe(pkg))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        String routeInfo = DijkstraAlgorithm.getShortestPath(cityGraph, pickupNode, dropNode);

        new AlertDialog.Builder(this)
                .setTitle("P2P Detour Cost (Dijkstra)")
                .setMessage("Package: " + pkg.getPackageId() + "\n\n" + routeInfo + "\n\nDo you want to accept this load?")
                .setPositiveButton("Accept Load", (dialog, which) -> assignPackageToMe(pkg))
                .setNegativeButton("Reject", null)
                .show();
    }

    private void assignPackageToMe(Package pkg) {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String myName = doc.getString("fullName");

            db.collection("users").document(uid).update("isOnDuty", true);

            pkg.setStatus(Package.STATUS_ASSIGNED);
            pkg.setAssignedRiderId(myName);

            db.collection("packages").document(pkg.getPackageId()).set(pkg).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Load Accepted! Navigating to Delivery View.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(P2PDashboardActivity.this, DeliveryActivity.class);
                intent.putExtra("PACKAGE_ID", pkg.getPackageId());
                startActivity(intent);
            });
        });
    }

    // Helper to ensure exact matching with the city list
    private String extractCity(String address) {
        if (address == null) return null;
        for (String city : dhakaCities) {
            if (address.toLowerCase().contains(city.toLowerCase())) {
                return city;
            }
        }
        // Fallback for base zones
        String lower = address.toLowerCase();
        if (lower.contains("uttara")) return "Uttara";
        if (lower.contains("mirpur")) return "Mirpur";
        if (lower.contains("gulshan")) return "Gulshan";
        if (lower.contains("farmgate")) return "Farmgate";
        if (lower.contains("dhanmondi")) return "Dhanmondi";
        if (lower.contains("motijheel")) return "Motijheel";
        return null;
    }
}