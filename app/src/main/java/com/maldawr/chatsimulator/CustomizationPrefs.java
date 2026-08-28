package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class CustomizationPrefs {
    private static final String PREFS = "chat_simulator_customization_v71";
    private static final String NOTIF_MODE = "notification_mode", NOTIF_URI = "notification_uri";
    private static final String CALL_MODE = "call_mode", CALL_URI = "call_uri";
    private static final String CHAT_SOUNDS = "chat_sounds", VIBRATE = "vibrate", TICKS = "ticks", TYPING = "typing";
    private static final String IN_PATH = "incoming_path", IN_NAME = "incoming_name", OUT_PATH = "outgoing_path", OUT_NAME = "outgoing_name";
    private static final String PRESET = "preset", FONT = "message_font", RADIUS = "bubble_radius", PATTERN = "pattern_intensity";

    private CustomizationPrefs() {}
    private static SharedPreferences p(Context c){ return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static String getNotificationMode(Context c){ return p(c).getString(NOTIF_MODE,"default"); }
    public static String getNotificationUri(Context c){ return p(c).getString(NOTIF_URI,""); }
    public static void setNotificationSound(Context c,String mode,String uri){ p(c).edit().putString(NOTIF_MODE,mode).putString(NOTIF_URI,uri==null?"":uri).apply(); }
    public static String getCallMode(Context c){ return p(c).getString(CALL_MODE,"default"); }
    public static String getCallUri(Context c){ return p(c).getString(CALL_URI,""); }
    public static void setCallSound(Context c,String mode,String uri){ p(c).edit().putString(CALL_MODE,mode).putString(CALL_URI,uri==null?"":uri).apply(); }

    public static boolean chatSoundsEnabled(Context c){ return p(c).getBoolean(CHAT_SOUNDS,true); }
    public static void setChatSoundsEnabled(Context c,boolean v){ p(c).edit().putBoolean(CHAT_SOUNDS,v).apply(); }
    public static boolean vibrationEnabled(Context c){ return p(c).getBoolean(VIBRATE,true); }
    public static void setVibrationEnabled(Context c,boolean v){ p(c).edit().putBoolean(VIBRATE,v).apply(); }
    public static boolean showReadReceipts(Context c){ return p(c).getBoolean(TICKS,true); }
    public static void setShowReadReceipts(Context c,boolean v){ p(c).edit().putBoolean(TICKS,v).apply(); }
    public static boolean showTyping(Context c){ return p(c).getBoolean(TYPING,true); }
    public static void setShowTyping(Context c,boolean v){ p(c).edit().putBoolean(TYPING,v).apply(); }

    public static String getPreset(Context c){ return p(c).getString(PRESET,"midnight"); }
    public static void setPreset(Context c,String value){ p(c).edit().putString(PRESET,value==null?"midnight":value).apply(); }
    public static int getMessageFontSize(Context c){ return clamp(p(c).getInt(FONT,17),14,22); }
    public static void setMessageFontSize(Context c,int v){ p(c).edit().putInt(FONT,clamp(v,14,22)).apply(); }
    public static int getBubbleRadius(Context c){ return clamp(p(c).getInt(RADIUS,10),4,24); }
    public static void setBubbleRadius(Context c,int v){ p(c).edit().putInt(RADIUS,clamp(v,4,24)).apply(); }
    public static int getPatternIntensity(Context c){ return clamp(p(c).getInt(PATTERN,55),0,100); }
    public static void setPatternIntensity(Context c,int v){ p(c).edit().putInt(PATTERN,clamp(v,0,100)).apply(); }

    public static int chatBackground(Context c){ switch(getPreset(c)){case "ocean":return 0xFF07151F;case "violet":return 0xFF15111E;case "warm":return 0xFF17110F;case "graphite":return 0xFF101214;default:return 0xFF0B141A;} }
    public static int incomingBubble(Context c){ switch(getPreset(c)){case "ocean":return 0xFF1D2E3A;case "violet":return 0xFF2C2436;case "warm":return 0xFF352A27;case "graphite":return 0xFF24282B;default:return 0xFF202C33;} }
    public static int outgoingBubble(Context c){ switch(getPreset(c)){case "ocean":return 0xFF0C4A6E;case "violet":return 0xFF5B3F75;case "warm":return 0xFF6B4F3A;case "graphite":return 0xFF37474F;default:return 0xFF075E54;} }
    public static int accent(Context c){ switch(getPreset(c)){case "ocean":return 0xFF38BDF8;case "violet":return 0xFFB59BE6;case "warm":return 0xFFE7A66B;case "graphite":return 0xFF90A4AE;default:return 0xFF25B889;} }
    public static int patternColor(Context c){ switch(getPreset(c)){case "ocean":return 0xFF1E4154;case "violet":return 0xFF382C47;case "warm":return 0xFF3D2D28;case "graphite":return 0xFF30363A;default:return 0xFF1D2B31;} }

    public static String incomingPath(Context c){ return validPath(p(c).getString(IN_PATH,"")); }
    public static String outgoingPath(Context c){ return validPath(p(c).getString(OUT_PATH,"")); }
    public static String incomingName(Context c){ return p(c).getString(IN_NAME,"Receive 3 (built-in)"); }
    public static String outgoingName(Context c){ return p(c).getString(OUT_NAME,"Send (built-in)"); }

    public static boolean saveChatSound(Context c, Uri uri, boolean incoming){
        if(uri==null)return false;
        String prefix=incoming?"custom_chat_incoming":"custom_chat_outgoing";
        try{
            for(File f:c.getFilesDir().listFiles()) if(f.getName().startsWith(prefix)) f.delete();
            String mime=c.getContentResolver().getType(uri); String ext=MimeTypeMap.getSingleton().getExtensionFromMimeType(mime); if(ext==null||ext.isEmpty())ext="audio";
            File out=new File(c.getFilesDir(),prefix+"."+ext);
            try(InputStream in=c.getContentResolver().openInputStream(uri); FileOutputStream fos=new FileOutputStream(out)){
                if(in==null)return false; byte[] buf=new byte[16384]; int n; while((n=in.read(buf))>0)fos.write(buf,0,n);
            }
            String name=queryName(c,uri);
            p(c).edit().putString(incoming?IN_PATH:OUT_PATH,out.getAbsolutePath()).putString(incoming?IN_NAME:OUT_NAME,name).apply();
            return true;
        }catch(Exception e){return false;}
    }
    public static void resetChatSound(Context c,boolean incoming){
        String path=incoming?incomingPath(c):outgoingPath(c); if(!path.isEmpty())new File(path).delete();
        p(c).edit().remove(incoming?IN_PATH:OUT_PATH).putString(incoming?IN_NAME:OUT_NAME,incoming?"Receive 3 (built-in)":"Send (built-in)").apply();
    }
    private static String queryName(Context c,Uri uri){
        try(android.database.Cursor cur=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cur!=null&&cur.moveToFirst())return cur.getString(0);}catch(Exception ignored){}
        return "Custom audio";
    }
    private static String validPath(String value){ if(value==null||value.isEmpty())return ""; return new File(value).exists()?value:""; }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
}
