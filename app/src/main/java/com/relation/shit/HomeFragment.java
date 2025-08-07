package com.relation.shit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // Import SwipeRefreshLayout

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.relation.shit.adapter.ConversationAdapter;
import com.relation.shit.adapter.SelectAgentAdapter;
import com.relation.shit.model.Agent;
import com.relation.shit.model.Conversation;
import com.relation.shit.utils.SharedPrefManager;

import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment implements ConversationAdapter.OnConversationClickListener, SelectAgentAdapter.OnAgentSelectedListener {

    private View rootView;
    private SharedPrefManager sharedPrefManager;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;
    private RecyclerView conversationHistoryContainer;
    private LinearLayout emptyConversationsState;
    private View conversationHistoryScrollView;
    private AlertDialog selectAgentDialog; // To hold the reference to the dialog
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView greetingText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.home_fragment, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPrefManager = new SharedPrefManager(requireContext());

        greetingText = rootView.findViewById(R.id.greeting_text);
        conversationHistoryContainer = rootView.findViewById(R.id.conversation_history_container);
        emptyConversationsState = rootView.findViewById(R.id.empty_conversations_state);
        conversationHistoryScrollView = rootView.findViewById(R.id.conversation_history_scrollview);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);

        updateGreeting();

        conversationList = sharedPrefManager.getConversations();
        conversationAdapter = new ConversationAdapter(conversationList, this);
        conversationHistoryContainer.setLayoutManager(new LinearLayoutManager(requireContext()));
        conversationHistoryContainer.setAdapter(conversationAdapter);

        rootView.findViewById(R.id.button_new_chat).setOnClickListener(v -> showSelectAgentDialog());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshConversations();
            swipeRefreshLayout.setRefreshing(false);
        });

        updateConversationHistoryVisibility();
    }

    private void updateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        // Good Morning: From midnight (00:00) until just before noon (12:00)
        if (hourOfDay >= 0 && hourOfDay < 12) {
            greeting = "Good Morning!";
        }
        // Good Afternoon: From noon (12:00) until just before 5 PM (17:00)
        else if (hourOfDay >= 12 && hourOfDay < 17) {
            greeting = "Good Afternoon!";
        }
        // Good Evening: From 5 PM (17:00) until just before 10 PM (22:00)
        else if (hourOfDay >= 17 && hourOfDay < 22) {
            greeting = "Good Evening!";
        }
        // Good Night: From 10 PM (22:00) until just before midnight (00:00)
        else {
            greeting = "Good Night!";
        }
        greetingText.setText(greeting);
    }

    private void refreshConversations() {
        conversationList.clear();
        conversationList.addAll(sharedPrefManager.getConversations());
        conversationAdapter.notifyDataSetChanged();
        updateConversationHistoryVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh conversations when returning to HomeFragment
        refreshConversations();
    }

    /**
     * Updates the visibility of the conversation history and empty state message.
     */
    private void updateConversationHistoryVisibility() {
        if (conversationList.isEmpty()) {
            conversationHistoryScrollView.setVisibility(View.GONE);
            emptyConversationsState.setVisibility(View.VISIBLE);
        } else {
            conversationHistoryScrollView.setVisibility(View.VISIBLE);
            emptyConversationsState.setVisibility(View.GONE);
        }
    }

    private void showSelectAgentDialog() {
        List<Agent> agents = sharedPrefManager.getAgents();
        if (agents.isEmpty()) {
            Toast.makeText(requireContext(), "Please create an agent in Settings first!", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_agent, null);
        RecyclerView agentsRecyclerView = dialogView.findViewById(R.id.agents_recyclerview);
        agentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        SelectAgentAdapter selectAgentAdapter = new SelectAgentAdapter(agents, this);
        agentsRecyclerView.setAdapter(selectAgentAdapter);

        // Store the dialog instance
        selectAgentDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Agent for New Chat")
                .setView(dialogView)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("conversation_id", conversation.getId());
        intent.putExtra("agent", conversation.getAgent()); // Pass agent for display
        intent.putExtra("api_provider", conversation.getAgent().getApiProvider());
        startActivity(intent);
    }

    @Override
    public void onAgentSelected(Agent agent) {
        // Dismiss the dialog before starting the activity
        if (selectAgentDialog != null && selectAgentDialog.isShowing()) {
            selectAgentDialog.dismiss();
        }

        // Conversation will be created in ChatActivity
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("agent", agent);
        intent.putExtra("api_provider", agent.getApiProvider());
        startActivity(intent);

        // The onResume of HomeFragment will handle refreshing the list when returning.
        // No need for explicit list update here, as it's handled on fragment resume.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Dismiss the dialog if it's still showing when the view is destroyed
        if (selectAgentDialog != null && selectAgentDialog.isShowing()) {
            selectAgentDialog.dismiss();
        }
    }
}
