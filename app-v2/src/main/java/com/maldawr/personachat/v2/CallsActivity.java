package com.maldawr.personachat.v2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.maldawr.personachat.v2.data.AppDatabase;
import com.maldawr.personachat.v2.data.CallSessionEntity;

import java.util.List;

public class CallsActivity extends Activity {
    private LinearLayout body;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.topBar(this, "Calls", "Voice and video call history"));
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 20));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(Ui.bottomNav(this, "Calls"));
        setContentView(root);
        new Thread(this::load).start();
    }

    private void load() {
        List<CallSessionEntity> ringing = AppDatabase.get(this).dao().ringingCalls();
        runOnUiThread(() -> {
            body.removeAllViews();
            if (ringing.isEmpty()) {
                body.addView(Ui.text(this, "No active call right now.", 16, Ui.SUB, false));
                body.addView(Ui.text(this, "Call history and stable answer/end controls will live here.", 13, 0xFF6F858E, false));
                return;
            }
            for (CallSessionEntity c : ringing) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
                row.setBackground(Ui.round(Ui.CARD, 16, this));
                row.addView(Ui.text(this, "Incoming simulated call", 17, Ui.TEXT, true));
                row.addView(Ui.text(this, "Call ID " + c.id + " • " + c.state, 12, Ui.SUB, false));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 0, 0, Ui.dp(this, 10));
                body.addView(row, lp);
            }
        });
    }
}
