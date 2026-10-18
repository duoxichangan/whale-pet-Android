const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'app/src/main/assets');
const baseline=JSON.parse(fs.readFileSync(path.join(root,'tests/fixtures/asset-sha256.json'),'utf8'));
for(const [file,hash]of Object.entries(baseline))assert.equal(crypto.createHash('sha256').update(fs.readFileSync(path.join(assets,file))).digest('hex'),hash,file);
const manifest=JSON.parse(fs.readFileSync(path.join(assets,'pet/manifest.json'),'utf8')),bounds=JSON.parse(fs.readFileSync(path.join(assets,'native-clip-bounds.json'),'utf8'));
assert.equal(Object.keys(manifest.clips).length,106);assert.equal(Object.keys(bounds).length,106);
for(const [id,clip]of Object.entries(manifest.clips)){assert.ok(fs.existsSync(path.join(assets,'pet',clip.file)));assert.ok(bounds[id][0]>=0&&bounds[id][1]>=0&&bounds[id][2]<=480&&bounds[id][3]<=270);}
console.log('PASS: all '+Object.keys(baseline).length+' original pet assets and policy files unchanged; 106 native animation bounds available.');

const registration=JSON.parse(fs.readFileSync(path.join(assets,"native-clip-registration.json"),"utf8"));assert.equal(Object.keys(registration).length,106);for(const [id,r]of Object.entries(registration)){assert.ok(manifest.clips[id]);assert.ok(r.bodyScale>0&&r.bodyScale<2);assert.ok(r.anchor[0]>=0&&r.anchor[0]<=480&&r.anchor[1]>=0&&r.anchor[1]<=270);assert.equal(r.referenceHeight,216);}console.log("PASS: 106 fixed per-clip character registrations.");

const behaviors=JSON.parse(fs.readFileSync(path.join(assets,'native-action-behavior.json'),'utf8'));
const policy=JSON.parse(fs.readFileSync(path.join(assets,'pet/action-policy.json'),'utf8'));
const passive=new Set(Object.values(behaviors).filter(Array.isArray).flat());
assert.equal(passive.size,61);
for(const [id,action]of Object.entries(policy.actions)){assert.equal(passive.has(id),!['feed','play'].includes(action.category),id);}
for(const id of behaviors.dailyAmbient)assert.ok(!['rest','action_045','action_060','action_085','action_100','action_097'].includes(id));
console.log('PASS: all 106 clips assigned; 61 self/triggered actions visible; random pools separated by mode.');
