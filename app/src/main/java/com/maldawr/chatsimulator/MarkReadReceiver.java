package com.maldawr.chatsimulator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MarkReadReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        long botId = intent.getLongExtra("bot_id", -1L);
        if (botId == -1L) return;
        Store.ensureSeeded(context);
        Store.markRead(context, botId);
    }
}
