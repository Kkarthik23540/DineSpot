package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserOrderSummaryActivity extends CommonBaseActivity implements PaymentResultListener {

    private RecyclerView orderItemsRecyclerView;
    private UserOrderItemAdapter orderItemsAdapter;
    private List<CommonFoodItem> orderItems;
    private FirebaseFirestore firestore;
    private TextView restaurantNameText, totalCostText, userNameText, userPhoneText;
    private Button paymentButton;
    private String restaurantId;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View orderScroll, orderSkeleton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_order_summary);

        Checkout.preload(getApplicationContext());

        setupToolbar("Order Summary", true);
        
        restaurantNameText = findViewById(R.id.restaurantName);
        orderItemsRecyclerView = findViewById(R.id.orderItemsRecyclerView);
        totalCostText = findViewById(R.id.totalCostText);
        userNameText = findViewById(R.id.userName);
        userPhoneText = findViewById(R.id.userPhone);
        paymentButton = findViewById(R.id.paymentButton);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        orderScroll = findViewById(R.id.order_scroll);
        orderSkeleton = findViewById(R.id.order_skeleton);

        orderItems = new ArrayList<>();
        orderItemsAdapter = new UserOrderItemAdapter(this, orderItems);
        orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderItemsRecyclerView.setAdapter(orderItemsAdapter);

        restaurantId = getIntent().getStringExtra("restaurantId");

        firestore = FirebaseFirestore.getInstance();
        
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchOrderDetails);
        }
        
        fetchOrderDetails();

        paymentButton.setOnClickListener(v -> startPayment());
    }

    @android.annotation.SuppressLint("MissingSuperCall")
    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(this, UserCartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
        return true;
    }

    private void fetchOrderDetails() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        firestore.collection("users").whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(userSnaps -> {
            if (userSnaps.isEmpty()) return;
            com.google.firebase.firestore.DocumentReference userRef = userSnaps.getDocuments().get(0).getReference();
            
            // 1. Fetch Cart Items
            userRef.collection("cart").get()
                    .addOnCompleteListener(task -> {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        hideSkeleton();
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                orderItems.clear();
                                for (QueryDocumentSnapshot documentSnapshot : querySnapshot) {
                                    CommonFoodItem foodItem = documentSnapshot.toObject(CommonFoodItem.class);
                                    foodItem.setId(documentSnapshot.getId());
                                    orderItems.add(foodItem);
                                }
                                orderItemsAdapter.notifyItemRangeInserted(0, orderItems.size());
                                calculateTotalCost();
                            }
                        }
                    });

            // 2. Load User Info
            com.google.firebase.firestore.DocumentSnapshot documentSnapshot = userSnaps.getDocuments().get(0);
            userNameText.setText(documentSnapshot.getString("name"));
            String phone = documentSnapshot.getString("phone");
            userPhoneText.setText(phone != null ? phone : "No phone number");
            
            // 3. Clear Cart helper will use userRef later
        });

        if (restaurantId != null) {
            fetchRestaurantName(restaurantId);
        }
    }

    private void fetchRestaurantName(String restaurantId) {
        firestore.collection("restaurants")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        restaurantNameText.setText(documentSnapshot.getString("name"));
                    } else {
                        restaurantNameText.setText("Restaurant Information");
                    }
                })
                .addOnFailureListener(e -> restaurantNameText.setText("Order Summary"));
    }

    private void calculateTotalCost() {
        long totalCost = 0;
        for (CommonFoodItem foodItem : orderItems) {
            long qty = foodItem.getQuantity() > 0 ? foodItem.getQuantity() : 1;
            totalCost += (foodItem.getPrice() * qty);
        }
        totalCostText.setText("₹" + totalCost);
    }

    private void startPayment() {
        if (orderItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        togglePaymentLoading(true);

        Checkout checkout = new Checkout();
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID);
        
        try {
            JSONObject options = new JSONObject();
            options.put("name", "DineSpot");
            options.put("description", "Food Order from " + restaurantNameText.getText().toString());
            options.put("currency", "INR");
            options.put("amount", calculateTotalAmountInPaise());
            options.put("theme.color", "#118D9D");

            JSONObject prefill = new JSONObject();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                prefill.put("email", user.getEmail());
            }
            prefill.put("contact", userPhoneText.getText().toString());
            options.put("prefill", prefill);

            checkout.open(UserOrderSummaryActivity.this, options);

        } catch (Exception e) {
            togglePaymentLoading(false);
            Toast.makeText(this, "Error in payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void togglePaymentLoading(boolean isLoading) {
        paymentButton.setText(isLoading ? "" : "Confirm and Pay");
        paymentButton.setEnabled(!isLoading);
        if (isLoading) showLoading(); else hideLoading();
    }

    private void hideSkeleton() {
        if (orderSkeleton != null) orderSkeleton.setVisibility(View.GONE);
        if (orderScroll != null) orderScroll.setVisibility(View.VISIBLE);
    }

    private int calculateTotalAmountInPaise() {
        long totalCost = 0;
        for (CommonFoodItem foodItem : orderItems) {
            long qty = foodItem.getQuantity() > 0 ? foodItem.getQuantity() : 1;
            totalCost += (foodItem.getPrice() * qty);
        }
        return (int) (totalCost * 100);
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();
        saveOrderToHistory();
    }

    private void saveOrderToHistory() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();

        // 1. Find User Doc
        firestore.collection("users").whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(userSnaps -> {
            if (userSnaps.isEmpty()) return;
            com.google.firebase.firestore.DocumentReference userRef = userSnaps.getDocuments().get(0).getReference();

            // 2. Check for active booking
            firestore.collection("bookings")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("restaurantId", restaurantId)
                    .whereIn("status", java.util.Arrays.asList("Accepted", "Pending"))
                    .get()
                    .addOnSuccessListener(bookingSnaps -> {
                        String bookingId = null;
                        if (!bookingSnaps.isEmpty()) {
                            bookingId = bookingSnaps.getDocuments().get(0).getId();
                        }

                        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
                        
                        List<Map<String, Object>> itemsList = new ArrayList<>();
                        for (CommonFoodItem item : orderItems) {
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("name", item.getName());
                            itemMap.put("price", item.getPrice());
                            itemsList.add(itemMap);
                        }

                        if (bookingId != null) {
                            DocumentReference bookingRef = firestore.collection("bookings").document(bookingId);
                            DocumentSnapshot bDoc = bookingSnaps.getDocuments().get(0);
                            String tableType = bDoc.getString("tableType");
                            
                            batch.update(bookingRef, "orderedItems", itemsList);
                            batch.update(bookingRef, "orderStatus", "Served");
                            batch.update(bookingRef, "status", "Completed");

                            // Restore table availability
                            if (tableType != null) {
                                DocumentReference resRef = firestore.collection("restaurants").document(restaurantId);
                                batch.update(resRef, "tableTypes." + tableType, com.google.firebase.firestore.FieldValue.increment(1));
                                batch.update(resRef, "noOfTables", com.google.firebase.firestore.FieldValue.increment(1));
                            }
                        } else {
                            // If no booking found, create a standalone order (User might have booked via another channel or it's a walk-in/direct order)
                            Map<String, Object> foodOrder = new HashMap<>();
                            foodOrder.put("userId", userId);
                            foodOrder.put("userName", userNameText.getText().toString());
                            foodOrder.put("phoneNumber", userPhoneText.getText().toString());
                            foodOrder.put("restaurantId", restaurantId);
                            foodOrder.put("restaurantName", restaurantNameText.getText().toString());
                            foodOrder.put("status", "Pending");
                            foodOrder.put("orderStatus", "Received");
                            foodOrder.put("orderedItems", itemsList);
                            foodOrder.put("dateTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                            
                            batch.set(firestore.collection("bookings").document(), foodOrder);
                        }

                        // 3. Clear Cart
                        userRef.collection("cart").get().addOnSuccessListener(querySnapshot -> {
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                batch.delete(doc.getReference());
                            }

                            batch.commit().addOnCompleteListener(task -> {
                                togglePaymentLoading(false);
                                if (task.isSuccessful()) {
                                    Intent intent = new Intent(UserOrderSummaryActivity.this, UserPaymentSuccessActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, "Failed to finalize order", Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
                    });
        });
    }

    @Override
    public void onPaymentError(int code, String response) {
        togglePaymentLoading(false);
        Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show();
    }
}
