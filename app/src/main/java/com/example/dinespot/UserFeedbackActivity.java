package com.example.dinespot;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class UserFeedbackActivity extends CommonBaseActivity {

    private EditText etFeedback;
    private Button btnSubmit;
    private LinearLayout btnEmoji1, btnEmoji2, btnEmoji3, btnEmoji4, btnEmoji5;
    private TextView tvLabel1, tvLabel2, tvLabel3, tvLabel4, tvLabel5;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_feedback);

        setupToolbar("Send Feedback", true);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFeedback = findViewById(R.id.et_feedback);
        btnSubmit = findViewById(R.id.btn_submit_feedback);
        
        btnEmoji1 = findViewById(R.id.btnEmoji1);
        btnEmoji2 = findViewById(R.id.btnEmoji2);
        btnEmoji3 = findViewById(R.id.btnEmoji3);
        btnEmoji4 = findViewById(R.id.btnEmoji4);
        btnEmoji5 = findViewById(R.id.btnEmoji5);

        // Map labels for color changes
        tvLabel1 = (TextView) btnEmoji1.getChildAt(1);
        tvLabel2 = (TextView) btnEmoji2.getChildAt(1);
        tvLabel3 = (TextView) btnEmoji3.getChildAt(1);
        tvLabel4 = (TextView) btnEmoji4.getChildAt(1);
        tvLabel5 = (TextView) btnEmoji5.getChildAt(1);

        View.OnClickListener emojiClickListener = v -> {
            int id = v.getId();
            if (id == R.id.btnEmoji1) selectedRating = 1;
            else if (id == R.id.btnEmoji2) selectedRating = 2;
            else if (id == R.id.btnEmoji3) selectedRating = 3;
            else if (id == R.id.btnEmoji4) selectedRating = 4;
            else if (id == R.id.btnEmoji5) selectedRating = 5;
            
            updateEmojiUI();
            vibrateAction();
        };

        btnEmoji1.setOnClickListener(emojiClickListener);
        btnEmoji2.setOnClickListener(emojiClickListener);
        btnEmoji3.setOnClickListener(emojiClickListener);
        btnEmoji4.setOnClickListener(emojiClickListener);
        btnEmoji5.setOnClickListener(emojiClickListener);

        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void updateEmojiUI() {
        resetEmojiUI();
        LinearLayout selectedLayout = null;
        TextView selectedLabel = null;

        if (selectedRating == 1) { selectedLayout = btnEmoji1; selectedLabel = tvLabel1; }
        else if (selectedRating == 2) { selectedLayout = btnEmoji2; selectedLabel = tvLabel2; }
        else if (selectedRating == 3) { selectedLayout = btnEmoji3; selectedLabel = tvLabel3; }
        else if (selectedRating == 4) { selectedLayout = btnEmoji4; selectedLabel = tvLabel4; }
        else if (selectedRating == 5) { selectedLayout = btnEmoji5; selectedLabel = tvLabel5; }

        if (selectedLayout != null && selectedLabel != null) {
            selectedLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_status_bg));
            selectedLayout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.colorPrimaryLight)));
            selectedLabel.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            selectedLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void resetEmojiUI() {
        btnEmoji1.setBackground(null);
        btnEmoji2.setBackground(null);
        btnEmoji3.setBackground(null);
        btnEmoji4.setBackground(null);
        btnEmoji5.setBackground(null);

        int grey = ContextCompat.getColor(this, R.color.textSecondary);
        tvLabel1.setTextColor(grey); tvLabel1.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvLabel2.setTextColor(grey); tvLabel2.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvLabel3.setTextColor(grey); tvLabel3.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvLabel4.setTextColor(grey); tvLabel4.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvLabel5.setTextColor(grey); tvLabel5.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    private void submitFeedback() {
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select an emoji rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        String feedbackText = etFeedback.getText().toString().trim();
        if (TextUtils.isEmpty(feedbackText)) {
            Toast.makeText(this, "Please write a message.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.google.firebase.auth.FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            showLoading();
            
            db.collection("users").whereEqualTo("email", currentUser.getEmail()).get().addOnSuccessListener(snaps -> {
                String customUserId = "DS-USER-00";
                String name = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";
                
                if (!snaps.isEmpty()) {
                    com.google.firebase.firestore.DocumentSnapshot doc = snaps.getDocuments().get(0);
                    if (doc.getString("userId") != null) customUserId = doc.getString("userId");
                    if (doc.getString("name") != null) name = doc.getString("name");
                }

                String ratingLabel = "Neutral";
                if (selectedRating == 1) ratingLabel = "Very Sad";
                else if (selectedRating == 2) ratingLabel = "Sad";
                else if (selectedRating == 3) ratingLabel = "Happy";
                else if (selectedRating == 4) ratingLabel = "Good";
                else if (selectedRating == 5) ratingLabel = "Excellent";

                Map<String, Object> data = new java.util.LinkedHashMap<>();
                data.put("userId", customUserId);
                data.put("userEmail", currentUser.getEmail());
                data.put("userName", name);
                data.put("rating", selectedRating);
                data.put("ratingLabel", ratingLabel);
                data.put("feedback", feedbackText);
                data.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                
                db.collection("feedback").add(data).addOnSuccessListener(ref -> {
                    hideLoading();
                    vibrateSuccess();
                    Toast.makeText(this, "Feedback submitted! Thank you.", Toast.LENGTH_LONG).show();
                    finish();
                }).addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Submission failed.", Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                hideLoading();
                Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
