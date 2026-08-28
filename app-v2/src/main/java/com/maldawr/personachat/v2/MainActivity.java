package com.maldawr.personachat.v2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.maldawr.personachat.v2.data.AppDatabase;
import com.maldawr.personachat.v2.data.DemoSeeder;
import com.maldawr.personachat.v2.data.GroupEntity;
import com.maldawr.personachat.v2.data.MessageEntity;
import com.maldawr.personachat.v2.data.PersonaEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private LinearLayout body;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.topBar(this, "PersonaChat", "Chats • fictional AI simulation"));

        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 18));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(Ui.bottomNav(this, "Chats"));
        setContentView(root);
        load();
    }

    @Override protected void onResume() {
        super.onResume();
        if (body != null) load();
    }

    private void load() {
        new Thread(() -> {
            DemoSeeder.ensureSeeded(this);
            AppDatabase.AppDao dao = AppDatabase.get(this).dao();
            List<GroupEntity> groups = dao.groups();
            Map<Long, List<PersonaEntity>> members = new HashMap<>();
            Map<Long, List<MessageEntity>> messages = new HashMap<>();
            for (GroupEntity g : groups) {
                members.put(g.id, dao.personasForGroup(g.id));
                messages.put(g.id, dao.messagesForGroup(g.id));
            }
            runOnUiThread(() -> render(groups, members, messages));
        }).start();
    }

    private void render(List<GroupEntity> groups,
                        Map<Long, List<PersonaEntity>> members,
                        Map<Long, List<MessageEntity>> messages) {
        body.removeAllViews();

        LinearLayout quick = new LinearLayout(this);
        quick.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.Button newPersona = Ui.button(this, "New persona");
        newPersona.setOnClickListener(v -> startActivity(new Intent(this, PersonasActivity.class)));
        quick.addView(newPersona, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f));
        android.widget.Button newGroup = Ui.button(this, "Groups");
        newGroup.setOnClickListener(v -> startActivity(new Intent(this, GroupsActivity.class)));
        LinearLayout.LayoutParams ng = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f);
        ng.setMargins(Ui.dp(this, 8), 0, 0, 0);
        quick.addView(newGroup, ng);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(-1, -2);
        qlp.setMargins(0, 0, 0, Ui.dp(this, 12));
        body.addView(quick, qlp);

        if (groups.isEmpty()) {
            body.addView(Ui.text(this, "No conversations yet.", 17, Ui.SUB, false));
            return;
        }

        for (GroupEntity g : groups) {
            List<PersonaEntity> ps = members.get(g.id);
            List<MessageEntity> ms = messages.get(g.id);
            MessageEntity last = ms == null || ms.isEmpty() ? null : ms.get(ms.size() - 1);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Ui.dp(this, 16), Ui.dp(this, 13), Ui.dp(this, 16), Ui.dp(this, 13));
            row.setBackground(Ui.round(Ui.CARD, 16, this));
            row.addView(Ui.text(this, g.name, 18, Ui.TEXT, true));

            StringBuilder subtitle = new StringBuilder("You");
            if (ps != null) for (PersonaEntity p : ps) subtitle.append(", ").append(p.displayName);
            row.addView(Ui.text(this, subtitle.toString(), 12, Ui.SUB, false));
            row.addView(Ui.text(this, last == null ? "Start a conversation" : last.text, 14, 0xFFB7C3C9, false));
            row.addView(Ui.text(this, g.allowAutonomousConversation ? "AI group • autonomous" : "AI group", 11, Ui.ACCENT, false));

            row.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class).putExtra("group_id", g.id)));
            row.setOnLongClickListener(v -> {
                startActivity(new Intent(this, GroupProfileActivity.class).putExtra("group_id", g.id));
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, Ui.dp(this, 9));
            body.addView(row, lp);
        }
    }
}
