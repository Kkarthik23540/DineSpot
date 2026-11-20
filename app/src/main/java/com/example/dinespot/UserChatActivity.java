package com.example.dinespot;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserChatActivity extends CommonBaseActivity {

    private RecyclerView rvChat;
    private CommonChatAdapter adapter;
    private List<CommonChatMessage> chatMessages;
    private EditText etMessage;
    private String bookingId, otherUserId, chatTitle;
    private FirebaseFirestore db;
    private String currentUserId;

    private View replyPreviewCard;
    private TextView tvReplySender, tvReplyText, tvTicketId;
    private String replyToId = null, replyToText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_chat);

        bookingId = getIntent().getStringExtra("bookingId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        chatTitle = getIntent().getStringExtra("chatTitle");

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        setupToolbar("", true);
        initViews();
        setupChatList();
        setupSwipeToReply();

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btn_cancel_reply).setOnClickListener(v -> cancelReply());
        findViewById(R.id.btn_attachment).setOnClickListener(v -> Toast.makeText(this, "Attachment feature coming soon!", Toast.LENGTH_SHORT).show());

        listenForMessages();
    }

    private void showCategorySelectionDialog() {
        // Method removed as per request
    }

    private void initViews() {
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        replyPreviewCard = findViewById(R.id.reply_preview_card);
        tvReplySender = findViewById(R.id.tv_reply_sender);
        tvReplyText = findViewById(R.id.tv_reply_text);
        tvTicketId = findViewById(R.id.tv_ticket_id);
        
        TextView tvTitle = findViewById(R.id.tv_chat_title);
        if (tvTitle != null) tvTitle.setText(chatTitle != null ? chatTitle : "Support Desk");
        
        // Generate a stable Ticket ID based on bookingId
        if (tvTicketId != null && bookingId != null) {
            String shortId = bookingId.length() > 6 ? bookingId.substring(bookingId.length() - 6).toUpperCase() : bookingId.toUpperCase();
            tvTicketId.setText("Ticket #DS-" + shortId);
        }
    }

    private void setupChatList() {
        chatMessages = new ArrayList<>();
        adapter = new CommonChatAdapter(chatMessages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);
    }

    private void setupSwipeToReply() {
        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CommonChatMessage message = chatMessages.get(position);
                    showReplyUI(message);
                }
                // Notify adapter to snap the item back immediately
                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // Limit the swipe distance to about 1/4th of the screen width (roughly "half" of the bubble)
                float maxWidth = recyclerView.getWidth() / 4f;
                float limitedDX = Math.min(dX, maxWidth);
                
                super.onChildDraw(c, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive);
            }
            
            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.3f; // Trigger swiped action after 30% of the allowed distance
            }
        };
        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(rvChat);
    }

    private void showReplyUI(CommonChatMessage message) {
        replyToId = message.getSenderId(); 
        replyToText = message.getMessage();
        
        tvReplySender.setText(currentUserId.equals(message.getSenderId()) ? "Replying to yourself" : "Replying to Support");
        tvReplyText.setText(replyToText);
        replyPreviewCard.setVisibility(View.VISIBLE);
        
        etMessage.requestFocus();
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etMessage, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }

    private void cancelReply() {
        replyToId = null;
        replyToText = null;
        replyPreviewCard.setVisibility(View.GONE);
    }

    private void listenForMessages() {
        String collectionPath = bookingId.startsWith("support_") ? "support_chats" : "bookings";
        
        db.collection(collectionPath).document(bookingId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(this, (snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Listener failed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        chatMessages.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            CommonChatMessage message = doc.toObject(CommonChatMessage.class);
                            if (message != null) {
                                message.setMessageId(doc.getId());
                                chatMessages.add(message);
                                
                                // Mark incoming messages as read
                                if (!message.getSenderId().equals(currentUserId) && !"read".equals(message.getStatus())) {
                                    db.collection(collectionPath).document(bookingId)
                                            .collection("messages").document(doc.getId()).update("status", "read");
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (!chatMessages.isEmpty()) {
                            rvChat.scrollToPosition(chatMessages.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        String msgText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(msgText)) return;

        long ts = System.currentTimeMillis();
        CommonChatMessage message = new CommonChatMessage(msgText, currentUserId, ts);
        
        if (replyToId != null) {
            message.setReplyToId(replyToId);
            message.setReplyToText(replyToText);
            message.setReplyToSenderName(tvReplySender.getText().toString());
        }

        String collectionPath = bookingId.startsWith("support_") ? "support_chats" : "bookings";
        
        // Ensure parent document exists so it shows up in Admin's list
        if (collectionPath.equals("support_chats")) {
            Map<String, Object> update = new HashMap<>();
            update.put("lastMessage", msgText);
            update.put("lastTimestamp", ts);
            update.put("userId", currentUserId);
            
            // Fix: Store user email in the summary to avoid glitchy loading in Inbox
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                update.put("userEmail", user.getEmail());
                update.put("userName", user.getDisplayName());
            }
            
            db.collection(collectionPath).document(bookingId).set(update, com.google.firebase.firestore.SetOptions.merge());
        }

        db.collection(collectionPath).document(bookingId)
                .collection("messages").add(message)
                .addOnSuccessListener(ref -> {
                    etMessage.setText("");
                    cancelReply();
                    Snackbar.make(rvChat, "Message Sent", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show());
    }

}
