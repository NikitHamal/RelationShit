package com.relation.shit.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.relation.shit.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.core.content.res.ResourcesCompat;

public class SimpleLineChartView extends View {

    private List<Float> values = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private Paint linePaint;
    private Paint textPaint;
    private Paint gridPaint;
    private Paint pointPaint;

    private int primaryColor;
    private int onSurfaceVariantColor;
    private int outlineColor;

    public SimpleLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        primaryColor = ContextCompat.getColor(context, R.color.md_theme_primary);
        onSurfaceVariantColor = ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant);
        outlineColor = ContextCompat.getColor(context, R.color.md_theme_outline);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(primaryColor);
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(onSurfaceVariantColor);
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(ResourcesCompat.getFont(context, R.font.reg));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(outlineColor);
        gridPaint.setStrokeWidth(1f);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(primaryColor);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<Float> values, List<String> labels) {
        this.values = values;
        this.labels = labels;
        invalidate(); // Redraw the view with new data
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (values == null || values.isEmpty()) {
            return;
        }

        float padding = 60f;
        float chartWidth = getWidth() - 2 * padding;
        float chartHeight = getHeight() - 2 * padding;

        // Find min and max values for scaling
        float maxValue = Float.MIN_VALUE;
        float minValue = Float.MAX_VALUE;
        for (Float value : values) {
            if (value > maxValue) maxValue = value;
            if (value < minValue) minValue = value;
        }

        float valueRange = maxValue - minValue;
        if (valueRange == 0) {
            valueRange = 1; // Avoid division by zero if all values are the same
        }

        float xStep = chartWidth / (values.size() - 1);

        // Draw horizontal grid lines and Y-axis labels
        int numHorizontalLines = 5;
        for (int i = 0; i < numHorizontalLines; i++) {
            float y = padding + (chartHeight / (numHorizontalLines - 1)) * i;
            canvas.drawLine(padding, y, padding + chartWidth, y, gridPaint);

            // Y-axis labels
            float value = maxValue - (valueRange / (numHorizontalLines - 1)) * i;
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", value), padding - 20, y + textPaint.getTextSize() / 3, textPaint);
        }

        // Draw vertical grid lines and X-axis labels
        for (int i = 0; i < labels.size(); i++) {
            float x = padding + i * xStep;
            canvas.drawLine(x, padding, x, padding + chartHeight, gridPaint);

            // X-axis labels
            canvas.drawText(labels.get(i), x, getHeight() - padding + 40, textPaint);
        }

        // Draw line and data points
        for (int i = 0; i < values.size(); i++) {
            float x = padding + i * xStep;
            float y = padding + chartHeight - ((values.get(i) - minValue) / valueRange * chartHeight);

            // Draw line segment
            if (i < values.size() - 1) {
                float nextX = padding + (i + 1) * xStep;
                float nextY = padding + chartHeight - ((values.get(i + 1) - minValue) / valueRange * chartHeight);
                canvas.drawLine(x, y, nextX, nextY, linePaint);
            }

            // Draw data point
            canvas.drawCircle(x, y, 10f, pointPaint);
        }
    }
}