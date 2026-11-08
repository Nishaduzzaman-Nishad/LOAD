package com.example.load_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load_app.R;
import com.example.load_app.adapters.PackageAdapter;
import com.example.load_app.models.Package;
import com.example.load_app.utils.DijkstraAlgorithm;
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
    private PackageAdapter adapter;
    private List<Package> p2pPackages;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Our mock city map
    private Map<String, List<DijkstraAlgorithm.Edge>> cityGraph;

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
        cityGraph = DijkstraAlgorithm.buildDhakaGraph(); // Initialize graph

        rvP2PPackages = findViewById(R.id.rvP2PPackages);
        TextView tvLogout = findViewById(R.id.tvLogout);

        tvLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(P2PDashboardActivity.this, LoginActivity.class));
            finish();
        });

        p2pPackages = new ArrayList<>();

        // Modified click listener to use Dijkstra
        adapter = new PackageAdapter(p2pPackages, pkg -> {
            showDijkstraRouteDialog(pkg);
        }, "P2P");

        rvP2PPackages.setLayoutManager(new LinearLayoutManager(this));
        rvP2PPackages.setAdapter(adapter);

        loadAvailablePackages();
    }

    private void showDijkstraRouteDialog(Package pkg) {
        // Parse addresses to match our graph nodes
        String pickupNode = extractNode(pkg.getPickupAddress());
        String dropNode = extractNode(pkg.getDropAddress());

        if (pickupNode == null || dropNode == null) {
            // If addresses don't match our known zones, fallback to direct assignment
            new AlertDialog.Builder(this)
                    .setTitle("Accept Delivery?")
                    .setMessage("Route calculation unavailable for this area. Accept anyway?")
                    .setPositiveButton("Accept", (d, w) -> assignPackageToMe(pkg))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // Run Dijkstra
        String routeInfo = DijkstraAlgorithm.getShortestPath(cityGraph, pickupNode, dropNode);

        new AlertDialog.Builder(this)
                .setTitle("P2P Detour Cost (Dijkstra)")
                .setMessage("Package: " + pkg.getPackageId() + "\n\n" + routeInfo + "\n\nDo you want to accept this load?")
                .setPositiveButton("Accept Load", (dialog, which) -> assignPackageToMe(pkg))
                .setNegativeButton("Reject", null)
                .show();
    }

    private void assignPackageToMe(Package pkg) {
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(doc -> {
            String myName = doc.getString("fullName");
            pkg.setStatus(Package.STATUS_ASSIGNED);
            pkg.setAssignedRiderId(myName);
            db.collection("packages").document(pkg.getPackageId()).set(pkg);
            startActivity(new Intent(P2PDashboardActivity.this, RiderDashboardActivity.class));
            finish();
        });
    }

    // Helper to find a city zone in the address string
    private String extractNode(String address) {
        if (address == null) return null;
        String lower = address.toLowerCase(Locale.ROOT);
        if (lower.contains("uttara")) return "Uttara";
        if (lower.contains("mirpur")) return "Mirpur";
        if (lower.contains("gulshan")) return "Gulshan";
        if (lower.contains("farmgate")) return "Farmgate";
        if (lower.contains("dhanmondi")) return "Dhanmondi";
        if (lower.contains("motijheel")) return "Motijheel";
        return null;
    }

    private void loadAvailablePackages() {
        db.collection("packages").whereEqualTo("status", Package.STATUS_PENDING).addSnapshotListener((value, error) -> {
            if (value != null) {
                p2pPackages.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Package p = doc.toObject(Package.class);
                    if (p != null) {
                        p2pPackages.add(p);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}