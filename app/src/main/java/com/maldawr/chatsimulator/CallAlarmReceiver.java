package com.maldawr.chatsimulator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CallAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Store.ensureSeeded(context);
        long botId = intent.getLongExtra("bot_id", -1L);
        Store.Bot bot = Store.getBot(context, botId);
        if (bot == null) return;
        long now = System.currentTimeMillis();
        Store.addCall(context, new Store.CallItem(now, botId, "missed", now, 0));
        NotificationHelper.showCall(context, bot);
    }
}
