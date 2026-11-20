package com.example.dinespot;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Map;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.BookingViewHolder> {

    private final List<CommonBooking> bookingList;
    private final OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onStatusChanged(CommonBooking booking, String newStatus);
        void onOrderUpdate(CommonBooking booking, String orderStatus);
        void onDelete(CommonBooking booking);
    }

    public AdminBookingAdapter(List<CommonBooking> bookingList, OnBookingActionListener listener) {
        this.bookingList = bookingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        CommonBooking booking = bookingList.get(position);
        Context context = holder.itemView.getContext();
        
        holder.tvRestaurantName.setText(booking.getRestaurantName());
        holder.tvDateTime.setText("Date: " + booking.getDateTime());
        holder.tvTimeSlot.setText("Time: " + booking.getTimeSlot());
        holder.tvUserName.setText("Customer: " + booking.getUserName());
        holder.tvPhoneNumber.setText("Phone: " + booking.getPhoneNumber());
        
        String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
        holder.tvStatus.setText("Status: " + status.toUpperCase());

        // SHOW ORDERED ITEMS
        List<Map<String, Object>> items = booking.getOrderedItems();
        if (items != null && !items.isEmpty()) {
            StringBuilder sb = new StringBuilder("Ordered Items:\n");
            for (Map<String, Object> item : items) {
                sb.append("- ").append(item.get("name")).append(" (₹").append(item.get("price")).append(")\n");
            }
            holder.tvOrderedItems.setVisibility(View.VISIBLE);
            holder.tvOrderedItems.setText(sb.toString().trim());
        } else {
            holder.tvOrderedItems.setVisibility(View.GONE);
        }

        if (status.equalsIgnoreCase("Accepted")) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnComplete.setVisibility(View.VISIBLE);
        } else if (status.equalsIgnoreCase("Rejected")) {
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnComplete.setVisibility(View.GONE);
        } else if (status.equalsIgnoreCase("Completed")) {
            holder.tvStatus.setTextColor(Color.parseColor("#757575"));
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnComplete.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#118D9D"));
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnComplete.setVisibility(View.GONE);
        }

        if (booking.getSpecialRequests() != null && !booking.getSpecialRequests().isEmpty()) {
            holder.tvSpecialRequests.setVisibility(View.VISIBLE);
            holder.tvSpecialRequests.setText("Notes: " + booking.getSpecialRequests());
        } else {
            holder.tvSpecialRequests.setVisibility(View.GONE);
        }

        if (!"none".equals(booking.getOrderStatus())) {
            holder.tvOrderStatus.setVisibility(View.VISIBLE);
            holder.tvOrderStatus.setText("Cooking: " + booking.getOrderStatus());
        } else {
            holder.tvOrderStatus.setVisibility(View.GONE);
        }

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(booking));
        
        holder.tvOrderStatus.setOnClickListener(v -> showOrderUpdateMenu(v, booking));
        
        holder.btnAccept.setOnClickListener(v -> listener.onStatusChanged(booking, "Accepted"));
        holder.btnReject.setOnClickListener(v -> listener.onStatusChanged(booking, "Rejected"));
        holder.btnComplete.setOnClickListener(v -> listener.onStatusChanged(booking, "Completed"));
    }

    private void showOrderUpdateMenu(View v, CommonBooking booking) {
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.getMenu().add("Received");
        popup.getMenu().add("Preparing");
        popup.getMenu().add("Served");
        popup.setOnMenuItemClickListener(item -> {
            listener.onOrderUpdate(booking, item.getTitle().toString());
            return true;
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRestaurantName, tvDateTime, tvTimeSlot, tvUserName, tvPhoneNumber, tvStatus, tvSpecialRequests, tvOrderStatus, tvOrderedItems;
        MaterialButton btnAccept, btnReject, btnComplete;
        ImageButton btnDelete;
        View layoutActions;

        public BookingViewHolder(View itemView) {
            super(itemView);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSpecialRequests = itemView.findViewById(R.id.tvSpecialRequests);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderedItems = itemView.findViewById(R.id.tvOrderedItems);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnComplete = itemView.findViewById(R.id.btnComplete);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            layoutActions = itemView.findViewById(R.id.layoutActions);
        }
    }
}
