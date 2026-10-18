"""Native UI, read-only replay, silent status and screen recovery. Dedicated emulator only."""
from android_qa import *

def absent(*labels):
    nodes=list(tree().iter('node'))
    for label in labels:assert not any(n.get('text')==label or n.get('content-desc')==label for n in nodes),label

def same_row(*labels):
    anchor=bounds(find(labels[0]));center=(anchor[1]+anchor[3])/2
    nodes=list(tree().iter('node'));boxes=[anchor]
    for label in labels[1:]:
        choices=[bounds(n) for n in nodes if n.get('text')==label or n.get('content-desc')==label]
        assert choices,label
        boxes.append(min(choices,key=lambda b:abs((b[1]+b[3])/2-center)))
    centers=[(b[1]+b[3])/2 for b in boxes]
    assert max(centers)-min(centers)<18,(labels,boxes)
    return boxes

def verify_status():
    # The dumpsys enqueued section can contain several pending updates of the same key.
    for name in ['小海','泡泡','泡泡']:debug('settings',name=name,roam=False)
    time.sleep(1)
    dump=cmd('shell','dumpsys','notification','--noredact');(OUT/'notification-dump.txt').write_text(dump,encoding='utf-8')
    active=dump.split('Enqueued Notification List:')[0]
    records=[line for line in active.splitlines() if 'NotificationRecord(' in line and 'pkg=com.whale.pet' in line]
    assert len(records)==1,records
    assert 'channel=companion_status_v2' in records[0] and 'id=7' in records[0]
    channels=[line for line in active.splitlines() if "effectiveNotificationChannel=NotificationChannel{mId='companion_status_v2'" in line]
    assert len(channels)==1 and all(v in channels[0] for v in ['mSound=null','mVibrationEnabled=false','mShowBadge=false','mImportance=2'])
    for flag in ['ONGOING_EVENT','ONLY_ALERT_ONCE','FOREGROUND_SERVICE']:assert flag in records[0]
    window=snapshot()['window'];session=snapshot()['session'];cmd('shell','input','keyevent',223);time.sleep(1);off=snapshot();time.sleep(2);assert snapshot()['draws']<=off['draws']+1
    cmd('shell','input','keyevent',224);cmd('shell','wm','dismiss-keyguard');time.sleep(3);awake=json.loads(prefs()['native-diagnostics']);assert awake['draws']>off['draws'] and awake['window']==window and awake['session']==session
    report={'sharedHorizontalToolbars':True,'confirmationAppCenteredAndTitleHidden':True,'confirmationOverlayChromeHiddenAndRestored':True,'readOnlyReplay':['foodAt100','playAt0Energy','ambient'],'journalRemainsVisible':True,'stateUnchangedAfterReplay':True,'silentChannel':True,'onlyAlertOnce':True,'oneOngoingStatus':True,'retainedScreenWindow':True}
    (OUT/'unified-verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print('PASS',json.dumps(report),flush=True)

cmd('shell','am','force-stop','com.whale.pet');cmd('shell','pm','clear','com.whale.pet');cmd('shell','appops','set','com.whale.pet','SYSTEM_ALERT_WINDOW','allow');cmd('shell','pm','grant','com.whale.pet','android.permission.POST_NOTIFICATIONS')
cmd('shell','am','start','-n','com.whale.pet/.MainActivity');find('小屋')
debug('seed',satiety=40,energy=90,mode='walking')
click('互动');click('自发动作');same_row('自发动作','全部','休息');find('61 个小瞬间');shot('interaction-light.png')
click('一起吃东西');click('中秋赏月吃月饼');find('返回动作列表');absent('今天，一起做什么？','LITTLE JOYS / 今天的小计划')
back=bounds(find('返回动作列表'));buttons=bounds(find('确认结算'));action=bounds(find('中秋赏月吃月饼'));nav=bounds(find('足迹'))
assert back[1]<250 and action[1]>600 and buttons[3]<nav[1]
shot('confirmation-app-light.png');click('再想一想');find('今天，一起做什么？')
click('中秋赏月吃月饼');click('确认结算');find('收起动画');click('收起动画')
click('一起玩乐');click('三球抛接');click('确认结算');find('收起动画');click('收起动画')
click('自发动作');click('东张西望');find('收起动画');click('收起动画')
click('足迹');find('把小日子，慢慢收藏。');same_row('小小回忆','互动记录','全部记录');shot('journal-light.png')
# Even full hunger / depleted energy must not block viewing a memory.
debug('seed',satiety=100,energy=0)
before=prefs()['state:whale-station:pet:v1']
for name in ['中秋赏月吃月饼','三球抛接','东张西望']:
    click('回看动作：'+name);find('动作回看');find('回看动画：'+name);absent('确认结算','返回动作列表');time.sleep(.8)
    shot('replay-'+('food' if name.startswith('中秋') else 'play' if name=='三球抛接' else 'ambient')+'.png')
    click('收起回看');find('把小日子，慢慢收藏。');assert prefs()['state:whale-station:pet:v1']==before
click('设置');cmd('shell','input','swipe',540,1800,540,950,400);click('深海夜色');click('足迹');shot('journal-dark.png')
click('互动');click('自发动作');same_row('自发动作','全部','休息');shot('interaction-dark.png')
debug('seed',satiety=40,energy=90);click('一起吃东西');click('中秋赏月吃月饼');shot('confirmation-app-dark.png');click('返回动作列表');find('今天，一起做什么？')
click('小屋');shot('home-dark.png');click('让泡泡来陪你');cmd('shell','input','keyevent',3);time.sleep(1);debug('settings',roam=False);hold(snapshot());click('自发动作');same_row('自发动作','全部','休息');shot('desktop-toolbar-dark.png')
click('一起吃东西');click('中秋赏月吃月饼');find('返回动作列表');absent('陪伴时光','关闭菜单','回到小屋','待会儿见');shot('confirmation-desktop-dark.png')
click('返回动作列表');find('陪伴时光');find('关闭菜单');find('回到小屋');find('待会儿见');click('关闭菜单')
debug('settings',dark=False);hold(snapshot());click('一起吃东西');click('中秋赏月吃月饼');absent('陪伴时光','关闭菜单','回到小屋','待会儿见');shot('confirmation-desktop-light.png');click('再想一想');click('自发动作');same_row('自发动作','全部','休息');shot('desktop-toolbar-light.png');click('关闭菜单')
verify_status()
