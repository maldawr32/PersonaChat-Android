package com.maldawr.chatsimulator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ChatActivity extends Activity {
    private static volatile long visibleBotId = -1L;
    public static boolean isConversationVisible(long botId) { return visibleBotId == botId; }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long botId;
    private Store.Bot bot;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private FrameLayout sendWrap;
    private IconView sendIcon;
    private TextView status;
    private LinearLayout replyPreview;
    private TextView replyPreviewText;
    private long selectedReplyId;
    private int lastMessageCount = -1;
    private boolean sending;
    private ChatSoundPlayer sounds;

    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            if (isFinishing() || botId == -1L) return;
            List<Store.Message> current = Store.loadMessages(ChatActivity.this, botId);
            int count = current.size();
            if (count != lastMessageCount) {
                if (lastMessageCount >= 0 && count > lastMessageCount) {
                    for (int i = Math.max(0, lastMessageCount); i < current.size(); i++) {
                        Store.Message m = current.get(i);
                        if (m.incoming && "text".equals(m.kind)) { sounds.playIncoming(); break; }
                    }
                }
                bot = Store.getBot(ChatActivity.this, botId);
                renderMessages();
                Store.markRead(ChatActivity.this, botId);
            }
            handler.postDelayed(this, 220L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        } else getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(0xFF0B141A);
        getWindow().setNavigationBarColor(0xFF0B141A);
        sounds = new ChatSoundPlayer(this);
        Store.ensureSeeded(this);
        NotificationHelper.ensureChannels(this);
        botId = getIntent().getLongExtra("bot_id", -1L);
        bot = Store.getBot(this, botId);
        if (bot == null) { finish(); return; }
        Store.markRead(this, botId);
        buildUi();
        renderMessages();
    }

    @Override protected void onResume() { super.onResume(); visibleBotId = botId; Store.markRead(this, botId); handler.removeCallbacks(refreshTask); handler.post(refreshTask); }
    @Override protected void onPause() { if (visibleBotId == botId) visibleBotId = -1L; handler.removeCallbacks(refreshTask); super.onPause(); }
    @Override protected void onDestroy() { handler.removeCallbacks(refreshTask); if (sounds != null) sounds.release(); super.onDestroy(); }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0B141A);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(0xFF0B141A);
        int side = Ui.dp(this, 4), base = Ui.dp(this, 6), bottom = Ui.dp(this, 6);
        top.setPadding(side, base, side, bottom);
        top.setOnApplyWindowInsetsListener((v, insets) -> {
            int s = Build.VERSION.SDK_INT >= 30 ? insets.getInsets(WindowInsets.Type.statusBars()).top : insets.getSystemWindowInsetTop();
            v.setPadding(side, base + s, side, bottom);
            return insets;
        });

        IconView back = new IconView(this, IconView.BACK, 44);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));

        View avatar = Ui.avatar(this, bot, 44);
        avatar.setOnClickListener(v -> showProfile());
        top.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 2), 0);
        TextView name = Ui.oneLine(this, bot.name, 18, Color.WHITE);
        name.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        name.setOnClickListener(v -> showProfile());
        meta.addView(name, new LinearLayout.LayoutParams(-1, Ui.dp(this, 25)));
        String sub = bot.groupChat && !bot.groupSubtitle.isEmpty() ? bot.groupSubtitle : replyModeLabel(bot);
        status = Ui.oneLine(this, sub, 12, 0xFFB6C2C8);
        meta.addView(status, new LinearLayout.LayoutParams(-1, Ui.dp(this, 19)));
        top.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));

        IconView video = new IconView(this, IconView.VIDEO, 42);
        video.setOnClickListener(v -> Toast.makeText(this, "Simulated video call", Toast.LENGTH_SHORT).show());
        top.addView(video, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        IconView phone = new IconView(this, IconView.PHONE, 42);
        phone.setOnClickListener(v -> startSimulatedCall());
        top.addView(phone, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
        IconView more = new IconView(this, IconView.MORE, 38);
        more.setOnClickListener(v -> showConversationMenu());
        top.addView(more, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)));
        root.addView(top);

        TextView sim = Ui.safetyBanner(this);
        sim.setText("SIM • محادثة خيالية");
        sim.setTextColor(0xFF7F9199);
        sim.setBackgroundColor(0xFF0B141A);
        root.addView(sim);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackground(new ChatPatternDrawable(this));
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(Ui.dp(this, 7), Ui.dp(this, 8), Ui.dp(this, 7), Ui.dp(this, 12));
        scroll.addView(messages, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        replyPreview = new LinearLayout(this);
        replyPreview.setGravity(Gravity.CENTER_VERTICAL);
        replyPreview.setPadding(Ui.dp(this, 14), Ui.dp(this, 7), Ui.dp(this, 7), Ui.dp(this, 7));
        replyPreview.setBackground(Ui.rounded(0xFF1B2A31, 10, this));
        replyPreviewText = Ui.oneLine(this, "", 13, 0xFFD7E2E7);
        replyPreview.addView(replyPreviewText, new LinearLayout.LayoutParams(0, Ui.dp(this, 34), 1));
        TextView closeReply = Ui.label(this, "✕", 18, true);
        closeReply.setGravity(Gravity.CENTER);
        closeReply.setTextColor(0xFFB8C4C9);
        closeReply.setOnClickListener(v -> clearReply());
        replyPreview.addView(closeReply, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        replyPreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 70), Ui.dp(this, 4));
        root.addView(replyPreview, rp);

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setBackgroundColor(0xFF0B141A);
        final int ch = Ui.dp(this, 6), cv = Ui.dp(this, 5);
        composer.setPadding(ch, cv, ch, cv);

        LinearLayout field = new LinearLayout(this);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setBackground(Ui.rounded(0xFF1F2C33, 27, this));
        field.setPadding(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);

        TextView emoji = Ui.label(this, "☺", 25, false);
        emoji.setGravity(Gravity.CENTER);
        emoji.setTextColor(0xFFB7C3C9);
        emoji.setOnClickListener(v -> showEmojiPicker());
        field.addView(emoji, new LinearLayout.LayoutParams(Ui.dp(this, 45), Ui.dp(this, 50)));

        input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(5);
        input.setMinLines(1);
        input.setHint("Message");
        input.setTextSize(17);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xFF8696A0);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(Ui.dp(this, 2), Ui.dp(this, 7), Ui.dp(this, 2), Ui.dp(this, 7));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        field.addView(input, new LinearLayout.LayoutParams(0, -2, 1));

        IconView clip = new IconView(this, IconView.PLUS, 34).tint(0xFF9AA9B0);
        clip.setOnClickListener(v -> showAttachmentMenu());
        field.addView(clip, new LinearLayout.LayoutParams(Ui.dp(this, 37), Ui.dp(this, 44)));
        IconView cam = new IconView(this, IconView.CAMERA, 34).tint(0xFF9AA9B0);
        cam.setOnClickListener(v -> addQuickAttachment("📷 Photo", "image"));
        field.addView(cam, new LinearLayout.LayoutParams(Ui.dp(this, 39), Ui.dp(this, 44)));
        composer.addView(field, new LinearLayout.LayoutParams(0, -2, 1));

        sendWrap = new FrameLayout(this);
        sendWrap.setBackground(Ui.circle(0xFF25B889));
        sendWrap.setClickable(true);
        sendWrap.setFocusable(true);
        sendIcon = new IconView(this, IconView.MIC, 30);
        sendIcon.setClickable(false);
        sendIcon.setFocusable(false);
        sendIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        sendWrap.addView(sendIcon, new FrameLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30), Gravity.CENTER));
        LinearLayout.LayoutParams sw = new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56));
        sw.setMargins(Ui.dp(this, 6), 0, 0, 0);
        composer.addView(sendWrap, sw);

        sendWrap.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(.93f).scaleY(.93f).setDuration(55).start();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(70).start();
                if (input.getText().toString().trim().isEmpty()) addQuickAttachment("🎤 Voice note • 0:06", "voice");
                else performSend();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setScaleX(1f); v.setScaleY(1f); return true;
            }
            return true;
        });

        root.addView(composer);
        if (Build.VERSION.SDK_INT >= 30) installImeSync(composer, ch, cv);

        input.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { updateSendIcon(!s.toString().trim().isEmpty()); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        input.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEND && !input.getText().toString().trim().isEmpty()) { performSend(); return true; }
            return false;
        });

        setContentView(root);
        root.requestApplyInsets();
    }

    private void updateSendIcon(boolean hasText) {
        if (sendIcon == null) return;
        sendWrap.removeView(sendIcon);
        sendIcon = new IconView(this, hasText ? IconView.SEND : IconView.MIC, 30);
        sendIcon.setClickable(false);
        sendIcon.setFocusable(false);
        sendIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        sendWrap.addView(sendIcon, new FrameLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30), Gravity.CENTER));
    }

    private void performSend() {
        if (sending) return;
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        sending = true;
        sendWrap.setEnabled(false);
        long now = System.currentTimeMillis();
        long replyId = selectedReplyId;
        Store.Message outgoing = new Store.Message(Store.nextMessageId(), botId, text, false, now, "You", "", replyId, "text");
        Store.addMessage(this, outgoing);
        input.setText("");
        clearReply();
        renderMessages();
        sounds.playOutgoing();
        input.requestFocus();

        Store.ReplyPlan plan = Store.buildReplyPlan(this, bot, text);
        if (!plan.isEmpty()) {
            status.setText(bot.groupChat ? "typing… • SIM" : "typing… • SIM");
            ReplyScheduler.schedulePlan(this, botId, plan);
            long first = plan.items.get(0).delayMs;
            handler.postDelayed(() -> {
                Store.Bot latest = Store.getBot(this, botId);
                if (latest != null) status.setText(latest.groupChat && !latest.groupSubtitle.isEmpty() ? latest.groupSubtitle : replyModeLabel(latest));
            }, Math.min(90000L, first + 1200L));
        }
        if (!plan.onlineTopic.isEmpty()) {
            long delay = plan.items.isEmpty() ? 2500L : plan.items.get(plan.items.size() - 1).delayMs + 2500L;
            OnlineContentWorker.enqueue(this, botId, plan.onlineTopic, delay);
        }
        handler.postDelayed(() -> { sending = false; sendWrap.setEnabled(true); }, 130L);
    }

    private void addQuickAttachment(String label, String kind) {
        long now = System.currentTimeMillis();
        Store.addMessage(this, new Store.Message(Store.nextMessageId(), botId, label, false, now, "You", "", selectedReplyId, kind));
        clearReply();
        renderMessages();
        sounds.playOutgoing();
    }

    private void showAttachmentMenu() {
        String[] items = {"📷 Photo", "📄 Document", "📍 Location", "👤 Contact", "🎤 Voice note"};
        new AlertDialog.Builder(this).setTitle("Simulated attachment").setItems(items, (d, which) -> {
            String[] kinds = {"image", "document", "location", "contact", "voice"};
            addQuickAttachment(items[which], kinds[which]);
        }).show();
    }

    private void showEmojiPicker() {
        String[] emoji = {"😀","😄","😂","🤣","😊","😍","🥰","😘","😎","🤔","😮","😢","😭","😡","👍","👎","👏","🙏","❤️","🔥","✨","🎉","💯","🤝","😉","😅","🙌","💚","📌","👀","🤍","💙","💔","😴","🤩","🙄"};
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < 6; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            for (int c = 0; c < 6; c++) {
                int i = r * 6 + c;
                TextView e = Ui.label(this, emoji[i], 25, false);
                e.setGravity(Gravity.CENTER);
                e.setOnClickListener(v -> {
                    int pos = input.getSelectionStart();
                    input.getText().insert(Math.max(0, pos), ((TextView) v).getText());
                });
                row.addView(e, new LinearLayout.LayoutParams(0, Ui.dp(this, 47), 1f));
            }
            wrap.addView(row);
        }
        new AlertDialog.Builder(this).setTitle("Emoji").setView(wrap).setNegativeButton("Done", null).show();
    }

    private void selectReply(Store.Message m) {
        selectedReplyId = m.id;
        String who = m.incoming ? (m.sender == null || m.sender.isEmpty() ? bot.name : m.sender) : "You";
        replyPreviewText.setText("Reply to " + who + "  •  " + trim(m.text, 58));
        replyPreview.setVisibility(View.VISIBLE);
        input.requestFocus();
    }

    private void clearReply() {
        selectedReplyId = 0L;
        if (replyPreview != null) replyPreview.setVisibility(View.GONE);
    }

    private void showReactionPicker(Store.Message m) {
        String[] reactions = {"❤️", "😂", "👍", "😮", "😢", "🙏", "🔥", "↩ Reply"};
        new AlertDialog.Builder(this).setTitle("Message action").setItems(reactions, (d, which) -> {
            if (which == reactions.length - 1) { selectReply(m); return; }
            Store.applyReaction(this, botId, m.id, reactions[which], "You");
            renderMessages();
        }).show();
    }

    private void installSwipeReply(View view, Store.Message message) {
        final float[] downX = new float[1];
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { downX[0] = event.getX(); return false; }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float delta = event.getX() - downX[0];
                if (Math.abs(delta) > Ui.dp(this, 68)) { selectReply(message); v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); return true; }
            }
            return false;
        });
    }

    private void installImeSync(LinearLayout composer, int h, int v) {
        composer.setOnApplyWindowInsetsListener((view, insets) -> { applyComposerInsets(composer, insets, h, v); return insets; });
        composer.setWindowInsetsAnimationCallback(new WindowInsetsAnimation.Callback(WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            @Override public WindowInsets onProgress(WindowInsets insets, List<WindowInsetsAnimation> running) { applyComposerInsets(composer, insets, h, v); return insets; }
        });
    }

    private void applyComposerInsets(LinearLayout composer, WindowInsets insets, int h, int v) {
        if (Build.VERSION.SDK_INT < 30) return;
        int ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
        int nav = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
        composer.setPadding(h, v, h, v + Math.max(ime, nav));
        if (ime > 0 && scroll != null) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private String replyModeLabel(Store.Bot b) {
        String m = b.replyMode == null ? "natural" : b.replyMode;
        if ("instant".equals(m)) return "online • fast replies • SIM";
        if ("slow".equals(m)) return "away • slow replies • SIM";
        return "online • simulated contact";
    }

    private void renderMessages() {
        if (messages == null) return;
        messages.removeAllViews();
        List<Store.Message> list = Store.loadMessages(this, botId);
        lastMessageCount = list.size();
        long lastDay = -1;
        for (Store.Message m : list) {
            if ("reaction_event".equals(m.kind)) continue;
            long day = m.time / 86400000L;
            if (day != lastDay) {
                TextView d = Ui.label(this, day == (System.currentTimeMillis() / 86400000L) ? "Today" : "Earlier", 12, true);
                d.setTextColor(0xFF9AA8AE);
                d.setGravity(Gravity.CENTER);
                d.setBackground(Ui.rounded(0xFF18262D, 8, this));
                d.setPadding(Ui.dp(this, 13), Ui.dp(this, 5), Ui.dp(this, 13), Ui.dp(this, 5));
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-2, -2);
                dp.gravity = Gravity.CENTER;
                dp.setMargins(0, Ui.dp(this, 7), 0, Ui.dp(this, 8));
                messages.addView(d, dp);
                lastDay = day;
            }
            addBubble(m);
        }
        messages.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addBubble(Store.Message m) {
        FrameLayout holder = new FrameLayout(this);
        holder.setPadding(Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 4), Ui.dp(this, 2));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(Ui.dp(this, 10), Ui.dp(this, 7), Ui.dp(this, 9), Ui.dp(this, 5));
        bubble.setBackground(Ui.rounded(m.incoming ? 0xFF202C33 : 0xFF075E54, 10, this));
        bubble.setClickable(true);
        bubble.setFocusable(true);

        if (bot.groupChat && m.incoming) {
            String sender = m.sender == null || m.sender.isEmpty() ? bot.name : m.sender;
            TextView s = Ui.label(this, sender, 12, true);
            s.setTextColor(memberColor(sender));
            bubble.addView(s);
        }

        if (m.replyToId > 0) {
            Store.Message quoted = Store.getMessage(this, m.replyToId);
            if (quoted != null && !quoted.text.isEmpty()) {
                LinearLayout quote = new LinearLayout(this);
                quote.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
                quote.setBackground(Ui.rounded(m.incoming ? 0xFF17242B : 0xFF064C45, 7, this));
                TextView q = Ui.label(this, "↪ " + trim(quoted.text, 62), 12, false);
                q.setTextColor(0xFFB4C3C9);
                quote.addView(q);
                LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-1, -2);
                qp.setMargins(0, 0, 0, Ui.dp(this, 5));
                bubble.addView(quote, qp);
            }
        }

        if (!"text".equals(m.kind)) {
            TextView badge = Ui.label(this, attachmentLabel(m), 15, true);
            badge.setTextColor(0xFFE9EDEF);
            badge.setPadding(0, Ui.dp(this, 1), 0, Ui.dp(this, 2));
            bubble.addView(badge);
        } else {
            TextView text = Ui.label(this, m.text, 17, false);
            text.setTextColor(0xFFF0F3F4);
            text.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * .76f));
            text.setLineSpacing(0f, 1.04f);
            bubble.addView(text);
        }

        LinearLayout meta = new LinearLayout(this);
        meta.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        if (m.reaction != null && !m.reaction.isEmpty()) {
            TextView react = Ui.label(this, m.reaction + "  ", 15, false);
            react.setTextColor(Color.WHITE);
            meta.addView(react);
        }
        TextView time = Ui.label(this, Store.formatTime(m.time), 11, false);
        time.setTextColor(0xFFAAB5BA);
        meta.addView(time);
        if (!m.incoming) {
            TextView ticks = Ui.label(this, "  ✓✓", 12, true);
            ticks.setTextColor(0xFF79C6E6);
            meta.addView(ticks);
        }
        bubble.addView(meta);

        bubble.setOnLongClickListener(v -> { showReactionPicker(m); return true; });
        installSwipeReply(bubble, m);

        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, -2, m.incoming ? Gravity.START : Gravity.END);
        bp.leftMargin = Ui.dp(this, m.incoming ? 4 : 54);
        bp.rightMargin = Ui.dp(this, m.incoming ? 54 : 4);
        holder.addView(bubble, bp);
        messages.addView(holder, new LinearLayout.LayoutParams(-1, -2));
    }

    private String attachmentLabel(Store.Message m) {
        if ("image".equals(m.kind)) return "📷  Photo";
        if ("document".equals(m.kind)) return "📄  Document";
        if ("location".equals(m.kind)) return "📍  Shared location";
        if ("contact".equals(m.kind)) return "👤  Contact card";
        if ("voice".equals(m.kind)) return "🎤  Voice note   ▶  0:06";
        return m.text;
    }

    private int memberColor(String sender) {
        for (Store.GroupMember m : Store.loadGroupMembers(this, botId)) if (m.name.equals(sender)) return m.color;
        return 0xFFFFB36B;
    }

    private String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private void showProfile() {
        String line2 = bot.groupChat ? bot.groupSubtitle : bot.phone;
        new AlertDialog.Builder(this)
                .setTitle(bot.name + "  •  SIM")
                .setMessage(line2 + "\n\n" + bot.status + "\n\nThis profile is fictional and exists only inside PersonaChat.")
                .setPositiveButton("Edit", (d, w) -> {
                    Intent in = new Intent(this, BotEditorActivity.class);
                    in.putExtra("bot_id", botId);
                    startActivity(in);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void startSimulatedCall() {
        long now = System.currentTimeMillis();
        Store.addCall(this, new Store.CallItem(now, botId, "outgoing", now, 0));
        new AlertDialog.Builder(this)
                .setTitle("Simulated call")
                .setMessage("Calling " + bot.name + " inside PersonaChat only.")
                .setPositiveButton("End", null)
                .show();
    }

    private void showConversationMenu() {
        String[] items = {bot.favorite ? "Remove favorite" : "Add favorite", "Mark as read", "Schedule simulated message", "Conversation info", "Edit conversation"};
        new AlertDialog.Builder(this).setTitle(bot.name).setItems(items, (d, w) -> {
            if (w == 0) { bot.favorite = !bot.favorite; Store.saveBot(this, bot); }
            else if (w == 1) Store.markRead(this, botId);
            else if (w == 2) chooseMessageDelay();
            else if (w == 3) showProfile();
            else {
                Intent in = new Intent(this, BotEditorActivity.class);
                in.putExtra("bot_id", botId);
                startActivity(in);
            }
        }).show();
    }

    private void chooseMessageDelay() {
        String[] labels = {"After 10 seconds", "After 1 minute", "After 5 minutes", "After 15 minutes"};
        long[] delays = {10000, 60000, 300000, 900000};
        new AlertDialog.Builder(this).setTitle("Schedule incoming message").setItems(labels, (x, w) -> {
            ReplyScheduler.scheduleOne(this, botId, Store.smartReply("scheduled"), System.currentTimeMillis() + delays[w], 700 + w);
            Toast.makeText(this, "Message scheduled", Toast.LENGTH_SHORT).show();
        }).show();
    }
}
