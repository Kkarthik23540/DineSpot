package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminStaffManagementActivity extends CommonBaseActivity {

    private RecyclerView rvStaffList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddStaff;
    private com.google.android.material.button.MaterialButton btnSortStaff;
    private EditText etStaffSearch;
    private View llNoStaff;
    private TextView tvNoStaffMsg, tvNoStaffDesc;
    
    private final List<AdminInfo> staffList = new ArrayList<>();
    private final List<AdminInfo> fullStaffList = new ArrayList<>();
    private StaffAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration staffListener;
    private String currentSearchQuery = "";
    private String currentSortOrder = "ID"; // "ID" or "NAME"

    public static class AdminInfo {
        public String id, name, email, AdminId, phone, role;
        public AdminInfo() {}
        public String getName() { return name; }
        public String getEmail() { return email; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_staff_management);

        setupToolbar("Staff Management", true);
        db = FirebaseFirestore.getInstance();

        rvStaffList = findViewById(R.id.rvStaffList);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        fabAddStaff = findViewById(R.id.fabAddStaff);
        btnSortStaff = findViewById(R.id.btnSortStaff);
        etStaffSearch = findViewById(R.id.etStaffSearch);
        llNoStaff = findViewById(R.id.llNoStaff);
        tvNoStaffMsg = findViewById(R.id.tvNoStaffMsg);
        tvNoStaffDesc = findViewById(R.id.tvNoStaffDesc);

        rvStaffList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StaffAdapter();
        rvStaffList.setAdapter(adapter);

        fabAddStaff.setOnClickListener(v -> 
            startActivity(new Intent(this, AdminAddStaffActivity.class)));

        btnSortStaff.setOnClickListener(v -> showSortPopup());

        etStaffSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchStaff);
        }

        fetchStaff();
    }

    private void showSortPopup() {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, btnSortStaff);
        popup.getMenu().add(0, 0, 0, "Sort by Admin ID");
        popup.getMenu().add(0, 1, 1, "Sort by Name (A-Z)");
        popup.setOnMenuItemClickListener(item -> {
            currentSortOrder = item.getItemId() == 0 ? "ID" : "NAME";
            applyFilter();
            return true;
        });
        popup.show();
    }

    private void fetchStaff() {
        if (staffListener != null) staffListener.remove();
        
        staffListener = db.collection("admins").addSnapshotListener((value, error) -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (error != null) {
                android.util.Log.e("StaffMgmt", "Error: " + error.getMessage());
                return;
            }
            if (value == null) return;

            fullStaffList.clear();
            for (QueryDocumentSnapshot doc : value) {
                try {
                    AdminInfo info = new AdminInfo();
                    info.id = doc.getId();
                    info.name = doc.getString("name");
                    info.email = doc.getString("email");
                    info.AdminId = doc.getString("AdminId");
                    if (info.AdminId == null) info.AdminId = doc.getString("empId");
                    info.phone = doc.getString("phone");
                    info.role = doc.getString("role");
                    
                    fullStaffList.add(info);
                } catch (Exception e) {
                    android.util.Log.e("StaffMgmt", "Parse error: " + e.getMessage());
                }
            }
            applyFilter();
        });
    }

    private void applyFilter() {
        staffList.clear();
        for (AdminInfo info : fullStaffList) {
            String name = (info.name != null) ? info.name.toLowerCase() : "";
            String email = (info.email != null) ? info.email.toLowerCase() : "";
            
            if (name.contains(currentSearchQuery) || email.contains(currentSearchQuery)) {
                staffList.add(info);
            }
        }

        if (staffList.isEmpty()) {
            llNoStaff.setVisibility(View.VISIBLE);
            rvStaffList.setVisibility(View.GONE);
            tvNoStaffMsg.setText(currentSearchQuery.isEmpty() ? "No administrative staff found" : "No results found");
            tvNoStaffDesc.setText(currentSearchQuery.isEmpty() ? "Authorize new staff to see them here." : "No staff matches '" + currentSearchQuery + "'");
        } else {
            // Apply sorting
            if ("NAME".equals(currentSortOrder)) {
                java.util.Collections.sort(staffList, (a, b) -> {
                    String n1 = a.name != null ? a.name : "";
                    String n2 = b.name != null ? b.name : "";
                    return n1.compareToIgnoreCase(n2);
                });
            } else {
                java.util.Collections.sort(staffList, (a, b) -> {
                    String id1 = a.AdminId != null ? a.AdminId : "";
                    String id2 = b.AdminId != null ? b.AdminId : "";
                    return id1.compareToIgnoreCase(id2);
                });
            }

            llNoStaff.setVisibility(View.GONE);
            rvStaffList.setVisibility(View.VISIBLE);
        }
        
        adapter.notifyDataSetChanged();
    }

    private class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {
        @NonNull
        @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_item_staff, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AdminInfo admin = staffList.get(position);
            if (admin == null) return;
            
            holder.tvName.setText(admin.name != null ? admin.name : "Unnamed Admin");
            holder.tvEmail.setText(admin.email != null ? admin.email : "No Email");
            
            String idVal = admin.AdminId;
            holder.tvId.setText("Admin ID: " + (idVal != null ? idVal : "N/A"));

            // Set Initial Avatar with Theme Color
            if (admin.name != null && !admin.name.isEmpty()) {
                String initial = admin.name.substring(0, 1).toUpperCase();
                holder.tvInitial.setText(initial);
                
                // Use consistent Theme Blue color
                holder.cardAvatar.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(AdminStaffManagementActivity.this, R.color.colorPrimary));
                holder.tvInitial.setTextColor(android.graphics.Color.WHITE);
            } else {
                holder.tvInitial.setText("A");
                holder.cardAvatar.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(AdminStaffManagementActivity.this, R.color.colorPrimaryLight));
                holder.tvInitial.setTextColor(androidx.core.content.ContextCompat.getColor(AdminStaffManagementActivity.this, R.color.colorPrimary));
            }

            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(AdminStaffManagementActivity.this, AdminAddStaffActivity.class);
                intent.putExtra("admin_doc_id", admin.id);
                intent.putExtra("admin_email", admin.email);
                intent.putExtra("admin_name", admin.name);
                intent.putExtra("admin_phone", admin.phone);
                intent.putExtra("admin_role", admin.role);
                startActivity(intent);
            });
            
            holder.btnRemove.setOnClickListener(v -> removeStaff(admin));
        }

        @Override public int getItemCount() { return staffList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvId, tvInitial;
            ImageButton btnRemove, btnEdit;
            com.google.android.material.card.MaterialCardView cardAvatar;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvStaffName);
                tvEmail = v.findViewById(R.id.tvStaffEmail);
                tvId = v.findViewById(R.id.tvStaffId);
                tvInitial = v.findViewById(R.id.tvStaffInitial);
                cardAvatar = v.findViewById(R.id.cardStaffAvatar);
                btnRemove = v.findViewById(R.id.btnRemoveStaff);
                btnEdit = v.findViewById(R.id.btnEditStaff);
            }
        }
    }

    private void removeStaff(AdminInfo admin) {
        showCustomDialog(R.drawable.baseline_delete_outline_24,
                "Revoke Access",
                "Are you sure you want to remove " + admin.email + " from administrative access?",
                "Revoke", () -> {
                    showLoading();
                    db.collection("admins").document(admin.id).delete().addOnSuccessListener(v -> {
                        logAdminAction("Staff Access Revoked", admin.email);
                        hideLoading();
                        Toast.makeText(this, "Access Revoked", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> hideLoading());
                },
                "Cancel", null);
    }

    @Override
    protected void onDestroy() {
        if (staffListener != null) staffListener.remove();
        super.onDestroy();
    }
}
