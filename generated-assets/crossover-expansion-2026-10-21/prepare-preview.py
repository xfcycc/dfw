"""读取已冻结文案和本地生成结果，输出完整卡面审阅页及素材清单，不上传或绑定图片。"""
import hashlib
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parents[1]


def prepare():
    """无外部输入；读取本目录记录，写本地图片链接、审阅页和原图元数据，返回已生成数量。"""
    cards = json.loads((ROOT / 'generation-briefs.json').read_text())
    log = ROOT / 'image-results.jsonl'
    results = [json.loads(line) for line in log.read_text().splitlines() if line.strip()] if log.exists() else []
    by_code = {item['code']: item for item in results}
    upload_file = ROOT / 'oss-assets.json'
    uploads = {item['id']: item for item in json.loads(upload_file.read_text())} if upload_file.exists() else {}
    oss_ready = len(uploads) == 20 and all(item.get('verification', {}).get('status') == 200 for item in uploads.values())
    (ROOT / 'art').mkdir(exist_ok=True)
    revision_file = ROOT / 'white-clouds-red-lotus.json'
    revision = json.loads(revision_file.read_text()) if revision_file.exists() else None
    entries = []
    for card in cards:
        item = dict(card)
        if revision and card['code'] == revision['code']:
            item.update(artRevision=revision['revision'], originalPrompt=card['prompt'], prompt=revision['prompt'], motionPreview='white-clouds-red-lotus.html')
        result = by_code.get(card['code'])
        if result and result.get('output_hint'):
            paths = re.findall(r'/Users/[^\s]+\.png', result['output_hint'])
            assert paths, card['code']
            source = Path(paths[-1])
            raw = source.read_bytes()
            assert raw[:8] == b'\x89PNG\r\n\x1a\n'
            width, height = struct.unpack('>II', raw[16:24])
            target = ROOT / 'art' / (card['code'] + '.png')
            if target.is_symlink():
                if target.resolve() != source: target.unlink()
            if not target.exists(): target.symlink_to(source)
            item.update(status='generated', originalPath=str(source), image='art/'+target.name,
                        width=width, height=height, bytes=len(raw), sha256=hashlib.sha256(raw).hexdigest())
        else:
            item.update(status='pending', image=None)
        if oss_ready:
            item['url'] = uploads[item['skinId']]['url']
            item['ossVerification'] = uploads[item['skinId']]['verification']
        entries.append(item)
    frames = json.loads((PROJECT / 'generated-assets/card-frames-2026-10-15/labeled/manifest.json').read_text())
    for frame in frames['entries']:
        target = ROOT / 'art' / ('frame-' + frame['rarity'] + '.png')
        if not target.exists(): target.symlink_to(frame['source'])
    manifest = dict(status='oss_bound_and_verified' if oss_ready else 'local_review_only', ossUploaded=oss_ready, runtimeMappingChanged=oss_ready,
                    originalArtworkRetained=True, imageStorage='原图留在生成目录，本目录art为本地符号链接',
                    artworkStyle='原作人物比例与各作品画风；正方形独立插画；插画不烘焙文字与卡框',
                    quoteNote='蓝银缠绕使用用户指定梗台词；其他短句与场景沿用已确认二创或改编文案',
                    generatedCount=sum(x['status']=='generated' for x in entries), totalCount=len(entries), entries=entries)
    (ROOT / 'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    payload=json.dumps(entries,ensure_ascii=False).replace('</','<\\/')
    page=PAGE.replace('__CARD_DATA__',payload)
    (ROOT/'index.html').write_text(page)
    print(json.dumps({'generated':manifest['generatedCount'],'total':len(entries),'preview':str(ROOT/'index.html')},ensure_ascii=False))


PAGE='''<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>十七素材 · 完整卡面审阅</title>
<style>
:root{font-family:-apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif;color:#503e35;background:#fcf5ea;line-height:1.65;font-synthesis:none}*{box-sizing:border-box}body{margin:0}header{padding:34px max(24px,4vw) 24px;background:radial-gradient(ellipse at 100% 0,#f0ca8a77,transparent 55%),#f4e3cb;border-bottom:1px solid #e4ceb2}.eyebrow{font-size:12px;letter-spacing:3px;color:#967149;font-weight:700}h1{font-family:"Songti SC",serif;font-size:clamp(28px,4vw,42px);margin:5px 0 10px;letter-spacing:2px;color:#633d37}header p{margin:4px 0;font-size:14px;color:#85674f}.toolbar{display:flex;align-items:center;gap:10px;flex-wrap:wrap;padding:16px max(24px,4vw);position:sticky;top:0;z-index:10;background:#fcf5eaf5;border-bottom:1px solid #ebddc9;backdrop-filter:blur(12px)}input,select,button{font:inherit;color:inherit}input,select{padding:9px 12px;border:1px solid #d9c3a6;border-radius:9px;background:#fffaf3}input{width:290px;max-width:100%}.counter{font-size:13px;color:#9b7c61;margin-left:auto}main{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:26px 20px;padding:28px max(24px,4vw) 44px;align-items:start}.entry{min-width:0}.meta{font-size:11px;color:#9c806a;display:flex;justify-content:space-between;gap:10px;padding:0 10px 7px}.content-card{--accent:#b34155;--tint:#ffeadf;position:relative;isolation:isolate;container-type:inline-size;width:100%;padding:0;filter:drop-shadow(0 5px 10px #66432522);text-align:center}.rarity-SSR{--accent:#986811;--tint:#fff3d5}.rarity-SR{--accent:#765299;--tint:#f2eafa}.surface{display:flex;flex-direction:column;background:#fffaf1;clip-path:inset(4cqw round 9cqw)}.artwork{position:relative;width:100%;aspect-ratio:1;overflow:hidden;background:linear-gradient(135deg,#dfd2c0,#f4e9d9);cursor:zoom-in}.artwork img{display:block;width:100%;height:100%;object-fit:contain}.pending{display:grid;place-items:center;height:100%;color:#aa8d74}.copy{padding:5% clamp(12px,6cqw,25px) 24%;margin:0 6%;background:linear-gradient(135deg,#fffcf6,#fcf7ec)}h2{font-size:clamp(18px,6cqw,25px);line-height:1.4;margin:0 0 12px;color:var(--accent);overflow-wrap:anywhere}.scene{font-size:13px;line-height:1.85;text-align:left;color:#655749;margin:0 0 14px}.effect{padding:12px;border:1px solid #aa946430;border-radius:10px;background:var(--tint);font-size:13px;line-height:1.8;text-align:left;font-weight:600;color:var(--accent);margin:0}.quote{font-family:"Songti SC",serif;font-size:13px;line-height:1.7;margin:16px 0 0;padding-top:12px;border-top:1px solid #aa946440;color:#8d6656}.ornament{position:absolute;inset:0;z-index:2;display:grid;grid-template-columns:30% 40% 30%;grid-template-rows:auto minmax(0,1fr) auto;pointer-events:none}.cap{grid-column:1/-1;display:block;width:100%;aspect-ratio:1254/376}.side{display:block;width:100%;height:100%;min-height:0}.source{font-size:11px;line-height:1.5;color:#a28b75;padding:6px 14px 0}.footer{padding:0 max(24px,4vw) 30px;color:#9c806a;font-size:12px}dialog{border:1px solid #e5cdaa;border-radius:20px;background:#fcf5ea;padding:20px;max-width:min(1050px,95vw);max-height:94vh;color:#503e35}dialog::backdrop{background:#271b1cbb;backdrop-filter:blur(5px)}.detail{display:grid;grid-template-columns:1.05fr 1fr;gap:26px;align-items:start}.detail img{width:100%;border-radius:12px}.detail h2{font-size:26px;margin-top:8px}.detail p{font-size:15px}.close{float:right;border:0;border-radius:8px;padding:5px 12px;background:#eedec7;cursor:pointer}.detail blockquote{margin:14px 0;color:#a85c58;font-size:20px}.detail .label{font-size:12px;color:#a28b75;margin-top:20px}.screenshot-hide{display:none!important}@media(min-width:1800px){main{grid-template-columns:repeat(5,minmax(0,1fr))}}@media(max-width:1150px){main{grid-template-columns:repeat(3,minmax(0,1fr));gap:22px 12px;padding-left:18px;padding-right:18px}}@media(max-width:820px){main{grid-template-columns:repeat(2,minmax(0,1fr))}.detail{grid-template-columns:1fr}.counter{margin-left:0}}@media(max-width:480px){main{grid-template-columns:1fr;padding:18px 24px}.toolbar{position:static}header{padding:24px}h1{font-size:29px}}@media print{header,.toolbar,.footer{display:none}main{padding:0;grid-template-columns:repeat(3,1fr);gap:18px}.entry{break-inside:avoid}.content-card{filter:none}}
</style></head><body><header><div class="eyebrow">奇迹大陆 · 素材扩展</div><h1>十七素材 · 完整卡面审阅</h1><p>11 张道具 · 3 张机会 · 3 张财富｜原作人物比例与作品画风</p><p>蓝银缠绕台词：你也有取死之道！｜20张素材已上传OSS并逐张核对；17张插画及白云无尽红莲动态已接入。</p></header><nav class="toolbar"><input id="search" type="search" placeholder="搜索卡名、作品、台词或效果" aria-label="搜索卡牌"><select id="pool" aria-label="卡池"><option value="">全部卡池</option><option value="card">道具</option><option value="chance">机会</option><option value="fortune">财富</option></select><select id="rarity" aria-label="稀有度"><option value="">全部等级</option><option>UR</option><option>SSR</option><option>SR</option></select><span class="counter" id="count"></span></nav><main id="cards"></main><footer class="footer">点击插画查看大图与完整文案。短句与场景为已确认二创、改编或用户指定梗；效果独立显示。插画为本批独立生成的静态原图，UR 采用现有等级卡框，白云无尽另有红莲动态试作。</footer><dialog id="detail"><button class="close" aria-label="关闭详情">关闭 ×</button><div id="detail-content"></div></dialog><script>
const cards=__CARD_DATA__;
const poolNames={card:'道具',chance:'机会',fortune:'财富'};
/** 输入文本，返回HTML转义后的安全文案。 */
function esc(text){return String(text??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
/** 输入等级，返回复用现有花纹素材的卡框标记。 */
function frame(rarity){const image=`art/frame-${rarity}.png`;const part=(cls,view)=>`<svg class="${cls}" viewBox="${view}" preserveAspectRatio="${cls==='side'?'none':'xMidYMid meet'}"><image href="${image}" width="1254" height="1254"/></svg>`;return `<span class="ornament" aria-hidden="true">${part('cap','0 0 1254 376')}${part('side','0 376 376 502')}<span></span>${part('side','878 376 376 502')}${part('cap','0 878 1254 376')}</span>`;}
/** 无输入；按筛选项输出完整卡面，不触发游戏请求。 */
function render(){const query=document.querySelector('#search').value.trim().toLowerCase();const pool=document.querySelector('#pool').value;const rarity=document.querySelector('#rarity').value;const matches=cards.filter(c=>(!pool||c.pool===pool)&&(!rarity||c.rarity===rarity)&&(!query||[c.name,c.source,c.quote,c.description,c.effect].join(' ').toLowerCase().includes(query)));document.querySelector('#count').textContent=`${matches.length} / 17 张 · 插画 ${cards.filter(c=>c.image).length} / 17`;
document.querySelector('#cards').innerHTML=matches.map(c=>`<section class="entry" data-code="${c.code}"><div class="meta"><span>${c.code} · ${poolNames[c.pool]}</span><span>${c.rarity}</span></div><article class="content-card rarity-${c.rarity}">${frame(c.rarity)}<div class="surface"><div class="artwork" data-open="${c.code}" role="button" tabindex="0" aria-label="查看${esc(c.name)}完整设计">${c.image?`<img src="${c.url ?? c.image}" alt="${esc(c.name)}">`:'<div class="pending">插画生成中</div>'}</div><div class="copy"><h2>${esc(c.name)}</h2><p class="scene">${esc(c.description)}</p><p class="effect">${esc(c.effect)}</p><blockquote class="quote">「${esc(c.quote)}」</blockquote></div></div></article><div class="source">${esc(c.source)}${c.motionPreview?` · <a href="${c.motionPreview}">查看红莲演出</a>`:''}</div></section>`).join('');}
/** 输入卡牌编号，弹出该卡原图与已冻结的完整文案。 */
function show(code){const c=cards.find(x=>x.code===code);document.querySelector('#detail-content').innerHTML=`<div class="detail"><div>${c.image?`<img src="${c.url ?? c.image}" alt="${esc(c.name)}">`:''}</div><div><div class="label">${c.code} · ${poolNames[c.pool]} · ${c.rarity}</div><h2>${esc(c.name)}</h2><blockquote>「${esc(c.quote)}」</blockquote><p>${esc(c.description)}</p><div class="label">效果</div><p>${esc(c.effect)}</p><div class="label">${esc(c.source)}</div></div></div>`;document.querySelector('#detail').showModal();}
for(const id of ['search','pool','rarity'])document.querySelector('#'+id).addEventListener(id==='search'?'input':'change',render);
document.querySelector('#cards').addEventListener('click',e=>{const target=e.target.closest('[data-open]');if(target)show(target.dataset.open)});
document.querySelector('#cards').addEventListener('keydown',e=>{if(e.key==='Enter'){const target=e.target.closest('[data-open]');if(target)show(target.dataset.open)}});
document.querySelector('.close').onclick=()=>document.querySelector('#detail').close();
render();if(location.hash==='#C14'){document.querySelector('#search').value='白云无尽';render()}window.cardReview={cards,render,show};
</script></body></html>'''

if __name__=='__main__':
    prepare()
