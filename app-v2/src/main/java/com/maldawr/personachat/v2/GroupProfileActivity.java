package com.maldawr.personachat.v2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.maldawr.personachat.v2.data.AppDatabase;
import com.maldawr.personachat.v2.data.GroupEntity;
import com.maldawr.personachat.v2.data.PersonaEntity;

import java.util.List;

public class GroupProfileActivity extends Activity {
    private LinearLayout body;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.topBar(this, "Group profile", "Members and autonomous behavior"));
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 28));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        long id = getIntent().getLongExtra("group_id", -1);
        new Thread(() -> load(id)).start();
    }

    private void load(long id) {
        AppDatabase.AppDao dao = AppDatabase.get(this).dao();
        GroupEntity group = null;
        for (GroupEntity g : dao.groups()) if (g.id == id) group = g;
        GroupEntity finalGroup = group;
        List<PersonaEntity> personas = group == null ? java.util.Collections.emptyList() : dao.personasForGroup(group.id);
        runOnUiThread(() -> render(finalGroup, personas));
    }

    private void render(GroupEntity group, List<PersonaEntity> personas) {
        body.removeAllViews();
        if (group == null) {
            body.addView(Ui.text(this, "Group not found", 18, Ui.TEXT, true));
            return;
        }
        body.addView(Ui.text(this, group.name, 28, Ui.TEXT, true));
        body.addView(Ui.text(this, group.description, 14, Ui.SUB, false));
        body.addView(Ui.text(this, "Autonomy " + group.autonomyLevel + "%", 13, Ui.ACCENT, true));
        body.addView(Ui.text(this, "Members", 18, Ui.TEXT, true));
        for (PersonaEntity p : personas) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            row.setBackground(Ui.round(Ui.CARD, 14, this));
            row.addView(Ui.text(this, p.displayName, 16, p.accentColor, true));
            row.addView(Ui.text(this, p.relationship + " • " + p.style, 12, Ui.SUB, false));
            row.setOnClickListener(v -> startActivity(new Intent(this, PersonaProfileActivity.class).putExtra("persona_id", p.id)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, Ui.dp(this, 8), 0, 0);
            body.addView(row, lp);
        }
        android.widget.Button openChat = Ui.button(this, "Open group chat");
        openChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class).putExtra("group_id", group.id)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 52));
        lp.setMargins(0, Ui.dp(this, 18), 0, 0);
        body.addView(openChat, lp);
    }
}
