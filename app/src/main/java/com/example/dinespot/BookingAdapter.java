package com.example.dinespot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private final List<Booking> bookingList;

    public BookingAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        holder.tvRestaurantName.setText(booking.getRestaurantName());
        holder.tvDateTime.setText(booking.getDateTime());
        holder.tvTimeSlot.setText(booking.getTimeSlot());
        holder.tvUserName.setText(booking.getUserName());
        holder.tvPhoneNumber.setText(booking.getPhoneNumber());
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRestaurantName, tvDateTime, tvTimeSlot, tvUserName, tvPhoneNumber;

        public BookingViewHolder(View itemView) {
            super(itemView);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
        }
    }
}
