package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.SharedPreferences;

public final class DisplayPrefs {
    private static final String PREFS = "chat_simulator_display_v62";
    private static final String HOME_TITLE = "home_title";
    private static final String HOME_TITLE_SIZE = "home_title_size";

    private DisplayPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getHomeTitle(Context context) {
        return prefs(context).getString(HOME_TITLE, "PersonaChat");
    }

    public static void setHomeTitle(Context context, String title) {
        String value = title == null ? "" : title.trim();
        if (value.isEmpty()) value = "PersonaChat";
        if (value.length() > 40) value = value.substring(0, 40);
        prefs(context).edit().putString(HOME_TITLE, value).apply();
    }

    public static int getHomeTitleSize(Context context) {
        return Math.max(18, Math.min(38, prefs(context).getInt(HOME_TITLE_SIZE, 29)));
    }

    public static void setHomeTitleSize(Context context, int sizeSp) {
        prefs(context).edit().putInt(HOME_TITLE_SIZE, Math.max(18, Math.min(38, sizeSp))).apply();
    }
}
