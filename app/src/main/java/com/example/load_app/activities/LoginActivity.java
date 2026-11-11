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
import androidx.appcompat.app.AppCompatDelegate;

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
    private com.google.android.material.textfield.TextInputLayout tilEmail;
    private com.google.android.material.switchmaterial.SwitchMaterial switchTheme;
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
        tilEmail = findViewById(R.id.tilEmail);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        switchTheme = findViewById(R.id.switchLoginTheme);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        switchTheme.setChecked(currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

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
                    tilEmail.setHint("Email Address");
                    etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                } else {
                    isPhoneLogin = true;
                    tilEmail.setHint("Phone Number (e.g. 017...)");
                    etEmail.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
                }
                etEmail.setText("");
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

        btnLogin.setEnabled(false);

        if (isPhoneLogin) {
            String cleanPhone = sanitizePhoneNumber(input);
            if (cleanPhone == null) {
                Toast.makeText(this, "Invalid Phone! Use 01XXXXXXXXX", Toast.LENGTH_LONG).show();
                btnLogin.setEnabled(true);
                return;
            }

            // SMART FIX: Look up the user by phone in Firestore to find their actual Auth Email
            db.collection("users").whereEqualTo("phone", cleanPhone).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    // User exists, get their email
                    String actualEmail = task.getResult().getDocuments().get(0).getString("email");

                    // If the stored email doesn't have an @, it means they registered with phone directly in the email field
                    if (actualEmail != null && !actualEmail.contains("@")) {
                        actualEmail = sanitizePhoneNumber(actualEmail) + "@load-app.com";
                    }

                    // If actualEmail is somehow null, fallback to default
                    if (actualEmail == null) {
                        actualEmail = cleanPhone + "@load-app.com";
                    }

                    signInWithEmail(actualEmail, password);
                } else {
                    // If not found by phone in Firestore, try the default mapped email
                    signInWithEmail(cleanPhone + "@load-app.com", password);
                }
            });
        } else {
            if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                Toast.makeText(this, "Invalid Email format", Toast.LENGTH_SHORT).show();
                btnLogin.setEnabled(true);
                return;
            }
            signInWithEmail(input, password);
        }
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                routeUserBasedOnRole();
            } else {
                Toast.makeText(LoginActivity.this, "Login Failed: Check credentials", Toast.LENGTH_LONG).show();
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
                db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showRoleSelectionDialog(userId);
                    } else {
                        routeUserBasedOnRole();
                    }
                });
            } else {
                Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRoleSelectionDialog(String userId) {
        String[] roles = {"CUSTOMER", "RIDER", "P2P", "ADMIN"};
        new AlertDialog.Builder(this)
                .setTitle("Select Your Role")
                .setCancelable(false)
                .setItems(roles, (dialog, which) -> {
                    String selectedRole = roles[which];
                    java.util.Map<String, Object> user = new java.util.HashMap<>();
                    user.put("userId", userId);
                    user.put("fullName", mAuth.getCurrentUser().getDisplayName());
                    user.put("email", mAuth.getCurrentUser().getEmail());
                    user.put("phone", "");
                    user.put("role", selectedRole);

                    db.collection("users").document(userId).set(user).addOnSuccessListener(aVoid -> {
                        routeUserBasedOnRole();
                    });
                }).show();
    }

    private void routeUserBasedOnRole() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String role = doc.getString("role");
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