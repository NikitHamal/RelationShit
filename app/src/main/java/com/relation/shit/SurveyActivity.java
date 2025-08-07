package com.relation.shit;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.relation.shit.model.Survey;
import com.relation.shit.model.SurveyAnswer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SurveyActivity extends AppCompatActivity {

    private Survey survey;
    private Typeface regTypeface;
    private Typeface semTypeface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey);

        regTypeface = ResourcesCompat.getFont(this, R.font.reg);
        semTypeface = ResourcesCompat.getFont(this, R.font.sem);

        survey = (Survey) getIntent().getSerializableExtra("survey");

        MaterialToolbar toolbar = findViewById(R.id.survey_history_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ImageView backButton = findViewById(R.id.survey_history_back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        TextView surveyDate = findViewById(R.id.survey_date);
        surveyDate.setText("Taken on " + new SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault()).format(survey.getTimestamp()));

        LinearLayout surveyDetailsContainer = findViewById(R.id.survey_details_container);
        for (SurveyAnswer answer : survey.getAnswers()) {
            View questionAnswerView = getLayoutInflater().inflate(R.layout.item_survey_question_answer, surveyDetailsContainer, false);
            TextView question = questionAnswerView.findViewById(R.id.question_text);
            TextView answerText = questionAnswerView.findViewById(R.id.answer_text);
            question.setText(answer.getQuestion());
            answerText.setText(answer.getAnswer());
            surveyDetailsContainer.addView(questionAnswerView);
        }

        LinearLayout reportContainer = findViewById(R.id.report_container);
        displayAnalysisReport(survey.getAnalysisReport(), reportContainer);
    }

    private void displayAnalysisReport(String jsonReport, LinearLayout container) {
        try {
            JSONObject report = new JSONObject(jsonReport);

            // Summary
            addCollapsibleSection(container, "Summary", report.getString("summary"));

            // Detailed Analysis
            JSONObject detailedAnalysis = report.getJSONObject("detailedAnalysis");
            LinearLayout detailedAnalysisContent = new LinearLayout(this);
            detailedAnalysisContent.setOrientation(LinearLayout.VERTICAL);
            addSectionTitle(container, "Detailed Analysis", detailedAnalysisContent);
            JSONArray detailedAnalysisKeys = detailedAnalysis.names();
            if (detailedAnalysisKeys != null) {
                for (int i = 0; i < detailedAnalysisKeys.length(); i++) {
                    String key = detailedAnalysisKeys.getString(i);
                    addBulletPoint(detailedAnalysisContent, key, detailedAnalysis.getString(key));
                }
            }
            container.addView(detailedAnalysisContent);

            // Recommendations
            JSONObject recommendations = report.getJSONObject("recommendations");
            LinearLayout recommendationsContent = new LinearLayout(this);
            recommendationsContent.setOrientation(LinearLayout.VERTICAL);
            addSectionTitle(container, "Recommendations", recommendationsContent);

            addSubsectionTitle(recommendationsContent, "Immediate Actions");
            addBulletPoints(recommendationsContent, recommendations.getJSONArray("immediateActions"));

            addSubsectionTitle(recommendationsContent, "Long Term Strategies");
            addBulletPoints(recommendationsContent, recommendations.getJSONArray("longTermStrategies"));

            addSubsectionTitle(recommendationsContent, "Self Reflection");
            addBulletPoints(recommendationsContent, recommendations.getJSONArray("selfReflection"));
            container.addView(recommendationsContent);

            // Conclusion
            addCollapsibleSection(container, "Conclusion", report.getString("conclusion"));

        } catch (JSONException e) {
            e.printStackTrace();
            // Fallback to showing raw JSON if parsing fails
            TextView errorTextView = new TextView(this);
            errorTextView.setText("Error parsing report: " + e.getMessage() + "\nRaw Report: " + jsonReport);
            errorTextView.setTypeface(regTypeface);
            errorTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Default error text size
            errorTextView.setTextIsSelectable(true);
            container.addView(errorTextView);
        }
    }

    private void addCollapsibleSection(LinearLayout parent, String title, String content) {
        LinearLayout sectionLayout = new LinearLayout(this);
        sectionLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, dpToPx(16), 0, 0); // Increased top margin
        sectionLayout.setLayoutParams(layoutParams);

        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleLayout.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12)); // Increased vertical padding
        titleLayout.setBackgroundResource(R.drawable.rounded_item_background); // Apply a background for visual separation

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTypeface(semTypeface);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // Max 18sp
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleView.setLayoutParams(titleParams);
        titleLayout.addView(titleView);

        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.icon_arrow_down_round); // Assuming you have a down arrow icon
        arrow.setColorFilter(getResources().getColor(R.color.md_theme_onSurface, getTheme()));
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)); // Max 24dp
        arrow.setLayoutParams(arrowParams);
        titleLayout.addView(arrow);

        final LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setVisibility(View.GONE);
        contentLayout.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

        TextView contentView = new TextView(this);
        contentView.setText(content);
        contentView.setTypeface(regTypeface);
        contentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Max 18sp, slightly smaller for content
        contentView.setTextIsSelectable(true);
        contentLayout.addView(contentView);

        titleLayout.setOnClickListener(v -> {
            if (contentLayout.getVisibility() == View.GONE) {
                contentLayout.setVisibility(View.VISIBLE);
                arrow.setRotation(180);
            } else {
                contentLayout.setVisibility(View.GONE);
                arrow.setRotation(0);
            }
        });

        sectionLayout.addView(titleLayout);
        sectionLayout.addView(contentLayout);
        parent.addView(sectionLayout);
    }

    private void addSectionTitle(LinearLayout parent, String title, final LinearLayout contentToToggle) {
        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(8)); // Added horizontal padding
        titleLayout.setBackgroundResource(R.drawable.rounded_item_background); // Apply a background for visual separation
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, dpToPx(16), 0, 0); // Increased top margin
        titleLayout.setLayoutParams(layoutParams);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTypeface(semTypeface);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // Max 18sp
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleView.setLayoutParams(titleParams);
        titleLayout.addView(titleView);

        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.icon_arrow_down_round); // Assuming you have a down arrow icon
        arrow.setColorFilter(getResources().getColor(R.color.md_theme_onSurface, getTheme()));
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)); // Max 24dp
        arrow.setLayoutParams(arrowParams);
        titleLayout.addView(arrow);

        titleLayout.setOnClickListener(v -> {
            if (contentToToggle.getVisibility() == View.GONE) {
                contentToToggle.setVisibility(View.VISIBLE);
                arrow.setRotation(180);
            } else {
                contentToToggle.setVisibility(View.GONE);
                arrow.setRotation(0);
            }
        });

        parent.addView(titleLayout);
    }

    private void addSubsectionTitle(LinearLayout parent, String title) {
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTypeface(semTypeface);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Max 18sp, slightly smaller for subsection titles
        titleView.setPadding(0, dpToPx(8), 0, dpToPx(4)); // Add 4dp bottom padding
        parent.addView(titleView);
    }

    private void addBulletPoint(LinearLayout parent, String key, String value) {
        TextView bulletView = new TextView(this);
        bulletView.setText("\u2022 " + key + ": " + value);
        bulletView.setTypeface(regTypeface);
        bulletView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Max 18sp, smaller for bullet points
        bulletView.setPadding(dpToPx(16), 0, 0, dpToPx(4)); // Indent and add 4dp bottom padding
        bulletView.setTextIsSelectable(true);
        parent.addView(bulletView);
    }

    private void addBulletPoints(LinearLayout parent, org.json.JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            TextView bulletView = new TextView(this);
            bulletView.setText("\u2022 " + array.getString(i));
            bulletView.setTypeface(regTypeface);
            bulletView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Max 18sp, smaller for bullet points
            bulletView.setPadding(dpToPx(16), 0, 0, dpToPx(4)); // Indent and add 4dp bottom padding
            bulletView.setTextIsSelectable(true);
            parent.addView(bulletView);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private float spToPx(int sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}