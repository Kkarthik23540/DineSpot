package com.example.dinespot;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserRestaurantDetailsActivity extends CommonBaseActivity {

    private FirebaseFirestore firestore;
    private RecyclerView foodItemsRecyclerView, rvFoodCategories;
    private UserFoodAdapter foodAdapter;
    private UserCategoryAdapter categoryAdapter;
    private List<CommonFoodItem> foodItemList;
    private String selectedCategory = "All";
    private String selectedDietary = "All"; // "All", "Veg", "Non-Veg"
    private EditText searchBar;
    private com.google.android.material.button.MaterialButtonToggleGroup toggleDietary;
    private View layoutCartSummary;
    private TextView tvCartCount, tvCartTotal, tvEmptyMenu;
    private String restaurantId;
    private com.google.firebase.firestore.DocumentReference userDocRef;
    
    private TextView restaurantNameTextView, restaurantRatingTextView, tvReviewCount;
    private TextView tvTables2, tvTables4, tvTables6, tvTables10;
    private ImageView restaurantImageView;
    private android.widget.ViewFlipper imageFlipper;
    private Button selectTableButton, cancelBookingButton;
    private View directionsButton;
    private View menuLayout, bookingRequiredCard, bookingStatusCard, layoutMenuFilters;
    private com.google.android.material.appbar.CollapsingToolbarLayout collapsingToolbar;
    private ProgressBar selectTableProgress;
    private View detailsContentScroll, detailsSkeleton;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private ListenerRegistration bookingListener, restaurantRealtimeListener;
    private String currentBookingStatus = "none";
    private String restaurantStatus = "Open";
    private int totalAvailableTables = 0;
    private final Map<String, Long> currentTableInventory = new HashMap<>();
    private String currentBookingId = null;
    private String restaurantPhoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_restaurant_details);

        firestore = FirebaseFirestore.getInstance();
        restaurantId = getIntent().getStringExtra("restaurant_id");

        if (restaurantId == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar("Details", true);

        foodItemList = new ArrayList<>();
        foodAdapter = new UserFoodAdapter(this, foodItemList, restaurantId, userDocRef, item -> {
            // Animate Bottom Cart Bar on Add
            if (layoutCartSummary != null) {
                layoutCartSummary.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).withEndAction(() -> {
                    layoutCartSummary.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }).start();
            }
        });
        if (foodItemsRecyclerView != null) {
            foodItemsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            foodItemsRecyclerView.setAdapter(foodAdapter);
        }

        startRealtimeListeners();

        if (searchBar != null) {
            searchBar.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterFoodItems(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (toggleDietary != null) {
            toggleDietary.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                if (checkedId == R.id.btnVegOnly) selectedDietary = "Veg";
                else if (checkedId == R.id.btnNonVegOnly) selectedDietary = "Non-Veg";
                else selectedDietary = "All";
                filterFoodItems(searchBar.getText().toString());
            });
        }

        if (layoutCartSummary != null) {
            View btnViewCart = findViewById(R.id.btn_view_cart_summary);
            if (btnViewCart != null) {
                btnViewCart.setOnClickListener(v -> startActivity(new Intent(this, UserCartActivity.class)));
            }
            // Fix: Consume clicks on the bar itself to prevent "ghost" adding of items behind it
            layoutCartSummary.setOnClickListener(v -> {
                // Do nothing, just consume click
            });
        }

        if (selectTableButton != null) selectTableButton.setOnClickListener(v -> checkActiveBookingAndShowDialog());
        if (cancelBookingButton != null) cancelBookingButton.setOnClickListener(v -> cancelBookingRequest());
        
        // Setup Call Button
        setupActionButtons();
    }

    private void setupActionButtons() {
        View btnCall = findViewById(R.id.call_button);
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                if (restaurantPhoneNumber != null && !restaurantPhoneNumber.isEmpty()) {
                    dialNumber(restaurantPhoneNumber);
                } else {
                    // One-time fetch for phone if not in snapshot (Admin might have not set it in main doc)
                    firestore.collection("restaurants").document(restaurantId).get().addOnSuccessListener(doc -> {
                        String phone = doc.getString("phoneNumber");
                        if (phone != null && !phone.isEmpty()) {
                            restaurantPhoneNumber = phone;
                            dialNumber(phone);
                        } else {
                            Toast.makeText(this, "Restaurant phone not available", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
        
        if (directionsButton != null) {
            directionsButton.setOnClickListener(v -> {
                // Fetch latest address/location on click to ensure accuracy
                firestore.collection("restaurants").document(restaurantId).get().addOnSuccessListener(doc -> {
                    com.google.firebase.firestore.GeoPoint gp = doc.getGeoPoint("location");
                    String addr = doc.getString("address");
                    try {
                        Uri uri = (gp != null) ? Uri.parse("google.navigation:q=" + gp.getLatitude() + "," + gp.getLongitude()) : Uri.parse("google.navigation:q=" + Uri.encode(addr));
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        } else {
                            startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Maps could not be opened", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    private void dialNumber(String phone) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open dialer", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        foodItemsRecyclerView = findViewById(R.id.food_items_recycler_view);
        rvFoodCategories = findViewById(R.id.rvFoodCategories);
        searchBar = findViewById(R.id.search_bar);
        toggleDietary = findViewById(R.id.toggleDietary);
        layoutCartSummary = findViewById(R.id.layout_cart_summary);
        tvCartCount = findViewById(R.id.tv_cart_count);
        tvCartTotal = findViewById(R.id.tv_cart_total);
        tvEmptyMenu = findViewById(R.id.tvEmptyMenu);
        
        restaurantNameTextView = findViewById(R.id.restaurant_name);
        restaurantRatingTextView = findViewById(R.id.restaurant_rating);
        tvTables2 = findViewById(R.id.tv_tables_2);
        tvTables4 = findViewById(R.id.tv_tables_4);
        tvTables6 = findViewById(R.id.tv_tables_6);
        tvTables10 = findViewById(R.id.tv_tables_10);
        restaurantImageView = findViewById(R.id.restaurant_image);
        imageFlipper = findViewById(R.id.image_flipper);
        selectTableButton = findViewById(R.id.select_table_button);
        directionsButton = findViewById(R.id.location_button);
        selectTableProgress = findViewById(R.id.select_table_progress);
        cancelBookingButton = findViewById(R.id.cancel_booking_button);
        menuLayout = findViewById(R.id.menu_layout);
        bookingRequiredCard = findViewById(R.id.booking_required_card);
        bookingStatusCard = findViewById(R.id.booking_status_card);
        layoutMenuFilters = findViewById(R.id.layoutMenuFilters);
        detailsContentScroll = findViewById(R.id.details_content_scroll);
        detailsSkeleton = findViewById(R.id.details_skeleton);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                startRealtimeListeners();
            });
        }
    }

    private void startRealtimeListeners() {
        if (bookingListener != null) bookingListener.remove();
        if (restaurantRealtimeListener != null) restaurantRealtimeListener.remove();

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            hideSkeleton();
            return;
        }

        // Fetch User Doc Ref once and keep it
        if (userDocRef == null) {
            String email = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
            if (email != null) {
                firestore.collection("users").whereEqualTo("email", email).get().addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        userDocRef = snaps.getDocuments().get(0).getReference();
                        if (foodAdapter != null) foodAdapter.setUserDocRef(userDocRef);
                        setupCartListener();
                    }
                });
            }
        } else {
            setupCartListener();
        }

        // Use manual management (remove listener in onDestroy) to avoid ActivityScope/FragmentManager crash
        bookingListener = firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .whereEqualTo("restaurantId", restaurantId)
                .addSnapshotListener((snapshots, e) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (e != null) {
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<DocumentSnapshot> docs = getSortedDocs(snapshots);
                        // Find the absolute latest ACTIVE booking
                        DocumentSnapshot latestActive = null;
                        for (DocumentSnapshot doc : docs) {
                            String status = doc.getString("status");
                            if ("Pending".equalsIgnoreCase(status) || "Accepted".equalsIgnoreCase(status) || "Waitlisted".equalsIgnoreCase(status)) {
                                latestActive = doc;
                                break; 
                            }
                        }
                        
                        // If no active booking, just take the latest one (Completed/Rejected)
                        DocumentSnapshot finalMatch = (latestActive != null) ? latestActive : docs.get(0);
                        
                        currentBookingId = finalMatch.getId();
                        currentBookingStatus = finalMatch.getString("status") != null ? finalMatch.getString("status") : "none";
                        
                        // Check for review prompt after completion
                        if ("Completed".equalsIgnoreCase(currentBookingStatus)) {
                            showReviewPromptIfNeeded(finalMatch);
                        }
                        
                        updateUIBasedOnBooking(currentBookingStatus);
                    } else {
                        currentBookingId = null;
                        currentBookingStatus = "none";
                        updateUIBasedOnBooking("none");
                    }
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                });

        restaurantRealtimeListener = firestore.collection("restaurants").document(restaurantId)
                .addSnapshotListener((snapshot, e) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (snapshot != null && snapshot.exists()) {
                        updateRestaurantUI(snapshot);
                        fetchAndCalculateRating();
                    }
                });
    }

    private List<DocumentSnapshot> getSortedDocs(com.google.firebase.firestore.QuerySnapshot snapshots) {
        List<DocumentSnapshot> docs = new ArrayList<>(snapshots.getDocuments());
        docs.sort((d1, d2) -> {
            String t1 = d1.getString("dateTime");
            String t2 = d2.getString("dateTime");
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });
        return docs;
    }

    private void setupCartListener() {
        if (userDocRef == null) return;
        userDocRef.collection("cart").addSnapshotListener((cartSnaps, error) -> {
            if (cartSnaps != null) {
                updateCartSummary(cartSnaps);
            }
        });
    }

    private void showReviewPromptIfNeeded(DocumentSnapshot booking) {
        Boolean reviewed = booking.getBoolean("reviewed");
        if (reviewed != null && reviewed) return;

        showCustomDialog(R.drawable.baseline_star_24,
                "Rate Your Experience",
                "How was your dining experience at " + booking.getString("restaurantName") + "?",
                "Rate Now", () -> {
                    Intent intent = new Intent(this, UserFeedbackActivity.class);
                    intent.putExtra("restaurant_id", restaurantId);
                    intent.putExtra("booking_id", booking.getId());
                    startActivity(intent);
                    
                    // Mark as reviewed to avoid duplicate prompts
                    firestore.collection("bookings").document(booking.getId()).update("reviewed", true);
                },
                "Later", null);
    }

    private void updateUIBasedOnBooking(String status) {
        boolean accepted = "Accepted".equalsIgnoreCase(status);
        boolean pending = "Pending".equalsIgnoreCase(status);
        boolean waitlisted = "Waitlisted".equalsIgnoreCase(status);
        
        boolean isClosed = "Closed".equalsIgnoreCase(restaurantStatus);
        boolean isManuallyFull = "Full".equalsIgnoreCase(restaurantStatus);
        
        if (layoutMenuFilters != null) {
            layoutMenuFilters.setVisibility(isClosed ? View.GONE : View.VISIBLE);
        }

        // Hide cart summary if closed (cannot order)
        if (isClosed && layoutCartSummary != null) {
            layoutCartSummary.setVisibility(View.GONE);
        }

        // Primary button visibility
        boolean showCheckIn = !accepted && !pending && !waitlisted;
        
        if (selectTableButton != null) {
            selectTableButton.setVisibility(showCheckIn ? View.VISIBLE : View.GONE);
            if (isClosed) {
                selectTableButton.setText(R.string.status_closed);
                selectTableButton.setEnabled(false);
            } else if (totalAvailableTables <= 0 || isManuallyFull) {
                selectTableButton.setText(R.string.status_join_waitlist);
                selectTableButton.setEnabled(true);
            } else {
                selectTableButton.setText(R.string.status_check_in);
                selectTableButton.setEnabled(true);
            }
        }
        
        if (cancelBookingButton != null) {
            // Only show cancel if NOT accepted. Admin handles end of session or payment finishes it.
            cancelBookingButton.setVisibility(pending || waitlisted ? View.VISIBLE : View.GONE);
            cancelBookingButton.setText(waitlisted ? "Leave Waitlist" : "Withdraw Request");
        }

        if (bookingStatusCard != null) {
            bookingStatusCard.setVisibility(pending || waitlisted ? View.VISIBLE : View.GONE);
            TextView msg = findViewById(R.id.tv_booking_status_msg);
            ProgressBar progress = findViewById(R.id.booking_progress);
            if (msg != null) msg.setText(waitlisted ? "On Waitlist" : "Request Pending");
            if (progress != null) progress.setVisibility(View.VISIBLE);
        }
        
        if (bookingRequiredCard != null) {
            bookingRequiredCard.setVisibility(accepted || pending || waitlisted ? View.GONE : View.VISIBLE);
        }
        
        if (menuLayout != null) menuLayout.setVisibility(accepted ? View.VISIBLE : View.GONE);
        
        if (accepted) loadFoodItems();
    }

    private void updateRestaurantUI(DocumentSnapshot snapshot) {
        hideSkeleton();
        String name = snapshot.getString("name");
        if (restaurantNameTextView != null) restaurantNameTextView.setText(name);
        
        // Show restaurant name in collapsed toolbar for clarity
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(name != null ? name : "Restaurant Details");
        }

        // Map and Call data are handled in setupActionButtons listeners to be dynamic
        
        Map<String, Object> types = (Map<String, Object>) snapshot.get("tableTypes");
        if (types != null) {
            currentTableInventory.clear();
            long calculatedTotal = 0;
            for (Map.Entry<String, Object> entry : types.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    long count = ((Number) entry.getValue()).longValue();
                    currentTableInventory.put(entry.getKey(), count);
                    calculatedTotal += count;
                    
                    // Update specific UI labels
                    String k = entry.getKey();
                    if ("2".equals(k) && tvTables2 != null) tvTables2.setText(String.valueOf(count));
                    else if ("4".equals(k) && tvTables4 != null) tvTables4.setText(String.valueOf(count));
                    else if ("6".equals(k) && tvTables6 != null) tvTables6.setText(String.valueOf(count));
                    else if ("10".equals(k) && tvTables10 != null) tvTables10.setText(String.valueOf(count));
                }
            }
            
            totalAvailableTables = (int) calculatedTotal;
            restaurantStatus = snapshot.getString("status");
            restaurantPhoneNumber = snapshot.getString("phoneNumber");

            if (restaurantStatus == null) restaurantStatus = "Open";
            
            // Re-sync entire UI state now that we have fresh restaurant data
            updateUIBasedOnBooking(currentBookingStatus);
            
            if (bookingRequiredCard != null && bookingRequiredCard.getVisibility() == View.VISIBLE) {
                TextView reqTitle = bookingRequiredCard.findViewById(R.id.tv_booking_required_title);
                TextView reqDesc = bookingRequiredCard.findViewById(R.id.tv_booking_required_desc);
                if ("Closed".equalsIgnoreCase(restaurantStatus)) {
                    if (reqTitle != null) reqTitle.setText("Restaurant Closed");
                    if (reqDesc != null) reqDesc.setText("This restaurant is not accepting new guests right now.");
                } else {
                    if (reqTitle != null) reqTitle.setText("Pre-Booking Required");
                    if (reqDesc != null) reqDesc.setText("Please secure your table to unlock the menu.");
                }
            }
        }

        String imageUrl = snapshot.getString("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageFlipper != null) {
                imageFlipper.stopFlipping();
                imageFlipper.setDisplayedChild(0);
            }
            if (imageUrl.startsWith("http")) {
                if (restaurantImageView != null) {
                    Glide.with(getApplicationContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.baseline_image_24)
                            .error(R.drawable.baseline_image_24)
                            .into(restaurantImageView);
                }
            } else {
                new Thread(() -> {
                    try {
                        String cleanBase = imageUrl;
                        if (imageUrl.contains(",")) {
                            String[] parts = imageUrl.split(",");
                            if (parts.length > 1) {
                                cleanBase = parts[1];
                            }
                        }
                        final byte[] bytes = Base64.decode(cleanBase.trim(), Base64.DEFAULT);
                        runOnUiThread(() -> {
                            if (restaurantImageView != null) Glide.with(getApplicationContext()).asBitmap().load(bytes).into(restaurantImageView);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            if (imageFlipper != null) {
                                imageFlipper.setDisplayedChild(1);
                                imageFlipper.startFlipping();
                            }
                        });
                    }
                }).start();
            }
        } else {
            if (imageFlipper != null) {
                imageFlipper.setDisplayedChild(1);
                imageFlipper.startFlipping();
            }
        }
    }

    private void hideSkeleton() {
        if (detailsSkeleton != null) detailsSkeleton.setVisibility(View.GONE);
        if (detailsContentScroll != null) detailsContentScroll.setVisibility(View.VISIBLE);
    }

    private void fetchAndCalculateRating() {
        firestore.collection("restaurants").document(restaurantId).collection("reviews").get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                if (restaurantRatingTextView != null) restaurantRatingTextView.setText("0.0");
                if (tvReviewCount != null) tvReviewCount.setText("(0)");
                return;
            }
            double total = 0;
            for (DocumentSnapshot d : snaps) {
                Double r = d.getDouble("rating");
                total += r != null ? r : 0;
            }
            int count = snaps.size();
            float average = (float) (total / count);
            if (restaurantRatingTextView != null) restaurantRatingTextView.setText(String.format(Locale.getDefault(), "%.1f", average));
            if (tvReviewCount != null) tvReviewCount.setText("(" + count + ")");

            // Auto-Sync to main document if mismatch found
            firestore.collection("restaurants").document(restaurantId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Double currentRating = doc.getDouble("rating");
                    if (currentRating == null || Math.abs(currentRating - average) > 0.05) {
                        firestore.collection("restaurants").document(restaurantId).update("rating", average);
                    }
                }
            });
        });
    }

    private void checkActiveBookingAndShowDialog() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean hasActive = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        String rid = doc.getString("restaurantId");
                        
                        if ("Pending".equalsIgnoreCase(status) || "Accepted".equalsIgnoreCase(status)) {
                            hasActive = true;
                            break;
                        }
                        
                        // If waitlisted at a DIFFERENT restaurant, block them.
                        // If waitlisted at THIS restaurant, allow them to try checking-in again if it's now Open.
                        if ("Waitlisted".equalsIgnoreCase(status) && !restaurantId.equals(rid)) {
                            hasActive = true;
                            break;
                        }
                    }

                    if (hasActive) {
                        showCustomDialog(R.drawable.baseline_privacy_tip_24,
                                "Active Booking Found",
                                "You already have a pending or active booking. Please complete or cancel it before booking a new table.",
                                "View History", () -> {
                                    startActivity(new Intent(UserRestaurantDetailsActivity.this, UserBookingHistoryActivity.class));
                                },
                                "Close", null);
                    } else {
                        showTableTypeSelectionDialog();
                    }
                });
    }

    private void showTableTypeSelectionDialog() {
        String[] options = {"Couple (2)", "Family (4)", "Group (6)", "Large (10)"};
        String[] keys = {"2", "4", "6", "10"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Select Group Size")
                .setItems(options, (d, w) -> {
                    String selectedKey = keys[w];
                    
                    long count = 0;
                    if (currentTableInventory.containsKey(selectedKey)) {
                        Long c = currentTableInventory.get(selectedKey);
                        if (c != null) count = c;
                    }
                    
                    boolean isWaitlist = "Full".equalsIgnoreCase(restaurantStatus) || count <= 0;
                    
                    // Direct Booking: Current time, no special requests
                    String currentTime = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
                    sendBookingRequest(selectedKey, currentTime, "", isWaitlist);
                })
                .show();
    }

    private void sendBookingRequest(String tableType, String timeSlot, String requests, boolean isWaitlistHint) {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        toggleCheckInLoading(true);

        firestore.collection("users").whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(userSnaps -> {
            String name = "User";
            String phone = "Not provided";
            if (!userSnaps.isEmpty()) {
                DocumentSnapshot userDoc = userSnaps.getDocuments().get(0);
                name = userDoc.getString("name") != null ? userDoc.getString("name") : "User";
                phone = userDoc.getString("phone") != null ? userDoc.getString("phone") : "Not provided";
            }

            final String finalName = name;
            final String finalPhone = phone;
            final String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // START TRANSACTION FOR "LAST SEAT LOCK"
            firestore.runTransaction(transaction -> {
                DocumentReference resRef = firestore.collection("restaurants").document(restaurantId);
                DocumentSnapshot resSnap = transaction.get(resRef);
                
                if (!resSnap.exists()) throw new RuntimeException("Restaurant not found");

                Map<String, Object> types = (Map<String, Object>) resSnap.get("tableTypes");
                long count = 0;
                if (types != null && types.containsKey(tableType)) {
                    Object tVal = types.get(tableType);
                    if (tVal instanceof Number) {
                        count = ((Number) tVal).longValue();
                    }
                }

                String status;
                String resStatus = resSnap.getString("status");
                
                // Real-time availability check inside transaction
                if (count > 0 && !"Closed".equalsIgnoreCase(resStatus) && !"Full".equalsIgnoreCase(resStatus)) {
                    status = "Pending";
                    // LOCK THE SEAT: Decrement immediately
                    count--;
                    types.put(tableType, count);
                    transaction.update(resRef, "tableTypes", types);
                    
                    long total = resSnap.getLong("noOfTables") != null ? resSnap.getLong("noOfTables") : 0;
                    long newTotal = Math.max(0, total - 1);
                    transaction.update(resRef, "noOfTables", newTotal);
                    
                    // Auto-update to Full if this was the last seat
                    if (newTotal <= 0) transaction.update(resRef, "status", "Full");
                    
                } else {
                    status = "Waitlisted";
                }

                Map<String, Object> booking = new HashMap<>();
                booking.put("userId", userId);
                booking.put("userName", finalName);
                booking.put("phoneNumber", finalPhone);
                booking.put("restaurantId", restaurantId);
                booking.put("restaurantName", resSnap.getString("name"));
                booking.put("tableType", tableType);
                booking.put("timeSlot", timeSlot);
                booking.put("specialRequests", requests);
                booking.put("status", status);
                booking.put("orderStatus", "none");
                booking.put("dateTime", dateTime);

                DocumentReference newBookingRef;
                if (currentBookingId != null && "Waitlisted".equalsIgnoreCase(currentBookingStatus)) {
                    newBookingRef = firestore.collection("bookings").document(currentBookingId);
                    transaction.set(newBookingRef, booking);
                } else {
                    newBookingRef = firestore.collection("bookings").document();
                    transaction.set(newBookingRef, booking);
                }
                
                return status;
            }).addOnSuccessListener(resultStatus -> {
                toggleCheckInLoading(false);
                if ("Waitlisted".equalsIgnoreCase(resultStatus)) {
                    Toast.makeText(this, "Table was taken! You are now on the waitlist.", Toast.LENGTH_LONG).show();
                } else {
                    vibrateSuccess();
                }
            }).addOnFailureListener(e -> {
                toggleCheckInLoading(false);
                Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            toggleCheckInLoading(false);
            Toast.makeText(this, "Failed to get user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleCheckInLoading(boolean isLoading) {
        if (isLoading) {
            if (selectTableButton != null) {
                selectTableButton.setText("");
                selectTableButton.setEnabled(false);
            }
            if (selectTableProgress != null) selectTableProgress.setVisibility(View.VISIBLE);
        } else {
            if (selectTableButton != null) {
                // Text is restored by updateUIBasedOnBooking inside updateRestaurantUI logic
                selectTableButton.setEnabled(true);
            }
            if (selectTableProgress != null) selectTableProgress.setVisibility(View.GONE);
            updateUIBasedOnBooking(currentBookingStatus);
        }
    }

    private void cancelBookingRequest() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;
        
        showLoading();
        firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) {
                        hideLoading();
                        return;
                    }
                    
                    // Transaction for accurate table restoration and status sync
                    firestore.runTransaction(transaction -> {
                        DocumentReference resRef = firestore.collection("restaurants").document(restaurantId);
                        DocumentSnapshot resSnap = transaction.get(resRef);

                        for (DocumentSnapshot d : snaps) {
                            String status = d.getString("status");
                            String tType = d.getString("tableType");
                            DocumentReference bookingRef = d.getReference();

                            boolean isReservationLocked = "Accepted".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status);

                            if (isReservationLocked) {
                                // Mark as Completed (History) or Cancelled and restore table
                                String finalStatus = "Accepted".equalsIgnoreCase(status) ? "Completed" : "Cancelled";
                                transaction.update(bookingRef, "status", finalStatus);
                                
                                if (tType != null && resSnap.exists()) {
                                    // Update specific table type count
                                    Map<String, Object> types = (Map<String, Object>) resSnap.get("tableTypes");
                                    if (types != null) {
                                        long current = (types.get(tType) instanceof Number) ? ((Number) types.get(tType)).longValue() : 0;
                                        types.put(tType, current + 1);
                                        transaction.update(resRef, "tableTypes", types);
                                    }

                                    // Update total count
                                    long total = resSnap.getLong("noOfTables") != null ? resSnap.getLong("noOfTables") : 0;
                                    long newTotal = total + 1;
                                    transaction.update(resRef, "noOfTables", newTotal);

                                    // AUTO STATUS: Restore to "Open" if it was "Full"
                                    String resStatus = resSnap.getString("status");
                                    if (newTotal > 0 && "Full".equalsIgnoreCase(resStatus)) {
                                        transaction.update(resRef, "status", "Open");
                                    }
                                }
                            } else {
                                // If already waitlisted or otherwise, just delete/cancel without table logic
                                transaction.delete(bookingRef);
                            }
                        }
                        return null;
                    }).addOnCompleteListener(task -> {
                        hideLoading();
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Action successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Action failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> hideLoading());
    }

    private void loadFoodItems() {
        firestore.collection("restaurants").document(restaurantId).collection("foodItems").get().addOnSuccessListener(snaps -> {
            foodItemList.clear();
            List<String> categories = new ArrayList<>();
            categories.add("All");
            
            for (DocumentSnapshot d : snaps) {
                CommonFoodItem item = d.toObject(CommonFoodItem.class);
                if (item != null) { 
                    item.setId(d.getId()); 
                    foodItemList.add(item);
                    String cat = item.getCategory();
                    if (cat != null && !cat.isEmpty() && !categories.contains(cat)) {
                        categories.add(cat);
                    }
                }
            }
            setupCategoryFilter(categories);
            filterFoodItems(searchBar.getText().toString());
        });
    }

    private void setupCategoryFilter(List<String> categories) {
        if (rvFoodCategories == null) return;
        rvFoodCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new UserCategoryAdapter(categories, category -> {
            selectedCategory = category;
            filterFoodItems(searchBar.getText().toString());
        });
        rvFoodCategories.setAdapter(categoryAdapter);
    }

    private void updateCartSummary(com.google.firebase.firestore.QuerySnapshot snaps) {
        if (snaps.isEmpty()) {
            layoutCartSummary.setVisibility(View.GONE);
            return;
        }

        int count = snaps.size();
        double total = 0;
        for (DocumentSnapshot doc : snaps) {
            Long price = doc.getLong("price");
            Long qty = doc.getLong("quantity");
            if (price != null) total += price * (qty != null ? qty : 1);
        }

        layoutCartSummary.setVisibility(View.VISIBLE);
        tvCartCount.setText(count + (count == 1 ? " Item" : " Items"));
        tvCartTotal.setText("₹" + (int)total);
    }

    private void filterFoodItems(String q) {
        List<CommonFoodItem> filtered = new ArrayList<>();
        String query = q.toLowerCase().trim();
        for (CommonFoodItem i : foodItemList) {
            boolean matchesSearch = i.getName().toLowerCase().contains(query);
            boolean matchesCategory = "All".equals(selectedCategory) || selectedCategory.equalsIgnoreCase(i.getCategory());
            
            boolean matchesDietary = true;
            if ("Veg".equals(selectedDietary)) {
            matchesDietary = i.isVeg();
        } else if ("Non-Veg".equals(selectedDietary)) {
            matchesDietary = !i.isVeg();
        }

            if (matchesSearch && matchesCategory && matchesDietary) {
                filtered.add(i);
            }
        }
        
        if (tvEmptyMenu != null) tvEmptyMenu.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        foodAdapter.updateFoodItemList(filtered);
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) { if (item.getItemId() == android.R.id.home) { finish(); return true; } return super.onOptionsItemSelected(item); }
    @Override protected void onDestroy() { super.onDestroy(); if (bookingListener != null) bookingListener.remove(); if (restaurantRealtimeListener != null) restaurantRealtimeListener.remove(); }
}
