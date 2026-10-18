package com.whale.pet;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import java.util.concurrent.*;

/** Native counterpart to baifanyu's PetView. No browser, JS timer or web compositor. */
public final class PetAnimationView extends View implements Drawable.Callback,Choreographer.FrameCallback {
    public interface Gesture {void down();void dragStart();void drag(float dx,float dy);void drop();void tap();void hold();}
    private static final ExecutorService DECODER=Executors.newFixedThreadPool(2);
    private final Handler ui=new Handler(Looper.getMainLooper());
    private Drawable drawable;private PetCatalog.Clip clip;private boolean enabled=true,attached,mirrored,tracking,dragging,held;
    private float bodyPixels,footX=Float.NaN,footY=Float.NaN;
    public void bodySize(float pixels){bodyPixels=pixels;invalidate();}
    public void anchor(float x,float y){footX=x;footY=y;invalidate();}
    public static RectF envelope(PetCatalog.Clip c,float size){float s=size/216f*c.bodyScale;float radius=Math.max(Math.abs(c.crop.left-c.anchorX),Math.abs(c.crop.right-c.anchorX));return new RectF(-radius*s,(c.crop.top-c.anchorY)*s,radius*s,(c.crop.bottom-c.anchorY)*s);}
    private int generation;private float startX,startY,lastX,lastY;private Gesture gesture;
    public long draws,lastDrawAt;public String currentId="";public boolean decoded;
    private boolean looping;private long lastFrame;
    private final Runnable longPress=()->{if(tracking&&!dragging){held=true;performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);if(gesture!=null)gesture.hold();}};
    public PetAnimationView(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setContentDescription("泡泡桌宠");setClickable(true);}
    public void gesture(Gesture value){gesture=value;}
    public void mirror(boolean value){mirrored=value;invalidate();}
    public static boolean needsMirror(PetCatalog.Clip clip,boolean right){return clip!=null&&(clip.facing.equals("left")?right:clip.facing.equals("right")?!right:false);}
    public boolean isMirrored(){return mirrored;}
    public void faceToward(boolean right){mirror(needsMirror(clip,right));}
    public void show(PetCatalog.Clip next,Runnable loaded){
        if(next==null){loaded.run();return;}
        if(next.id.equals(currentId)&&drawable!=null&&clip!=null&&clip.id.equals(next.id)&&decoded){updatePlayback();loaded.run();return;}
        currentId=next.id;decoded=false;int ticket=++generation;
        DECODER.execute(()->{
            Drawable image=null;
            try{image=ImageDecoder.decodeDrawable(ImageDecoder.createSource(getContext().getAssets(),next.file));}
            catch(Exception error){android.util.Log.e("WhaleNative","Asset decode failed: "+next.id,error);}
            Drawable ready=image;
            ui.post(()->{
                if(ticket!=generation){if(ready instanceof AnimatedImageDrawable)((AnimatedImageDrawable)ready).stop();return;}
                stopDrawable();clip=next;drawable=ready;decoded=ready!=null;
                if(ready!=null){ready.setCallback(this);ready.setBounds(0,0,480,270);if(ready instanceof AnimatedImageDrawable)((AnimatedImageDrawable)ready).setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);}
                updatePlayback();invalidate();loaded.run();
            });
        });
    }
    public void screenActive(boolean value){enabled=value;if(!value){tracking=false;ui.removeCallbacks(longPress);}updatePlayback();if(value){requestLayout();postInvalidateOnAnimation();}}
    private void stopDrawable(){if(drawable instanceof AnimatedImageDrawable)((AnimatedImageDrawable)drawable).stop();if(drawable!=null)drawable.setCallback(null);}
    private boolean canDraw(){return enabled&&attached&&getWindowVisibility()==VISIBLE&&isShown();}
    private void updatePlayback(){boolean run=canDraw();if(drawable instanceof AnimatedImageDrawable){AnimatedImageDrawable a=(AnimatedImageDrawable)drawable;if(run)a.start();else a.stop();}if(run&&!looping){looping=true;Choreographer.getInstance().postFrameCallback(this);}else if(!run){looping=false;Choreographer.getInstance().removeFrameCallback(this);}}
    @Override public void doFrame(long frameTime){if(!canDraw()){looping=false;return;}if(frameTime-lastFrame>=66666666L){lastFrame=frameTime;invalidate();}Choreographer.getInstance().postFrameCallback(this);}
    public boolean animationRunning(){return drawable instanceof AnimatedImageDrawable&&((AnimatedImageDrawable)drawable).isRunning();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;updatePlayback();}
    @Override protected void onDetachedFromWindow(){attached=false;updatePlayback();ui.removeCallbacks(longPress);super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);updatePlayback();if(visibility==VISIBLE)postInvalidateOnAnimation();}
    @Override protected void onVisibilityChanged(View view,int visibility){super.onVisibilityChanged(view,visibility);updatePlayback();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(drawable==null||clip==null)return;
        // One fixed character registration for the entire clip. Props never resize her.
        float scale=(bodyPixels>0?bodyPixels:Math.min(getHeight()*.82f,getWidth()*.66f))/216f*clip.bodyScale;
        float x=Float.isNaN(footX)?getWidth()/2f:footX,y=Float.isNaN(footY)?getHeight()*.88f:footY;
        int checkpoint=canvas.save();canvas.translate(x,y);canvas.scale(mirrored?-scale:scale,scale);
        canvas.translate(-clip.anchorX,-clip.anchorY);drawable.draw(canvas);canvas.restoreToCount(checkpoint);
        draws++;lastDrawAt=SystemClock.elapsedRealtime();
    }
    @Override public void invalidateDrawable(Drawable who){if(enabled&&attached)postInvalidateOnAnimation();}
    @Override public void scheduleDrawable(Drawable who,Runnable what,long when){if(enabled)ui.postAtTime(what,who,when);}
    @Override public void unscheduleDrawable(Drawable who,Runnable what){ui.removeCallbacks(what,who);}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(gesture==null)return super.onTouchEvent(e);float x=e.getRawX(),y=e.getRawY();
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:tracking=true;dragging=held=false;startX=lastX=x;startY=lastY=y;gesture.down();ui.removeCallbacks(longPress);ui.postDelayed(longPress,550);return true;
            case MotionEvent.ACTION_MOVE:if(!held){if(!dragging&&Math.hypot(x-startX,y-startY)>Math.max(ViewConfiguration.get(getContext()).getScaledTouchSlop(),Ui.dp(getContext(),10))){dragging=true;ui.removeCallbacks(longPress);gesture.dragStart();}if(dragging)gesture.drag(x-lastX,y-lastY);lastX=x;lastY=y;}return true;
            case MotionEvent.ACTION_UP:ui.removeCallbacks(longPress);tracking=false;if(!held){if(dragging)gesture.drop();else{performClick();gesture.tap();}}return true;
            case MotionEvent.ACTION_CANCEL:ui.removeCallbacks(longPress);tracking=false;if(!held)gesture.drop();return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
    public void release(){generation++;enabled=false;looping=false;Choreographer.getInstance().removeFrameCallback(this);ui.removeCallbacksAndMessages(null);stopDrawable();drawable=null;}
}
