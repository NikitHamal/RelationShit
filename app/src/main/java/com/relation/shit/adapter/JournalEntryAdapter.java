package com.relation.shit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.relation.shit.R;
import com.relation.shit.model.JournalEntry;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class JournalEntryAdapter extends RecyclerView.Adapter<JournalEntryAdapter.JournalEntryViewHolder> {

    private List<JournalEntry> journalEntryList;
    private OnJournalEntryClickListener listener;

    public interface OnJournalEntryClickListener {
        void onJournalEntryClick(JournalEntry entry);
    }

    public JournalEntryAdapter(List<JournalEntry> journalEntryList, OnJournalEntryClickListener listener) {
        this.journalEntryList = journalEntryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JournalEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the new item_journal_entry.xml layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journal_entry, parent, false);
        return new JournalEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalEntryViewHolder holder, int position) {
        JournalEntry entry = journalEntryList.get(position);
        holder.title.setText(entry.getTitle());
        holder.contentPreview.setText(entry.getContent());

        // Format the timestamp for a more readable display
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()); // Added yyyy for year
        holder.timestamp.setText(sdf.format(entry.getTimestamp()));

        holder.itemView.setOnClickListener(v -> listener.onJournalEntryClick(entry));
    }

    @Override
    public int getItemCount() {
        return journalEntryList.size();
    }

    public void updateJournalEntries(List<JournalEntry> newEntries) {
        this.journalEntryList = newEntries;
        notifyDataSetChanged();
    }

    static class JournalEntryViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView contentPreview;
        TextView timestamp;

        JournalEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link to the new IDs in item_journal_entry.xml
            title = itemView.findViewById(R.id.text_journal_title);
            contentPreview = itemView.findViewById(R.id.text_journal_content_preview);
            timestamp = itemView.findViewById(R.id.text_journal_timestamp);
        }
    }
}
