package com.example.dinespot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileActivity extends CommonBaseActivity {

    private EditText etUserName, etUserPhone;
    private TextView tvDisplayNameHeader, tvDisplayEmailHeader, tvUserIdHeader, tvJoinedDate;
    private ImageButton btnEditName, btnEditPhone;
    private ImageView ivTermsArrow;
    private View layoutTermsExpand, tvTermsContent;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button btnLogout, btnDeleteAccount;
    private SwitchMaterial switchLocation, switchCamera, switchNotifications;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isTermsExpanded = false;

    private final androidx.activity.result.ActivityResultLauncher<String> locationPermissionLauncher = 
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
        updatePermissionSwitches();
        if (!isGranted) Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
    });

    private final androidx.activity.result.ActivityResultLauncher<String> cameraPermissionLauncher = 
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
        updatePermissionSwitches();
        if (!isGranted) Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
    });

    private final androidx.activity.result.ActivityResultLauncher<String> notificationPermissionLauncher = 
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
        updatePermissionSwitches();
        if (!isGranted) Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
    });

    private ImageView btnThemeToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupToolbar("My Profile", false);
        initViews();
        setupBottomNavigation();

        btnThemeToggle.setOnClickListener(v -> toggleTheme());

        populateUserDetails();
        updatePermissionSwitches();
        updateThemeIcon();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                populateUserDetails();
                syncEmailWithFirestore();
            });
        }

        btnEditName.setOnClickListener(v -> toggleEdit(etUserName, btnEditName, "name"));
        btnEditPhone.setOnClickListener(v -> toggleEdit(etUserPhone, btnEditPhone, "phone"));

        findViewById(R.id.tv_send_feedback).setOnClickListener(v -> 
            startActivity(new Intent(this, UserFeedbackActivity.class)));

        layoutTermsExpand.setOnClickListener(v -> toggleTerms());
        
        findViewById(R.id.tv_booking_history).setOnClickListener(v -> {
            Intent intent = new Intent(this, UserBookingHistoryActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.tv_support_chat).setOnClickListener(v -> {
            Intent intent = new Intent(this, UserChatActivity.class);
            intent.putExtra("bookingId", "support_" + mAuth.getUid());
            intent.putExtra("chatTitle", "App Support");
            startActivity(intent);
        });

        findViewById(R.id.tv_change_email).setOnClickListener(v -> showUpdateEmailDialog());
        findViewById(R.id.tv_change_password).setOnClickListener(v -> showUpdatePasswordDialog());

        findViewById(R.id.tv_call_us).setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:7893176982")));
        });

        findViewById(R.id.tv_email_us).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:dinespot15@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request");
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionSwitches();
        syncEmailWithFirestore();
    }

    private void syncEmailWithFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.isEmailVerified()) {
            String currentAuthEmail = user.getEmail();
            db.collection("users").whereEqualTo("email", currentAuthEmail).get().addOnSuccessListener(snaps -> {
                if (!snaps.isEmpty()) {
                    com.google.firebase.firestore.DocumentReference ref = snaps.getDocuments().get(0).getReference();
                    String firestoreEmail = snaps.getDocuments().get(0).getString("email");
                    if (currentAuthEmail != null && !currentAuthEmail.equals(firestoreEmail)) {
                        ref.update("email", currentAuthEmail);
                        tvDisplayEmailHeader.setText(currentAuthEmail);
                    }
                }
            });
        }
    }

    private void toggleTheme() {
        int currentMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    private void updateThemeIcon() {
        int currentMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            btnThemeToggle.setImageResource(R.drawable.ic_sun);
        } else {
            btnThemeToggle.setImageResource(R.drawable.ic_moon);
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
        Toast.makeText(this, "Please manage permissions in Settings", Toast.LENGTH_SHORT).show();
    }

    private void updatePermissionSwitches() {
        // Remove listeners temporarily to avoid loops
        switchLocation.setOnCheckedChangeListener(null);
        switchCamera.setOnCheckedChangeListener(null);
        switchNotifications.setOnCheckedChangeListener(null);

        // Location
        boolean hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        switchLocation.setChecked(hasLocation);

        // Camera
        boolean hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        switchCamera.setChecked(hasCamera);

        // Notifications
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            boolean hasNotify = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            switchNotifications.setChecked(hasNotify);
        } else {
            switchNotifications.setChecked(androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled());
        }

        // Re-attach listeners
        attachPermissionListeners();
    }

    private void attachPermissionListeners() {
        switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            } else {
                handlePermissionRevoke(Manifest.permission.ACCESS_FINE_LOCATION, "Location", switchLocation);
            }
        });

        switchCamera.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            } else {
                handlePermissionRevoke(Manifest.permission.CAMERA, "Camera", switchCamera);
            }
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                } else {
                    handlePermissionRevoke(Manifest.permission.POST_NOTIFICATIONS, "Notifications", switchNotifications);
                }
            } else {
                if (!isChecked) openAppSettings();
            }
        });
    }

    private void handlePermissionRevoke(String permission, String label, SwitchMaterial toggle) {
        // Modern Android security doesn't allow apps to silently turn OFF permissions 
        // without a user going to settings (except in API 33+ which can be restricted).
        // To follow your request of removing the dialog, we will use a simple Toast 
        // and guide the user to the right place.
        
        Toast.makeText(this, "To turn off " + label + ", please use App Info > Permissions.", Toast.LENGTH_LONG).show();
        
        // Reset the toggle visually so it remains accurate to the system state
        updatePermissionSwitches();
    }

    private void initViews() {
        etUserName = findViewById(R.id.et_user_name);
        etUserPhone = findViewById(R.id.et_user_phone);
        tvDisplayNameHeader = findViewById(R.id.tv_display_name_header);
        tvDisplayEmailHeader = findViewById(R.id.tv_display_email_header);
        tvUserIdHeader = findViewById(R.id.tv_user_id_header);
        tvJoinedDate = findViewById(R.id.tv_joined_date);

        btnEditName = findViewById(R.id.btn_edit_name);
        btnEditPhone = findViewById(R.id.btn_edit_phone);
        layoutTermsExpand = findViewById(R.id.layout_terms_expand);
        tvTermsContent = findViewById(R.id.tv_terms_content);
        ivTermsArrow = findViewById(R.id.iv_terms_arrow);
        btnLogout = findViewById(R.id.btn_logout);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);
        switchLocation = findViewById(R.id.switch_location);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchCamera = findViewById(R.id.switch_camera);
        btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bnv = findViewById(R.id.bottom_navigation);
        bnv.inflateMenu(R.menu.bottom_nav_menu);
        CommonNavigationHelper.setupUserNavigation(this, bnv, R.id.menu_profile);

        bnv.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.menu_profile) {
                populateUserDetails();
            }
        });
    }

    private void toggleTerms() {
        isTermsExpanded = !isTermsExpanded;
        android.transition.TransitionManager.beginDelayedTransition((ViewGroup) layoutTermsExpand.getParent());
        tvTermsContent.setVisibility(isTermsExpanded ? View.VISIBLE : View.GONE);
        ivTermsArrow.animate().rotation(isTermsExpanded ? 180 : 0).setDuration(300).start();
    }

    private void populateUserDetails() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        db.collection("users").whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(snaps -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (!snaps.isEmpty()) {
                com.google.firebase.firestore.DocumentSnapshot doc = snaps.getDocuments().get(0);
                String name = doc.getString("name");
                String email = doc.getString("email");
                String phone = doc.getString("phone");
                String userIdVal = doc.getString("userId");
                if (userIdVal == null) userIdVal = doc.getString("customId"); // Fallback for old records
                String joinedAt = doc.getString("joinedAt");
                
                // Fallback to Auth email if firestore is outdated
                if (email == null || (user.isEmailVerified() && !email.equals(user.getEmail()))) {
                    email = user.getEmail();
                }

                tvDisplayNameHeader.setText(name != null ? name : "User");
                tvDisplayEmailHeader.setText(email != null ? email : "");
                
                if (tvUserIdHeader != null) {
                    tvUserIdHeader.setText(userIdVal != null ? userIdVal : "DS-USER-00");
                }

                if (tvJoinedDate != null) {
                    tvJoinedDate.setText(formatJoinedDate(joinedAt));
                }
                etUserName.setText(name != null ? name : "");
                etUserPhone.setText(phone != null ? (phone.isEmpty() ? "Not Provided" : phone) : "Not Provided");
            }
        }).addOnFailureListener(e -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        });
    }

    private String formatJoinedDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "Not Available";
        try {
            // Input format: 2024-08-05 23:06:50
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(rawDate);
            
            // Output format: 05 Aug 2024
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return date != null ? outputFormat.format(date) : "Not Available";
        } catch (Exception e) {
            return rawDate; // Fallback to raw if parsing fails
        }
    }

    private void toggleEdit(EditText editText, ImageButton button, String field) {
        if (!editText.isEnabled()) {
            editText.setEnabled(true);
            editText.requestFocus();
            button.setImageResource(R.drawable.baseline_done_24);
        } else {
            String value = editText.getText().toString().trim();
            if (TextUtils.isEmpty(value)) {
                Toast.makeText(this, "Field cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUserField(field, value);
            editText.setEnabled(false);
            button.setImageResource(R.drawable.baseline_edit_24);
        }
    }

    private void updateUserField(String field, String value) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            showLoading();
            db.collection("users").whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(snaps -> {
                if (!snaps.isEmpty()) {
                    snaps.getDocuments().get(0).getReference().update(field, value).addOnSuccessListener(aVoid -> {
                        if (field.equals("name")) {
                            tvDisplayNameHeader.setText(value);
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(value)
                                    .build();
                            user.updateProfile(profileUpdates);
                        }
                        hideLoading();
                        Snackbar.make(findViewById(android.R.id.content), "Updated", Snackbar.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        hideLoading();
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void submitFeedback() {
        // Method moved to UserFeedbackActivity
    }

    private void showUpdateEmailDialog() {
        startActivity(new Intent(this, UserUpdateEmailActivity.class));
    }

    private void showUpdatePasswordDialog() {
        startActivity(new Intent(this, UserForgotPasswordActivity.class));
    }

    @android.annotation.SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, UserHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }

    private void showLogoutConfirmationDialog() {
        showCustomDialog(R.drawable.baseline_logout_24, 
                "Sign Out", 
                "Are you sure you want to sign out from DineSpot?", 
                "Sign Out", () -> {
                    mAuth.signOut();
                    startActivity(new Intent(this, CommonBothLoginActivity.class));
                    finishAffinity();
                }, 
                "Cancel", null);
    }

    private void showDeleteAccountConfirmation() {
        showCustomDialog(R.drawable.baseline_privacy_tip_24, 
                "Delete Account?", 
                "This action is permanent. All your profile data, bookings, and cart items will be deleted forever.", 
                "Delete Permanently", () -> deleteUserAccount(), 
                "Cancel", null);
    }

    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        showLoading();

        // RE-ORDERED: Try to delete auth account FIRST. 
        // This requires recent login and will fail if user session is old.
        user.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Now that Auth is gone, clean up Firestore
                db.collection("users").whereEqualTo("email", user.getEmail()).get().addOnSuccessListener(userSnaps -> {
                    if (!userSnaps.isEmpty()) {
                        userSnaps.getDocuments().get(0).getReference().delete().addOnSuccessListener(v -> {
                            db.collection("bookings").whereEqualTo("userId", uid).get().addOnSuccessListener(snaps -> {
                                for (QueryDocumentSnapshot doc : snaps) {
                                    doc.getReference().delete();
                                }
                                hideLoading();
                                Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(this, UserWelcomeActivity.class));
                                finishAffinity();
                            });
                        });
                    } else {
                        hideLoading();
                        startActivity(new Intent(this, UserWelcomeActivity.class));
                        finishAffinity();
                    }
                });
            } else {
                hideLoading();
                Exception e = task.getException();
                if (e instanceof com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    showCustomDialog(R.drawable.baseline_lock_reset_24,
                            "Re-authentication Required",
                            "For security, you must log out and log back in before you can delete your account.",
                            "Logout Now", () -> {
                                mAuth.signOut();
                                startActivity(new Intent(UserProfileActivity.this, CommonBothLoginActivity.class));
                                finishAffinity();
                            },
                            "Later", null);
                } else {
                    Toast.makeText(this, "Deletion failed: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
