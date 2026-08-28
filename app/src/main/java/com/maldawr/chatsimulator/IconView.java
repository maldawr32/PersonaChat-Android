package com.maldawr.chatsimulator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class IconView extends View {
    public static final int PHONE=1, VIDEO=2, CAMERA=3, SEARCH=4, CALENDAR=5, KEYPAD=6, HEART=7, CHAT=8, UPDATES=9, TOOLS=10, MORE=11, BACK=12, SEND=13, MIC=14, STAR=15, PLUS=16;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private int type; private int tint=0xFFE9EDEF;
    public IconView(Context c,int type,int sizeDp){super(c);this.type=type;setMinimumWidth(Ui.dp(c,sizeDp));setMinimumHeight(Ui.dp(c,sizeDp));setClickable(true);setFocusable(true);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
    public IconView tint(int color){tint=color;invalidate();return this;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),s=Math.min(w,h),cx=w/2f,cy=h/2f;String style=Store.getIconStyle(getContext());float sw="minimal".equals(style)?s*.055f:"bold".equals(style)?s*.085f:s*.067f;p.setStrokeWidth(sw);p.setColor(tint);p.setStyle(Paint.Style.STROKE);Path path=new Path();
        switch(type){
            case PHONE:{path.moveTo(cx-s*.22f,cy-s*.30f);path.cubicTo(cx-s*.35f,cy-s*.05f,cx-s*.08f,cy+s*.28f,cx+s*.20f,cy+s*.31f);path.cubicTo(cx+s*.29f,cy+s*.32f,cx+s*.34f,cy+s*.17f,cx+s*.27f,cy+s*.12f);path.lineTo(cx+s*.12f,cy+s*.04f);path.cubicTo(cx+s*.05f,cy, cx,cy+s*.08f,cx-s*.04f,cy+s*.02f);path.lineTo(cx-s*.12f,cy-s*.10f);path.cubicTo(cx-s*.16f,cy-s*.16f,cx-s*.06f,cy-s*.20f,cx-s*.09f,cy-s*.27f);path.lineTo(cx-s*.16f,cy-s*.38f);c.drawPath(path,p);break;}
            case VIDEO:{RectF r=new RectF(cx-s*.34f,cy-s*.23f,cx+s*.12f,cy+s*.23f);c.drawRoundRect(r,s*.05f,s*.05f,p);path.moveTo(cx+s*.12f,cy-s*.10f);path.lineTo(cx+s*.34f,cy-s*.22f);path.lineTo(cx+s*.34f,cy+s*.22f);path.lineTo(cx+s*.12f,cy+s*.10f);c.drawPath(path,p);break;}
            case CAMERA:{RectF r=new RectF(cx-s*.34f,cy-s*.24f,cx+s*.34f,cy+s*.28f);c.drawRoundRect(r,s*.08f,s*.08f,p);path.moveTo(cx-s*.15f,cy-s*.24f);path.lineTo(cx-s*.07f,cy-s*.36f);path.lineTo(cx+s*.10f,cy-s*.36f);path.lineTo(cx+s*.18f,cy-s*.24f);c.drawPath(path,p);c.drawCircle(cx,cy+s*.01f,s*.15f,p);break;}
            case SEARCH:{c.drawCircle(cx-s*.05f,cy-s*.05f,s*.22f,p);c.drawLine(cx+s*.11f,cy+s*.11f,cx+s*.31f,cy+s*.31f,p);break;}
            case CALENDAR:{RectF r=new RectF(cx-s*.30f,cy-s*.25f,cx+s*.30f,cy+s*.30f);c.drawRoundRect(r,s*.05f,s*.05f,p);c.drawLine(cx-s*.30f,cy-s*.08f,cx+s*.30f,cy-s*.08f,p);c.drawLine(cx-s*.15f,cy-s*.35f,cx-s*.15f,cy-s*.18f,p);c.drawLine(cx+s*.15f,cy-s*.35f,cx+s*.15f,cy-s*.18f,p);break;}
            case KEYPAD:{p.setStyle(Paint.Style.FILL);for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)c.drawCircle(cx+x*s*.18f,cy+y*s*.18f,s*.045f,p);break;}
            case HEART:{path.moveTo(cx,cy+s*.30f);path.cubicTo(cx-s*.42f,cy+s*.06f,cx-s*.34f,cy-s*.28f,cx-s*.12f,cy-s*.28f);path.cubicTo(cx,cy-s*.28f,cx,cy-s*.16f,cx,cy-s*.16f);path.cubicTo(cx,cy-s*.16f,cx,cy-s*.28f,cx+s*.12f,cy-s*.28f);path.cubicTo(cx+s*.34f,cy-s*.28f,cx+s*.42f,cy+s*.06f,cx,cy+s*.30f);c.drawPath(path,p);break;}
            case CHAT:{RectF r=new RectF(cx-s*.32f,cy-s*.25f,cx+s*.32f,cy+s*.20f);c.drawRoundRect(r,s*.11f,s*.11f,p);path.moveTo(cx-s*.12f,cy+s*.20f);path.lineTo(cx-s*.24f,cy+s*.34f);path.lineTo(cx+s*.02f,cy+s*.20f);c.drawPath(path,p);break;}
            case UPDATES:{c.drawCircle(cx,cy,s*.23f,p);c.drawArc(new RectF(cx-s*.35f,cy-s*.35f,cx+s*.35f,cy+s*.35f),35,105,false,p);c.drawArc(new RectF(cx-s*.35f,cy-s*.35f,cx+s*.35f,cy+s*.35f),215,105,false,p);break;}
            case TOOLS:{RectF r=new RectF(cx-s*.30f,cy-s*.22f,cx+s*.30f,cy+s*.27f);c.drawRoundRect(r,s*.04f,s*.04f,p);c.drawLine(cx-s*.30f,cy-s*.05f,cx+s*.30f,cy-s*.05f,p);c.drawLine(cx-s*.16f,cy-s*.33f,cx-s*.16f,cy-s*.14f,p);c.drawLine(cx+s*.16f,cy-s*.33f,cx+s*.16f,cy-s*.14f,p);break;}
            case MORE:{p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy-s*.20f,s*.045f,p);c.drawCircle(cx,cy,s*.045f,p);c.drawCircle(cx,cy+s*.20f,s*.045f,p);break;}
            case BACK:{c.drawLine(cx+s*.20f,cy-s*.26f,cx-s*.15f,cy,p);c.drawLine(cx-s*.15f,cy,cx+s*.20f,cy+s*.26f,p);c.drawLine(cx-s*.12f,cy,cx+s*.30f,cy,p);break;}
            case SEND:{p.setStyle(Paint.Style.FILL);path.moveTo(cx-s*.30f,cy-s*.22f);path.lineTo(cx+s*.35f,cy);path.lineTo(cx-s*.30f,cy+s*.22f);path.lineTo(cx-s*.17f,cy);path.close();c.drawPath(path,p);break;}
            case MIC:{c.drawRoundRect(new RectF(cx-s*.11f,cy-s*.30f,cx+s*.11f,cy+s*.10f),s*.10f,s*.10f,p);c.drawArc(new RectF(cx-s*.22f,cy-s*.06f,cx+s*.22f,cy+s*.26f),0,180,false,p);c.drawLine(cx,cy+s*.26f,cx,cy+s*.38f,p);break;}
            case STAR:{p.setStyle(Paint.Style.FILL);for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float rr=i%2==0?s*.34f:s*.15f;float x=cx+(float)Math.cos(a)*rr,y=cy+(float)Math.sin(a)*rr;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();c.drawPath(path,p);break;}
            case PLUS:{c.drawLine(cx-s*.25f,cy,cx+s*.25f,cy,p);c.drawLine(cx,cy-s*.25f,cx,cy+s*.25f,p);break;}
        }
    }
}
