package com.example.dinespot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.OrderItemViewHolder> {

    private Context context;
    private List<FoodItem> orderItems;

    public OrderItemsAdapter(Context context, List<FoodItem> orderItems) {
        this.context = context;
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.order_item, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        FoodItem foodItem = orderItems.get(position);

        holder.foodNameText.setText(foodItem.getName());
        holder.foodPriceText.setText("₹" + foodItem.getPrice());

        Glide.with(context)
                .load(foodItem.getImageUrl())
                .placeholder(R.drawable.baseline_restaurant_menu_24)
                .into(holder.foodImageView);
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
