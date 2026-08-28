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
        boolean forceGroupTest = getInputData().getBoolean("force_group_test", false);
        long forceGroupId = getInputData().getLong("force_group_id", -1L);

        if (forceGroupTest) return runForcedGroupTest(context, forceGroupId);
        if (!Store.isAutomationEnabled(context) || Store.isGlobalQuietTime(context)) return Result.success();

        List<Store.Bot> candidates = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Store.Bot bot : Store.loadBots(context)) {
            if (!bot.initiative || !bot.autoReply || !Store.isBotActive(bot)) continue;
            if (bot.groupChat && !GroupPrefs.isAutonomous(context, bot.id)) continue;
            int groupActivity = bot.groupChat ? GroupPrefs.getActivity(context, bot.id) : 0;
            long gap = bot.groupChat ? randomGroupGap(groupActivity) : 24 * 60_000L;
            if (now - bot.lastTime >= gap) candidates.add(bot);
        }
        Collections.shuffle(candidates, RANDOM);
        int count = candidates.isEmpty() ? 0 : (RANDOM.nextInt(100) < 20 ? 2 : 1);
        count = Math.min(count, candidates.size());
        for (int i = 0; i < count; i++) {
            Store.Bot bot = candidates.get(i);
            int chance = bot.groupChat ? GroupPrefs.getActivity(context, bot.id) : 48;
            if (RANDOM.nextInt(100) >= chance) continue;
            if (bot.groupChat && DeepSeekPrefs.hasApiKey(context)) {
                try {
                    Store.ReplyPlan aiRound = GroupAiRoundClient.requestBlocking(context, bot, false);
                    ReplyScheduler.schedulePlan(context, bot.id, aiRound);
                    continue;
                } catch (Exception ignored) {}
            }
            Store.ReplyPlan plan = ConversationEngine.planProactive(context, bot);
            if (!plan.isEmpty()) ReplyScheduler.schedulePlan(context, bot.id, plan);
            if (!plan.onlineTopic.isEmpty()) {
                long delay = plan.items.isEmpty() ? 6_000L : plan.items.get(plan.items.size() - 1).delayMs + 7_000L;
                OnlineContentWorker.enqueue(context, bot.id, plan.onlineTopic, delay);
            }
        }
        return Result.success();
    }

    private Result runForcedGroupTest(Context context, long requestedId) {
        Store.Bot bot = requestedId > 0 ? Store.getBot(context, requestedId) : null;
        if (bot == null || !bot.groupChat) {
            List<Store.Bot> groups = new ArrayList<>();
            for (Store.Bot candidate : Store.loadBots(context)) if (candidate.groupChat && candidate.autoReply) groups.add(candidate);
            if (groups.isEmpty()) return Result.success();
            bot = groups.get(RANDOM.nextInt(groups.size()));
        }

        if (DeepSeekPrefs.hasApiKey(context)) {
            try {
                Store.ReplyPlan aiRound = GroupAiRoundClient.requestBlocking(context, bot, true);
                ReplyScheduler.schedulePlan(context, bot.id, aiRound);
                return Result.success();
            } catch (Exception ignored) {}
        }
        Store.ReplyPlan local = ConversationEngine.planProactive(context, bot);
        if (!local.isEmpty()) ReplyScheduler.schedulePlan(context, bot.id, local);
        return Result.success();
    }

    private static long randomGroupGap(int activity) {
        long minMinutes;
        long maxMinutes;
        if (activity >= 85) { minMinutes = 8; maxMinutes = 20; }
        else if (activity >= 65) { minMinutes = 12; maxMinutes = 35; }
        else if (activity >= 40) { minMinutes = 20; maxMinutes = 55; }
        else { minMinutes = 35; maxMinutes = 90; }
        long minutes = minMinutes + RANDOM.nextInt((int) (maxMinutes - minMinutes + 1));
        return minutes * 60_000L;
    }
}
