package com.example.dinespot;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdminRestaurantAdapter extends RecyclerView.Adapter<AdminRestaurantAdapter.ViewHolder> {

    private Context context;
    private List<CommonRestaurant> restaurantList;

    public AdminRestaurantAdapter(Context context, List<CommonRestaurant> restaurantList) {
        this.context = context;
        this.restaurantList = restaurantList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.admin_item_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommonRestaurant restaurant = restaurantList.get(position);
        holder.tvName.setText(restaurant.getName());
        holder.tvAddress.setText(restaurant.getAddress());
        
        String status = restaurant.getStatus() != null ? restaurant.getStatus() : "Open";
        updateStatusUI(holder, status);

        // Display Hours
        if (holder.tvOperatingHours != null) {
            if (restaurant.getOpeningTime() != null && !restaurant.getOpeningTime().isEmpty()) {
                holder.tvOperatingHours.setVisibility(View.VISIBLE);
                holder.tvOperatingHours.setText(restaurant.getOpeningTime() + " - " + restaurant.getClosingTime());
            } else {
                holder.tvOperatingHours.setVisibility(View.GONE);
            }
        }

        String phone = restaurant.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            holder.tvPhone.setText(phone);
            holder.layoutActionCall.setVisibility(View.VISIBLE);
            holder.layoutActionCall.setOnClickListener(v -> {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phone));
                context.startActivity(callIntent);
            });
        } else {
            holder.tvPhone.setText("No Contact Number");
            holder.layoutActionCall.setVisibility(View.GONE);
        }

        loadImage(holder.ivRestaurant, restaurant.getImageUrl());

        holder.layoutActionEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminAddEditRestaurantActivity.class);
            intent.putExtra("restaurant_id", restaurant.getRestaurantId());
            context.startActivity(intent);
        });

        holder.layoutActionMenu.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminManageFoodItemsActivity.class);
            intent.putExtra("restaurant_id", restaurant.getRestaurantId());
            intent.putExtra("restaurant_name", restaurant.getName());
            context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminFloorPlanActivity.class);
            intent.putExtra("restaurant_id", restaurant.getRestaurantId());
            intent.putExtra("restaurant_name", restaurant.getName());
            context.startActivity(intent);
        });

        holder.layoutActionDelete.setOnClickListener(v -> checkDeleteAccess(restaurant, holder.itemView));
    }

    private void updateStatusUI(ViewHolder holder, String status) {
        if ("Full".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText("FULL");
            holder.cardStatusBadge.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorWarning));
        } else if ("Closed".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText("CLOSED");
            holder.cardStatusBadge.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorError));
        } else {
            holder.tvStatusBadge.setText("OPEN");
            holder.cardStatusBadge.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorSuccess));
        }
    }

    private void loadImage(ImageView imageView, String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("http")) {
                Glide.with(context.getApplicationContext()).load(imageUrl)
                        .placeholder(R.drawable.baseline_image_24)
                        .error(R.drawable.baseline_image_24)
                        .into(imageView);
            } else {
                try {
                    byte[] imageBytes = Base64.decode(imageUrl.contains(",") ? imageUrl.split(",")[1] : imageUrl, Base64.DEFAULT);
                    Glide.with(context.getApplicationContext()).asBitmap().load(imageBytes)
                            .placeholder(R.drawable.baseline_image_24)
                            .into(imageView);
                } catch (Exception e) {
                    imageView.setImageResource(R.drawable.baseline_image_24);
                }
            }
        } else {
            imageView.setImageResource(R.drawable.baseline_image_24);
        }
    }

    private void checkDeleteAccess(CommonRestaurant restaurant, View anchor) {
        String email = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email == null) return;

        FirebaseFirestore.getInstance().collection("admins")
                .whereEqualTo("email", email.toLowerCase())
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        String role = snaps.getDocuments().get(0).getString("role");
                        if ("super".equalsIgnoreCase(role)) {
                            showDeleteDialog(restaurant);
                        } else {
                            Snackbar.make(anchor, "Access Denied: Only Super Admins can delete restaurants.", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showDeleteDialog(CommonRestaurant restaurant) {
        if (context instanceof CommonBaseActivity) {
            ((CommonBaseActivity) context).showCustomDialog(R.drawable.baseline_delete_outline_24,
                    "Delete Restaurant",
                    "Are you sure you want to delete " + restaurant.getName() + "?",
                    "Delete", () -> {
                        FirebaseFirestore.getInstance().collection("restaurants").document(restaurant.getRestaurantId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    if (context instanceof CommonBaseActivity) {
                                        ((CommonBaseActivity) context).logAdminAction("Restaurant Deleted", restaurant.getName());
                                    }
                                    Snackbar.make(((CommonBaseActivity) context).findViewById(android.R.id.content), "Restaurant Removed Successfully", Snackbar.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Snackbar.make(((CommonBaseActivity) context).findViewById(android.R.id.content), "Failed to delete: " + e.getMessage(), Snackbar.LENGTH_SHORT).show());
                    },
                    "Cancel", null);
        }
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvPhone, tvStatusBadge, tvOperatingHours;
        ImageView ivRestaurant;
        View layoutActionEdit, layoutActionMenu, layoutActionDelete, layoutActionCall;
        MaterialCardView cardStatusBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvOperatingHours = itemView.findViewById(R.id.tvOperatingHours);
            ivRestaurant = itemView.findViewById(R.id.ivRestaurant);
            cardStatusBadge = itemView.findViewById(R.id.cardStatusBadge);
            layoutActionEdit = itemView.findViewById(R.id.layoutActionEdit);
            layoutActionMenu = itemView.findViewById(R.id.layoutActionMenu);
            layoutActionDelete = itemView.findViewById(R.id.layoutActionDelete);
            layoutActionCall = itemView.findViewById(R.id.layoutActionCall);
        }
    }
}
