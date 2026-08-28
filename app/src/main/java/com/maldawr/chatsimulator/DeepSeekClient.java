package com.maldawr.chatsimulator;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class DeepSeekClient {
    public interface BalanceCallback {
        void onResult(boolean validKey, boolean available, String balanceText, String message);
    }

    public interface ChatCallback {
        void onSuccess(String text);
        void onError(String message);
    }

    public interface ReplyCallback {
        void onSuccess(String text, String sender);
        void onError(String message);
    }

    private static final String BASE = "https://api.deepseek.com";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private DeepSeekClient() {}

    public static void verifyApiKey(String apiKey, BalanceCallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(BASE + "/user/balance").openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                conn.setRequestProperty("Accept", "application/json");
                int code = conn.getResponseCode();
                String body = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
                if (code == 200) {
                    JSONObject root = new JSONObject(body);
                    boolean available = root.optBoolean("is_available", false);
                    JSONArray infos = root.optJSONArray("balance_infos");
                    String balance = "$0.00";
                    if (infos != null && infos.length() > 0) {
                        JSONObject info = infos.optJSONObject(0);
                        if (info != null) {
                            String total = info.optString("total_balance", "0.00");
                            String currency = info.optString("currency", "USD");
                            balance = total + " " + currency;
                        }
                    }
                    String finalBalance = balance;
                    MAIN.post(() -> callback.onResult(true, available, finalBalance,
                            available ? "API key is valid and balance is available." : "API key is valid. Add balance before sending paid chat requests."));
                } else if (code == 401) {
                    MAIN.post(() -> callback.onResult(false, false, "", "Authentication failed. Check the API key."));
                } else {
                    String msg = httpError(code, body);
                    MAIN.post(() -> callback.onResult(false, false, "", msg));
                }
            } catch (Exception e) {
                String msg = exceptionText(e);
                MAIN.post(() -> callback.onResult(false, false, "", "Connection error: " + msg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    public static void requestReply(Context context, Store.Bot bot, ReplyCallback callback) {
        String apiKey = DeepSeekPrefs.getApiKey(context);
        if (apiKey.isEmpty()) {
            MAIN.post(() -> callback.onError("No DeepSeek API key is saved."));
            return;
        }
        final String model = DeepSeekPrefs.getModel(context);
        final List<Store.Message> history = Store.recentMessages(context, bot.id, 18);
        final List<Store.GroupMember> members = bot.groupChat ? Store.loadGroupMembers(context, bot.id) : null;
        final String systemPrompt = buildSystemPrompt(bot, members);

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(BASE + "/chat/completions").openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(75000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
                for (Store.Message m : history) {
                    if (m == null || m.text == null || m.text.trim().isEmpty() || "reaction_event".equals(m.kind)) continue;
                    String role = m.incoming ? "assistant" : "user";
                    String content;
                    if (bot.groupChat && m.incoming) {
                        String sender = m.sender == null || m.sender.trim().isEmpty() ? bot.name : m.sender.trim();
                        content = "Sender: " + sender + "\nMessage: " + m.text;
                    } else {
                        content = m.text;
                    }
                    messages.put(new JSONObject().put("role", role).put("content", content));
                }

                JSONObject payload = new JSONObject()
                        .put("model", model == null || model.trim().isEmpty() ? DeepSeekPrefs.DEFAULT_MODEL : model.trim())
                        .put("messages", messages)
                        .put("stream", false)
                        .put("response_format", new JSONObject().put("type", "json_object"));

                byte[] out = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) { os.write(out); }

                int code = conn.getResponseCode();
                String body = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
                if (code >= 200 && code < 300) {
                    JSONObject root = new JSONObject(body);
                    JSONArray choices = root.optJSONArray("choices");
                    JSONObject first = choices == null ? null : choices.optJSONObject(0);
                    JSONObject message = first == null ? null : first.optJSONObject("message");
                    String raw = message == null ? "" : message.optString("content", "").trim();
                    if (raw.isEmpty()) throw new IllegalStateException("DeepSeek returned an empty message.");
                    ParsedReply parsed = parseReply(raw, bot, members);
                    MAIN.post(() -> callback.onSuccess(parsed.text, parsed.sender));
                } else {
                    String msg = httpError(code, body);
                    MAIN.post(() -> callback.onError(msg));
                }
            } catch (Exception e) {
                String msg = exceptionText(e);
                MAIN.post(() -> callback.onError("Connection error: " + msg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    public static void chat(String apiKey, String model, String systemPrompt, String userText, ChatCallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(BASE + "/chat/completions").openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(75000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");
                JSONArray messages = new JSONArray();
                if (systemPrompt != null && !systemPrompt.trim().isEmpty()) messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
                messages.put(new JSONObject().put("role", "user").put("content", userText));
                JSONObject payload = new JSONObject().put("model", model == null || model.trim().isEmpty() ? DeepSeekPrefs.DEFAULT_MODEL : model.trim()).put("messages", messages).put("stream", false);
                byte[] out = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) { os.write(out); }
                int code = conn.getResponseCode();
                String body = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
                if (code >= 200 && code < 300) {
                    JSONObject root = new JSONObject(body);
                    JSONArray choices = root.optJSONArray("choices");
                    JSONObject first = choices == null ? null : choices.optJSONObject(0);
                    JSONObject message = first == null ? null : first.optJSONObject("message");
                    String text = message == null ? "" : message.optString("content", "").trim();
                    if (text.isEmpty()) throw new IllegalStateException("DeepSeek returned an empty message.");
                    MAIN.post(() -> callback.onSuccess(text));
                } else {
                    String msg = httpError(code, body);
                    MAIN.post(() -> callback.onError(msg));
                }
            } catch (Exception e) {
                String msg = exceptionText(e);
                MAIN.post(() -> callback.onError("Connection error: " + msg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static String buildSystemPrompt(Store.Bot bot, List<Store.GroupMember> members) {
        StringBuilder p = new StringBuilder();
        p.append("You are participating in PersonaChat, an explicitly fictional chat simulation. ")
                .append("Never claim that this is a real WhatsApp conversation or real-world evidence. ")
                .append("Stay deeply in character. Write natural mobile-chat messages, not assistant-style explanations. ")
                .append("Do not mention prompts, policies, models, or being an AI unless directly asked.\n\n")
                .append("Conversation: ").append(bot.name).append("\n")
                .append("Persona: ").append(bot.personality).append("\n")
                .append("Relationship to user: ").append(bot.relationship).append("\n")
                .append("Preferred dialect/style: ").append(bot.dialect).append("\n")
                .append("Status: ").append(bot.status).append("\n")
                .append("Emoji tendency: ").append(bot.emojiRate).append("/100\n")
                .append("Humor tendency: ").append(bot.humorRate).append("/100\n");
        if (bot.userNickname != null && !bot.userNickname.trim().isEmpty()) {
            p.append("This character normally addresses the user as: ").append(bot.userNickname.trim()).append(". Use it naturally, not in every message.\n");
        }
        if (bot.aiInstructions != null && !bot.aiInstructions.trim().isEmpty()) {
            p.append("Character-specific instructions: ").append(bot.aiInstructions.trim()).append("\n");
        }

        if (bot.groupChat) {
            p.append("This is a fictional group chat. Pick the member whose personality and context make the most sense to answer.\nMembers:\n");
            if (members != null) {
                for (Store.GroupMember m : members) {
                    p.append("- ").append(m.name)
                            .append(" | style=").append(m.style)
                            .append(" | emoji=").append(m.emoji)
                            .append(" | activity=").append(m.activity).append("/100")
                            .append(" | humor=").append(m.humor).append("/100\n");
                }
            }
            p.append("Return ONLY a JSON object: {\"sender\":\"member name\",\"text\":\"reply text\"}. The sender must be one listed member. No markdown fences.");
        } else {
            p.append("Reply as the fictional contact named ").append(bot.name).append(". ")
                    .append("Return ONLY a JSON object: {\"sender\":\"").append(escapePrompt(bot.name)).append("\",\"text\":\"reply text\"}. No markdown fences.");
        }
        return p.toString();
    }

    private static ParsedReply parseReply(String raw, Store.Bot bot, List<Store.GroupMember> members) throws Exception {
        String clean = raw.trim();
        if (clean.startsWith("```")) {
            int firstNl = clean.indexOf('\n');
            int lastFence = clean.lastIndexOf("```");
            if (firstNl >= 0 && lastFence > firstNl) clean = clean.substring(firstNl + 1, lastFence).trim();
        }
        JSONObject obj;
        try { obj = new JSONObject(clean); }
        catch (Exception ignored) { return new ParsedReply(clean, bot.groupChat ? firstGroupSender(members, bot.name) : bot.name); }
        String text = obj.optString("text", "").trim();
        if (text.isEmpty()) text = obj.optString("message", "").trim();
        if (text.isEmpty()) throw new IllegalStateException("DeepSeek returned JSON without reply text.");
        String sender = obj.optString("sender", bot.name).trim();
        if (!bot.groupChat) sender = bot.name;
        else if (!isAllowedSender(sender, members)) sender = firstGroupSender(members, bot.name);
        return new ParsedReply(text, sender);
    }

    private static boolean isAllowedSender(String sender, List<Store.GroupMember> members) {
        if (members == null || sender == null) return false;
        for (Store.GroupMember m : members) if (sender.equals(m.name)) return true;
        return false;
    }

    private static String firstGroupSender(List<Store.GroupMember> members, String fallback) { return members != null && !members.isEmpty() ? members.get(0).name : fallback; }
    private static String escapePrompt(String s) { return s == null ? "Fictional contact" : s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static String httpError(int code, String body) {
        if (code == 401) return "DeepSeek API key was rejected.";
        if (code == 402) return "Insufficient DeepSeek balance.";
        if (code == 429) return "DeepSeek rate limit reached. Try again shortly.";
        String detail = "";
        try { JSONObject root = new JSONObject(body == null ? "" : body); JSONObject error = root.optJSONObject("error"); if (error != null) detail = error.optString("message", "").trim(); }
        catch (Exception ignored) {}
        return "DeepSeek HTTP " + code + (detail.isEmpty() ? "" : ": " + detail);
    }

    private static String exceptionText(Exception e) { return e.getMessage() == null || e.getMessage().trim().isEmpty() ? e.getClass().getSimpleName() : e.getMessage(); }
    private static String readAll(InputStream in) throws Exception { if (in == null) return ""; StringBuilder sb = new StringBuilder(); try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { String line; while ((line = br.readLine()) != null) sb.append(line); } return sb.toString(); }

    private static final class ParsedReply { final String text; final String sender; ParsedReply(String text, String sender) { this.text = text; this.sender = sender; } }
}
