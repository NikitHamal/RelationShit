package com.relation.shit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.relation.shit.R;
import com.relation.shit.model.Conversation;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<Conversation> conversationList;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(List<Conversation> conversationList, OnConversationClickListener listener) {
        this.conversationList = conversationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);
        holder.title.setText(conversation.getTitle());
        holder.agentName.setText(conversation.getAgent().getName());
        holder.agentEmoji.setText(conversation.getAgent().getEmoji());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.timestamp.setText(sdf.format(conversation.getTimestamp()));

        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    public void updateConversations(List<Conversation> newConversations) {
        this.conversationList = newConversations;
        notifyDataSetChanged();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView agentName;
        TextView agentEmoji;
        TextView timestamp;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_conversation_title);
            agentName = itemView.findViewById(R.id.text_agent_name);
            agentEmoji = itemView.findViewById(R.id.text_agent_emoji);
            timestamp = itemView.findViewById(R.id.text_conversation_timestamp);
        }
    }
}
