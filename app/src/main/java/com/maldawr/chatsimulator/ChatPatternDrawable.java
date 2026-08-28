package com.maldawr.chatsimulator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class ChatPatternDrawable extends Drawable {
    private final Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG),ink=new Paint(Paint.ANTI_ALIAS_FLAG); private final int step,intensity;
    public ChatPatternDrawable(Context c){bg.setColor(CustomizationPrefs.chatBackground(c));ink.setColor(CustomizationPrefs.patternColor(c));ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(Ui.dp(c,1));intensity=CustomizationPrefs.getPatternIntensity(c);ink.setAlpha((int)(intensity*1.8f));step=Ui.dp(c,58);}
    @Override public void draw(Canvas canvas){Rect b=getBounds();canvas.drawRect(b,bg);if(intensity<=0)return;int index=0;for(int y=0;y<b.height()+step;y+=step)for(int x=0;x<b.width()+step;x+=step){int ox=x+(((y/step)%2)*step/2);drawMotif(canvas,ox,y,index++%6);}}
    private void drawMotif(Canvas c,float x,float y,int type){float s=step*.26f;Path p=new Path();switch(type){case 0:c.drawCircle(x+s,y+s,s*.55f,ink);c.drawCircle(x+s,y+s,s*.18f,ink);break;case 1:c.drawRect(x,y+s*.25f,x+s*1.4f,y+s,ink);c.drawLine(x+s*.25f,y+s*.25f,x+s*.25f,y,ink);break;case 2:p.moveTo(x,y+s);p.lineTo(x+s*.55f,y);p.lineTo(x+s*1.1f,y+s);p.close();c.drawPath(p,ink);break;case 3:c.drawCircle(x+s*.35f,y+s*.45f,s*.24f,ink);c.drawCircle(x+s*.85f,y+s*.45f,s*.24f,ink);c.drawLine(x+s*.45f,y+s*.8f,x+s*.75f,y+s*.8f,ink);break;case 4:c.drawLine(x,y+s*.5f,x+s*1.3f,y+s*.5f,ink);c.drawLine(x+s*.65f,y,x+s*.65f,y+s,ink);break;default:c.drawRect(x+s*.1f,y+s*.1f,x+s,y+s*.9f,ink);c.drawLine(x+s*.1f,y+s*.1f,x+s,y+s*.9f,ink);}}
    @Override public void setAlpha(int a){ink.setAlpha(a);} @Override public void setColorFilter(android.graphics.ColorFilter f){ink.setColorFilter(f);} @Override public int getOpacity(){return android.graphics.PixelFormat.OPAQUE;}
}
