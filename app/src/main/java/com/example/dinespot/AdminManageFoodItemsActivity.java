package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminManageFoodItemsActivity extends CommonBaseActivity {

    private RecyclerView recyclerViewFoodItems;
    private AdminFoodItemAdapter adapter;
    private List<CommonFoodItem> foodItemList = new ArrayList<>();
    private List<CommonFoodItem> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private FloatingActionButton fabAddFoodItem;
    private String restaurantId, restaurantName;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View shimmerView, llNoFoodItems, noResultsText;
    private EditText etSearch;
    private ListenerRegistration menuListener;

    private String selectedFilter = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_manage_food_items);

        restaurantId = getIntent().getStringExtra("restaurant_id");
        restaurantName = getIntent().getStringExtra("restaurant_name");

        setupToolbar("Menu: " + restaurantName, true);

        db = FirebaseFirestore.getInstance();

        recyclerViewFoodItems = findViewById(R.id.recyclerViewFoodItems);
        fabAddFoodItem = findViewById(R.id.fabAddFoodItem);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        shimmerView = findViewById(R.id.shimmer_view_container);
        llNoFoodItems = findViewById(R.id.llNoFoodItems);
        noResultsText = findViewById(R.id.noResultsText);
        etSearch = findViewById(R.id.etSearchFood);

        recyclerViewFoodItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminFoodItemAdapter(this, filteredList, restaurantId);
        recyclerViewFoodItems.setAdapter(adapter);

        setupSearch();
        setupFilters();

        fabAddFoodItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminAddEditFoodItemActivity.class);
            intent.putExtra("restaurant_id", restaurantId);
            startActivity(intent);
        });

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchFoodItems);
        }

        fetchFoodItems();
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
        MaterialCardView fVeg = findViewById(R.id.filter_veg);
        MaterialCardView fNonVeg = findViewById(R.id.filter_non_veg);

        View.OnClickListener clickListener = v -> {
            resetFilterUI();
            MaterialCardView card = (MaterialCardView) v;
            card.setStrokeWidth(dpToPx(1.5f));
            card.setStrokeColor(ContextCompat.getColor(this, R.color.colorPrimary));
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryLight));
            
            TextView tv = (TextView) card.getChildAt(0);
            tv.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

            if (v.getId() == R.id.filter_all) selectedFilter = "All";
            else if (v.getId() == R.id.filter_veg) selectedFilter = "Veg";
            else if (v.getId() == R.id.filter_non_veg) selectedFilter = "Non-Veg";

            applyFilters();
        };

        if (fAll != null) fAll.setOnClickListener(clickListener);
        if (fVeg != null) fVeg.setOnClickListener(clickListener);
        if (fNonVeg != null) fNonVeg.setOnClickListener(clickListener);
    }

    private void resetFilterUI() {
        int[] ids = {R.id.filter_all, R.id.filter_veg, R.id.filter_non_veg};
        for (int id : ids) {
            MaterialCardView card = findViewById(id);
            if (card != null) {
                card.setStrokeWidth(dpToPx(1));
                card.setStrokeColor(ContextCompat.getColor(this, R.color.divider));
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
                TextView tv = (TextView) card.getChildAt(0);
                tv.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
            }
        }
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void applyFilters() {
        filteredList.clear();
        for (CommonFoodItem i : foodItemList) {
            boolean matchesSearch = i.getName().toLowerCase().contains(currentSearchQuery);
            boolean matchesDietary = "All".equalsIgnoreCase(selectedFilter) ||
                    ("Veg".equalsIgnoreCase(selectedFilter) && i.isVeg()) ||
                    ("Non-Veg".equalsIgnoreCase(selectedFilter) && !i.isVeg());

            if (matchesSearch && matchesDietary) {
                filteredList.add(i);
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();

        if (llNoFoodItems != null) {
            llNoFoodItems.setVisibility(foodItemList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (noResultsText != null) {
            noResultsText.setVisibility(!foodItemList.isEmpty() && filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void fetchFoodItems() {
        if (restaurantId == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "Error: Missing restaurant ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (menuListener != null) menuListener.remove();
        
        menuListener = db.collection("restaurants").document(restaurantId)
                .collection("foodItems").addSnapshotListener((value, error) -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (shimmerView != null) shimmerView.setVisibility(View.GONE);
            if (error != null) return;
            if (value != null) {
                foodItemList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    try {
                        CommonFoodItem foodItem = doc.toObject(CommonFoodItem.class);
                        if (foodItem != null) {
                            foodItem.setId(doc.getId());
                            foodItemList.add(foodItem);
                        }
                    } catch (Exception e) {
                        // Skip invalid data
                    }
                }
                applyFilters();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (menuListener != null) menuListener.remove();
        super.onDestroy();
    }

}
