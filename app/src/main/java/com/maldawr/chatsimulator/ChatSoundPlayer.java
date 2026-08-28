package com.maldawr.chatsimulator;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public final class ChatSoundPlayer {
    private final Context context;
    private SoundPool soundPool;
    private int incomingId,outgoingId,incomingStreamId,outgoingStreamId;
    private volatile boolean incomingReady,outgoingReady,pendingIncoming,pendingOutgoing;

    public ChatSoundPlayer(Context context){
        this.context=context.getApplicationContext();
        if(!CustomizationPrefs.chatSoundsEnabled(this.context))return;
        AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        soundPool=new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build();
        soundPool.setOnLoadCompleteListener((pool,sampleId,status)->{
            if(status!=0)return;
            if(sampleId==incomingId){incomingReady=true;if(pendingIncoming){pendingIncoming=false;incomingStreamId=playLoaded(incomingId,incomingStreamId);}}
            if(sampleId==outgoingId){outgoingReady=true;if(pendingOutgoing){pendingOutgoing=false;outgoingStreamId=playLoaded(outgoingId,outgoingStreamId);}}
        });
        String in=CustomizationPrefs.incomingPath(this.context),out=CustomizationPrefs.outgoingPath(this.context);
        incomingId=in.isEmpty()?soundPool.load(this.context,R.raw.message_incoming_v3,1):soundPool.load(in,1);
        outgoingId=out.isEmpty()?soundPool.load(this.context,R.raw.message_outgoing,1):soundPool.load(out,1);
    }
    public void playIncoming(){if(soundPool==null||!CustomizationPrefs.chatSoundsEnabled(context))return;if(!incomingReady||incomingId==0){pendingIncoming=true;return;}pendingIncoming=false;incomingStreamId=playLoaded(incomingId,incomingStreamId);}
    public void playOutgoing(){if(soundPool==null||!CustomizationPrefs.chatSoundsEnabled(context))return;if(!outgoingReady||outgoingId==0){pendingOutgoing=true;return;}pendingOutgoing=false;outgoingStreamId=playLoaded(outgoingId,outgoingStreamId);}
    private int playLoaded(int sampleId,int previous){try{if(previous!=0)soundPool.stop(previous);return soundPool.play(sampleId,1f,1f,1,0,1f);}catch(Exception ignored){return 0;}}
    public void release(){try{if(soundPool!=null)soundPool.release();}catch(Exception ignored){}soundPool=null;incomingReady=outgoingReady=pendingIncoming=pendingOutgoing=false;}
}
