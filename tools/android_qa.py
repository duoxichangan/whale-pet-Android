"""Shared ADB helpers for a dedicated test emulator."""
import os,re,json,time,pathlib,subprocess,xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parents[1]
ADB=os.environ.get('WHALE_ADB','adb');DEVICE=os.environ.get('WHALE_DEVICE','emulator-5554')
assert DEVICE.startswith('emulator-'),'Use a dedicated emulator only'
OUT=ROOT/'tests/output';OUT.mkdir(parents=True,exist_ok=True)
def cmd(*args):
    p=subprocess.run([ADB,'-s',DEVICE,*map(str,args)],capture_output=True,encoding='utf-8',errors='replace',check=True)
    return p.stdout.strip()
def prefs():
    try:root=ET.fromstring(cmd('shell','run-as','com.whale.pet','cat','shared_prefs/whale.xml'))
    except subprocess.CalledProcessError:return {}
    return {n.get('name'):n.text if n.tag=='string' else n.get('value') for n in root}
def state():return json.loads(prefs()['state:whale-station:pet:v1'])
def snapshot():
    debug('snapshot');time.sleep(.1);return json.loads(prefs()['native-diagnostics'])
def debug(command,**values):
    args=['shell','am','broadcast','-n','com.whale.pet/.DebugReceiver','--es','command',command]
    for k,v in values.items():args.extend(['--ez' if isinstance(v,bool) else '--ei' if isinstance(v,int) else '--es',k,str(v).lower() if isinstance(v,bool) else str(v)])
    cmd(*args)
def tree():
    data=prefs()
    if json.loads(data.get('native-diagnostics','{}')).get('menu'):
        debug('snapshot');time.sleep(.1);root=ET.Element('hierarchy')
        for item in json.loads(prefs().get('debug-native-ui','[]')):ET.SubElement(root,'node',item)
        return root
    for _ in range(5):
        try:
            cmd('shell','uiautomator','dump','/sdcard/native-ui.xml');root=ET.fromstring(cmd('shell','cat','/sdcard/native-ui.xml'))
            hint=next((n for n in root.iter('node') if n.get('resource-id')=='com.android.systemui:id/ok'),None)
            if hint is not None:tap(hint);continue
            return root
        except (subprocess.CalledProcessError,ET.ParseError):time.sleep(.3)
    raise AssertionError('Unable to inspect native UI')
def bounds(node):return tuple(map(int,re.findall(r'\d+',node.get('bounds'))))
def tap(node):
    x1,y1,x2,y2=bounds(node);cmd('shell','input','tap',(x1+x2)//2,(y1+y2)//2)
def find(text,contains=False):
    for _ in range(5):
        for n in tree().iter('node'):
            values=[n.get('text',''),n.get('content-desc','')]
            if any(text in s if contains else text==s for s in values):return n
        time.sleep(.2)
    raise AssertionError('Missing UI: '+text)
def click(text,contains=False):tap(find(text,contains));time.sleep(.15)
def shot(name):cmd('shell','screencap','-p','/sdcard/native-shot.png');cmd('pull','/sdcard/native-shot.png',OUT/name)
def hold(d):
    x,y=round(d['anchorX']),round(d['anchorY']-100);cmd('shell','input','swipe',x,y,x,y,800)
    find('一起吃东西')

