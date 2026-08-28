package com.maldawr.personachat.v2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.maldawr.personachat.v2.data.AppDatabase;
import com.maldawr.personachat.v2.data.PersonaEntity;

import java.util.ArrayList;
import java.util.List;

public class PersonasActivity extends Activity {
    private LinearLayout body;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.topBar(this, "Personas", "AI contacts and behavior profiles"));
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 20));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(Ui.bottomNav(this, "Personas"));
        setContentView(root);
        new Thread(this::load).start();
    }

    private void load() {
        AppDatabase.AppDao dao = AppDatabase.get(this).dao();
        List<PersonaEntity> all = new ArrayList<>();
        for (com.maldawr.personachat.v2.data.GroupEntity g : dao.groups()) {
            for (PersonaEntity p : dao.personasForGroup(g.id)) if (!contains(all, p.id)) all.add(p);
        }
        runOnUiThread(() -> render(all));
    }

    private boolean contains(List<PersonaEntity> list, long id) {
        for (PersonaEntity p : list) if (p.id == id) return true;
        return false;
    }

    private void render(List<PersonaEntity> personas) {
        body.removeAllViews();
        for (PersonaEntity p : personas) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
            row.setBackground(Ui.round(Ui.CARD, 16, this));
            row.addView(Ui.text(this, p.displayName, 18, Ui.TEXT, true));
            row.addView(Ui.text(this, p.relationship + " • calls you " + p.nicknameForUser, 13, Ui.SUB, false));
            row.addView(Ui.text(this, p.dialect + " • " + p.style, 12, 0xFF7F9199, false));
            row.setOnClickListener(v -> startActivity(new Intent(this, PersonaProfileActivity.class).putExtra("persona_id", p.id)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, Ui.dp(this, 10));
            body.addView(row, lp);
        }
    }
}
