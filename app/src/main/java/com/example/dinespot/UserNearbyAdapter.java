package com.example.dinespot;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.util.List;

public class UserNearbyAdapter extends RecyclerView.Adapter<UserNearbyAdapter.ViewHolder> {

    private Context context;
    private List<CommonRestaurant> nearbyRestaurants;

    public UserNearbyAdapter(Context context, List<CommonRestaurant> nearbyRestaurants) {
        this.context = context;
        this.nearbyRestaurants = nearbyRestaurants;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.user_item_nearby, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommonRestaurant r = nearbyRestaurants.get(position);
        holder.tvName.setText(r.getName());
        float rating = r.getRating();
        if (rating > 0) {
            holder.tvRating.setText(String.format(java.util.Locale.getDefault(), "%.1f★", rating));
        } else {
            holder.tvRating.setText("New");
        }
        
        String status = r.getStatus();
        if (status == null) status = "Open";
        holder.tvStatus.setText(status.toUpperCase());
        
        int color;
        int bgColor;
        if ("Closed".equalsIgnoreCase(status)) {
            color = androidx.core.content.ContextCompat.getColor(context, R.color.colorError);
            bgColor = androidx.core.content.ContextCompat.getColor(context, R.color.divider); 
        } else if ("Full".equalsIgnoreCase(status)) {
            color = androidx.core.content.ContextCompat.getColor(context, R.color.colorWarning);
            bgColor = androidx.core.content.ContextCompat.getColor(context, R.color.colorLightBackground);
        } else {
            color = androidx.core.content.ContextCompat.getColor(context, R.color.colorPrimary);
            bgColor = androidx.core.content.ContextCompat.getColor(context, R.color.colorPrimaryLight);
        }
        
        holder.tvStatus.setTextColor(color);
        holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
        
        // Use restaurant name as category for now or a fixed label
        holder.tvCategory.setText("Highly Rated");

        String imageUrl = r.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("http")) {
                Glide.with(context.getApplicationContext()).load(imageUrl)
                        .placeholder(R.mipmap.dine_spot_logo)
                        .into(holder.ivImage);
            } else {
                try {
                    String base64 = imageUrl.contains(",") ? imageUrl.split(",")[1] : imageUrl;
                    byte[] bytes = android.util.Base64.decode(base64.trim(), android.util.Base64.DEFAULT);
                    Glide.with(context.getApplicationContext()).asBitmap().load(bytes).into(holder.ivImage);
                } catch (Exception e) {
                    holder.ivImage.setImageResource(R.mipmap.dine_spot_logo);
                }
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserRestaurantDetailsActivity.class);
            intent.putExtra("restaurant_id", r.getRestaurantId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return nearbyRestaurants.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvCategory, tvRating, tvStatus;
        ViewHolder(View v) {
            super(v);
            ivImage = v.findViewById(R.id.iv_nearby_image);
            tvName = v.findViewById(R.id.tv_nearby_name);
            tvCategory = v.findViewById(R.id.tv_nearby_category);
            tvRating = v.findViewById(R.id.tv_nearby_rating);
            tvStatus = v.findViewById(R.id.tv_nearby_status);
        }
    }
}
