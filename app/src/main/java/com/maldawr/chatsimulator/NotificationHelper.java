package com.maldawr.chatsimulator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import java.util.List;

public final class NotificationHelper {
    private static final String MSG_PREFIX="sim_messages_custom_",CALL_PREFIX="sim_calls_custom_";
    private NotificationHelper(){}
    private static int messageNotificationId(long botId){return (int)(10000+(botId%100000));}

    public static String currentMessageChannelId(Context c){String sig=CustomizationPrefs.getNotificationMode(c)+"|"+CustomizationPrefs.getNotificationUri(c)+"|"+CustomizationPrefs.vibrationEnabled(c);return MSG_PREFIX+Integer.toHexString(sig.hashCode());}
    public static String currentCallChannelId(Context c){String sig=CustomizationPrefs.getCallMode(c)+"|"+CustomizationPrefs.getCallUri(c)+"|"+CustomizationPrefs.vibrationEnabled(c);return CALL_PREFIX+Integer.toHexString(sig.hashCode());}

    public static void ensureChannels(Context c){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(nm==null)return;
        String msgId=currentMessageChannelId(c);
        if(nm.getNotificationChannel(msgId)==null){
            NotificationChannel ch=new NotificationChannel(msgId,"PersonaChat simulated messages",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Notifications for fictional PersonaChat conversations");
            applySound(ch,resolveNotificationUri(c),"silent".equals(CustomizationPrefs.getNotificationMode(c)),AudioAttributes.USAGE_NOTIFICATION);
            ch.enableVibration(CustomizationPrefs.vibrationEnabled(c));
            ch.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            nm.createNotificationChannel(ch);
        }
        String callId=currentCallChannelId(c);
        if(nm.getNotificationChannel(callId)==null){
            NotificationChannel ch=new NotificationChannel(callId,"PersonaChat simulated calls",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Fictional PersonaChat incoming call simulation");
            applySound(ch,resolveCallUri(c),"silent".equals(CustomizationPrefs.getCallMode(c)),AudioAttributes.USAGE_NOTIFICATION_RINGTONE);
            ch.enableVibration(CustomizationPrefs.vibrationEnabled(c));
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
    }

    private static void applySound(NotificationChannel ch,Uri uri,boolean silent,int usage){if(silent){ch.setSound(null,null);return;}ch.setSound(uri,new AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());}
    public static Uri resolveNotificationUri(Context c){String mode=CustomizationPrefs.getNotificationMode(c);if("silent".equals(mode))return null;if("custom".equals(mode)){try{return Uri.parse(CustomizationPrefs.getNotificationUri(c));}catch(Exception ignored){}}return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);}
    public static Uri resolveCallUri(Context c){String mode=CustomizationPrefs.getCallMode(c);if("silent".equals(mode))return null;if("custom".equals(mode)){try{return Uri.parse(CustomizationPrefs.getCallUri(c));}catch(Exception ignored){}}return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);}

    public static void showMessage(Context c,Store.Bot bot,String newestText){
        ensureChannels(c);
        Intent open=new Intent(c,ChatActivity.class);open.putExtra("bot_id",bot.id);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi=PendingIntent.getActivity(c,(int)bot.id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent read=new Intent(c,MarkReadReceiver.class);read.putExtra("bot_id",bot.id);
        PendingIntent readPi=PendingIntent.getBroadcast(c,(int)(bot.id+200000),read,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Bitmap avatar=characterAvatar(c,bot);
        Person.Builder personBuilder=new Person.Builder().setName(bot.name+" • Simulation").setKey("sim-bot-"+bot.id);
        if(avatar!=null)personBuilder.setIcon(Icon.createWithAdaptiveBitmap(avatar));
        Person person=personBuilder.build();

        Notification.MessagingStyle style=new Notification.MessagingStyle(person).setConversationTitle(bot.name+" • Simulation");
        List<Store.Message> recent=Store.loadMessages(c,bot.id);
        for(int i=Math.max(0,recent.size()-8);i<recent.size();i++){
            Store.Message m=recent.get(i);
            if(!"reaction_event".equals(m.kind))style.addMessage(m.text,m.time,m.incoming?person:null);
        }
        Notification.Action markRead=new Notification.Action.Builder(Icon.createWithResource(c,R.drawable.ic_notification),"Mark as read",readPi).setSemanticAction(Notification.Action.SEMANTIC_ACTION_MARK_AS_READ).build();
        Notification.Builder b=new Notification.Builder(c,currentMessageChannelId(c))
                .setSmallIcon(CustomizationHelper.notificationSmallIcon(c))
                .setContentTitle(bot.name+" • Simulation")
                .setContentText(newestText)
                .setStyle(style)
                .setContentIntent(openPi)
                .addAction(markRead)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setGroup("sim_chat_"+bot.id)
                .setOnlyAlertOnce(false)
                .setNumber(bot.unread)
                .setVisibility(Notification.VISIBILITY_PRIVATE);
        if(avatar!=null)b.setLargeIcon(avatar);
        ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(messageNotificationId(bot.id),b.build());
    }

    public static void cancelMessage(Context c,long botId){NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(nm!=null)nm.cancel(messageNotificationId(botId));}

    public static void showCall(Context c,Store.Bot bot){
        ensureChannels(c);
        Intent open=new Intent(c,IncomingCallActivity.class);open.putExtra("bot_id",bot.id);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(c,(int)(bot.id+4000),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(c,currentCallChannelId(c))
                .setSmallIcon(CustomizationHelper.notificationSmallIcon(c))
                .setContentTitle("SIMULATED INCOMING CALL")
                .setContentText(bot.name+" - "+bot.phone)
                .setContentIntent(pi)
                .setFullScreenIntent(pi,true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_CALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        Bitmap avatar=characterAvatar(c,bot);if(avatar!=null)b.setLargeIcon(avatar);
        ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify((int)(bot.id+8000),b.build());
    }

    public static void cancelCall(Context c,Store.Bot bot){NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(nm!=null)nm.cancel((int)(bot.id+8000));}

    private static Bitmap characterAvatar(Context c,Store.Bot bot){
        if(bot!=null&&bot.avatarUri!=null&&!bot.avatarUri.trim().isEmpty()){
            try{
                ImageDecoder.Source source=ImageDecoder.createSource(c.getContentResolver(),Uri.parse(bot.avatarUri));
                Bitmap raw=ImageDecoder.decodeBitmap(source,(decoder,info,src)->decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
                if(raw!=null){
                    int side=Math.min(raw.getWidth(),raw.getHeight());
                    int left=(raw.getWidth()-side)/2,top=(raw.getHeight()-side)/2;
                    Bitmap crop=Bitmap.createBitmap(raw,left,top,side,side);
                    Bitmap scaled=Bitmap.createScaledBitmap(crop,256,256,true);
                    if(crop!=raw&&!crop.isRecycled())crop.recycle();
                    if(raw!=scaled&&!raw.isRecycled())raw.recycle();
                    return scaled;
                }
            }catch(Exception ignored){}
        }
        return CustomizationHelper.notificationLargeBitmap(c);
    }
}
