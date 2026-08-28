package com.maldawr.chatsimulator;

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

public final class DeepSeekClient {
    public interface BalanceCallback {
        void onResult(boolean validKey, boolean available, String balanceText, String message);
    }

    public interface ChatCallback {
        void onSuccess(String text);
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
                    String msg = "DeepSeek returned HTTP " + code + (body.isEmpty() ? "" : ": " + body);
                    MAIN.post(() -> callback.onResult(false, false, "", msg));
                }
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                MAIN.post(() -> callback.onResult(false, false, "", "Connection error: " + msg));
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
                conn.setReadTimeout(60000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");

                JSONArray messages = new JSONArray();
                if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                    messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
                }
                messages.put(new JSONObject().put("role", "user").put("content", userText));
                JSONObject payload = new JSONObject()
                        .put("model", model == null || model.trim().isEmpty() ? DeepSeekPrefs.DEFAULT_MODEL : model.trim())
                        .put("messages", messages)
                        .put("stream", false);

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
                    String msg = code == 402 ? "Insufficient DeepSeek balance." : "DeepSeek HTTP " + code + (body.isEmpty() ? "" : ": " + body);
                    MAIN.post(() -> callback.onError(msg));
                }
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                MAIN.post(() -> callback.onError("Connection error: " + msg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
