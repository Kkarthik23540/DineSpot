package com.example.dinespot;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class UserRestaurantAdapter extends RecyclerView.Adapter<UserRestaurantAdapter.RestaurantViewHolder> {
    private Context context;
    private List<CommonRestaurant> restaurantList;
    private List<String> favoriteIds;
    private OnFavoriteClickListener favoriteListener;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(CommonRestaurant restaurant, boolean isFavorite);
    }

    public UserRestaurantAdapter(Context context, List<CommonRestaurant> restaurantList, List<String> favoriteIds, OnFavoriteClickListener favoriteListener) {
        this.context = context;
        this.restaurantList = restaurantList;
        this.favoriteIds = favoriteIds;
        this.favoriteListener = favoriteListener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        CommonRestaurant r = restaurantList.get(position);
        return r != null && r.getRestaurantId() != null ? r.getRestaurantId().hashCode() : position;
    }

    public void updateList(List<CommonRestaurant> newList) {
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override public int getOldListSize() { return restaurantList.size(); }
            @Override public int getNewListSize() { return newList.size(); }
            @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return restaurantList.get(oldItemPosition).getRestaurantId().equals(newList.get(newItemPosition).getRestaurantId());
            }
            @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                CommonRestaurant oldR = restaurantList.get(oldItemPosition);
                CommonRestaurant newR = newList.get(newItemPosition);
                return java.util.Objects.equals(oldR.getStatus(), newR.getStatus()) && 
                       oldR.getRating() == newR.getRating() && 
                       java.util.Objects.equals(oldR.getName(), newR.getName());
            }
        });
        this.restaurantList.clear();
        this.restaurantList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public void updateFavorites(List<String> favoriteIds) {
        this.favoriteIds = favoriteIds;
        notifyDataSetChanged();
    }

    @Override
    public RestaurantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.user_item_restaurant, parent, false);
        return new RestaurantViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(RestaurantViewHolder holder, int position) {
        // Removed entry animation to prevent "right-side sliding" on refresh
        
        CommonRestaurant restaurant = restaurantList.get(position);
        if (restaurant == null) return;

        holder.nameTextView.setText(restaurant.getName() != null ? restaurant.getName() : "Unknown Restaurant");
        holder.addressTextView.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "Address not available");
        
        // Fix Rating Glitch: Use 1 decimal place consistency and ensure visibility
        float rating = restaurant.getRating();
        if (rating > 0) {
            holder.ratingTextView.setText(String.format(java.util.Locale.getDefault(), "%.1f", rating));
            holder.ratingTextView.setVisibility(View.VISIBLE);
            holder.ivStar.setVisibility(View.VISIBLE);
        } else {
            holder.ratingTextView.setText("New");
            holder.ivStar.setVisibility(View.GONE);
        }
        
        // Total tables count removed as requested
        if (holder.tablesTextView != null) holder.tablesTextView.setVisibility(View.GONE);

        if (restaurant.getDistance() > 0) {
            holder.tvDistance.setVisibility(View.VISIBLE);
            holder.tvDistance.setText(String.format(java.util.Locale.getDefault(), "%.1f km far away", restaurant.getDistance()));
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        // Favorite Logic
        boolean isFav = favoriteIds != null && favoriteIds.contains(restaurant.getRestaurantId());
        holder.btnFavorite.setImageResource(isFav ? R.drawable.baseline_favorite_24 : R.drawable.baseline_favorite_border_24);
        holder.btnFavorite.setOnClickListener(v -> {
            if (favoriteListener != null) {
                favoriteListener.onFavoriteClick(restaurant, !isFav);
            }
        });

        String imageUrl = restaurant.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (holder.imageFlipper != null) {
                holder.imageFlipper.stopFlipping();
                holder.imageFlipper.setDisplayedChild(0);
            }
            if (imageUrl.startsWith("http")) {
                if (context != null) Glide.with(context.getApplicationContext()).load(imageUrl)
                        .placeholder(R.color.white)
                        .error(android.R.drawable.alert_light_frame)
                        .into(holder.imageView);
            } else {
                try {
                    String cleanBase64 = imageUrl;
                    if (imageUrl.contains(",")) {
                        String[] parts = imageUrl.split(",");
                        if (parts.length > 1) {
                            cleanBase64 = parts[1];
                        }
                    }
                    byte[] imageBytes = Base64.decode(cleanBase64.trim(), Base64.DEFAULT);
                    if (context != null) Glide.with(context.getApplicationContext())
                            .asBitmap()
                            .load(imageBytes)
                            .placeholder(R.color.white)
                            .error(android.R.drawable.alert_light_frame)
                            .into(holder.imageView);
                } catch (Exception e) {
                    if (holder.imageView != null) holder.imageView.setImageResource(R.color.white);
                }
            }
        } else {
            if (holder.imageFlipper != null) {
                holder.imageFlipper.setDisplayedChild(1);
                holder.imageFlipper.startFlipping();
            }
        }

        if (restaurant == null) return;

        String status = restaurant.getStatus();
        if (status == null) status = "Open";

        if ("Closed".equalsIgnoreCase(status)) {
            holder.itemView.setAlpha(1.0f);
            holder.statusBadgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorError));
            holder.tvStatusBadge.setText("CLOSED");
        } else if ("Full".equalsIgnoreCase(status)) {
            holder.itemView.setAlpha(1.0f);
            holder.statusBadgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorWarning));
            holder.tvStatusBadge.setText("FULL");
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.statusBadgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorSuccess));
            holder.tvStatusBadge.setText("OPEN");
        }

        // Show hours for user
        if (restaurant.getOpeningTime() != null && !restaurant.getOpeningTime().isEmpty()) {
            holder.tvOperatingHours.setVisibility(View.VISIBLE);
            holder.tvOperatingHours.setText(restaurant.getOpeningTime() + " - " + restaurant.getClosingTime());
        } else {
            holder.tvOperatingHours.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (restaurant != null && restaurant.getRestaurantId() != null) {
                Intent intent = new Intent(context, UserRestaurantDetailsActivity.class);
                intent.putExtra("restaurant_id", restaurant.getRestaurantId());
                
                // Professional Shared Element Transition (Hero Animation)
                if (context instanceof android.app.Activity) {
                    androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                            (android.app.Activity) context, holder.imageView, "restaurant_image_transition");
                    context.startActivity(intent, options.toBundle());
                } else {
                    context.startActivity(intent);
                }
            } else {
                Toast.makeText(context, "Restaurant data error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }
    public static class RestaurantViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView, addressTextView, ratingTextView, tablesTextView, tvStatusBadge, tvDistance, tvOperatingHours;
        public ImageView imageView, ivStar;
        public android.widget.ViewFlipper imageFlipper;
        public android.widget.ImageButton btnFavorite;
        public MaterialCardView statusBadgeCard;

        public RestaurantViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.restaurant_name);
            addressTextView = itemView.findViewById(R.id.restaurant_address);
            ratingTextView = itemView.findViewById(R.id.restaurant_rating);
            ivStar = itemView.findViewById(R.id.iv_star_rating);
            tablesTextView = itemView.findViewById(R.id.restaurant_tables);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            tvOperatingHours = itemView.findViewById(R.id.tv_operating_hours);
            statusBadgeCard = itemView.findViewById(R.id.statusBadgeCard);
            imageView = itemView.findViewById(R.id.restaurant_image);
            imageFlipper = itemView.findViewById(R.id.image_flipper);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }
    }

}
