package com.maldawr.chatsimulator;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class AutomationManager {
    private static final String PERIODIC = "v7-human-conversation-pulse";
    private static final String TEST = "v7-human-conversation-test";
    private AutomationManager() {}

    public static void ensureScheduled(Context context) {
        WorkManager wm = WorkManager.getInstance(context.getApplicationContext());
        if (!Store.isAutomationEnabled(context)) {
            wm.cancelUniqueWork(PERIODIC);
            return;
        }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ConversationPulseWorker.class, 15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES)
                .addTag("conversation-automation-v7")
                .build();
        wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void runNow(Context context) {
        Data data = new Data.Builder().putBoolean("force_group_test", true).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConversationPulseWorker.class)
                .setInputData(data)
                .addTag("conversation-automation-v7-test")
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(TEST, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancel(Context context) {
        WorkManager wm = WorkManager.getInstance(context.getApplicationContext());
        wm.cancelUniqueWork(PERIODIC);
        wm.cancelUniqueWork(TEST);
    }
}
