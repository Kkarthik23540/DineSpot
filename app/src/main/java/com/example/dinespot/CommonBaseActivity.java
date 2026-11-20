package com.example.dinespot;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class CommonBaseActivity extends AppCompatActivity {

    protected CommonNetworkMonitor networkMonitor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNetworkMonitor();
    }

    protected void setLightStatusBar(boolean isLight) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                if (isLight) {
                    controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                } else {
                    controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (isLight) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    protected void showLoading() {}
    protected void hideLoading() {}

    protected void vibrateAction() {
        View decor = getWindow().getDecorView();
        if (decor != null) {
            decor.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    protected void vibrateSuccess() {
        View decor = getWindow().getDecorView();
        if (decor != null) {
            decor.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }
    }

    protected void logAdminAction(String action, String target) {
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        if (admin == null) return;
        
        java.util.Map<String, Object> log = new java.util.HashMap<>();
        log.put("adminEmail", admin.getEmail());
        log.put("action", action);
        log.put("target", target);
        log.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        
        FirebaseFirestore.getInstance().collection("audit_logs").add(log);
    }

    protected void setupToolbar(String title, boolean showBackButton) {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setDisplayHomeAsUpEnabled(showBackButton);
            }
            if (showBackButton) {
                toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
            }
        }
    }

    protected void showCustomDialog(int iconRes, String title, String message, String posText, Runnable posAction, String negText, Runnable negAction) {
        View view = getLayoutInflater().inflate(R.layout.common_dialog_custom_alert, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivIcon = view.findViewById(R.id.dialog_icon);
        TextView tvTitle = view.findViewById(R.id.dialog_title);
        TextView tvMsg = view.findViewById(R.id.dialog_message);
        MaterialButton btnPos = view.findViewById(R.id.btn_positive);
        MaterialButton btnNeg = view.findViewById(R.id.btn_negative);

        if (iconRes != 0) ivIcon.setImageResource(iconRes);
        else ivIcon.setVisibility(View.GONE);

        tvTitle.setText(title);
        tvMsg.setText(message);
        btnPos.setText(posText);

        btnPos.setOnClickListener(v -> {
            dialog.dismiss();
            if (posAction != null) posAction.run();
        });

        if (negText != null) {
            btnNeg.setText(negText);
            btnNeg.setOnClickListener(v -> {
                dialog.dismiss();
                if (negAction != null) negAction.run();
            });
        } else {
            btnNeg.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void setupNetworkMonitor() {
        networkMonitor = new CommonNetworkMonitor(this, new CommonNetworkMonitor.NetworkStateListener() {
            @Override public void onNetworkAvailable() {}
            @Override public void onNetworkLost() {}
        });
        networkMonitor.start();
    }

    @Override
    protected void onDestroy() {
        if (networkMonitor != null) networkMonitor.stop();
        super.onDestroy();
    }
}
