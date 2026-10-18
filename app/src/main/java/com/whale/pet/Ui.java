package com.whale.pet;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.animation.ValueAnimator;
import android.view.*;
import android.widget.*;
import org.json.*;

/** Small native design system, shared by the house and desktop menus. */
public final class Ui {
    public static int dp(Context c,float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    public static final class Palette {
        public final boolean dark;public final int bg,card,ink,muted,line,blue,soft;
        public Palette(boolean dark){this.dark=dark;bg=Color.parseColor(dark?"#101B30":"#F5F8FE");card=Color.parseColor(dark?"#192B4A":"#FFFFFF");ink=Color.parseColor(dark?"#E4EDFF":"#223C66");muted=Color.parseColor(dark?"#9AAFD0":"#7A8EAF");line=Color.parseColor(dark?"#2B4165":"#E0E8F6");blue=Color.parseColor(dark?"#9ABFFF":"#3575DF");soft=Color.parseColor(dark?"#263E65":"#E6EFFF");}
    }
    public static GradientDrawable round(int color,int stroke,float radius,Context c){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));if(stroke!=0)d.setStroke(dp(c,1),stroke);return d;}
    public static TextView text(Context c,String text,float sp,int color,boolean bold){TextView t=new TextView(c);t.setText(text);t.setTextSize(sp);t.setTextColor(color);t.setFontFeatureSettings("tnum");t.setTypeface(android.graphics.Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);return t;}
    public static TextView button(Context c,String text,Palette p,Runnable click){TextView b=text(c,text,13,p.ink,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));b.setBackground(ripple(c,p.soft,p.blue,16));press(b);b.setClickable(true);b.setOnClickListener(v->click.run());b.setMinimumHeight(dp(c,44));return b;}
    public static Drawable ripple(Context c,int color,int ink,float radius){return new RippleDrawable(ColorStateList.valueOf((ink&0x00ffffff)|0x22000000),round(color,0,radius,c),round(Color.WHITE,0,radius,c));}
    public static void press(View v){v.setOnTouchListener((view,event)->{if(ValueAnimator.areAnimatorsEnabled()){if(event.getActionMasked()==MotionEvent.ACTION_DOWN)view.animate().scaleX(.97f).scaleY(.97f).setDuration(100).start();else if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)view.animate().scaleX(1).scaleY(1).setDuration(180).start();}return false;});}
    public static void enter(View v){if(!ValueAnimator.areAnimatorsEnabled())return;v.setAlpha(0);v.setTranslationY(dp(v.getContext(),8));v.animate().alpha(1).translationY(0).setDuration(240).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
    public static Drawable symbol(Context c,String name,int color,int size){int id=c.getResources().getIdentifier("ui_"+name,"drawable",c.getPackageName());Drawable d=c.getDrawable(id).mutate();d.setTint(color);d.setBounds(0,0,dp(c,size),dp(c,size));return d;}
    public static ImageView icon(Context c,String name,int color){ImageView v=new ImageView(c);v.setImageDrawable(symbol(c,name,color,24));return v;}
    public static ImageView iconButton(Context c,String name,String description,Palette p,Runnable action){ImageView b=icon(c,name,p.ink);b.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));b.setBackground(ripple(c,Color.TRANSPARENT,p.blue,22));b.setContentDescription(description);b.setOnClickListener(v->action.run());press(b);return b;}
    public static TextView labeled(Context c,String label,String icon,Palette p,Runnable action){TextView t=button(c,label,p,action);t.setCompoundDrawables(symbol(c,icon,p.blue,19),null,null,null);t.setCompoundDrawablePadding(dp(c,8));return t;}
    public static void selected(TextView b,boolean on,Palette p){b.setSelected(on);b.setTextColor(on?(p.dark?0xff102344:Color.WHITE):p.muted);b.setBackground(ripple(b.getContext(),on?p.blue:Color.TRANSPARENT,p.blue,16));}
    /** Compact shared segmented filter; its caller owns the surrounding toolbar. */
    public static LinearLayout filters(Context c,String[] labels,int selected,Palette p,java.util.function.IntConsumer choose){
        LinearLayout group=row(c);group.setPadding(dp(c,3),dp(c,3),dp(c,3),dp(c,3));group.setBackground(round(p.soft,0,14,c));
        for(int i=0;i<labels.length;i++){final int index=i;boolean on=i==selected;TextView chip=text(c,labels[i],11,on?p.blue:p.muted,on);chip.setGravity(Gravity.CENTER);chip.setMaxLines(1);chip.setPadding(dp(c,10),0,dp(c,10),0);chip.setSelected(on);chip.setBackground(ripple(c,on?p.card:Color.TRANSPARENT,p.blue,11));chip.setOnClickListener(v->choose.accept(index));press(chip);group.addView(chip,new LinearLayout.LayoutParams(-2,dp(c,32)));}return group;
    }
    public static View divider(Context c,Palette p){View v=new View(c);v.setBackgroundColor(p.line);return v;}
    public static void rule(LinearLayout parent,Palette p){add(parent,divider(parent.getContext(),p),1,0);}
    public static LinearLayout modes(Context c,PetStore store,Palette p,java.util.function.Consumer<String> choose){LinearLayout row=row(c);row.setPadding(dp(c,4),dp(c,4),dp(c,4),dp(c,4));row.setBackground(round(p.soft,0,21,c));String selected=PetStore.mode(store.read());for(String key:new String[]{"walking","resting"}){TextView b=labeled(c,key.equals("walking")?"日常":"休息",key.equals("walking")?"wave":"moon",p,()->choose.accept(key));selected(b,key.equals(selected),p);b.setCompoundDrawables(symbol(c,key.equals("walking")?"wave":"moon",key.equals(selected)?(p.dark?0xff102344:Color.WHITE):p.muted,18),null,null,null);weight(row,b,2);}return row;}
    public static String actualEffects(PetCatalog.Action a,JSONObject state){StringBuilder s=new StringBuilder();String[] keys={"satiety","mood","energy"},names={"饱食","心情","精力"};for(int i=0;i<3;i++)if(a.effects.has(keys[i])){double before=state.optDouble(keys[i]),delta=PetStore.clamp(before+a.effects.optDouble(keys[i]))-before;if(s.length()>0)s.append("  ·  ");s.append(names[i]).append(delta>=0?" +":" ").append((int)Math.round(delta));}return s.toString();}
    /** Small code-native stationery motifs: no emoji or font-dependent operation glyphs. */
    public static final class Motif extends View {
        private final Palette p;private final String kind;private final Paint brush=new Paint(3);
        public Motif(Context c,Palette p,String kind){super(c);this.p=p;this.kind=kind;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();brush.setColor(p.dark?0xff29477a:0xffdfebff);c.drawCircle(w*.5f,h*.5f,Math.min(w,h)*.38f,brush);c.save();float size=Math.min(w,h)*.64f;c.translate((w-size)/2,(h-size)/2);c.scale(size/24,size/24);Drawable d=symbol(getContext(),kind,p.blue,24);d.setBounds(0,0,24,24);d.draw(c);c.restore();brush.setColor(p.blue);c.drawCircle(w*.84f,h*.25f,3,brush);c.drawCircle(w*.17f,h*.79f,2,brush);}
    }
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public static void add(LinearLayout parent,View child,int height,int bottom){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,height<0?height:dp(parent.getContext(),height));lp.bottomMargin=dp(parent.getContext(),bottom);parent.addView(child,lp);}
    public static void weight(LinearLayout parent,View child,int margin){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(parent.getContext(),margin),0,dp(parent.getContext(),margin),0);parent.addView(child,lp);}
    public static LinearLayout meters(Context c,JSONObject state,Palette p){LinearLayout row=row(c);String[] keys={"satiety","mood","energy"},names={"饱食","心情","精力"};int[] colors={0xffc8a565,0xffc88ca4,0xff759feb};for(int i=0;i<3;i++){
        LinearLayout cell=row(c);cell.setPadding(dp(c,8),dp(c,12),dp(c,8),dp(c,12));
        Meter ring=new Meter(c,state.optDouble(keys[i]),colors[i],p,i);cell.addView(ring,new LinearLayout.LayoutParams(dp(c,34),dp(c,34)));
        LinearLayout labels=column(c);labels.setPadding(dp(c,7),0,0,0);labels.addView(text(c,""+(int)(i==1?Math.round(state.optDouble(keys[i])):Math.floor(state.optDouble(keys[i]))),18,p.ink,true));TextView small=text(c,names[i],10,p.muted,false);small.setPadding(0,dp(c,5),0,0);labels.addView(small);cell.addView(labels);weight(row,cell,3);
    }return row;}
    private static final class Meter extends View{
        final double value;final int color,kind;final Palette palette;final Paint paint=new Paint(3);final Path symbol=new Path();
        Meter(Context c,double v,int color,Palette p,int kind){super(c);value=v;this.color=color;palette=p;this.kind=kind;
            if(kind==1){symbol.moveTo(12,21);symbol.cubicTo(-5,10,2,0,12,7);symbol.cubicTo(22,0,29,10,12,21);}
            if(kind==2){symbol.moveTo(4,19);symbol.cubicTo(-1,7,7,1,22,2);symbol.cubicTo(24,17,13,24,4,19);symbol.moveTo(2,23);symbol.lineTo(16,9);}
        }
        @Override protected void onDraw(Canvas c){float stroke=dp(getContext(),3),r=getWidth()/2f-stroke;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor(palette.soft);c.drawCircle(getWidth()/2f,getHeight()/2f,r,paint);paint.setColor(color);paint.setStrokeCap(Paint.Cap.ROUND);c.drawArc(stroke,stroke,getWidth()-stroke,getHeight()-stroke,-90,(float)(value*3.6),false,paint);
            c.save();float size=dp(getContext(),12);c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/24,size/24);paint.setStrokeWidth(1.7f);paint.setStrokeJoin(Paint.Join.ROUND);if(kind==0){c.drawArc(2,5,22,22,0,180,false,paint);c.drawLine(2,13,22,13,paint);c.drawLine(8,2,8,7,paint);c.drawLine(15,2,15,7,paint);}else c.drawPath(symbol,paint);c.restore();}
    }
    public static String effects(PetCatalog.Action a){StringBuilder text=new StringBuilder();String[] keys={"satiety","mood","energy"},names={"饱食","心情","精力"};for(int i=0;i<3;i++)if(a.effects.has(keys[i])){double v=a.effects.optDouble(keys[i]);if(text.length()>0)text.append(" · ");text.append(names[i]).append(v>0?"+":"").append((int)v);}return text.toString();}
    public static final class Bubble extends View{
        private final Paint paint=new Paint(3);private String text="";private Palette palette;private boolean below;private float tailX=Float.NaN;
        public Bubble(Context c,Palette p){super(c);palette=p;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(LAYER_TYPE_SOFTWARE,null);}
        public void text(String s){text=s;setContentDescription(s);invalidate();}
        public void palette(Palette p){palette=p;invalidate();}
        public void placement(boolean below,float tail){this.below=below;tailX=tail;invalidate();}
        @Override protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight()-7*d;float top=below?7*d:0,pointer=Float.isNaN(tailX)?w/2:Math.max(16*d,Math.min(w-16*d,tailX));c.save();c.translate(0,top);
            paint.setColor(palette.dark?0xff203958:0xfff8fbff);paint.setStyle(Paint.Style.FILL);paint.setShadowLayer(3*d,0,d,0x18224477);c.drawRoundRect(2*d,2*d,w-2*d,h,16*d,16*d,paint);paint.clearShadowLayer();Path tail=new Path();tail.moveTo(pointer-7*d,below?2*d:h-1);tail.lineTo(pointer,below?-7*d:h+7*d);tail.lineTo(pointer+7*d,below?2*d:h-1);c.drawPath(tail,paint);
            paint.setColor(palette.dark?0xffdbeaff:0xff44699d);paint.setTextSize(11*d);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));float max=w-26*d;String first=text;String second="";int count=paint.breakText(text,true,max,null);if(count<text.length()){first=text.substring(0,count);second=text.substring(count);int end=paint.breakText(second,true,max,null);if(end<second.length())second=second.substring(0,Math.max(0,end-1))+"…";}
            float y=second.isEmpty()?h/2+4*d:h/2-3*d;c.drawText(first,13*d,y,paint);if(!second.isEmpty())c.drawText(second,13*d,y+16*d,paint);c.restore();
        }
    }
}
