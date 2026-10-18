package com.whale.pet;

import android.content.*;
import org.json.*;
import java.util.*;
import java.util.function.LongSupplier;

/** Native, serialized state transactions. Reads the exact 1.x SharedPreferences save. */
public final class PetStore {
    public static final String KEY="state:whale-station:pet:v1";
    private static final Object LOCK=new Object();
    public final SharedPreferences prefs;private final LongSupplier clock;
    private static final String[] METERS={"satiety","mood","energy"};
    public PetStore(Context c){this(c.getSharedPreferences("whale",Context.MODE_PRIVATE),System::currentTimeMillis);}
    public PetStore(SharedPreferences prefs,LongSupplier clock){this.prefs=prefs;this.clock=clock;}
    public static double clamp(double x){return Math.max(0,Math.min(100,Double.isFinite(x)?x:0));}
    public static String mode(JSONObject s){String m=s.optString("mode",s.optBoolean("resting")?"resting":"walking");return m.equals("resting")?"resting":"walking";}
    private JSONObject load(long t)throws JSONException{
        String raw=prefs.getString(KEY,null);JSONObject s=null;boolean recovered=false;
        if(raw!=null)try{JSONObject value=new JSONObject(raw);if(value.optInt("schema")==1&&value.has("satiety")&&value.has("energy")&&value.has("mood"))s=value;}catch(JSONException ignored){}
        if(s==null){recovered=true;if(raw!=null)prefs.edit().putString(KEY+":invalid",raw).apply();s=new JSONObject().put("schema",1).put("revision",0).put("outfit","daily").put("satiety",75).put("mood",70).put("energy",80).put("lastUpdatedAt",t).put("satietyUpdatedAt",t);}
        if(s.optJSONArray("events")==null)s.put("events",new JSONArray());
        if(s.optJSONArray("recentCommitIds")==null)s.put("recentCommitIds",new JSONArray());
        // Save defaults once; periodic reads must never restart the hunger clock.
        if(raw==null||recovered)prefs.edit().putString(KEY,s.toString()).apply();
        return s;
    }
    public JSONObject read(){synchronized(LOCK){try{
        long t=clock.getAsLong();JSONObject s=load(t);long last=Math.min(t,Math.max(0,s.optLong("lastUpdatedAt",t)));
        double hours=Math.min(8,Math.max(0,t-last)/3600000d);long satAt=Math.min(t,Math.max(0,s.optLong("satietyUpdatedAt",last))),ticks=(t-satAt)/900000;
        String mode=mode(s);s.put("mode",mode).put("satiety",Math.floor(clamp(s.optDouble("satiety",75)-Math.min(32,ticks))))
            .put("satietyUpdatedAt",satAt+ticks*900000).put("energy",clamp(s.optDouble("energy",80)+(mode.equals("resting")?hours*30:0)))
            .put("mood",clamp(s.optDouble("mood",70)-hours*1.5)).put("lastUpdatedAt",t)
            .put("reading",false).put("resting",mode.equals("resting")).put("roaming",mode.equals("walking"));
        s.put("recovering",s.optDouble("energy")<10);return s;
    }catch(JSONException e){throw new IllegalStateException(e);}}}
    public String refusal(PetCatalog.Action a){JSONObject s=read();if(a.category.equals("feed")&&s.optDouble("satiety")>=100)return "已经吃饱啦，给肚子留一点空闲吧";if(a.category.equals("play")&&(s.optDouble("energy")<10||s.optDouble("energy")< -a.effects.optDouble("energy")))return "有点困啦，先陪我休息一会儿吧";return null;}
    public boolean action(PetCatalog.Action a,String token){synchronized(LOCK){
        if(refusal(a)!=null)return false;
        return change(a.category.equals("feed")?"feed":a.category.equals("play")?"play":"action",a,token,null);
    }}
    public boolean touch(){return change("touch",null,UUID.randomUUID().toString(),null);}
    public boolean walk(){return change("walk",null,UUID.randomUUID().toString(),null);}
    public boolean setMode(String mode){if(!mode.equals("walking")&&!mode.equals("resting"))return false;synchronized(LOCK){if(mode(read()).equals(mode))return false;return change("mode",null,UUID.randomUUID().toString(),mode);}}
    private boolean change(String kind,PetCatalog.Action action,String id,String mode){synchronized(LOCK){try{
        JSONObject s=read();long t=clock.getAsLong();JSONArray ids=s.getJSONArray("recentCommitIds");
        for(int i=0;i<ids.length();i++)if(id.equals(ids.optString(i)))return false;
        JSONObject effects=new JSONObject(),before=new JSONObject();for(String key:METERS)before.put(key,s.optDouble(key));
        if(kind.equals("touch")){if(s.has("lastTouchedAt")&&!s.isNull("lastTouchedAt")&&t-s.optLong("lastTouchedAt")<1500)return false;s.put("mood",clamp(s.optDouble("mood")+1)).put("lastTouchedAt",t);}
        else if(kind.equals("walk")){long last=Math.min(t,Math.max(0,s.optLong("walkEnergyAt",s.optLong("lastWalkedAt",t))));if(t-last>=600000){s.put("energy",clamp(s.optDouble("energy")-1));last=t;}s.put("walkEnergyAt",last).put("lastWalkedAt",t);}
        else if(kind.equals("mode"))s.put("mode",mode);
        else if(action!=null){for(String key:METERS)if(action.effects.has(key))s.put(key,clamp(s.optDouble(key)+action.effects.optDouble(key)));if(kind.equals("feed"))s.put("lastFedAt",t).put("satietyUpdatedAt",t);if(kind.equals("play"))s.put("lastPlayedAt",t);}
        for(String key:METERS)if(s.optDouble(key)!=before.optDouble(key)||(action!=null&&action.effects.has(key)))effects.put(key,s.optDouble(key)-before.optDouble(key));
        s.put("lastUpdatedAt",t).put("revision",s.optLong("revision")+1).put("recovering",s.optDouble("energy")<10).put("reading",false).put("resting",mode(s).equals("resting")).put("roaming",mode(s).equals("walking"));
        if(!kind.equals("touch")&&!kind.equals("walk")){
            JSONObject event=new JSONObject().put("id",id).put("kind",kind).put("at",t).put("appliedEffects",effects);
            if(action!=null)event.put("actionId",action.id).put("name",action.name).put("food",action.name).put("toy",action.name).put("hits",1);
            if(mode!=null)event.put("mode",mode);JSONArray events=s.getJSONArray("events");events.put(event);s.put("events",tail(events,100));
        }
        ids.put(id);s.put("recentCommitIds",tail(ids,100));prefs.edit().putString(KEY,s.toString()).apply();return true;
    }catch(JSONException e){throw new IllegalStateException(e);}}}
    private static JSONArray tail(JSONArray a,int n){JSONArray out=new JSONArray();for(int i=Math.max(0,a.length()-n);i<a.length();i++)out.put(a.opt(i));return out;}
    public String name(){return prefs.getString("name","泡泡");}
    public boolean dark(){return prefs.getBoolean("dark",false);}
}
