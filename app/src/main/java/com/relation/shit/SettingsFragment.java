package com.relation.shit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.LinearLayout; // Import LinearLayout

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.relation.shit.api.DeepseekApi;
import com.relation.shit.api.GeminiApi;
import com.relation.shit.api.QwenApi;
import com.relation.shit.model.Agent;
import com.relation.shit.utils.SharedPrefManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import android.widget.AdapterView;
import android.util.Log;
import com.relation.shit.adapter.AgentAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SettingsFragment extends Fragment implements AgentAdapter.OnAgentClickListener {

    private View rootView;
    private SharedPrefManager sharedPrefManager;
    private AgentAdapter agentAdapter;

    private EditText editTextDeepseekApiKey;
    private EditText editTextGeminiApiKey;
    private LinearLayout emptyAgentsState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.settings_fragment, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPrefManager = new SharedPrefManager(requireContext());

        editTextDeepseekApiKey = rootView.findViewById(R.id.edit_text_deepseek_api_key);
        editTextGeminiApiKey = rootView.findViewById(R.id.edit_text_gemini_api_key);
        emptyAgentsState = rootView.findViewById(R.id.empty_agents_state);

        // Load and display API Keys
        editTextDeepseekApiKey.setText(sharedPrefManager.getDeepseekApiKey());
        editTextGeminiApiKey.setText(sharedPrefManager.getGeminiApiKey());

        rootView.findViewById(R.id.button_save_deepseek_api_key).setOnClickListener(v -> {
            String apiKey = editTextDeepseekApiKey.getText().toString().trim();
            sharedPrefManager.saveDeepseekApiKey(apiKey);
            Toast.makeText(requireContext(), "Deepseek API Key saved!", Toast.LENGTH_SHORT).show();
        });

        rootView.findViewById(R.id.button_save_gemini_api_key).setOnClickListener(v -> {
            String apiKey = editTextGeminiApiKey.getText().toString().trim();
            sharedPrefManager.saveGeminiApiKey(apiKey);
            Toast.makeText(requireContext(), "Gemini API Key saved!", Toast.LENGTH_SHORT).show();
        });

        // Setup Agents RecyclerView
        ((androidx.recyclerview.widget.RecyclerView) rootView.findViewById(R.id.agents_recyclerview)).setLayoutManager(new LinearLayoutManager(requireContext()));
        agentAdapter = new AgentAdapter(sharedPrefManager.getAgents(), this);
        ((androidx.recyclerview.widget.RecyclerView) rootView.findViewById(R.id.agents_recyclerview)).setAdapter(agentAdapter);

        rootView.findViewById(R.id.button_create_agent).setOnClickListener(v -> showCreateAgentDialog(null));

        // Initial refresh of agents when the view is created
        refreshAgents();
    }

    private void refreshAgents() {
        List<Agent> agents = sharedPrefManager.getAgents();
        agentAdapter.updateAgents(agents);
        if (agents.isEmpty()) {
            emptyAgentsState.setVisibility(View.VISIBLE);
            ((androidx.recyclerview.widget.RecyclerView) rootView.findViewById(R.id.agents_recyclerview)).setVisibility(View.GONE);
        } else {
            emptyAgentsState.setVisibility(View.GONE);
            ((androidx.recyclerview.widget.RecyclerView) rootView.findViewById(R.id.agents_recyclerview)).setVisibility(View.VISIBLE);
        }
    }

    private void showCreateAgentDialog(@Nullable Agent agentToEdit) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_agent, null);
        EditText editTextAgentName = dialogView.findViewById(R.id.edit_text_agent_name);
        EditText editTextAgentPrompt = dialogView.findViewById(R.id.edit_text_agent_prompt);
        EditText editTextAgentEmoji = dialogView.findViewById(R.id.edit_text_agent_emoji);
        Spinner spinnerApiProvider = dialogView.findViewById(R.id.spinner_api_provider);
        Spinner spinnerModel = dialogView.findViewById(R.id.spinner_model);

        boolean isEditing = (agentToEdit != null);
        String dialogTitle = isEditing ? "Edit Agent" : "Create New Agent";
        String positiveButtonText = isEditing ? "Save" : "Create";

        if (isEditing) {
            editTextAgentName.setText(agentToEdit.getName());
            editTextAgentPrompt.setText(agentToEdit.getPrompt());
            editTextAgentEmoji.setText(agentToEdit.getEmoji());
        }

        // Populate API Provider Spinner
        List<String> apiProviders = Arrays.asList("Deepseek", "Gemini", "Alibaba");
        ArrayAdapter<String> apiProviderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, apiProviders);
        apiProviderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerApiProvider.setAdapter(apiProviderAdapter);

        if (isEditing) {
            int providerPosition = apiProviderAdapter.getPosition(agentToEdit.getApiProvider());
            if (providerPosition != -1) {
                spinnerApiProvider.setSelection(providerPosition);
            }
        }

        // Listener for API Provider selection to update models
        spinnerApiProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProvider = (String) parent.getItemAtPosition(position);
                fetchAndPopulateModels(selectedProvider, spinnerModel, agentToEdit != null ? agentToEdit.getModel() : null);

                // Show/hide API key sections based on provider
                if ("Alibaba".equals(selectedProvider)) {
                    rootView.findViewById(R.id.deepseek_api_key_layout).setVisibility(View.GONE);
                    rootView.findViewById(R.id.gemini_api_key_layout).setVisibility(View.GONE);
                } else {
                    rootView.findViewById(R.id.deepseek_api_key_layout).setVisibility(View.VISIBLE);
                    rootView.findViewById(R.id.gemini_api_key_layout).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Initial model population based on default or existing agent's provider
        String initialProvider = isEditing ? agentToEdit.getApiProvider() : apiProviders.get(0);
        fetchAndPopulateModels(initialProvider, spinnerModel, agentToEdit != null ? agentToEdit.getModel() : null);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(dialogTitle)
                .setView(dialogView)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    String name = editTextAgentName.getText().toString().trim();
                    String prompt = editTextAgentPrompt.getText().toString().trim();
                    String emoji = editTextAgentEmoji.getText().toString().trim();
                    String apiProvider = (String) spinnerApiProvider.getSelectedItem();
                    String selectedModelText = (String) spinnerModel.getSelectedItem();
                    String model;
                    if (selectedModelText != null && selectedModelText.contains("(")) {
                        model = selectedModelText.substring(selectedModelText.indexOf("(") + 1, selectedModelText.indexOf(")"));
                    } else {
                        model = selectedModelText;
                    }


                    if (name.isEmpty() || prompt.isEmpty() || emoji.isEmpty() || apiProvider == null || model == null) {
                        Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isEditing) {
                        agentToEdit.setName(name);
                        agentToEdit.setPrompt(prompt);
                        agentToEdit.setEmoji(emoji);
                        agentToEdit.setModel(model);
                        agentToEdit.setApiProvider(apiProvider);
                        sharedPrefManager.updateAgent(agentToEdit);
                        Toast.makeText(requireContext(), "Agent updated!", Toast.LENGTH_SHORT).show();
                    } else {
                        Agent newAgent = new Agent(UUID.randomUUID().toString(), name, prompt, emoji, model, apiProvider);
                        sharedPrefManager.addAgent(newAgent);
                        Toast.makeText(requireContext(), "Agent created!", Toast.LENGTH_SHORT).show();
                    }
                    agentAdapter.updateAgents(sharedPrefManager.getAgents());
                    refreshAgents(); // Refresh the list after adding/updating an agent
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void fetchAndPopulateModels(String apiProvider, Spinner spinner, @Nullable String selectedModel) {
        List<String> models = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        if ("Deepseek".equals(apiProvider)) {
            String deepseekApiKey = sharedPrefManager.getDeepseekApiKey();
            if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
                DeepseekApi deepseekApi = new DeepseekApi(deepseekApiKey);
                deepseekApi.getModels(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to fetch Deepseek models: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        latch.countDown();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                JSONObject jsonResponse = new JSONObject(response.body().string());
                                JSONArray data = jsonResponse.getJSONArray("data");
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject modelObject = data.getJSONObject(i);
                                    models.add(modelObject.getString("id"));
                                }
                            } catch (JSONException e) {
                                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to parse Deepseek models: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        } else {
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to fetch Deepseek models: " + response.message(), Toast.LENGTH_LONG).show());
                        }
                        latch.countDown();
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Deepseek API Key not set!", Toast.LENGTH_LONG).show();
                latch.countDown();
            }
        } else if ("Gemini".equals(apiProvider)) {
            String geminiApiKey = sharedPrefManager.getGeminiApiKey();
            if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
                GeminiApi geminiApi = new GeminiApi(geminiApiKey);
                geminiApi.getModels(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to fetch Gemini models: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        latch.countDown();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                JSONObject jsonResponse = new JSONObject(response.body().string());
                                JSONArray modelsArray = jsonResponse.getJSONArray("models");
                                for (int i = 0; i < modelsArray.length(); i++) {
                                    JSONObject modelObject = modelsArray.getJSONObject(i);
                                    String modelName = modelObject.getString("name").replace("models/", "");
                                    String contextWindow = "N/A";
                                    String rateLimit = "N/A";

                                    if (modelObject.has("inputTokenLimit") && modelObject.has("outputTokenLimit")) {
                                        int inputLimit = modelObject.getInt("inputTokenLimit");
                                        int outputLimit = modelObject.getInt("outputTokenLimit");
                                        contextWindow = (inputLimit + outputLimit) + " tokens";
                                    }

                                    // Hardcode rate limits based on web search results
                                    if (modelName.contains("gemini-1.5-flash")) {
                                        rateLimit = "15 RPM (free), 2,000 RPM (paid)";
                                    } else if (modelName.contains("gemini-1.5-pro")) {
                                        rateLimit = "2 RPM (free), 1,000 RPM (paid)";
                                    } else if (modelName.equals("gemini-pro") || modelName.equals("gemini-pro-vision")) {
                                        rateLimit = "60 RPM";
                                    }

                                    // Filter for chat-capable models and exclude vision models for now, unless it's gemini-pro-vision
                                    if (modelObject.has("supportedGenerationMethods") &&
                                        modelObject.getJSONArray("supportedGenerationMethods").toString().contains("GENERATE_CONTENT") &&
                                        (!modelName.contains("vision") || modelName.equals("gemini-pro-vision"))) {
                                        models.add(modelName + " (Context: " + contextWindow + ", Rate: " + rateLimit + ")");
                                    }
                                }
                            } catch (JSONException e) {
                                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to parse Gemini models: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        } else {
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to fetch Gemini models: " + response.message(), Toast.LENGTH_LONG).show());
                        }
                        latch.countDown();
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Gemini API Key not set!", Toast.LENGTH_LONG).show();
                latch.countDown();
            }
        } else if ("Alibaba".equals(apiProvider)) {
            QwenApi qwenApi = new QwenApi();
            qwenApi.getModels(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to fetch Qwen models: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    latch.countDown();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response.body().string());
                            JSONArray data = jsonResponse.getJSONArray("data");
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject modelObject = data.getJSONObject(i);
                                String modelId = modelObject.getString("id");
                                String modelName = modelObject.getString("name");
                                boolean supportsThinking = modelObject.getJSONObject("info").getJSONObject("capabilities").optBoolean("thinking", false);
                                models.add(modelName + " (" + modelId + ")" + (supportsThinking ? " (Thinking)" : ""));
                            }
                        } catch (JSONException e) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to parse Qwen models: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    } else {
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to fetch Qwen models: " + response.message(), Toast.LENGTH_LONG).show());
                    }
                    latch.countDown();
                }
            });
        }

        new Thread(() -> {
            try {
                latch.await(); // Wait for API call to complete
                requireActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, models);
                    modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(modelAdapter);
                    if (selectedModel != null) {
                        int spinnerPosition = modelAdapter.getPosition(selectedModel);
                        if (spinnerPosition != -1) {
                            spinner.setSelection(spinnerPosition);
                        }
                    }
                });
            } catch (InterruptedException e) {
                Log.e("SettingsFragment", "Model fetch interrupted: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void onAgentClick(Agent agent) {
        showCreateAgentDialog(agent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}