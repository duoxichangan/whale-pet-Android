package com.whale.pet;

import org.json.*;
import java.util.*;

/** Summaries are derived from retained local events, never lifetime estimates. */
public final class PetJournal {
    public static final class Summary {public int feed,play,actions;public double satiety,mood,energy;public String favoriteId="",favoriteName="";public int favoriteCount;}
    public static Summary today(JSONArray events,long now){Summary s=new Summary();Calendar day=Calendar.getInstance();day.setTimeInMillis(now);day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);Map<String,Integer> counts=new LinkedHashMap<>();
        if(events==null)return s;for(int i=0;i<events.length();i++){JSONObject e=events.optJSONObject(i);if(e==null||e.optLong("at")<day.getTimeInMillis()||e.optLong("at")>now)continue;String kind=e.optString("kind");if(kind.equals("feed"))s.feed++;else if(kind.equals("play"))s.play++;else if(kind.equals("action"))s.actions++;else continue;
            JSONObject effects=e.optJSONObject("appliedEffects");if(effects!=null){s.satiety+=effects.optDouble("satiety",0);s.mood+=effects.optDouble("mood",0);s.energy+=effects.optDouble("energy",0);}String id=e.optString("actionId",e.optString("name",""));int n=counts.getOrDefault(id,0)+1;counts.put(id,n);if(!id.isEmpty()&&n>s.favoriteCount){s.favoriteId=e.optString("actionId","");s.favoriteName=e.optString("name","小小的陪伴");s.favoriteCount=n;}}
        return s;}
    public static final class Entry {public final JSONObject event;public final JSONObject effects=new JSONObject();public int count=0;Entry(JSONObject e){event=e;add(e);}void add(JSONObject e){count++;JSONObject raw=e.optJSONObject("appliedEffects");if(raw!=null)for(String key:new String[]{"satiety","mood","energy"})if(raw.has(key))try{effects.put(key,effects.optDouble(key,0)+raw.optDouble(key));}catch(JSONException ignored){}}}
    public static List<Entry> entries(JSONArray events,boolean modes){List<Entry> rows=new ArrayList<>();if(events==null)return rows;for(int i=events.length()-1;i>=0;i--){JSONObject e=events.optJSONObject(i);if(e==null||(!modes&&e.optString("kind").equals("mode")))continue;Entry last=rows.isEmpty()?null:rows.get(rows.size()-1);boolean same=last!=null&&last.event.optString("kind").equals(e.optString("kind"))&&last.event.optString("actionId",last.event.optString("name",last.event.optString("mode"))).equals(e.optString("actionId",e.optString("name",e.optString("mode"))))&&Math.abs(last.event.optLong("at")-e.optLong("at"))<=120000;
            Calendar a=Calendar.getInstance(),b=Calendar.getInstance();if(last!=null)a.setTimeInMillis(last.event.optLong("at"));b.setTimeInMillis(e.optLong("at"));same=same&&a.get(Calendar.YEAR)==b.get(Calendar.YEAR)&&a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);if(same)last.add(e);else rows.add(new Entry(e));}return rows;}
    public static String effects(JSONObject effects){StringBuilder text=new StringBuilder();String[] keys={"satiety","mood","energy"},labels={"饱食","心情","精力"};for(int i=0;i<keys.length;i++){double n=effects.optDouble(keys[i],0);if(Math.abs(n)<.01)continue;if(text.length()>0)text.append("  ·  ");text.append(labels[i]).append(n>=0?" +":" ").append(Math.round(n));}return text.toString();}
}
