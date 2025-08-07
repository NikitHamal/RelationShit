package com.relation.shit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_activity);

        TextView crashLogTextView = findViewById(R.id.crash_log_textview);
        MaterialButton copyButton = findViewById(R.id.button_copy_log);
        MaterialButton restartButton = findViewById(R.id.button_restart_app);

        SharedPreferences prefs = getSharedPreferences("CrashLog", MODE_PRIVATE);
        String stackTrace = prefs.getString("stackTrace", "No crash log found.");
        crashLogTextView.setText(stackTrace);

        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Log", stackTrace);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Log copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        restartButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
