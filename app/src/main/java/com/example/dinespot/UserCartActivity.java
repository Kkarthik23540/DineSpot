package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserCartActivity extends CommonBaseActivity {

    private RecyclerView cartRecyclerView;
    private UserCartAdapter cartAdapter;
    private List<CommonFoodItem> cartItems;
    private List<CommonFoodItem> fullCartList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private View emptyCartMessage, shimmerView, searchCard;
    private Button orderButton;
    private TextView totalPriceText;
    private View summaryCard, bookingInfoCard;
    private TextView tvBookingTable, tvBookingTime;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText searchBar;
    private ListenerRegistration cartListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_cart);

        setupToolbar("My Cart", true);

        cartRecyclerView = findViewById(R.id.cartRecyclerView);
        emptyCartMessage = findViewById(R.id.emptyCartMessage);
        orderButton = findViewById(R.id.orderButton);
        totalPriceText = findViewById(R.id.totalPriceText);
        progressBar = findViewById(R.id.progressBar);
        summaryCard = findViewById(R.id.bottom_summary_card);
        bookingInfoCard = findViewById(R.id.bookingInfoCard);
        tvBookingTable = findViewById(R.id.tvBookingTable);
        tvBookingTime = findViewById(R.id.tvBookingTime);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        shimmerView = findViewById(R.id.shimmer_view_container);
        searchBar = findViewById(R.id.searchBar);
        searchCard = findViewById(R.id.searchCard);
        
        cartItems = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();
        
        cartAdapter = new UserCartAdapter(this, cartItems);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartRecyclerView.setAdapter(cartAdapter);

        setupSwipeToDelete();
        setupSearch();
        setupRealtimeCartListener();
        setupBottomNavigation();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::setupRealtimeCartListener);
        }

        orderButton.setOnClickListener(v -> proceedToOrderSummary());
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                CommonFoodItem item = cartItems.get(pos);
                removeCartItem(item);
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(cartRecyclerView);
    }

    private void setupSearch() {
        if (searchBar != null) {
            searchBar.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterCart(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filterCart(String query) {
        String lowerQuery = query.toLowerCase().trim();
        cartItems.clear();
        for (CommonFoodItem item : fullCartList) {
            if (item.getName().toLowerCase().contains(lowerQuery)) {
                cartItems.add(item);
            }
        }
        cartAdapter.notifyDataSetChanged();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bnv = findViewById(R.id.bottom_navigation);
        bnv.inflateMenu(R.menu.bottom_nav_menu);
        CommonNavigationHelper.setupUserNavigation(this, bnv, R.id.menu_cart);
    }

    private void setupRealtimeCartListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (cartListener != null) cartListener.remove();
        
        firestore.collection("users").whereEqualTo("email", currentUser.getEmail()).get().addOnSuccessListener(snaps -> {
            if (!snaps.isEmpty()) {
                cartListener = snaps.getDocuments().get(0).getReference()
                        .collection("cart")
                        .addSnapshotListener((snapshots, e) -> {
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                            if (shimmerView != null) shimmerView.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            if (snapshots != null) {
                                fullCartList.clear();
                                for (QueryDocumentSnapshot document : snapshots) {
                                    CommonFoodItem foodItem = document.toObject(CommonFoodItem.class);
                                    foodItem.setId(document.getId());
                                    fullCartList.add(foodItem);
                                }
                                updateCartUI();
                                filterCart(searchBar != null ? searchBar.getText().toString() : "");
                                fetchActiveBooking();
                            }
                        });
            } else {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void fetchActiveBooking() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cartItems.isEmpty()) {
            if (bookingInfoCard != null) bookingInfoCard.setVisibility(View.GONE);
            return;
        }

        String rid = cartItems.get(0).getRestaurantId();
        firestore.collection("bookings")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("restaurantId", rid)
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        DocumentSnapshot b = snaps.getDocuments().get(0);
                        if (bookingInfoCard != null) bookingInfoCard.setVisibility(View.VISIBLE);
                        if (tvBookingTable != null) {
                            String type = b.getString("tableType");
                            tvBookingTable.setText(getFriendlyTableType(type) + " Table Reserved");
                        }
                        if (tvBookingTime != null) {
                            tvBookingTime.setText("Time: " + b.getString("timeSlot"));
                        }
                    } else {
                        if (bookingInfoCard != null) bookingInfoCard.setVisibility(View.GONE);
                    }
                });
    }

    private String getFriendlyTableType(String type) {
        if ("2".equals(type)) return "Couple";
        if ("4".equals(type)) return "Family";
        if ("6".equals(type)) return "Group";
        if ("10".equals(type)) return "Large Group";
        return "Standard";
    }

    private void showEmptyCartMessage() {
        emptyCartMessage.setVisibility(View.VISIBLE);
        cartRecyclerView.setVisibility(View.GONE);
        if (summaryCard != null) summaryCard.setVisibility(View.GONE);
    }

    private void updateCartUI() {
        if (fullCartList.isEmpty()) {
            showEmptyCartMessage();
            if (searchCard != null) searchCard.setVisibility(View.GONE);
        } else {
            emptyCartMessage.setVisibility(View.GONE);
            cartRecyclerView.setVisibility(View.VISIBLE);
            if (summaryCard != null) summaryCard.setVisibility(View.VISIBLE);
            if (searchCard != null) searchCard.setVisibility(View.VISIBLE);
            totalPriceText.setText("₹" + calculateTotalPrice());
        }
        invalidateOptionsMenu();
    }

    private int calculateTotalPrice() {
        int total = 0;
        for (CommonFoodItem foodItem : fullCartList) {
            total += (foodItem.getPrice() * foodItem.getQuantity());
        }
        return total;
    }

    private void removeCartItem(CommonFoodItem foodItem) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        showLoading();
        
        firestore.collection("users").whereEqualTo("email", currentUser.getEmail()).get().addOnSuccessListener(snaps -> {
            if (!snaps.isEmpty()) {
                snaps.getDocuments().get(0).getReference()
                        .collection("cart")
                        .document(foodItem.getId())
                        .delete()
                        .addOnCompleteListener(task -> hideLoading());
            } else {
                hideLoading();
            }
        });
    }

    private void proceedToOrderSummary() {
        Intent intent = new Intent(UserCartActivity.this, UserOrderSummaryActivity.class);
        if (!cartItems.isEmpty()) {
            intent.putExtra("restaurantId", cartItems.get(0).getRestaurantId());
        }
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cart_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem clearItem = menu.findItem(R.id.clearCart);
        if (clearItem != null) {
            clearItem.setVisible(!cartItems.isEmpty());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.clearCart) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Cart")
                    .setMessage("Clear all items?")
                    .setPositiveButton("Clear", (dialog, which) -> clearCart())
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearCart() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        showLoading();
        
        firestore.collection("users").whereEqualTo("email", currentUser.getEmail()).get().addOnSuccessListener(snaps -> {
            if (!snaps.isEmpty()) {
                snaps.getDocuments().get(0).getReference()
                        .collection("cart").get().addOnSuccessListener(querySnapshot -> {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                document.getReference().delete();
                            }
                            hideLoading();
                        }).addOnFailureListener(e -> hideLoading());
            } else {
                hideLoading();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartListener != null) cartListener.remove();
    }
}
