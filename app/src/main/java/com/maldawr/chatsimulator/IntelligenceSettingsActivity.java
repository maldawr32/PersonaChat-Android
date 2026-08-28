package com.maldawr.chatsimulator;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class IntelligenceSettingsActivity extends Activity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);build();}
    private void build(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xFF0B141A);root.addView(Ui.safetyBanner(this));ScrollView sc=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,32));TextView title=Ui.label(this,"Conversation Intelligence",25,true);title.setTextColor(0xFFFFFFFF);body.addView(title);TextView note=Ui.label(this,"Controls fictional local simulator conversations. Background timing is approximate because Android manages WorkManager execution.",13,false);note.setTextColor(0xFF8696A0);note.setPadding(0,Ui.dp(this,6),0,Ui.dp(this,14));body.addView(note);
        addSwitch(body,"Background conversation activity","Allow fictional bots and groups to start conversations periodically",Store.isAutomationEnabled(this),(c,v)->{Store.setAutomationEnabled(c,v);if(v)AutomationManager.ensureScheduled(c);else AutomationManager.cancel(c);});
        addSwitch(body,"Online information","Allow weather, currency and market data providers",Store.isOnlineContentEnabled(this),Store::setOnlineContentEnabled);
        addSwitch(body,"Wi‑Fi only for online data","Avoid mobile data for information lookups",Store.isWifiOnly(this),Store::setWifiOnly);
        addSlider(body,"Group activity",Store.getGroupActivity(this),Store::setGroupActivity);addSlider(body,"Emoji frequency",Store.getEmojiLevel(this),Store::setEmojiLevel);addSlider(body,"Humor & playful replies",Store.getHumorLevel(this),Store::setHumorLevel);
        Button test=Ui.button(this,"Run a background conversation test");test.setOnClickListener(v->{AutomationManager.runNow(this);Toast.makeText(this,"Background test queued",Toast.LENGTH_SHORT).show();});body.addView(test);sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private interface BoolSetter{void set(android.content.Context c,boolean v);}private interface IntSetter{void set(android.content.Context c,int v);}
    private void addSwitch(LinearLayout body,String title,String sub,boolean checked,BoolSetter setter){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10));LinearLayout text=new LinearLayout(this);text.setOrientation(LinearLayout.VERTICAL);TextView t=Ui.label(this,title,16,true);t.setTextColor(0xFFFFFFFF);text.addView(t);TextView s=Ui.label(this,sub,13,false);s.setTextColor(0xFF8696A0);text.addView(s);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));Switch sw=new Switch(this);sw.setChecked(checked);sw.setOnCheckedChangeListener((b,v)->setter.set(this,v));row.addView(sw);body.addView(row);}
    private void addSlider(LinearLayout body,String title,int value,IntSetter setter){TextView label=Ui.label(this,title+"  "+value+"%",15,true);label.setTextColor(0xFFFFFFFF);label.setPadding(0,Ui.dp(this,12),0,0);body.addView(label);SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(value);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){label.setText(title+"  "+p+"%");if(f)setter.set(IntelligenceSettingsActivity.this,p);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});body.addView(bar);}
}
