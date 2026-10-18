package com.whale.pet;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Native small-window service, following baifanyu's retained-View/screen lifecycle. */
public final class PetService extends Service implements PetEngine.Host,PetAnimationView.Gesture {
    static final String STATUS_CHANNEL="companion_status_v2";
    public static volatile PetService instance;
    private PetStore store;private PetCatalog catalog;private PetEngine engine;
    private WindowManager wm;private WindowManager.LayoutParams lp,menuLp;
    private android.widget.FrameLayout root;private PetAnimationView pet;private Ui.Bubble bubble;private View menu;
    private final Handler handler=new Handler(Looper.getMainLooper());private ValueAnimator motion;
    private boolean registered,screenActive,dragging,bubbleBelow;private float worldX,worldY;private boolean positionReady;private PetCatalog.Clip shownClip;private String bubbleText="";private int motionTarget;
    private final String session=UUID.randomUUID().toString();private long taps,holds,sleeps;
    private final Runnable careTick=new Runnable(){@Override public void run(){if(screenActive){reminders();snapshot();handler.postDelayed(this,5000);}}};
    private final Runnable clearBubble=()->{bubbleText="";layoutPet(false);};
    private final BroadcastReceiver screen=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){
        if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){sleeps++;handler.removeCallbacks(careTick);hideMenu();pauseForScreen();}
        else {updateVisibility();for(long delay:new long[]{120,450,1000,2500})handler.postDelayed(()->{updateVisibility();snapshot();},delay);}
        snapshot();
    }};
    private int dp(float n){return Ui.dp(this,n);}
    private int width(){return getResources().getDisplayMetrics().widthPixels;}
    private int height(){return getResources().getDisplayMetrics().heightPixels;}
    private int petSize(){return dp(store.prefs.getInt("size",168));}
    private void savePosition(){if(positionReady&&lp!=null)store.prefs.edit().putFloat("anchor-x",worldX/Math.max(1,width())).putFloat("anchor-y",worldY/Math.max(1,height())).apply();}
    private void restorePosition(){if(!positionReady){int size=petSize();worldX=store.prefs.contains("anchor-x")?store.prefs.getFloat("anchor-x",.8f)*width():store.prefs.getFloat("x",.8f)*(width()-size)+size/2f;worldY=store.prefs.contains("anchor-y")?store.prefs.getFloat("anchor-y",.8f)*height():store.prefs.getFloat("y",.72f)*(height()-size-dp(30))+size;positionReady=true;}worldX=Math.max(petSize()*.4f,Math.min(width()-petSize()*.4f,worldX));worldY=Math.max(petSize()+dp(28),Math.min(height()-dp(30),worldY));}
    @Override public void onCreate(){
        super.onCreate();instance=this;store=new PetStore(this);catalog=new PetCatalog(this);wm=getSystemService(WindowManager.class);engine=new PetEngine(store,catalog,this);
        // A new silent channel also migrates installs whose old channel could alert.
        NotificationChannel status=new NotificationChannel(STATUS_CHANNEL,"陪伴状态",NotificationManager.IMPORTANCE_LOW);status.setDescription("静默显示桌面陪伴状态");status.setSound(null,null);status.enableVibration(false);status.enableLights(false);status.setShowBadge(false);
        getSystemService(NotificationManager.class).createNotificationChannel(status);
        try{if(Build.VERSION.SDK_INT>=34)startForeground(7,notification(),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(7,notification());}catch(Exception e){android.util.Log.e("WhaleNative","Foreground service failed",e);stopSelf();return;}
        if(!Settings.canDrawOverlays(this)||!store.prefs.getBoolean("enabled",false)){stopSelf();return;}
        createPet();layoutPet(false);IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_USER_PRESENT);registerReceiver(screen,f);registered=true;updateVisibility();
    }
    private void createPet(){
        // As in baifanyu: a compact NOT_FOCUSABLE native window, retained through sleep.
        pet=new PetAnimationView(this);pet.gesture(this);pet.setContentDescription("桌面宠物");bubble=new Ui.Bubble(this,new Ui.Palette(store.dark()));
        root=new FrameLayout(this){@Override public boolean dispatchTouchEvent(MotionEvent e){return pet.dispatchTouchEvent(e);}};
        root.addView(pet,new FrameLayout.LayoutParams(-1,petSize(),Gravity.BOTTOM));root.addView(bubble,new FrameLayout.LayoutParams(-1,dp(52),Gravity.TOP));bubble.setVisibility(View.GONE);
        lp=new WindowManager.LayoutParams(petSize(),petSize(),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.LEFT;lp.setTitle("泡泡鲸·桌面宠物");restorePosition();
        try{wm.addView(root,lp);}catch(Exception e){android.util.Log.e("WhaleNative","Cannot attach overlay",e);pet.release();pet=null;root=null;store.prefs.edit().putBoolean("enabled",false).apply();stopSelf();}
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&"STOP".equals(intent.getAction())){stopPet();return START_NOT_STICKY;}
        if(!store.prefs.getBoolean("enabled",false)){stopSelf();return START_NOT_STICKY;}
        if(intent!=null&&"RECOVER".equals(intent.getAction())&&pet!=null){pet.screenActive(false);pet.screenActive(true);root.requestLayout();root.postInvalidateOnAnimation();}
        updateVisibility();return START_STICKY;
    }
    public void updateVisibility(){
        if(root==null)return;if(!Settings.canDrawOverlays(this)){stopPet();return;}
        boolean interactive=getSystemService(PowerManager.class).isInteractive(),locked=getSystemService(KeyguardManager.class).isKeyguardLocked();
        // Do not set INVISIBLE, remove, or recreate the pet when the screen locks.
        // System window visibility and explicit render pause are separate concerns.
        root.setVisibility(MainActivity.visible?View.GONE:View.VISIBLE);
        boolean run=interactive&&!locked&&!MainActivity.visible;
        if(run&&!screenActive){screenActive=true;engine.resume();root.requestLayout();root.postInvalidateOnAnimation();handler.removeCallbacks(careTick);handler.postDelayed(careTick,500);}
        else if(!run)pauseForScreen();
        else if(run){pet.screenActive(true);root.postInvalidateOnAnimation();}
    }
    private void pauseForScreen(){screenActive=false;if(engine!=null)engine.pause();stopMotion();handler.removeCallbacks(careTick);handler.removeCallbacks(clearBubble);if(!bubbleText.isEmpty()){bubbleText="";layoutPet(false);}snapshot();}
    public void applySettings(){if(pet==null)return;bubble.palette(new Ui.Palette(store.dark()));stopMotion();savePosition();layoutPet(false);getSystemService(NotificationManager.class).notify(7,notification());changed();}
    private void layoutPet(boolean persist){
        if(root==null||lp==null)return;if(persist)savePosition();restorePosition();
        PetCatalog.Clip clip=shownClip==null?catalog.clips.get("idle"):shownClip;
        RectF envelope=PetAnimationView.envelope(clip,petSize());int speechHeight=bubbleText.isEmpty()?0:dp(52);
        // Intersect the complete action envelope with the display; body anchor does not change.
        int left=Math.max(0,(int)Math.floor(worldX+envelope.left)-dp(4));
        int right=Math.min(width(),(int)Math.ceil(worldX+envelope.right)+dp(4));
        if(speechHeight>0){left=Math.min(left,Math.max(0,(int)worldX-dp(110)));right=Math.max(right,Math.min(width(),(int)worldX+dp(110)));}
        int top=Math.max(dp(28),(int)Math.floor(worldY+envelope.top)-dp(4));
        int bottom=Math.min(height()-dp(24),(int)Math.ceil(worldY+envelope.bottom)+dp(4));
        bubbleBelow=top-speechHeight<dp(28);int imageTop=top;
        if(speechHeight>0){if(bubbleBelow)bottom=Math.min(height()-dp(24),bottom+speechHeight);else top-=speechHeight;}
        lp.x=left;lp.y=top;lp.width=Math.max(dp(80),right-left);lp.height=Math.max(dp(80),bottom-top);
        FrameLayout.LayoutParams image=new FrameLayout.LayoutParams(-1,-1);pet.setLayoutParams(image);pet.bodySize(petSize());pet.anchor(worldX-left,worldY-top);
        int bw=Math.min(dp(220),lp.width);FrameLayout.LayoutParams speech=new FrameLayout.LayoutParams(bw,dp(52),(bubbleBelow?Gravity.BOTTOM:Gravity.TOP)|Gravity.LEFT);speech.leftMargin=Math.max(0,Math.min(lp.width-bw,Math.round(worldX-left-bw/2f)));bubble.placement(bubbleBelow,worldX-left-speech.leftMargin);bubble.setLayoutParams(speech);bubble.setVisibility(bubbleText.isEmpty()?View.GONE:View.VISIBLE);bubble.text(bubbleText);
        try{wm.updateViewLayout(root,lp);}catch(IllegalArgumentException ignored){}
    }
    private void reminders(){if(menu==null&&!dragging)engine.chatTick();}
    @Override public void message(String text){if(!screenActive)return;engine.noteSpeech(text);hideMenu();savePosition();bubbleText=text;handler.removeCallbacks(clearBubble);layoutPet(false);handler.postDelayed(clearBubble,7000);snapshot();}
    @Override public void show(String id,Runnable loaded){if(pet!=null){pet.mirror(false);pet.show(catalog.clips.get(id),()->{shownClip=catalog.clips.get(id);layoutPet(false);loaded.run();});}else loaded.run();}
    @Override public void active(boolean value){if(pet!=null)pet.screenActive(value);}
    @Override public void changed(){snapshot();}
    @Override public boolean move(boolean right,int duration){
        if(pet==null||menu!=null||dragging||!screenActive||!store.prefs.getBoolean("roam",true))return false;stopMotion();int start=Math.round(worldX),min=Math.round(-PetAnimationView.envelope(shownClip==null?catalog.clips.get("idle"):shownClip,petSize()).left),max=width()-min,end=right?max:min;if(Math.abs(end-start)<dp(10))end=end==min?max:min;pet.faceToward(end>start);motionTarget=end;
        ValueAnimator animator=ValueAnimator.ofInt(start,end);motion=animator;animator.setDuration(duration);animator.setInterpolator(new android.view.animation.LinearInterpolator());animator.addUpdateListener(a->{worldX=(int)a.getAnimatedValue();layoutPet(false);});animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){savePosition();if(motion==a)motion=null;}});animator.start();return true;
    }
    @Override public void stopMotion(){if(motion!=null){ValueAnimator old=motion;motion=null;old.cancel();}if(store!=null)savePosition();}
    @Override public void down(){stopMotion();}
    @Override public void dragStart(){dragging=true;handler.removeCallbacks(clearBubble);savePosition();bubbleText="";layoutPet(false);engine.drag();}
    @Override public void drag(float dx,float dy){worldX+=dx;worldY+=dy;layoutPet(false);}
    @Override public void drop(){dragging=false;savePosition();engine.drop();engine.sayEvent("drop");snapshot();}
    @Override public void tap(){taps++;engine.touch();snapshot();}
    @Override public void hold(){holds++;showMenu();snapshot();}
    private void showMenu(){
        if(!screenActive||menu!=null)return;engine.menu(true);Context c=this;Ui.Palette p=new Ui.Palette(store.dark());LinearLayout box=Ui.column(c);box.setPadding(dp(5),dp(10),dp(5),dp(5));box.setBackground(Ui.round(p.bg,p.line,24,c));
        LinearLayout heading=Ui.row(c);heading.setTag("menu-heading");TextView title=Ui.text(c,"陪伴时光",16,p.ink,true);title.setPadding(dp(15),0,dp(8),0);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));heading.addView(Ui.iconButton(c,"close","关闭菜单",p,this::dismissMenu),new LinearLayout.LayoutParams(dp(44),dp(44)));Ui.add(box,heading,-2,4);
        PetPanel panel=new PetPanel(c,store,catalog,new PetPanel.Host(){
            @Override public void choose(PetCatalog.Action a,String token){hideMenu();engine.choose(a,true,token);}
            @Override public void mode(String mode){engine.mode(mode);}
            @Override public void message(String message){PetService.this.message(message);}
            @Override public void confirmation(boolean visible){resizeMenu(visible);}
        },true);panel.setTag("actions-panel");box.addView(panel,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=Ui.row(c);footer.setTag("menu-footer");Ui.weight(footer,Ui.labeled(c,"回到小屋","home",p,PetService.this::openApp),3);Ui.weight(footer,Ui.labeled(c,"待会儿见","moon",p,PetService.this::stopPet),3);Ui.add(box,footer,-2,4);
        menuLp=new WindowManager.LayoutParams(Math.min(dp(352),width()-dp(24)),Math.min(dp(570),height()-dp(100)),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);menuLp.gravity=Gravity.CENTER;menuLp.setTitle("泡泡鲸·互动菜单");menu=box;
        try{wm.addView(menu,menuLp);Ui.enter(menu);}catch(Exception e){menu=null;engine.menu(false);}
    }
    private void resizeMenu(boolean confirmation){
        if(menu==null||menuLp==null)return;View panel=menu.findViewWithTag("actions-panel");if(panel==null)return;
        for(String tag:new String[]{"menu-heading","menu-footer"}){View chrome=menu.findViewWithTag(tag);if(chrome!=null)chrome.setVisibility(confirmation?View.GONE:View.VISIBLE);}
        panel.setLayoutParams(new LinearLayout.LayoutParams(-1,confirmation?-2:0,confirmation?0:1));
        menuLp.height=confirmation?WindowManager.LayoutParams.WRAP_CONTENT:Math.min(dp(570),height()-dp(100));
        try{wm.updateViewLayout(menu,menuLp);}catch(IllegalArgumentException ignored){}
    }
    private void dismissMenu(){if(menu==null)return;if(!ValueAnimator.areAnimatorsEnabled()){hideMenu();return;}menu.animate().alpha(0).scaleX(.98f).scaleY(.98f).setDuration(120).withEndAction(this::hideMenu).start();}
    private void hideMenu(){if(menu!=null){try{wm.removeView(menu);}catch(IllegalArgumentException ignored){}menu=null;if(engine!=null&&engine.active)engine.menu(false);}}
    private void openApp(){hideMenu();startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
    public void stopPet(){store.prefs.edit().putBoolean("enabled",false).apply();stopSelf();}
    public JSONObject diagnostics(){JSONObject d=new JSONObject();try{d.put("session",session).put("window",root==null?0:System.identityHashCode(root)).put("screenActive",screenActive).put("attached",root!=null&&root.isAttachedToWindow()).put("draws",pet==null?0:pet.draws).put("lastDrawAt",pet==null?0:pet.lastDrawAt).put("animationRunning",pet!=null&&pet.animationRunning()).put("loaded",pet!=null&&pet.decoded).put("clip",pet==null?"":pet.currentId).put("taps",taps).put("holds",holds).put("sleeps",sleeps).put("menu",menu!=null).put("mirrored",pet!=null&&pet.isMirrored()).put("motionTarget",motionTarget).put("bubble",bubbleText).put("anchorX",worldX).put("anchorY",worldY).put("x",lp==null?0:lp.x).put("y",lp==null?0:lp.y).put("at",SystemClock.elapsedRealtime());}catch(JSONException ignored){}return d;}
    void snapshot(){if(store!=null)store.prefs.edit().putString("native-diagnostics",diagnostics().toString()).apply();}
    private Notification notification(){
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent recover=PendingIntent.getService(this,2,new Intent(this,PetService.class).setAction("RECOVER"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT),stop=PendingIntent.getService(this,1,new Intent(this,PetService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,STATUS_CHANNEL).setOnlyAlertOnce(true).setSound(null).setVibrate(null).setDefaults(0).setCategory(Notification.CATEGORY_SERVICE).setSmallIcon(R.drawable.ic_bubble_notification).setContentTitle(store.name()+"正在陪伴你").setContentText("轻触回应 · 长按打开陪伴时光 · 点击回到小屋").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"回来陪我",recover).build()).addAction(new Notification.Action.Builder(null,"待会儿见",stop).build()).build();
    }
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);hideMenu();applySettings();}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);hideMenu();if(engine!=null)engine.dispose();if(pet!=null){savePosition();pet.release();try{wm.removeViewImmediate(root);}catch(IllegalArgumentException ignored){}}if(registered)unregisterReceiver(screen);instance=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
