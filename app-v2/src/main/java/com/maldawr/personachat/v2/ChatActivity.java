package com.maldawr.personachat.v2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.maldawr.personachat.v2.data.AppDatabase;
import com.maldawr.personachat.v2.data.GroupEntity;
import com.maldawr.personachat.v2.data.MessageEntity;
import com.maldawr.personachat.v2.data.PersonaEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends Activity {
    private long groupId;
    private GroupEntity group;
    private LinearLayout messagesBox;
    private ScrollView scroll;
    private EditText composer;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        groupId = getIntent().getLongExtra("group_id", -1);
        buildUi();
        reload();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout top = Ui.topBar(this, "Conversation", "Loading…");
        top.setOnClickListener(v -> {
            if (group != null) startActivity(new Intent(this, GroupProfileActivity.class).putExtra("group_id", group.id));
        });
        root.addView(top);

        scroll = new ScrollView(this);
        messagesBox = new LinearLayout(this);
        messagesBox.setOrientation(LinearLayout.VERTICAL);
        messagesBox.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14));
        scroll.addView(messagesBox);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout composerRow = new LinearLayout(this);
        composerRow.setGravity(Gravity.CENTER_VERTICAL);
        composerRow.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        composerRow.setBackgroundColor(Ui.PANEL);

        Button attach = Ui.button(this, "+");
        attach.setEnabled(false);
        attach.setContentDescription("Media attachments will be enabled in the media milestone");
        composerRow.addView(attach, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        composer = new EditText(this);
        composer.setHint("Message");
        composer.setHintTextColor(Ui.SUB);
        composer.setTextColor(Ui.TEXT);
        composer.setTextSize(16);
        composer.setSingleLine(false);
        composer.setMaxLines(5);
        composer.setImeOptions(EditorInfo.IME_ACTION_SEND);
        composer.setBackground(Ui.round(Ui.CARD, 22, this));
        composer.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 8));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, -2, 1f);
        inputLp.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        composerRow.addView(composer, inputLp);

        Button send = Ui.button(this, "Send");
        send.setOnClickListener(v -> sendMessage());
        composer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        composerRow.addView(send, new LinearLayout.LayoutParams(Ui.dp(this, 74), Ui.dp(this, 48)));
        root.addView(composerRow);

        setContentView(root);
    }

    private void reload() {
        new Thread(() -> {
            AppDatabase.AppDao dao = AppDatabase.get(this).dao();
            GroupEntity found = null;
            for (GroupEntity g : dao.groups()) if (g.id == groupId) found = g;
            if (found == null) return;
            List<PersonaEntity> personas = dao.personasForGroup(groupId);
            List<MessageEntity> messages = dao.messagesForGroup(groupId);
            Map<Long, PersonaEntity> byId = new HashMap<>();
            for (PersonaEntity p : personas) byId.put(p.id, p);
            GroupEntity finalFound = found;
            runOnUiThread(() -> render(finalFound, personas, messages, byId));
        }).start();
    }

    private void render(GroupEntity found, List<PersonaEntity> personas, List<MessageEntity> messages, Map<Long, PersonaEntity> byId) {
        group = found;
        messagesBox.removeAllViews();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 14));
        header.addView(Ui.text(this, found.name, 20, Ui.TEXT, true));
        StringBuilder memberText = new StringBuilder("You");
        for (PersonaEntity p : personas) memberText.append(", ").append(p.displayName);
        header.addView(Ui.text(this, memberText.toString(), 12, Ui.SUB, false));
        header.addView(Ui.text(this, "Simulation • AI group", 11, 0xFF6F858E, false));
        messagesBox.addView(header);

        for (MessageEntity m : messages) addBubble(m, byId.get(m.senderPersonaId));
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void addBubble(MessageEntity m, PersonaEntity persona) {
        boolean mine = m.senderPersonaId == 0;
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        bubble.setBackground(Ui.round(mine ? Ui.OUT : Ui.CARD, 14, this));

        if (!mine && persona != null) bubble.addView(Ui.text(this, persona.displayName, 13, persona.accentColor, true));
        bubble.addView(Ui.text(this, m.text, 16, Color.WHITE, false));
        bubble.addView(Ui.text(this, mine ? "You" : (persona == null ? "AI member" : persona.nicknameForUser), 10, 0xFF93A4AB, false));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.gravity = mine ? Gravity.END : Gravity.START;
        lp.setMargins(mine ? Ui.dp(this, 70) : 0, 0, mine ? 0 : Ui.dp(this, 70), Ui.dp(this, 8));
        messagesBox.addView(bubble, lp);
    }

    private void sendMessage() {
        String text = composer.getText().toString().trim();
        if (text.isEmpty() || groupId < 0) return;
        composer.setText("");
        new Thread(() -> {
            long now = System.currentTimeMillis();
            AppDatabase.get(this).dao().putMessage(new MessageEntity(now * 1000L + (now % 997), groupId, 0, text, now));
            reload();
        }).start();
    }
}
