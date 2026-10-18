package com.whale.pet;

import android.content.Context;
import android.graphics.Rect;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The user's original manifest and action policy are the only source of actions. */
public final class PetCatalog {
    public static final class Clip {
        public final String id,file,facing; public final int duration; public final Rect crop; public final float anchorX,anchorY,bodyScale;
        Clip(String id,JSONObject data,JSONArray bounds,JSONObject registration) throws JSONException {
            facing=registration.optString("facing","front");anchorX=(float)registration.getJSONArray("anchor").getDouble(0);anchorY=(float)registration.getJSONArray("anchor").getDouble(1);bodyScale=(float)registration.getDouble("bodyScale");
            this.id=id;file="pet/"+data.getString("file");duration=data.getInt("duration");
            crop=bounds==null?new Rect(128,24,352,254):new Rect(bounds.getInt(0),bounds.getInt(1),bounds.getInt(2),bounds.getInt(3));
        }
    }
    public static final class Action {
        public final String id,name,category; public final JSONObject effects;
        Action(String id,JSONObject data){this.id=id;name=data.optString("name",id);category=data.optString("category","other");effects=data.optJSONObject("effects")==null?new JSONObject():data.optJSONObject("effects");}
        public boolean transaction(){return category.equals("feed")||category.equals("play");}
    }
    public final Map<String,Clip> clips=new LinkedHashMap<>();
    public final Map<String,Action> actions=new LinkedHashMap<>();
    public final List<String> ambient,rest,clicks,restClicks,walks;
    private final Set<String> dailyIds=new LinkedHashSet<>(),restIds=new LinkedHashSet<>(),dragIds=new LinkedHashSet<>();
    public PetCatalog(Context c){
        try{
            JSONObject manifest=read(c,"pet/manifest.json"),policy=read(c,"pet/action-policy.json"),bounds=read(c,"native-clip-bounds.json"),registration=read(c,"native-clip-registration.json"),behavior=read(c,"native-action-behavior.json");
            JSONObject raw=manifest.getJSONObject("clips");Iterator<String> keys=raw.keys();
            while(keys.hasNext()){String id=keys.next();clips.put(id,new Clip(id,raw.getJSONObject(id),bounds.optJSONArray(id),registration.getJSONObject(id)));}
            JSONArray ordered=manifest.getJSONArray("actions");JSONObject entries=policy.getJSONObject("actions");
            for(int i=0;i<ordered.length();i++){String id=ordered.getJSONObject(i).getString("id");if(entries.has(id))actions.put(id,new Action(id,entries.getJSONObject(id)));}
            ambient=list(behavior.getJSONArray("dailyAmbient"));rest=list(behavior.getJSONArray("restAmbient"));
            clicks=list(behavior.getJSONArray("dailyTouch"));restClicks=list(behavior.getJSONArray("restTouch"));walks=list(behavior.getJSONArray("walking"));
            dragIds.addAll(list(behavior.getJSONArray("drag")));dailyIds.addAll(ambient);dailyIds.addAll(clicks);dailyIds.addAll(walks);dailyIds.addAll(dragIds);dailyIds.addAll(list(behavior.getJSONArray("base")));restIds.addAll(rest);restIds.addAll(restClicks);
            for(Action a:actions.values())if(a.transaction()?(dailyIds.contains(a.id)||restIds.contains(a.id)):(!dailyIds.contains(a.id)&&!restIds.contains(a.id)))throw new IllegalStateException("Incomplete behavior assignment: "+a.id);
        }catch(Exception e){throw new IllegalStateException("Local pet catalog is invalid",e);}
    }
    private static JSONObject read(Context c,String path)throws Exception{try(InputStream in=c.getAssets().open(path)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return new JSONObject(out.toString("UTF-8"));}}
    private static List<String> list(JSONArray a)throws JSONException{List<String> out=new ArrayList<>();for(int i=0;i<a.length();i++)out.add(a.getString(i));return Collections.unmodifiableList(out);}
    public List<Action> category(String category){List<Action> out=new ArrayList<>();for(Action a:actions.values())if(a.category.equals(category))out.add(a);return out;}
    /** All 61 non-transaction clips remain discoverable, including triggered responses. */
    public List<Action> spontaneous(String filter){List<Action> result=new ArrayList<>();for(Action a:actions.values())if(!a.transaction()&&(filter.equals("all")||(filter.equals("walking")?dailyIds:restIds).contains(a.id)))result.add(a);return result;}
    public String behaviorLabel(Action a){
        if(dragIds.contains(a.id))return "日常 · 拖动时回应";
        if(a.id.equals("idle"))return "日常 / 休息 · 安静陪伴";
        if(restClicks.contains(a.id)&&!rest.contains(a.id))return "休息 · 轻触时回应";
        if(clicks.contains(a.id))return "日常 · 轻触时回应";
        if(walks.contains(a.id))return "日常 · 精力充足时自在走动";
        if(dailyIds.contains(a.id)&&restIds.contains(a.id))return "日常 / 休息 · 自发小动作";
        return restIds.contains(a.id)?"休息 · 自发小动作":"日常 · 自发小动作";
    }
    public static final class Bag {
        private final List<String> source,bag=new ArrayList<>();private String last;
        public Bag(List<String> values){source=new ArrayList<>(new LinkedHashSet<>(values));}
        public String next(){if(source.isEmpty())return null;if(bag.isEmpty()){bag.addAll(source);Collections.shuffle(bag);if(bag.size()>1&&Objects.equals(bag.get(bag.size()-1),last))Collections.swap(bag,0,bag.size()-1);}return last=bag.remove(bag.size()-1);}
    }
}
