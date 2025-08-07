package com.relation.shit;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Process;
import androidx.annotation.NonNull;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler(this));
    }

    private static class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Context context;
        private final Thread.UncaughtExceptionHandler defaultUEH;

        GlobalExceptionHandler(Context context) {
            this.context = context;
            this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();

            SharedPreferences prefs = context.getSharedPreferences("CrashLog", MODE_PRIVATE);
            prefs.edit().putString("stackTrace", stackTrace).commit();

            Intent intent = new Intent(context, DebugActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);

            Process.killProcess(Process.myPid());
            System.exit(10);
        }
    }
}
