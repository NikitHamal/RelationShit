package com.relation.shit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.relation.shit.adapter.SurveyAdapter;
import com.relation.shit.api.DeepseekApiService;
import com.relation.shit.api.GeminiApiService;
import com.relation.shit.api.BaseApiService;
import com.relation.shit.model.Conversation;
import com.relation.shit.model.Insight;
import com.relation.shit.model.Message;
import com.relation.shit.model.Survey;
import com.relation.shit.model.SurveyAnswer;
import com.relation.shit.utils.SharedPrefManager;
import com.relation.shit.view.SimpleLineChartView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

public class InsightsActivity extends AppCompatActivity implements SurveyAdapter.OnSurveyClickListener {

    private SharedPrefManager sharedPrefManager;
    private BaseApiService apiService;
    private String conversationId;
    private Insight currentInsight;
    private List<Survey> surveyList;
    private SurveyAdapter surveyAdapter;

    private MaterialToolbar insightsToolbar;
    private ImageView insightsBackButton;
    private TextView insightsTitle;

    private LinearLayout emptyInsightsState;
    private LinearLayout loadingInsightsState;
    private LinearLayout contentInsightsState;
    private LinearLayout insightsContainer;
    private LinearLayout surveyContainer;
    private LinearLayout surveyHistoryLayout;
    private RecyclerView surveyHistoryRecyclerView;

