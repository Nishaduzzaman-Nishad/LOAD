package com.example.load_app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.load_app.R;
import com.example.load_app.adapters.PackageAdapter;
import com.example.load_app.models.Package;
import com.example.load_app.services.ShiftService;
import com.example.load_app.utils.PriorityQueueAlgorithm;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class RiderDashboardActivity extends AppCompatActivity {
    private RecyclerView rvRiderPackages;
    private TextView tvRiderStats, tvEmptyState, tvLogout;
    private Button btnGoOnline;
    private Switch switchTheme;
    private PackageAdapter adapter;
    private List<Package> riderPackages;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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

        setContentView(R.layout.activity_rider_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvRiderPackages = findViewById(R.id.rvRiderPackages);
        tvRiderStats = findViewById(R.id.tvRiderStats);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnGoOnline = findViewById(R.id.btnGoOnline);
        tvLogout = findViewById(R.id.tvLogout);
        switchTheme = findViewById(R.id.switchTheme);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                // Trigger existing logout logic
                tvLogout.performClick();
                return true;
            }
            // Other buttons just stay on this screen for the demo
            return true;
        });

        // Fetch Weather
        fetchWeather();

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switchTheme.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        tvLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(RiderDashboardActivity.this, LoginActivity.class));
            finish();
        });

        riderPackages = new ArrayList<>();
        adapter = new PackageAdapter(riderPackages, null, "RIDER");
        adapter.setOnPackageClickListener(pkg -> {
            Intent intent = new Intent(RiderDashboardActivity.this, DeliveryActivity.class);
            intent.putExtra("PACKAGE_ID", pkg.getPackageId());
            startActivity(intent);
        });

        rvRiderPackages.setLayoutManager(new LinearLayoutManager(this));
        rvRiderPackages.setAdapter(adapter);

        btnGoOnline.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                    return;
                }
            }
            Intent serviceIntent = new Intent(this, ShiftService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "Shift Started! You are online.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyPackages();
    }

    private void loadMyPackages() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String myName = userDoc.getString("fullName");
            if (myName == null) return;

            db.collection("packages").addSnapshotListener((value, error) -> {
                if (value != null) {
                    riderPackages.clear();
                    int assigned = 0, pickedUp = 0;

                    // 1. Create the Priority Queue (Min-Heap)
                    PriorityQueue<Package> sortedQueue = PriorityQueueAlgorithm.createDeliveryQueue();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Package p = doc.toObject(Package.class);
                        if (p != null && myName.equals(p.getAssignedRiderId())) {
                            if (p.getStatus().equals(Package.STATUS_ASSIGNED) || p.getStatus().equals(Package.STATUS_PICKED_UP)) {
                                sortedQueue.add(p); // Add to Min-Heap
                                if (p.getStatus().equals(Package.STATUS_ASSIGNED)) assigned++;
                                if (p.getStatus().equals(Package.STATUS_PICKED_UP)) pickedUp++;
                            }
                        }
                    }

                    // 2. Extract from queue (Urgent packages pop out first, then oldest)
                    while(!sortedQueue.isEmpty()){
                        riderPackages.add(sortedQueue.poll());
                    }

                    tvRiderStats.setText(String.valueOf(assigned));
                    tvEmptyState.setVisibility(riderPackages.isEmpty() ? View.VISIBLE : View.GONE);
                    rvRiderPackages.setVisibility(riderPackages.isEmpty() ? View.GONE : View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }

    // HTTP Request & JSON Parsing for Weather API
    private void fetchWeather() {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=23.8103&longitude=90.4125&current_weather=true";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject currentWeather = response.getJSONObject("current_weather");
                        double temp = currentWeather.getDouble("temperature");
                        double wind = currentWeather.getDouble("windspeed");

                        TextView tvWeather = findViewById(R.id.tvWeatherStatus);
                        tvWeather.setText("Weather: " + temp + "°C | Wind: " + wind + " km/h");

                        if (wind > 15.0) {
                            tvWeather.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            tvWeather.append(" ⚠️ High Wind!");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    TextView tvWeather = findViewById(R.id.tvWeatherStatus);
                    tvWeather.setText("Weather data unavailable");
                });
        queue.add(request);
    }
}