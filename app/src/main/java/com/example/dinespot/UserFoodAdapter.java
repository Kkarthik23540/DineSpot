package com.example.dinespot;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class UserFoodAdapter extends RecyclerView.Adapter<UserFoodAdapter.FoodViewHolder> {

    private Context context;
    private List<CommonFoodItem> foodItemList;
    private FirebaseFirestore firestore;
    private String restaurantId;
    private OnItemAddedListener addListener;
    private com.google.firebase.firestore.DocumentReference userDocRef;

    public interface OnItemAddedListener {
        void onItemAdded(CommonFoodItem item);
    }

    public UserFoodAdapter(Context context, List<CommonFoodItem> foodItemList, String restaurantId, com.google.firebase.firestore.DocumentReference userDocRef, OnItemAddedListener addListener) {
        this.context = context;
        this.foodItemList = foodItemList;
        this.restaurantId = restaurantId;
        this.firestore = FirebaseFirestore.getInstance();
        this.userDocRef = userDocRef;
        this.addListener = addListener;
    }

    public void setUserDocRef(com.google.firebase.firestore.DocumentReference userDocRef) {
        this.userDocRef = userDocRef;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        CommonAnimationHelper.animateListEntry(holder.itemView, position);
        CommonAnimationHelper.applyClickAnimation(holder.addButton);

        CommonFoodItem foodItem = foodItemList.get(position);
        holder.foodName.setText(foodItem.getName());
        holder.foodPrice.setText("₹" + foodItem.getPrice());

        if (foodItem.isVeg()) {
            holder.ivVegNonVeg.setImageResource(R.drawable.baseline_done_24); 
            holder.ivVegNonVeg.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.colorSuccess));
        } else {
            holder.ivVegNonVeg.setImageResource(R.drawable.baseline_close_24);
            holder.ivVegNonVeg.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.colorError));
        }

        boolean isOutOfStock = foodItem.getStockCount() <= 0;
        if (foodItem.isAvailable() && !isOutOfStock) {
            holder.addButton.setEnabled(true);
            holder.addButton.setText("Add");
            holder.addButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(context, R.color.colorPrimary)));
            holder.itemView.setAlpha(1.0f);
            if (foodItem.getStockCount() < 10) {
                holder.tvStock.setVisibility(View.VISIBLE);
                holder.tvStock.setText(foodItem.getStockCount() + " left");
            } else {
                holder.tvStock.setVisibility(View.GONE);
            }
        } else {
            holder.addButton.setEnabled(false);
            holder.addButton.setText(isOutOfStock ? "Out of Stock" : "Sold Out");
            holder.addButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(context, R.color.textTertiary)));
            holder.itemView.setAlpha(0.6f);
            holder.tvStock.setVisibility(View.GONE);
        }

        String imageUrl = foodItem.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("http")) {
                Glide.with(context.getApplicationContext()).load(imageUrl).placeholder(R.drawable.baseline_restaurant_menu_24).into(holder.foodImage);
            } else {
                try {
                    byte[] imageBytes = Base64.decode(imageUrl.contains(",") ? imageUrl.split(",")[1] : imageUrl, Base64.DEFAULT);
                    Glide.with(context.getApplicationContext()).asBitmap().load(imageBytes).placeholder(R.drawable.baseline_restaurant_menu_24).into(holder.foodImage);
                } catch (Exception e) {
                    holder.foodImage.setImageResource(R.drawable.baseline_restaurant_menu_24);
                }
            }
        } else {
            holder.foodImage.setImageResource(R.drawable.baseline_restaurant_menu_24);
        }

        holder.addButton.setOnClickListener(v -> checkAndAddToCart(v, foodItem));
    }

    private void checkAndAddToCart(View view, CommonFoodItem foodItem) {
        if (userDocRef == null) {
            Toast.makeText(context, "Linking account...", Toast.LENGTH_SHORT).show();
            return;
        }

        userDocRef.collection("cart").get(com.google.firebase.firestore.Source.DEFAULT)
                .addOnCompleteListener(task -> {
                    boolean differentRestaurant = false;
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String existingRid = doc.getString("restaurantId");
                            if (existingRid != null && !existingRid.equals(restaurantId)) {
                                differentRestaurant = true;
                                break;
                            }
                        }
                    }

                    if (differentRestaurant) {
                        new AlertDialog.Builder(context)
                                .setTitle("Clear Cart?")
                                .setMessage("Clear existing items and add " + foodItem.getName() + "?")
                                .setPositiveButton("Clear & Add", (dialog, which) -> {
                                    clearCartAndAdd(userDocRef, foodItem, view);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        performAddToCart(userDocRef, foodItem, view);
                    }
                });
    }

    private void clearCartAndAdd(com.google.firebase.firestore.DocumentReference userRef, CommonFoodItem foodItem, View view) {
        userRef.collection("cart").get().addOnSuccessListener(snaps -> {
            for (QueryDocumentSnapshot doc : snaps) doc.getReference().delete();
            performAddToCart(userRef, foodItem, view);
        });
    }

    private void performAddToCart(com.google.firebase.firestore.DocumentReference userRef, CommonFoodItem foodItem, View view) {
        if (context instanceof CommonBaseActivity) ((CommonBaseActivity) context).vibrateAction();
        
        String itemId = foodItem.getId();
        if (itemId == null) return;

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", itemId);
        data.put("name", foodItem.getName());
        data.put("price", foodItem.getPrice());
        data.put("imageUrl", foodItem.getImageUrl());
        data.put("restaurantId", restaurantId);
        data.put("category", foodItem.getCategory() != null ? foodItem.getCategory() : "Others");
        data.put("veg", foodItem.isVeg());
        data.put("quantity", FieldValue.increment(1));

        userRef.collection("cart").document(itemId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (addListener != null) addListener.onItemAdded(foodItem);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Add failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return foodItemList.size();
    }

    public void updateFoodItemList(List<CommonFoodItem> filteredList) {
        this.foodItemList = filteredList;
        notifyDataSetChanged();
    }

    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView foodName, foodPrice, tvStock;
        ImageView foodImage, ivVegNonVeg;
        Button addButton;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            foodName = itemView.findViewById(R.id.foodName);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            tvStock = itemView.findViewById(R.id.tvStock);
            foodImage = itemView.findViewById(R.id.foodImage);
            ivVegNonVeg = itemView.findViewById(R.id.ivVegNonVeg);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }
}
