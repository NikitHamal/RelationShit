package com.relation.shit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.relation.shit.R;
import com.relation.shit.model.Knowledge;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class KnowledgeAdapter extends RecyclerView.Adapter<KnowledgeAdapter.KnowledgeViewHolder> {

    private List<Knowledge> knowledgeList;
    private OnKnowledgeClickListener listener;

    public interface OnKnowledgeClickListener {
        void onKnowledgeClick(Knowledge knowledge);
        void onKnowledgeEdit(Knowledge knowledge);
        void onKnowledgeDelete(Knowledge knowledge);
    }

    public KnowledgeAdapter(List<Knowledge> knowledgeList, OnKnowledgeClickListener listener) {
        this.knowledgeList = knowledgeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public KnowledgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_knowledge_entry, parent, false);
        return new KnowledgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KnowledgeViewHolder holder, int position) {
        Knowledge knowledge = knowledgeList.get(position);
        holder.title.setText(knowledge.getTitle());
        holder.contentPreview.setText(knowledge.getContent());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.getDefault());
        holder.timestamp.setText(sdf.format(knowledge.getTimestamp()));

        holder.itemView.setOnClickListener(v -> listener.onKnowledgeClick(knowledge));
        // You might want to add more specific click listeners for edit/delete icons if they are part of item_knowledge_entry.xml
        // For now, I'm assuming a long press or a separate menu for edit/delete.
        // If you want buttons/icons inside the card, update item_knowledge_entry.xml and add listeners here.
    }

    @Override
    public int getItemCount() {
        return knowledgeList.size();
    }

    public void updateKnowledgeList(List<Knowledge> newKnowledgeList) {
        this.knowledgeList = newKnowledgeList;
        notifyDataSetChanged();
    }

    static class KnowledgeViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView contentPreview;
        TextView timestamp;

        KnowledgeViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_knowledge_title);
            contentPreview = itemView.findViewById(R.id.text_knowledge_content_preview);
            timestamp = itemView.findViewById(R.id.text_knowledge_timestamp);
        }
    }
}
