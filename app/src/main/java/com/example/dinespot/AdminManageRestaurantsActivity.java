package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminManageRestaurantsActivity extends CommonBaseActivity {

    private RecyclerView recyclerViewRestaurants;
    private AdminRestaurantAdapter adapter;
    private List<CommonRestaurant> restaurantList = new ArrayList<>();
    private List<CommonRestaurant> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private FloatingActionButton fabAddRestaurant;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View shimmerView, noResultsText;
    private EditText etSearch;
    private ListenerRegistration restaurantListener;
    
    private String selectedStatus = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_manage_restaurants);

        setupToolbar("My Restaurants", true);

        db = FirebaseFirestore.getInstance();

        recyclerViewRestaurants = findViewById(R.id.recyclerViewRestaurants);
        fabAddRestaurant = findViewById(R.id.fabAddRestaurant);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        shimmerView = findViewById(R.id.shimmer_view_container);
        noResultsText = findViewById(R.id.noResultsText);
        etSearch = findViewById(R.id.etSearchRestaurant);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        CommonNavigationHelper.setupAdminNavigation(this, bottomNavigationView, R.id.admin_manage);

        recyclerViewRestaurants.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminRestaurantAdapter(this, filteredList);
        recyclerViewRestaurants.setAdapter(adapter);

        setupSearch();
        setupFilters();

        fabAddRestaurant.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminAddEditRestaurantActivity.class));
        });

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchRestaurants);
        }

        fetchRestaurants();
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().toLowerCase().trim();
                    applyFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupFilters() {
        MaterialCardView fAll = findViewById(R.id.filter_all);
        MaterialCardView fOpen = findViewById(R.id.filter_open);
        MaterialCardView fFull = findViewById(R.id.filter_full);
        MaterialCardView fClosed = findViewById(R.id.filter_closed);

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
            if (id == R.id.filter_all) selectedStatus = "All";
            else if (id == R.id.filter_open) selectedStatus = "Open";
            else if (id == R.id.filter_full) selectedStatus = "Full";
            else if (id == R.id.filter_closed) selectedStatus = "Closed";

            applyFilters();
        };

        fAll.setOnClickListener(clickListener);
        fOpen.setOnClickListener(clickListener);
        fFull.setOnClickListener(clickListener);
        fClosed.setOnClickListener(clickListener);
    }

    private void resetFilterUI() {
        int[] ids = {R.id.filter_all, R.id.filter_open, R.id.filter_full, R.id.filter_closed};
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

    private void applyFilters() {
        filteredList.clear();
        for (CommonRestaurant r : restaurantList) {
            String status = r.getStatus();
            if (status == null) status = "Open";

            // If time-based auto-close applied locally to match what user sees
            if (!CommonUtils.isWithinOperatingHours(r) && !"Closed".equalsIgnoreCase(status)) {
                status = "Closed";
            }

            boolean matchesStatus = "All".equalsIgnoreCase(selectedStatus) || selectedStatus.equalsIgnoreCase(status);
            boolean matchesSearch = r.getName().toLowerCase().contains(currentSearchQuery) || 
                                   (r.getAddress() != null && r.getAddress().toLowerCase().contains(currentSearchQuery));
            
            if (matchesStatus && matchesSearch) {
                filteredList.add(r);
            }
        }
        
        if (adapter != null) adapter.notifyDataSetChanged();
        
        if (noResultsText != null) {
            noResultsText.setVisibility(filteredList.isEmpty() && !restaurantList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void fetchRestaurants() {
        if (restaurantListener != null) restaurantListener.remove();
        
        restaurantListener = db.collection("restaurants").addSnapshotListener((value, error) -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (shimmerView != null) shimmerView.setVisibility(View.GONE);
            if (error != null) return;
            if (value != null) {
                restaurantList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    try {
                        CommonRestaurant restaurant = doc.toObject(CommonRestaurant.class);
                        if (restaurant != null) {
                            restaurant.setRestaurantId(doc.getId());
                            restaurantList.add(restaurant);
                            
                            // AUTO-SYNC LOGIC: If time suggests it should be closed, but status is "Open"
                            // or vice-versa, perform a silent update to keep DB accurate.
                            syncRestaurantStatusIfNeeded(restaurant);
                        }
                    } catch (Exception e) {
                        // Skip invalid data
                    }
                }
                applyFilters();
            }
        });
    }

    private void syncRestaurantStatusIfNeeded(CommonRestaurant r) {
        boolean shouldBeOpen = CommonUtils.isWithinOperatingHours(r);
        String currentStatus = r.getStatus();
        
        if (currentStatus == null) return;

        // If it's Open but outside hours -> Auto Close
        if ("Open".equalsIgnoreCase(currentStatus) && !shouldBeOpen) {
            db.collection("restaurants").document(r.getRestaurantId()).update("status", "Closed");
        } 
        // We only auto-close. We DO NOT auto-open.
        // This prevents the system from fighting an admin who manually sets the status to "Closed"
    }

    @Override
    protected void onDestroy() {
        if (restaurantListener != null) restaurantListener.remove();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
