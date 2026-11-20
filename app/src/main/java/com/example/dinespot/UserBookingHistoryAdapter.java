package com.example.dinespot;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class UserBookingHistoryAdapter extends RecyclerView.Adapter<UserBookingHistoryAdapter.ViewHolder> {

    private List<CommonBooking> bookingList;
    private OnBookingCancelListener cancelListener;

    public interface OnBookingCancelListener {
        void onCancel(CommonBooking booking);
    }

    public UserBookingHistoryAdapter(List<CommonBooking> bookingList, OnBookingCancelListener cancelListener) {
        this.bookingList = bookingList;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item_booking_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommonAnimationHelper.animateListEntry(holder.itemView, position);
        
        CommonBooking booking = bookingList.get(position);
        holder.tvResName.setText(booking.getRestaurantName());
        holder.tvDateTime.setText(booking.getDateTime());
        
        String tableTypeStr = booking.getTableType() != null ? booking.getTableType() : "Standard";
        String typeLabel = getFriendlyTableType(tableTypeStr);
        holder.tvTableDetails.setText(typeLabel + " Table • " + booking.getTimeSlot());
        
        String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
        holder.tvStatus.setText(status);
        
        // Dynamic Status Colors
        int statusColor;
        if ("Accepted".equalsIgnoreCase(status)) {
            statusColor = Color.parseColor("#4CAF50");
            holder.btnCancel.setVisibility(View.GONE);
        } else if ("Rejected".equalsIgnoreCase(status)) {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorError);
            holder.btnCancel.setVisibility(View.GONE);
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.textSecondary);
            holder.btnCancel.setVisibility(View.GONE);
        } else if ("Completed".equalsIgnoreCase(status)) {
            statusColor = Color.parseColor("#757575");
            holder.btnCancel.setVisibility(View.GONE);
        } else {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWarning);
            holder.btnCancel.setVisibility(View.VISIBLE);
        }
        holder.cardStatus.setCardBackgroundColor(statusColor);

        // ORDER DETAILS UI
        boolean hasOrder = booking.getOrderStatus() != null && !"none".equalsIgnoreCase(booking.getOrderStatus());
        List<Map<String, Object>> items = booking.getOrderedItems();
        boolean hasItems = items != null && !items.isEmpty();

        if (hasOrder || hasItems) {
            holder.layoutOrderDetails.setVisibility(View.VISIBLE);
            String ordStatus = booking.getOrderStatus();
            if (hasOrder) {
                holder.tvOrderStatusBadge.setText(ordStatus);
                // Dynamic Order Status Colors
                if ("Served".equalsIgnoreCase(ordStatus)) {
                    holder.tvOrderStatusBadge.setTextColor(Color.parseColor("#4CAF50"));
                } else if ("Preparing".equalsIgnoreCase(ordStatus)) {
                    holder.tvOrderStatusBadge.setTextColor(Color.parseColor("#FF9800"));
                } else {
                    holder.tvOrderStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
                }
            } else {
                holder.tvOrderStatusBadge.setText("No Order");
                holder.tvOrderStatusBadge.setTextColor(Color.GRAY);
            }

            if (hasItems) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> item : items) {
                    sb.append("• ").append(item.get("name")).append(" (₹").append(item.get("price")).append(")\n");
                }
                holder.tvFoodList.setText(sb.toString().trim());
                holder.tvFoodList.setVisibility(View.VISIBLE);
            } else {
                holder.tvFoodList.setVisibility(View.GONE);
            }
        } else {
            holder.layoutOrderDetails.setVisibility(View.GONE);
        }

        holder.btnCancel.setOnClickListener(v -> {
            if (cancelListener != null) cancelListener.onCancel(booking);
        });
    }

    private String getFriendlyTableType(String type) {
        switch (type) {
            case "2": return "Couple";
            case "4": return "Family";
            case "6": return "Big Family";
            case "10": return "Party Group";
            default: return "Reserved";
        }
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvResName, tvStatus, tvDateTime, tvTableDetails, tvOrderStatusBadge, tvFoodList;
        com.google.android.material.card.MaterialCardView cardStatus;
        View btnCancel, layoutOrderDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvResName = itemView.findViewById(R.id.tv_res_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvOrderStatusBadge = itemView.findViewById(R.id.tv_order_status_badge);
            tvFoodList = itemView.findViewById(R.id.tv_food_list);
            cardStatus = itemView.findViewById(R.id.card_status);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            tvTableDetails = itemView.findViewById(R.id.tv_table_details);
            btnCancel = itemView.findViewById(R.id.btn_cancel_booking);
            layoutOrderDetails = itemView.findViewById(R.id.layout_order_details);
        }
    }
}
