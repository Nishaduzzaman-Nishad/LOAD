package com.example.load_app.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.load_app.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvName, tvEmail, tvPhone;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName = findViewById(R.id.tvProfName);
        tvEmail = findViewById(R.id.tvProfEmail);
        tvPhone = findViewById(R.id.tvProfPhone);
        ivAvatar = findViewById(R.id.ivProfileAvatar);
        Button btnEdit = findViewById(R.id.btnEditProfile);
        Button btnLogout = findViewById(R.id.btnProfLogout);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        SwitchMaterial switchTheme = findViewById(R.id.switchProfileTheme);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switchTheme.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        // THE FIX: Only trigger theme change if the user actually pressed the switch
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                finish(); // Go back to the previous dashboard
                return true;
            }
            return true;
        });

        btnEdit.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    tvName.setText(doc.getString("fullName"));
                    tvEmail.setText(doc.getString("email"));
                    tvPhone.setText(doc.getString("phone"));

                    String base64Image = doc.getString("profileImage");
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivAvatar.setImageBitmap(decodedByte);
                            ivAvatar.setImageTintList(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }
}