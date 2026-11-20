package com.example.dinespot;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class CommonBothLoginActivity extends CommonBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_activity_login_both);

        View btnUserLogin = findViewById(R.id.btnUserLogin);
        View btnAdminLogin = findViewById(R.id.btnAdminLogin);

        btnUserLogin.setOnClickListener(v -> {
            Intent intent = new Intent(CommonBothLoginActivity.this, UserLoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnAdminLogin.setOnClickListener(v -> {
            Intent intent = new Intent(CommonBothLoginActivity.this, AdminLoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
