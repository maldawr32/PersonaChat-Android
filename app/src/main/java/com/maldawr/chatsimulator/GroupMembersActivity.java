package com.maldawr.chatsimulator;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class GroupMembersActivity extends Activity {
    private Store.Bot bot;
    private LinearLayout membersHost;
    private TextView activityLabel;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        long botId = getIntent().getLongExtra("bot_id", -1L);
        bot = Store.getBot(this, botId);
        if (bot == null || !bot.groupChat) { finish(); return; }
        build();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.bg(this));
        root.addView(Ui.safetyBanner(this));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,34));

        TextView title = Ui.label(this, bot.name + " • Group control", 24, true);
        title.setTextColor(Ui.text(this));
        body.addView(title);
        TextView sub = Ui.label(this,"Independent member behavior, autonomous rounds and test controls.",13,false);
        sub.setTextColor(Ui.sub(this));
        sub.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,16));
        body.addView(sub);

        CheckBox autonomous = new CheckBox(this);
        autonomous.setText("Autonomous member-to-member conversation");
        autonomous.setTextColor(Ui.text(this));
        autonomous.setChecked(GroupPrefs.isAutonomous(this, bot.id));
        autonomous.setOnCheckedChangeListener((b,v)->GroupPrefs.setAutonomous(this, bot.id, v));
        body.addView(autonomous);

        int activity = GroupPrefs.getActivity(this, bot.id);
        activityLabel = Ui.label(this,"Group activity  " + activity + "%",16,true);
        activityLabel.setTextColor(Ui.text(this));
        activityLabel.setPadding(0,Ui.dp(this,12),0,0);
        body.addView(activityLabel);
        SeekBar activityBar = new SeekBar(this);
        activityBar.setMax(100);
        activityBar.setProgress(activity);
        activityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean from){activityLabel.setText("Group activity  "+p+"%");if(from)GroupPrefs.setActivity(GroupMembersActivity.this,bot.id,p);}
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        body.addView(activityBar);

        Button test = Ui.button(this,"Test this group now");
        test.setOnClickListener(v->{AutomationManager.runGroupNow(this,bot.id);Toast.makeText(this,"AI group round queued",Toast.LENGTH_SHORT).show();});
        body.addView(test);

        TextView memberTitle = Ui.label(this,"Members",18,true);
        memberTitle.setTextColor(Ui.text(this));
        memberTitle.setPadding(0,Ui.dp(this,22),0,Ui.dp(this,8));
        body.addView(memberTitle);
        membersHost = new LinearLayout(this);
        membersHost.setOrientation(LinearLayout.VERTICAL);
        body.addView(membersHost);
        renderMembers();

        Button add = Ui.button(this,"+ Add fictional member");
        add.setOnClickListener(v->editMember(null));
        body.addView(add);

        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private void renderMembers() {
        membersHost.removeAllViews();
        for (Store.GroupMember m : Store.loadGroupMembers(this, bot.id)) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Ui.dp(this,14),Ui.dp(this,10),Ui.dp(this,14),Ui.dp(this,10));
            row.setBackground(Ui.rounded(0xFF1F2C33,14,this));
            TextView n = Ui.label(this,m.name + "  " + m.emoji,16,true); n.setTextColor(m.color); row.addView(n);
            TextView s = Ui.label(this,m.style + " • activity " + m.activity + "% • humor " + m.humor + "%",13,false); s.setTextColor(Ui.sub(this)); row.addView(s);
            row.setOnClickListener(v->editMember(m));
            row.setOnLongClickListener(v->{confirmDelete(m);return true;});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,Ui.dp(this,8));membersHost.addView(row,lp);
        }
    }

    private void editMember(Store.GroupMember existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);
        EditText name = field("Name", existing==null?"":existing.name);
        EditText style = field("Personality / speaking style", existing==null?"friendly, natural":existing.style);
        EditText emoji = field("Signature emoji", existing==null?"🙂":existing.emoji);
        EditText activity = field("Member activity 0-100", existing==null?"70":String.valueOf(existing.activity));
        EditText humor = field("Humor 0-100", existing==null?"45":String.valueOf(existing.humor));
        form.addView(name);form.addView(style);form.addView(emoji);form.addView(activity);form.addView(humor);
        new AlertDialog.Builder(this)
                .setTitle(existing==null?"New group member":"Edit " + existing.name)
                .setView(form)
                .setPositiveButton("Save",(d,w)->{
                    List<Store.GroupMember> members = new ArrayList<>(Store.loadGroupMembers(this,bot.id));
                    Store.GroupMember target = existing;
                    if (target == null) {
                        target = new Store.GroupMember(System.currentTimeMillis(),bot.id,"Member","friendly","🙂",70,45,0xFF53BDEB);
                        members.add(target);
                    }
                    target.name = clean(name,"Member");
                    target.style = clean(style,"friendly");
                    target.emoji = clean(emoji,"🙂");
                    target.activity = clamp(activity,0,100,70);
                    target.humor = clamp(humor,0,100,45);
                    Store.replaceGroupMembers(this,bot.id,members);
                    renderMembers();
                }).setNegativeButton("Cancel",null).show();
    }

    private void confirmDelete(Store.GroupMember member) {
        new AlertDialog.Builder(this).setTitle("Remove " + member.name + "?")
                .setMessage("This only removes the fictional member from this simulated group.")
                .setPositiveButton("Remove",(d,w)->{
                    List<Store.GroupMember> members = new ArrayList<>(Store.loadGroupMembers(this,bot.id));
                    members.removeIf(x->x.id==member.id);
                    Store.replaceGroupMembers(this,bot.id,members);
                    renderMembers();
                }).setNegativeButton("Cancel",null).show();
    }

    private EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(Ui.text(this));e.setHintTextColor(Ui.sub(this));return e;}
    private String clean(EditText e,String fallback){String x=e.getText().toString().trim();return x.isEmpty()?fallback:x;}
    private int clamp(EditText e,int min,int max,int fallback){try{return Math.max(min,Math.min(max,Integer.parseInt(e.getText().toString().trim())));}catch(Exception ignored){return fallback;}}
}
