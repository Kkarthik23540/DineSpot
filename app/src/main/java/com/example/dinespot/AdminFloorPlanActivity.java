package com.example.dinespot;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminFloorPlanActivity extends CommonBaseActivity {

    private String restaurantId, restaurantName;
    private RecyclerView rvFloorPlan;
    private TextView tvRestaurantTitle;
    private com.google.android.material.chip.ChipGroup cgStatusFilter;
    private android.widget.EditText etSearchGuest;
    private View llNoResults;
    private TextView tvNoResultsMsg;
    private com.google.android.material.button.MaterialButton btnSortFloor;
    private FirebaseFirestore db;
    
    private final List<FloorItem> displayList = new ArrayList<>();
    private final Map<String, List<TableVisual>> tablesByType = new HashMap<>();
    private String currentStatusFilter = "ALL";
    private String currentNameQuery = "";
    private String currentSortOrder = "NUMBER"; // "NUMBER" or "NAME"
    
    private TableVisualAdapter adapter;
    private ListenerRegistration bookingListener, restaurantListener;

    interface FloorItem { int getType(); }

    public static class SectionHeader implements FloorItem {
        String title;
        SectionHeader(String title) { this.title = title; }
        @Override public int getType() { return 0; }
    }

    public static class TableVisual implements FloorItem {
        String type;
        String status = "FREE"; 
        String currentBookingId, userName, userPhone;
        int tableNumber;

        TableVisual(String type, int num) { this.type = type; this.tableNumber = num; }
        @Override public int getType() { return 1; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_floor_plan);

        restaurantId = getIntent().getStringExtra("restaurant_id");
        restaurantName = getIntent().getStringExtra("restaurant_name");

        setupToolbar("Live Floor Plan", true);
        tvRestaurantTitle = findViewById(R.id.tvRestaurantTitle);
        tvRestaurantTitle.setText(restaurantName != null ? restaurantName : "Restaurant");

        rvFloorPlan = findViewById(R.id.rvFloorPlan);
        cgStatusFilter = findViewById(R.id.cgStatusFilter);
        etSearchGuest = findViewById(R.id.etSearchGuest);
        llNoResults = findViewById(R.id.llNoResults);
        tvNoResultsMsg = findViewById(R.id.tvNoResultsMsg);
        btnSortFloor = findViewById(R.id.btnSortFloor);
        
        cgStatusFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFree) currentStatusFilter = "FREE";
            else if (checkedId == R.id.chipPending) currentStatusFilter = "PENDING";
            else if (checkedId == R.id.chipSeated) currentStatusFilter = "SEATED";
            else currentStatusFilter = "ALL";
            rebuildDisplayList();
        });

        etSearchGuest.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentNameQuery = s.toString().toLowerCase().trim();
                rebuildDisplayList();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnSortFloor.setOnClickListener(v -> showSortPopup());

        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return displayList.get(position).getType() == 0 ? 4 : 1;
            }
        });
        rvFloorPlan.setLayoutManager(layoutManager);
        
        adapter = new TableVisualAdapter();
        rvFloorPlan.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadInitialTables();
    }

    private void showSortPopup() {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, btnSortFloor);
        popup.getMenu().add(0, 0, 0, "Sort by Table Number");
        popup.getMenu().add(0, 1, 1, "Sort by Guest Name");
        popup.setOnMenuItemClickListener(item -> {
            currentSortOrder = item.getItemId() == 0 ? "NUMBER" : "NAME";
            rebuildDisplayList();
            return true;
        });
        popup.show();
    }

    private void loadInitialTables() {
        if (restaurantId == null) return;
        
        restaurantListener = db.collection("restaurants").document(restaurantId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> types = (Map<String, Object>) snapshot.get("tableTypes");
                        if (types != null && !types.isEmpty()) {
                            tablesByType.clear();
                            List<String> sortedKeys = new ArrayList<>(types.keySet());
                            Collections.sort(sortedKeys, (a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
                            
                            int globalCount = 1;
                            for (String key : sortedKeys) {
                                List<TableVisual> tables = new ArrayList<>();
                                Object val = types.get(key);
                                int count = (val instanceof Number) ? ((Number) val).intValue() : 0;
                                for (int i = 0; i < count; i++) {
                                    tables.add(new TableVisual(key, globalCount++));
                                }
                                tablesByType.put(key, tables);
                            }
                            startBookingListener();
                        } else {
                            // No tables defined
                            tablesByType.clear();
                            rebuildDisplayList();
                        }
                    }
                });
    }

    private void startBookingListener() {
        if (bookingListener != null) bookingListener.remove();
        
        bookingListener = db.collection("bookings")
                .whereEqualTo("restaurantId", restaurantId)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    
                    for (List<TableVisual> list : tablesByType.values()) {
                        for (TableVisual t : list) {
                            t.status = "FREE";
                            t.currentBookingId = null;
                        }
                    }

                    for (QueryDocumentSnapshot doc : value) {
                        String bStatus = doc.getString("status");
                        String bType = doc.getString("tableType");
                        
                        if (bStatus != null && ("Pending".equalsIgnoreCase(bStatus) || "Accepted".equalsIgnoreCase(bStatus))) {
                            List<TableVisual> pool = tablesByType.get(bType);
                            if (pool != null) {
                                for (TableVisual t : pool) {
                                    if ("FREE".equals(t.status)) {
                                        t.status = "Accepted".equalsIgnoreCase(bStatus) ? "SEATED" : "PENDING";
                                        t.currentBookingId = doc.getId();
                                        t.userName = doc.getString("userName");
                                        t.userPhone = doc.getString("phoneNumber");
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    rebuildDisplayList();
                });
    }

    private void rebuildDisplayList() {
        displayList.clear();
        
        if (tablesByType.isEmpty()) {
            llNoResults.setVisibility(View.VISIBLE);
            tvNoResultsMsg.setText("This restaurant has no tables configured.");
            rvFloorPlan.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
            return;
        }

        List<String> sortedKeys = new ArrayList<>(tablesByType.keySet());
        Collections.sort(sortedKeys, (a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
        
        boolean hasAnyMatch = false;
        for (String key : sortedKeys) {
            List<TableVisual> tables = tablesByType.get(key);
            if (tables != null && !tables.isEmpty()) {
                List<TableVisual> filteredTables = new ArrayList<>();
                for (TableVisual t : tables) {
                    boolean matchesStatus = "ALL".equals(currentStatusFilter) || currentStatusFilter.equalsIgnoreCase(t.status);
                    boolean matchesName = currentNameQuery.isEmpty() || (t.userName != null && t.userName.toLowerCase().contains(currentNameQuery));
                    
                    if (matchesStatus && matchesName) {
                        filteredTables.add(t);
                    }
                }

                if (!filteredTables.isEmpty()) {
                    hasAnyMatch = true;
                    
                    // Apply Sorting to filtered tables
                    if ("NAME".equals(currentSortOrder)) {
                        Collections.sort(filteredTables, (t1, t2) -> {
                            String n1 = t1.userName != null ? t1.userName : "ZZZZ";
                            String n2 = t2.userName != null ? t2.userName : "ZZZZ";
                            return n1.compareToIgnoreCase(n2);
                        });
                    } else {
                        Collections.sort(filteredTables, (t1, t2) -> Integer.compare(t1.tableNumber, t2.tableNumber));
                    }

                    String title = key + " Person Tables";
                    if ("2".equals(key)) title = "Couple Tables (2P)";
                    else if ("4".equals(key)) title = "Family Tables (4P)";
                    else if ("6".equals(key)) title = "Group Tables (6P)";
                    else if ("10".equals(key)) title = "Party Tables (10P)";
                    
                    displayList.add(new SectionHeader(title));
                    displayList.addAll(filteredTables);
                }
            }
        }
        
        if (hasAnyMatch) {
            llNoResults.setVisibility(View.GONE);
            rvFloorPlan.setVisibility(View.VISIBLE);
        } else {
            llNoResults.setVisibility(View.VISIBLE);
            tvNoResultsMsg.setText(currentNameQuery.isEmpty() ? "No tables match current status." : "No guest found matching '" + currentNameQuery + "'");
            rvFloorPlan.setVisibility(View.GONE);
        }
        
        adapter.notifyDataSetChanged();
    }

    private class TableVisualAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public int getItemViewType(int position) { return displayList.get(position).getType(); }

        @NonNull
        @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_item_floor_section_header, parent, false));
            } else {
                return new TableViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_item_floor_table, parent, false));
            }
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            FloorItem item = displayList.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).tvTitle.setText(((SectionHeader) item).title);
            } else if (holder instanceof TableViewHolder) {
                bindTable((TableViewHolder) holder, (TableVisual) item);
            }
        }

        private void bindTable(TableViewHolder h, TableVisual table) {
            h.tvLabel.setText("Table " + table.tableNumber);
            h.tvType.setText(table.type + "P");
            h.tvStatus.setText(table.status);

            int color = ContextCompat.getColor(AdminFloorPlanActivity.this, R.color.colorSuccess);
            if ("SEATED".equals(table.status)) color = ContextCompat.getColor(AdminFloorPlanActivity.this, R.color.colorError);
            else if ("PENDING".equals(table.status)) color = Color.parseColor("#FF9800");

            h.card.setCardBackgroundColor(color);
            h.tvLabel.setTextColor(Color.WHITE);
            h.tvType.setTextColor(Color.WHITE);
            h.tvStatus.setTextColor(Color.WHITE);

            h.itemView.setOnClickListener(v -> {
                if ("SEATED".equals(table.status)) showSeatedTableActions(table);
                else if ("PENDING".equals(table.status)) showPendingTableActions(table);
                else showFreeTableActions(table);
            });
        }

        @Override public int getItemCount() { return displayList.size(); }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        HeaderViewHolder(View v) { super(v); tvTitle = v.findViewById(R.id.tvSectionTitle); }
    }

    static class TableViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel, tvType, tvStatus;
        MaterialCardView card;
        TableViewHolder(View v) { 
            super(v); 
            tvLabel = v.findViewById(R.id.tvTableLabel); 
            tvType = v.findViewById(R.id.tvTableType);
            tvStatus = v.findViewById(R.id.tvTableStatus);
            card = v.findViewById(R.id.cardTable);
        }
    }

    private void showSeatedTableActions(TableVisual table) {
        String[] options = {"Chat with Guest", "End Session (Free Table)", "View Guest Info"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Table " + table.tableNumber)
                .setItems(options, (d, w) -> {
                    if (w == 0) openChat(table.currentBookingId, table.userName);
                    else if (w == 1) endSession(table);
                    else showGuestInfo(table);
                }).show();
    }

    private void showPendingTableActions(TableVisual table) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Pending Request: Table " + table.tableNumber)
                .setMessage("Guest: " + table.userName)
                .setPositiveButton("Accept", (d, w) -> updateBookingStatus(table.currentBookingId, "Accepted", table.type, true))
                .setNegativeButton("Reject", (d, w) -> updateBookingStatus(table.currentBookingId, "Rejected", table.type, false))
                .setNeutralButton("Chat", (d, w) -> openChat(table.currentBookingId, table.userName))
                .show();
    }

    private void showFreeTableActions(TableVisual table) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Table " + table.tableNumber + " is Free")
                .setMessage("Assign a manual walk-in guest?")
                .setPositiveButton("Assign Walk-in", (d, w) -> assignWalkIn(table))
                .setNegativeButton("Cancel", null).show();
    }

    private void showGuestInfo(TableVisual table) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Guest Details")
                .setMessage("Name: " + table.userName + "\nPhone: " + table.userPhone)
                .setPositiveButton("OK", null).show();
    }

    private void openChat(String id, String name) {
        Intent intent = new Intent(this, UserChatActivity.class);
        intent.putExtra("bookingId", id);
        intent.putExtra("chatTitle", name);
        startActivity(intent);
    }

    private void assignWalkIn(TableVisual table) {
        showLoading();
        String walkInId = "walkin_" + System.currentTimeMillis();
        Map<String, Object> booking = new HashMap<>();
        booking.put("userId", "admin_manual");
        booking.put("userName", "Walk-in Guest");
        booking.put("restaurantId", restaurantId);
        booking.put("tableType", table.type);
        booking.put("status", "Accepted");
        booking.put("dateTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        db.collection("bookings").document(walkInId).set(booking).addOnSuccessListener(v -> {
            adjustTableCount(table.type, -1);
            hideLoading();
            Toast.makeText(this, "Table assigned", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> hideLoading());
    }

    private void updateBookingStatus(String id, String status, String type, boolean accept) {
        showLoading();
        db.collection("bookings").document(id).update("status", status).addOnSuccessListener(v -> {
            if (accept) adjustTableCount(type, -1);
            hideLoading();
        }).addOnFailureListener(e -> hideLoading());
    }

    private void endSession(TableVisual table) {
        showLoading();
        db.collection("bookings").document(table.currentBookingId).update("status", "Completed").addOnSuccessListener(v -> {
            adjustTableCount(table.type, 1);
            hideLoading();
        }).addOnFailureListener(e -> hideLoading());
    }

    private void adjustTableCount(String type, int delta) {
        DocumentReference resRef = db.collection("restaurants").document(restaurantId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(resRef);
            if (snap != null && snap.exists()) {
                transaction.update(resRef, "tableTypes." + type, FieldValue.increment(delta));
                transaction.update(resRef, "noOfTables", FieldValue.increment(delta));
            }
            return null;
        });
    }

    @Override protected void onDestroy() { 
        super.onDestroy(); 
        if (bookingListener != null) bookingListener.remove(); 
        if (restaurantListener != null) restaurantListener.remove(); 
    }
}
