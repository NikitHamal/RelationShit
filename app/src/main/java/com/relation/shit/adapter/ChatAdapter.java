package com.relation.shit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.relation.shit.R;
import com.relation.shit.model.Message;
import com.google.android.material.button.MaterialButton;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.relation.shit.utils.MarkdownUtils;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;
    private static final int VIEW_TYPE_THINKING = 3;
    private static final int VIEW_TYPE_ERROR = 4;

    private List<Message> messageList;
    private int userMessageMaxWidth; // Field to store max width for user messages

    // Constructor updated to accept max width
    public ChatAdapter(List<Message> messageList, int userMessageMaxWidth) {
        this.messageList = messageList;
        this.userMessageMaxWidth = userMessageMaxWidth;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        switch (message.getType()) {
            case "user":
                return VIEW_TYPE_USER;
            case "ai":
                return VIEW_TYPE_AI;
            case "thinking":
                return VIEW_TYPE_THINKING;
            case "error":
                return VIEW_TYPE_ERROR;
            default:
                return VIEW_TYPE_AI; // Default to AI for unknown types
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_user, parent, false);
            UserMessageViewHolder holder = new UserMessageViewHolder(view);
            // Apply max width to the TextView in the user message bubble
            if (holder.messageText != null) {
                holder.messageText.setMaxWidth(userMessageMaxWidth);
            }
            return holder;
        } else if (viewType == VIEW_TYPE_AI) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_ai, parent, false);
            return new AiMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_THINKING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_thinking, parent, false);
            return new ThinkingMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_ERROR) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_error, parent, false);
            return new ErrorMessageViewHolder(view);
        } else {
            // Fallback, though should not happen with proper type handling
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_ai, parent, false);
            return new AiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserMessageViewHolder) holder).messageText.setText(message.getContent());
        } else if (holder.getItemViewType() == VIEW_TYPE_AI) {
            ((AiMessageViewHolder) holder).messageText.setText(MarkdownUtils.formatMarkdown(message.getContent()));
        } else if (holder.getItemViewType() == VIEW_TYPE_THINKING) {
            ((ThinkingMessageViewHolder) holder).messageText.setText(message.getContent());
        } else if (holder.getItemViewType() == VIEW_TYPE_ERROR) {
            ErrorMessageViewHolder errorHolder = (ErrorMessageViewHolder) holder;
            errorHolder.messageText.setText(message.getContent());
            if (message.getRetryAction() != null) {
                errorHolder.retryButton.setVisibility(View.VISIBLE);
                errorHolder.retryButton.setOnClickListener(v -> message.getRetryAction().run());
            } else {
                errorHolder.retryButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
        }
    }

    static class ThinkingMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        ThinkingMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
        }
    }

    static class ErrorMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        MaterialButton retryButton;

        ErrorMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            retryButton = itemView.findViewById(R.id.button_retry);
        }
    }
}