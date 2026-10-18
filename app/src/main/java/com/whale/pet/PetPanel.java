package com.whale.pet;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Fixed controls and an independently scrolling editorial action list. */
public final class PetPanel extends LinearLayout {
    public interface Host {void choose(PetCatalog.Action a,String token);void mode(String value);void message(String text);default void confirmation(boolean visible){} }
    private final PetStore store;private final PetCatalog catalog;private final Host host;private final Ui.Palette p;private final boolean compact;
    private String category="feed",autoFilter="all";private boolean extras;
    private int dp(float n){return Ui.dp(getContext(),n);}
    public PetPanel(Context c,PetStore store,PetCatalog catalog,Host host,boolean compact){super(c);this.store=store;this.catalog=catalog;this.host=host;this.compact=compact;p=new Ui.Palette(store.dark());setOrientation(VERTICAL);setPadding(dp(compact?16:0),0,dp(compact?16:0),0);render();}
    private void render(){
        Context c=getContext();host.confirmation(false);removeAllViews();Ui.add(this,Ui.meters(c,store.read(),p),-2,8);
        Ui.add(this,Ui.modes(c,store,p,key->{host.mode(key);render();}),-2,12);
        LinearLayout primary=Ui.row(c);entry(primary,"feed","一起吃东西","给今天加点甜","bowl");entry(primary,"play","一起玩乐","把快乐分给你","kite");Ui.add(this,primary,compact?66:96,6);
        LinearLayout toolbar=Ui.row(c);toolbar.setPadding(0,dp(4),0,dp(4));
        TextView fold=Ui.text(c,"自发动作",12,extras?p.blue:p.ink,true);fold.setGravity(Gravity.CENTER);fold.setPadding(dp(8),0,dp(8),0);fold.setCompoundDrawables(null,null,Ui.symbol(c,extras?"up":"down",p.blue,12),null);fold.setCompoundDrawablePadding(dp(4));fold.setBackground(Ui.ripple(c,Color.TRANSPARENT,p.blue,12));fold.setOnClickListener(v->{extras=!extras;category=extras?"auto":"feed";render();});toolbar.addView(fold,new LayoutParams(-2,dp(40)));
        View space=new View(c);toolbar.addView(space,new LayoutParams(0,1,1));
        if(extras){String[] keys={"all","walking","resting"};int selection=Arrays.asList(keys).indexOf(autoFilter);toolbar.addView(Ui.filters(c,new String[]{"全部","日常","休息"},selection,p,index->{autoFilter=keys[index];category="auto";render();}));}
        else {TextView hint=Ui.text(c,"偶尔的小惊喜",10,p.muted,false);hint.setPadding(0,0,dp(8),0);toolbar.addView(hint);}
        Ui.add(this,toolbar,48,4);
        List<PetCatalog.Action> shown=category.equals("auto")?catalog.spontaneous(autoFilter):catalog.category(category);
        Ui.rule(this,p);LinearLayout count=Ui.row(c);TextView heading=Ui.text(c,category.equals("feed")?"今日小食单":category.equals("play")?"快乐游乐场":autoFilter.equals("all")?"全部小动作":autoFilter.equals("walking")?"日常的小动作":"休息的小动作",12,p.ink,true);count.addView(heading,new LayoutParams(0,-2,1));count.addView(Ui.text(c,String.format(Locale.ROOT,"%02d 个小瞬间",shown.size()),10,p.muted,false));count.setPadding(dp(2),dp(12),dp(2),dp(8));Ui.add(this,count,-2,0);
        ScrollView scroll=new ScrollView(c);scroll.setVerticalScrollBarEnabled(false);scroll.setClipToPadding(true);scroll.setVerticalFadingEdgeEnabled(true);scroll.setFadingEdgeLength(dp(12));LinearLayout list=Ui.column(c);list.setPadding(0,0,0,dp(16));scroll.addView(list);addView(scroll,new LayoutParams(-1,0,1));
        for(PetCatalog.Action a:shown){LinearLayout row=Ui.row(c);row.setPadding(dp(2),dp(11),dp(4),dp(11));row.setMinimumHeight(dp(68));ImageView art=new ImageView(c);art.setImageResource(c.getResources().getIdentifier("pet_"+a.id,"drawable",c.getPackageName()));art.setColorFilter(p.blue);row.addView(art,new LayoutParams(dp(27),dp(27)));LinearLayout words=Ui.column(c);words.setPadding(dp(14),0,dp(8),0);TextView name=Ui.text(c,a.name,13,p.ink,true);name.setMaxLines(2);words.addView(name);TextView sub=Ui.text(c,a.transaction()?Ui.effects(a):catalog.behaviorLabel(a),10,p.muted,false);sub.setPadding(0,dp(6),0,0);words.addView(sub);row.addView(words,new LayoutParams(0,-2,1));ImageView next=Ui.icon(c,"chevron",p.muted);row.addView(next,new LayoutParams(dp(16),dp(16)));row.setBackground(Ui.ripple(c,Color.TRANSPARENT,p.blue,12));row.setOnClickListener(v->request(a));Ui.press(row);Ui.add(list,row,-2,0);Ui.rule(list,p);}Ui.enter(list);
    }
    private void entry(LinearLayout parent,String key,String label,String note,String icon){Context c=getContext();boolean selected=category.equals(key);LinearLayout entry=Ui.row(c);entry.setPadding(dp(12),dp(8),dp(8),dp(8));entry.setBackground(Ui.ripple(c,selected?p.soft:Color.TRANSPARENT,p.blue,18));entry.setSelected(selected);entry.setContentDescription(label+(selected?"，已选择":""));LinearLayout words=Ui.column(c);TextView title=Ui.text(c,label,compact?12:14,selected?p.blue:p.ink,true);words.addView(title);if(!compact){TextView sub=Ui.text(c,note,10,p.muted,false);sub.setPadding(0,dp(7),0,0);words.addView(sub);}entry.addView(words,new LayoutParams(0,-2,1));Ui.Motif art=new Ui.Motif(c,p,icon);entry.addView(art,new LayoutParams(dp(compact?31:48),dp(compact?40:58)));entry.setOnClickListener(v->{category=key;extras=false;render();});Ui.press(entry);LayoutParams lp=new LayoutParams(0,-1,1);lp.setMargins(dp(2),0,dp(2),0);parent.addView(entry,lp);}
    public void request(PetCatalog.Action a){String refused=store.refusal(a);if(refused!=null){host.message(refused);return;}if(!a.transaction()){host.choose(a,UUID.randomUUID().toString());return;}
        Context c=getContext();String token=UUID.randomUUID().toString();removeAllViews();
        FrameLayout back=new FrameLayout(c);TextView backTitle=Ui.text(c,"返回动作列表",13,p.ink,true);backTitle.setGravity(Gravity.CENTER);backTitle.setPadding(dp(44),0,dp(44),0);back.addView(backTitle,new FrameLayout.LayoutParams(-1,-1));ImageView backIcon=Ui.icon(c,"back",p.blue);backIcon.setPadding(dp(12),dp(12),dp(12),dp(12));back.addView(backIcon,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.LEFT|Gravity.CENTER_VERTICAL));back.setBackground(Ui.ripple(c,Color.TRANSPARENT,p.blue,16));back.setOnClickListener(v->render());Ui.add(this,back,44,4);
        FrameLayout stage=new FrameLayout(c);LinearLayout detail=Ui.column(c);detail.setGravity(Gravity.CENTER);detail.setPadding(dp(12),dp(16),dp(12),dp(16));
        TextView action=Ui.text(c,a.name,20,p.ink,true);action.setGravity(Gravity.CENTER);action.setMaxLines(3);Ui.add(detail,action,-2,16);
        TextView effect=Ui.text(c,Ui.actualEffects(a,store.read()),13,p.blue,true);effect.setGravity(Gravity.CENTER);effect.setLineSpacing(dp(6),1);Ui.add(detail,effect,-2,24);
        LinearLayout buttons=Ui.row(c);TextView cancel=Ui.button(c,"再想一想",p,this::render);TextView confirm=Ui.button(c,"确认结算",p,()->{String blocked=store.refusal(a);if(blocked!=null){host.message(blocked);render();return;}host.choose(a,token);render();});Ui.selected(confirm,true,p);
        for(TextView button:new TextView[]{cancel,confirm}){LayoutParams lp=new LayoutParams(0,dp(48),1);lp.setMargins(dp(4),0,dp(4),0);buttons.addView(button,lp);}Ui.add(detail,buttons,48,0);stage.addView(detail,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));addView(stage,compact?new LayoutParams(-1,-2):new LayoutParams(-1,0,1));host.confirmation(true);Ui.enter(detail);
    }
}
