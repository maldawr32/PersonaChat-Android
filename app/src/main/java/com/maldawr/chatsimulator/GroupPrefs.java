package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.SharedPreferences;

public final class GroupPrefs {
    private static final String PREFS = "personachat_group_prefs";
    private GroupPrefs() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int getActivity(Context c, long botId) {
        return prefs(c).getInt("activity_" + botId, Store.getGroupActivity(c));
    }

    public static void setActivity(Context c, long botId, int value) {
        prefs(c).edit().putInt("activity_" + botId, Math.max(0, Math.min(100, value))).apply();
    }

    public static boolean isAutonomous(Context c, long botId) {
        return prefs(c).getBoolean("autonomous_" + botId, true);
    }

    public static void setAutonomous(Context c, long botId, boolean value) {
        prefs(c).edit().putBoolean("autonomous_" + botId, value).apply();
    }
}
