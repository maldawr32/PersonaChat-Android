package com.maldawr.chatsimulator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
    private Store.Bot bot; private EditText name,phone,status,unread,activeFrom,activeTo,maxBurst,groupSubtitle; private CheckBox autoReply,initiative,favorite,groupChat; private Spinner replyMode; private ImageView avatarPreview; private String avatarUri="";
    @Override protected void onCreate(Bundle b){super.onCreate(b);Store.ensureSeeded(this);long id=getIntent().getLongExtra("bot_id",-1);bot=id==-1?null:Store.getBot(this,id);if(bot==null)bot=new Store.Bot(System.currentTimeMillis(),"New Fictional Chat",Store.getPrefix(this)+" 000 000 000");avatarUri=bot.avatarUri==null?"":bot.avatarUri;buildUi();}
    private void buildUi(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Ui.bg(this));root.addView(Ui.safetyBanner(this));ScrollView scroll=new ScrollView(this);LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,28));TextView title=Ui.label(this,"Conversation editor",24,true);form.addView(title);avatarPreview=new ImageView(this);avatarPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);avatarPreview.setBackground(Ui.circle(0xFF4C7F86));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(Ui.dp(this,96),Ui.dp(this,96));ap.setMargins(0,Ui.dp(this,12),0,Ui.dp(this,8));form.addView(avatarPreview,ap);if(!avatarUri.isEmpty())try{avatarPreview.setImageURI(Uri.parse(avatarUri));}catch(Exception ignored){}Button choose=Ui.button(this,"Choose profile / group image");choose.setOnClickListener(v->chooseImage());form.addView(choose);
        name=field("Name",bot.name);phone=field("Fictional number",bot.phone);status=field("Status",bot.status);unread=field("Unread counter",String.valueOf(bot.unread));activeFrom=field("Active from hour (0-23)",String.valueOf(bot.activeFrom));activeTo=field("Active until hour (1-24)",String.valueOf(bot.activeTo));maxBurst=field("Maximum reply burst (1-5)",String.valueOf(bot.maxBurst));groupSubtitle=field("Group members subtitle",bot.groupSubtitle);form.addView(name);form.addView(phone);form.addView(status);
        favorite=check("Favorite conversation",bot.favorite);form.addView(favorite);groupChat=check("Group conversation",bot.groupChat);form.addView(groupChat);form.addView(groupSubtitle);form.addView(unread);form.addView(activeFrom);form.addView(activeTo);TextView ml=Ui.label(this,"Reply timing",14,true);form.addView(ml);replyMode=new Spinner(this);replyMode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Instant","Natural","Slow"}));String current=bot.replyMode==null?"natural":bot.replyMode;replyMode.setSelection("instant".equals(current)?0:"slow".equals(current)?2:1);form.addView(replyMode);form.addView(maxBurst);autoReply=check("Automatic contextual replies",bot.autoReply);initiative=check("Allow later follow-up messages",bot.initiative);form.addView(autoReply);form.addView(initiative);Button save=Ui.button(this,"Save conversation");save.setOnClickListener(v->save());form.addView(save);scroll.addView(form,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private CheckBox check(String t,boolean v){CheckBox c=new CheckBox(this);c.setText(t);c.setTextColor(Ui.text(this));c.setChecked(v);return c;} private EditText field(String h,String v){EditText e=new EditText(this);e.setHint(h);e.setText(v==null?"":v);e.setTextColor(Ui.text(this));e.setHintTextColor(Ui.sub(this));return e;}
    private void chooseImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQ_IMAGE);}@Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r!=REQ_IMAGE||c!=RESULT_OK||d==null||d.getData()==null)return;Uri u=d.getData();try{getContentResolver().takePersistableUriPermission(u,d.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}avatarUri=u.toString();avatarPreview.setImageURI(u);}
    private void save(){bot.name=clean(name,"Fictional Chat");bot.phone=clean(phone,Store.getPrefix(this)+" 000 000 000");bot.status=clean(status,"SIMULATION - fictional conversation");bot.unread=clamp(unread,0,99,0);bot.activeFrom=clamp(activeFrom,0,23,0);bot.activeTo=clamp(activeTo,1,24,24);bot.maxBurst=clamp(maxBurst,1,5,3);bot.autoReply=autoReply.isChecked();bot.initiative=initiative.isChecked();bot.favorite=favorite.isChecked();bot.groupChat=groupChat.isChecked();bot.groupSubtitle=bot.groupChat?clean(groupSubtitle,"Maya, Nour, You"):"";int s=replyMode.getSelectedItemPosition();bot.replyMode=s==0?"instant":s==2?"slow":"natural";bot.avatarUri=avatarUri;Store.saveBot(this,bot);Toast.makeText(this,"Conversation saved",Toast.LENGTH_SHORT).show();finish();}
    private String clean(EditText e,String f){String x=e.getText().toString().trim();return x.isEmpty()?f:x;}private int clamp(EditText e,int min,int max,int f){try{return Math.max(min,Math.min(max,Integer.parseInt(e.getText().toString().trim())));}catch(Exception x){return f;}}
}
