package com.example.dinespot;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {
    private Context context;
    private List<Restaurant> restaurantList;
    public RestaurantAdapter(Context context, List<Restaurant> restaurantList) {
        this.context = context;
        this.restaurantList = restaurantList;
    }
    @Override
    public RestaurantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.restaurant_item, parent, false);
        return new RestaurantViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(RestaurantViewHolder holder, int position) {
        Restaurant restaurant = restaurantList.get(position);
        holder.nameTextView.setText(restaurant.getName());
        holder.addressTextView.setText(restaurant.getAddress());
        holder.ratingTextView.setText("Rating: " + restaurant.getRating());
        holder.tablesTextView.setText("Tables Available: " + restaurant.getNoOfTables());
        String imageUrl = restaurant.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl)
                    .placeholder(R.color.white)
                    .error(android.R.drawable.alert_light_frame)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.color.white);
        }
        if (restaurant.getNoOfTables() == 0) {
            holder.itemView.setClickable(false);

            holder.itemView.setAlpha(0.5f);
        } else {
            holder.itemView.setClickable(true);
            holder.itemView.setAlpha(1.0f);
        }
        holder.itemView.setOnClickListener(v -> {
            if (restaurant.getNoOfTables() > 0) {
                Intent intent = new Intent(context, RestaurantDetailsActivity.class);
                intent.putExtra("restaurant_id", restaurant.getRestaurantId());
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No tables available at "+restaurant.getName()+" this restaurant", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public int getItemCount() {
        return restaurantList.size();
    }
    public static class RestaurantViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView, addressTextView, ratingTextView, tablesTextView;
        public ImageView imageView;
        public RestaurantViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.restaurant_name);
            addressTextView = itemView.findViewById(R.id.restaurant_address);
            ratingTextView = itemView.findViewById(R.id.restaurant_rating);
            tablesTextView = itemView.findViewById(R.id.restaurant_tables);
            imageView = itemView.findViewById(R.id.restaurant_image);
        }
    }

}
