package com.relation.shit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.relation.shit.model.Agent;
import com.relation.shit.model.Conversation;
import com.relation.shit.model.Insight;
import com.relation.shit.model.JournalEntry;
import com.relation.shit.model.Knowledge;
import com.relation.shit.model.Survey;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SharedPrefManager {

    private static final String PREF_NAME = "RelationShitPrefs";
    private static final String KEY_DEEPSEEK_API_KEY = "deepseek_api_key";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    private static final String KEY_AGENTS = "chat_agents";
    private static final String KEY_CONVERSATIONS = "conversations";
    private static final String KEY_JOURNAL_ENTRIES = "journal_entries";
    private static final String KEY_KNOWLEDGE_BASES = "knowledge_bases";
    private static final String KEY_INSIGHTS = "insights";
    private static final String KEY_SURVEY_HISTORY = "survey_history";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveDeepseekApiKey(String apiKey) {
        sharedPreferences.edit().putString(KEY_DEEPSEEK_API_KEY, apiKey).apply();
    }

    public String getDeepseekApiKey() {
        return sharedPreferences.getString(KEY_DEEPSEEK_API_KEY, "");
    }

    public void saveGeminiApiKey(String apiKey) {
        sharedPreferences.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply();
    }

    public String getGeminiApiKey() {
        return sharedPreferences.getString(KEY_GEMINI_API_KEY, "");
    }

    public void saveAgents(List<Agent> agents) {
        String json = gson.toJson(agents);
        sharedPreferences.edit().putString(KEY_AGENTS, json).apply();
    }

    public List<Agent> getAgents() {
        String json = sharedPreferences.getString(KEY_AGENTS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Agent>>() {}.getType();
        List<Agent> agents = gson.fromJson(json, type);
        // Handle older agents that might not have apiProvider set
        for (Agent agent : agents) {
            if (agent.getApiProvider() == null) {
                agent.setApiProvider("Deepseek"); // Default to Deepseek for old agents
            }
        }
        return agents;
    }

    public void addAgent(Agent agent) {
        List<Agent> agents = getAgents();
        agents.add(agent);
        saveAgents(agents);
    }

    public void updateAgent(Agent updatedAgent) {
        List<Agent> agents = getAgents();
        for (int i = 0; i < agents.size(); i++) {
            if (agents.get(i).getId().equals(updatedAgent.getId())) {
                agents.set(i, updatedAgent);
                break;
            }
        }
        saveAgents(agents);
    }

    public void saveConversations(List<Conversation> conversations) {
        String json = gson.toJson(conversations);
        sharedPreferences.edit().putString(KEY_CONVERSATIONS, json).apply();
    }

    public List<Conversation> getConversations() {
        String json = sharedPreferences.getString(KEY_CONVERSATIONS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Conversation>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void addConversation(Conversation conversation) {
        List<Conversation> conversations = getConversations();
        conversations.add(0, conversation);
        saveConversations(conversations);
    }

    public void updateConversation(Conversation updatedConversation) {
        List<Conversation> conversations = getConversations();
        boolean conversationExists = false;
        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i).getId().equals(updatedConversation.getId())) {
                conversations.set(i, updatedConversation);
                conversationExists = true;
                break;
            }
        }
        if (!conversationExists) {
            conversations.add(0, updatedConversation);
        }
        saveConversations(conversations);
    }

    public Conversation getConversationById(String conversationId) {
        List<Conversation> conversations = getConversations();
        for (Conversation conversation : conversations) {
            if (conversation.getId().equals(conversationId)) {
                return conversation;
            }
        }
        return null;
    }

    public void saveJournalEntries(List<JournalEntry> entries) {
        String json = gson.toJson(entries);
        sharedPreferences.edit().putString(KEY_JOURNAL_ENTRIES, json).apply();
    }

    public List<JournalEntry> getJournalEntries() {
        String json = sharedPreferences.getString(KEY_JOURNAL_ENTRIES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<JournalEntry>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void addJournalEntry(JournalEntry entry) {
        List<JournalEntry> entries = getJournalEntries();
        entries.add(0, entry);
        saveJournalEntries(entries);
    }

    // --- Knowledge Base Methods ---

    public void saveKnowledgeBases(List<Knowledge> knowledgeBases) {
        String json = gson.toJson(knowledgeBases);
        sharedPreferences.edit().putString(KEY_KNOWLEDGE_BASES, json).apply();
    }

    public List<Knowledge> getAllKnowledgeBases() {
        String json = sharedPreferences.getString(KEY_KNOWLEDGE_BASES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Knowledge>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public List<Knowledge> getKnowledgeBasesForConversation(String conversationId) {
        List<Knowledge> allKnowledge = getAllKnowledgeBases();
        return allKnowledge.stream()
                .filter(knowledge -> knowledge.getConversationId().equals(conversationId))
                .collect(Collectors.toList());
    }

    public void addKnowledgeBase(Knowledge knowledge) {
        List<Knowledge> allKnowledge = getAllKnowledgeBases();
        allKnowledge.add(0, knowledge);
        saveKnowledgeBases(allKnowledge);
    }

    public void updateKnowledgeBase(Knowledge updatedKnowledge) {
        List<Knowledge> allKnowledge = getAllKnowledgeBases();
        for (int i = 0; i < allKnowledge.size(); i++) {
            if (allKnowledge.get(i).getId().equals(updatedKnowledge.getId())) {
                allKnowledge.set(i, updatedKnowledge);
                break;
            }
        }
        saveKnowledgeBases(allKnowledge);
    }

    public void deleteKnowledgeBase(String knowledgeId) {
        List<Knowledge> allKnowledge = getAllKnowledgeBases();
        allKnowledge.removeIf(knowledge -> knowledge.getId().equals(knowledgeId));
        saveKnowledgeBases(allKnowledge);
    }

    // --- Insights Methods ---

    public void saveInsights(List<Insight> insights) {
        String json = gson.toJson(insights);
        sharedPreferences.edit().putString(KEY_INSIGHTS, json).apply();
    }

    public List<Insight> getAllInsights() {
        String json = sharedPreferences.getString(KEY_INSIGHTS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Insight>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public Insight getInsightForConversation(String conversationId) {
        List<Insight> allInsights = getAllInsights();
        for (Insight insight : allInsights) {
            if (insight.getConversationId().equals(conversationId)) {
                return insight;
            }
        }
        return null;
    }

    public void addInsight(Insight insight) {
        List<Insight> allInsights = getAllInsights();
        allInsights.add(0, insight);
        saveInsights(allInsights);
    }

    public void updateInsight(Insight updatedInsight) {
        List<Insight> allInsights = getAllInsights();
        for (int i = 0; i < allInsights.size(); i++) {
            if (allInsights.get(i).getId().equals(updatedInsight.getId())) {
                allInsights.set(i, updatedInsight);
                break;
            }
        }
        saveInsights(allInsights);
    }

    public void deleteInsight(String insightId) {
        List<Insight> allInsights = getAllInsights();
        allInsights.removeIf(insight -> insight.getId().equals(insightId));
        saveInsights(allInsights);
    }

    // --- Survey History Methods ---

    public void saveSurveyHistory(List<Survey> surveys) {
        String json = gson.toJson(surveys);
        sharedPreferences.edit().putString(KEY_SURVEY_HISTORY, json).apply();
    }

    public List<Survey> getSurveyHistoryForConversation(String conversationId) {
        String json = sharedPreferences.getString(KEY_SURVEY_HISTORY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Survey>>() {}.getType();
        List<Survey> allSurveys = gson.fromJson(json, type);
        return allSurveys.stream()
                .filter(survey -> survey.getConversationId().equals(conversationId))
                .collect(Collectors.toList());
    }

    public void addSurveyToHistory(Survey survey) {
        List<Survey> allSurveys = getSurveyHistoryForConversation(survey.getConversationId());
        allSurveys.add(0, survey);
        saveSurveyHistory(allSurveys);
    }
}