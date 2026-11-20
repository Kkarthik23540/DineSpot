package com.example.dinespot;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserCartAdapter extends RecyclerView.Adapter<UserCartAdapter.CartViewHolder> {

    private Context context;
    private List<CommonFoodItem> cartItems;

    public UserCartAdapter(Context context, List<CommonFoodItem> cartItems) {
        this.context = context;
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CommonFoodItem item = cartItems.get(position);
        holder.foodName.setText(item.getName());
        holder.foodPrice.setText("₹" + (item.getPrice() * item.getQuantity()));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        holder.btnPlus.setOnClickListener(v -> updateQuantity(item, 1));
        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                updateQuantity(item, -1);
            } else {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Remove Item")
                        .setMessage("Do you want to remove " + item.getName() + " from cart?")
                        .setPositiveButton("Remove", (d, w) -> removeCartItem(item))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        holder.btnRemove.setOnClickListener(v -> removeCartItem(item));

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("http")) {
                Glide.with(context).load(imageUrl).placeholder(R.drawable.baseline_restaurant_menu_24).into(holder.itemImage);
            } else {
                try {
                    byte[] bytes = Base64.decode(imageUrl, Base64.DEFAULT);
                    Glide.with(context).load(bytes).placeholder(R.drawable.baseline_restaurant_menu_24).into(holder.itemImage);
                } catch (Exception e) {
                    holder.itemImage.setImageResource(R.drawable.baseline_restaurant_menu_24);
                }
            }
        }
    }

    private void updateQuantity(CommonFoodItem item, int delta) {
        int newQty = item.getQuantity() + delta;
        if (newQty < 1 || newQty > 20) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .whereEqualTo("email", user.getEmail())
                    .get()
                    .addOnSuccessListener(snaps -> {
                        if (!snaps.isEmpty()) {
                            snaps.getDocuments().get(0).getReference()
                                    .collection("cart").document(item.getId())
                                    .update("quantity", newQty);
                        }
                    });
        }
    }

    private void removeCartItem(CommonFoodItem item) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (context instanceof CommonBaseActivity) {
                ((CommonBaseActivity) context).vibrateAction();
            }
            FirebaseFirestore.getInstance().collection("users")
                    .whereEqualTo("email", user.getEmail())
                    .get()
                    .addOnSuccessListener(snaps -> {
                        if (!snaps.isEmpty()) {
                            snaps.getDocuments().get(0).getReference()
                                    .collection("cart").document(item.getId())
                                    .delete();
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView foodName, foodPrice, tvQuantity;
        ImageView itemImage;
        ImageButton btnPlus, btnMinus, btnRemove;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            foodName = itemView.findViewById(R.id.foodName);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            itemImage = itemView.findViewById(R.id.cartItemImage);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
