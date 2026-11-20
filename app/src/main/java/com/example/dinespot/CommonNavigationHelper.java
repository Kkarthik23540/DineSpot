package com.example.dinespot;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CommonNavigationHelper {

    public static void setupUserNavigation(Activity activity, BottomNavigationView bnv, int selectedItemId) {
        if (bnv == null) return;
        
        if (selectedItemId != 0) {
            try {
                bnv.setSelectedItemId(selectedItemId);
            } catch (Exception e) {}
        }
        bnv.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == selectedItemId) return true;

            Intent intent = null;
            if (itemId == R.id.menu_home) {
                intent = new Intent(activity, UserHomeActivity.class);
            } else if (itemId == R.id.menu_bookings) {
                intent = new Intent(activity, UserBookingHistoryActivity.class);
            } else if (itemId == R.id.menu_cart) {
                intent = new Intent(activity, UserCartActivity.class);
            } else if (itemId == R.id.menu_profile) {
                intent = new Intent(activity, UserProfileActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return false; // Return false to prevent the current activity from updating its visual selection
            }
            return false;
        });
    }

    public static void setupAdminNavigation(Activity activity, BottomNavigationView bnv, int selectedItemId) {
        if (bnv == null) return;

        if (selectedItemId != 0) {
            try {
                bnv.setSelectedItemId(selectedItemId);
            } catch (Exception e) {}
        }
        bnv.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == selectedItemId) return true;

            Intent intent = null;
            if (itemId == R.id.admin_home) {
                intent = new Intent(activity, AdminDashboardActivity.class);
            } else if (itemId == R.id.admin_manage) {
                intent = new Intent(activity, AdminManageRestaurantsActivity.class);
            } else if (itemId == R.id.admin_analytics) {
                intent = new Intent(activity, AdminAnalyticsActivity.class);
            } else if (itemId == R.id.admin_profile) {
                intent = new Intent(activity, AdminProfileActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return false; // Return false to prevent the current activity from updating its visual selection
            }
            return false;
        });
    }
}
