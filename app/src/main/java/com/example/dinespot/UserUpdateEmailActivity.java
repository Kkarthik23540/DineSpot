package com.example.dinespot;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;

public class UserUpdateEmailActivity extends CommonBaseActivity {

    private TextInputEditText etNewEmail;
    private TextView tvCurrentEmail;
    private com.google.android.material.button.MaterialButton btnSend;
    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_update_email);

        setupToolbar("Update Email", true);

        etNewEmail = findViewById(R.id.et_new_email);
        tvCurrentEmail = findViewById(R.id.tv_current_email);
        btnSend = findViewById(R.id.btn_send_verification);
        progressBar = findViewById(R.id.progressBar);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvCurrentEmail.setText(user.getEmail());
        }

        btnSend.setOnClickListener(v -> {
            String newEmail = etNewEmail.getText().toString().trim();
            if (!TextUtils.isEmpty(newEmail)) {
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    sendVerification(newEmail);
                } else {
                    etNewEmail.setError("Invalid email format");
                }
            } else {
                etNewEmail.setError("Email is required");
            }
        });
    }

    private void sendVerification(String newEmail) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            if (user.isEmailVerified()) {
                Toast.makeText(this, "This email is already verified and currently in use.", Toast.LENGTH_SHORT).show();
            } else {
                toggleLoading(true);
                user.sendEmailVerification().addOnCompleteListener(v -> {
                    toggleLoading(false);
                    Toast.makeText(this, "Verification link re-sent to " + newEmail, Toast.LENGTH_LONG).show();
                });
            }
            return;
        }

        // Check for Google login
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            if (profile.getProviderId().equals("google.com")) {
                Toast.makeText(this, "Email update is not available for Google Sign-in users.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        toggleLoading(true);

        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            toggleLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Verification link sent to " + newEmail + ". Please verify to complete the update.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Exception e = task.getException();
                if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                    Toast.makeText(this, "Security sensitive action. Please logout and login again to update email.", Toast.LENGTH_LONG).show();
                } else {
                    String msg = e != null ? e.getMessage() : "Unknown error";
                    Toast.makeText(this, "Failed: " + msg, Toast.LENGTH_LONG).show();
                    android.util.Log.e("UpdateEmail", "Error: ", e);
                }
            }
        });
    }

    private void toggleLoading(boolean isLoading) {
        if (isLoading) {
            btnSend.setText("");
            btnSend.setEnabled(false);
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        } else {
            btnSend.setText("Send Verification Link");
            btnSend.setEnabled(true);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }
        etNewEmail.setEnabled(!isLoading);
    }
}
