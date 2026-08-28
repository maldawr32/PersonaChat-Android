package com.maldawr.personachat.v2;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
        buildShell();
        new Thread(this::loadDemoGroup).start();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 20, 26));

        TextView banner = label("PERSONACHAT V2 • FICTIONAL SIMULATION", 12, 0xFF8DA0A8, true);
        banner.setGravity(Gravity.CENTER);
        banner.setPadding(12, 18, 12, 12);
        root.addView(banner);

        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(24, 20, 24, 40);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void loadDemoGroup() {
        DemoSeeder.ensureSeeded(this);
        AppDatabase.AppDao dao = AppDatabase.get(this).dao();
        List<GroupEntity> groups = dao.groups();
        if (groups.isEmpty()) return;
        GroupEntity group = groups.get(0);
        List<PersonaEntity> personas = dao.personasForGroup(group.id);
        List<MessageEntity> messages = dao.messagesForGroup(group.id);
        Map<Long, PersonaEntity> byId = new HashMap<>();
        for (PersonaEntity p : personas) byId.put(p.id, p);
        runOnUiThread(() -> render(group, personas, messages, byId));
    }

    private void render(GroupEntity group, List<PersonaEntity> personas, List<MessageEntity> messages, Map<Long, PersonaEntity> byId) {
        body.removeAllViews();

        TextView title = label(group.name, 27, Color.WHITE, true);
        body.addView(title);

        StringBuilder names = new StringBuilder("You");
        for (PersonaEntity p : personas) names.append(", ").append(p.displayName);
        TextView members = label(names.toString(), 15, 0xFF9AA9B0, false);
        members.setPadding(0, 4, 0, 8);
        body.addView(members);

        TextView autonomy = label("Autonomous group conversation: ON • " + group.autonomyLevel + "%", 12, 0xFF53CFA0, true);
        autonomy.setPadding(0, 0, 0, 20);
        body.addView(autonomy);

        for (MessageEntity m : messages) {
            boolean mine = m.senderPersonaId == 0;
            PersonaEntity p = mine ? null : byId.get(m.senderPersonaId);
            LinearLayout bubble = new LinearLayout(this);
            bubble.setOrientation(LinearLayout.VERTICAL);
            bubble.setPadding(16, 10, 16, 10);
            bubble.setBackgroundColor(mine ? 0xFF075E54 : 0xFF202C33);

            if (!mine && p != null) {
                TextView sender = label(p.displayName, 14, p.accentColor, true);
                bubble.addView(sender);
            }
            TextView text = label(m.text, 17, Color.WHITE, false);
            text.setPadding(0, mine ? 0 : 4, 0, 0);
            bubble.addView(text);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.gravity = mine ? Gravity.END : Gravity.START;
            lp.setMargins(mine ? 90 : 0, 0, mine ? 0 : 90, 12);
            body.addView(bubble, lp);
        }

        TextView footer = label("Next: real avatars, editable personas, DeepSeek multi-agent turns, media and voice.", 13, 0xFF7F9199, false);
        footer.setPadding(0, 16, 0, 0);
        body.addView(footer);
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return view;
    }
}
