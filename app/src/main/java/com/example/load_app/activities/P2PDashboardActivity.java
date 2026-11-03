package com.example.load_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load_app.R;
import com.example.load_app.adapters.PackageAdapter;
import com.example.load_app.models.Package;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class P2PDashboardActivity extends AppCompatActivity {
    private RecyclerView rvP2PPackages;
    private PackageAdapter adapter;
    private List<Package> p2pPackages;
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

        setContentView(R.layout.activity_p2p_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvP2PPackages = findViewById(R.id.rvP2PPackages);
        TextView tvLogout = findViewById(R.id.tvLogout);

        tvLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(P2PDashboardActivity.this, LoginActivity.class));
            finish();
        });

        p2pPackages = new ArrayList<>();
        adapter = new PackageAdapter(p2pPackages, pkg -> {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(doc -> {
                String myName = doc.getString("fullName");
                pkg.setStatus(Package.STATUS_ASSIGNED);
                pkg.setAssignedRiderId(myName);
                db.collection("packages").document(pkg.getPackageId()).set(pkg);
                startActivity(new Intent(P2PDashboardActivity.this, RiderDashboardActivity.class));
                finish();
            });
        });

        rvP2PPackages.setLayoutManager(new LinearLayoutManager(this));
        rvP2PPackages.setAdapter(adapter);

        loadAvailablePackages();
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