    private MaterialButton generateInsightsButton;
    private MaterialButton refreshInsightsButton;
    private MaterialButton psychologicalSurveyButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insights);

        sharedPrefManager = new SharedPrefManager(this);

        // Initialize the correct API service based on the conversation's agent's API provider
        Conversation conversation = sharedPrefManager.getConversationById(conversationId);
        if (conversation == null) {
            Toast.makeText(this, "Error: Conversation not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if ("Deepseek".equals(conversation.getAgent().getApiProvider())) {
            apiService = new DeepseekApiService(sharedPrefManager.getDeepseekApiKey());
        } else if ("Gemini".equals(conversation.getAgent().getApiProvider())) {
            apiService = new GeminiApiService(sharedPrefManager.getGeminiApiKey());
        } else {
            Toast.makeText(this, "Error: Unknown API provider.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        conversationId = getIntent().getStringExtra("conversation_id");
        if (conversationId == null) {
            Toast.makeText(this, "Error: Conversation ID not provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        updateUiState();

        generateInsightsButton.setOnClickListener(v -> generateInsights());
        refreshInsightsButton.setOnClickListener(v -> generateInsights());
        psychologicalSurveyButton.setOnClickListener(v -> generatePsychologicalSurvey());
    }

    private void initializeViews() {
        insightsToolbar = findViewById(R.id.insights_toolbar);
        insightsBackButton = findViewById(R.id.insights_back_button);
        insightsTitle = findViewById(R.id.insights_title);

        emptyInsightsState = findViewById(R.id.empty_insights_state);
        loadingInsightsState = findViewById(R.id.loading_insights_state);
        contentInsightsState = findViewById(R.id.content_insights_state);
        insightsContainer = findViewById(R.id.insights_container);
        surveyContainer = findViewById(R.id.survey_container);
        surveyHistoryLayout = findViewById(R.id.survey_history_layout);
        surveyHistoryRecyclerView = findViewById(R.id.survey_history_recyclerview);

        generateInsightsButton = findViewById(R.id.button_generate_insights);
        refreshInsightsButton = findViewById(R.id.button_refresh_insights);
        psychologicalSurveyButton = findViewById(R.id.button_psychological_survey);
    }

    private void setupToolbar() {
        setSupportActionBar(insightsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        insightsBackButton.setOnClickListener(v -> onBackPressed());
    }

    private void updateUiState() {
        currentInsight = sharedPrefManager.getInsightForConversation(conversationId);
        if (currentInsight == null) {
            showEmptyState();
        } else {
            displayInsights(currentInsight.getContent());
            showContentState();
        }
        loadSurveys();
    }

    private void showEmptyState() {
        emptyInsightsState.setVisibility(View.VISIBLE);
        loadingInsightsState.setVisibility(View.GONE);
        contentInsightsState.setVisibility(View.GONE);
    }

    private void showLoadingState() {
        emptyInsightsState.setVisibility(View.GONE);
        loadingInsightsState.setVisibility(View.VISIBLE);
        contentInsightsState.setVisibility(View.GONE);
    }

    private void showContentState() {
        emptyInsightsState.setVisibility(View.GONE);
        loadingInsightsState.setVisibility(View.GONE);
        contentInsightsState.setVisibility(View.VISIBLE);
    }

    private void generateInsights() {
        showLoadingState();
        Conversation conversation = sharedPrefManager.getConversationById(conversationId);
        if (conversation == null || conversation.getMessages().isEmpty()) {
            Toast.makeText(this, "Conversation is empty.", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        StringBuilder historyBuilder = new StringBuilder();
        for (Message msg : conversation.getMessages()) {
            historyBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        apiService.generateInsights(
            historyBuilder.toString(),
            new BaseApiService.ApiResponseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(
                        () -> {
                            if (currentInsight == null) {
                                currentInsight = new Insight(UUID.randomUUID().toString(), conversationId, result, System.currentTimeMillis());
                                sharedPrefManager.addInsight(currentInsight);
                            } else {
                                currentInsight = new Insight(currentInsight.getId(), conversationId, result, System.currentTimeMillis());
                                sharedPrefManager.updateInsight(currentInsight);
                            }
                            displayInsights(result);
                            showContentState();
                        });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(
                        () -> {
                            Toast.makeText(InsightsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            updateUiState();
                        });
                }
            });
    }

    private void displayInsights(String insightsJson) {
        insightsContainer.removeAllViews();
        try {
            JSONObject insights = new JSONObject(insightsJson);
            JSONArray insightsArray = insights.getJSONArray("insights");

            for (int i = 0; i < insightsArray.length(); i++) {
                JSONObject insight = insightsArray.getJSONObject(i);
                String type = insight.getString("type");

                switch (type) {
                    case "chart":
                        addChartView(insight);
                        break;
                    case "progress":
                        addProgressView(insight);
                        break;
                    case "text":
                        addTextView(insight);
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to parse insights.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addChartView(JSONObject insight) throws JSONException {
        View chartView = getLayoutInflater().inflate(R.layout.item_insight_chart, insightsContainer, false);
        TextView chartTitle = chartView.findViewById(R.id.chart_title);
        SimpleLineChartView lineChart = chartView.findViewById(R.id.line_chart);

        chartTitle.setText(insight.getString("title"));

        JSONObject data = insight.getJSONObject("data");
        JSONArray labelsJson = data.getJSONArray("labels");
        JSONArray valuesJson = data.getJSONArray("values");

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < labelsJson.length(); i++) {
            labels.add(labelsJson.getString(i));
        }

        List<Float> values = new ArrayList<>();
        for (int i = 0; i < valuesJson.length(); i++) {
            values.add((float) valuesJson.getDouble(i));
        }

        lineChart.setData(values, labels);
        insightsContainer.addView(chartView);
    }

    private void addProgressView(JSONObject insight) throws JSONException {
        View progressView = getLayoutInflater().inflate(R.layout.item_insight_progress, insightsContainer, false);
        TextView progressTitle = progressView.findViewById(R.id.progress_title);
        LinearProgressIndicator progressBar = progressView.findViewById(R.id.progress_bar);
        TextView progressDescription = progressView.findViewById(R.id.progress_description);

        progressTitle.setText(insight.getString("title"));
        progressBar.setProgress(insight.getInt("value"));
        progressDescription.setText(insight.getString("description"));

        insightsContainer.addView(progressView);
    }

    private void addTextView(JSONObject insight) throws JSONException {
        View textView = getLayoutInflater().inflate(R.layout.item_insight_text, insightsContainer, false);
        TextView textTitle = textView.findViewById(R.id.text_title);
        TextView textContent = textView.findViewById(R.id.text_content);

        textTitle.setText(insight.getString("title"));
        textContent.setText(insight.getString("content"));

        insightsContainer.addView(textView);
    }

    private void generatePsychologicalSurvey() {
        if (currentInsight == null) {
            Toast.makeText(this, "Generate insights first.", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView loadingText = loadingInsightsState.findViewById(R.id.loading_text);
        loadingText.setText("Generating survey...");
        showLoadingState();

        apiService.generatePsychologicalSurvey(
            currentInsight.getContent(),
            new BaseApiService.ApiResponseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(
                        () -> {
                            displaySurvey(result);
                            showContentState();
                            surveyContainer.setVisibility(View.VISIBLE);
                            insightsContainer.setVisibility(View.GONE);
                            surveyHistoryLayout.setVisibility(View.GONE);
                        });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(
                        () -> {
                            Toast.makeText(InsightsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            showContentState();
                            surveyContainer.setVisibility(View.GONE);
                            insightsContainer.setVisibility(View.VISIBLE);
                        });
                }
            });
    }

    private void displaySurvey(String surveyJson) {
        surveyContainer.removeAllViews();
        try {
            JSONObject survey = new JSONObject(surveyJson);
            JSONArray questions = survey.getJSONArray("questions");

            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                addQuestionView(question, i);
            }

            MaterialButton submitButton = new MaterialButton(this);
            submitButton.setText("Submit Answers");
            submitButton.setOnClickListener(v -> submitSurvey());
            surveyContainer.addView(submitButton);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to parse survey.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addQuestionView(JSONObject question, int index) throws JSONException {
        String type = question.getString("type");
        String questionText = question.getString("question");

        TextView questionTextView = new TextView(this);
        questionTextView.setText((index + 1) + ". " + questionText);
        questionTextView.setTextAppearance(R.style.TextAppearance_MaterialComponents_Headline6);
        questionTextView.setTypeface(ResourcesCompat.getFont(this, R.font.sem));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 24, 0, 8);
        questionTextView.setLayoutParams(params);
        surveyContainer.addView(questionTextView);

        questionTextView.setTag("question_" + index);

        switch (type) {
            case "multiple_choice":
                addMultipleChoiceOptions(question.getJSONArray("options"), index);
                break;
            case "single_choice":
                addSingleChoiceOptions(question.getJSONArray("options"), index);
                break;
            case "text":
                addTextField(index);
                break;
        }
    }

    private void addMultipleChoiceOptions(JSONArray options, int questionIndex) throws JSONException {
        LinearLayout checkboxGroup = new LinearLayout(this);
        checkboxGroup.setOrientation(LinearLayout.VERTICAL);
        checkboxGroup.setTag("answer_group_" + questionIndex);
        for (int i = 0; i < options.length(); i++) {
            MaterialCheckBox checkBox = new MaterialCheckBox(this);
            checkBox.setText(options.getString(i));
            checkBox.setTypeface(ResourcesCompat.getFont(this, R.font.reg));
            checkboxGroup.addView(checkBox);
        }
        surveyContainer.addView(checkboxGroup);
    }

    private void addSingleChoiceOptions(JSONArray options, int questionIndex) throws JSONException {
        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setTag("answer_group_" + questionIndex);
        for (int i = 0; i < options.length(); i++) {
            MaterialRadioButton radioButton = new MaterialRadioButton(this);
            radioButton.setText(options.getString(i));
            radioButton.setId(View.generateViewId());
            radioButton.setTypeface(ResourcesCompat.getFont(this, R.font.reg));
            radioGroup.addView(radioButton);
        }
        surveyContainer.addView(radioGroup);
    }

    private void addTextField(int questionIndex) {
        TextInputEditText editText = new TextInputEditText(this);
        editText.setHint("Your answer...");
        editText.setTag("answer_group_" + questionIndex);
        surveyContainer.addView(editText);
    }

    private void submitSurvey() {
        TextView loadingText = loadingInsightsState.findViewById(R.id.loading_text);
        loadingText.setText("Analyzing your answers...");
        showLoadingState();

        List<SurveyAnswer> answersList = new ArrayList<>();
        for (int i = 0; ; i++) {
            View questionView = surveyContainer.findViewWithTag("question_" + i);
            if (questionView == null) {
                break; // No more questions
            }
            String questionText = ((TextView) questionView).getText().toString();
            View answerGroup = surveyContainer.findViewWithTag("answer_group_" + i);

            if (answerGroup instanceof RadioGroup) {
                RadioGroup radioGroup = (RadioGroup) answerGroup;
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    MaterialRadioButton radioButton = radioGroup.findViewById(selectedId);
                    answersList.add(new SurveyAnswer(questionText, radioButton.getText().toString()));
                }
            } else if (answerGroup instanceof LinearLayout) { // Checkbox group
                LinearLayout checkboxGroup = (LinearLayout) answerGroup;
                StringBuilder selectedAnswers = new StringBuilder();
                for (int j = 0; j < checkboxGroup.getChildCount(); j++) {
                    View child = checkboxGroup.getChildAt(j);
                    if (child instanceof MaterialCheckBox && ((MaterialCheckBox) child).isChecked()) {
                        if (selectedAnswers.length() > 0) selectedAnswers.append(", ");
                        selectedAnswers.append(((MaterialCheckBox) child).getText().toString());
                    }
                }
                if (selectedAnswers.length() > 0) {
                    answersList.add(new SurveyAnswer(questionText, selectedAnswers.toString()));
                }
            } else if (answerGroup instanceof TextInputEditText) {
                TextInputEditText editText = (TextInputEditText) answerGroup;
                if (!editText.getText().toString().trim().isEmpty()) {
                    answersList.add(new SurveyAnswer(questionText, editText.getText().toString().trim()));
                }
            }
        }

        String surveyAnswersJson = new Gson().toJson(answersList);

        apiService.analyzeSurveyAnswers(
            currentInsight.getContent(),
            surveyAnswersJson,
            new BaseApiService.ApiResponseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(
                        () -> {
                            try {
                                JSONObject analysis = new JSONObject(result);
                                String updatedInsights = analysis.getString("updatedInsights");
                                String analysisReport = analysis.getString("analysisReport");

                                currentInsight = new Insight(currentInsight.getId(), conversationId, updatedInsights, System.currentTimeMillis());
                                sharedPrefManager.updateInsight(currentInsight);

                                Survey survey = new Survey(UUID.randomUUID().toString(), conversationId, answersList, analysisReport, System.currentTimeMillis());
                                sharedPrefManager.addSurveyToHistory(survey);

                                displayInsights(updatedInsights);
                                surveyContainer.setVisibility(View.GONE);
                                insightsContainer.setVisibility(View.VISIBLE);
                                loadSurveys();
                                showContentState();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(InsightsActivity.this, "Failed to parse analysis.", Toast.LENGTH_SHORT).show();
                                showContentState();
                            }
                        });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(
                        () -> {
                            Toast.makeText(InsightsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            showContentState();
                        });
                }
            });
    }

    private void loadSurveys() {
        surveyList = sharedPrefManager.getSurveyHistoryForConversation(conversationId);
        surveyAdapter = new SurveyAdapter(surveyList, this);
        surveyHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        surveyHistoryRecyclerView.setAdapter(surveyAdapter);
        surveyHistoryLayout.setVisibility(surveyList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onSurveyClick(Survey survey) {
        if (survey != null) {
            Intent intent = new Intent(InsightsActivity.this, SurveyActivity.class);
            intent.putExtra("survey", survey);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Error: Survey data is missing.", Toast.LENGTH_SHORT).show();
        }
    }
}