package com.example.dinespot;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class RestaurantsActivityMenu extends AppCompatActivity {

    private LinearLayout restaurantDetailsContainer;
    private FirebaseFirestore firestore;
    private EditText searchEditText;
    private List<DocumentSnapshot> allRestaurants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurants_menu);

        firestore = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nearby Restaurants");
        }

        restaurantDetailsContainer = findViewById(R.id.restaurant_details_container);
        searchEditText = findViewById(R.id.searchEditText);

        allRestaurants = new ArrayList<>();

        fetchRestaurantDetails();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRestaurants(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void fetchRestaurantDetails() {
        firestore.collection("restaurants")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();
                        allRestaurants.clear();
                        allRestaurants.addAll(querySnapshot.getDocuments());
                        displayRestaurants(allRestaurants);
                    }
                })
                .addOnFailureListener(e -> {
                });
    }

    private void displayRestaurants(List<DocumentSnapshot> restaurants) {
        restaurantDetailsContainer.removeAllViews();
        for (DocumentSnapshot document : restaurants) {
            addRestaurantToLayout(document);
        }
    }

    private void addRestaurantToLayout(DocumentSnapshot document) {
        View restaurantView = getLayoutInflater().inflate(R.layout.restaurant_lists, restaurantDetailsContainer, false);

        ImageView restaurantImage = restaurantView.findViewById(R.id.restaurant_image);
        TextView restaurantName = restaurantView.findViewById(R.id.restaurant_name);
        TextView restaurantDescription = restaurantView.findViewById(R.id.restaurant_description);
        TextView restaurantStatus = restaurantView.findViewById(R.id.restaurant_status);
        TextView restaurantTimings = restaurantView.findViewById(R.id.restaurant_timings);

        restaurantName.setText(document.getString("name"));
        restaurantDescription.setText("Description: " + document.getString("description"));
        restaurantStatus.setText("Status: " + document.getString("status"));
        restaurantTimings.setText("Opening Hours: " + document.getString("openingHours"));

        String imageUrl = document.getString("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(restaurantImage);
        }

        restaurantDetailsContainer.addView(restaurantView);
    }

    private void filterRestaurants(String query) {
        if (query.isEmpty()) {
            displayRestaurants(allRestaurants);
            return;
        }

        List<DocumentSnapshot> filteredRestaurants = new ArrayList<>();
        for (DocumentSnapshot document : allRestaurants) {
            String name = document.getString("name");
            if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                filteredRestaurants.add(document);
            }
        }

        displayRestaurants(filteredRestaurants);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
