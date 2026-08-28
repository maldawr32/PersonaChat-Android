package com.maldawr.personachat.v2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.maldawr.personachat.v2.data.AppDatabase;
import com.maldawr.personachat.v2.data.GroupEntity;

import java.util.List;

public class GroupsActivity extends Activity {
    private LinearLayout body;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.topBar(this, "Groups", "Autonomous AI group conversations"));
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 20));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(Ui.bottomNav(this, "Groups"));
        setContentView(root);
        new Thread(this::load).start();
    }

    private void load() {
        AppDatabase.AppDao dao = AppDatabase.get(this).dao();
        List<GroupEntity> groups = dao.groups();
        runOnUiThread(() -> {
            body.removeAllViews();
            for (GroupEntity g : groups) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
                row.setBackground(Ui.round(Ui.CARD, 16, this));
                row.addView(Ui.text(this, g.name, 18, Ui.TEXT, true));
                row.addView(Ui.text(this, g.description, 13, Ui.SUB, false));
                row.addView(Ui.text(this, "Autonomy " + g.autonomyLevel + "% • " + (g.allowAutonomousConversation ? "AI members can talk together" : "manual turns"), 12, Ui.ACCENT, false));
                row.setOnClickListener(v -> startActivity(new Intent(this, GroupProfileActivity.class).putExtra("group_id", g.id)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 0, 0, Ui.dp(this, 10));
                body.addView(row, lp);
            }
        });
    }
}
