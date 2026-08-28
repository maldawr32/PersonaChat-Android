package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

/** Adds richer fictional history once, without replacing user-created data. */
public final class RichDemoSeeder {
    private static final String PREFS = "personachat_rich_demo";
    private RichDemoSeeder() {}

    public static void ensure(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p.getBoolean("seeded_v1", false)) return;
        long now = System.currentTimeMillis();
        for (Store.Bot bot : Store.loadBots(context)) {
            List<Store.Message> existing = Store.loadMessages(context, bot.id);
            if (existing.size() >= 6) continue;
            if (bot.groupChat) {
                List<Store.GroupMember> members = Store.loadGroupMembers(context, bot.id);
                if (members.size() >= 2) {
                    Store.GroupMember a = members.get(0), b = members.get(1);
                    Store.addMessage(context,new Store.Message(Store.nextMessageId(),bot.id,"شو رأيكم نثبت الخطة قبل ما نبلش؟",true,now-5_400_000L,a.name,"",0L,"text"));
                    Store.addMessage(context,new Store.Message(Store.nextMessageId(),bot.id,"أنا مع، بس خلونا نخلي في مجال لأي تعديل صغير.",true,now-5_250_000L,b.name,"",0L,"text"));
                    Store.addMessage(context,new Store.Message(Store.nextMessageId(),bot.id,"تمام، خلونا نمشي فيها خطوة خطوة.",false,now-5_100_000L,"You","",0L,"text"));
                }
            } else {
                Store.addMessage(context,new Store.Message(Store.nextMessageId(),bot.id,"وينك مختفي اليوم؟ 😄",true,now-4_800_000L,bot.name,"",0L,"text"));
                Store.addMessage(context,new Store.Message(Store.nextMessageId(),bot.id,"كنت مشغول شوي، هلأ فضيت.",false,now-4_650_000L,"You","",0L,"text"));
                Store.addMessage(context,new Store.Message(Store.nextMessageId(),bot.id,"ولا يهمك، خبرني لما تكون رايق نحكي.",true,now-4_500_000L,bot.name,"",0L,"text"));
            }
        }
        p.edit().putBoolean("seeded_v1", true).apply();
    }
}
