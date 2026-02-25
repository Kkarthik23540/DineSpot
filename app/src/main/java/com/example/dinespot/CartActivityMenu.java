package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CartActivityMenu extends AppCompatActivity {

    private RecyclerView cartRecyclerView;
    private CartAdapter cartAdapter;
    private List<FoodItem> cartItems;
    private FirebaseFirestore firestore;
    private View emptyCartMessage;
    private Button orderButton;
    private TextView totalPriceText;
    private ProgressBar progressBar; // ProgressBar
    private boolean isMultiSelectMode = false;
    private List<FoodItem> selectedItems = new ArrayList<>(); // For storing selected items

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_menu);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cartRecyclerView = findViewById(R.id.cartRecyclerView);
        emptyCartMessage = findViewById(R.id.emptyCartMessage);
        orderButton = findViewById(R.id.orderButton);
        totalPriceText = findViewById(R.id.totalPriceText);
        progressBar = findViewById(R.id.progressBar);
        cartItems = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();
        cartAdapter = new CartAdapter(this, cartItems, this::onItemLongClick, this::showRemoveConfirmationDialog);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartRecyclerView.setAdapter(cartAdapter);

        fetchCartItems();

        orderButton.setOnClickListener(v -> proceedToOrderSummary());
    }

    private void fetchCartItems() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE); // Hide ProgressBar
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            emptyCartMessage.setVisibility(View.GONE);
                            cartRecyclerView.setVisibility(View.VISIBLE);
                            cartItems.clear();
                            for (QueryDocumentSnapshot documentSnapshot : querySnapshot) {
                                FoodItem foodItem = documentSnapshot.toObject(FoodItem.class);
                                foodItem.setId(documentSnapshot.getId());
                                cartItems.add(foodItem);
                            }
                            cartAdapter.notifyDataSetChanged();
                            updateCart();
                        } else {
                            showEmptyCartMessage();
                        }
                    } else {
                        showEmptyCartMessage();
                    }
                });
    }

    private void showEmptyCartMessage() {
        emptyCartMessage.setVisibility(View.VISIBLE);
        cartRecyclerView.setVisibility(View.GONE);
        orderButton.setVisibility(View.GONE);
        totalPriceText.setVisibility(View.GONE);
    }

    private void updateCart() {
        if (cartItems.isEmpty()) {
            showEmptyCartMessage();
        } else {
            emptyCartMessage.setVisibility(View.GONE);
            cartRecyclerView.setVisibility(View.VISIBLE);
            totalPriceText.setVisibility(View.VISIBLE);
            orderButton.setVisibility(View.VISIBLE);
            totalPriceText.setText("Total: ₹" + calculateTotalPrice());
        }
    }

    private int calculateTotalPrice() {
        int total = 0;
        for (FoodItem foodItem : cartItems) {
            total += foodItem.getPrice();
        }
        return total;
    }

    private void onItemLongClick(FoodItem foodItem) {
        isMultiSelectMode = true;
        toggleSelection(foodItem);
    }

    private void toggleSelection(FoodItem foodItem) {
        if (selectedItems.contains(foodItem)) {
            selectedItems.remove(foodItem);
        } else {
            selectedItems.add(foodItem);
        }
        cartAdapter.setSelectedItems(selectedItems);
        cartAdapter.notifyDataSetChanged();
    }

    private void showRemoveConfirmationDialog(FoodItem foodItem) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Item")
                .setMessage("Are you sure you want to remove this item from your cart?")
                .setPositiveButton("Yes", (dialog, which) -> removeCartItem(foodItem))
                .setNegativeButton("No", null)
                .show();
    }

    private void removeCartItem(FoodItem foodItem) {
        progressBar.setVisibility(View.VISIBLE); // Show ProgressBar
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .document(foodItem.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    cartItems.remove(foodItem);
                    cartAdapter.notifyDataSetChanged();
                    updateCart();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void clearCart() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        document.getReference().delete();
                    }
                    cartItems.clear();
                    cartAdapter.notifyDataSetChanged();
                    showEmptyCartMessage();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
    }

    private void proceedToOrderSummary() {
        Intent intent = new Intent(CartActivityMenu.this, OrderSummaryActivity.class);
        intent.putExtra("totalAmount", calculateTotalPrice());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cart_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.clearCart) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Cart")
                    .setMessage("Are you sure you want to clear your entire cart?")
                    .setPositiveButton("Yes", (dialog, which) -> clearCart())
                    .setNegativeButton("No", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
