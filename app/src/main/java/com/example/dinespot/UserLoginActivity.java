package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserLoginActivity extends CommonBaseActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signupButton;
    private TextView forgotPasswordText;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null) {
                        firebaseAuthWithGoogle(account);
                    }
                } catch (ApiException e) {
                    int statusCode = e.getStatusCode();
                    String errorMessage = "Google sign in failed. Code: " + statusCode;
                    if (statusCode == 10 || statusCode == 12500) {
                        errorMessage = "Configuration Error (Code " + statusCode + "). Please check:\n1. SHA-1 fingerprint in Firebase Console\n2. Web Client ID matches strings.xml\n3. Support email is set in Firebase settings.";
                    } else if (statusCode == 7) {
                        errorMessage = "Network Error. Please check your internet connection.";
                    } else if (statusCode == 12501) {
                        errorMessage = "Sign-in cancelled. Note: This can also happen if SHA-1 is missing in Firebase Console or there is a configuration mismatch.";
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    android.util.Log.e("UserLoginActivity", "Google Sign-In failed. Error Code: " + statusCode, e);
                }
            }
    );

    private void logSHA1() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    android.content.pm.PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA1");
                md.update(signature.toByteArray());
                byte[] digest = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02X:", b));
                }
                android.util.Log.d("SHA1_KEY", sb.toString());
            }
        } catch (Exception e) {
            android.util.Log.e("SHA1_KEY", "Error getting SHA1", e);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_login);

        mAuth = FirebaseAuth.getInstance();
        logSHA1();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        progressBar = findViewById(R.id.loginProgressBar);

        SignInButton googleSignInButton = findViewById(R.id.googleSignInButton);
        googleSignInButton.setSize(SignInButton.SIZE_WIDE);
        googleSignInButton.setColorScheme(SignInButton.COLOR_LIGHT);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        loginButton.setOnClickListener(v -> loginUser());

        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserLoginActivity.this, UserSignUpActivity.class);
            startActivity(intent);
        });

        forgotPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(UserLoginActivity.this, UserForgotPasswordActivity.class);
            startActivity(intent);
        });

        CommonAnimationHelper.applyClickAnimation(loginButton);
        CommonAnimationHelper.applyClickAnimation(findViewById(R.id.googleSignInButton));
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        showLoading();
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveGoogleUserToFirestore(user);
                        }
                    } else {
                        Toast.makeText(UserLoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveGoogleUserToFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", user.getDisplayName());
        userData.put("email", user.getEmail());
        
        // Ensure new users get a role
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                userData.put("role", "user");
            }
            db.collection("users").document(user.getUid())
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(UserLoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateAfterLogin();
                    })
                    .addOnFailureListener(e -> navigateAfterLogin());
        });
    }

    private void navigateAfterLogin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail() != null ? currentUser.getEmail().toLowerCase().trim() : "";
            
            FirebaseFirestore.getInstance().collection("users").whereEqualTo("email", email).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Valid User, navigate to Home
                            startActivity(new Intent(UserLoginActivity.this, UserHomeActivity.class));
                            finish();
                        } else {
                            // Record missing, though Auth exists (Rare case)
                            mAuth.signOut();
                            Toast.makeText(this, "Profile not found. Please register again.", Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        toggleLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    toggleLoading(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            validateUserAccess(user.getEmail());
                        }
                    } else {
                        toggleLoading(false);
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed!";
                        Toast.makeText(UserLoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void validateUserAccess(String email) {
        String cleanEmail = email.toLowerCase().trim();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Check if email is in Admins collection (Silently block)
        db.collection("admins").whereEqualTo("email", cleanEmail).get()
                .addOnSuccessListener(adminSnaps -> {
                    if (!adminSnaps.isEmpty()) {
                        handleAuthFailure("Invalid email or password."); // Generic message for security
                    } else {
                        // 2. Check if email is in Users collection (Email based query since doc ID is name)
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            db.collection("users").whereEqualTo("email", cleanEmail).get()
                                    .addOnSuccessListener(userSnaps -> {
                                        if (!userSnaps.isEmpty() && user.isEmailVerified()) {
                                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                                            navigateAfterLogin();
                                        } else if (!userSnaps.isEmpty()) {
                                            handleAuthFailure("Please verify your email address first. Check your inbox.");
                                        } else {
                                            handleAuthFailure("Invalid email or password.");
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> handleAuthFailure("Authentication failed."));
    }

    private void handleAuthFailure(String msg) {
        if (mAuth != null) mAuth.signOut();
        toggleLoading(false);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void toggleLoading(boolean isLoading) {
        if (isLoading) {
            loginButton.setText("");
            loginButton.setEnabled(false);
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        } else {
            loginButton.setText("Sign In");
            loginButton.setEnabled(true);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }
    }
}
