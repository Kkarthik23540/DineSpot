package com.example.dinespot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RestaurantDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private RecyclerView foodItemsRecyclerView;
    private FoodAdapter foodAdapter;
    private List<FoodItem> foodItemList;
    private EditText searchBar;
    private String restaurantId;
    private boolean isTableBooked = false;
    private String bookedRestaurantId;
    private TextView restaurantNameTextView, restaurantRatingTextView, restaurantTablesTextView;
    private ImageView restaurantImageView;
    private GeoPoint restaurantLocation;
    private Button selectTableButton;
    private Button cancelBookingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_details);

        firestore = FirebaseFirestore.getInstance();

        restaurantId = getIntent().getStringExtra("restaurant_id");

        foodItemsRecyclerView = findViewById(R.id.food_items_recycler_view);
        searchBar = findViewById(R.id.search_bar);

        restaurantNameTextView = findViewById(R.id.restaurant_name);
        restaurantRatingTextView = findViewById(R.id.restaurant_rating);
        restaurantTablesTextView = findViewById(R.id.restaurant_tables);
        restaurantImageView = findViewById(R.id.restaurant_image);
        selectTableButton = findViewById(R.id.select_table_button);
        cancelBookingButton = findViewById(R.id.cancel_booking_button);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Restaurants");
        }

        foodItemList = new ArrayList<>();
        foodAdapter = new FoodAdapter(this, foodItemList);
        foodItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        foodItemsRecyclerView.setAdapter(foodAdapter);

        cancelBookingButton.setVisibility(View.GONE);

        checkTableBookingStatus();

        loadRestaurantDetails();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                filterFoodItems(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        selectTableButton.setOnClickListener(v -> {
            if (isTableBooked) {
                showCancelBookingDialog();
            } else {
                showTimeSlotSelectionDialog();
            }
        });

        cancelBookingButton.setOnClickListener(v -> {
            if (isTableBooked) {
                cancelTableBooking();
            } else {
                Toast.makeText(RestaurantDetailsActivity.this, "No table is booked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRestaurantDetails() {
        firestore.collection("restaurants")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Restaurant restaurant = documentSnapshot.toObject(Restaurant.class);
                        if (restaurant != null) {
                            restaurantNameTextView.setText(restaurant.getName());
                            restaurantRatingTextView.setText("Rating: " + restaurant.getRating());

                            int noOfTables = restaurant.getNoOfTables();
                            restaurantTablesTextView.setText("Tables Available: " + noOfTables);

                            String imageUrl = restaurant.getImageUrl();
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(this).load(imageUrl).into(restaurantImageView);
                            }

                            restaurantLocation = restaurant.getLocation();
                            findViewById(R.id.location_button).setOnClickListener(v -> openLocationInMaps());

                            if (isTableBooked && restaurantId.equals(bookedRestaurantId)) {
                                loadFoodItems();
                            } else {
                                foodItemsRecyclerView.setVisibility(View.GONE);
                                selectTableButton.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading restaurant details", e));
    }

    private void loadFoodItems() {
        firestore.collection("restaurants")
                .document(restaurantId)
                .collection("foodItems")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    foodItemList.clear();
                    for (var document : queryDocumentSnapshots) {
                        FoodItem foodItem = document.toObject(FoodItem.class);
                        foodItemList.add(foodItem);
                    }
                    foodAdapter.notifyDataSetChanged();
                    foodItemsRecyclerView.setVisibility(View.VISIBLE);
                    selectTableButton.setVisibility(View.GONE);
                    cancelBookingButton.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading food items", e));
    }

    private void filterFoodItems(String query) {
        List<FoodItem> filteredList = new ArrayList<>();
        for (FoodItem foodItem : foodItemList) {
            if (foodItem.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(foodItem);
            }
        }
        foodAdapter.updateFoodItemList(filteredList);
    }

    private void showTimeSlotSelectionDialog() {
        String[] timeSlots = {"12:00 PM", "02:00 PM", "04:00 PM", "06:00 PM", "08:00 PM"};
        new AlertDialog.Builder(this)
                .setTitle("Select a Time Slot")
                .setItems(timeSlots, (dialog, which) -> {
                    String selectedTimeSlot = timeSlots[which];
                    bookTable(selectedTimeSlot);
                })
                .setCancelable(true)
                .show();
    }

    private void bookTable(String selectedTimeSlot) {
        if (isTableBooked) {
            restoreTableCountBeforeBooking();
        } else {
            proceedWithBooking(selectedTimeSlot);
        }
    }

    private void restoreTableCountBeforeBooking() {
        DocumentReference previousRestaurantRef = firestore.collection("restaurants").document(bookedRestaurantId);

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
                    Restaurant previousRestaurant = transaction.get(previousRestaurantRef).toObject(Restaurant.class);
                    if (previousRestaurant != null) {
                        int noOfTables = previousRestaurant.getNoOfTables();
                        transaction.update(previousRestaurantRef, "noOfTables", noOfTables + 1);
                    }
                    return null;
                }).addOnSuccessListener(aVoid -> proceedWithBooking("No time selected"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error restoring table count", e));
    }

    private void proceedWithBooking(String selectedTimeSlot) {
        DocumentReference restaurantRef = firestore.collection("restaurants").document(restaurantId);

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            Restaurant restaurant = transaction.get(restaurantRef).toObject(Restaurant.class);
            if (restaurant != null) {
                int noOfTables = restaurant.getNoOfTables();
                if (noOfTables > 0) {
                    transaction.update(restaurantRef, "noOfTables", noOfTables - 1);
                    isTableBooked = true;
                    bookedRestaurantId = restaurantId;
                    saveTableBookingStatus(true);
                    saveBookedRestaurantId(restaurantId);
                    storeBookingDetails(restaurant.getName(), selectedTimeSlot);
                    runOnUiThread(() -> {
                        restaurantTablesTextView.setText("Tables Available: " + (noOfTables - 1));
                        Toast.makeText(this, "Table booked successfully for " + selectedTimeSlot, Toast.LENGTH_SHORT).show();
                        loadFoodItems();
                    });
                } else {
                    Toast.makeText(RestaurantDetailsActivity.this, "No tables available", Toast.LENGTH_SHORT).show();
                }
            }
            return null;
        }).addOnFailureListener(e -> Log.e("Firestore", "Error booking table", e));
    }

    private void storeBookingDetails(String restaurantName, String selectedTimeSlot) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");
                        String phoneNumber = documentSnapshot.getString("phone");

                        Map<String, Object> bookingDetails = new HashMap<>();
                        bookingDetails.put("restaurantName", restaurantName);
                        bookingDetails.put("userName", userName);
                        bookingDetails.put("dateTime", dateTime);
                        bookingDetails.put("timeSlot", selectedTimeSlot);
                        bookingDetails.put("phoneNumber", phoneNumber);

                        firestore.collection("bookings")
                                .add(bookingDetails)
                                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Booking details saved"))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error saving booking details", e));
                    } else {
                        Log.e("Firestore", "User document not found");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching user details", e));
    }


    private void cancelTableBooking() {
        DocumentReference restaurantRef = firestore.collection("restaurants").document(bookedRestaurantId);

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            Restaurant restaurant = transaction.get(restaurantRef).toObject(Restaurant.class);
            if (restaurant != null) {
                int noOfTables = restaurant.getNoOfTables();
                transaction.update(restaurantRef, "noOfTables", noOfTables + 1);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            removeBookingDetails();
            isTableBooked = false;
            saveTableBookingStatus(false);
            Toast.makeText(this, "Booking canceled", Toast.LENGTH_SHORT).show();
            loadRestaurantDetails();
            cancelBookingButton.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Error canceling table booking", e);
            Toast.makeText(RestaurantDetailsActivity.this, "Error canceling booking", Toast.LENGTH_SHORT).show();
        });
    }


    private void removeBookingDetails() {
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();

        String bookedRestaurantId = "your_booked_restaurant_id";

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("bookings")
                .whereEqualTo("userName", userName)
                .whereEqualTo("restaurantId", bookedRestaurantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (var document : queryDocumentSnapshots) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "Booking details deleted");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "Error deleting booking details", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching booking details for deletion", e);
                });
    }


    private void checkTableBookingStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences("DineSpotPreferences", MODE_PRIVATE);
        isTableBooked = sharedPreferences.getBoolean("isTableBooked", false);
        bookedRestaurantId = sharedPreferences.getString("bookedRestaurantId", null);
    }

    private void saveTableBookingStatus(boolean isBooked) {
        SharedPreferences sharedPreferences = getSharedPreferences("DineSpotPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isTableBooked", isBooked);
        editor.apply();
    }

    private void saveBookedRestaurantId(String restaurantId) {
        SharedPreferences sharedPreferences = getSharedPreferences("DineSpotPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("bookedRestaurantId", restaurantId);
        editor.apply();
    }
    private void showCancelBookingDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel your booking?")
                .setPositiveButton("Stay at Current Restaurant", (dialog, which) -> {
                })
                .setNegativeButton("Cancel Booking at previous restaurant", (dialog, which) -> cancelTableBooking())
                .setCancelable(true)
                .show();
    }

    private void openLocationInMaps() {
        if (restaurantLocation != null) {
            double latitude = restaurantLocation.getLatitude();
            double longitude = restaurantLocation.getLongitude();
            Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + restaurantNameTextView.getText().toString());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Location data not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
