package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserBookingHistoryActivity extends CommonBaseActivity {

    private RecyclerView rvBookingHistory;
    private UserBookingHistoryAdapter adapter;
    private List<CommonBooking> bookingList;
    private List<CommonBooking> fullList = new ArrayList<>();
    private ProgressBar progressBar;
    private View emptyBookingMessage, shimmerView;
    private FirebaseFirestore db;
    private ListenerRegistration bookingListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText searchBar;
    private View searchCard;
    private View filterAll, filterActive, filterPending, filterHistory;
    
    private String currentStatusFilter = "All";
    private String currentSearchQuery = "";
    
    private static final int PAGE_SIZE = 10;
    private DocumentSnapshot lastVisibleBooking;
    private boolean isBookingsLoading = false;
    private boolean isLastBookingsPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_booking_history);

        setupToolbar("Booking History", true);

        rvBookingHistory = findViewById(R.id.rv_booking_history);
        progressBar = findViewById(R.id.progressBar);
        emptyBookingMessage = findViewById(R.id.emptyBookingMessage);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        searchBar = findViewById(R.id.searchBar);
        searchCard = findViewById(R.id.searchCard);
        
        filterAll = findViewById(R.id.filter_all);
        filterActive = findViewById(R.id.filter_active);
        filterPending = findViewById(R.id.filter_pending);
        filterHistory = findViewById(R.id.filter_history);

        if (searchCard != null) searchCard.setVisibility(View.GONE); // Default hide until data loads
        shimmerView = findViewById(R.id.shimmer_view_container);
        
        db = FirebaseFirestore.getInstance();
        bookingList = new ArrayList<>();
        adapter = new UserBookingHistoryAdapter(bookingList, booking -> {
            // Cancel booking logic only for Pending/Waitlisted
            cancelBooking(booking);
        });
        
        rvBookingHistory.setLayoutManager(new LinearLayoutManager(this));
        rvBookingHistory.setAdapter(adapter);

        setupFilters();

        rvBookingHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == bookingList.size() - 1) {
            if (!isBookingsLoading && !isLastBookingsPage && searchBar.getText().toString().isEmpty()) {
                loadMoreBookings();
            }
        }
    }
        });

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                isLastBookingsPage = false;
                lastVisibleBooking = null;
                fetchBookingHistory();
            });
        }

        setupSearch();
        fetchBookingHistory();
        setupBottomNavigation();
    }

    private void setupFilters() {
        View.OnClickListener clickListener = v -> {
            resetFilterUI();
            MaterialCardView card = (MaterialCardView) v;
            LinearLayout layout = (LinearLayout) card.getChildAt(0);
            TextView tv = (TextView) layout.getChildAt(1);

            card.setStrokeWidth(dpToPx(1.5f));
            card.setStrokeColor(ContextCompat.getColor(this, R.color.colorPrimary));
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryLight));
            tv.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

            int id = v.getId();
            if (id == R.id.filter_all) currentStatusFilter = "All";
            else if (id == R.id.filter_active) currentStatusFilter = "Accepted";
            else if (id == R.id.filter_pending) currentStatusFilter = "Pending";
            else if (id == R.id.filter_history) currentStatusFilter = "History";

            filter(searchBar.getText().toString());
        };

        filterAll.setOnClickListener(clickListener);
        filterActive.setOnClickListener(clickListener);
        filterPending.setOnClickListener(clickListener);
        filterHistory.setOnClickListener(clickListener);
    }

    private void resetFilterUI() {
        int[] ids = {R.id.filter_all, R.id.filter_active, R.id.filter_pending, R.id.filter_history};
        for (int id : ids) {
            MaterialCardView card = findViewById(id);
            if (card != null) {
                LinearLayout layout = (LinearLayout) card.getChildAt(0);
                TextView tv = (TextView) layout.getChildAt(1);
                card.setStrokeWidth(dpToPx(1));
                card.setStrokeColor(ContextCompat.getColor(this, R.color.divider));
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
                tv.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
            }
        }
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                filter(currentSearchQuery);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String query) {
        String lowerQuery = query != null ? query.toLowerCase(Locale.getDefault()) : "";
        bookingList.clear();
        for (CommonBooking b : fullList) {
            if (b == null) continue;
            
            // Apply Status Filter
            boolean matchesStatus = true;
            String status = b.getStatus();
            if ("Accepted".equalsIgnoreCase(currentStatusFilter)) {
                matchesStatus = "Accepted".equalsIgnoreCase(status);
            } else if ("Pending".equalsIgnoreCase(currentStatusFilter)) {
                matchesStatus = "Pending".equalsIgnoreCase(status) || "Waitlisted".equalsIgnoreCase(status);
            } else if ("History".equalsIgnoreCase(currentStatusFilter)) {
                matchesStatus = "Completed".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status);
            }

            // Apply Search Query
            String name = b.getRestaurantName();
            boolean matchesSearch = name != null && name.toLowerCase(Locale.getDefault()).contains(lowerQuery);
            
            if (matchesStatus && matchesSearch) {
                bookingList.add(b);
            }
        }
        adapter.notifyDataSetChanged();
        
        boolean hasBookings = !fullList.isEmpty();
        boolean listIsEmpty = bookingList.isEmpty();
        
        if (searchCard != null) searchCard.setVisibility(hasBookings ? View.VISIBLE : View.GONE);
        
        if (emptyBookingMessage != null) emptyBookingMessage.setVisibility(listIsEmpty ? View.VISIBLE : View.GONE);
        if (rvBookingHistory != null) rvBookingHistory.setVisibility(listIsEmpty ? View.GONE : View.VISIBLE);
        invalidateOptionsMenu();
    }

    private void cancelBooking(CommonBooking booking) {
        String status = booking.getStatus();
        if ("Completed".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
            Toast.makeText(this, "Cannot modify history records.", Toast.LENGTH_SHORT).show();
            return;
        }

        showCustomDialog(R.drawable.baseline_privacy_tip_24,
                "Cancel Request",
                "Do you want to cancel this " + status + " booking?",
                "Cancel Now", () -> performCancellation(booking),
                "Keep It", null);
    }

    private void performCancellation(CommonBooking booking) {
        showLoading();
        // Transactional delete/update
        db.runTransaction(transaction -> {
            DocumentReference bookingRef = db.collection("bookings").document(booking.getId());
            DocumentSnapshot snap = transaction.get(bookingRef);
            String currentStatus = snap.getString("status");
            
            if ("Pending".equalsIgnoreCase(currentStatus) || "Accepted".equalsIgnoreCase(currentStatus)) {
                // Return table if it was locked
                String rId = booking.getRestaurantId();
                String tType = booking.getTableType();
                if (rId != null && tType != null) {
                    DocumentReference resRef = db.collection("restaurants").document(rId);
                    transaction.update(resRef, "noOfTables", com.google.firebase.firestore.FieldValue.increment(1));
                    transaction.update(resRef, "tableTypes." + tType, com.google.firebase.firestore.FieldValue.increment(1));
                }
            }
            
            // Mark as Cancelled instead of deleting to preserve history
            transaction.update(bookingRef, "status", "Cancelled");
            return null;
        }).addOnSuccessListener(v -> {
            hideLoading();
            Toast.makeText(this, "Booking Cancelled", Toast.LENGTH_SHORT).show();
            fetchBookingHistory();
            
            // AUTO-FILL WAITLIST PROMOTION
            promoteWaitlist(booking.getRestaurantId(), booking.getTableType());
        }).addOnFailureListener(e -> {
            hideLoading();
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void promoteWaitlist(String resId, String tableType) {
        if (resId == null || tableType == null) return;
        
        // Find the OLDEST waitlisted user for this specific table type
        db.collection("bookings")
                .whereEqualTo("restaurantId", resId)
                .whereEqualTo("tableType", tableType)
                .whereEqualTo("status", "Waitlisted")
                .orderBy("dateTime", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        DocumentSnapshot oldestWaitlist = snaps.getDocuments().get(0);
                        
                        // Transaction to accurately move from Waitlist to Pending and LOCK the seat
                        db.runTransaction(transaction -> {
                            DocumentReference resRef = db.collection("restaurants").document(resId);
                            DocumentSnapshot resSnap = transaction.get(resRef);
                            
                            // Re-verify table is still available (Transaction safety)
                            long available = 0;
                            Object rawTypesObj = resSnap.get("tableTypes");
                            if (rawTypesObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> types = (Map<String, Object>) rawTypesObj;
                                if (types.containsKey(tableType) && types.get(tableType) instanceof Number) {
                                    available = ((Number) types.get(tableType)).longValue();
                                }
                                
                                if (available > 0) {
                                    // 1. Deduct table
                                    available--;
                                    types.put(tableType, available);
                                    transaction.update(resRef, "tableTypes", types);
                                    
                                    Long rawTotal = resSnap.getLong("noOfTables");
                                    long total = rawTotal != null ? rawTotal : 0;
                                    transaction.update(resRef, "noOfTables", Math.max(0, total - 1));
                                    
                                    // 2. Promote user to Pending
                                    transaction.update(oldestWaitlist.getReference(), "status", "Pending");
                                    
                                    return true;
                                }
                            }
                            return false;
                        }).addOnSuccessListener(promoted -> {
                            if (promoted != null && promoted) {
                                // In a real app, this is where you'd send an FCM Push Notification
                                Toast.makeText(this, "A waitlisted user was automatically promoted!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
    }

    private void deleteBooking(CommonBooking booking) {
        // Redundant now, using performCancellation
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu);
        CommonNavigationHelper.setupUserNavigation(this, bottomNavigationView, R.id.menu_bookings);
    }

    private void fetchBookingHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        }

        if (bookingListener != null) bookingListener.remove();

        isBookingsLoading = true;
        
        // Initial Attempt: Server-side sort (requires composite index)
        com.google.firebase.firestore.Query baseQuery = db.collection("bookings")
                .whereEqualTo("userId", userId)
                .orderBy("dateTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        bookingListener = baseQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                // If index is missing (FAILED_PRECONDITION), fallback to client-side sort
                if (e.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    loadBookingsFallback(userId);
                } else {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (shimmerView != null) shimmerView.setVisibility(View.GONE);
                    if (emptyBookingMessage != null) emptyBookingMessage.setVisibility(View.VISIBLE);
                }
                return;
            }
            handleBookingSnapshots(snapshots);
        });
    }

    private void loadBookingsFallback(String userId) {
        if (bookingListener != null) bookingListener.remove();
        
        // Fallback: Query without orderBy, sort locally
        bookingListener = db.collection("bookings")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (shimmerView != null) shimmerView.setVisibility(View.GONE);
                    
                    if (e != null || snapshots == null) {
                        if (emptyBookingMessage != null) emptyBookingMessage.setVisibility(View.VISIBLE);
                        return;
                    }
                    
                    fullList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        CommonBooking b = doc.toObject(CommonBooking.class);
                        if (b != null) {
                            b.setId(doc.getId());
                            fullList.add(b);
                        }
                    }
                    
                    // Client-side sort
                    fullList.sort((b1, b2) -> {
                        String t1 = b1.getDateTime() != null ? b1.getDateTime() : "";
                        String t2 = b2.getDateTime() != null ? b2.getDateTime() : "";
                        return t2.compareTo(t1);
                    });
                    
                    filter(searchBar.getText().toString());
                });
    }

    private void handleBookingSnapshots(com.google.firebase.firestore.QuerySnapshot snapshots) {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (shimmerView != null) shimmerView.setVisibility(View.GONE);
        isBookingsLoading = false;

        if (snapshots != null) {
            fullList.clear();
            if (!snapshots.isEmpty()) {
                lastVisibleBooking = snapshots.getDocuments().get(snapshots.size() - 1);
                isLastBookingsPage = snapshots.size() < PAGE_SIZE;

                for (QueryDocumentSnapshot doc : snapshots) {
                    CommonBooking b = doc.toObject(CommonBooking.class);
                    if (b != null) {
                        b.setId(doc.getId());
                        fullList.add(b);
                    }
                }
            } else {
                isLastBookingsPage = true;
            }
            
            // Explicitly check for empty list to hide search bar immediately
            if (fullList.isEmpty()) {
                if (searchCard != null) searchCard.setVisibility(View.GONE);
                if (emptyBookingMessage != null) emptyBookingMessage.setVisibility(View.VISIBLE);
                if (rvBookingHistory != null) rvBookingHistory.setVisibility(View.GONE);
            } else {
                if (searchCard != null) searchCard.setVisibility(View.VISIBLE);
                filter(searchBar.getText().toString());
            }
        }
    }

    private void loadMoreBookings() {
        if (lastVisibleBooking == null || isLastBookingsPage) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        isBookingsLoading = true;
        
        db.collection("bookings")
                .whereEqualTo("userId", userId)
                .orderBy("dateTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .startAfter(lastVisibleBooking)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(snapshots -> {
                    isBookingsLoading = false;
                    if (!snapshots.isEmpty()) {
                        lastVisibleBooking = snapshots.getDocuments().get(snapshots.size() - 1);
                        isLastBookingsPage = snapshots.size() < PAGE_SIZE;

                        for (QueryDocumentSnapshot doc : snapshots) {
                            CommonBooking b = doc.toObject(CommonBooking.class);
                            if (b != null) {
                                b.setId(doc.getId());
                                fullList.add(b);
                            }
                        }
                        filter(searchBar.getText().toString());
                    } else {
                        isLastBookingsPage = true;
                    }
                })
                .addOnFailureListener(e -> isBookingsLoading = false);
    }

    @Override
    protected void onDestroy() {
        if (bookingListener != null) bookingListener.remove();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        MenuItem clearItem = menu.findItem(R.id.clear_history);
        if (clearItem != null) {
            clearItem.setVisible(!fullList.isEmpty());
        }
        return true;
    }

    private void confirmClearHistory() {
        showCustomDialog(R.drawable.baseline_delete_outline_24, 
                "Clear History", 
                "Delete all past booking records permanently?", 
                "Clear All", () -> clearAllBookings(), 
                "Cancel", null);
    }

    private void clearAllBookings() {
        if (fullList.isEmpty()) return;
        showLoading();
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (CommonBooking b : fullList) {
            batch.delete(db.collection("bookings").document(b.getId()));
        }
        batch.commit().addOnCompleteListener(task -> {
            hideLoading();
            if (task.isSuccessful()) {
                Snackbar.make(findViewById(android.R.id.content), "History Cleared", Snackbar.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.clear_history) {
            confirmClearHistory();
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
