package com.maldawr.personachat.v2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BG = 0xFF0B141A;
    public static final int PANEL = 0xFF111B21;
    public static final int CARD = 0xFF202C33;
    public static final int TEXT = Color.WHITE;
    public static final int SUB = 0xFF8696A0;
    public static final int ACCENT = 0xFF25B889;
    public static final int OUT = 0xFF005C4B;

    private Ui() {}

    public static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context c, String value, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value == null ? "" : value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    public static GradientDrawable round(int color, int radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static TextView toolbarTitle(Context c, String title, String subtitle) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(c, 18), dp(c, 10), dp(c, 18), dp(c, 10));
        TextView t = text(c, title, 23, TEXT, true);
        box.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = text(c, subtitle, 12, SUB, false);
            box.addView(s);
        }
        TextView holder = new TextView(c);
        holder.setTag(box);
        return holder;
    }

    public static LinearLayout topBar(Context c, String title, String subtitle) {
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(PANEL);
        LinearLayout labels = new LinearLayout(c);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(c, 18), dp(c, 10), dp(c, 18), dp(c, 10));
        labels.addView(text(c, title, 23, TEXT, true));
        if (subtitle != null && !subtitle.isEmpty()) labels.addView(text(c, subtitle, 12, SUB, false));
        bar.addView(labels, new LinearLayout.LayoutParams(0, dp(c, 68), 1f));
        return bar;
    }

    public static LinearLayout bottomNav(Activity a, String selected) {
        LinearLayout nav = new LinearLayout(a);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(PANEL);
        String[] labels = {"Chats", "Personas", "Groups", "Calls", "Settings"};
        Class<?>[] targets = {MainActivity.class, PersonasActivity.class, GroupsActivity.class, CallsActivity.class, SettingsActivity.class};
        for (int i = 0; i < labels.length; i++) {
            final Class<?> target = targets[i];
            TextView v = text(a, labels[i], 12, labels[i].equals(selected) ? ACCENT : SUB, labels[i].equals(selected));
            v.setGravity(Gravity.CENTER);
            v.setPadding(dp(a, 2), dp(a, 12), dp(a, 2), dp(a, 12));
            v.setOnClickListener(view -> {
                if (a.getClass() == target) return;
                Intent intent = new Intent(a, target);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                a.startActivity(intent);
            });
            nav.addView(v, new LinearLayout.LayoutParams(0, dp(a, 58), 1f));
        }
        return nav;
    }

    public static Button button(Context c, String text) {
        Button b = new Button(c);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setTextSize(15);
        b.setBackground(round(CARD, 14, c));
        return b;
    }

    public static View divider(Context c) {
        View d = new View(c);
        d.setBackgroundColor(0xFF26343B);
        return d;
    }
}
