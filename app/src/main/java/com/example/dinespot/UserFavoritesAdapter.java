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

public class UserFavoritesAdapter extends RecyclerView.Adapter<UserFavoritesAdapter.ViewHolder> {

    private Context context;
    private List<CommonRestaurant> favorites;

    public UserFavoritesAdapter(Context context, List<CommonRestaurant> favorites) {
        this.context = context;
        this.favorites = favorites;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.user_item_favorite, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommonRestaurant r = favorites.get(position);
        holder.tvName.setText(r.getName());
        float rating = r.getRating();
        if (rating > 0) {
            holder.tvRating.setText(String.format(java.util.Locale.getDefault(), "%.1f★", rating));
        } else {
            holder.tvRating.setText("New");
        }

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
        return favorites.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvRating;
        ViewHolder(View v) {
            super(v);
            ivImage = v.findViewById(R.id.iv_fav_image);
            tvName = v.findViewById(R.id.tv_fav_name);
            tvRating = v.findViewById(R.id.tv_fav_rating);
        }
    }
}