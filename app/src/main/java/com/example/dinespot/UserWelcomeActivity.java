package com.example.dinespot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserWelcomeActivity extends CommonBaseActivity {

    private View logoCard, appTitle, appTagline, tvLoadingMsg, onboardingLayout, bottomArea;
    private Button btnGetStarted;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_welcome);

        prefs = getSharedPreferences("DineSpotPrefs", MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean("isFirstTime", true);

        logoCard = findViewById(R.id.logoCard);
        appTitle = findViewById(R.id.appTitle);
        appTagline = findViewById(R.id.appTagline);
        tvLoadingMsg = findViewById(R.id.tvLoadingMsg);
        onboardingLayout = findViewById(R.id.onboardingLayout);
        bottomArea = findViewById(R.id.bottomArea);
        btnGetStarted = findViewById(R.id.btnGetStarted);

        startEntranceAnimations();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Logged in: Fast track
            onboardingLayout.setVisibility(View.GONE);
            bottomArea.setVisibility(View.VISIBLE);
            new Handler(android.os.Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, 1200);
        } else if (isFirstTime) {
            // First time: Show Onboarding
            new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                bottomArea.setVisibility(View.GONE);
                onboardingLayout.setVisibility(View.VISIBLE);
                onboardingLayout.setAlpha(0f);
                onboardingLayout.animate().alpha(1f).setDuration(600).start();
            }, 3500);

            btnGetStarted.setOnClickListener(v -> {
                vibrateAction();
                prefs.edit().putBoolean("isFirstTime", false).apply();
                startActivity(new Intent(this, CommonBothLoginActivity.class));
                finish();
            });
        } else {
            // Returning unauthenticated
            onboardingLayout.setVisibility(View.GONE);
            bottomArea.setVisibility(View.VISIBLE);
            new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, CommonBothLoginActivity.class));
                finish();
            }, 1800);
        }
    }

    private void startEntranceAnimations() {
        logoCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(1000).setInterpolator(new AnticipateOvershootInterpolator()).start();
        appTitle.animate().alpha(1f).setStartDelay(400).setDuration(800).start();
        appTagline.animate().alpha(1f).setStartDelay(650).setDuration(800).start();
        tvLoadingMsg.animate().alpha(1f).setStartDelay(900).setDuration(800).start();
    }

    private void checkAuthAndNavigate() {
        if (isFinishing() || isDestroyed()) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            if (email == null) {
                navigateToUserHome();
                return;
            }

            FirebaseFirestore.getInstance().collection("admins")
                    .whereEqualTo("email", email.toLowerCase().trim())
                    .get()
                    .addOnSuccessListener(snaps -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (!snaps.isEmpty()) {
                            String adminName = snaps.getDocuments().get(0).getString("name");
                            if (adminName != null && tvLoadingMsg instanceof TextView) {
                                ((TextView)tvLoadingMsg).setText("Welcome back, " + adminName.split(" ")[0]);
                            }
                            vibrateAction();
                            new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                startActivity(new Intent(this, AdminDashboardActivity.class));
                                finish();
                            }, 500);
                        } else {
                            navigateToUserHome();
                        }
                    })
                    .addOnFailureListener(e -> navigateToUserHome());
        } else {
            startActivity(new Intent(this, CommonBothLoginActivity.class));
            finish();
        }
    }

    private void navigateToUserHome() {
        vibrateAction();
        startActivity(new Intent(this, UserHomeActivity.class));
        finish();
    }
}
