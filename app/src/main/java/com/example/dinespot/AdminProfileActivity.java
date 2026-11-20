package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends CommonBaseActivity {

    private TextView tvAdminNameHeader, tvAdminEmailHeader, tvAdminIdHeader;
    private EditText etAdminName, etAdminPhone;
    private ImageButton btnEditName, btnEditPhone;
    private View btnManageStaff, btnAuthorizeNewAdmin;
    private View dividerStaff1, dividerStaff2;
    private ImageView btnThemeToggle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String currentAdminDocId = null;
    private String currentAdminRole = "staff"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupToolbar("Admin Profile", false);
        initViews();
        setupNavigation();

        btnThemeToggle.setOnClickListener(v -> toggleTheme());
        updateThemeIcon();

        populateAdminDetails();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::populateAdminDetails);
        }

        btnEditName.setOnClickListener(v -> toggleEdit(etAdminName, btnEditName, "name"));
        btnEditPhone.setOnClickListener(v -> toggleEdit(etAdminPhone, btnEditPhone, "phone"));

        findViewById(R.id.tv_admin_reviews).setOnClickListener(v -> 
            startActivity(new Intent(this, AdminFeedbackActivity.class)));

        findViewById(R.id.tv_admin_support_inbox).setOnClickListener(v -> 
            startActivity(new Intent(this, AdminSupportInboxActivity.class)));

        btnManageStaff.setOnClickListener(v -> 
            startActivity(new Intent(this, AdminStaffManagementActivity.class)));

        btnAuthorizeNewAdmin.setOnClickListener(v -> 
            startActivity(new Intent(this, AdminAddStaffActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutConfirmation());
    }

    private void initViews() {
        tvAdminNameHeader = findViewById(R.id.tv_admin_name_header);
        tvAdminEmailHeader = findViewById(R.id.tv_admin_email_header);
        tvAdminIdHeader = findViewById(R.id.tv_admin_id_header);
        
        etAdminName = findViewById(R.id.etAdminName);
        etAdminPhone = findViewById(R.id.etAdminPhone);
        
        btnEditName = findViewById(R.id.btn_edit_name);
        btnEditPhone = findViewById(R.id.btn_edit_phone);
        
        btnManageStaff = findViewById(R.id.btnManageStaff);
        btnAuthorizeNewAdmin = findViewById(R.id.btnAuthorizeNewAdmin);
        dividerStaff1 = findViewById(R.id.divider_staff_1);
        dividerStaff2 = findViewById(R.id.divider_staff_2);
        
        btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupNavigation() {
        BottomNavigationView bnv = findViewById(R.id.bottom_navigation);
        CommonNavigationHelper.setupAdminNavigation(this, bnv, R.id.admin_profile);
    }

    private void populateAdminDetails() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        db.collection("admins").whereEqualTo("email", user.getEmail().toLowerCase().trim()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            
            if (!queryDocumentSnapshots.isEmpty()) {
                DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                currentAdminDocId = doc.getId();
                currentAdminRole = doc.getString("role");
                if (currentAdminRole == null) currentAdminRole = "staff";
                
                String name = doc.getString("name");
                String email = doc.getString("email");
                String empId = doc.getString("AdminId");
                if (empId == null) empId = doc.getString("empId"); // Fallback
                if (empId == null) empId = doc.getString("userId"); // Extra Fallback
                String phone = doc.getString("phone");

                tvAdminNameHeader.setText(name != null ? name : "Administrator");

                if (tvAdminEmailHeader != null) {
                    tvAdminEmailHeader.setVisibility(View.VISIBLE);
                    tvAdminEmailHeader.setText(email != null ? email : "");
                }
                
                if (tvAdminIdHeader != null) {
                    tvAdminIdHeader.setVisibility(View.VISIBLE);
                    tvAdminIdHeader.setText("ID: " + (empId != null ? empId : "N/A"));
                }
                
                etAdminName.setText(name != null ? name : "");
                etAdminPhone.setText(phone != null ? phone : "");
                
                // Show/Hide restricted features based on role
                boolean isSuper = "super".equalsIgnoreCase(currentAdminRole);
                setStaffToolsVisibility(isSuper);
            } else {
                // Fallback for First Admin (No record in DB yet)
                setStaffToolsVisibility(true);
                tvAdminNameHeader.setText("Admin Found");
                tvAdminIdHeader.setText("Role Setup Required");
            }
        }).addOnFailureListener(e -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            setStaffToolsVisibility(true); // Show on error so you're not locked out
        });
    }

    private void setStaffToolsVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (btnManageStaff != null) btnManageStaff.setVisibility(visibility);
        if (btnAuthorizeNewAdmin != null) btnAuthorizeNewAdmin.setVisibility(visibility);
        if (dividerStaff1 != null) dividerStaff1.setVisibility(visibility);
        if (dividerStaff2 != null) dividerStaff2.setVisibility(visibility);
    }

    private void toggleEdit(EditText editText, ImageButton button, String field) {
        if (!editText.isEnabled()) {
            editText.setEnabled(true);
            editText.requestFocus();
            button.setImageResource(R.drawable.baseline_done_24);
        } else {
            String value = editText.getText().toString().trim();
            if (TextUtils.isEmpty(value)) {
                Toast.makeText(this, "Field cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            updateAdminField(field, value);
            editText.setEnabled(false);
            button.setImageResource(R.drawable.baseline_edit_24);
        }
    }

    private void updateAdminField(String field, String value) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        showLoading();
        
        if (currentAdminDocId != null) {
            // Document exists, update it using set merge
            Map<String, Object> updateData = new HashMap<>();
            updateData.put(field, value);
            db.collection("admins").document(currentAdminDocId)
                    .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> handleUpdateSuccess(field, value))
                    .addOnFailureListener(this::handleUpdateFailure);
        } else {
            // Document doesn't exist, create it (First time setup)
            db.collection("admins").get().addOnSuccessListener(allAdmins -> {
                int count = allAdmins.size() + 1;
                String empId = String.format(java.util.Locale.getDefault(), "DS-ADMIN-%02d", count);
                
                Map<String, Object> data = new java.util.LinkedHashMap<>();
                data.put("AdminId", empId);
                data.put("name", (value != null && field.equals("name")) ? value : "Root Admin");
                data.put("email", user.getEmail() != null ? user.getEmail().toLowerCase() : "");
                data.put("role", "super");
                data.put("status", "active");

                // Use name as Document ID (Sanitized)
                String nameForId = (String) data.get("name");
                String docId = nameForId.toLowerCase().trim().replace(" ", "_").replaceAll("[^a-z0-9_]", "");

                db.collection("admins").document(docId).set(data).addOnSuccessListener(v -> {
                    currentAdminDocId = docId;
                    handleUpdateSuccess(field, value);
                }).addOnFailureListener(this::handleUpdateFailure);
            });
        }
    }

    private void handleUpdateSuccess(String field, String value) {
        hideLoading();
        if (field.equals("name")) tvAdminNameHeader.setText(value);
        if (field.equals("AdminId") || field.equals("empId")) tvAdminIdHeader.setText("Admin ID: " + value);
        Snackbar.make(findViewById(android.R.id.content), "Profile Saved Successfully", Snackbar.LENGTH_SHORT).show();
    }

    private void handleUpdateFailure(Exception e) {
        hideLoading();
        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void toggleTheme() {
        int currentMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    private void updateThemeIcon() {
        int currentMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            btnThemeToggle.setImageResource(R.drawable.ic_sun);
        } else {
            btnThemeToggle.setImageResource(R.drawable.ic_moon);
        }
    }

    private void showLogoutConfirmation() {
        showCustomDialog(R.drawable.baseline_logout_24, 
                "Sign Out", 
                "Are you sure you want to sign out from the Admin Dashboard?", 
                "Sign Out", this::performLogout, 
                "Cancel", null);
    }

    private void performLogout() {
        mAuth.signOut();
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                startActivity(new Intent(this, CommonBothLoginActivity.class));
                finishAffinity();
            });
        } catch (Exception e) {
            startActivity(new Intent(this, CommonBothLoginActivity.class));
            finishAffinity();
        }
    }
}
