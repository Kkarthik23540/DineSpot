package com.example.dinespot;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminSupportInboxActivity extends CommonBaseActivity {

    private RecyclerView rvSupportChats;
    private SupportChatAdapter adapter;
    private List<SupportSummary> chatSummaries = new ArrayList<>();
    private List<SupportSummary> fullSummariesList = new ArrayList<>();
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View shimmerView;
    private TextView tvEmpty;
    private EditText etSearch;
    private ListenerRegistration inboxListener;

    public static class SupportSummary {
        private String chatId, userEmail, userName, lastMessage;
        private long lastTimestamp;

        public SupportSummary() {}
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
        public long getLastTimestamp() { return lastTimestamp; }
        public void setLastTimestamp(long lastTimestamp) { this.lastTimestamp = lastTimestamp; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_support_inbox);

        setupToolbar("Support Inbox", true);

        rvSupportChats = findViewById(R.id.rvSupportChats);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        shimmerView = findViewById(R.id.shimmer_view_container);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearchUser);

        db = FirebaseFirestore.getInstance();

        rvSupportChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SupportChatAdapter(chatSummaries, (chatId, userEmail) -> {
            Intent intent = new Intent(this, UserChatActivity.class);
            intent.putExtra("bookingId", chatId);
            intent.putExtra("otherUserId", chatId.replace("support_", ""));
            intent.putExtra("chatTitle", userEmail != null ? userEmail : "User Support");
            startActivity(intent);
        });
        rvSupportChats.setAdapter(adapter);

        setupSearch();
        swipeRefreshLayout.setOnRefreshListener(this::fetchChats);
        fetchChats();
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterChats(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filterChats(String query) {
        String lowerQuery = query.toLowerCase().trim();
        chatSummaries.clear();
        for (SupportSummary s : fullSummariesList) {
            boolean matches = (s.getUserEmail() != null && s.getUserEmail().toLowerCase().contains(lowerQuery)) ||
                             (s.getUserName() != null && s.getUserName().toLowerCase().contains(lowerQuery));
            if (matches) chatSummaries.add(s);
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(chatSummaries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void fetchChats() {
        if (inboxListener != null) inboxListener.remove();
        
        inboxListener = db.collection("support_chats")
                .orderBy("lastTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (shimmerView != null) shimmerView.setVisibility(View.GONE);
                    if (error != null) return;
                    
                    if (value != null) {
                        fullSummariesList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            SupportSummary summary = doc.toObject(SupportSummary.class);
                            summary.setChatId(doc.getId());
                            fullSummariesList.add(summary);
                        }
                        filterChats(etSearch != null ? etSearch.getText().toString() : "");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (inboxListener != null) inboxListener.remove();
        super.onDestroy();
    }

    private static class SupportChatAdapter extends RecyclerView.Adapter<SupportChatAdapter.ViewHolder> {
        private final List<SupportSummary> summaries;
        private final OnChatClickListener listener;

        interface OnChatClickListener { void onChatClick(String chatId, String userEmail); }

        SupportChatAdapter(List<SupportSummary> summaries, OnChatClickListener listener) {
            this.summaries = summaries;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_item_support_chat, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SupportSummary summary = summaries.get(position);
            String email = summary.getUserEmail();
            holder.tvEmail.setText(email != null ? email : "Inquiry #" + summary.getChatId().substring(8, 12));
            
            String shortId = summary.getChatId().length() > 6 ? summary.getChatId().substring(summary.getChatId().length() - 6).toUpperCase() : summary.getChatId().toUpperCase();
            holder.tvTicket.setText("Ticket #DS-" + shortId);
            
            holder.itemView.setOnClickListener(v -> listener.onChatClick(summary.getChatId(), holder.tvEmail.getText().toString()));
        }

        @Override public int getItemCount() { return summaries.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmail, tvTicket;
            ViewHolder(View v) {
                super(v);
                tvEmail = v.findViewById(R.id.tvUserEmail);
                tvTicket = v.findViewById(R.id.tvTicketId);
            }
        }
    }
}