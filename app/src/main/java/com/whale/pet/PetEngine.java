package com.whale.pet;

import android.os.*;
import java.util.*;

/** Native scheduler; screen-off pauses work without destroying its View or service. */
public final class PetEngine {
    public interface Host {
        void show(String id,Runnable loaded);void active(boolean value);void message(String text);void changed();
        boolean move(boolean right,int duration);void stopMotion();
    }
    private final PetStore store;private final PetCatalog catalog;private final Host host;
    private final PetCatalog.Bag clicks,restClicks;
    private final Handler handler=new Handler(Looper.getMainLooper());
    public boolean active,menu,busy,manual;private int revision;private final Random random=new Random();private final PetSpeech speech;
    public PetEngine(PetStore s,PetCatalog c,Host h){store=s;catalog=c;host=h;speech=new PetSpeech(s);clicks=new PetCatalog.Bag(c.clicks);restClicks=new PetCatalog.Bag(c.restClicks);}
    public void clear(){revision++;handler.removeCallbacksAndMessages(null);busy=manual=false;host.stopMotion();}
    public void resume(){if(active)return;active=true;host.active(true);idle();schedule(15000);}
    public void pause(){active=false;clear();host.active(false);}
    public void idle(){host.show(PetStore.mode(store.read()).equals("resting")?"rest":"idle",()->{});host.changed();}
    private void schedule(long delay){if(active&&!menu)handler.postDelayed(this::autonomous,delay);}
    public void menu(boolean value){menu=value;clear();if(active){idle();if(!value)schedule(15000);}}
    public void mode(String mode){store.setMode(mode);clear();if(active){idle();schedule(2500);}host.changed();}
    public void touch(){if(!active)return;if(busy&&manual){host.message("正在回应你的心意，稍等一下下");return;}if(store.touch()){perform(PetStore.mode(store.read()).equals("resting")?restClicks.next():clicks.next(),true,false,false);sayEvent("touch");host.changed();}}
    public void drag(){clear();if(active)host.show("action_097",()->{});}
    public void drop(){clear();if(active){idle();schedule(15000);}}
    public boolean choose(PetCatalog.Action a,boolean confirmed,String token){
        if(!active||a==null||(a.transaction()&&!confirmed))return false;
        String refused=store.refusal(a);if(refused!=null){host.message(refused);return false;}
        if(!store.action(a,token)){host.message("这份心意已经收到了");return false;}
        perform(a.id,true,catalog.walks.contains(a.id)&&PetStore.mode(store.read()).equals("walking")&&store.read().optDouble("energy")>=10);sayEvent(a.category.equals("feed")?"feed":a.category.equals("play")?"play":"action");host.changed();return true;
    }
    public void autonomous(){
        if(!active||menu||busy)return;
        boolean resting=PetStore.mode(store.read()).equals("resting");
        boolean canWalk=!resting&&store.read().optDouble("energy")>=10&&store.prefs.getBoolean("roam",true);
        String id=sampleSpontaneous(catalog,resting,canWalk,random);perform(id,false,catalog.walks.contains(id));
    }
    public static String sampleSpontaneous(PetCatalog c,boolean resting,boolean canWalk,Random random){
        if(resting)return c.rest.get(random.nextInt(c.rest.size()));
        if(canWalk&&random.nextDouble()<.60)return "run";
        List<String> choices=new ArrayList<>(c.ambient);if(canWalk){for(String id:c.walks)if(!id.equals("run"))choices.add(id);}
        return choices.get(random.nextInt(choices.size()));
    }
    public void chatTick(){if(active&&!menu&&!manual){String text=speech.poll();if(text!=null)host.message(text);}}
    public void noteSpeech(String text){speech.note(text);}
    public void sayEvent(String event){String text=speech.event(event);if(text!=null)host.message(text);}
    private void perform(String id,boolean user,boolean walk){
        perform(id,user,walk,user);
    }
    private void perform(String id,boolean user,boolean walk,boolean protect){
        clear();if(!active||id==null)return;busy=true;manual=protect;int ticket=revision;
        host.show(id,()->{
            if(ticket!=revision||!active)return;
            boolean moved=walk&&host.move(new Random().nextBoolean(),catalog.clips.get(id).duration);
            handler.postDelayed(()->{if(ticket!=revision||!active)return;if(moved)store.walk();busy=manual=false;idle();schedule(user?15000:9000+new Random().nextInt(9000));},catalog.clips.get(id).duration);
        });
    }
    public void dispose(){pause();}
}
