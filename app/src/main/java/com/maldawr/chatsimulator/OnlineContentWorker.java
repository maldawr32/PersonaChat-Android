package com.maldawr.chatsimulator;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class OnlineContentWorker extends Worker {
    public OnlineContentWorker(@NonNull Context appContext, @NonNull WorkerParameters params) { super(appContext, params); }

    public static void enqueue(Context context, long botId, String topic, long delayMs) {
        if (!Store.isOnlineContentEnabled(context)) return;
        Data data = new Data.Builder().putLong("bot_id", botId).putString("topic", topic).build();
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(Store.isWifiOnly(context) ? NetworkType.UNMETERED : NetworkType.CONNECTED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OnlineContentWorker.class).setInputData(data).setConstraints(constraints).setInitialDelay(Math.max(0L, delayMs), TimeUnit.MILLISECONDS).addTag("online-content-v7").build();
        WorkManager.getInstance(context.getApplicationContext()).enqueue(request);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        long botId = getInputData().getLong("bot_id", -1L);
        String topic = getInputData().getString("topic");
        if (botId == -1L || topic == null || topic.isEmpty()) return Result.failure();
        Store.Bot bot = Store.getBot(context, botId);
        if (bot == null || !Store.isOnlineContentEnabled(context)) return Result.success();
        String sender = chooseSender(context, bot);
        List<String> chunks;
        try {
            if ("weather".equals(topic)) chunks = weather();
            else if ("currency".equals(topic)) chunks = currency();
            else chunks = market();
        } catch (Exception e) {
            chunks = new ArrayList<>();
            chunks.add("ما قدرت أوصل للمصدر هلق 😅");
            chunks.add("منجرب بعدين، ولا تعتمد على رقم قديم قبل ما تتأكد.");
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < chunks.size(); i++) ReplyScheduler.scheduleOne(context, botId, chunks.get(i), sender, "", 0L, "text", now + 800L + i * 3500L, 8000 + i);
        return Result.success();
    }

    private static List<String> weather() throws Exception { JSONObject c=new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=33.5138&longitude=36.2765&current=temperature_2m,weather_code,wind_speed_10m&timezone=auto")).getJSONObject("current");List<String> r=new ArrayList<>();r.add(String.format(Locale.getDefault(),"آخر تحديث لدمشق: %.1f° مئوية، %s.",c.getDouble("temperature_2m"),weatherDescription(c.optInt("weather_code",-1))));r.add(String.format(Locale.getDefault(),"الرياح تقريباً %.1f كم/س.",c.optDouble("wind_speed_10m",0)));r.add("المصدر: Open-Meteo • "+timestamp());return r;}
    private static List<String> currency() throws Exception {JSONObject rates=new JSONObject(get("https://api.frankfurter.app/latest?from=USD&to=EUR,TRY")).getJSONObject("rates");List<String>r=new ArrayList<>();r.add(String.format(Locale.getDefault(),"1 دولار ≈ %.4f يورو.",rates.getDouble("EUR")));r.add(String.format(Locale.getDefault(),"1 دولار ≈ %.2f ليرة تركية.",rates.getDouble("TRY")));r.add("المصدر: Frankfurter / ECB reference • "+timestamp());return r;}
    private static List<String> market() throws Exception {JSONObject root=new JSONObject(get("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd&include_24hr_change=true"));JSONObject b=root.getJSONObject("bitcoin"),e=root.getJSONObject("ethereum");List<String>r=new ArrayList<>();r.add(String.format(Locale.getDefault(),"بيتكوين: $%,.0f (%+.2f%% خلال 24 ساعة).",b.getDouble("usd"),b.optDouble("usd_24h_change",0)));r.add(String.format(Locale.getDefault(),"إيثريوم: $%,.0f (%+.2f%% خلال 24 ساعة).",e.getDouble("usd"),e.optDouble("usd_24h_change",0)));r.add("المصدر: CoinGecko • بيانات تعليمية وليست توصية مالية • "+timestamp());return r;}
    private static String get(String endpoint) throws Exception {HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","ChatSimulator-V7/0.4");int status=c.getResponseCode();InputStream s=status>=200&&status<300?c.getInputStream():c.getErrorStream();if(s==null)throw new IllegalStateException("No body");StringBuilder b=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(s,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)b.append(line);}finally{c.disconnect();}if(status<200||status>=300)throw new IllegalStateException("HTTP "+status);return b.toString();}
    private static String chooseSender(Context c,Store.Bot b){if(!b.groupChat)return b.name;List<Store.GroupMember>m=Store.loadGroupMembers(c,b.id);return m.isEmpty()?b.name:m.get((int)(System.currentTimeMillis()%m.size())).name;}private static String timestamp(){return new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date());}private static String weatherDescription(int c){if(c==0)return"الجو صافي";if(c<=3)return"غائم جزئياً";if(c==45||c==48)return"في ضباب";if(c>=51&&c<=67)return"في هطول";if(c>=71&&c<=77)return"في ثلوج";if(c>=80&&c<=82)return"زخات مطر";if(c>=95)return"عواصف رعدية محتملة";return"حالة جوية متغيرة";}
}
