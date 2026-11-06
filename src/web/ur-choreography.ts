import type { UrScene } from './ur-spell-directions';

export const UR_PART_CELLS = {gate:0,hand:1,sword:2,coin:3,dragon:4,dragonball:5,door:6,key:7,bird:8,pillar:9,dress:10,jewels:11,parasol:12,stone:13,lotus:14,scroll:15} as const;
type Shape = 'art'|'copy'|'result'|'ring'|'orb'|'ray'|'shard'|'panel'|'rune'|'bolt'|'arc'|'smoke'|'wing'|'jet'|'slash'|keyof typeof UR_PART_CELLS;
export interface UrTrack { shape:Shape; x:number; y:number; size:number; color?:string; target:number; frames:Keyframe[] }
export interface UrPoint { x:number; y:number }
interface Stage { actor:UrPoint; center:UrPoint; targets:UrPoint[]; unit:number; outcome?:'win'|'lose' }
/** 输入时间比例和相对位移/缩放/角度/透明度，输出原生动画关键帧。 */
function k(offset:number,x=0,y=0,scale=1,rotate=0,opacity=1):Keyframe { return {offset,transform:`translate(${x}px,${y}px) rotate(${rotate}deg) scale(${scale})`,opacity}; }

/** 输入逐卡场景和真实目标坐标，返回该卡手写的多轨分镜；无随机与业务结算。 */
export function urChoreography(scene:UrScene, s:Stage):UrTrack[] {
  const tracks:UrTrack[]=[];const u=s.unit;const c=s.center;const a=s.actor;const targets=s.targets.length?s.targets:[a];const t=targets[0]!;
  /** 输入元素形状、位置、尺寸与时间轴，将一个独立动作加入本次场景。 */
  function add(shape:Shape,p:UrPoint,size:number,frames:Keyframe[],color?:string,target=0):void {tracks.push({shape,x:p.x,y:p.y,size:size*u,frames,color,target});}
  /** 输入位置偏移，返回棋盘内相对舞台坐标。 */
  function p(x:number,y:number):UrPoint{return{x:c.x+x*u,y:c.y+y*u};}
  /** 输入静态主体的起止比例和位置，加入渐显渐隐的定格主体。 */
  function hold(shape:Shape,at:UrPoint,size:number,start=.1,end=.8,color?:string):void{add(shape,at,size,[k(0,0,0,.8,0,0),k(start,0,0,.8,0,0),k(Math.min(start+.06,end)),k(end),k(1,0,0,1,0,0)],color);}
  /** 输入起止坐标及发生时间，加入精确连接两个坐标的光束。 */
  function beam(from:UrPoint,to:UrPoint,start:number,end:number,color?:string):void{const length=Math.hypot(to.x-from.x,to.y-from.y);const angle=Math.atan2(to.y-from.y,to.x-from.x)*180/Math.PI;add('ray',from,length/u,[{offset:0,opacity:0,transform:`rotate(${angle}deg) scaleX(0)`},{offset:start,opacity:0,transform:`rotate(${angle}deg) scaleX(0)`},{offset:start+.04,opacity:1,transform:`rotate(${angle}deg) scaleX(1)`},{offset:end,opacity:1,transform:`rotate(${angle}deg) scaleX(1)`},{offset:1,opacity:0,transform:`rotate(${angle}deg) scaleX(1)`}],color);}
  /** 输入目标、爆开时点和颜色，加入不遮住目标的单次冲击环。 */
  function pulse(at:UrPoint,start:number,size=2,color?:string):void{add('ring',at,size,[k(0,0,0,.05,0,0),k(start,0,0,.05,0,0),k(start+.04,0,0,.3),k(1,0,0,1.2,0,0)],color);}
  /** 输入中心、开始时间和碎片数，生成固定的散落路径，不产生随机结果。 */
  function fragments(at:UrPoint,start:number,count=8,color?:string):void{for(let i=0;i<count;i++){const angle=i*Math.PI*2/count;add('shard',at,.15,[k(0,0,0,0,0,0),k(start,0,0,.1,0,0),k(start+.03),k(1,Math.cos(angle)*u*1.4,Math.sin(angle)*u+u*.4,.4,i*65,0)],color);}}
  /** 输入开始比例，按实际目标顺序显出服务端结果外观副本。 */
  function results(start:number):void{targets.forEach((at,i)=>add('result',at,.7,[k(0,0,0,.5,0,0),k(start,0,0,.5,0,0),k(Math.min(.9,start+.08+i*.008)),k(.95),k(1,0,0,1,0,0)],undefined,i));}

  switch(scene){
    case 'coin':
      add('coin',c,1.7,[k(0,0,u,.2,0,0),k(.22,0,-u*.6,1,180),k(.4,0,-u*.9,.12,540),k(.56,0,-u*.2,1,900),k(.72,0,0,1,1080),k(.9,0,0,1,1080),k(1,0,0,1,1080,0)]);
      if(s.outcome==='win'){beam(p(0,-2),c,.68,.86,'#fff3b5');pulse(c,.72,2.5);}
      if(s.outcome==='lose')add('bolt',c,2,[k(0,0,0,1,0,0),k(.67,0,0,1,0,0),k(.7),k(.82),k(1,0,0,1,0,0)],'#c7b5ff');
      break;
    case 'ticket':
      hold('art',p(0,.75),1.1,.17,.8);for(const side of [-1,1])add('panel',p(side*.38,-.3),1.6,[k(0,0,-u,1,0,0),k(.2),k(.45),k(.7,side*u*.65,0,1,side*12),k(1,side*u*.65,0,1,side*12,0)],'#554156');
      if(s.outcome)hold(s.outcome==='win'?'orb':'rune',p(0,-.3),1.2,.66,.87,s.outcome==='win'?'#ffe8a8':'#9c8cba');
      break;
    case 'repulsion':
      add('orb',c,.8,[k(0,0,0,2,0,0),k(.26,0,0,.2),k(.38,0,0,.2),k(.46,0,0,1.3),k(.65,0,0,0,0,0)]);
      for(let i=0;i<3;i++)pulse(c,.35+i*.06,4+i);
      targets.forEach((at,i)=>add('copy',at,.75,[k(0),k(.4),k(.7,(at.x-c.x)*.3,(at.y-c.y)*.3,.8,(i%2?1:-1)*18),k(1,(at.x-c.x)*.45,(at.y-c.y)*.45,.1,50,0)],undefined,i));break;
    case 'timber':
      targets.forEach((at,i)=>{const start=.12+(i%5)*.05;for(const side of [-1,1])add('pillar',{x:at.x+side*u*.24,y:at.y},.85,[k(0,0,u,.05,0,0),k(start,0,u,.05,0,0),k(start+.2),k(.7),k(1,0,0,1,0,0)]);beam({x:at.x-u*.4,y:at.y-u*.25},{x:at.x+u*.4,y:at.y-u*.25},start+.22,.76,'#c6a577');});results(.72);break;
    case 'dragon':
      for(let i=0;i<7;i++){const angle=i*2*Math.PI/7;add('dragonball',c,.34,[k(0,Math.cos(angle)*u*2,Math.sin(angle)*u,0,0,0),k(.15,Math.cos(angle)*u*1.4,Math.sin(angle)*u*.65,1),k(.38,Math.cos(angle)*u*.75,Math.sin(angle)*u*.4+u*.5,1),k(.76,Math.cos(angle)*u*.75,Math.sin(angle)*u*.4+u*.5,1),k(1,0,0,0,0,0)]);}
      add('dragon',p(0,-.5),2.7,[k(0,0,u,0,0,0),k(.3,0,u*.3,.3,0,0),k(.48),k(.72),k(1,0,-u*.3,1.1,0,0)]);targets.forEach(at=>beam(c,at,.69,.82));results(.81);break;
    case 'gates':
      for(let i=0;i<3;i++){const at=p((i-1)*.72,(i-1)*.25);add('gate',at,1.7,[k(0,0,u,.7,0,0),k(.08+i*.12,0,u,.7,0,0),k(.22+i*.12),k(.58+i*.06,0,0,1),k(.62+i*.06,0,0,.95,-3),k(.75),k(1,0,u*.3,1,0,0)]);beam(p(-2,.4),at,.5+i*.07,.56+i*.07);pulse(at,.54+i*.07,1);}
      break;
    case 'alchemy':
      targets.forEach((at,i)=>{add('rune',at,1.6,[k(0,0,0,0,0,0),k(.22,0,0,1,70),k(.7,0,0,1,190),k(1,0,0,0,240,0)]);add('copy',at,.7,[k(0),k(.3),k(.46,0,-u*.2,.1,0,0)],undefined,i);for(let n=0;n<5;n++)add('shard',at,.15,[k(0,0,0,0,0,0),k(.35,0,0,0,0,0),k(.45,(n-2)*u*.15,-u*.4,1),k(.63,(c.x-at.x)*.5,(c.y-at.y)*.5,1,90),k(.76,0,0,0,180,0)]);});results(.76);break;
    case 'enclosure':
      for(let i=0;i<4;i++)add('panel',c,2.6,[k(0,(i%2?1:-1)*u*2,0,0,i*90,0),k(.16+i*.05,0,0,.7,i*90,.25),k(.5,0,0,1,i*90,.45),k(.83,0,0,1,i*90,.45),k(1,0,0,1,i*90,0)]);
      hold('art',c,2,.35,.76);targets.forEach(at=>pulse(at,.68,1));break;
    case 'projection':
      for(let i=0;i<3;i++){beam(p(-1.5,.6-i*.5),p(1.5,.6-i*.5),.09+i*.08,.66);add('art',p((i-1)*.85,0),1.5,[k(0,0,u*.3,.2,-15,0),k(.25+i*.09,0,u*.3,.2,-15,0),k(.5+i*.08,0,0,1,i*15-15),k(.82,0,0,1,i*15-15),k(1,0,-u*.2,.9,0,0)]);}break;
    case 'breaker':
      hold('rune',t,2,.1,.46);add('hand',t,1.6,[k(0,-u*1.5,u*.4,.7,-20,0),k(.25,-u,u*.3,.8,-20),k(.46),k(.53),k(.82,-u*.3,u*.2,1),k(1,-u*.3,u*.2,1,0,0)]);fragments(t,.49,12);break;
    case 'reflection':
      hold('art',t,1.6,.1,.8);beam(a,t,.18,.38,'#f3c8a3');pulse(t,.4,.8);beam(t,a,.47,.7,'#c9f9ff');for(let i=0;i<3;i++)add('shard',a,.25,[k(0,0,0,0,0,0),k(.18+i*.03),k(.4,t.x-a.x,t.y-a.y,1),k(.44,t.x-a.x,t.y-a.y,1),k(.73,0,0,1,180),k(1,-u,0,0,180,0)]);break;
    case 'return':
      for(let side of [-1,1])add('art',p(side*1.3,0),1.8,[k(0,side*u,0,1,side*15,0),k(.32,-side*u*.5,0,1,0),k(.55,-side*u*.5,0,1,0),k(.9,side*u,0,1,side*15,0)]);
      for(let n=0;n<3;n++)add('copy',t,.8,[k(0),k(.25,n*u*.22,-n*u*.14,1,0,.4),k(.55,n*u*.22,-n*u*.14,1,0,.4),k(.68+n*.08),k(1,0,0,1,0,0)]);pulse(t,.8,1.7);break;
    case 'explosion':
      hold('art',p(-.9,-.4),1.6,.12,.55);for(let i=0;i<3;i++)add('rune',p(0,-.6+i*.55),1.8+i*.25,[k(0,0,0,0,0,0),k(.15+i*.08,0,0,.1),k(.48,0,0,1,i*45),k(.56,0,0,1,i*45),k(.7,0,0,0,i*90,0)]);beam({x:t.x,y:0},t,.54,.64);pulse(t,.61,3.6);for(let i=0;i<5;i++)add('smoke',t,.8,[k(0,0,0,0,0,0),k(.65,0,0,0,0,0),k(.8,(i-2)*u*.4,-u*.6,1.3),k(1,(i-2)*u*.6,-u*1.4,1.8,0,0)]);break;
    case 'command':
      hold('art',p(-.6,-.3),1.6,.12,.45);for(let i=0;i<3;i++)add('shard',p((i-1)*.3,0),.5,[k(0,0,0,0,i*120,0),k(.16+i*.06,0,0,1,i*120),k(.42,0,0,1,i*120),k(.53,0,0,i===0?0:1,i*120,i===0?0:1),k(.85,0,0,i===0?0:1,i*120,i===0?0:1),k(1,0,0,0,i*120,0)]);targets.forEach(at=>{beam(c,at,.56,.74);hold('rune',at,.7,.75,.9)});break;
    case 'firestorm':
      add('art',c,2,[k(0,0,0,.3,-90,0),k(.2,0,0,1,-90),k(.4,0,0,1,60),k(.7,0,0,1,300),k(1,0,-u,.4,420,0)]);for(let i=0;i<4;i++)add('arc',p(0,.5-i*.35),2-i*.2,[k(0,0,0,0,0,0),k(.2+i*.06,0,0,.2,i*60),k(.6,0,0,1,i*60+210),k(.82,t.x-c.x,t.y-c.y,1,i*60+480),k(1,t.x-c.x,t.y-c.y,0,i*60+600,0)]);fragments(t,.79,10);break;
    case 'railgun':
      add('coin',a,.35,[k(0,0,0,0,0,0),k(.16,0,-u*.8,1,120),k(.29,0,-u*.95,1,240),k(.38,0,-u*.9,1,300),k(.44,0,0,1),k(.52,t.x-a.x,t.y-a.y,0,720,0)]);for(let i=0;i<3;i++)add('bolt',a,1.2,[k(0,0,0,0,i*120,0),k(.22+i*.04,0,-u*.4,.5,i*120),k(.43,0,0,1,i*120),k(.49,0,0,0,i*120,0)],'#a4dbff');beam(a,t,.45,.64,'#ffb869');pulse(t,.51,2);break;
    case 'cardseal':
      hold('art',p(-.9,0),1.6,.1,.6);hold('rune',t,2,.24,.66);for(let i=0;i<10;i++){const angle=i*Math.PI/5;add('orb',t,.16,[k(0,Math.cos(angle)*u,Math.sin(angle)*u,0,0,0),k(.24,Math.cos(angle)*u,Math.sin(angle)*u,1),k(.58,0,0,.2,0,0)]);}add('panel',t,1,[k(0,0,0,0,0,0),k(.55,0,0,.1),k(.68),k(.82,0,0,1,180),k(1,0,0,.7,180,0)]);break;
    case 'frost':
      hold('art',p(-.6,-.2),2,.15,.7);targets.forEach(at=>{for(let i=0;i<6;i++){const angle=i*Math.PI/3;beam(at,{x:at.x+Math.cos(angle)*u*.7,y:at.y+Math.sin(angle)*u*.7},.3+i*.025,.72);add('shard',at,.45,[k(0,0,0,0,i*60,0),k(.48+i*.025,0,0,0,i*60,0),k(.7,Math.cos(angle)*u*.4,Math.sin(angle)*u*.4,1,i*60),k(1,Math.cos(angle)*u*.4,Math.sin(angle)*u*.4,1,i*60,0)]);}});break;
    case 'teapot':
      add('art',p(-.8,0),2,[k(0,0,u*.5,.5,0,0),k(.22),k(.4,0,0,1,24),k(.7,0,0,1,24),k(.9),k(1,0,0,.5,0,0)]);for(let side of [-1,1])add('smoke',c,2,[k(0,0,0,0,0,0),k(.34,0,0,.2),k(.58,side*u*.85,0,1),k(.85,side*u*.85,0,1),k(1,side*u*1.1,0,1,0,0)]);targets.forEach((at,i)=>add('result',at,.75,[k(0,c.x-at.x,c.y-at.y,0,0,0),k(.57,c.x-at.x,c.y-at.y,0,0,0),k(.8,0,0,1),k(.94),k(1,0,0,1,0,0)],undefined,i));break;
    case 'thunderlord':
      add('art',p(0,-.3),3,[k(0,0,u,0,0,0),k(.32),k(.47,0,-u*.2,1,-12),k(.61,0,u*.25,1,16),k(.67,0,u*.25,1,16),k(1,0,-u*.2,1,16,0)]);targets.forEach(at=>{beam(p(0,-1.4),at,.58,.66);add('bolt',at,1.8,[k(0,0,0,0,0,0),k(.62,0,0,0,0,0),k(.66),k(.8),k(1,0,0,1,0,0)]);});break;
    case 'bankai':
      for(let i=0;i<6;i++){const angle=i*Math.PI/3;beam(p(Math.cos(angle)*1.7,Math.sin(angle)),c,.08+i*.03,.3,'#a69bbb');}add('art',c,2,[k(0,0,0,.3,30,0),k(.26,0,0,1,0),k(.44,0,0,.7,-25),k(.56,t.x-c.x,t.y-c.y,.9,35),k(.65,t.x-c.x,t.y-c.y,.9,35),k(.8,t.x-c.x+u,t.y-c.y,.4,60,0)]);add('arc',t,2.3,[k(0,0,0,0,-60,0),k(.51,0,0,0,-60,0),k(.57,0,0,1,30),k(.7,0,0,1.1,60),k(1,u,0,1.4,90,0)],'#8f77b2');break;
    case 'rapier':
      for(let i=0;i<3;i++){const from={x:a.x,y:a.y+(i-1)*u*.4};beam(from,t,.18+i*.16,.26+i*.16,'#fff0e2');add('art',from,1.4,[k(0,0,0,0,0,0),k(.15+i*.16,0,0,.6,-25,0),k(.21+i*.16,t.x-from.x,t.y-from.y,1,25),k(.27+i*.16,t.x-from.x,t.y-from.y,1,25),k(.32+i*.16,0,0,.6,-25,0)]);}beam(a,t,.73,.83);pulse(t,.78,1.3);break;
    case 'fortress':
      hold('art',t,2.4,.18,.91);for(let i=0;i<3;i++){const hit={x:t.x+(i-1)*u*.25,y:t.y+(i%2)*u*.25};beam(a,hit,.27+i*.16,.35+i*.16);pulse(hit,.35+i*.16,.8);add('smoke',hit,.9,[k(0,0,0,0,0,0),k(.36+i*.16,0,0,0,0,0),k(.42+i*.16),k(1,(i-1)*u,-u*.5,1.5,0,0)]);}break;
    case 'swordprison':
      for(let i=0;i<3;i++){const angle=i*2*Math.PI/3-Math.PI/2;const at={x:t.x+Math.cos(angle)*u*.7,y:t.y+Math.sin(angle)*u*.7};add('sword',at,1.5,[k(0,0,-u*2,1,0,0),k(.12+i*.14,0,-u*2,1,0,0),k(.24+i*.14),k(.86),k(1,0,0,1,0,0)]);const next=angle+2*Math.PI/3;beam(at,{x:t.x+Math.cos(next)*u*.7,y:t.y+Math.sin(next)*u*.7},.58,.86);}break;
    case 'fusion':
      for(let side of [-1,1]){add('panel',p(side*1.4,0),1,[k(0,side*u,0,.5,side*30,0),k(.2),k(.45,-side*u*.7,-side*u*.55,.7,side*160),k(.64,-side*u*1.4,0,0,side*360,0)],side===1?'#efa46d':'#90ccec');add('arc',c,2.4,[k(0,0,0,0,side*90,0),k(.25,0,0,1,side*90),k(.58,0,0,.3,side*350),k(.68,0,0,0,side*400,0)],side===1?'#efa46d':'#90ccec');}hold('art',c,2,.72,.9);pulse(c,.7,2);break;
    case 'core':
      hold('rune',c,2.5,.12,.83);for(let i=0;i<8;i++){const angle=i*Math.PI/4;add('orb',c,.17,[k(0,Math.cos(angle)*u*1.6,Math.sin(angle)*u,0,0,0),k(.17+i*.04,Math.cos(angle)*u*1.6,Math.sin(angle)*u,1),k(.62,0,0,0,0,0)]);}add('art',c,1.6,[k(0,0,0,0,0,0),k(.45,0,0,.1),k(.73),k(.9),k(1,0,0,1,0,0)]);pulse(c,.76,2.7);break;
    case 'mending':
      add('bolt',p(0,-.65),3,[k(0,0,0,0,90,0),k(.18,0,0,1,90),k(.48,0,0,1,90),k(.85,0,0,0,90,0)]);['#eaaaab','#f4da8c','#a4d5b1','#91cbe5','#c6a8e1'].forEach((color,i)=>add('orb',p((i-2)*.6,.75),.4,[k(0,0,0,0,0,0),k(.22+i*.04),k(.56+i*.055,0,-u*1.4,1),k(.87,0,-u*1.4,.1,0,0)],color));hold('stone',p(0,-.5),1.2,.78,.9);targets.forEach(at=>beam(p(0,-.5),at,.84,.91));results(.87);break;
    case 'firefly': {
      const launch=p(-.95,.65), apex=p(.35,-1.1);
      add('wing',launch,2.8,[k(0,0,0,0,0,0),k(.16,0,0,.25,-15),k(.31,0,0,1,-15),k(.48,apex.x-launch.x,apex.y-launch.y,1,-35),k(.59,apex.x-launch.x,apex.y-launch.y,1,-35),k(.71,t.x-launch.x,t.y-launch.y,.6,40),k(.81,t.x-launch.x,t.y-launch.y,.2,40,0)]);
      add('art',launch,1.6,[k(0,0,u*.4,.3,-20,0),k(.15,0,0,.8,-20),k(.31,0,0,1,-20),k(.48,apex.x-launch.x,apex.y-launch.y,1,-35),k(.59,apex.x-launch.x,apex.y-launch.y,1,-35),k(.7,t.x-launch.x,t.y-launch.y,1,45),k(.75,t.x-launch.x,t.y-launch.y,1,45),k(.84,t.x-launch.x+u*.3,t.y-launch.y+u*.3,.5,60,0)]);
      add('jet',launch,2,[k(0,0,0,0,-40,0),k(.25,-u*.3,u*.2,.4,-40),k(.48,apex.x-launch.x-u*.4,apex.y-launch.y+u*.25,1.3,-40),k(.59,apex.x-launch.x-u*.4,apex.y-launch.y+u*.25,1,-40),k(.7,t.x-launch.x-u*.4,t.y-launch.y-u*.4,1.3,45),k(.78,t.x-launch.x,t.y-launch.y,0,45,0)]);
      beam(launch,apex,.33,.48,'#9bf9df');beam(apex,t,.62,.73,'#d7ffe7');add('slash',t,2.7,[k(0,0,0,0,-30,0),k(.68,0,0,0,-30,0),k(.72,0,0,1,20),k(.77,0,0,1,20),k(1,u*.4,u*.25,1.4,65,0)]);fragments(t,.76,14,'#a6f9cf');targets.slice(1).forEach(at=>{beam(t,at,.76,.85,'#baffde');pulse(at,.79,1.4,'#a6f9cf');});break; }
    case 'sacrifice':
      add('art',p(-.7,-.3),1.7,[k(0,0,u,.4,0,0),k(.23),k(.43),k(.65,0,-u*.2,.1,0,0)]);for(let i=0;i<12;i++){const angle=i*Math.PI/6;add('orb',c,.14,[k(0,Math.cos(angle)*u,Math.sin(angle)*u,0,0,0),k(.25,Math.cos(angle)*u,Math.sin(angle)*u,1),k(.58,(t.x-c.x)*.5,(t.y-c.y)*.5-u*.5,1),k(.81,t.x-c.x+Math.cos(angle)*u*.5,t.y-c.y+Math.sin(angle)*u*.5,1),k(1,t.x-c.x,t.y-c.y,0,0,0)],'#ffa6c0');}pulse(t,.8,1.5);break;
    case 'answer':
      for(let i=0;i<4;i++)add('panel',p((i%2-.5)*.85,(Math.floor(i/2)-.5)*.55),.6,[k(0,0,u*.3,0,0,0),k(.15+i*.07),k(.52),k(.69,0,0,i===3?1.25:.7,0,i===3?1:.25),k(.87,0,0,i===3?1.25:.7,0,i===3?1:.25),k(1,0,0,1,0,0)]);hold('art',p(0,-.8),1,.12,.81);break;
    case 'bill':
      for(let i=0;i<5;i++)add('panel',p(0,(i-2)*.32),.6,[k(0,0,-u*.5,0,0,0),k(.08+i*.1,0,-u*.3,0,0,0),k(.2+i*.1),k(.82),k(1,0,u*.15,1,0,0)],'#f6ead1');add('art',p(.5,.45),1.1,[k(0,0,-u,.5,-25,0),k(.58,0,-u,.5,-25,0),k(.7),k(.86),k(1,0,0,1,0,0)]);break;
    case 'claybird':
      add('bird',a,1.7,[k(0,0,0,.5,-15,0),k(.18,0,0,.7,-15),k(.36,(t.x-a.x)*.4,(t.y-a.y)*.4-u,.9,10),k(.59,t.x-a.x,t.y-a.y,1,35),k(.65,t.x-a.x,t.y-a.y,0,35,0)]);beam({x:t.x-u,y:t.y},{x:t.x+u,y:t.y},.62,.69);beam({x:t.x,y:t.y-u},{x:t.x,y:t.y+u},.62,.69);pulse(t,.66,2.5);add('smoke',t,2,[k(0,0,0,0,0,0),k(.68,0,0,0,0,0),k(.8),k(1,0,-u*.5,1.4,0,0)]);break;
    case 'oldgod':
      add('arc',c,3,[k(0,0,u*.5,0,0,0),k(.25,0,u*.5,1),k(.8,0,u*.5,1),k(1,0,u*.5,0,0,0)]);for(let side of [-1,1])add('arc',p(side*.9,.4),2,[k(0,0,u,0,side*60,0),k(.22,0,u,0,side*60,0),k(.45,0,0,1,side*60),k(.73,side*u*.2,-u*.3,1,side*35),k(1,0,u,.5,side*60,0)]);add('art',c,2.5,[k(0,0,u,.1,0,0),k(.3,0,u*.5,.1,0,0),k(.55),k(.79),k(1,0,u,.1,0,0)]);targets.forEach(at=>{beam(c,at,.65,.82);pulse(at,.75,1)});break;
    case 'inheritance':
      for(let i=0;i<3;i++)add('panel',p((i-1)*.25,0),.9,[k(0,0,0,.5,i*12-12,0),k(.17,0,0,1,i*12-12),k(.4,(1-i)*u*.25,0,.7,0),k(.5,0,0,0,0,0)]);pulse(p(.6,-.4),.36,.7);pulse(p(.6,-.4),.46,1);add('key',c,1.5,[k(0,0,-u,0,0,0),k(.49,0,-u,.5,0,0),k(.67),k(.89),k(1,0,0,1,0,0)]);break;
    case 'crash':
      hold('panel',c,1.6,.12,.86,'#f5e5d1');const chart=[p(-.6,-.5),p(-.2,-.1),p(.2,-.25),p(.6,.55)];for(let i=0;i<3;i++)beam(chart[i]!,chart[i+1]!, .22+i*.12,.8,'#d96d6d');add('art',p(0,.4),1,[k(0,0,-u,.5,-15,0),k(.53,0,-u,.5,-15,0),k(.65),k(.8),k(1,0,0,.5,0,0)]);fragments(p(0,.4),.72,5);break;
    case 'crane':
      targets.forEach((at,i)=>{const start=.12+(i%5)*.06;beam({x:at.x,y:0},at,start,.78,'#e8bc72');add('result',at,.75,[k(0,0,-u*2,1,0,0),k(start,0,-u*2,1,0,0),k(start+.35),k(.88),k(1,0,0,1,0,0)],undefined,i);});hold('art',p(0,-.8),1.5,.08,.7);break;
    case 'pigs':
      for(let i=0;i<3;i++)add('panel',p((i-1)*.6,.15),1,[k(0,0,u,0,i===0?18:0,0),k(.08+i*.18,0,u,0,i===0?18:0,0),k(.24+i*.18,0,0,1,i===0?18:0),k(.7,0,0,1),k(.8,(1-i)*u*.6,0,.5),k(1,0,0,0,0,0)],['#dbc77b','#b58e65','#d59f8d'][i]);hold('art',p(0,-.65),1.6,.35,.75);results(.8);break;
    case 'offer':
      for(let i=0;i<3;i++)add('panel',c,1,[k(0,0,0,.3,0,0),k(.2,(i-1)*u*.55,0,1,(i-1)*18),k(.42,(i-1)*u*.55,0,1,(i-1)*18),k(.58,0,0,.7),k(.75,0,0,.7),k(1,0,0,.3,0,0)]);add('art',c,1.6,[k(0,0,-u,0,0,0),k(.45,0,-u,.3,0,0),k(.6),k(.8,(t.x-c.x)*.5,(t.y-c.y)*.5-u*.3,.8),k(1,t.x-c.x,t.y-c.y,.1,0,0)]);results(.83);break;
    case 'visits':
      hold('door',c,2,.1,.7);for(let i=0;i<3;i++)pulse(p(.45,0),.19+i*.16,.5+i*.15);for(let side of [-1,1])add('panel',p(side*.32,.1),1.2,[k(0,0,0,0,0,0),k(.1),k(.66),k(.83,side*u*.6,0,1,side*16),k(1,side*u*.6,0,1,side*16,0)],'#b4946e');beam(c,t,.79,.9);results(.86);break;
    case 'tornado':
      for(let i=0;i<3;i++)beam(p(-1.5,.3+i*.3),p(1.5,.3+i*.3),.06+i*.04,.3,'#b6b5b5');add('art',c,2.7,[k(0,-u*1.6,u*.3,.1,0,0),k(.24,-u*.9,0,.8,-12),k(.5,0,0,1,8),k(.77,u*.9,0,1,-8),k(1,u*1.6,0,.4,0,0)]);targets.forEach((at,i)=>add('copy',at,.7,[k(0),k(.4),k(.65,(c.x-at.x)*.7,-u,.6,100),k(.9,c.x-at.x+u,-u*1.5,0,210,0)],undefined,i));fragments(c,.55,12);break;
    case 'heaven':
      add('art',c,2.8,[k(0,0,0,.1,-70,0),k(.25,0,0,1,-70),k(.45,0,0,1,-70),k(.6,0,0,1,60),k(.68,0,0,1,60),k(1,u*.6,0,.4,90,0)]);for(let i=0;i<5;i++)add('panel',p((i-2)*.45,-.65),.4,[k(0,0,0,0,0,0),k(.18),k(.53),k(1,(i-2)*u*.45,u,0,i*90,0)],'#c5b6a3');targets.forEach(at=>pulse(at,.62,1));break;
    case 'genius':
      for(let i=0;i<3;i++){const angle=i*2*Math.PI/3-Math.PI/2;const at=p(Math.cos(angle),Math.sin(angle)*.7);add('panel',at,.75,[k(0,c.x-at.x,c.y-at.y,0,0,0),k(.25),k(.7),k(.87,c.x-at.x,c.y-at.y,.5),k(1,c.x-at.x,c.y-at.y,.5,0,0)]);beam(at,c,.32+i*.12,.74);}add('art',c,1.6,[k(0,0,0,0,0,0),k(.25,0,0,.5),k(.65,0,0,1,90),k(.88,0,0,1,120),k(1,0,0,1,120,0)]);break;
    case 'lotus':
      add('lotus',c,2,[k(0,0,u*.5,.2,0,0),k(.25,0,0,.5),k(.48),k(.75),k(1,0,-u*.2,1,0,0)]);for(let i=0;i<3;i++)add('arc',c,2+i*.6,[k(0,0,0,0,-70,0),k(.28+i*.08,0,0,.3,-70,0),k(.58+i*.06,0,0,1,0),k(1,0,0,1.2,30,0)],'#f7e5ed');targets.forEach(at=>{beam(c,at,.7,.85);pulse(at,.8,1.3)});break;
    case 'wardrobe':
      hold('panel',c,2,.1,.84,'#f5d4e8');(['dress','jewels','parasol'] as const).forEach((shape,i)=>add(shape,c,1.5,[k(0,-u*.8,0,.5,-15,0),k(.17+i*.19,-u*.8,0,.5,-15,0),k(.24+i*.19),k(.32+i*.19),k(.4+i*.19,u*.8,0,.5,15,0)]));for(let i=0;i<8;i++){const angle=i*Math.PI/4;add('orb',c,.13,[k(0,0,0,0,0,0),k(.74,0,0,0,0,0),k(.84,Math.cos(angle)*u*.8,Math.sin(angle)*u*.8,1),k(1,t.x-c.x,t.y-c.y,0,0,0)]);}break;
    // 四张既有 UR 的完整分镜由 UrSpellLayer 渲染；仅衍生金额反馈会走此短主体提示。
    case 'kamui':case 'punch':case 'sakura':case 'reaper':hold('art',c,1.5,.1,.75);break;
  }
  return tracks;
}
