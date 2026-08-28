package com.maldawr.personachat.v2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class SettingsActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.topBar(this, "Settings", "PersonaChat behavior and customization"));
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 24));

        String[][] rows = {
                {"DeepSeek AI", "API key, model, memory and response behavior"},
                {"Notifications", "Name, sound, vibration, heads-up and icon behavior"},
                {"Appearance & icons", "Theme, launcher shortcuts and notification identity"},
                {"Background activity", "Scheduled messages and autonomous persona activity"},
                {"Voice & media", "Voice-note recording, playback and image behavior"},
                {"Privacy", "Local storage and simulation disclosures"}
        };
        for (String[] row : rows) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
            card.setBackground(Ui.round(Ui.CARD, 16, this));
            card.addView(Ui.text(this, row[0], 17, Ui.TEXT, true));
            card.addView(Ui.text(this, row[1], 12, Ui.SUB, false));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, Ui.dp(this, 10));
            body.addView(card, lp);
        }

        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(Ui.bottomNav(this, "Settings"));
        setContentView(root);
    }
}
