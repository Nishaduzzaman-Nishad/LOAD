package com.example.load_app.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.load_app.R;
import com.example.load_app.adapters.PackageAdapter;
import com.example.load_app.models.Package;
import com.example.load_app.services.ShiftService;
import com.example.load_app.utils.PriorityQueueAlgorithm;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class RiderDashboardActivity extends AppCompatActivity {
    private RecyclerView rvRiderPackages;
    private TextView tvRiderStats, tvWelcomeMsg, tvCurrentWeight, tvRiderScore, tvTotalIncome, tvPenalty, tvWeatherStatus;
    private ProgressBar pbRiderScore;
    private EditText etSearch;
    private LinearLayout llEmptyState;
    private SwipeRefreshLayout swipeRefresh;
    private SwitchMaterial switchOnDuty;
    private PackageAdapter adapter;
    private List<Package> displayPackages;
    private List<Package> allMyPackages;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentTab = "ACTIVE";
    private String searchQuery = "";
    private Handler timerHandler;
    private Runnable timerRunnable;
    private SharedPreferences prefs;
    private boolean isOnDuty = true;

    private String currentRiderName = "";
    private ListenerRegistration assignmentListener;

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
        prefs = getSharedPreferences("LOAD_PREFS", MODE_PRIVATE);

        rvRiderPackages = findViewById(R.id.rvRiderPackages);
        tvRiderStats = findViewById(R.id.tvRiderStats);
        tvWelcomeMsg = findViewById(R.id.tvWelcomeMsg);
        tvCurrentWeight = findViewById(R.id.tvCurrentWeight);
        tvRiderScore = findViewById(R.id.tvRiderScore);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvPenalty = findViewById(R.id.tvPenalty);
        tvWeatherStatus = findViewById(R.id.tvWeatherStatus);
        pbRiderScore = findViewById(R.id.pbRiderScore);
        etSearch = findViewById(R.id.etSearch);
        llEmptyState = findViewById(R.id.llEmptyState);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        switchOnDuty = findViewById(R.id.switchOnDuty);
        Button btnSupport = findViewById(R.id.btnSupport);

        SwitchMaterial switchRiderTheme = findViewById(R.id.switchRiderTheme);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switchRiderTheme.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        switchRiderTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
                    currentRiderName = doc.getString("fullName");
                    tvWelcomeMsg.setText("Hello, " + (currentRiderName != null ? currentRiderName : "Rider") + "!");

                    Boolean dutyStatus = doc.getBoolean("isOnDuty");
                    isOnDuty = (dutyStatus == null) ? true : dutyStatus;
                    switchOnDuty.setChecked(isOnDuty);
                    updateDutyStatusUI();

                    // START REAL-TIME LISTENER FOR NEW ASSIGNMENTS
                    listenForNewAssignments();
                }
            });
        }

        switchOnDuty.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                isOnDuty = isChecked;
                if (mAuth.getCurrentUser() != null) {
                    db.collection("users").document(mAuth.getCurrentUser().getUid()).update("isOnDuty", isOnDuty);
                }
                updateDutyStatusUI();
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            loadMyPackages();
            fetchWeather();
            swipeRefresh.setRefreshing(false);
        });

        btnSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:01700000001"));
            startActivity(intent);
        });

        findViewById(R.id.cardTotalIncome).setOnClickListener(v -> {
            startActivity(new Intent(RiderDashboardActivity.this, RiderRevenueActivity.class));
        });

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                currentTab = "ACTIVE";
                filterList();
                return true;
            } else if (item.getItemId() == R.id.nav_deliveries) {
                currentTab = "COMPLETED";
                filterList();
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(RiderDashboardActivity.this, ProfileActivity.class));
                return true;
            }
            return true;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                filterList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fetchWeather();
        listenForAdminBroadcast();

        displayPackages = new ArrayList<>();
        allMyPackages = new ArrayList<>();

        adapter = new PackageAdapter(displayPackages, pkg -> {
            if (Package.STATUS_DELIVERED.equals(pkg.getStatus())) {
                Intent intent = new Intent(RiderDashboardActivity.this, PackageDetailsActivity.class);
                intent.putExtra("PACKAGE_ID", pkg.getPackageId());
                startActivity(intent);
            } else {
                Intent intent = new Intent(RiderDashboardActivity.this, DeliveryActivity.class);
                intent.putExtra("PACKAGE_ID", pkg.getPackageId());
                startActivity(intent);
            }
        });

        rvRiderPackages.setLayoutManager(new LinearLayoutManager(this));
        rvRiderPackages.setAdapter(adapter);
    }

    private void listenForNewAssignments() {
        if (currentRiderName.isEmpty()) return;

        assignmentListener = db.collection("packages")
                .whereEqualTo("assignedRiderId", currentRiderName)
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED ||
                                    (dc.getType() == DocumentChange.Type.MODIFIED && Package.STATUS_ASSIGNED.equals(dc.getDocument().getString("status")))) {

                                Package p = dc.getDocument().toObject(Package.class);
                                showAssignmentNotification(p);
                            }
                        }
                        loadMyPackages(); // Refresh list automatically
                    }
                });
    }

    private void showAssignmentNotification(Package p) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "AssignmentChannel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "New Assignments", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_truck_foreground)
                .setContentTitle("New Delivery Assigned! 📦")
                .setContentText("You have a new package: " + p.getPackageId() + " to " + p.getDropAddress())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(3, builder.build());
    }

    private void updateDutyStatusUI() {
        if (isOnDuty) {
            switchOnDuty.setText("On Duty");
            switchOnDuty.setTextColor(getResources().getColor(R.color.successGreen));
            Intent serviceIntent = new Intent(this, ShiftService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent);
            else startService(serviceIntent);
            loadMyPackages();
        } else {
            switchOnDuty.setText("Off Duty");
            switchOnDuty.setTextColor(getResources().getColor(R.color.errorRed));
            stopService(new Intent(this, ShiftService.class));
            displayPackages.clear();
            adapter.notifyDataSetChanged();
            llEmptyState.setVisibility(View.VISIBLE);
            rvRiderPackages.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isOnDuty) loadMyPackages();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (assignmentListener != null) {
            assignmentListener.remove();
        }
    }

    private void listenForAdminBroadcast() {
        db.collection("system_alerts").document("current_alert").addSnapshotListener((value, error) -> {
            if (value != null && value.exists()) {
                Long timestamp = value.getLong("timestamp");
                String message = value.getString("message");
                long lastSeen = prefs.getLong("last_alert_time", 0);

                if (timestamp != null && timestamp > lastSeen) {
                    prefs.edit().putLong("last_alert_time", timestamp).apply();
                    if (message != null) {
                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        String channelId = "BroadcastChannel";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel channel = new NotificationChannel(channelId, "Admin Broadcasts", NotificationManager.IMPORTANCE_HIGH);
                            notificationManager.createNotificationChannel(channel);
                        }
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                                .setSmallIcon(R.drawable.ic_truck_foreground)
                                .setContentTitle("Message from Admin")
                                .setContentText(message)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true);
                        notificationManager.notify(2, builder.build());

                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("📢 Message from Admin")
                                .setMessage(message)
                                .setPositiveButton("Acknowledge", null)
                                .show();
                    }
                }
            }
        });
    }

    private void loadMyPackages() {
        if (mAuth.getCurrentUser() == null || currentRiderName.isEmpty()) return;

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(userDoc -> {
            Double penalty = userDoc.getDouble("totalPenalty");
            if (penalty == null) penalty = 0.0;
            tvPenalty.setText("Penalty: ৳" + String.format("%.0f", penalty));

            db.collection("packages").get().addOnSuccessListener(value -> {
                if (value != null) {
                    allMyPackages.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Package p = doc.toObject(Package.class);
                        if (p != null && currentRiderName.equals(p.getAssignedRiderId())) {
                            allMyPackages.add(p);
                        }
                    }
                    filterList();
                }
            });
        });
    }

    private void filterList() {
        if (!isOnDuty) return;

        displayPackages.clear();
        int assignedCount = 0;
        int pickedUpCount = 0;
        int deliveredCount = 0;
        double currentLoad = 0.0;
        double totalIncome = 0.0;

        for (Package p : allMyPackages) {
            if (Package.STATUS_ASSIGNED.equals(p.getStatus())) {
                assignedCount++; currentLoad += p.getWeightKg();
            } else if (Package.STATUS_PICKED_UP.equals(p.getStatus())) {
                pickedUpCount++; currentLoad += p.getWeightKg();
            } else if (Package.STATUS_DELIVERED.equals(p.getStatus())) {
                deliveredCount++; totalIncome += p.getRiderFee();
            }
        }

        tvCurrentWeight.setText(String.format("Load: %.1f kg", currentLoad));
        tvTotalIncome.setText("৳" + String.format("%.0f", totalIncome));

        double totalForScore = deliveredCount + assignedCount + (pickedUpCount / 2.0);
        int score = totalForScore == 0 ? 0 : (int) ((deliveredCount / totalForScore) * 100);
        tvRiderScore.setText(score + "%");
        pbRiderScore.setProgress(score);
        tvRiderStats.setText(String.valueOf(assignedCount + pickedUpCount));

        if (currentTab.equals("ACTIVE")) {
            PriorityQueue<Package> sortedQueue = PriorityQueueAlgorithm.createDeliveryQueue();
            for (Package p : allMyPackages) {
                if (Package.STATUS_ASSIGNED.equals(p.getStatus()) || Package.STATUS_PICKED_UP.equals(p.getStatus())) {
                    if (p.getPackageId().toLowerCase().contains(searchQuery)) sortedQueue.add(p);
                }
            }
            while(!sortedQueue.isEmpty()) displayPackages.add(sortedQueue.poll());
        } else {
            for (Package p : allMyPackages) {
                if (Package.STATUS_DELIVERED.equals(p.getStatus())) {
                    if (p.getPackageId().toLowerCase().contains(searchQuery)) displayPackages.add(p);
                }
            }
        }

        llEmptyState.setVisibility(displayPackages.isEmpty() ? View.VISIBLE : View.GONE);
        rvRiderPackages.setVisibility(displayPackages.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void fetchWeather() {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=23.8103&longitude=90.4125&current_weather=true";
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject currentWeather = response.getJSONObject("current_weather");
                        double temp = currentWeather.getDouble("temperature");
                        double wind = currentWeather.getDouble("windspeed");
                        tvWeatherStatus.setText(temp + "°C | " + wind + " km/h");
                    } catch (JSONException e) { e.printStackTrace(); }
                },
                error -> tvWeatherStatus.setText("Weather N/A"));
        queue.add(request);
    }
}