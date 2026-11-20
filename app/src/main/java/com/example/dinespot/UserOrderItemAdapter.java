package com.example.dinespot;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class UserOrderItemAdapter extends RecyclerView.Adapter<UserOrderItemAdapter.OrderItemViewHolder> {

    private Context context;
    private List<CommonFoodItem> orderItems;

    public UserOrderItemAdapter(Context context, List<CommonFoodItem> orderItems) {
        this.context = context;
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item_order, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        CommonFoodItem foodItem = orderItems.get(position);

        holder.foodNameText.setText(foodItem.getName());
        holder.foodPriceText.setText("₹" + foodItem.getPrice());

        String imageUrl = foodItem.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("http")) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.baseline_restaurant_menu_24)
                        .error(R.drawable.baseline_restaurant_menu_24)
                        .into(holder.foodImageView);
            } else {
                try {
                    byte[] imageBytes = Base64.decode(imageUrl, Base64.DEFAULT);
                    Glide.with(context)
                            .load(imageBytes)
                            .placeholder(R.drawable.baseline_restaurant_menu_24)
                            .error(R.drawable.baseline_restaurant_menu_24)
                            .into(holder.foodImageView);
                } catch (Exception e) {
                    holder.foodImageView.setImageResource(R.drawable.baseline_restaurant_menu_24);
                }
            }
        } else {
            holder.foodImageView.setImageResource(R.drawable.baseline_restaurant_menu_24);
        }
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        TextView foodNameText, foodPriceText;
        ImageView foodImageView;

        public OrderItemViewHolder(View itemView) {
            super(itemView);
            foodNameText = itemView.findViewById(R.id.foodName);
            foodPriceText = itemView.findViewById(R.id.foodPrice);
            foodImageView = itemView.findViewById(R.id.foodImage);
        }
    }
}
