package com.relation.shit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.relation.shit.R;
import com.relation.shit.model.Agent;
import java.util.List;

public class AgentAdapter extends RecyclerView.Adapter<AgentAdapter.AgentViewHolder> {

    private List<Agent> agentList;
    private OnAgentClickListener listener;

    public interface OnAgentClickListener {
        void onAgentClick(Agent agent);
    }

    public AgentAdapter(List<Agent> agentList, OnAgentClickListener listener) {
        this.agentList = agentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AgentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_agent, parent, false);
        return new AgentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AgentViewHolder holder, int position) {
        Agent agent = agentList.get(position);
        holder.agentName.setText(agent.getName());
        holder.agentEmoji.setText(agent.getEmoji());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAgentClick(agent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return agentList.size();
    }

    public static class AgentViewHolder extends RecyclerView.ViewHolder {
        TextView agentName;
        TextView agentEmoji;

        public AgentViewHolder(@NonNull View itemView) {
            super(itemView);
            agentName = itemView.findViewById(R.id.text_agent_name);
            agentEmoji = itemView.findViewById(R.id.text_agent_emoji);
        }
    }

    public void updateAgents(List<Agent> newAgents) {
        this.agentList = newAgents;
        notifyDataSetChanged();
    }
}
