package com.example.dinespot;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private EditText searchBar;
    private RecyclerView recyclerViewRestaurants;
    private RestaurantAdapter restaurantAdapter;
    private final List<Restaurant> restaurantList = new ArrayList<>();
    private final List<Restaurant> filteredList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private final List<Integer> selectedTableOptions = new ArrayList<>();
    private String query;
    private ProgressBar loadingProgressBar;
    private TextView userInitialText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchBar = findViewById(R.id.searchBar);
        recyclerViewRestaurants = findViewById(R.id.recyclerViewRestaurants);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        recyclerViewRestaurants.setLayoutManager(new GridLayoutManager(this, 1));

        userInitialText = findViewById(R.id.userInitial);

        loadingProgressBar.setVisibility(View.VISIBLE);

        loadRestaurantsFromFirebase();


        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                filterRestaurants(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        Spinner ratingSpinner = findViewById(R.id.ratingSpinner);
        List<String> ratings = new ArrayList<>();
        ratings.add("Select Rating");
        ratings.add("5 stars");
        ratings.add("4 stars and above");
        ratings.add("3 stars and above");
        ratings.add("2 stars and above");
        ArrayAdapter<String> ratingAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ratings);
        ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ratingSpinner.setAdapter(ratingAdapter);

        ratingSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0) {
                    filterByRating(6 - position);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        findViewById(R.id.tablesButton).setOnClickListener(v -> showTableSelectionDialog());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_home) {
                Toast.makeText(HomeActivity.this, "Home Selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_restaurants) {
                Toast.makeText(HomeActivity.this, "Restaurants Selected", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(HomeActivity.this, RestaurantsActivityMenu.class));
                return true;
            } else if (itemId == R.id.menu_cart) {
                Toast.makeText(HomeActivity.this, "Cart Selected", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(HomeActivity.this, CartActivityMenu.class));
                return true;
            } else if (itemId == R.id.menu_profile) {
                Toast.makeText(HomeActivity.this, "Profile Selected", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(HomeActivity.this, ProfileActivityMenu.class));
                return true;
            }
            return false;
        });

        Menu menu = bottomNavigationView.getMenu();
        menu.add(Menu.NONE, R.id.menu_home, 0, "Home").setIcon(R.drawable.baseline_home_24);
        menu.add(Menu.NONE, R.id.menu_restaurants, 1, "Restaurants").setIcon(R.drawable.baseline_restaurant_menu_24);
        menu.add(Menu.NONE, R.id.menu_cart, 2, "Cart").setIcon(R.drawable.baseline_shopping_cart_24);
        menu.add(Menu.NONE, R.id.menu_profile, 3, "Profile").setIcon(R.drawable.baseline_account_circle_24);

        ImageButton sortButton = findViewById(R.id.sortButton);
        sortButton.setOnClickListener(v -> showSortOptionsDialog());

        setUserInitialInToolbar();
    }

    private void setUserInitialInToolbar() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        CollectionReference usersRef = firestore.collection("users");
        usersRef.document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String fullName = document.getString("name");
                    if (fullName != null && !fullName.isEmpty()) {
                        String initial = fullName.substring(0, 1).toUpperCase();
                        userInitialText.setText(initial);
                        userInitialText.setOnClickListener(v -> {
                            // Navigate to ProfileActivityMenu when the initial is clicked
                            Intent intent = new Intent(HomeActivity.this, ProfileActivityMenu.class);
                            startActivity(intent);
                        });
                    }
                }
            } else {
                Toast.makeText(HomeActivity.this, "Failed to fetch user name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRestaurantsFromFirebase() {
        CollectionReference restaurantRef = firestore.collection("restaurants");
        restaurantRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return;
            }
            if (snapshot != null) {
                restaurantList.clear();
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    Restaurant restaurant = document.toObject(Restaurant.class);
                    if (restaurant != null) {
                        restaurant.setRestaurantId(document.getId());
                        restaurantList.add(restaurant);
                    }
                }
                filteredList.clear();
                filteredList.addAll(restaurantList);
                restaurantAdapter = new RestaurantAdapter(HomeActivity.this, filteredList);
                recyclerViewRestaurants.setAdapter(restaurantAdapter);
                toggleLoading(false);
            }
        });
    }

    private void toggleLoading(boolean isLoading) {
        if (isLoading) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
        }
    }

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    private void filterRestaurants(String query) {
        this.query = query;
        filteredList.clear();

        if (selectedTableOptions.isEmpty()) {
            for (Restaurant restaurant : restaurantList) {
                if (restaurant.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(restaurant);
                }
            }
        } else {
            for (Restaurant restaurant : restaurantList) {
                if (restaurant.getName().toLowerCase().contains(query.toLowerCase()) &&
                        selectedTableOptions.contains(restaurant.getNoOfTables())) {
                    filteredList.add(restaurant);
                }
            }
        }

        if (restaurantAdapter != null) {
            restaurantAdapter.notifyDataSetChanged();
        }

        TextView noResultsText = findViewById(R.id.noResultsText);
        if (filteredList.isEmpty()) {
            noResultsText.setVisibility(View.VISIBLE);
            noResultsText.setText("No restaurants available for the selected filter. Please try again.");
        } else {
            noResultsText.setVisibility(View.GONE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterByRating(int ratingThreshold) {
        filteredList.clear();
        boolean resultsFound = false;

        for (Restaurant restaurant : restaurantList) {
            if (restaurant.getRating() >= ratingThreshold) {
                filteredList.add(restaurant);
                resultsFound = true;
            }
        }
        if (!resultsFound) {
            TextView noResultsText = findViewById(R.id.noResultsText);
            noResultsText.setVisibility(View.VISIBLE);
            noResultsText.setText("No restaurants match the selected rating.");
        } else {
            TextView noResultsText = findViewById(R.id.noResultsText);
            noResultsText.setVisibility(View.GONE);
            restaurantAdapter.notifyDataSetChanged();
        }
    }


    private void showSortOptionsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Sort by Name")
                .setItems(new String[]{"A-Z", "Z-A"}, (dialog, which) -> {
                    if (which == 0) {
                        sortRestaurantsAlphabetically(true);
                        Toast.makeText(this, "Restaurants sorted in A-Z order", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        sortRestaurantsAlphabetically(false);
                        Toast.makeText(this, "Restaurants sorted in Z-A order", Toast.LENGTH_SHORT).show();
                    }
                })
                .create()
                .show();
    }

    private void sortRestaurantsAlphabetically(boolean isAscending) {
        if (isAscending) {
            filteredList.sort((r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()));
        } else {
            filteredList.sort((r1, r2) -> r2.getName().compareToIgnoreCase(r1.getName()));
        }

        if (restaurantAdapter != null) {
            restaurantAdapter.notifyDataSetChanged();
        }
    }

    private void showTableSelectionDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select No of Tables");

        boolean[] checkedItems = new boolean[5];

        for (int i = 0; i < 5; i++) {
            int tableOption = 0;switch (i) {
                case 0: tableOption = 2;break;
                case 1: tableOption = 4;break;
                case 2: tableOption = 6;break;
                case 3: tableOption = 8;break;
                case 4: tableOption = 12;break;
            }
            checkedItems[i] = selectedTableOptions.contains(tableOption);
        }

        builder.setMultiChoiceItems(new String[]{"2 ", "4 ", "6 ", "8 ", "12"}, checkedItems,
                (dialog, which, isChecked) -> {
                    int tableOption = 0;
                    switch (which) {
                        case 0: tableOption = 2;break;
                        case 1: tableOption = 4;break;
                        case 2: tableOption = 6;break;
                        case 3: tableOption = 8;break;
                        case 4: tableOption = 12;break;
                    }

                    if (isChecked) {
                        selectedTableOptions.add(tableOption);
                    } else {
                        selectedTableOptions.remove(Integer.valueOf(tableOption));
                    }
                });

        builder.setNeutralButton("Clear", (dialog, which) -> {
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItems[i] = false;
            }
            selectedTableOptions.clear();
            filterRestaurants("");

            dialog.dismiss();
        });

        builder.setPositiveButton("OK", (dialog, which) -> filterRestaurants(searchBar.getText().toString()));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
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
