package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    private Ui(){}
    private static final class SimTextView extends TextView {
        SimTextView(Context c){super(c);}
        @Override public void setText(CharSequence text,BufferType type){CharSequence value=text;if(getContext() instanceof ChatActivity){String s=text==null?"":text.toString();if(!CustomizationPrefs.showReadReceipts(getContext())&&s.contains("✓✓"))value=s.replace("✓✓","").trim();if(!CustomizationPrefs.showTyping(getContext())&&s.toLowerCase().contains("typing"))value="online • simulated contact";}super.setText(value,type);}
    }
    public static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}public static boolean isDark(Context c){int mode=c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK;return mode==Configuration.UI_MODE_NIGHT_YES;}
    public static int bg(Context c){return isDark(c)?Color.rgb(17,27,33):Color.WHITE;}public static int card(Context c){return isDark(c)?Color.rgb(31,44,51):Color.WHITE;}public static int text(Context c){return isDark(c)?Color.rgb(233,237,239):Color.rgb(17,27,33);}public static int sub(Context c){return isDark(c)?Color.rgb(134,150,160):Color.rgb(102,119,129);}public static int divider(Context c){return isDark(c)?Color.rgb(42,57,66):Color.rgb(236,239,241);}public static int chatBg(Context c){return CustomizationPrefs.chatBackground(c);}public static int brand(){return Color.rgb(11,107,93);}public static int brandDark(){return Color.rgb(8,83,74);}public static int brandBright(){return Color.rgb(33,161,121);}public static int red(){return Color.rgb(211,60,60);}
    public static GradientDrawable rounded(int color,float radiusDp,Context c){if(c instanceof ChatActivity){if(color==0xFF202C33)color=CustomizationPrefs.incomingBubble(c);else if(color==0xFF075E54)color=CustomizationPrefs.outgoingBubble(c);if(color==CustomizationPrefs.incomingBubble(c)||color==CustomizationPrefs.outgoingBubble(c))radiusDp=CustomizationPrefs.getBubbleRadius(c);}GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,Math.round(radiusDp)));return d;}
    public static GradientDrawable circle(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setShape(GradientDrawable.OVAL);return d;}
    public static TextView label(Context c,String value,float sp,boolean bold){TextView v=c instanceof ChatActivity?new SimTextView(c):new TextView(c);if(c instanceof ChatActivity&&sp==17)sp=CustomizationPrefs.getMessageFontSize(c);v.setText(value);v.setTextSize(sp);v.setTextColor(text(c));v.setIncludeFontPadding(false);v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);return v;}
    public static Button button(Context c,String title){Button b=new Button(c);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(Color.WHITE);b.setBackground(rounded(CustomizationPrefs.accent(c),12,c));b.setPadding(dp(c,16),dp(c,7),dp(c,16),dp(c,7));return b;}
    public static TextView iconButton(Context c,String glyph,int sizeDp,float sp,int bgColor,int textColor){TextView v=new TextView(c);v.setText(glyph);v.setTextSize(sp);v.setTextColor(textColor);v.setGravity(Gravity.CENTER);v.setIncludeFontPadding(false);if(bgColor!=Color.TRANSPARENT)v.setBackground(circle(bgColor));v.setLayoutParams(new ViewGroup.LayoutParams(dp(c,sizeDp),dp(c,sizeDp)));v.setClickable(true);v.setFocusable(true);return v;}
    public static TextView oneLine(Context c,String value,float sp,int color){TextView v=label(c,value,sp,false);v.setTextColor(color);v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);return v;}
    public static View divider(Context c,int startDp){View v=new View(c);v.setBackgroundColor(divider(c));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(c,1));lp.setMargins(dp(c,startDp),0,0,0);v.setLayoutParams(lp);return v;}
    public static TextView safetyBanner(Context c){TextView b=new TextView(c);b.setText("محاكاة شخصية • بيانات خيالية");b.setTextSize(10);b.setTextColor(sub(c));b.setGravity(Gravity.CENTER);b.setIncludeFontPadding(false);b.setPadding(dp(c,8),dp(c,4),dp(c,8),dp(c,4));return b;}
    public static View avatar(Context c,Store.Bot bot,int sizeDp){int px=dp(c,sizeDp);FrameLayout f=new FrameLayout(c);f.setLayoutParams(new ViewGroup.LayoutParams(px,px));if(bot!=null&&bot.avatarUri!=null&&!bot.avatarUri.isEmpty())try{ImageView i=new ImageView(c);i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setImageURI(Uri.parse(bot.avatarUri));i.setClipToOutline(true);i.setBackground(circle(Color.rgb(95,118,126)));f.addView(i,new FrameLayout.LayoutParams(px,px));return f;}catch(Exception ignored){}TextView t=new TextView(c);String name=bot==null?"S":bot.name;String first=name==null||name.trim().isEmpty()?"B":name.trim().substring(0,1).toUpperCase();t.setText(first);t.setTextColor(Color.WHITE);t.setTextSize(sizeDp*.34f);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER);t.setIncludeFontPadding(false);t.setBackground(circle(Color.rgb(79,118,126)));f.addView(t,new FrameLayout.LayoutParams(px,px));return f;}
    public static LinearLayout cardRow(Context c){LinearLayout r=new LinearLayout(c);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(c,14),dp(c,11),dp(c,14),dp(c,11));r.setBackground(rounded(card(c),12,c));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(c,10),dp(c,5),dp(c,10),dp(c,5));r.setLayoutParams(p);return r;}
}
