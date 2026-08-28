package com.maldawr.chatsimulator;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReplyWorker extends Worker {
    public ReplyWorker(@NonNull Context appContext, @NonNull WorkerParameters params) { super(appContext, params); }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        long botId = getInputData().getLong("bot_id", -1L);
        if (botId == -1L) return Result.failure();
        Store.Bot bot = Store.getBot(context, botId);
        if (bot == null) return Result.failure();
        String text = value(getInputData().getString("text"));
        String sender = value(getInputData().getString("sender"));
        String reaction = value(getInputData().getString("reaction"));
        String kind = value(getInputData().getString("kind"));
        long replyToId = getInputData().getLong("reply_to_id", 0L);
        if (sender.isEmpty()) sender = bot.name;
        if ("reaction".equals(kind)) {
            Store.applyReaction(context, botId, replyToId, reaction, sender);
            return Result.success();
        }
        if (text.trim().isEmpty()) return Result.success();
        Store.addMessage(context, new Store.Message(Store.nextMessageId(), botId, text, true, System.currentTimeMillis(), sender, "", replyToId, "text"));
        Store.Bot updated = Store.getBot(context, botId);
        if (updated != null && !ChatActivity.isConversationVisible(botId)) NotificationHelper.showMessage(context, updated, text);
        return Result.success();
    }

    private static String value(String value) { return value == null ? "" : value; }
}
