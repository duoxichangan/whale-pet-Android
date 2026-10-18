from pathlib import Path
from PIL import Image
import json
base=Path('app/src/main/assets');m=json.loads((base/'pet/manifest.json').read_text(encoding='utf-8'));result={}
for id,clip in m['clips'].items():
 bounds=None
 with Image.open(base/'pet'/clip['file']) as im:
  for frame in range(im.n_frames):
   im.seek(frame)
   b=im.getchannel('A').point(lambda x:255 if x>24 else 0).getbbox()
   if b: bounds=b if bounds is None else (min(bounds[0],b[0]),min(bounds[1],b[1]),max(bounds[2],b[2]),max(bounds[3],b[3]))
 result[id]=[max(0,bounds[0]-6),max(0,bounds[1]-6),min(480,bounds[2]+6),min(270,bounds[3]+6)] if bounds else [128,24,352,254]
(base/'native-clip-bounds.json').write_text(json.dumps(result),encoding='utf-8');print('Measured all frames of',len(result),'original animations')
