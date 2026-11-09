package com.example.load_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.load_app.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogleLogin;
    private TextView tvGoToRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean isPhoneLogin = false;
    private int RC_SIGN_IN = 9001;

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

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    isPhoneLogin = false;
                    etEmail.setHint("Email Address");
                    etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                } else {
                    isPhoneLogin = true;
                    etEmail.setHint("Phone Number (e.g. +88017...)");
                    etEmail.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnLogin.setOnClickListener(v -> handleLogin());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        tvGoToRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void handleLogin() {
        String input = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (input.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPhoneLogin) {
            String cleanPhone = sanitizePhoneNumber(input);
            if (cleanPhone == null) {
                Toast.makeText(this, "Invalid Phone! Use 01XXXXXXXXX or +8801XXXXXXXXX", Toast.LENGTH_LONG).show();
                return;
            }
            input = cleanPhone + "@load-app.com";
        } else {
            if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                Toast.makeText(this, "Invalid Email format", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(input, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                routeUserBasedOnRole();
            } else {
                Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                btnLogin.setEnabled(true);
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                String userId = mAuth.getCurrentUser().getUid();

                // Check if user exists
                db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // FIRST TIME GOOGLE LOGIN: Ask for role!
                        showRoleSelectionDialog(userId);
                    } else {
                        // Returning Google user, route them normally
                        routeUserBasedOnRole();
                    }
                });
            } else {
                Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // NEW METHOD: Beautiful Dialog to select role for Google Users
    private void showRoleSelectionDialog(String userId) {
        String[] roles = {"CUSTOMER", "RIDER", "P2P", "ADMIN"};

        new AlertDialog.Builder(this)
                .setTitle("Select Your Role")
                .setMessage("Welcome! Please select how you will use LOAD:")
                .setCancelable(false)
                .setItems(roles, (dialog, which) -> {
                    String selectedRole = roles[which];

                    // Create user profile in Firestore with selected role
                    java.util.Map<String, Object> user = new java.util.HashMap<>();
                    user.put("userId", userId);
                    user.put("fullName", mAuth.getCurrentUser().getDisplayName());
                    user.put("email", mAuth.getCurrentUser().getEmail());
                    user.put("phone", "");
                    user.put("role", selectedRole);

                    db.collection("users").document(userId).set(user).addOnSuccessListener(aVoid -> {
                        Toast.makeText(LoginActivity.this, "Registered as " + selectedRole, Toast.LENGTH_SHORT).show();
                        routeUserBasedOnRole();
                    });
                })
                .show();
    }

    private void routeUserBasedOnRole() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String role = doc.getString("role");
                Toast.makeText(LoginActivity.this, "Logged in as: " + role, Toast.LENGTH_SHORT).show();

                if ("RIDER".equals(role)) {
                    startActivity(new Intent(LoginActivity.this, RiderDashboardActivity.class));
                } else if ("CUSTOMER".equals(role)) {
                    startActivity(new Intent(LoginActivity.this, CustomerDashboardActivity.class));
                } else if ("P2P".equals(role)) {
                    startActivity(new Intent(LoginActivity.this, P2PDashboardActivity.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                }
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "User profile not found in Database.", Toast.LENGTH_LONG).show();
                btnLogin.setEnabled(true);
            }
        });
    }

    private String sanitizePhoneNumber(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("880") && digits.length() == 13) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("88") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        }
        if (digits.startsWith("01") && digits.length() == 11) {
            return digits;
        }
        return null;
    }
}