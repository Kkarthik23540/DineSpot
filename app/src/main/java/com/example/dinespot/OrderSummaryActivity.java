package com.example.dinespot;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import org.json.JSONObject;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

public class OrderSummaryActivity extends AppCompatActivity implements PaymentResultListener {

    private RecyclerView orderItemsRecyclerView;
    private OrderItemsAdapter orderItemsAdapter;
    private List<FoodItem> orderItems;
    private FirebaseFirestore firestore;
    private TextView restaurantNameText, totalCostText, userNameText, userPhoneText;
    private Button paymentButton;
    private String restaurantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        Checkout.preload(getApplicationContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        restaurantNameText = findViewById(R.id.restaurantName);
        orderItemsRecyclerView = findViewById(R.id.orderItemsRecyclerView);
        totalCostText = findViewById(R.id.totalCostText);
        userNameText = findViewById(R.id.userName);
        userPhoneText = findViewById(R.id.userPhone);
        paymentButton = findViewById(R.id.paymentButton);

        orderItems = new ArrayList<>();
        orderItemsAdapter = new OrderItemsAdapter(this, orderItems);
        orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderItemsRecyclerView.setAdapter(orderItemsAdapter);

        restaurantId = getIntent().getStringExtra("restaurantId");

        firestore = FirebaseFirestore.getInstance();
        fetchOrderDetails();

        paymentButton.setOnClickListener(v -> startPayment());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void fetchOrderDetails() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            orderItems.clear();
                            for (QueryDocumentSnapshot documentSnapshot : querySnapshot) {
                                FoodItem foodItem = documentSnapshot.toObject(FoodItem.class);
                                foodItem.setId(documentSnapshot.getId());
                                orderItems.add(foodItem);
                            }
                            orderItemsAdapter.notifyDataSetChanged();
                            calculateTotalCost();
                        }
                    }
                });

        if (restaurantId != null) {
            fetchRestaurantName(restaurantId);
        }

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userNameText.setText(documentSnapshot.getString("name"));
                        userPhoneText.setText(documentSnapshot.getString("phone"));
                    }
                });
    }

    private void fetchRestaurantName(String restaurantId) {
        firestore.collection("restaurants")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        restaurantNameText.setText(documentSnapshot.getString("name"));
                    } else {
                        restaurantNameText.setText("Restaurant not found");
                    }
                })
                .addOnFailureListener(e -> {
                    restaurantNameText.setText("Error fetching restaurant name");
                });
    }

    private void calculateTotalCost() {
        int totalCost = 0;
        for (FoodItem foodItem : orderItems) {
            totalCost += foodItem.getPrice();
        }
        totalCostText.setText("₹" + totalCost);
    }

    private void startPayment() {
        Checkout checkout = new Checkout();

        checkout.setKeyID("rzp_test_1Nw23mh0dK1hUx");
        try {
            JSONObject options = new JSONObject();

            options.put("name", "DineSpot");
            options.put("description", "Payment for your order");
            options.put("currency", "INR");
            options.put("amount", calculateTotalAmountInPaise());
            options.put("description","Reference No. #1");
            options.put("theme.color","#3399cc");

            JSONObject prefill = new JSONObject();
            prefill.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
            prefill.put("contact", userPhoneText.getText().toString());
            options.put("prefill", prefill);

            checkout.open(OrderSummaryActivity.this, options);

        } catch (Exception e) {
            Toast.makeText(this, "Error in payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private int calculateTotalAmountInPaise() {
        int totalCost = 0;
        for (FoodItem foodItem : orderItems) {
            totalCost += foodItem.getPrice();
        }
        return totalCost * 100;
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Toast.makeText(this, "Payment Successful! Payment ID: " + razorpayPaymentID, Toast.LENGTH_SHORT).show();

        // TODO: Send confirmation email or update backend
        Intent intent = new Intent(OrderSummaryActivity.this, PaymentSuccessActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onPaymentError(int code, String response) {
        Toast.makeText(this, "Payment failed: " + response, Toast.LENGTH_SHORT).show();
    }
}
