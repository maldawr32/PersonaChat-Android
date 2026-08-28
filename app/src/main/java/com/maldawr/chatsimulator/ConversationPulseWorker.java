package com.maldawr.chatsimulator;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ConversationPulseWorker extends Worker {
    private static final Random RANDOM = new Random();
    public ConversationPulseWorker(@NonNull Context appContext, @NonNull WorkerParameters params) { super(appContext, params); }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        Store.ensureSeeded(context);
        if (!Store.isAutomationEnabled(context) || Store.isGlobalQuietTime(context)) return Result.success();
        List<Store.Bot> candidates = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Store.Bot bot : Store.loadBots(context)) {
            if (!bot.initiative || !bot.autoReply || !Store.isBotActive(bot)) continue;
            long gap = bot.groupChat ? 12 * 60_000L : 24 * 60_000L;
            if (now - bot.lastTime >= gap) candidates.add(bot);
        }
        Collections.shuffle(candidates, RANDOM);
        int count = candidates.isEmpty() ? 0 : (RANDOM.nextInt(100) < 20 ? 2 : 1);
        count = Math.min(count, candidates.size());
        for (int i = 0; i < count; i++) {
            Store.Bot bot = candidates.get(i);
            int chance = bot.groupChat ? Store.getGroupActivity(context) : 48;
            if (RANDOM.nextInt(100) >= chance) continue;
            Store.ReplyPlan plan = ConversationEngine.planProactive(context, bot);
            if (!plan.isEmpty()) ReplyScheduler.schedulePlan(context, bot.id, plan);
            if (!plan.onlineTopic.isEmpty()) {
                long delay = plan.items.isEmpty() ? 6_000L : plan.items.get(plan.items.size() - 1).delayMs + 7_000L;
                OnlineContentWorker.enqueue(context, bot.id, plan.onlineTopic, delay);
            }
        }
        return Result.success();
    }
}
