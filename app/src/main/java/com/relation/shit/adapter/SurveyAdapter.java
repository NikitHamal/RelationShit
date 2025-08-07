package com.relation.shit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.relation.shit.R;
import com.relation.shit.model.Survey;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SurveyAdapter extends RecyclerView.Adapter<SurveyAdapter.SurveyViewHolder> {

    private List<Survey> surveyList;
    private OnSurveyClickListener listener;

    public interface OnSurveyClickListener {
        void onSurveyClick(Survey survey);
    }

    public SurveyAdapter(List<Survey> surveyList, OnSurveyClickListener listener) {
        this.surveyList = surveyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SurveyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_survey, parent, false);
        return new SurveyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SurveyViewHolder holder, int position) {
        Survey survey = surveyList.get(position);
        holder.surveyTitle.setText("Survey from " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(survey.getTimestamp()));
        holder.itemView.setOnClickListener(v -> listener.onSurveyClick(survey));
    }

    @Override
    public int getItemCount() {
        return surveyList.size();
    }

    static class SurveyViewHolder extends RecyclerView.ViewHolder {
        TextView surveyTitle;

        SurveyViewHolder(@NonNull View itemView) {
            super(itemView);
            surveyTitle = itemView.findViewById(R.id.survey_title);
        }
    }
}