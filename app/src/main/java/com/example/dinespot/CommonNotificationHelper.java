package com.example.dinespot;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

public class CommonNotificationHelper {

    public static final String CHANNEL_ID = "support_chat_notifications";
    public static final String BOOKING_CHANNEL_ID = "booking_notifications";
    public static final String KEY_TEXT_REPLY = "key_text_reply";
    public static final String ACTION_REPLY = "com.example.dinespot.ACTION_REPLY";
    public static final String ACTION_MARK_READ = "com.example.dinespot.ACTION_MARK_READ";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Support Channel
            NotificationChannel supportChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Support Chat",
                    NotificationManager.IMPORTANCE_HIGH
            );
            supportChannel.setDescription("Notifications for Help & Support messages");
            manager.createNotificationChannel(supportChannel);

            // Booking Channel
            NotificationChannel bookingChannel = new NotificationChannel(
                    BOOKING_CHANNEL_ID,
                    "Booking Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            bookingChannel.setDescription("Alerts when your table request is accepted or rejected");
            manager.createNotificationChannel(bookingChannel);
        }
    }

    public static void showBookingNotification(Context context, String restaurantName, String status, String bookingId) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, UserBookingHistoryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                bookingId.hashCode(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String message = "Accepted".equalsIgnoreCase(status) 
                ? "Your table at " + restaurantName + " has been CONFIRMED! See you there."
                : "Sorry, your request for " + restaurantName + " was not accepted.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, BOOKING_CHANNEL_ID)
                .setSmallIcon(R.mipmap.dine_spot_logo)
                .setContentTitle("Booking Update")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(bookingId.hashCode(), builder.build());
        }
    }

    public static void showChatNotification(Context context, String title, String message, String chatId, String senderId) {
        createNotificationChannel(context);

        // Intent to open the chat activity
        Intent chatIntent = new Intent(context, UserChatActivity.class);
        chatIntent.putExtra("bookingId", chatId);
        chatIntent.putExtra("chatTitle", title);
        chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 
                chatId.hashCode(), 
                chatIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Reply Action
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("Type your reply...")
                .build();

        Intent replyIntent = new Intent(context, CommonNotificationReceiver.class);
        replyIntent.setAction(ACTION_REPLY);
        replyIntent.putExtra("chatId", chatId);
        replyIntent.putExtra("senderId", senderId);

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                context,
                chatId.hashCode() + 1,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.baseline_feedback_24,
                "Reply",
                replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();

        // Mark as Read Action
        Intent readIntent = new Intent(context, CommonNotificationReceiver.class);
        readIntent.setAction(ACTION_MARK_READ);
        readIntent.putExtra("chatId", chatId);

        PendingIntent readPendingIntent = PendingIntent.getBroadcast(
                context,
                chatId.hashCode() + 2,
                readIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Action readAction = new NotificationCompat.Action.Builder(
                R.drawable.baseline_done_all_24,
                "Mark as Read",
                readPendingIntent)
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.dine_spot_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(contentIntent)
                .addAction(replyAction)
                .addAction(readAction)
                .setOnlyAlertOnce(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(chatId.hashCode(), builder.build());
        }
    }
}
