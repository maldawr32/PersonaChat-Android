package com.maldawr.personachat.v2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.maldawr.personachat.v2.data.AppDatabase;
import com.maldawr.personachat.v2.data.PersonaEntity;

public class PersonaProfileActivity extends Activity {
    private LinearLayout body;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.topBar(this, "Persona profile", "Independent AI identity"));
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 28));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        long id = getIntent().getLongExtra("persona_id", -1);
        new Thread(() -> load(id)).start();
    }

    private void load(long id) {
        PersonaEntity p = AppDatabase.get(this).dao().persona(id);
        runOnUiThread(() -> {
            body.removeAllViews();
            if (p == null) {
                body.addView(Ui.text(this, "Persona not found", 18, Ui.TEXT, true));
                return;
            }
            body.addView(Ui.text(this, p.displayName, 28, Ui.TEXT, true));
            body.addView(Ui.text(this, p.relationship, 15, Ui.SUB, false));
            add("Calls you", p.nicknameForUser);
            add("Dialect", p.dialect);
            add("Style", p.style);
            add("Humor", p.humor + "%");
            add("Warmth", p.warmth + "%");
            add("Initiative", p.initiative + "%");
            add("AI behavior", p.instructions);
        });
    }

    private void add(String title, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        card.setBackground(Ui.round(Ui.CARD, 14, this));
        card.addView(Ui.text(this, title, 12, Ui.ACCENT, true));
        card.addView(Ui.text(this, value, 15, Ui.TEXT, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, Ui.dp(this, 8), 0, 0);
        body.addView(card, lp);
    }
}
