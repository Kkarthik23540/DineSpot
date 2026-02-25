package com.example.dinespot;

import android.content.Context;
import android.content.Intent;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private Context context;
    private List<FoodItem> foodItemList;
    private FirebaseFirestore firestore;

    public FoodAdapter(Context context, List<FoodItem> foodItemList) {
        this.context = context;
        this.foodItemList = foodItemList;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.food_item, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem foodItem = foodItemList.get(position);
        holder.foodName.setText(foodItem.getName());
        holder.foodPrice.setText("₹" + foodItem.getPrice());

        Glide.with(context)
                .load(foodItem.getImageUrl())
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(holder.foodImage);

        holder.addButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(foodItem.getName() + " added to cart")
                    .setPositiveButton("Go to Cart", (dialog, id) -> {
                        addItemToCart(foodItem);
                        dialog.dismiss();
                        Intent intent = new Intent(context, CartActivityMenu.class);
                        context.startActivity(intent);
                    })
                    .setNegativeButton("Continue", (dialog, id) -> {
                        addItemToCart(foodItem);
                        dialog.dismiss();
                        Toast.makeText(context, foodItem.getName() + " added to cart", Toast.LENGTH_SHORT).show();
                    });
            builder.create().show();
        });
    }

    @Override
    public int getItemCount() {
        return foodItemList.size();
    }

    private void addItemToCart(FoodItem foodItem) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .add(foodItem)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(context, foodItem.getName() + " added to cart", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error adding item to cart", Toast.LENGTH_SHORT).show();
                });
    }

    public void updateFoodItemList(List<FoodItem> filteredList) {
        this.foodItemList = filteredList;
        notifyDataSetChanged();
    }

    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView foodName, foodPrice;
        ImageView foodImage;
        Button addButton;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            foodName = itemView.findViewById(R.id.foodName);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            foodImage = itemView.findViewById(R.id.foodImage);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }
}
