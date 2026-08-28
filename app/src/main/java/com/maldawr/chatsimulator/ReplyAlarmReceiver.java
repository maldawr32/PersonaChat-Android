package com.maldawr.chatsimulator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReplyAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Store.ensureSeeded(context);
        long botId = intent.getLongExtra("bot_id", -1L);
        String text = intent.getStringExtra("text");
        if (botId == -1L || text == null || text.trim().isEmpty()) return;

        Store.Bot bot = Store.getBot(context, botId);
        if (bot == null) return;

        long now = System.currentTimeMillis();
        Store.addMessage(context, new Store.Message(now, botId, text, true, now));
        Store.Bot updated = Store.getBot(context, botId);
        if (updated != null && !ChatActivity.isConversationVisible(botId)) {
            NotificationHelper.showMessage(context, updated, text);
        }
    }
}
