package com.example.dinespot;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<FoodItem> cartItems;
    private List<FoodItem> selectedItems;
    private OnRemoveItemListener onRemoveItemListener;
    private OnItemLongClickListener onItemLongClickListener;

    public CartAdapter(Context context, List<FoodItem> cartItems, OnItemLongClickListener onItemLongClickListener, OnRemoveItemListener onRemoveItemListener) {
        this.context = context;
        this.cartItems = cartItems;
        this.selectedItems = new ArrayList<>();
        this.onRemoveItemListener = onRemoveItemListener;
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        FoodItem foodItem = cartItems.get(position);
        holder.foodName.setText(foodItem.getName());
        holder.foodPrice.setText("₹" + foodItem.getPrice());

        if (selectedItems.contains(foodItem)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.black));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.removeButton.setOnClickListener(v -> {
            if (selectedItems.isEmpty()) {
                onRemoveItemListener.onRemoveItem(foodItem);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            onItemLongClickListener.onItemLongClick(foodItem);
            toggleSelection(foodItem);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (!selectedItems.isEmpty()) {
                toggleSelection(foodItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void setSelectedItems(List<FoodItem> selectedItems) {
        this.selectedItems = selectedItems;
        notifyDataSetChanged();
    }

    private void toggleSelection(FoodItem foodItem) {
        if (selectedItems.contains(foodItem)) {
            selectedItems.remove(foodItem);
        } else {
            selectedItems.add(foodItem);
        }
        notifyDataSetChanged();
    }

    public List<FoodItem> getSelectedItems() {
        return selectedItems;
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView foodName, foodPrice;
        View removeButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            foodName = itemView.findViewById(R.id.foodName);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }

    public interface OnRemoveItemListener {
        void onRemoveItem(FoodItem foodItem);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(FoodItem foodItem);
    }
}
