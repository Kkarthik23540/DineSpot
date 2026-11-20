package com.example.dinespot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeActivity serves as the main dashboard for the DineSpot application.
 * It provides users with a searchable and filterable list of restaurants,
 * a map view to locate nearby dining spots, and recommendations based on ratings.
 *
 * Features include:
 * - Real-time restaurant data from Firebase Firestore.
 * - Map integration using Google Maps API.
 * - Advanced filtering (rating, status, distance, alphabetical).
 * - Pagination for efficient loading of restaurant lists.
 * - Integration with Firebase Cloud Messaging for notifications.
 */
public class UserHomeActivity extends CommonBaseActivity implements OnMapReadyCallback {

    private EditText searchBar;
    private RecyclerView recyclerViewRestaurants, rvNearbyRestaurants, rvFavorites;
    private UserRestaurantAdapter restaurantAdapter;
    private UserNearbyAdapter nearbyAdapter;
    private UserFavoritesAdapter favoritesAdapter;
    private final List<CommonRestaurant> restaurantList = new ArrayList<>();
    private final List<CommonRestaurant> filteredList = new ArrayList<>();
    private final List<CommonRestaurant> nearbyList = new ArrayList<>();
    private final List<CommonRestaurant> favoritesList = new ArrayList<>();
    private final List<String> userFavoriteIds = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private int selectedRatingThreshold = 0;
    private String selectedAvailability = "All";
    private ShimmerFrameLayout shimmerFrameLayout;
    private TextView userInitialText;
    private View listViewContainer, mapViewContainer;
    private GoogleMap googleMap;
    private boolean isMapView = false;
    private boolean isFirstMapLoad = true;
    private LatLng userLatLng = null;
    private FusedLocationProviderClient fusedLocationClient;
    private ListenerRegistration restaurantListener, supportListener, bookingUpdateListener;

    private static final int PAGE_SIZE = 10;
    private DocumentSnapshot lastVisibleRestaurant;
    private boolean isRestaurantsLoading = false;
    private boolean isLastRestaurantsPage = false;

