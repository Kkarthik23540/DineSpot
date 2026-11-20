package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminAddStaffActivity extends CommonBaseActivity {

    private EditText etEmail, etName, etPhone, etPassword;
    private android.widget.RadioGroup rgStaffRole;
    private Button btnAuthorize;
    private FirebaseFirestore db;
    private String adminDocId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_add_staff);

        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etStaffEmail);
        etName = findViewById(R.id.etStaffName);
        etPhone = findViewById(R.id.etStaffPhone);
        etPassword = findViewById(R.id.etStaffPassword);
        rgStaffRole = findViewById(R.id.rgStaffRole);
        btnAuthorize = findViewById(R.id.btnAuthorize);

        adminDocId = getIntent().getStringExtra("admin_doc_id");
        
        if (adminDocId != null) {
            setupEditMode();
        } else {
            setupToolbar("Authorize Staff", true);
        }

        btnAuthorize.setOnClickListener(v -> {
            if (adminDocId != null) {
                updateStaff();
            } else {
                authorizeStaff();
            }
        });
    }

    private void setupEditMode() {
        setupToolbar("Edit Staff Details", true);
        btnAuthorize.setText("Save Changes");
        
        // Hide password for existing staff
        View tilPassword = findViewById(R.id.tilStaffPassword);
        if (tilPassword != null) tilPassword.setVisibility(View.GONE);
        
        // Populate fields
        etEmail.setText(getIntent().getStringExtra("admin_email"));
        etEmail.setEnabled(false); 
        etName.setText(getIntent().getStringExtra("admin_name"));
        etPhone.setText(getIntent().getStringExtra("admin_phone"));
        
        String role = getIntent().getStringExtra("admin_role");
        if ("super".equalsIgnoreCase(role)) {
            rgStaffRole.check(R.id.rbRoleSuper);
        } else {
            rgStaffRole.check(R.id.rbRoleStaff);
        }
    }

    private void authorizeStaff() {
        String email = etEmail.getText().toString().trim().toLowerCase();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        String role = (rgStaffRole.getCheckedRadioButtonId() == R.id.rbRoleSuper) ? "super" : "staff";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(name) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Email, Name and Password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();

        db.collection("admins").whereEqualTo("email", email).get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        hideLoading();
                        Toast.makeText(this, "This email is already an Admin.", Toast.LENGTH_LONG).show();
                    } else {
                        // Check if registered as regular user
                        db.collection("users").whereEqualTo("email", email).get()
                                .addOnSuccessListener(userSnaps -> {
                                    if (!userSnaps.isEmpty()) {
                                        hideLoading();
                                        Toast.makeText(this, "This email belongs to a regular user.", Toast.LENGTH_LONG).show();
                                    } else {
                                        generateIdAndCreateAuth(email, name, phone, password, role);
                                    }
                                });
                    }
                });
    }

    private void generateIdAndCreateAuth(String email, String name, String phone, String password, String role) {
        db.collection("admins").get().addOnSuccessListener(allAdmins -> {
            int count = allAdmins.size() + 1;
            String empId = String.format(Locale.getDefault(), "DS-ADMIN-%02d", count);
            
            Map<String, Object> staffData = new java.util.LinkedHashMap<>();
            staffData.put("AdminId", empId);
            staffData.put("name", name);
            staffData.put("email", email);
            staffData.put("phone", phone);
            staffData.put("role", role);
            staffData.put("status", "active");
            
            String currentUserEmail = "Unknown";
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail();
            }
            staffData.put("authorizedBy", currentUserEmail);

            createAuthAccountAndSave(email, password, staffData, name);
        });
    }

    private void createAuthAccountAndSave(String email, String password, Map<String, Object> staffData, String name) {
        com.google.firebase.auth.FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        // Use Admin name as Document ID
                        String docId = name.toLowerCase().trim().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
                        
                        db.collection("admins").document(docId).set(staffData).addOnSuccessListener(v -> {
                            logAdminAction("Staff Authorized", email);
                            if (task.getResult().getUser() != null) task.getResult().getUser().sendEmailVerification();
                            hideLoading();
                            Toast.makeText(this, "Admin Account Created Successfully!", Toast.LENGTH_LONG).show();
                            
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                            Toast.makeText(this, "Staff authorized! Please re-login.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, CommonBothLoginActivity.class));
                            finishAffinity();
                        });
                    } else {
                        hideLoading();
                        String err = (task.getException() != null) ? task.getException().getMessage() : "Creation Failed";
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateStaff() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String role = (rgStaffRole.getCheckedRadioButtonId() == R.id.rbRoleSuper) ? "super" : "staff";

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("role", role);

        db.collection("admins").document(adminDocId).update(updates)
                .addOnSuccessListener(v -> {
                    hideLoading();
                    Toast.makeText(this, "Staff details updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override protected void onDestroy() { super.onDestroy(); }
}
