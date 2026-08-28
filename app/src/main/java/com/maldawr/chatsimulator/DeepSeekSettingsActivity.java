package com.maldawr.chatsimulator;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DeepSeekSettingsActivity extends Activity {
    private static final int BG = Color.rgb(11,20,26);
    private static final int PANEL = Color.rgb(31,44,51);
    private static final int GREEN = Color.rgb(37,211,102);
    private static final int MUTED = Color.rgb(134,150,160);

    private EditText keyInput;
    private TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(BG);
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this,20), Ui.dp(this,24), Ui.dp(this,20), Ui.dp(this,32));
        scroll.addView(root, new ScrollView.LayoutParams(-1,-2));

        TextView title = Ui.label(this, "DeepSeek Settings", 27, true);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        TextView note = Ui.label(this,
                "Your API key is stored encrypted on this device with Android Keystore. It is not uploaded to GitHub.",
                14, false);
        note.setTextColor(MUTED);
        note.setPadding(0, Ui.dp(this,8), 0, Ui.dp(this,18));
        root.addView(note);

        keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setHint(DeepSeekPrefs.hasApiKey(this) ? "Saved API key ••••••••" : "Paste DeepSeek API key");
        keyInput.setTextColor(Color.WHITE);
        keyInput.setHintTextColor(MUTED);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setBackground(Ui.rounded(PANEL, 18, this));
        keyInput.setPadding(Ui.dp(this,14),0,Ui.dp(this,14),0);
        root.addView(keyInput, new LinearLayout.LayoutParams(-1, Ui.dp(this,58)));

        TextView modelLabel = Ui.label(this, "Model", 14, true);
        modelLabel.setTextColor(GREEN);
        modelLabel.setPadding(0, Ui.dp(this,18),0,Ui.dp(this,6));
        root.addView(modelLabel);

        EditText modelInput = new EditText(this);
        modelInput.setSingleLine(true);
        modelInput.setText(DeepSeekPrefs.getModel(this));
        modelInput.setTextColor(Color.WHITE);
        modelInput.setBackground(Ui.rounded(PANEL, 18, this));
        modelInput.setPadding(Ui.dp(this,14),0,Ui.dp(this,14),0);
        root.addView(modelInput, new LinearLayout.LayoutParams(-1, Ui.dp(this,54)));

        Button save = new Button(this);
        save.setText("Save settings");
        save.setAllCaps(false);
        save.setOnClickListener(v -> {
            String entered = keyInput.getText().toString().trim();
            if (!entered.isEmpty() && !DeepSeekPrefs.saveApiKey(this, entered)) {
                Toast.makeText(this, "Could not encrypt API key on this device.", Toast.LENGTH_LONG).show();
                return;
            }
            DeepSeekPrefs.setModel(this, modelInput.getText().toString());
            keyInput.setText("");
            keyInput.setHint(DeepSeekPrefs.hasApiKey(this) ? "Saved API key ••••••••" : "Paste DeepSeek API key");
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, Ui.dp(this,52));
        saveLp.setMargins(0, Ui.dp(this,18),0,0);
        root.addView(save, saveLp);

        Button verify = new Button(this);
        verify.setText("Verify API key / balance");
        verify.setAllCaps(false);
        verify.setOnClickListener(v -> verifyNow());
        LinearLayout.LayoutParams verifyLp = new LinearLayout.LayoutParams(-1, Ui.dp(this,52));
        verifyLp.setMargins(0, Ui.dp(this,10),0,0);
        root.addView(verify, verifyLp);

        Button clear = new Button(this);
        clear.setText("Clear saved API key");
        clear.setAllCaps(false);
        clear.setOnClickListener(v -> {
            DeepSeekPrefs.clearApiKey(this);
            keyInput.setText("");
            keyInput.setHint("Paste DeepSeek API key");
            status.setText("No API key saved.");
        });
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(-1, Ui.dp(this,48));
        clearLp.setMargins(0, Ui.dp(this,10),0,0);
        root.addView(clear, clearLp);

        status = Ui.label(this,
                DeepSeekPrefs.hasApiKey(this) ? "API key saved. Tap Verify to check it." : "No API key saved.",
                14, false);
        status.setTextColor(MUTED);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        status.setPadding(0, Ui.dp(this,18),0,0);
        root.addView(status, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(scroll);
    }

    private void verifyNow() {
        String entered = keyInput.getText().toString().trim();
        if (!entered.isEmpty()) {
            if (!DeepSeekPrefs.saveApiKey(this, entered)) {
                status.setText("Could not encrypt API key on this device.");
                return;
            }
            keyInput.setText("");
            keyInput.setHint("Saved API key ••••••••");
        }
        String key = DeepSeekPrefs.getApiKey(this);
        if (key.isEmpty()) {
            status.setText("Enter and save your DeepSeek API key first.");
            return;
        }
        status.setText("Checking DeepSeek...");
        DeepSeekClient.verifyApiKey(key, (valid, available, balance, message) -> {
            if (valid) {
                status.setText("✓ API key valid\nBalance: " + balance + "\n" + message);
                status.setTextColor(GREEN);
            } else {
                status.setText("✕ " + message);
                status.setTextColor(Color.rgb(244,67,98));
            }
        });
    }
}
