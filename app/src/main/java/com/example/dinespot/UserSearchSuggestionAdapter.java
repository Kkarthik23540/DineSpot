package com.example.dinespot;

import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserSearchSuggestionAdapter extends RecyclerView.Adapter<UserSearchSuggestionAdapter.ViewHolder> {

    private List<Address> suggestions;
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(Address address);
    }

    public UserSearchSuggestionAdapter(List<Address> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    public void updateData(List<Address> newSuggestions) {
        this.suggestions = newSuggestions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Address address = suggestions.get(position);
        holder.textView.setText(address.getAddressLine(0));
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(address));
    }

    @Override
    public int getItemCount() {
        return suggestions != null ? suggestions.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
            textView.setPadding(32, 16, 32, 16);
            textView.setTextSize(14);
        }
    }
}
