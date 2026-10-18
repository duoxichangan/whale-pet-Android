package com.whale.pet;

import android.content.*;
import org.json.*;

/** Emulator test hook, excluded from release and restricted to shell/system DUMP permission. */
public final class DebugReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if((c.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
        PetStore store=new PetStore(c);String command=intent.getStringExtra("command");
        if("seed".equals(command))try{JSONObject state=store.read();for(String key:new String[]{"satiety","mood","energy"})if(intent.hasExtra(key))state.put(key,intent.getIntExtra(key,80));if(intent.hasExtra("mode"))state.put("mode",intent.getStringExtra("mode"));state.put("lastUpdatedAt",System.currentTimeMillis()).put("satietyUpdatedAt",System.currentTimeMillis());store.prefs.edit().putString(PetStore.KEY,state.toString()).remove("native-reminder:tired").remove("native-reminder:hungry").apply();}catch(JSONException e){throw new IllegalStateException(e);}
        if("settings".equals(command)){SharedPreferences.Editor edit=store.prefs.edit();for(String key:new String[]{"dark","roam"})if(intent.hasExtra(key))edit.putBoolean(key,intent.getBooleanExtra(key,false));if(intent.hasExtra("name"))edit.putString("name",intent.getStringExtra("name"));edit.apply();if(PetService.instance!=null)PetService.instance.applySettings();}
        if(PetService.instance!=null){
            if("preview".equals(command)||"resume".equals(command)){try{java.lang.reflect.Field f=PetService.class.getDeclaredField("engine");f.setAccessible(true);((PetEngine)f.get(PetService.instance)).menu("preview".equals(command));if("preview".equals(command))PetService.instance.show(intent.getStringExtra("clip"),()->PetService.instance.snapshot());}catch(Exception e){throw new IllegalStateException(e);}}
            if("motion".equals(command)||"chat".equals(command)){try{java.lang.reflect.Field f=PetService.class.getDeclaredField("engine");f.setAccessible(true);PetEngine engine=(PetEngine)f.get(PetService.instance);if("motion".equals(command)){engine.menu(true);PetService.instance.show(intent.getStringExtra("clip"),()->{PetService.instance.move(intent.getBooleanExtra("right",true),4000);PetService.instance.snapshot();});}else{store.prefs.edit().remove("speech:quiet-until").remove("speech:care:tired").remove("speech:care:hungry").remove("speech:care:mood").apply();engine.chatTick();}}catch(Exception e){throw new IllegalStateException(e);}}
            PetService.instance.snapshot();
            try{java.lang.reflect.Field field=PetService.class.getDeclaredField("menu");field.setAccessible(true);android.view.View menu=(android.view.View)field.get(PetService.instance);JSONArray nodes=new JSONArray();if(menu!=null)collect(menu,nodes);store.prefs.edit().putString("debug-native-ui",nodes.toString()).apply();}catch(Exception e){throw new IllegalStateException(e);}
        }
    }
    private void collect(android.view.View v,JSONArray nodes)throws JSONException{
        android.graphics.Rect visible=new android.graphics.Rect();if(!v.isShown()||!v.getLocalVisibleRect(visible))return;
        if(v instanceof android.widget.TextView||v.getContentDescription()!=null){int[] xy=new int[2];v.getLocationOnScreen(xy);nodes.put(new JSONObject().put("text",v instanceof android.widget.TextView?((android.widget.TextView)v).getText().toString():v.getContentDescription().toString()).put("bounds","["+(xy[0]+visible.left)+","+(xy[1]+visible.top)+"]["+(xy[0]+visible.right)+","+(xy[1]+visible.bottom)+"]"));}
        if(v instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)v;for(int i=0;i<group.getChildCount();i++)collect(group.getChildAt(i),nodes);}
    }
}
