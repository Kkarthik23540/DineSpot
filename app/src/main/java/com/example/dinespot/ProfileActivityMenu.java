package com.example.dinespot;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProfileActivityMenu extends AppCompatActivity {

    private EditText etUserName, etUserEmail, etUserPhone, etFeedback;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button btnLogout, btnSubmitFeedback;
    private Menu menu;
    private boolean isEditing = false;
    private boolean hasChanges = false;

    private RecyclerView rvOrderHistory;
    private OrderHistoryAdapter orderHistoryAdapter;
    private List<Order> orderHistoryList;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_menu);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        Toolbar toolbar = findViewById(R.id.toolbar);
        etUserName = findViewById(R.id.et_user_name);
        etUserEmail = findViewById(R.id.et_user_email);
        etUserPhone = findViewById(R.id.et_user_phone);
        etFeedback = findViewById(R.id.et_feedback);
        btnLogout = findViewById(R.id.btn_logout);
        btnSubmitFeedback = findViewById(R.id.btn_submit_feedback);
        rvOrderHistory = findViewById(R.id.rvOrderHistory);
        progressBar = findViewById(R.id.progressBar);

        // Set Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        populateUserDetails();
        toggleEditingMode();
        addFieldChangeListener();
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        initOrderHistoryRecyclerView();
        fetchOrderHistory();
        TextView tvNeedHelp = findViewById(R.id.tv_need_help);
        tvNeedHelp.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:7893176982"));
            startActivity(callIntent);
        });

        TextView tvContactUs = findViewById(R.id.tv_contact_us);
        tvContactUs.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"dinespot15@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Contact Us Inquiry");

            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(emailIntent);
            } else {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        TextView tvTermsConditions = findViewById(R.id.tv_terms_conditions);
        tvTermsConditions.setOnClickListener(v -> showTermsConditionsDialog());

        // Feedback Button Listener
        btnSubmitFeedback.setOnClickListener(v -> submitFeedback());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        this.menu = menu; // Save menu reference
        updateEditSaveMenuItem();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            if (isEditing && hasChanges) {
                saveUserDetails();
                isEditing = false;
                hasChanges = false;
            } else {
                isEditing = true;
            }

            updateEditSaveMenuItem();
            toggleEditingMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateEditSaveMenuItem() {
        if (menu != null) {
            MenuItem editItem = menu.findItem(R.id.action_edit);
            if (editItem != null) {
                if (isEditing && hasChanges) {
                    editItem.setTitle("Save");
                } else if (isEditing) {
                    editItem.setTitle("Edit");
                } else {
                    editItem.setTitle("Edit");
                }
            }
        }
    }

    private void toggleEditingMode() {
        etUserName.setEnabled(isEditing);
        etUserPhone.setEnabled(false);
        etUserEmail.setEnabled(false);

        if (isEditing) {
            etUserName.requestFocus();
        }
    }

    private void addFieldChangeListener() {
        etUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanges = true;
                updateEditSaveMenuItem();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void populateUserDetails() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            progressBar.setVisibility(View.VISIBLE);

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        progressBar.setVisibility(View.GONE);

                        if (documentSnapshot.exists()) {
                            String userName = documentSnapshot.getString("name");
                            String userEmail = documentSnapshot.getString("email");
                            String userPhone = documentSnapshot.getString("phone");

                            etUserName.setText(userName != null ? userName : "Unknown User");
                            etUserEmail.setText(userEmail != null ? userEmail : "No Email Found");
                            etUserPhone.setText(userPhone != null ? userPhone : "Not Provided");
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Failed to load user details.",
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void saveUserDetails() {
        String updatedName = etUserName.getText().toString();

        if (TextUtils.isEmpty(updatedName)) {
            etUserName.setError("Name cannot be empty");
            return;
        }

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            db.collection("users").document(userId)
                    .update("name", updatedName)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this,
                            "Profile updated successfully.",
                            Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Failed to update profile. Please try again.",
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {

                    mAuth.signOut();
                    Intent intent = new Intent(this, BothLogin.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void initOrderHistoryRecyclerView() {
        orderHistoryList = new ArrayList<>();
        orderHistoryAdapter = new OrderHistoryAdapter(orderHistoryList);
        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        rvOrderHistory.setAdapter(orderHistoryAdapter);
    }

    private void fetchOrderHistory() {
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .document(userId)
                .collection("orders")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    orderHistoryList.clear();
                    if (querySnapshot.isEmpty()) {
                        findViewById(R.id.tv_no_orders).setVisibility(View.VISIBLE);
                        rvOrderHistory.setVisibility(View.GONE);
                    } else {
                        findViewById(R.id.tv_no_orders).setVisibility(View.GONE);
                        rvOrderHistory.setVisibility(View.VISIBLE);

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Order order = document.toObject(Order.class);
                            orderHistoryList.add(order);
                        }
                        orderHistoryAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Failed to load order history.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showTermsConditionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Terms and Conditions")
                .setMessage("Welcome to DineSpot!\n\n" +
                        "Here’s what you need to know:\n\n" +
                        "1. You can browse restaurants, order food, and pay through the app.\n" +
                        "2. Please sign up or log in to use these features.\n" +
                        "3. Payments are safe and handled by trusted partners.\n" +
                        "4. We’re not responsible for mistakes made by restaurants.\n" +
                        "5. Follow local laws when using the app.\n\n" +
                        "Thanks for using DineSpot!")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void submitFeedback() {
        String feedback = etFeedback.getText().toString().trim();

        if (TextUtils.isEmpty(feedback)) {
            Toast.makeText(this, "Please provide feedback before submitting.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            // Get user details (name and email)
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userName = documentSnapshot.getString("name");
                            String userEmail = documentSnapshot.getString("email");

                            // Format the current date and time
                            String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                            // Create Feedback object with name, email, feedback, and formatted timestamp
                            Feedback feedbackData = new Feedback(feedback, userName, userEmail, dateTime);

                            // Store feedback in the "feedback" collection
                            db.collection("feedback")
                                    .document(userId)
                                    .set(feedbackData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                                        etFeedback.setText("");
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to submit feedback. Please try again.", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(this, "User details not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to retrieve user details.", Toast.LENGTH_SHORT).show();
                    });
        }
    }


}
