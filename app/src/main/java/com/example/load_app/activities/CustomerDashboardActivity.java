package com.example.load_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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

public class CustomerDashboardActivity extends AppCompatActivity {
    private RecyclerView rvCustomerPackages;
    private PackageAdapter adapter;
    private List<Package> customerPackages;
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

        setContentView(R.layout.activity_customer_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvCustomerPackages = findViewById(R.id.rvCustomerPackages);
        Button btnCreateRequest = findViewById(R.id.btnCreateRequest);
        TextView tvLogout = findViewById(R.id.tvLogout);

        tvLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(CustomerDashboardActivity.this, LoginActivity.class));
            finish();
        });

        btnCreateRequest.setOnClickListener(v -> {
            // We will update CreatePackageActivity next to handle OTPs!
            startActivity(new Intent(CustomerDashboardActivity.this, CreatePackageActivity.class));
        });

        customerPackages = new ArrayList<>();
        adapter = new PackageAdapter(customerPackages, null);
        rvCustomerPackages.setLayoutManager(new LinearLayoutManager(this));
        rvCustomerPackages.setAdapter(adapter);

        loadCustomerPackages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerPackages();
    }

    private void loadCustomerPackages() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        db.collection("packages").addSnapshotListener((value, error) -> {
            if (value != null) {
                customerPackages.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Package p = doc.toObject(Package.class);
                    // Only show packages created by THIS customer
                    if (p != null && myId.equals(p.getCustomerId())) {
                        customerPackages.add(p);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}