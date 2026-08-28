package com.maldawr.chatsimulator;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Generates a short autonomous multi-member group conversation with DeepSeek. */
public final class GroupAiRoundClient {
    private static final String ENDPOINT = "https://api.deepseek.com/chat/completions";
    private static final Random RANDOM = new Random();
    private GroupAiRoundClient() {}

    public static Store.ReplyPlan requestBlocking(Context context, Store.Bot bot, boolean testMode) throws Exception {
        if (bot == null || !bot.groupChat) throw new IllegalArgumentException("Group required");
        String apiKey = DeepSeekPrefs.getApiKey(context);
        if (apiKey.isEmpty()) throw new IllegalStateException("DeepSeek API key is not configured");

        List<Store.GroupMember> members = Store.loadGroupMembers(context, bot.id);
        if (members.size() < 2) throw new IllegalStateException("Group needs at least two members");
        List<Store.Message> history = Store.recentMessages(context, bot.id, 32);
        int activity = GroupPrefs.getActivity(context, bot.id);
        int desiredCount = desiredMessageCount(activity, members.size(), testMode);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", buildSystemPrompt(bot, members, activity, desiredCount, testMode)));
        for (Store.Message m : history) {
            if (m == null || m.text == null || m.text.trim().isEmpty() || "reaction_event".equals(m.kind)) continue;
            String who = m.incoming ? (m.sender == null || m.sender.trim().isEmpty() ? "Group member" : m.sender.trim()) : "Taj";
            messages.put(new JSONObject().put("role", "user").put("content", who + ": " + m.text.trim()));
        }
        String instruction = testMode
                ? "Start a natural test conversation now. Pick up a plausible topic from the recent chat and let members respond to one another."
                : "Continue the group naturally without pretending Taj just sent a new message. Use recent context and let members respond to each other when it makes sense.";
        messages.put(new JSONObject().put("role", "user").put("content", instruction));

        JSONObject payload = new JSONObject()
                .put("model", DeepSeekPrefs.getModel(context))
                .put("messages", messages)
                .put("stream", false)
                .put("response_format", new JSONObject().put("type", "json_object"));

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            conn.setConnectTimeout(20_000);
            conn.setReadTimeout(80_000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            byte[] out = payload.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) { os.write(out); }
            int code = conn.getResponseCode();
            String body = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (code < 200 || code >= 300) throw new IllegalStateException("DeepSeek HTTP " + code + errorDetail(body));
            JSONObject root = new JSONObject(body);
            JSONArray choices = root.optJSONArray("choices");
            JSONObject first = choices == null ? null : choices.optJSONObject(0);
            JSONObject msg = first == null ? null : first.optJSONObject("message");
            String raw = msg == null ? "" : msg.optString("content", "").trim();
            if (raw.isEmpty()) throw new IllegalStateException("DeepSeek returned an empty group round");
            return parseRound(raw, members, activity, desiredCount);
        } finally { if (conn != null) conn.disconnect(); }
    }

    private static String buildSystemPrompt(Store.Bot bot, List<Store.GroupMember> members, int activity, int desiredCount, boolean testMode) {
        StringBuilder p = new StringBuilder();
        p.append("You write a fictional PersonaChat group simulation. Never present it as a real WhatsApp conversation or real-world evidence.\n")
                .append("Group: ").append(bot.name).append("\n")
                .append("Group personality/context: ").append(bot.personality).append("\n")
                .append("Group activity: ").append(activity).append("/100\n")
                .append("Generate exactly ").append(desiredCount).append(" short mobile-chat messages as one coherent mini-conversation.\n")
                .append("Members must react to previous content, can agree, disagree, tease, ask follow-ups, correct each other, or add useful information.\n")
                .append("Keep facts consistent with the recent conversation. Do not invent major personal facts unless implied by context.\n")
                .append("Do not make every member speak. Avoid assistant-like phrases, summaries, narration, and repeated ideas.\nMembers:\n");
        for (Store.GroupMember m : members) {
            p.append("- ").append(m.name).append(" | style=").append(m.style)
                    .append(" | signatureEmoji=").append(m.emoji)
                    .append(" | activity=").append(m.activity).append("/100")
                    .append(" | humor=").append(m.humor).append("/100\n");
        }
        p.append("Output ONLY JSON: {\"messages\":[{\"sender\":\"exact member name\",\"text\":\"message\"}]}. ")
                .append("Use only listed sender names. Taj is the user and must not be generated as a sender. Prefer speaker changes unless a natural double-message is useful.");
        if (testMode) p.append(" This is a user-triggered simulation test, so always produce the requested round.");
        return p.toString();
    }

    private static Store.ReplyPlan parseRound(String raw, List<Store.GroupMember> members, int activity, int desiredCount) throws Exception {
        JSONObject obj = new JSONObject(stripFence(raw));
        JSONArray array = obj.optJSONArray("messages");
        if (array == null || array.length() == 0) throw new IllegalStateException("DeepSeek returned no group messages");
        Set<String> allowed = new HashSet<>();
        for (Store.GroupMember m : members) allowed.add(m.name);
        Store.ReplyPlan plan = new Store.ReplyPlan();
        long delay = firstDelay(activity);
        String previousSender = "";
        int max = Math.min(5, desiredCount);
        for (int i = 0; i < array.length() && plan.items.size() < max; i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            String sender = item.optString("sender", "").trim();
            String text = item.optString("text", "").trim();
            if (!allowed.contains(sender) || text.isEmpty()) continue;
            if (sender.equals(previousSender) && plan.items.size() > 0 && i + 1 < array.length()) {
                JSONObject next = array.optJSONObject(i + 1);
                String nextSender = next == null ? "" : next.optString("sender", "").trim();
                if (allowed.contains(nextSender) && !nextSender.equals(sender)) continue;
            }
            plan.add(text, sender, "", 0L, "text", delay);
            previousSender = sender;
            delay += gapDelay(activity);
        }
        if (plan.items.size() < 2) throw new IllegalStateException("DeepSeek group round was too short after validation");
        return plan;
    }

    private static int desiredMessageCount(int activity, int memberCount, boolean testMode) {
        if (testMode) return Math.min(5, Math.max(3, memberCount));
        if (activity >= 85) return Math.min(5, 3 + RANDOM.nextInt(3));
        if (activity >= 60) return 2 + RANDOM.nextInt(3);
        if (activity >= 35) return 2 + RANDOM.nextInt(2);
        return 2;
    }
    private static long firstDelay(int activity) { return activity >= 80 ? between(900,3500) : activity >= 50 ? between(1500,6000) : between(2500,9000); }
    private static long gapDelay(int activity) { return activity >= 85 ? between(2000,7000) : activity >= 60 ? between(3500,11000) : activity >= 35 ? between(6000,18000) : between(10000,30000); }
    private static long between(long min,long max){return min+(long)(RANDOM.nextDouble()*Math.max(1L,max-min+1L));}
    private static String stripFence(String value){String s=value==null?"":value.trim();if(!s.startsWith("```"))return s;int nl=s.indexOf('\n'),end=s.lastIndexOf("```");return nl>=0&&end>nl?s.substring(nl+1,end).trim():s;}
    private static String errorDetail(String body){try{JSONObject root=new JSONObject(body==null?"":body);JSONObject e=root.optJSONObject("error");String t=e==null?"":e.optString("message","").trim();return t.isEmpty()?"":": "+t;}catch(Exception ignored){return"";}}
    private static String readAll(InputStream in)throws Exception{if(in==null)return"";StringBuilder sb=new StringBuilder();try(BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=br.readLine())!=null)sb.append(line);}return sb.toString();}
}
