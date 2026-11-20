package com.example.dinespot;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class UserCategoryAdapter extends RecyclerView.Adapter<UserCategoryAdapter.ViewHolder> {

    private List<String> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0; // "All" selected by default

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    public UserCategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = categories.get(position);
        holder.tvCategory.setText(category);

        boolean isSelected = selectedPosition == position;
        if (isSelected) {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
            holder.tvCategory.setTextColor(Color.WHITE);
        } else {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surface));
            holder.tvCategory.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.textPrimary));
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        MaterialCardView card;
        ViewHolder(View v) {
            super(v);
            tvCategory = v.findViewById(R.id.tvCategoryName);
            card = v.findViewById(R.id.categoryCard);
        }
    }
}