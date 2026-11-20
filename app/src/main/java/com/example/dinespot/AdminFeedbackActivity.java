package com.example.dinespot;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminFeedbackActivity extends CommonBaseActivity {

    private RecyclerView rvFeedback;
    private AdminFeedbackAdapter feedbackAdapter;
    private final List<CommonFeedback> feedbackList = new ArrayList<>();
    private final List<CommonFeedback> fullFeedbackList = new ArrayList<>();
    private ProgressBar progressBar;
    private View tvNoFeedback, shimmerView;
    private View searchCard;
    private FirebaseFirestore db;
    private EditText searchBar;
    private View filterAll, filter5, filter4, filter3;
    private String currentSearchQuery = "";
    private int selectedRatingFilter = 0; // 0=All, 5, 4 (means >=4), 3 (means <4)
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListenerRegistration feedbackListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_feedback);

        setupToolbar("Customer Feedback", true);

        rvFeedback = findViewById(R.id.rv_feedback);
        progressBar = findViewById(R.id.progress_bar);
        tvNoFeedback = findViewById(R.id.tv_no_feedback);
        searchBar = findViewById(R.id.searchBar);
        searchCard = findViewById(R.id.searchCard);
        shimmerView = findViewById(R.id.shimmer_view_container);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        filterAll = findViewById(R.id.filter_all);
        filter5 = findViewById(R.id.filter_5);
        filter4 = findViewById(R.id.filter_4);
        filter3 = findViewById(R.id.filter_3);

        db = FirebaseFirestore.getInstance();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchFeedback);
        }

        rvFeedback.setLayoutManager(new LinearLayoutManager(this));
        feedbackAdapter = new AdminFeedbackAdapter(feedbackList, null);
        rvFeedback.setAdapter(feedbackAdapter);

        setupSearch();
        setupFilters();
        fetchFeedback();
    }

    private void setupFilters() {
        View.OnClickListener clickListener = v -> {
            resetFilterUI();
            MaterialCardView card = (MaterialCardView) v;
            TextView tv = (TextView) card.getChildAt(0);

            card.setStrokeColor(Color.WHITE);
            card.setCardBackgroundColor(Color.WHITE);
            tv.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

            int id = v.getId();
            if (id == R.id.filter_all) selectedRatingFilter = 0;
            else if (id == R.id.filter_5) selectedRatingFilter = 5;
            else if (id == R.id.filter_4) selectedRatingFilter = 4;
            else if (id == R.id.filter_3) selectedRatingFilter = 3;

            filterFeedback(searchBar.getText().toString());
        };

        filterAll.setOnClickListener(clickListener);
        filter5.setOnClickListener(clickListener);
        filter4.setOnClickListener(clickListener);
        filter3.setOnClickListener(clickListener);
    }

    private void resetFilterUI() {
        int[] ids = {R.id.filter_all, R.id.filter_5, R.id.filter_4, R.id.filter_3};
        for (int id : ids) {
            MaterialCardView card = findViewById(id);
            if (card != null) {
                TextView tv = (TextView) card.getChildAt(0);
                card.setStrokeColor(Color.parseColor("#4DFFFFFF"));
                card.setCardBackgroundColor(Color.TRANSPARENT);
                tv.setTextColor(Color.WHITE);
            }
        }
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                filterFeedback(currentSearchQuery);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.admin_feedback_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        android.view.MenuItem clearItem = menu.findItem(R.id.clear_all_feedback);
        if (clearItem != null) {
            clearItem.setVisible(!fullFeedbackList.isEmpty());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.clear_all_feedback) {
            showCustomDialog(R.drawable.baseline_delete_outline_24,
                    "Clear Feedback",
                    "Are you sure you want to delete all customer feedback permanently?",
                    "Clear All", this::clearAllFeedback,
                    "Cancel", null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearAllFeedback() {
        if (fullFeedbackList.isEmpty()) return;
        showLoading();
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (CommonFeedback f : fullFeedbackList) {
            batch.delete(db.collection("feedback").document(f.getId()));
        }
        batch.commit().addOnCompleteListener(task -> {
            hideLoading();
            if (task.isSuccessful()) {
                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Feedback History Cleared", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterFeedback(String query) {
        String lowerQuery = query.toLowerCase().trim();
        feedbackList.clear();
        
        for (CommonFeedback f : fullFeedbackList) {
            boolean matchesSearch = f.getName().toLowerCase().contains(lowerQuery) || 
                                   f.getFeedback().toLowerCase().contains(lowerQuery);
            
            boolean matchesRating = true;
            double r = f.getRating();
            if (selectedRatingFilter == 5) matchesRating = r == 5.0;
            else if (selectedRatingFilter == 4) matchesRating = r >= 4.0;
            else if (selectedRatingFilter == 3) matchesRating = r < 4.0;

            if (matchesSearch && matchesRating) {
                feedbackList.add(f);
            }
        }
        
        feedbackAdapter.notifyDataSetChanged();
        
        boolean hasFeedback = !fullFeedbackList.isEmpty();
        if (searchCard != null) searchCard.setVisibility(hasFeedback ? View.VISIBLE : View.GONE);
        
        if (tvNoFeedback != null) {
            tvNoFeedback.setVisibility(feedbackList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        invalidateOptionsMenu();
    }

    private void fetchFeedback() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        if (feedbackListener != null) feedbackListener.remove();
        
        feedbackListener = db.collection("feedback")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (shimmerView != null) shimmerView.setVisibility(View.GONE);
                    if (e != null) {
                        return;
                    }
                    
                    if (queryDocumentSnapshots != null) {
                        fullFeedbackList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                CommonFeedback f = document.toObject(CommonFeedback.class);
                                f.setId(document.getId());
                                if (f.getFeedback() == null) f.setFeedback("No content");
                                if (f.getName() == null) f.setName("Anonymous");
                                fullFeedbackList.add(f);
                            } catch (Exception ex) {
                                // Skip invalid entries
                            }
                        }
                        
                        filterFeedback(searchBar.getText().toString());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (feedbackListener != null) feedbackListener.remove();
        super.onDestroy();
    }
}
