package com.maldawr.chatsimulator;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class ConversationEngine {
    private static final Random RANDOM = new Random();
    private ConversationEngine() {}

    public static Store.ReplyPlan planForUserMessage(Context context, Store.Bot bot, String input) {
        Store.ReplyPlan plan = new Store.ReplyPlan();
        if (bot == null || !bot.autoReply || !Store.isBotActive(bot)) return plan;
        String normalized = normalize(input);
        List<Store.Message> recent = Store.recentMessages(context, bot.id, 24);
        long replyToId = lastOutgoingId(recent);
        int consecutiveOutgoing = consecutiveOutgoing(recent);
        String topic = detectTopic(normalized, bot.lastTopic);
        bot.lastTopic = topic;
        Store.saveBot(context, bot);
        int chance = bot.replyChance;
        if ("instant".equals(bot.replyMode)) chance = Math.max(chance, 97);
        if ("slow".equals(bot.replyMode)) chance = Math.min(chance, 82);
        chance += Math.min(8, consecutiveOutgoing * 2);
        String onlineTopic = detectOnlineTopic(normalized);
        if (!onlineTopic.isEmpty() && bot.onlineContent && Store.isOnlineContentEnabled(context)) plan.onlineTopic = onlineTopic;
        if (RANDOM.nextInt(100) >= clamp(chance, 10, 100)) return plan;
        if (bot.groupChat) buildGroupReply(context, bot, normalized, topic, onlineTopic, replyToId, consecutiveOutgoing, plan);
        else buildDirectReply(context, bot, normalized, topic, onlineTopic, replyToId, consecutiveOutgoing, plan);
        return plan;
    }

    public static Store.ReplyPlan planProactive(Context context, Store.Bot bot) {
        Store.ReplyPlan plan = new Store.ReplyPlan();
        if (bot == null || !bot.initiative || !bot.autoReply || !Store.isBotActive(bot)) return plan;
        String[] topics = {"daily", "checkin", "fun", "plans", "memory"};
        String topic = topics[RANDOM.nextInt(topics.length)];
        List<Store.Message> recent = Store.recentMessages(context, bot.id, 18);
        long lastId = recent.isEmpty() ? 0L : recent.get(recent.size() - 1).id;
        if (bot.groupChat) {
            List<Store.GroupMember> members = Store.loadGroupMembers(context, bot.id);
            if (members.isEmpty()) return plan;
            Collections.shuffle(members, RANDOM);
            int count = Math.min(members.size(), 2 + RANDOM.nextInt(Math.max(1, Math.min(3, members.size()))));
            long delay = randomBetween(1200L, 4500L);
            plan.add(proactiveStarter(topic, members.get(0)), members.get(0).name, "", lastId, "text", delay);
            for (int i = 1; i < count; i++) {
                delay += randomBetween(3500L, 11000L);
                Store.GroupMember member = members.get(i);
                plan.add(proactiveContinuation(topic, member, i), member.name, "", 0L, "text", delay);
            }
            if (RANDOM.nextInt(100) < 50) {
                Store.GroupMember reactor = members.get(RANDOM.nextInt(members.size()));
                plan.add("", reactor.name, pickReaction(), lastId, "reaction", delay + randomBetween(1500L, 5000L));
            }
        } else {
            long delay = randomBetween(1200L, 5500L);
            plan.add(proactiveDirect(topic), bot.name, "", lastId, "text", delay);
            if (RANDOM.nextInt(100) < 42) plan.add(proactiveDirectContinuation(topic), bot.name, "", 0L, "text", delay + randomBetween(3500L, 12000L));
        }
        if (Store.isOnlineContentEnabled(context) && bot.onlineContent && RANDOM.nextInt(100) < 10) {
            String[] online = {"weather", "currency", "market"};
            plan.onlineTopic = online[RANDOM.nextInt(online.length)];
        }
        return plan;
    }

    public static String detectOnlineTopic(String input) {
        String value = normalize(input);
        if (containsAny(value, "طقس", "حرارة", "جو", "weather", "مطر")) return "weather";
        if (containsAny(value, "دولار", "يورو", "ليرة تركية", "عملة", "صرف", "exchange", "currency")) return "currency";
        if (containsAny(value, "بورصة", "سوق", "بيتكوين", "ايثريوم", "إيثريوم", "crypto", "bitcoin", "stock")) return "market";
        return "";
    }

    private static void buildDirectReply(Context context, Store.Bot bot, String input, String topic, String onlineTopic, long replyToId, int consecutiveOutgoing, Store.ReplyPlan plan) {
        int emojiRate = blended(bot.emojiRate, Store.getEmojiLevel(context));
        int humorRate = blended(bot.humorRate, Store.getHumorLevel(context));
        int max = Math.max(1, Math.min(5, bot.maxBurst));
        int count = chooseBurst(max, consecutiveOutgoing, false);
        long delay = firstDelay(bot.replyMode);
        if (!onlineTopic.isEmpty()) {
            plan.add(withEmoji(onlineAcknowledgement(onlineTopic), emojiRate), bot.name, "", replyToId, "text", delay);
            if (count > 1) plan.add("ثواني وبجيبلك آخر رقم من المصدر 🌐", bot.name, "", 0L, "text", delay + gapDelay(bot.replyMode));
            return;
        }
        List<String> replies = repliesFor(topic, input, bot.personality, humorRate);
        for (int i = 0; i < count; i++) {
            String text = i < replies.size() ? replies.get(i) : contextualContinuation(topic, i);
            plan.add(withEmoji(text, emojiRate), bot.name, "", i == 0 ? replyToId : 0L, "text", delay);
            delay += gapDelay(bot.replyMode);
        }
        if (replyToId > 0L && RANDOM.nextInt(100) < 35) plan.add("", bot.name, pickReaction(), replyToId, "reaction", Math.max(700L, delay - randomBetween(1000L, 2500L)));
        if (bot.initiative && RANDOM.nextInt(100) < 20) plan.add(followUpForTopic(topic), bot.name, "", 0L, "text", delay + randomBetween(6 * 60_000L, 30 * 60_000L));
    }

    private static void buildGroupReply(Context context, Store.Bot bot, String input, String topic, String onlineTopic, long replyToId, int consecutiveOutgoing, Store.ReplyPlan plan) {
        List<Store.GroupMember> members = Store.loadGroupMembers(context, bot.id);
        if (members.isEmpty()) return;
        Collections.shuffle(members, RANDOM);
        int activity = Store.getGroupActivity(context);
        int max = Math.max(2, Math.min(5, bot.maxBurst));
        int count = chooseBurst(max, consecutiveOutgoing, true);
        if (activity < 35) count = Math.min(count, 2);
        if (activity > 75 && RANDOM.nextBoolean()) count = Math.min(max, count + 1);
        count = Math.min(count, members.size() + 1);
        long delay = firstDelay(bot.replyMode);
        Set<String> used = new HashSet<>();
        if (!onlineTopic.isEmpty()) {
            Store.GroupMember member = selectMember(context, bot, used);
            used.add(member.name);
            plan.add(withEmoji(onlineAcknowledgement(onlineTopic), Store.getEmojiLevel(context)), member.name, "", replyToId, "text", delay);
            if (count > 1) {
                Store.GroupMember second = selectMember(context, bot, used);
                plan.add("إي ابعتولنا آخر تحديث لما يطلع 😄", second.name, "", 0L, "text", delay + randomBetween(3000L, 9000L));
            }
            return;
        }
        List<String> baseReplies = repliesFor(topic, input, "group", Store.getHumorLevel(context));
        for (int i = 0; i < count; i++) {
            Store.GroupMember member = selectMember(context, bot, used);
            used.add(member.name);
            String text = i < baseReplies.size() ? adaptForMember(baseReplies.get(i), member) : groupContinuation(topic, member, i);
            text = withMemberEmoji(text, member, Store.getEmojiLevel(context));
            plan.add(text, member.name, "", i == 0 ? replyToId : 0L, "text", delay);
            delay += randomBetween(2500L, 10000L);
        }
        if (replyToId > 0L && RANDOM.nextInt(100) < 62) {
            Store.GroupMember reactor = members.get(RANDOM.nextInt(members.size()));
            plan.add("", reactor.name, pickReaction(), replyToId, "reaction", delay + randomBetween(900L, 4000L));
        }
        if (bot.initiative && RANDOM.nextInt(100) < 30) {
            Store.GroupMember later = members.get(RANDOM.nextInt(members.size()));
            plan.add(groupLaterFollowUp(topic), later.name, "", 0L, "text", delay + randomBetween(7 * 60_000L, 35 * 60_000L));
        }
    }

    private static String detectTopic(String input, String previous) {
        if (containsAny(input, "مرحبا", "هلا", "هاي", "hello", "hi", "صباح", "مساء")) return "greeting";
        if (containsAny(input, "موعد", "ساعة", "وقت", "اجتماع", "meeting", "بكرا", "اليوم")) return "plans";
        if (containsAny(input, "وين", "مكان", "وصلت", "طريق", "where")) return "location";
        if (containsAny(input, "شكرا", "شكراً", "thanks", "يسلمو")) return "thanks";
        if (containsAny(input, "زعلان", "تعبان", "مضايق", "حزين", "مبسوط", "فرحان")) return "mood";
        if (containsAny(input, "نكتة", "مزحة", "ضحك", "هههه", "lol")) return "joke";
        if (!detectOnlineTopic(input).isEmpty()) return detectOnlineTopic(input);
        if (input.endsWith("؟") || input.contains("?")) return "question";
        if (input.length() < 5 && previous != null && !previous.isEmpty()) return previous;
        return "general";
    }

    private static List<String> repliesFor(String topic, String input, String personality, int humorRate) {
        List<String> result = new ArrayList<>();
        switch (topic) {
            case "greeting": result.add("أهلا وسهلا"); result.add("كيفك اليوم؟"); result.add("شو الأخبار معك؟"); break;
            case "plans": result.add("تمام، أنا مناسبني الموضوع."); result.add("أي ساعة بتناسبك أكتر؟"); result.add("إذا تغيّر شي خبرني قبلها."); break;
            case "location": result.add("لسا بالطريق تقريباً."); result.add("بعطيك خبر أول ما أوصل."); result.add("لا تشيل هم، ما رح أتأخر كتير."); break;
            case "thanks": result.add("العفو، ولا يهمك."); result.add("نحنا لبعض."); break;
            case "mood": result.add("فهمت عليك، خذها شوي شوي."); result.add("بدك تحكي شو صار؟ أنا سامعك."); result.add("إن شاء الله بتخف عنك قريب."); break;
            case "joke": result.add(randomJoke()); result.add("بعرف إنها بايخة شوي بس ضحكتني 😂"); break;
            case "question": result.add("على الأغلب إي."); result.add("بس خليني أتأكد قبل ما أعطيك جواب نهائي."); result.add("إنت شو رأيك بالموضوع؟"); break;
            default:
                String keyword = keyword(input);
                result.add(keyword.isEmpty() ? "إي وصلت الفكرة." : "إي فهمت عليك بموضوع " + keyword + ".");
                result.add(personalityLine(personality));
                result.add(humorRate > 55 && RANDOM.nextInt(100) < humorRate ? "المهم ما نعملها مسلسل من 30 حلقة 😂" : "منكمل فيها بهدوء وبنشوف الأنسب.");
        }
        return result;
    }

    private static String personalityLine(String p) { if("direct".equals(p))return"خلينا نحسمها بطريقة واضحة.";if("calm".equals(p))return"ما في داعي للاستعجال، منرتبها خطوة خطوة.";if("funny".equals(p))return"أنا معك، بس بدون دراما زيادة 😄";if("supportive".equals(p))return"أنا موجود إذا احتجت أي شي.";if("curious".equals(p))return"بس خبرني أكتر، الموضوع لفتني.";if("work".equals(p))return"خلينا نحدد المطلوب ونقسمه لنقاط.";if("family".equals(p))return"المهم الكل يكون مرتاح ومتفق.";return"تمام، منحكي فيها وناخد القرار الأنسب."; }
    private static String contextualContinuation(String topic,int i){String[] g={"خبرني شو بصير معك.","أنا متابع معك.","وإذا في تفاصيل زيادة ابعتها.","منرجع منحكي فيها بعد شوي."};if("plans".equals(topic))return i%2==0?"أنا بسجل الموعد عندي.":"لا تنسى تبعتلي المكان كمان.";if("mood".equals(topic))return i%2==0?"خد نفس وريح حالك شوي.":"ما لازم تحمل كل شي لحالك.";return g[Math.floorMod(i+RANDOM.nextInt(g.length),g.length)];}
    private static String groupContinuation(String topic,Store.GroupMember m,int i){if("plans".equals(topic))return i%2==0?"أنا مناسبني بعد العصر.":"خلونا نثبت الساعة قبل ما ننسى.";if("joke".equals(topic)||m.humor>65)return i%2==0?"😂😂 خلص وقّفوا هون":"مين فتح باب النكت اليوم؟";if("question".equals(topic))return i%2==0?"أنا رأيي نجرب هيك أول شي.":"ممكن، بس بدنا نتأكد من التفاصيل.";return i%2==0?"أنا مع هالحكي.":"إي بس في نقطة صغيرة لازم ننتبهلها.";}
    private static String adaptForMember(String t,Store.GroupMember m){if("organized".equals(m.style))return t.replace("على الأغلب","برأيي");if("funny".equals(m.style)&&!t.contains("😂"))return t+" 😂";if("calm".equals(m.style))return t.replace("تمام","تمام، بهدوء");return t;}
    private static String proactiveStarter(String t,Store.GroupMember m){if("checkin".equals(t))return"وين العالم اليوم؟ الكل مختفي "+m.emoji;if("fun".equals(t))return"سؤال سريع: مين أكتر واحد بيتأخر بالمجموعة؟ 😂";if("plans".equals(t))return"شو رأيكم نرتب شي لهالأسبوع؟";if("memory".equals(t))return"تذكرت الموقف تبع المرة الماضية ولسا عم اضحك 😂";return"شو الأخبار يا جماعة "+m.emoji;}
    private static String proactiveContinuation(String t,Store.GroupMember m,int i){if("fun".equals(t))return i%2==0?"أنا عندي اسم بس ما رح احكي 😅":"واضحة وما بدها تصويت أصلاً 😂";if("plans".equals(t))return i%2==0?"أنا موجود، بس خبروني من بكير.":"خلونا نعمل تصويت عالوقت.";if("checkin".equals(t))return i%2==0?"موجودين بس عم نراقب بصمت 👀":"أنا هون، كنت مشغول شوي.";return i%2==0?"إي والله نفس الشي خطرلي.":"هاي بدها قعدة مو رسالتين.";}
    private static String proactiveDirect(String t){if("checkin".equals(t))return"كيفك؟ صارلي فترة ما سألت عنك.";if("fun".equals(t))return"تذكرت شغلة وضحكت لحالي 😂";if("plans".equals(t))return"شو برنامجك اليوم؟";if("memory".equals(t))return"على فكرة تذكرت الحكي اللي كنا عم نحكيه.";return"شو الأخبار عندك؟ 😊";}
    private static String proactiveDirectContinuation(String t){if("plans".equals(t))return"إذا فاضي بعدين خبرني.";if("fun".equals(t))return"بس ما رح احكيلك إلا إذا رديت 😄";return"ما في شي مستعجل، بس حبيت اطمن.";}
    private static String followUpForTopic(String t){if("plans".equals(t))return"لسا الموعد مناسب إلك؟";if("mood".equals(t))return"كيف صرت هلق، أحسن شوي؟";if("location".equals(t))return"وصلت ولا لسا؟";return"على فكرة، تذكرت شغلة صغيرة بخصوص حكيّنا.";}
    private static String groupLaterFollowUp(String t){if("plans".equals(t))return"يا جماعة ثبتنا الوقت بالنهاية ولا لسا؟";if("joke".equals(t))return"لسا عم اضحك عالحكي اللي صار 😂";return"رجعت قريت الحكي… في نقطة نسيناها.";}
    private static String onlineAcknowledgement(String t){if("weather".equals(t))return"دقيقة عم شوف آخر تحديث للطقس.";if("currency".equals(t))return"لحظة، عم أجيب آخر أسعار العملات من المصدر.";return"ثواني عم أراجع آخر بيانات السوق المتاحة.";}
    private static String withEmoji(String t,int rate){if(t==null||t.isEmpty()||RANDOM.nextInt(100)>=clamp(rate,0,100)||containsEmoji(t))return t;String[] e={" 😊"," 👍"," 😄"," 🙏"," ✨"," 🙂"," 😅"};return t+e[RANDOM.nextInt(e.length)];}
    private static String withMemberEmoji(String t,Store.GroupMember m,int rate){if(t==null||t.isEmpty()||containsEmoji(t))return t;return RANDOM.nextInt(100)<clamp(rate,0,100)?t+" "+m.emoji:t;}
    private static boolean containsEmoji(String v){return v.contains("😂")||v.contains("😊")||v.contains("👍")||v.contains("🙏")||v.contains("😄")||v.contains("✨")||v.contains("❤️")||v.contains("🤔")||v.contains("😅");}
    private static Store.GroupMember selectMember(Context c,Store.Bot b,Set<String> excluded){List<Store.GroupMember> m=Store.loadGroupMembers(c,b.id);List<Store.GroupMember> candidates=new ArrayList<>();for(Store.GroupMember x:m)if(!excluded.contains(x.name))candidates.add(x);if(candidates.isEmpty())candidates.addAll(m);if(candidates.isEmpty())return new Store.GroupMember(b.id*100,b.id,b.name,"friendly","🙂",50,30,0xFF25D366);int total=0;for(Store.GroupMember x:candidates)total+=Math.max(5,x.activity);int pick=RANDOM.nextInt(Math.max(1,total));for(Store.GroupMember x:candidates){pick-=Math.max(5,x.activity);if(pick<0)return x;}return candidates.get(0);}
    private static int chooseBurst(int max,int outgoing,boolean group){int r=RANDOM.nextInt(100);int count=group?(r<18?1:r<52?2:r<82?3:r<95?4:5):(r<42?1:r<76?2:r<93?3:r<99?4:5);if(outgoing>=3&&RANDOM.nextBoolean())count++;return Math.max(1,Math.min(max,count));}
    private static int consecutiveOutgoing(List<Store.Message> m){int c=0;for(int i=m.size()-1;i>=0;i--){Store.Message x=m.get(i);if("reaction_event".equals(x.kind))continue;if(x.incoming)break;c++;}return c;}
    private static long lastOutgoingId(List<Store.Message> m){for(int i=m.size()-1;i>=0;i--){Store.Message x=m.get(i);if(!x.incoming&&"text".equals(x.kind))return x.id;}return 0L;}
    private static String keyword(String input){String v=normalize(input).replace("؟","").replace("?","");String[] stop={"انا","إنا","انت","إنت","هو","هي","شو","ليش","كيف","تمام","اوكي","أوكي","يعني","هذا","هاي","هالشي"};for(String w:v.split("\\s+")){if(w.length()<4)continue;boolean blocked=false;for(String s:stop)if(w.equals(s))blocked=true;if(!blocked)return w;}return"";}
    private static String randomJoke(){String[] j={"واحد راح عالدكتور قاله كل ما بشرب قهوة بوجعني عيني… قاله شيل الملعقة من الكاسة 😂","مرة واحد بخيل فتح محل عصير، كتب عالباب: التذوق بالنظر فقط 😄","واحد سأل صاحبه ليش الساعة زعلانة؟ قاله لأنها عم تعدّ أيامها 😂"};return j[RANDOM.nextInt(j.length)];}
    private static String pickReaction(){String[] v={"❤️","😂","👍","😮","🙏","🔥"};return v[RANDOM.nextInt(v.length)];}
    private static int blended(int a,int b){return clamp((a+b)/2,0,100);} private static long firstDelay(String m){if("instant".equals(m))return randomBetween(550,1800);if("slow".equals(m))return randomBetween(20000,85000);return randomBetween(1800,9000);} private static long gapDelay(String m){if("instant".equals(m))return randomBetween(800,2600);if("slow".equals(m))return randomBetween(8000,26000);return randomBetween(1800,7000);} private static long randomBetween(long min,long max){if(max<=min)return min;long v=RANDOM.nextLong()&Long.MAX_VALUE;return min+v%(max-min);} private static String normalize(String v){return v==null?"":v.toLowerCase(Locale.ROOT).trim();} private static boolean containsAny(String v,String... n){for(String x:n)if(v.contains(x))return true;return false;} private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
}
