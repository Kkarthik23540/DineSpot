package com.example.dinespot;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class AdminDashboardActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvRestaurantCount, tvBookingCount, tvNoBookingsMessage;
    private RecyclerView recyclerViewBookings;
    private BookingAdapter bookingAdapter;
    private List<Booking> bookingList = new ArrayList<>();
    private FirebaseFirestore db;
    private Spinner spinnerSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SpannableString title = new SpannableString("Admin Dashboard");
        title.setSpan(new ForegroundColorSpan(Color.WHITE), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        progressBar = findViewById(R.id.progressBar);
        tvRestaurantCount = findViewById(R.id.tvRestaurantCount);
        tvBookingCount = findViewById(R.id.tvBookingCount);
        tvNoBookingsMessage = findViewById(R.id.tvNoBookingsMessage);
        recyclerViewBookings = findViewById(R.id.recyclerViewBookings);
        spinnerSort = findViewById(R.id.spinnerSort);

        recyclerViewBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingAdapter(bookingList);
        recyclerViewBookings.setAdapter(bookingAdapter);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedOption = parentView.getItemAtPosition(position).toString();
                handleSortSelection(selectedOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        db = FirebaseFirestore.getInstance();

        progressBar.setVisibility(View.VISIBLE);
        fetchDashboardData();
    }

    private void fetchDashboardData() {
        db.collection("restaurants").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvRestaurantCount.setText("Total Restaurants: " + queryDocumentSnapshots.size());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch restaurant data.", Toast.LENGTH_SHORT).show());

        db.collection("bookings").addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Failed to fetch booking data.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (queryDocumentSnapshots != null) {
                int bookingCount = queryDocumentSnapshots.size();
                tvBookingCount.setText("Total Bookings: " + bookingCount);

                if (bookingCount == 0) {
                    tvNoBookingsMessage.setVisibility(View.VISIBLE); // Show message if no bookings
                } else {
                    tvNoBookingsMessage.setVisibility(View.GONE); // Hide message if bookings exist
                }

                bookingList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String restaurantName = document.getString("restaurantName");
                    String dateTime = document.getString("dateTime");
                    String timeSlot = document.getString("timeSlot");
                    String userName = document.getString("userName"); // Fetch userName
                    String phoneNumber=document.getString("phoneNumber");

                    Log.d("AdminDashboard", "Restaurant Name: " + restaurantName);

                    if (restaurantName != null && !restaurantName.isEmpty()) {
                        Booking booking = new Booking(restaurantName, dateTime, timeSlot, userName,phoneNumber);
                        bookingList.add(booking);
                    }
                }

                bookingAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void handleSortSelection(String selectedOption) {
        if (selectedOption.equals("Old to New")) {
            sortBookingsOldToNew();
        } else if (selectedOption.equals("New to Old")) {
            sortBookingsNewToOld();
        }
    }

    private void sortBookingsOldToNew() {
        Collections.sort(bookingList, new Comparator<Booking>() {
            @Override
            public int compare(Booking b1, Booking b2) {
                return b1.getDateTime().compareTo(b2.getDateTime());
            }
        });
        bookingAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Sorted from Old to New", Toast.LENGTH_SHORT).show();
    }

    private void sortBookingsNewToOld() {
        Collections.sort(bookingList, new Comparator<Booking>() {
            @Override
            public int compare(Booking b1, Booking b2) {
                return b2.getDateTime().compareTo(b1.getDateTime());
            }
        });
        bookingAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Sorted from New to Old", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu for the toolbar
        getMenuInflater().inflate(R.menu.menu_admin_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            handleLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, BothLogin.class));
        finish();
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit");
        builder.setMessage("Are you sure you want to exit?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            finishAffinity();

        });

        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog exitDialog = builder.create();
        exitDialog.show();
    }
}
