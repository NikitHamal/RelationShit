package com.relation.shit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // Import SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.relation.shit.adapter.JournalEntryAdapter;
import com.relation.shit.model.JournalEntry;
import com.relation.shit.utils.SharedPrefManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JournalFragment extends Fragment implements JournalEntryAdapter.OnJournalEntryClickListener {

    private View rootView;
    private SharedPrefManager sharedPrefManager;
    private JournalEntryAdapter journalEntryAdapter;
    private List<JournalEntry> journalEntryList;
    private RecyclerView journalEntriesRecyclerView;
    private LinearLayout emptyJournalState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.journal_fragment, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPrefManager = new SharedPrefManager(requireContext());

        journalEntriesRecyclerView = rootView.findViewById(R.id.journal_entries_recyclerview);
        emptyJournalState = rootView.findViewById(R.id.empty_journal_state);

        journalEntryList = sharedPrefManager.getJournalEntries();
        journalEntryAdapter = new JournalEntryAdapter(journalEntryList, this);
        journalEntriesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        journalEntriesRecyclerView.setAdapter(journalEntryAdapter);

        // Set OnClickListener for the new button at the top
        rootView.findViewById(R.id.button_add_journal_entry_top).setOnClickListener(v -> showCreateJournalEntryDialog());

        updateEmptyStateVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshJournalEntries();
    }

    private void refreshJournalEntries() {
        journalEntryList.clear();
        journalEntryList.addAll(sharedPrefManager.getJournalEntries());
        journalEntryAdapter.notifyDataSetChanged();
        updateEmptyStateVisibility();
    }

    private void showCreateJournalEntryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_journal_entry, null);
        EditText editTextJournalTitle = dialogView.findViewById(R.id.edit_text_journal_title);
        EditText editTextJournalContent = dialogView.findViewById(R.id.edit_text_journal_content);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("New Journal Entry")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = editTextJournalTitle.getText().toString().trim();
                    String content = editTextJournalContent.getText().toString().trim();

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JournalEntry newEntry = new JournalEntry(UUID.randomUUID().toString(), title, content, System.currentTimeMillis());
                    sharedPrefManager.addJournalEntry(newEntry);
                    journalEntryList.add(0, newEntry);
                    journalEntryAdapter.notifyItemInserted(0);
                    journalEntriesRecyclerView.scrollToPosition(0);
                    Toast.makeText(requireContext(), "Journal entry saved!", Toast.LENGTH_SHORT).show();
                    updateEmptyStateVisibility();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onJournalEntryClick(JournalEntry entry) {
        Toast.makeText(requireContext(), "Viewing Journal Entry: " + entry.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void updateEmptyStateVisibility() {
        if (journalEntryList.isEmpty()) {
            emptyJournalState.setVisibility(View.VISIBLE);
            journalEntriesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyJournalState.setVisibility(View.GONE);
            journalEntriesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
