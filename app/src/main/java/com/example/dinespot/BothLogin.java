package com.example.dinespot;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class BothLogin extends AppCompatActivity {

    private Button btnUserLogin, btnAdminLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_both);

        btnUserLogin = findViewById(R.id.btnUserLogin);
        btnAdminLogin = findViewById(R.id.btnAdminLogin);

        btnUserLogin.setOnClickListener(v -> {
            Intent intent = new Intent(BothLogin.this, LoginActivity.class);
            startActivity(intent);
        });

        btnAdminLogin.setOnClickListener(v -> {
            Intent intent = new Intent(BothLogin.this, AdminLoginActivity.class);
            startActivity(intent);
        });
    }
}
