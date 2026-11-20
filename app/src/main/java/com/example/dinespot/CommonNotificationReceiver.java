package com.example.dinespot;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class CommonNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String chatId = intent.getStringExtra("chatId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (CommonNotificationHelper.ACTION_REPLY.equals(action)) {
            handleReply(context, intent, chatId, db);
        } else if (CommonNotificationHelper.ACTION_MARK_READ.equals(action)) {
            handleMarkAsRead(context, chatId, db);
        }
    }

    private void handleReply(Context context, Intent intent, String chatId, FirebaseFirestore db) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            CharSequence replyText = remoteInput.getCharSequence(CommonNotificationHelper.KEY_TEXT_REPLY);
            String currentUserId = FirebaseAuth.getInstance().getUid();

            if (replyText != null && currentUserId != null) {
                long ts = System.currentTimeMillis();
                CommonChatMessage message = new CommonChatMessage(replyText.toString(), currentUserId, ts);
                
                String collectionPath = chatId.startsWith("support_") ? "support_chats" : "bookings";
                
                // Update parent doc last message
                Map<String, Object> update = new HashMap<>();
                update.put("lastMessage", replyText.toString());
                update.put("lastTimestamp", ts);
                update.put("userId", currentUserId);
                db.collection(collectionPath).document(chatId).set(update, com.google.firebase.firestore.SetOptions.merge());

                // Add the message
                db.collection(collectionPath).document(chatId)
                        .collection("messages").add(message)
                        .addOnSuccessListener(ref -> {
                            // Clear notification
                            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            manager.cancel(chatId.hashCode());
                        });
            }
        }
    }

    private void handleMarkAsRead(Context context, String chatId, FirebaseFirestore db) {
        String collectionPath = chatId.startsWith("support_") ? "support_chats" : "bookings";
        
        // Find unread messages from other user and mark them read
        db.collection(collectionPath).document(chatId)
                .collection("messages")
                .whereNotEqualTo("senderId", FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        if (!"read".equals(doc.getString("status"))) {
                            batch.update(doc.getReference(), "status", "read");
                        }
                    }
                    batch.commit().addOnSuccessListener(v -> {
                        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        manager.cancel(chatId.hashCode());
                    });
                });
    }
}
