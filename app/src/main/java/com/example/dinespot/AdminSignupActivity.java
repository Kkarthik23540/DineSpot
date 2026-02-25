package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminSignupActivity extends AppCompatActivity {

    private EditText etAdminSignupName, etAdminSignupPhone, etAdminSignupEmail, etAdminSignupPassword, etAdminSignupConfirmPassword;
    private Button btnAdminSignupSubmit;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_signup);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etAdminSignupName = findViewById(R.id.etAdminSignupName);
        etAdminSignupPhone = findViewById(R.id.etAdminSignupPhone);
        etAdminSignupEmail = findViewById(R.id.etAdminSignupEmail);
        etAdminSignupPassword = findViewById(R.id.etAdminSignupPassword);
        etAdminSignupConfirmPassword = findViewById(R.id.etAdminSignupConfirmPassword);
        btnAdminSignupSubmit = findViewById(R.id.btnAdminSignupSubmit);

        btnAdminSignupSubmit.setOnClickListener(v -> registerAdmin());
    }

    private void registerAdmin() {
        String name = etAdminSignupName.getText().toString().trim();
        String phone = etAdminSignupPhone.getText().toString().trim();
        String email = etAdminSignupEmail.getText().toString().trim();
        String password = etAdminSignupPassword.getText().toString().trim();
        String confirmPassword = etAdminSignupConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etAdminSignupName.setError("Name is required!");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etAdminSignupPhone.setError("Phone number is required!");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etAdminSignupEmail.setError("Email is required!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etAdminSignupPassword.setError("Password is required!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etAdminSignupConfirmPassword.setError("Passwords do not match!");
            return;
        }

        if (password.length() < 6) {
            etAdminSignupPassword.setError("Password must be at least 6 characters long!");
            return;
        }

        // Register admin in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Store additional details in Firestore
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> adminDetails = new HashMap<>();
                        adminDetails.put("name", name);
                        adminDetails.put("phone", phone);
                        adminDetails.put("email", email);

                        db.collection("admins").document(userId)
                                .set(adminDetails)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Admin Registered Successfully", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, AdminDashboardActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
