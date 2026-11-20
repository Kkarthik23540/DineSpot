package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends CommonBaseActivity {

    private EditText etAdminEmail, etAdminPassword;
    private Button btnAdminLogin;
    private android.widget.ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_login);

        mAuth = FirebaseAuth.getInstance();

        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
        progressBar = findViewById(R.id.adminLoginProgress);

        btnAdminLogin.setOnClickListener(v -> loginAdmin());
    }

    private void loginAdmin() {
        String email = etAdminEmail.getText().toString().trim();
        String password = etAdminPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Fields missing", Toast.LENGTH_SHORT).show();
            return;
        }

        toggleLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String userEmail = mAuth.getCurrentUser().getEmail();
                        if (userEmail != null) {
                            validateAdminAccess(userEmail);
                        }
                    } else {
                        toggleLoading(false);
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                        Toast.makeText(this, "Login Failed: " + errMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void validateAdminAccess(String userEmail) {
        String cleanEmail = userEmail.toLowerCase().trim();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Check if email is in regular Users collection (Silently block)
        db.collection("users").whereEqualTo("email", cleanEmail).get()
                .addOnSuccessListener(userSnaps -> {
                    if (!userSnaps.isEmpty()) {
                        handleAuthFailure("Invalid email or password."); // Generic message
                    } else {
                        // 2. Check Admin collection OR Root Config
                        checkAdminCollection(cleanEmail);
                    }
                });
    }

    private void checkAdminCollection(String cleanEmail) {
        FirebaseFirestore.getInstance().collection("admins")
                .whereEqualTo("email", cleanEmail)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        proceedToDashboard();
                    } else {
                        checkRootConfig(cleanEmail);
                    }
                })
                .addOnFailureListener(e -> handleAuthFailure("Authentication failed."));
    }

    private void checkRootConfig(String email) {
        FirebaseFirestore.getInstance().collection("app_config")
                .document("admin_settings")
                .get()
                .addOnSuccessListener(doc -> {
                    String rootEmail = doc.getString("root_email");
                    if (rootEmail != null && rootEmail.equalsIgnoreCase(email)) {
                        proceedToDashboard();
                    } else {
                        handleAuthFailure("Invalid email or password.");
                    }
                })
                .addOnFailureListener(e -> handleAuthFailure("Authentication failed."));
    }

    private void proceedToDashboard() {
        toggleLoading(false);
        Toast.makeText(this, "Admin Login Successful", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, AdminDashboardActivity.class));
        finish();
    }

    private void handleAuthFailure(String msg) {
        mAuth.signOut();
        toggleLoading(false);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void toggleLoading(boolean isLoading) {
        btnAdminLogin.setText(isLoading ? "" : "Sign In");
        btnAdminLogin.setEnabled(!isLoading);
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
