package com.maldawr.chatsimulator;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IncomingCallActivity extends Activity {
    private Store.Bot bot;private Ringtone ringtone;private boolean answered=false;private long answeredAt=0;private TextView state;private FrameLayout answer;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setShowWhenLocked(true);setTurnScreenOn(true);setVolumeControlStream(AudioManager.STREAM_RING);getWindow().setStatusBarColor(0xFF0B141A);getWindow().setNavigationBarColor(0xFF0B141A);Store.ensureSeeded(this);bot=Store.getBot(this,getIntent().getLongExtra("bot_id",-1));if(bot==null){finish();return;}buildUi();startRinging();}
    private void buildUi(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(Ui.dp(this,22),Ui.dp(this,40),Ui.dp(this,22),Ui.dp(this,34));root.setBackgroundColor(0xFF0B141A);TextView sim=Ui.label(this,"SIMULATED CALL",11,true);sim.setTextColor(CustomizationPrefs.accent(this));sim.setGravity(Gravity.CENTER);root.addView(sim,new LinearLayout.LayoutParams(-1,Ui.dp(this,30)));TextView type=Ui.label(this,"Incoming simulated call",15,false);type.setTextColor(0xFF8696A0);type.setGravity(Gravity.CENTER);root.addView(type,new LinearLayout.LayoutParams(-1,Ui.dp(this,42)));LinearLayout.LayoutParams avp=new LinearLayout.LayoutParams(Ui.dp(this,132),Ui.dp(this,132));avp.setMargins(0,Ui.dp(this,20),0,Ui.dp(this,18));root.addView(Ui.avatar(this,bot,132),avp);TextView name=Ui.label(this,bot.name,29,true);name.setTextColor(Color.WHITE);name.setGravity(Gravity.CENTER);root.addView(name);TextView sub=Ui.label(this,bot.groupChat?bot.groupSubtitle:bot.phone,15,false);sub.setTextColor(0xFF8696A0);sub.setGravity(Gravity.CENTER);root.addView(sub);state=Ui.label(this,"Ringing…",14,false);state.setTextColor(0xFF8696A0);state.setGravity(Gravity.CENTER);root.addView(state,new LinearLayout.LayoutParams(-1,Ui.dp(this,70)));View spacer=new View(this);root.addView(spacer,new LinearLayout.LayoutParams(1,0,1));LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);FrameLayout decline=roundAction(0xFFE53935,IconView.PHONE);decline.setRotation(135);decline.setOnClickListener(v->endCall());answer=roundAction(CustomizationPrefs.accent(this),IconView.PHONE);answer.setOnClickListener(v->{if(answered)endCall();else answerCall();});LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(Ui.dp(this,74),Ui.dp(this,74));dp.setMargins(0,0,Ui.dp(this,78),0);actions.addView(decline,dp);actions.addView(answer,new LinearLayout.LayoutParams(Ui.dp(this,74),Ui.dp(this,74)));root.addView(actions);setContentView(root);}
    private FrameLayout roundAction(int color,int icon){FrameLayout f=new FrameLayout(this);f.setBackground(Ui.circle(color));IconView v=new IconView(this,icon,36);f.addView(v,new FrameLayout.LayoutParams(Ui.dp(this,36),Ui.dp(this,36),Gravity.CENTER));return f;}
    private void startRinging(){if("silent".equals(CustomizationPrefs.getCallMode(this)))return;try{Uri uri=NotificationHelper.resolveCallUri(this);ringtone=RingtoneManager.getRingtone(this,uri);if(ringtone!=null){ringtone.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());ringtone.play();}}catch(Exception ignored){}}
    private void stopRinging(){try{if(ringtone!=null&&ringtone.isPlaying())ringtone.stop();}catch(Exception ignored){}}
    private void answerCall(){stopRinging();answered=true;answeredAt=System.currentTimeMillis();state.setText("Connected • simulation only");answer.setBackground(Ui.circle(0xFFE53935));answer.setRotation(135);}
    private void endCall(){stopRinging();NotificationHelper.cancelCall(this,bot);long now=System.currentTimeMillis();Store.addCall(this,new Store.CallItem(now,bot.id,answered?"incoming":"missed",now,answered?(int)Math.max(1,(now-answeredAt)/1000):0));finish();}
    @Override protected void onDestroy(){stopRinging();super.onDestroy();}
}
