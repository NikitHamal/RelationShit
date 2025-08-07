package com.relation.shit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // Import SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.relation.shit.adapter.KnowledgeAdapter;
import com.relation.shit.model.Knowledge;
import com.relation.shit.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KnowledgeActivity extends AppCompatActivity implements KnowledgeAdapter.OnKnowledgeClickListener {

    private SharedPrefManager sharedPrefManager;
    private KnowledgeAdapter knowledgeAdapter;
    private List<Knowledge> knowledgeList;
    private String conversationId;

    private RecyclerView knowledgeEntriesRecyclerView;
    private LinearLayout emptyKnowledgeState;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.knowledge); // Set the content view to the new knowledge.xml

        sharedPrefManager = new SharedPrefManager(this);

        MaterialToolbar toolbar = findViewById(R.id.knowledge_toolbar);
        setSupportActionBar(toolbar);
        // Remove title from toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ImageView backButton = findViewById(R.id.knowledge_back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        knowledgeEntriesRecyclerView = findViewById(R.id.knowledge_entries_recyclerview);
        emptyKnowledgeState = findViewById(R.id.empty_knowledge_state);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        // Get conversation ID from intent
        conversationId = getIntent().getStringExtra("conversation_id");
        if (conversationId == null) {
            Toast.makeText(this, "Error: Conversation ID not provided.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no conversation ID
            return;
        }

        knowledgeList = sharedPrefManager.getKnowledgeBasesForConversation(conversationId);
        knowledgeAdapter = new KnowledgeAdapter(knowledgeList, this);
        knowledgeEntriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        knowledgeEntriesRecyclerView.setAdapter(knowledgeAdapter);

        findViewById(R.id.button_add_knowledge_top).setOnClickListener(v -> showCreateEditKnowledgeDialog(null));

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshKnowledgeList();
            swipeRefreshLayout.setRefreshing(false);
        });

        updateEmptyStateVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh knowledge entries when returning to KnowledgeActivity
        refreshKnowledgeList();
    }

    private void refreshKnowledgeList() {
        knowledgeList.clear();
        knowledgeList.addAll(sharedPrefManager.getKnowledgeBasesForConversation(conversationId));
        knowledgeAdapter.notifyDataSetChanged();
        updateEmptyStateVisibility();
    }

    private void showCreateEditKnowledgeDialog(@Nullable Knowledge knowledgeToEdit) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_knowledge_entry, null);
        EditText editTextKnowledgeTitle = dialogView.findViewById(R.id.edit_text_knowledge_title);
        EditText editTextKnowledgeContent = dialogView.findViewById(R.id.edit_text_knowledge_content);

        boolean isEditing = (knowledgeToEdit != null);
        if (isEditing) {
            editTextKnowledgeTitle.setText(knowledgeToEdit.getTitle());
            editTextKnowledgeContent.setText(knowledgeToEdit.getContent());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(isEditing ? "Edit Knowledge Entry" : "New Knowledge Entry")
                .setView(dialogView)
                .setPositiveButton(isEditing ? "Update" : "Save", (dialog, which) -> {
                    String title = editTextKnowledgeTitle.getText().toString().trim();
                    String content = editTextKnowledgeContent.getText().toString().trim();

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isEditing) {
                        knowledgeToEdit.setTitle(title);
                        knowledgeToEdit.setContent(content);
                        knowledgeToEdit.setTimestamp(System.currentTimeMillis()); // Update timestamp on edit
                        sharedPrefManager.updateKnowledgeBase(knowledgeToEdit);
                        Toast.makeText(this, "Knowledge entry updated!", Toast.LENGTH_SHORT).show();
                    } else {
                        Knowledge newKnowledge = new Knowledge(UUID.randomUUID().toString(), conversationId, title, content, System.currentTimeMillis());
                        sharedPrefManager.addKnowledgeBase(newKnowledge);
                        Toast.makeText(this, "Knowledge entry saved!", Toast.LENGTH_SHORT).show();
                    }
                    refreshKnowledgeList(); // Refresh the list after save/update
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showDeleteConfirmationDialog(Knowledge knowledgeToDelete) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Knowledge Entry?")
                .setMessage("Are you sure you want to delete \"" + knowledgeToDelete.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    sharedPrefManager.deleteKnowledgeBase(knowledgeToDelete.getId());
                    Toast.makeText(this, "Knowledge entry deleted!", Toast.LENGTH_SHORT).show();
                    refreshKnowledgeList(); // Refresh the list after deletion
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateEmptyStateVisibility() {
        if (knowledgeList.isEmpty()) {
            emptyKnowledgeState.setVisibility(View.VISIBLE);
            knowledgeEntriesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyKnowledgeState.setVisibility(View.GONE);
            knowledgeEntriesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onKnowledgeClick(Knowledge knowledge) {
        // When a knowledge entry is clicked, you might want to show a detailed view or directly open the edit dialog
        showCreateEditKnowledgeDialog(knowledge);
    }

    @Override
    public void onKnowledgeEdit(Knowledge knowledge) {
        showCreateEditKnowledgeDialog(knowledge);
    }

    @Override
    public void onKnowledgeDelete(Knowledge knowledge) {
        showDeleteConfirmationDialog(knowledge);
    }
}
