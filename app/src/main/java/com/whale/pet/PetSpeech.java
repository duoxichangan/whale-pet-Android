package com.whale.pet;

import java.util.*;
import java.util.function.LongSupplier;
import org.json.JSONObject;

/** Shared home/desktop chatter clock. Automatic care never opens or closes menus. */
public final class PetSpeech {
    private final PetStore store;private final LongSupplier clock;private final Random random;
    public PetSpeech(PetStore store){this(store,System::currentTimeMillis,new Random());}
    public PetSpeech(PetStore store,LongSupplier clock,Random random){this.store=store;this.clock=clock;this.random=random;}
    private static final String[] HUNGRY={"肚子咕噜咕噜，想吃点东西啦","饭点到了吗？一起吃点好吃的吧","小肚子在等投喂，长按看看菜单吧","想和你分享一顿热乎乎的饭"};
    private static final String[] TIRED={"有点困啦，长按陪我休息吧","脚步慢下来啦，想歇一小会儿","电量快用完啦，陪我打个盹吧","想找个舒服的地方，补充一点精力"};
    private static final String[] MOOD={"今天想多靠近你一点","摸摸头，好心情会慢慢回来","陪我玩一会儿，好不好？","想收集一点快乐，和你一起"};
    private static final String[] REST={"慢慢充电中，别着急","把今天的疲惫交给海风吧","眯一小会儿，醒来再陪你","被子盖好啦，你也记得歇歇","安安静静待在你身边，也很好"};
    private static final String[] DAILY={"今天也在认真陪着你","路过你的桌面，留下一点可爱","别忘了喝口水，再伸个懒腰","窗外的风，会不会也带着海的味道","忙你的吧，我就在这里","想把这一点好心情分给你","小尾巴晃一晃，烦恼先放一边","今天有没有遇见开心的小事？","偶尔发发呆，也是一件正经事","看到你，就想悄悄说声嗨"};
    private String pick(String[] values){String last=store.prefs.getString("speech:last","");List<String> choices=new ArrayList<>(Arrays.asList(values));if(choices.size()>1)choices.remove(last);return choices.get(random.nextInt(choices.size()));}
    public void note(String text){long now=clock.getAsLong();store.prefs.edit().putString("speech:last",text).putLong("speech:quiet-until",now+45000+random.nextInt(30001)).apply();}
    public String poll(){long now=clock.getAsLong();long quiet=store.prefs.getLong("speech:quiet-until",0);if(now<quiet&&quiet-now<=75000)return null;JSONObject state=store.read();
        boolean[] needs={state.optDouble("energy")<10&&PetStore.mode(state).equals("walking"),state.optDouble("satiety")<35,state.optDouble("mood")<35};String[] keys={"tired","hungry","mood"};String[][] pools={TIRED,HUNGRY,MOOD};long[] intervals={90000,120000,180000};boolean pending=false;String[] lines=null;
        for(int i=0;i<needs.length;i++)if(needs[i]){pending=true;long last=store.prefs.getLong("speech:care:"+keys[i],0);if(last>0&&now>=last&&now-last<intervals[i])continue;store.prefs.edit().putLong("speech:care:"+keys[i],now).apply();lines=pools[i];break;}
        if(lines==null){if(pending)return null;lines=PetStore.mode(state).equals("resting")?REST:DAILY;}
        String result=pick(lines);note(result);return result;
    }
    public String event(String event){long now=clock.getAsLong();if(event.equals("touch")||event.equals("drop")){long last=store.prefs.getLong("speech:event:"+event,0),gap=event.equals("touch")?12000:20000;if(last>0&&now>=last&&now-last<gap)return null;store.prefs.edit().putLong("speech:event:"+event,now).apply();}
        String[] lines;
        switch(event){case "feed":lines=new String[]{"好好吃，谢谢你记得我的小肚子","这一口，幸福得尾巴都翘起来啦","吃饱一点，继续陪你过小日子"};break;
            case "play":lines=new String[]{"和你一起玩，今天又亮了一点","快乐收到啦，还想陪你玩好久","这一刻的开心，要悄悄收藏起来"};break;
            case "touch":lines=PetStore.mode(store.read()).equals("resting")?new String[]{"唔…知道你在，睡得更安心啦","轻轻摸摸就好，我还想眯一会儿","听见你啦，醒来再陪你逛逛"}:new String[]{"摸摸头，开心就冒泡啦","被你发现啦，我一直都在","嘿嘿，今天也喜欢和你待在一起","再靠近一点点，好不好？"};break;
            case "drop":lines=new String[]{"落地成功，这里风景也不错","新位置收到，我就在这里陪你","小尾巴站稳啦，继续陪伴"};break;
            case "action":lines=new String[]{"给你表演一个小小的拿手动作","这一点可爱，专门留给你","看我看我，小表演开始啦"};break;
            default:return null;}
        String result=pick(lines);note(result);return result;
    }
}
