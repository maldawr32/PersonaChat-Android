package com.maldawr.chatsimulator;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class ReplyScheduler {
    private ReplyScheduler() {}

    public static void schedulePlan(Context context, long botId, Store.ReplyPlan plan) {
        if (plan == null || plan.isEmpty()) return;
        for (int i = 0; i < plan.items.size(); i++) {
            Store.PlannedMessage item = plan.items.get(i);
            scheduleOne(context, botId, item.text, item.sender, item.reaction, item.replyToId, item.kind, System.currentTimeMillis() + item.delayMs, i);
        }
    }

    public static void scheduleOne(Context context, long botId, String text, long triggerAt, int sequence) {
        scheduleOne(context, botId, text, "", "", 0L, "text", triggerAt, sequence);
    }

    public static void scheduleOne(Context context, long botId, String text, String sender, String reaction, long replyToId, String kind, long triggerAt, int sequence) {
        long delay = Math.max(0L, triggerAt - System.currentTimeMillis());
        Data data = new Data.Builder()
                .putLong("bot_id", botId)
                .putString("text", text == null ? "" : text)
                .putString("sender", sender == null ? "" : sender)
                .putString("reaction", reaction == null ? "" : reaction)
                .putLong("reply_to_id", replyToId)
                .putString("kind", kind == null ? "text" : kind)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReplyWorker.class)
                .setInputData(data)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("sim-reply-" + botId)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueue(request);
    }
}
