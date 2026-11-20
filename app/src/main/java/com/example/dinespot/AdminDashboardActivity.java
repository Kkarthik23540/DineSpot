package com.example.dinespot;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends CommonBaseActivity {

    private TextView tvRestaurantCount, tvNoBookingsMessage;
    private TextView tvOpenCount, tvFullCount, tvClosedCount;
    private RecyclerView recyclerViewBookings;
    private AdminBookingAdapter bookingAdapter;
    private final List<CommonBooking> bookingList = new ArrayList<>();
    private final List<CommonBooking> fullBookingList = new ArrayList<>();
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private View btnAll, btnPending, btnLive, btnDone, btnDenied;
    private TextView tvAll, tvPending, tvLive, tvDone, tvDenied;
    private String currentStatusFilter = "";
    
    private android.widget.EditText etAdminSearch;
    private String currentSearchQuery = "";
    private ListenerRegistration bookingListener, restaurantListener, supportListener, adminProfileListener;
    private View shimmerView;
    private TextView userInitialText;

    private MaterialCardView cardManageRestaurants, cardAddRestaurantNew, cardFloorPlan, cardSupportInbox;

    private void setUserInitialInToolbar() {
        userInitialText = findViewById(R.id.userInitial);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        if (adminProfileListener != null) adminProfileListener.remove();

        adminProfileListener = db.collection("admins").whereEqualTo("email", user.getEmail().toLowerCase())
                .addSnapshotListener((snaps, e) -> {
                    if (e != null || snaps == null) return;
                    
                    String initial = "A";
                    if (!snaps.isEmpty()) {
                        DocumentSnapshot doc = snaps.getDocuments().get(0);
                        String name = doc.getString("name");
                        if (name == null || name.isEmpty()) name = doc.getString("username");
                        if (name == null || name.isEmpty()) name = doc.getString("userName");
                        
                        if (name != null && !name.isEmpty()) {
                            initial = name.substring(0, 1).toUpperCase();
                        }
                    }
                    
                    if (userInitialText != null) {
                        userInitialText.setText(initial);
                        userInitialText.setOnClickListener(v -> {
                            Intent intent = new Intent(this, AdminProfileActivity.class);
                            startActivity(intent);
                        });
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_dashboard);

        setupToolbar("Admin Dashboard", false);
        initViews();
        setupNavigation();
        setupRecyclerView();
        setupFilterLogic();
        updateFilterUI(R.id.btnFilterAll); // Initial state
        
        db = FirebaseFirestore.getInstance();
        setUserInitialInToolbar();
        startRealtimeListeners();
        listenForSupportNotifications();
    }

    private void listenForSupportNotifications() {
        supportListener = db.collection("support_chats").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            for (DocumentChange dc : value.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.MODIFIED || dc.getType() == DocumentChange.Type.ADDED) {
                    String lastMsg = dc.getDocument().getString("lastMessage");
                    String userEmail = dc.getDocument().getString("userEmail");
                    String chatId = dc.getDocument().getId();
                    String senderId = dc.getDocument().getString("userId");
                    Long lastTs = dc.getDocument().getLong("lastTimestamp");
                    
                    if (lastMsg != null && senderId != null && !senderId.equals(FirebaseAuth.getInstance().getUid())) {
                        if (lastTs != null && System.currentTimeMillis() - lastTs < 30000) {
                            CommonNotificationHelper.showChatNotification(this, 
                                    userEmail != null ? userEmail : "New User Query", 
                                    lastMsg, chatId, senderId);
                            db.collection("support_chats").document(chatId).update("status", "delivered");
                        }
                    }
                }
            }
        });
    }

    private void initViews() {
        tvRestaurantCount = findViewById(R.id.tvRestaurantCount);
        tvOpenCount = findViewById(R.id.tvOpenCount);
        tvFullCount = findViewById(R.id.tvFullCount);
        tvClosedCount = findViewById(R.id.tvClosedCount);
        
        tvNoBookingsMessage = findViewById(R.id.tvNoBookingsMessage);
        recyclerViewBookings = findViewById(R.id.recyclerViewBookings);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        btnAll = findViewById(R.id.btnFilterAll);
        btnPending = findViewById(R.id.btnFilterPending);
        btnLive = findViewById(R.id.btnFilterLive);
        btnDone = findViewById(R.id.btnFilterDone);
        btnDenied = findViewById(R.id.btnFilterDenied);

        tvAll = findViewById(R.id.tvFilterAll);
        tvPending = findViewById(R.id.tvFilterPending);
        tvLive = findViewById(R.id.tvFilterLive);
        tvDone = findViewById(R.id.tvFilterDone);
        tvDenied = findViewById(R.id.tvFilterDenied);

        cardManageRestaurants = findViewById(R.id.cardManageRestaurants);
        cardAddRestaurantNew = findViewById(R.id.cardAddRestaurantNew);
        cardSupportInbox = findViewById(R.id.cardSupportInbox);
        cardFloorPlan = findViewById(R.id.cardFloorPlan);
        etAdminSearch = findViewById(R.id.etAdminSearch);
        
        ProgressBar pb = findViewById(R.id.progressBar);
        if (pb != null) pb.setVisibility(View.GONE);
        shimmerView = findViewById(R.id.shimmer_view_container);

        if (cardManageRestaurants != null) cardManageRestaurants.setOnClickListener(v -> startActivity(new Intent(this, AdminManageRestaurantsActivity.class)));
        if (cardAddRestaurantNew != null) cardAddRestaurantNew.setOnClickListener(v -> startActivity(new Intent(this, AdminAddEditRestaurantActivity.class)));
        if (cardSupportInbox != null) cardSupportInbox.setOnClickListener(v -> startActivity(new Intent(this, AdminSupportInboxActivity.class)));
        if (cardFloorPlan != null) cardFloorPlan.setOnClickListener(v -> {
            Toast.makeText(this, "Select a restaurant first from 'Manage' to view its Floor Plan.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, AdminManageRestaurantsActivity.class));
        });

        etAdminSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilter();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::startRealtimeListeners);
        }
    }

    private void setupNavigation() {
        BottomNavigationView bnv = findViewById(R.id.bottom_navigation);
        if (bnv != null) {
            CommonNavigationHelper.setupAdminNavigation(this, bnv, R.id.admin_home);
        }
    }

    private void setupRecyclerView() {
        recyclerViewBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new AdminBookingAdapter(bookingList, new AdminBookingAdapter.OnBookingActionListener() {
            @Override
            public void onStatusChanged(CommonBooking booking, String newStatus) { updateBookingStatus(booking, newStatus); }
            @Override
            public void onOrderUpdate(CommonBooking booking, String orderStatus) { updateOrderStatus(booking, orderStatus); }
            @Override
            public void onDelete(CommonBooking booking) { showDeleteConfirmation(booking); }
        });
        recyclerViewBookings.setAdapter(bookingAdapter);
    }

    private void setupFilterLogic() {
        View.OnClickListener clickListener = v -> {
            vibrateAction();
            int id = v.getId();
            if (id == R.id.btnFilterAll) currentStatusFilter = "";
            else if (id == R.id.btnFilterPending) currentStatusFilter = "Pending";
            else if (id == R.id.btnFilterLive) currentStatusFilter = "Accepted";
            else if (id == R.id.btnFilterDone) currentStatusFilter = "Completed";
            else if (id == R.id.btnFilterDenied) currentStatusFilter = "Rejected";

            updateFilterUI(id);
            applyFilter();
        };

        btnAll.setOnClickListener(clickListener);
        btnPending.setOnClickListener(clickListener);
        btnLive.setOnClickListener(clickListener);
        btnDone.setOnClickListener(clickListener);
        btnDenied.setOnClickListener(clickListener);
    }

    private void updateFilterUI(int activeId) {
        resetChip(btnAll, R.id.btnFilterAll == activeId, ContextCompat.getColor(this, R.color.colorPrimary));
        resetChip(btnPending, R.id.btnFilterPending == activeId, Color.parseColor("#FF9800"));
        resetChip(btnLive, R.id.btnFilterLive == activeId, ContextCompat.getColor(this, R.color.colorSuccess));
        resetChip(btnDenied, R.id.btnFilterDenied == activeId, ContextCompat.getColor(this, R.color.colorError));
        resetChip(btnDone, R.id.btnFilterDone == activeId, ContextCompat.getColor(this, R.color.textSecondary));
    }

    private void resetChip(View cardView, boolean isActive, int statusColor) {
        if (cardView == null) return;
        MaterialCardView card = (MaterialCardView) cardView;
        LinearLayout layout = (LinearLayout) card.getChildAt(0);
        TextView tv = (TextView) layout.getChildAt(1);
        
        if (isActive) {
            card.setStrokeWidth(dpToPx(1.5f));
            card.setStrokeColor(statusColor);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryLight));
            tv.setTextColor(statusColor);
        } else {
            card.setStrokeWidth(dpToPx(1));
            card.setStrokeColor(ContextCompat.getColor(this, R.color.divider));
            card.setCardBackgroundColor(Color.WHITE);
            tv.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        }
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void startRealtimeListeners() {
        if (bookingListener != null) bookingListener.remove();
        if (restaurantListener != null) restaurantListener.remove();

        // Initial Attempt: Server-side sort (requires single property index, which is usually automatic)
        com.google.firebase.firestore.Query bookingQuery = db.collection("bookings")
                .orderBy("dateTime", Query.Direction.DESCENDING);

        bookingListener = bookingQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                // If query fails (e.g. missing index or rule), fallback to client-side sort
                if (e.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    loadBookingsFallback();
                } else {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    tvNoBookingsMessage.setVisibility(View.VISIBLE);
                }
                return;
            }
            handleBookingSnapshots(snapshots);
        });

        restaurantListener = db.collection("restaurants")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        int total = snapshots.size();
                        int open = 0, full = 0, closed = 0;
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String status = doc.getString("status");
                            if ("Full".equalsIgnoreCase(status)) full++;
                            else if ("Closed".equalsIgnoreCase(status)) closed++;
                            else open++;
                        }
                        
                        if (tvRestaurantCount != null) CommonAnimationHelper.animateStatChange(tvRestaurantCount, total);
                        if (tvOpenCount != null) CommonAnimationHelper.animateStatChange(tvOpenCount, open);
                        if (tvFullCount != null) CommonAnimationHelper.animateStatChange(tvFullCount, full);
                        if (tvClosedCount != null) CommonAnimationHelper.animateStatChange(tvClosedCount, closed);
                    }
                });
    }

    private void loadBookingsFallback() {
        if (bookingListener != null) bookingListener.remove();
        
        // Fallback: Query without orderBy, sort locally
        bookingListener = db.collection("bookings")
                .addSnapshotListener((snapshots, e) -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (e != null || snapshots == null) {
                        tvNoBookingsMessage.setVisibility(View.VISIBLE);
                        return;
                    }
                    
                    List<CommonBooking> newList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        CommonBooking b = doc.toObject(CommonBooking.class);
                        if (b != null) {
                            b.setId(doc.getId());
                            newList.add(b);
                        }
                    }
                    
                    // Client-side sort
                    newList.sort((b1, b2) -> {
                        String t1 = b1.getDateTime() != null ? b1.getDateTime() : "";
                        String t2 = b2.getDateTime() != null ? b2.getDateTime() : "";
                        return t2.compareTo(t1);
                    });

                    updateUIWithBookings(newList);
                });
    }

    private void handleBookingSnapshots(com.google.firebase.firestore.QuerySnapshot snapshots) {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        if (snapshots == null) return;

        List<CommonBooking> newList = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshots) {
            CommonBooking b = doc.toObject(CommonBooking.class);
            if (b != null) {
                b.setId(doc.getId());
                newList.add(b);
            }
        }
        updateUIWithBookings(newList);
    }

    private void updateUIWithBookings(List<CommonBooking> newList) {
        if (shimmerView != null) shimmerView.setVisibility(View.GONE);
        fullBookingList.clear();
        fullBookingList.addAll(newList);
        applyFilter();
    }

    private void applyFilter() {
        bookingList.clear();
        for (CommonBooking b : fullBookingList) {
            if (b == null) continue;
            String uName = b.getUserName() != null ? b.getUserName().toLowerCase() : "";
            String rName = b.getRestaurantName() != null ? b.getRestaurantName().toLowerCase() : "";
            boolean matchesStatus = currentStatusFilter.isEmpty() || currentStatusFilter.equalsIgnoreCase(b.getStatus());
            boolean matchesSearch = uName.contains(currentSearchQuery) || rName.contains(currentSearchQuery);
            if (matchesStatus && matchesSearch) bookingList.add(b);
        }
        bookingAdapter.notifyDataSetChanged();
        tvNoBookingsMessage.setVisibility(bookingList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateBookingStatus(CommonBooking booking, String newStatus) {
        showLoading();
        db.collection("bookings").document(booking.getId()).get().addOnSuccessListener(snapshot -> {
            String oldStatus = snapshot.getString("status");
            WriteBatch batch = db.batch();
            DocumentReference bookingRef = db.collection("bookings").document(booking.getId());
            batch.update(bookingRef, "status", newStatus);

            batch.commit().addOnCompleteListener(task -> {
                hideLoading();
                if (task.isSuccessful()) {
                    Snackbar.make(recyclerViewBookings, "Booking " + newStatus, Snackbar.LENGTH_SHORT).show();
                    
                    boolean wasSeated = "Pending".equalsIgnoreCase(oldStatus) || "Accepted".equalsIgnoreCase(oldStatus);
                    boolean isStillSeated = "Accepted".equalsIgnoreCase(newStatus) || "Pending".equalsIgnoreCase(newStatus);

                    if (wasSeated && !isStillSeated) {
                        // User was taking a seat, but now Rejected/Cancelled/Completed -> RETURN TABLE
                        adjustTableCount(booking.getRestaurantId(), booking.getTableType(), 1);
                        
                        // PROMOTING WAITLISTED USER AUTOMATICALLY
                        promoteWaitlist(booking.getRestaurantId(), booking.getTableType());
                    } else if (!wasSeated && isStillSeated) {
                        // Moving from Waitlist to Pending/Accepted (Rare for admin to do manually, but handled) -> DEDUCT TABLE
                        adjustTableCount(booking.getRestaurantId(), booking.getTableType(), -1);
                    }
                } else {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            });
        }).addOnFailureListener(e -> hideLoading());
    }

    private void promoteWaitlist(String resId, String tableType) {
        if (resId == null || tableType == null) return;
        db.collection("bookings")
                .whereEqualTo("restaurantId", resId)
                .whereEqualTo("tableType", tableType)
                .whereEqualTo("status", "Waitlisted")
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        DocumentSnapshot oldestWaitlist = snaps.getDocuments().get(0);
                        db.runTransaction(transaction -> {
                            DocumentReference resRef = db.collection("restaurants").document(resId);
                            DocumentSnapshot resSnap = transaction.get(resRef);
                            long available = 0;
                            Map<String, Object> types = (Map<String, Object>) resSnap.get("tableTypes");
                            if (types != null && types.containsKey(tableType)) {
                                available = ((Number) types.get(tableType)).longValue();
                            }
                            if (available > 0) {
                                available--;
                                types.put(tableType, available);
                                transaction.update(resRef, "tableTypes", types);
                                long total = resSnap.getLong("noOfTables") != null ? resSnap.getLong("noOfTables") : 0;
                                transaction.update(resRef, "noOfTables", Math.max(0, total - 1));
                                transaction.update(oldestWaitlist.getReference(), "status", "Pending");
                                return true;
                            }
                            return false;
                        });
                    }
                });
    }

    private void updateOrderStatus(CommonBooking booking, String orderStatus) {
        db.collection("bookings").document(booking.getId())
                .update("orderStatus", orderStatus)
                .addOnSuccessListener(aVoid -> {
                    Snackbar.make(recyclerViewBookings, "Order updated: " + orderStatus, Snackbar.LENGTH_SHORT).show();
                });
    }

    private void adjustTableCount(String restaurantId, String type, int delta) {
        if (restaurantId == null || type == null) return;
        DocumentReference resRef = db.collection("restaurants").document(restaurantId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(resRef);
            if (snap != null && snap.exists()) {
                Object rawTypesObj = snap.get("tableTypes");
                Map<String, Long> types = new HashMap<>();
                if (rawTypesObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawTypes = (Map<String, Object>) rawTypesObj;
                    for (Map.Entry<String, Object> entry : rawTypes.entrySet()) {
                        if (entry.getValue() instanceof Number) {
                            types.put(entry.getKey(), ((Number) entry.getValue()).longValue());
                        }
                    }
                }
                long current = types.getOrDefault(type, 0L);
                long newTypeCount = current + delta;
                types.put(type, Math.max(0, newTypeCount)); // Prevent negative table counts
                transaction.update(resRef, "tableTypes", types);
                
                Long total = snap.getLong("noOfTables");
                long newTotal = (total != null ? total : 0) + delta;
                long finalTotal = Math.max(0, newTotal);
                transaction.update(resRef, "noOfTables", finalTotal);

                // AUTOMATIC "FULL" STATUS LOGIC
                String currentStatus = snap.getString("status");
                if (finalTotal <= 0) {
                    // Only auto-set to Full if it's currently Open. (Don't override "Closed")
                    if ("Open".equalsIgnoreCase(currentStatus)) {
                        transaction.update(resRef, "status", "Full");
                    }
                } else {
                    // Auto-reopen if it was marked as Full previously
                    if ("Full".equalsIgnoreCase(currentStatus)) {
                        transaction.update(resRef, "status", "Open");
                    }
                }
            }
            return null;
        });
    }

    private void showDeleteConfirmation(CommonBooking booking) {
        showCustomDialog(R.drawable.baseline_delete_outline_24,
                "Delete Booking",
                "Are you sure you want to delete this record? This cannot be undone.",
                "Delete", () -> {
                    db.collection("bookings").document(booking.getId()).delete()
                            .addOnSuccessListener(aVoid -> Snackbar.make(recyclerViewBookings, "Booking Record Removed Successfully", Snackbar.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show());
                },
                "Cancel", null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingListener != null) bookingListener.remove();
        if (restaurantListener != null) restaurantListener.remove();
        if (supportListener != null) supportListener.remove();
        if (adminProfileListener != null) adminProfileListener.remove();
    }
}