    private SwipeRefreshLayout swipeRefreshLayout;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Initializes the activity, sets up UI components, listeners, and starts
     * loading data from Firebase.
     * 
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_home);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        setupToolbar("DineSpot", false);

        searchBar = findViewById(R.id.searchBar);
        recyclerViewRestaurants = findViewById(R.id.recyclerViewRestaurants);
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        listViewContainer = findViewById(R.id.listViewContainer);
        mapViewContainer = findViewById(R.id.mapViewContainer);
        rvNearbyRestaurants = findViewById(R.id.rvNearbyRestaurants);
        rvFavorites = findViewById(R.id.rvFavorites);
        
        // Update header for nearby restaurants
        TextView tvNearbyHeader = findViewById(R.id.tvNearbyHeader);
        if (tvNearbyHeader != null) {
            tvNearbyHeader.setText("Nearby Restaurants (20km)");
        }

        recyclerViewRestaurants.setLayoutManager(new GridLayoutManager(this, 1));
        rvNearbyRestaurants.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFavorites.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        recyclerViewRestaurants.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (isMapView) return;
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == filteredList.size() - 1) {
                    if (!isRestaurantsLoading && !isLastRestaurantsPage && searchBar.getText().toString().isEmpty()) {
                        loadMoreRestaurants();
                    }
                }
            }
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            isLastRestaurantsPage = false;
            lastVisibleRestaurant = null;
            
            // Reset search when refreshing
            if (searchBar != null) searchBar.setText("");
            
            // Refresh lists
            zoomToCurrentLocation(); // Refresh location too
            loadRestaurantsFromFirebase();
            fetchUserFavorites();
            
            // Scroll to start
            if (rvNearbyRestaurants != null) rvNearbyRestaurants.scrollToPosition(0);
            if (rvFavorites != null) rvFavorites.scrollToPosition(0);
        });

        userInitialText = findViewById(R.id.userInitial);
        if (userInitialText != null) {
            userInitialText.setOnClickListener(v -> {
                startActivity(new Intent(UserHomeActivity.this, UserProfileActivity.class));
            });
        }

        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
            if (mapFragment != null) mapFragment.getMapAsync(this);
        } catch (Exception e) {
            Log.e("UserHomeActivity", "Map fragment error", e);
        }

        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.startShimmer();
            shimmerFrameLayout.setVisibility(View.VISIBLE);
        }
        recyclerViewRestaurants.setVisibility(View.GONE);

        requestLocationPermission();
        loadRestaurantsFromFirebase();
        fetchUserFavorites();
        listenForSupportReplies();
        listenForBookingUpdates();
        requestNotificationPermission();

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

        setupFilterSpinners();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu);
        CommonNavigationHelper.setupUserNavigation(this, bottomNavigationView, R.id.menu_home);
        
        bottomNavigationView.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.menu_home) {
                loadRestaurantsFromFirebase();
            }
        });

        setUserInitialInToolbar();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showCustomDialog(R.drawable.baseline_privacy_tip_24, 
                        "Exit App", 
                        "Are you sure you want to exit DineSpot?", 
                        "Exit Now", () -> finishAffinity(), 
                        "Stay Here", null);
            }
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            zoomToCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                zoomToCurrentLocation();
                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        Log.e("UserHomeActivity", "SecurityException on setMyLocationEnabled", e);
                    }
                }
            }
        }
    }

    /**
     * Configures the filter spinners/popups for status, sort, and rating.
     */
    private void setupFilterSpinners() {
        View filterStatus = findViewById(R.id.filter_status);
        View filterSort = findViewById(R.id.filter_sort_premium);
        View filterRating = findViewById(R.id.filter_rating);
        View filterLocation = findViewById(R.id.filter_location);

        filterStatus.setOnClickListener(v -> showFilterPopupMenu(v,
            new String[]{"All Status", "Open Only", "Full Only", "Closed Only"},
            (position) -> {
                String[] labels = {"All Status", "Open", "Full", "Closed"};
                selectedAvailability = labels[position];
                ((TextView)findViewById(R.id.tv_filter_status)).setText(labels[position]);
                filterRestaurants(searchBar.getText().toString());
            }));

        filterSort.setOnClickListener(v -> showFilterPopupMenu(v,
            new String[]{"Nearest First", "Rating (High to Low)", "Alphabetical (A-Z)", "Alphabetical (Z-A)"},
            (position) -> {
                String text = "Sort";
                if (position == 0) {
                    sortRestaurantsByDistance();
                    text = "Nearest";
                } else if (position == 1) {
                    sortRestaurantsByRating();
                    text = "Rating";
                } else if (position == 2) {
                    sortRestaurantsAlphabetically(true);
                    text = "A-Z";
                } else {
                    sortRestaurantsAlphabetically(false);
                    text = "Z-A";
                }
                ((TextView)findViewById(R.id.tv_filter_sort)).setText(text);
            }));

        filterRating.setOnClickListener(v -> showFilterPopupMenu(v,
            new String[]{"All Ratings", "5 Stars", "4+ Stars", "3+ Stars", "2+ Stars"},
            (position) -> {
                selectedRatingThreshold = position == 0 ? 0 : 6 - position;
                String[] labels = {"All Ratings", "5 Stars", "4+", "3+", "2+"};
                ((TextView)findViewById(R.id.tv_filter_rating)).setText(labels[position]);
                filterRestaurants(searchBar.getText().toString());
            }));

        filterLocation.setOnClickListener(v -> toggleViewMode());
    }

    /**
     * Displays a popup menu for selecting filter options.
     * 
     * @param anchor   The view to which the popup is attached.
     * @param options  The list of options to display.
     * @param listener Callback for when an option is selected.
     */
    private void showFilterPopupMenu(View anchor, String[] options, OnFilterSelectedListener listener) {
        PopupMenu popup = new PopupMenu(this, anchor);
        for (int i = 0; i < options.length; i++) {
            popup.getMenu().add(0, i, i, options[i]);
        }
        popup.setOnMenuItemClickListener(item -> {
            listener.onFilterSelected(item.getItemId());
            return true;
        });
        popup.show();
    }

    /**
     * Interface for handling filter selection events.
     */
    public interface OnFilterSelectedListener {
        /**
         * Called when a filter option is selected.
         * 
         * @param position The index of the selected option.
         */
        void onFilterSelected(int position);
    }

    /**
     * Fetches the current user's initial from Firestore and displays it in the toolbar.
     * Queries by email since the document ID may not match the Firebase UID.
     */
    private void setUserInitialInToolbar() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String email = firebaseAuth.getCurrentUser().getEmail();
        if (email == null) return;

        firestore.collection("users").whereEqualTo("email", email.toLowerCase().trim()).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                    String fullName = document.getString("name");
                    if (fullName != null && !fullName.isEmpty()) {
                        String initial = fullName.substring(0, 1).toUpperCase();
                        if (userInitialText != null) {
                            userInitialText.setText(initial);
                            userInitialText.setOnClickListener(v -> {
                                // Navigate to UserProfileActivity when the initial is clicked
                                Intent intent = new Intent(UserHomeActivity.this, UserProfileActivity.class);
                                startActivity(intent);
                            });
                        }
                    }
                } else {
                    // Fallback to Display Name from Auth if Firestore fails
                    String displayName = firebaseAuth.getCurrentUser().getDisplayName();
                    if (displayName != null && !displayName.isEmpty()) {
                        String initial = displayName.substring(0, 1).toUpperCase();
                        if (userInitialText != null) userInitialText.setText(initial);
                    }
                }
            });
    }

    /**
     * Toggles between the list view and the map view for displaying restaurants.
     */
    private void toggleViewMode() {
        isMapView = !isMapView;
        if (listViewContainer != null) listViewContainer.setVisibility(isMapView ? View.GONE : View.VISIBLE);
        if (mapViewContainer != null) mapViewContainer.setVisibility(isMapView ? View.VISIBLE : View.GONE);
        
        View filterLocation = findViewById(R.id.filter_location);
        TextView tvLocation = findViewById(R.id.tv_filter_location);
        ImageView ivLocation = findViewById(R.id.iv_location_icon);
        
        if (isMapView) {
            if (filterLocation != null) filterLocation.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimaryLight)));
            if (tvLocation != null) tvLocation.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            if (ivLocation != null) ivLocation.setImageResource(R.drawable.baseline_close_24);
        } else {
            if (filterLocation != null) filterLocation.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface)));
            if (tvLocation != null) tvLocation.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
            if (ivLocation != null) ivLocation.setImageResource(R.drawable.baseline_location_on_24);
        }
        
        if (isMapView && googleMap != null) {
            updateMapMarkers();
        }
    }

    /**
     * Callback for when the Google Map is ready to be used.
     * 
     * @param map The GoogleMap object.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        
        // Enable basic UI settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            if (isFirstMapLoad) {
                zoomToCurrentLocation();
                isFirstMapLoad = false;
            }
        }

        updateMapMarkers();
    }

    /**
     * Zooms the map to the user's current location if permissions are granted.
     */
    private void zoomToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f));
                }
                // Refresh list to show distances now that we have location
                loadRestaurantsFromFirebase();
            }
        });
    }

    /**
     * Updates the markers on the map based on the current filtered list of restaurants.
     */
    private void updateMapMarkers() {
        if (googleMap == null) return;
        try {
            googleMap.clear();
            if (filteredList == null || filteredList.isEmpty()) return;

            for (CommonRestaurant r : new ArrayList<>(filteredList)) {
                if (r != null && r.getLocation() != null) {
                    LatLng pos = new LatLng(r.getLocation().getLatitude(), r.getLocation().getLongitude());
                    
                    // Dynamic Marker Coloring based on Availability
                    float markerHue;
                    String status = r.getStatus() != null ? r.getStatus() : "Open";
                    if ("Closed".equalsIgnoreCase(status)) {
                        markerHue = com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE; // Grayish-blue
                    } else if ("Full".equalsIgnoreCase(status)) {
                        markerHue = com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED;
                    } else {
                        markerHue = 174f; // Teal matching app theme
                    }

                    googleMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                            .position(pos)
                            .title(r.getName())
                            .snippet(status + " • " + r.getRating() + " Stars")
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(markerHue)));
                }
            }
        } catch (Exception e) {
            Log.e("UserHomeActivity", "Error updating markers", e);
        }
    }

    /**
     * Loads the first page of restaurants from Firebase Firestore and listens for updates.
     */
    private void loadRestaurantsFromFirebase() {
        if (restaurantListener != null) restaurantListener.remove();
        
        isRestaurantsLoading = true;
        CollectionReference restaurantRef = firestore.collection("restaurants");
        
        Query query = restaurantRef.orderBy("name").limit(PAGE_SIZE);
        
        restaurantListener = query.addSnapshotListener((snapshot, e) -> {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            isRestaurantsLoading = false;
            if (isFinishing() || isDestroyed()) return;
            if (e != null) {
                Log.e("UserHomeActivity", "Error loading: " + e.getMessage());
                return;
            }
            if (snapshot != null) {
                if (!snapshot.isEmpty()) {
                    lastVisibleRestaurant = snapshot.getDocuments().get(snapshot.size() - 1);
                    isLastRestaurantsPage = snapshot.size() < PAGE_SIZE;
                } else {
                    isLastRestaurantsPage = true;
                }

                List<CommonRestaurant> newList = new ArrayList<>();
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    CommonRestaurant restaurant = document.toObject(CommonRestaurant.class);
                    if (restaurant != null) {
                        restaurant.setRestaurantId(document.getId());
                        if (userLatLng != null && restaurant.getLocation() != null) {
                            double dist = calculateDistance(userLatLng.latitude, userLatLng.longitude,
                                    restaurant.getLocation().getLatitude(), restaurant.getLocation().getLongitude());
                            restaurant.setDistance(dist);
                        }
                        newList.add(restaurant);
                        syncStatusByTime(restaurant);
                    }
                }

                // Update data source atomically to prevent flickering
                restaurantList.clear();
                restaurantList.addAll(newList);
                
                if (restaurantAdapter == null) {
                    restaurantAdapter = new UserRestaurantAdapter(UserHomeActivity.this, filteredList, userFavoriteIds, this::toggleFavorite);
                    recyclerViewRestaurants.setAdapter(restaurantAdapter);
                }

                restaurantAdapter.updateFavorites(userFavoriteIds);
                
                // Show data containers before filtering so visibility changes work correctly
                toggleLoading(false);
                
                filterRestaurants(searchBar.getText().toString());
                loadNearbyRestaurants();
                loadFavoriteRestaurants(); 
                
                if (isMapView) updateMapMarkers();
            }
        });
    }

    /**
     * Loads the next page of restaurants from Firestore for pagination.
     */
    private void loadMoreRestaurants() {
        if (lastVisibleRestaurant == null || isLastRestaurantsPage) return;

        isRestaurantsLoading = true;
        firestore.collection("restaurants")
                .orderBy("name")
                .startAfter(lastVisibleRestaurant)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    isRestaurantsLoading = false;
                    if (!snapshot.isEmpty()) {
                        lastVisibleRestaurant = snapshot.getDocuments().get(snapshot.size() - 1);
                        isLastRestaurantsPage = snapshot.size() < PAGE_SIZE;

                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            CommonRestaurant restaurant = document.toObject(CommonRestaurant.class);
                            if (restaurant != null) {
                                restaurant.setRestaurantId(document.getId());
                                if (userLatLng != null && restaurant.getLocation() != null) {
                                    double dist = calculateDistance(userLatLng.latitude, userLatLng.longitude,
                                            restaurant.getLocation().getLatitude(), restaurant.getLocation().getLongitude());
                                    restaurant.setDistance(dist);
                                }
                                restaurantList.add(restaurant);
                            }
                        }
                        filterRestaurants(searchBar.getText().toString());
                        if (isMapView) updateMapMarkers();
                    } else {
                        isLastRestaurantsPage = true;
                    }
                })
                .addOnFailureListener(e -> isRestaurantsLoading = false);
    }

    private void fetchUserFavorites() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        firestore.collection("users").whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(snaps -> {
            if (snaps != null && !snaps.isEmpty()) {
                Object favsObj = snaps.getDocuments().get(0).get("favoriteIds");
                userFavoriteIds.clear();
                if (favsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> favs = (List<String>) favsObj;
                    if (favs != null) userFavoriteIds.addAll(favs);
                }
                loadFavoriteRestaurants();
                
                // Also update the main list to reflect favorite status if already loaded
                if (restaurantAdapter != null) {
                    restaurantAdapter.updateFavorites(userFavoriteIds);
                }
            }
        });
    }

    private void loadFavoriteRestaurants() {
        if (rvFavorites == null) return;
        
        favoritesList.clear();
        for (CommonRestaurant r : restaurantList) {
            if (r != null && r.getRestaurantId() != null && userFavoriteIds.contains(r.getRestaurantId())) {
                favoritesList.add(r);
            }
        }

        View header = findViewById(R.id.tvFavoritesHeader);
        if (!favoritesList.isEmpty()) {
            rvFavorites.setVisibility(View.VISIBLE);
            if (header != null) header.setVisibility(View.VISIBLE);
            if (favoritesAdapter == null) {
                favoritesAdapter = new UserFavoritesAdapter(this, favoritesList);
                rvFavorites.setAdapter(favoritesAdapter);
            } else {
                favoritesAdapter.notifyDataSetChanged();
            }
        } else {
            rvFavorites.setVisibility(View.GONE);
            if (header != null) header.setVisibility(View.GONE);
        }
    }

    /**
     * Populates the horizontal list with nearby restaurants sorted by distance.
     * Only displays restaurants that are currently "Open" or "Full" and within 20km.
     */
    private void loadNearbyRestaurants() {
        if (rvNearbyRestaurants == null) return;
        
        nearbyList.clear();
        // Filter out closed restaurants and distance > 20km
        List<CommonRestaurant> eligible = new ArrayList<>();
        for (CommonRestaurant r : restaurantList) {
            if (r == null) continue;
            boolean isOperating = r.getStatus() == null || !"Closed".equalsIgnoreCase(r.getStatus());
            boolean isNearby = r.getDistance() > 0 && r.getDistance() <= 20.0;
            if (isOperating && isNearby) {
                eligible.add(r);
            }
        }

        // Sort by distance (asc)
        eligible.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
        nearbyList.addAll(eligible);

        View header = findViewById(R.id.tvNearbyHeader);
        if (!nearbyList.isEmpty()) {
            rvNearbyRestaurants.setVisibility(View.VISIBLE);
            if (header != null) header.setVisibility(View.VISIBLE);
            if (nearbyAdapter == null) {
                nearbyAdapter = new UserNearbyAdapter(this, nearbyList);
                rvNearbyRestaurants.setAdapter(nearbyAdapter);
            } else {
                nearbyAdapter.notifyDataSetChanged();
            }
        } else {
            rvNearbyRestaurants.setVisibility(View.GONE);
            if (header != null) header.setVisibility(View.GONE);
        }
    }

    private void toggleFavorite(CommonRestaurant restaurant, boolean shouldFavorite) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        if (shouldFavorite) {
            userFavoriteIds.add(restaurant.getRestaurantId());
        } else {
            userFavoriteIds.remove(restaurant.getRestaurantId());
        }

        // Update UI immediately
        if (restaurantAdapter != null) restaurantAdapter.updateFavorites(userFavoriteIds);
        loadFavoriteRestaurants();

        // Sync with Firestore
        firestore.collection("users").whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(snaps -> {
            if (!snaps.isEmpty()) {
                snaps.getDocuments().get(0).getReference().update("favoriteIds", userFavoriteIds);
            }
        });
    }

    private void toggleLoading(boolean isLoading) {
        TextView noResultsText = findViewById(R.id.noResultsText);
        View nearbyHeader = findViewById(R.id.tvNearbyHeader);
        View favHeader = findViewById(R.id.tvFavoritesHeader);
        View exploreHeader = findViewById(R.id.tvExploreHeader);

        if (isLoading) {
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.startShimmer();
                shimmerFrameLayout.setVisibility(View.VISIBLE);
            }
            recyclerViewRestaurants.setVisibility(View.GONE);
            if (rvNearbyRestaurants != null) rvNearbyRestaurants.setVisibility(View.GONE);
            if (rvFavorites != null) rvFavorites.setVisibility(View.GONE);
            if (nearbyHeader != null) nearbyHeader.setVisibility(View.GONE);
            if (favHeader != null) favHeader.setVisibility(View.GONE);
            if (exploreHeader != null) exploreHeader.setVisibility(View.GONE);
            if (noResultsText != null) noResultsText.setVisibility(View.GONE);
        } else {
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
            }
            if (exploreHeader != null) exploreHeader.setVisibility(View.VISIBLE);
            // filterRestaurants will handle showing recyclerViewRestaurants
        }
    }

    /**
     * Filters the restaurant list based on the search query, rating, and availability status.
     * 
     * @param query The search string entered by the user.
     */
    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    private void filterRestaurants(String query) {
        filteredList.clear();
        String q = query != null ? query.toLowerCase().trim() : "";
        
        // When searching or filtering, evaluate visibility of extra lists
        boolean isSearching = !q.isEmpty() || selectedRatingThreshold > 0 || !"All".equals(selectedAvailability);
        
        if (isSearching) {
            // Hide everything else during active search/filtering
            if (rvNearbyRestaurants != null) rvNearbyRestaurants.setVisibility(View.GONE);
            if (rvFavorites != null) rvFavorites.setVisibility(View.GONE);
            
            View nearbyHeader = findViewById(R.id.tvNearbyHeader);
            if (nearbyHeader != null) nearbyHeader.setVisibility(View.GONE);
            View favHeader = findViewById(R.id.tvFavoritesHeader);
            if (favHeader != null) favHeader.setVisibility(View.GONE);
        } else {
            // Restore visibility based on actual content
            loadNearbyRestaurants();
            loadFavoriteRestaurants();
        }

        for (CommonRestaurant restaurant : restaurantList) {
            if (restaurant == null || restaurant.getName() == null) continue;
            
            boolean matchesQuery = restaurant.getName().toLowerCase().contains(q) || 
                                 (restaurant.getAddress() != null && restaurant.getAddress().toLowerCase().contains(q));
            
            boolean matchesRating = restaurant.getRating() >= selectedRatingThreshold;
            
            boolean matchesAvailability = true;
            String status = restaurant.getStatus();
            if (status == null) status = "Open";
            
            if ("Open".equals(selectedAvailability)) {
                matchesAvailability = "Open".equalsIgnoreCase(status);
            } else if ("Full".equals(selectedAvailability)) {
                matchesAvailability = "Full".equalsIgnoreCase(status);
            } else if ("Closed".equals(selectedAvailability)) {
                matchesAvailability = "Closed".equalsIgnoreCase(status);
            }
            
            if (matchesQuery && matchesRating && matchesAvailability) {
                filteredList.add(restaurant);
            }
        }

        // Sort: Operating (Open/Full) restaurants at top, Open with tables first
        filteredList.sort((r1, r2) -> {
            float score1 = getStatusScore(r1);
            float score2 = getStatusScore(r2);
            return Float.compare(score2, score1); // Higher score first
        });

        if (restaurantAdapter != null) {
            restaurantAdapter.notifyDataSetChanged();
        }

        TextView noResultsText = findViewById(R.id.noResultsText);
        if (filteredList.isEmpty()) {
            recyclerViewRestaurants.setVisibility(View.GONE);
            noResultsText.setVisibility(View.VISIBLE);
            if (!q.isEmpty() || selectedRatingThreshold > 0) {
                noResultsText.setText("No restaurants available for the selected filters. Please try again.");
            } else {
                noResultsText.setText("No restaurants found.");
            }
        } else {
            recyclerViewRestaurants.setVisibility(View.VISIBLE);
            noResultsText.setVisibility(View.GONE);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }
    }

    /**
     * Listens for incoming support chat messages and triggers system notifications.
     */
    private void listenForSupportReplies() {
        if (firebaseAuth.getUid() == null) return;
        String chatId = "support_" + firebaseAuth.getUid();
        
        supportListener = firestore.collection("support_chats").document(chatId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    
                    Long lastTs = snapshot.getLong("lastTimestamp");
                    String userId = snapshot.getString("userId");
                    String lastMsg = snapshot.getString("lastMessage");
                    
                    if (lastTs != null && userId != null && !userId.equals(firebaseAuth.getUid())) {
                        if (System.currentTimeMillis() - lastTs < 30000) { // Within 30 seconds
                            // Trigger System Notification
                            CommonNotificationHelper.showChatNotification(this, "App Support", lastMsg, chatId, userId);
                            
                            // Mark as delivered
                            firestore.collection("support_chats").document(chatId).update("status", "delivered");
                        }
                    }
                });
    }

    /**
     * Listens for changes in the status of user bookings and triggers notifications.
     */
    private void listenForBookingUpdates() {
        if (firebaseAuth.getUid() == null) return;
        
        bookingUpdateListener = firestore.collection("bookings")
                .whereEqualTo("userId", firebaseAuth.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            String status = dc.getDocument().getString("status");
                            String resName = dc.getDocument().getString("restaurantName");
                            String bid = dc.getDocument().getId();
                            
                            // Notify only on significant status changes
                            if ("Accepted".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) {
                                CommonNotificationHelper.showBookingNotification(this, 
                                        resName != null ? resName : "DineSpot", 
                                        status, bid);
                            }
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (restaurantListener != null) restaurantListener.remove();
        if (supportListener != null) supportListener.remove();
        if (bookingUpdateListener != null) bookingUpdateListener.remove();
        super.onDestroy();
    }

    /**
     * Calculates a priority score for a restaurant based on its status and table availability.
     * 
     * @param r The restaurant object.
     * @return A float score (higher means more accessible).
     */
    private float getStatusScore(CommonRestaurant r) {
        if (r == null) return -1f;
        String status = r.getStatus();
        if ("Closed".equalsIgnoreCase(status)) return 0f;
        if ("Full".equalsIgnoreCase(status)) return 1f;
        
        if (r.getNoOfTables() <= 0) return 1.5f; 
        return 2f; 
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Sorts the filtered restaurant list by rating (descending).
     */
    private void sortRestaurantsByRating() {
        filteredList.sort((r1, r2) -> {
            float rating1 = r1 != null ? r1.getRating() : 0f;
            float rating2 = r2 != null ? r2.getRating() : 0f;
            return Float.compare(rating2, rating1); // Descending
        });
        if (restaurantAdapter != null) {
            restaurantAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Sorts the filtered restaurant list by distance from the user (ascending).
     */
    private void sortRestaurantsByDistance() {
        filteredList.sort((r1, r2) -> {
            double dist1 = r1 != null ? r1.getDistance() : Double.MAX_VALUE;
            double dist2 = r2 != null ? r2.getDistance() : Double.MAX_VALUE;
            return Double.compare(dist1, dist2); // Ascending
        });
        if (restaurantAdapter != null) {
            restaurantAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Calculates the distance between two geographic coordinates in kilometers.
     * 
     * @param lat1 Latitude of point 1.
     * @param lon1 Longitude of point 1.
     * @param lat2 Latitude of point 2.
     * @param lon2 Longitude of point 2.
     * @return Distance in kilometers.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000.0; // Convert to km
    }

    /**
     * Sorts the filtered restaurant list alphabetically by name.
     * 
     * @param isAscending True for A-Z, false for Z-A.
     */
    private void sortRestaurantsAlphabetically(boolean isAscending) {
        filteredList.sort((r1, r2) -> {
            String n1 = r1.getName() != null ? r1.getName() : "";
            String n2 = r2.getName() != null ? r2.getName() : "";
            return isAscending ? n1.compareToIgnoreCase(n2) : n2.compareToIgnoreCase(n1);
        });

        if (restaurantAdapter != null) {
            restaurantAdapter.notifyDataSetChanged();
        }
    }

    private void syncStatusByTime(CommonRestaurant r) {
        boolean shouldBeOpen = CommonUtils.isWithinOperatingHours(r);
        String currentStatus = r.getStatus();
        if (currentStatus == null || r.getRestaurantId() == null) return;

        // Only auto-close based on time. We DO NOT auto-open to respect Admin manual overrides.
        if ("Open".equalsIgnoreCase(currentStatus) && !shouldBeOpen) {
            firestore.collection("restaurants").document(r.getRestaurantId()).update("status", "Closed");
        }
    }
}
