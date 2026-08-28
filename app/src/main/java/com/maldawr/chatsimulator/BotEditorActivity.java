package com.maldawr.chatsimulator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BotEditorActivity extends Activity {
    private static final int REQ_IMAGE=701;
    private Store.Bot bot;
    private EditText name,phone,status,unread,activeFrom,activeTo,maxBurst,groupSubtitle,personality,emojiRate,humorRate,replyChance,quietStart,quietEnd;
    private CheckBox autoReply,initiative,favorite,groupChat,onlineContent;
    private Spinner replyMode;
    private ImageView avatarPreview;
    private String avatarUri="";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        Store.ensureSeeded(this);
        long id=getIntent().getLongExtra("bot_id",-1);
        bot=id==-1?null:Store.getBot(this,id);
        if(bot==null)bot=new Store.Bot(System.currentTimeMillis(),"New Fictional Chat",Store.getPrefix(this)+" 000 000 000");
        if(getIntent().getBooleanExtra("new_group",false)){bot.groupChat=true;bot.name="New Fictional Group";bot.groupSubtitle="Member 1, Member 2, You";bot.maxBurst=5;bot.personality="active group of distinct friends";}
        avatarUri=bot.avatarUri==null?"":bot.avatarUri;
        buildUi();
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Ui.bg(this));root.addView(Ui.safetyBanner(this));
        ScrollView scroll=new ScrollView(this);LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,28));
        TextView title=Ui.label(this,"Conversation & persona editor",24,true);form.addView(title);
        avatarPreview=new ImageView(this);avatarPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);avatarPreview.setBackground(Ui.circle(0xFF4C7F86));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(Ui.dp(this,96),Ui.dp(this,96));ap.setMargins(0,Ui.dp(this,12),0,Ui.dp(this,8));form.addView(avatarPreview,ap);if(!avatarUri.isEmpty())try{avatarPreview.setImageURI(Uri.parse(avatarUri));}catch(Exception ignored){}
        Button choose=Ui.button(this,"Choose profile / group image");choose.setOnClickListener(v->chooseImage());form.addView(choose);

        name=field("Name",bot.name);phone=field("Fictional number",bot.phone);status=field("Status / about",bot.status);personality=field("Personality / role instructions",bot.personality);groupSubtitle=field("Group members subtitle",bot.groupSubtitle);
        form.addView(name);form.addView(phone);form.addView(status);form.addView(personality);
        favorite=check("Favorite conversation",bot.favorite);groupChat=check("Group conversation",bot.groupChat);form.addView(favorite);form.addView(groupChat);form.addView(groupSubtitle);

        TextView behavior=Ui.label(this,"Behavior",18,true);behavior.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,4));form.addView(behavior);
        emojiRate=field("Emoji tendency 0-100",String.valueOf(bot.emojiRate));humorRate=field("Humor 0-100",String.valueOf(bot.humorRate));replyChance=field("Reply probability 0-100",String.valueOf(bot.replyChance));maxBurst=field("Maximum reply burst 1-5",String.valueOf(bot.maxBurst));
        form.addView(emojiRate);form.addView(humorRate);form.addView(replyChance);form.addView(maxBurst);
        autoReply=check("Automatic contextual replies",bot.autoReply);initiative=check("Allow proactive / follow-up messages",bot.initiative);onlineContent=check("Allow online information when useful",bot.onlineContent);form.addView(autoReply);form.addView(initiative);form.addView(onlineContent);

        TextView timing=Ui.label(this,"Timing",18,true);timing.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,4));form.addView(timing);
        activeFrom=field("Active from hour 0-23",String.valueOf(bot.activeFrom));activeTo=field("Active until hour 1-24",String.valueOf(bot.activeTo));quietStart=field("Quiet start hour 0-23",String.valueOf(bot.quietStart));quietEnd=field("Quiet end hour 0-23 (same = off)",String.valueOf(bot.quietEnd));unread=field("Unread counter",String.valueOf(bot.unread));
        form.addView(activeFrom);form.addView(activeTo);form.addView(quietStart);form.addView(quietEnd);form.addView(unread);
        TextView ml=Ui.label(this,"Reply timing style",14,true);form.addView(ml);replyMode=new Spinner(this);replyMode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Instant","Natural","Slow"}));String current=bot.replyMode==null?"natural":bot.replyMode;replyMode.setSelection("instant".equals(current)?0:"slow".equals(current)?2:1);form.addView(replyMode);

        Button save=Ui.button(this,"Save conversation");save.setOnClickListener(v->save(false));form.addView(save);
        Button groupControl=Ui.button(this,"Save & open group control center");groupControl.setOnClickListener(v->save(true));form.addView(groupControl);

        scroll.addView(form,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private CheckBox check(String t,boolean v){CheckBox c=new CheckBox(this);c.setText(t);c.setTextColor(Ui.text(this));c.setChecked(v);return c;}
    private EditText field(String h,String v){EditText e=new EditText(this);e.setHint(h);e.setText(v==null?"":v);e.setTextColor(Ui.text(this));e.setHintTextColor(Ui.sub(this));return e;}
    private void chooseImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQ_IMAGE);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r!=REQ_IMAGE||c!=RESULT_OK||d==null||d.getData()==null)return;Uri u=d.getData();try{getContentResolver().takePersistableUriPermission(u,d.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}avatarUri=u.toString();avatarPreview.setImageURI(u);}

    private void save(boolean openGroup){
        bot.name=clean(name,"Fictional Chat");bot.phone=clean(phone,Store.getPrefix(this)+" 000 000 000");bot.status=clean(status,"Fictional conversation");bot.personality=clean(personality,"friendly");
        bot.unread=clamp(unread,0,99,0);bot.activeFrom=clamp(activeFrom,0,23,0);bot.activeTo=clamp(activeTo,1,24,24);bot.quietStart=clamp(quietStart,0,23,0);bot.quietEnd=clamp(quietEnd,0,23,0);bot.maxBurst=clamp(maxBurst,1,5,3);bot.emojiRate=clamp(emojiRate,0,100,45);bot.humorRate=clamp(humorRate,0,100,25);bot.replyChance=clamp(replyChance,0,100,84);
        bot.autoReply=autoReply.isChecked();bot.initiative=initiative.isChecked();bot.onlineContent=onlineContent.isChecked();bot.favorite=favorite.isChecked();bot.groupChat=groupChat.isChecked();bot.groupSubtitle=bot.groupChat?clean(groupSubtitle,"Member 1, Member 2, You"):"";
        int s=replyMode.getSelectedItemPosition();bot.replyMode=s==0?"instant":s==2?"slow":"natural";bot.avatarUri=avatarUri;Store.saveBot(this,bot);
        Toast.makeText(this,"Conversation saved",Toast.LENGTH_SHORT).show();
        if(openGroup){if(!bot.groupChat){Toast.makeText(this,"Enable Group conversation first",Toast.LENGTH_SHORT).show();return;}Intent i=new Intent(this,GroupMembersActivity.class);i.putExtra("bot_id",bot.id);startActivity(i);}else finish();
    }

    private String clean(EditText e,String f){String x=e.getText().toString().trim();return x.isEmpty()?f:x;}
    private int clamp(EditText e,int min,int max,int f){try{return Math.max(min,Math.min(max,Integer.parseInt(e.getText().toString().trim())));}catch(Exception x){return f;}}
}
