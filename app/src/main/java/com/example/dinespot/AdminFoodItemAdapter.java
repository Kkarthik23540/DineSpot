package com.example.dinespot;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdminFoodItemAdapter extends RecyclerView.Adapter<AdminFoodItemAdapter.ViewHolder> {

    private Context context;
    private List<CommonFoodItem> foodItemList;
    private String restaurantId;

    public AdminFoodItemAdapter(Context context, List<CommonFoodItem> foodItemList, String restaurantId) {
        this.context = context;
        this.foodItemList = foodItemList;
        this.restaurantId = restaurantId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.admin_item_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommonFoodItem foodItem = foodItemList.get(position);
        holder.tvName.setText(foodItem.getName());
        holder.tvPrice.setText("Price: ₹" + foodItem.getPrice());

        if (foodItem.isAvailable()) {
            holder.tvStatus.setText("Available");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorSuccess));
        } else {
            holder.tvStatus.setText("Sold Out");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorError));
        }

        if (foodItem.getImageUrl() != null && !foodItem.getImageUrl().isEmpty()) {
            String imageUrl = foodItem.getImageUrl();
            if (imageUrl.startsWith("http")) {
                Glide.with(context.getApplicationContext()).load(imageUrl)
                        .placeholder(R.drawable.baseline_restaurant_menu_24)
                        .error(R.drawable.baseline_restaurant_menu_24)
                        .into(holder.ivFood);
            } else {
                try {
                    String cleanBase = imageUrl.contains(",") ? imageUrl.split(",")[1] : imageUrl;
                    byte[] imageBytes = Base64.decode(cleanBase.trim(), Base64.DEFAULT);
                    Glide.with(context.getApplicationContext()).asBitmap().load(imageBytes)
                            .placeholder(R.drawable.baseline_restaurant_menu_24)
                            .error(R.drawable.baseline_restaurant_menu_24)
                            .into(holder.ivFood);
                } catch (Exception e) {
                    holder.ivFood.setImageResource(R.drawable.baseline_restaurant_menu_24);
                }
            }
        } else {
            holder.ivFood.setImageResource(R.drawable.baseline_restaurant_menu_24);
        }

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminAddEditFoodItemActivity.class);
            intent.putExtra("restaurant_id", restaurantId);
            intent.putExtra("food_item_id", foodItem.getId());
            context.startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> {
            String email = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail();
            if (email != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("admins")
                        .whereEqualTo("email", email.toLowerCase())
                        .get()
                        .addOnSuccessListener(snaps -> {
                            if (!snaps.isEmpty()) {
                                String role = snaps.getDocuments().get(0).getString("role");
                                if ("super".equalsIgnoreCase(role)) {
                                    showDeleteDialog(holder, foodItem);
                                } else {
                                    Snackbar.make(holder.itemView, "Access Denied: Only Super Admins can delete items.", Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }

    private void showDeleteDialog(ViewHolder holder, CommonFoodItem foodItem) {
        if (context instanceof CommonBaseActivity) {
            ((CommonBaseActivity) context).showCustomDialog(R.drawable.baseline_delete_outline_24,
                    "Delete Food Item",
                    "Are you sure you want to delete this food item?",
                    "Delete", () -> {
                        FirebaseFirestore.getInstance().collection("restaurants").document(restaurantId)
                                .collection("foodItems").document(foodItem.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> Snackbar.make(holder.itemView, "Food Item Removed Successfully", Snackbar.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Snackbar.make(holder.itemView, "Failed to delete: " + e.getMessage(), Snackbar.LENGTH_SHORT).show());
                    },
                    "Cancel", null);
        }
    }

    @Override
    public int getItemCount() {
        return foodItemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvStatus;
        ImageView ivFood;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivFood = itemView.findViewById(R.id.ivFood);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
