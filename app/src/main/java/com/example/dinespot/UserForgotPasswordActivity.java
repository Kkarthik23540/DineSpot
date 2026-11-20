package com.example.dinespot;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class UserForgotPasswordActivity extends CommonBaseActivity {

    private EditText emailEditText;
    private android.widget.Button resetPasswordButton;
    private android.widget.ProgressBar resetProgressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_forgot_password);

        setupToolbar("Forgot Password", true);

        emailEditText = findViewById(R.id.emailEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        resetProgressBar = findViewById(R.id.resetProgressBar);

        mAuth = FirebaseAuth.getInstance();

        resetPasswordButton.setOnClickListener(v -> handlePasswordReset());
    }

    private void handlePasswordReset() {
        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return;
        }

        toggleLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    toggleLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(UserForgotPasswordActivity.this,
                                "Password reset email sent!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Error occurred";
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(UserForgotPasswordActivity.this,
                                    "This email is not registered. Please sign up first.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UserForgotPasswordActivity.this,
                                    "Failed to send reset email: " + errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void toggleLoading(boolean isLoading) {
        if (isLoading) {
            resetPasswordButton.setText("");
            resetPasswordButton.setEnabled(false);
            if (resetProgressBar != null) resetProgressBar.setVisibility(View.VISIBLE);
        } else {
            resetPasswordButton.setText("Send Reset Link");
            resetPasswordButton.setEnabled(true);
            if (resetProgressBar != null) resetProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
