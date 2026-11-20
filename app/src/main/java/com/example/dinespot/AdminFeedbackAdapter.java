package com.example.dinespot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminFeedbackAdapter extends RecyclerView.Adapter<AdminFeedbackAdapter.FeedbackViewHolder> {

    private final List<CommonFeedback> feedbackList;
    private final OnFeedbackActionListener listener;

    public interface OnFeedbackActionListener {
        void onReply(CommonFeedback feedback);
        void onDelete(CommonFeedback feedback);
    }

    public AdminFeedbackAdapter(List<CommonFeedback> feedbackList, OnFeedbackActionListener listener) {
        this.feedbackList = feedbackList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_item_feedback, parent, false);
        return new FeedbackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        CommonFeedback feedback = feedbackList.get(position);
        holder.tvName.setText(feedback.getName() != null ? feedback.getName() : "Anonymous");
        holder.tvEmail.setText(feedback.getEmail());
        
        String uid = feedback.getUserId();
        if (uid != null) {
            holder.tvUserId.setVisibility(View.VISIBLE);
            holder.tvUserId.setText("ID: " + uid);
        } else {
            holder.tvUserId.setVisibility(View.GONE);
        }

        holder.tvContent.setText(feedback.getFeedback());
        holder.tvTimestamp.setText(feedback.getTimestamp());

        // Set Rating Emoji and Label
        int rating = feedback.getRating();
        String emoji = "🙂";
        if (rating == 1) emoji = "☹️";
        else if (rating == 2) emoji = "🙁";
        else if (rating == 3) emoji = "🙂";
        else if (rating == 4) emoji = "😊";
        else if (rating == 5) emoji = "😍";
        
        holder.tvRatingEmoji.setText(emoji);
        holder.tvRatingLabel.setText(feedback.getRatingLabel() != null ? feedback.getRatingLabel() : "Neutral");

        if (feedback.getAdminReply() != null && !feedback.getAdminReply().isEmpty()) {
            holder.layoutAdminReply.setVisibility(View.VISIBLE);
            holder.tvAdminReplyContent.setText(feedback.getAdminReply());
        } else {
            holder.layoutAdminReply.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    public static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvUserId, tvContent, tvTimestamp, tvAdminReplyContent, tvRatingEmoji, tvRatingLabel;
        View layoutAdminReply;

        public FeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_feedback_name);
            tvEmail = itemView.findViewById(R.id.tv_feedback_email);
            tvUserId = itemView.findViewById(R.id.tv_feedback_userid);
            tvContent = itemView.findViewById(R.id.tv_feedback_content);
            tvTimestamp = itemView.findViewById(R.id.tv_feedback_timestamp);
            tvRatingEmoji = itemView.findViewById(R.id.tv_item_rating_emoji);
            tvRatingLabel = itemView.findViewById(R.id.tv_item_rating_label);
            tvAdminReplyContent = itemView.findViewById(R.id.tv_admin_reply_content);
            layoutAdminReply = itemView.findViewById(R.id.layout_admin_reply);
        }
    }
}
