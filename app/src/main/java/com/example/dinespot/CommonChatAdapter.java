package com.example.dinespot;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommonChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private List<CommonChatMessage> chatMessages;
    private String currentUserId;

    public CommonChatAdapter(List<CommonChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).getSenderId().equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.common_item_chat_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.common_item_chat_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CommonChatMessage message = chatMessages.get(position);
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(message.getTimestamp()));
        
        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;
            h.tvMessage.setText(message.getMessage());
            h.tvTime.setText(time);
            
            // Handle Reply
            if (message.getReplyToId() != null) {
                h.layoutReply.setVisibility(View.VISIBLE);
                h.tvReplySender.setText(message.getReplyToSenderName());
                h.tvReplyText.setText(message.getReplyToText());
            } else {
                h.layoutReply.setVisibility(View.GONE);
            }
            
            // Handle Status
            if (message.getSenderId().equals(currentUserId)) {
                h.ivStatus.setVisibility(View.VISIBLE);
                String status = message.getStatus() != null ? message.getStatus() : "sent";
                
                if ("read".equalsIgnoreCase(status)) {
                    h.ivStatus.setImageResource(R.drawable.baseline_done_all_24); // Double tick
                    h.ivStatus.setColorFilter(Color.parseColor("#4CAF50")); // Green
                } else if ("delivered".equalsIgnoreCase(status)) {
                    h.ivStatus.setImageResource(R.drawable.baseline_done_all_24); // Double tick
                    h.ivStatus.setColorFilter(Color.parseColor("#BDBDBD")); // Grey
                } else {
                    h.ivStatus.setImageResource(R.drawable.baseline_done_24); // Single tick
                    h.ivStatus.setColorFilter(Color.parseColor("#BDBDBD")); // Grey
                }
            } else {
                h.ivStatus.setVisibility(View.GONE);
            }

        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            h.tvMessage.setText(message.getMessage());
            h.tvTime.setText(time);
            
            // Handle Reply
            if (message.getReplyToId() != null) {
                h.layoutReply.setVisibility(View.VISIBLE);
                h.tvReplySender.setText(message.getReplyToSenderName());
                h.tvReplyText.setText(message.getReplyToText());
            } else {
                h.layoutReply.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvReplySender, tvReplyText;
        View layoutReply;
        ImageView ivStatus;
        SentViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            layoutReply = itemView.findViewById(R.id.layout_reply);
            tvReplySender = itemView.findViewById(R.id.tv_reply_sender_name);
            tvReplyText = itemView.findViewById(R.id.tv_reply_message_text);
            ivStatus = itemView.findViewById(R.id.ivStatus);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvReplySender, tvReplyText;
        View layoutReply;
        ReceivedViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            layoutReply = itemView.findViewById(R.id.layout_reply);
            tvReplySender = itemView.findViewById(R.id.tv_reply_sender_name);
            tvReplyText = itemView.findViewById(R.id.tv_reply_message_text);
        }
    }
}
