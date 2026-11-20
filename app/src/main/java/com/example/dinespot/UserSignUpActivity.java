package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class UserSignUpActivity extends CommonBaseActivity {

    private EditText nameEditText, emailEditText, phoneEditText, passwordEditText, confirmPasswordEditText;
    private Button signUpButton;
    private TextView loginTextView;
    private android.widget.ProgressBar signUpProgressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null) {
                        firebaseAuthWithGoogle(account.getIdToken());
                    }
                } catch (ApiException e) {
                    Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        loginTextView = findViewById(R.id.loginTextView);
        signUpProgressBar = findViewById(R.id.signUpProgressBar);

        SignInButton googleSignUpButton = findViewById(R.id.googleSignUpButton);
        googleSignUpButton.setSize(SignInButton.SIZE_WIDE);
        googleSignUpButton.setColorScheme(SignInButton.COLOR_LIGHT);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignUpButton.setOnClickListener(v -> signInWithGoogle());

        signUpButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(UserSignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(UserSignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else if (!phone.matches("\\d{10}")) {
                Toast.makeText(UserSignUpActivity.this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            } else {
                validateAndSignup(name, email, phone, password);
            }
        });

        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(UserSignUpActivity.this, UserLoginActivity.class));
            finish();
        });

        CommonAnimationHelper.applyClickAnimation(signUpButton);
    }

    private void validateAndSignup(String name, String email, String phone, String password) {
        String cleanEmail = email.toLowerCase().trim();
        toggleSignUpLoading(true);

        // 1. Block authorized Admin emails from regular user signup (Generic message for security)
        db.collection("admins").whereEqualTo("email", cleanEmail).get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        toggleSignUpLoading(false);
                        Toast.makeText(this, "This email is already registered.", Toast.LENGTH_LONG).show();
                    } else {
                        // 2. Perform User Signup
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        FirebaseUser currentUser = mAuth.getCurrentUser();
                                        if (currentUser != null) {
                                            currentUser.sendEmailVerification();
                                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(name).build();
                                            currentUser.updateProfile(profileUpdates);
                                            saveUserDetailsToFirestore(name, email, phone, currentUser.getUid());
                                        }
                                    } else {
                                        toggleSignUpLoading(false);
                                        String err = (task.getException() != null) ? task.getException().getMessage() : "Signup failed";
                                        Toast.makeText(UserSignUpActivity.this, "Error: " + err, Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
    }

    private void saveUserDetailsToFirestore(String name, String email, String phone, String userId) {
        String cleanEmail = email.toLowerCase().trim();
        
        // Generate a sequential DS-USER-XX ID by checking collection size
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            int userCount = queryDocumentSnapshots.size() + 1;
            String customId = String.format(Locale.getDefault(), "DS-USER-%02d", userCount);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // Use LinkedHashMap to maintain your requested logical order
            Map<String, Object> userData = new java.util.LinkedHashMap<>();
            userData.put("userId", customId);
            userData.put("name", name);
            userData.put("email", cleanEmail);
            userData.put("phone", phone);
            userData.put("role", "user");
            userData.put("joinedAt", timestamp);

            // Use the user's name as the Document ID (Sanitized for safety)
            String docId = name.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
            
            // Check if a document with this name already exists to avoid overwriting
            db.collection("users").document(docId).get().addOnSuccessListener(doc -> {
                String finalDocId = docId;
                if (doc.exists()) {
                    // Append a small random string or UID part if name collision happens
                    finalDocId = docId + "_" + userId.substring(0, 4);
                }
                
                db.collection("users").document(finalDocId)
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            toggleSignUpLoading(false);
                            // Force Email Verification Flow
                            mAuth.signOut(); 
                            Toast.makeText(this, "Registration successful! Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(UserSignUpActivity.this, UserLoginActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            toggleSignUpLoading(false);
                            Toast.makeText(this, "Failed to save user data", Toast.LENGTH_LONG).show();
                        });
            });
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveGoogleUserToFirestore(user);
                        }
                    } else {
                        Toast.makeText(UserSignUpActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveGoogleUserToFirestore(FirebaseUser user) {
        String name = user.getDisplayName() != null ? user.getDisplayName() : "Google User";
        String email = user.getEmail() != null ? user.getEmail().toLowerCase().trim() : "";
        
        db.collection("users").whereEqualTo("email", email).get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                db.collection("users").get().addOnSuccessListener(allUsers -> {
                    int count = allUsers.size() + 1;
                    String userIdString = String.format(Locale.getDefault(), "DS-USER-%02d", count);
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    Map<String, Object> userData = new java.util.LinkedHashMap<>();
                    userData.put("userId", userIdString);
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("role", "user");
                    userData.put("joinedAt", timestamp);
                    
                    String docId = name.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
                    db.collection("users").document(docId).set(userData)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(UserSignUpActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(UserSignUpActivity.this, UserHomeActivity.class));
                                finish();
                            });
                });
            } else {
                startActivity(new Intent(UserSignUpActivity.this, UserHomeActivity.class));
                finish();
            }
        });
    }

    private void toggleSignUpLoading(boolean isLoading) {
        signUpButton.setText(isLoading ? "" : "Sign Up");
        signUpButton.setEnabled(!isLoading);
        if (signUpProgressBar != null) signUpProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
