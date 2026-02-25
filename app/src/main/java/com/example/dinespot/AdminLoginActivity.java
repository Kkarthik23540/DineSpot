package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText etAdminEmail, etAdminPassword;
    private Button btnAdminLogin;
    private TextView btnAdminSignup;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
        btnAdminSignup = findViewById(R.id.btnAdminSignup);

        // Login button
        btnAdminLogin.setOnClickListener(v -> loginAdmin());

        // Redirect to sign-up activity
        btnAdminSignup.setOnClickListener(v -> {
            Intent intent = new Intent(AdminLoginActivity.this, AdminSignupActivity.class);
            startActivity(intent);
        });
    }

    private void loginAdmin() {
        String email = etAdminEmail.getText().toString().trim();
        String password = etAdminPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etAdminEmail.setError("Email is required!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etAdminPassword.setError("Password is required!");
            return;
        }

        // Firebase login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Check if the logged-in user is the admin
                        if (email.equals("kurmathikarthik@gmail.com")) {
                            Toast.makeText(this, "Admin Login Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, AdminDashboardActivity.class));
                            finish();
                        } else {
                            mAuth.signOut();
                            Toast.makeText(this, "Not authorized as admin!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
