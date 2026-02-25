package com.example.dinespot;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class PaymentSuccessActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "dinespot_notifications";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;

    private TextView successMessage, timerTextView;
    private Button cancelPaymentButton;
    private android.os.CountDownTimer countDownTimer;

    private static final long TIMER_DURATION = 1 * 60 * 1000;
    private static final long TIMER_INTERVAL = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);
        successMessage = findViewById(R.id.successMessage);
        timerTextView = findViewById(R.id.timerTextView);
        cancelPaymentButton = findViewById(R.id.cancelPaymentButton);
        Button backToHomeButton = findViewById(R.id.backToHomeButton);
        successMessage.setText("Your payment was successful!");
        startTimer();
        moveCartItemsToOrders();
        handleNotification("Payment Confirmed", "Your order has been successfully placed!");
        backToHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentSuccessActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            finish();
        });
        cancelPaymentButton.setOnClickListener(v -> {
            showCancelConfirmationDialog();
        });
        createNotificationChannel();
    }

    private void handleNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            } else {
                showNotification(title, message);
            }
        } else {
            showNotification(title, message);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "DineSpot Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications from DineSpot");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.dine_spot_round)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(0, builder.build());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showNotification("Payment Confirmed", "Your order has been successfully placed!");
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startTimer() {
        countDownTimer = new android.os.CountDownTimer(TIMER_DURATION, TIMER_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                String timeLeft = String.format("%02d:%02d", minutes, seconds);
                timerTextView.setText("Time Left: " + timeLeft);
            }

            @Override
            public void onFinish() {
                cancelPaymentButton.setEnabled(false);
                cancelPaymentButton.setText("Time Expired");
                Toast.makeText(PaymentSuccessActivity.this, "You can no longer cancel the payment.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void moveCartItemsToOrders() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(PaymentSuccessActivity.this, "No items in cart to process.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            db.collection("users")
                                    .document(userId)
                                    .collection("orders")
                                    .add(document.getData())
                                    .addOnSuccessListener(orderDocument -> document.getReference().delete())
                                    .addOnFailureListener(e -> Toast.makeText(PaymentSuccessActivity.this, "Failed to add item to orders.", Toast.LENGTH_SHORT).show());
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(PaymentSuccessActivity.this, "Failed to retrieve cart items.", Toast.LENGTH_SHORT).show());
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Payment")
                .setMessage("Are you sure you want to cancel the payment?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    timerTextView.setText("");
                    cancelPaymentButton.setVisibility(Button.INVISIBLE);
                    cancelPayment();
                    successMessage.setText("Your payment has been cancelled.");
                    successMessage.setTextColor(getResources().getColor(R.color.colorError));
                    handleNotification("Payment Cancelled", "Your payment has been successfully cancelled.");
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelPayment() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(userId)
                .collection("orders")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(PaymentSuccessActivity.this, "No items to cancel.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            document.getReference().delete();
                        }
                        Toast.makeText(PaymentSuccessActivity.this, "Payment cancelled.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(PaymentSuccessActivity.this, "Failed to retrieve order items.", Toast.LENGTH_SHORT).show());
    }

}
