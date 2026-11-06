<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { MotionSegment, PublicPlayer } from '../../shared/types';
import { fullCardSpellFor } from '../full-card-spells';
import FullSpellProp from './FullSpellProp.vue';
import CharacterPawn from './CharacterPawn.vue';
import UrSpellLayer from './UrSpellLayer.vue';
import { UR_SPELL_SAMPLES } from '../ur-spell-preview';
import UniqueUrSpellLayer from './UniqueUrSpellLayer.vue';
import { urDirection } from '../ur-spell-directions';
type Point={x:number;y:number};
const props=defineProps<{ segment:MotionSegment;players:PublicPlayer[];point:(id:number)=>Point;playerPoint:(id:string,position:number,after?:boolean)=>Point;tileSize:number;width:number;height:number;reduced?:boolean }>();
const emit=defineEmits<{done:[id:string]}>();
const media=window.matchMedia('(prefers-reduced-motion: reduce)');const mediaReduced=ref(media.matches);
const prefersReduced=computed(()=>props.reduced||mediaReduced.value);
/** 无入参，同步系统的减少动态效果偏好，不更改系统设置。 */
function preference():void{mediaReduced.value=media.matches;}
const card=computed(()=>fullCardSpellFor(props.segment)!);
const ur=computed(()=>props.segment.boardEffect?.triggered?undefined:UR_SPELL_SAMPLES.find(sample=>sample.skinId===card.value.skinId));
const unique=computed(()=>!UR_SPELL_SAMPLES.some(sample=>sample.skinId===card.value.skinId)&&!!urDirection(card.value.skinId));
const effect=computed(()=>props.segment.boardEffect!);
// 全员机制明确包含本人；单体攻击继续排除仅用于标记来源的施术者编号。
const participantIds=computed(()=>(effect.value.playerIds??[]).filter(id=>id!==props.segment.playerId||['all_cards','all_morale','all_income','bottom_bonus','handDestruction'].includes(card.value.mechanism)));
const targetPlayers=computed(()=>(participantIds.value.length?participantIds.value:[props.segment.playerId]).map(id=>props.players.find(p=>p.id===id)!));
const actor=computed(()=>props.playerPoint(props.segment.playerId,props.segment.from));
const targets=computed(()=>effect.value.beforeTiles.length?effect.value.beforeTiles.map(t=>props.point(t.id)):effect.value.moves.length?effect.value.moves.map(m=>props.playerPoint(m.playerId,m.to,true)):participantIds.value.map(id=>{const p=props.players.find(p=>p.id===id)!;return props.playerPoint(id,p.position)}));
const anchors=computed(()=>effect.value.paymentBefore!=null?[actor.value]:targets.value.length?targets.value:[actor.value]);
const family=computed(()=>card.value.actions[0]);
const carriedProp=computed(()=>family.value==='T14'&&props.segment.path.length>0&&!prefersReduced.value);
// 非 UR 的后续动作占用原时长后半段；状态短触发和沿路径托随仍使用原单段表现。
const secondaryAction=computed(()=>card.value.rarity!=='UR'&&!effect.value.triggered&&!carriedProp.value&&!prefersReduced.value?card.value.actions[1]:undefined);
const phase=computed(()=>effect.value.triggered||(card.value.rarity==='UR'?['answer','bill','inheritance','crash'].includes(urDirection(card.value.skinId)?.scene??''):card.value.actions.includes('T11'))?'light':card.value.rarity==='UR'?'ultimate':'local');
/** 输入真实目标点，主体向棋盘内侧展开；作用圈仍保持真实命中位置。 */
function subjectPosition(p:Point):{left:string;top:string}{const margin=props.tileSize*1.3;const radius=props.tileSize*(phase.value==='light'?.5:.9);return {left:`${Math.max(margin,Math.min(props.width-margin,p.x))-p.x-radius}px`,top:`${Math.max(margin,Math.min(props.height-margin,p.y))-p.y-radius}px`};}
/** 输入目标序号，地产使用实际原等级，人物使用公开头像和颜色。 */
function targetAppearance(index:number):{name:string;level?:number;avatarId?:string;color?:string}{const tile=effect.value.beforeTiles[index];if(tile)return{name:`${tile.id}号地产`,level:tile.level};const player=targetPlayers.value[index]??targetPlayers.value[0]!;return{name:player.name,avatarId:player.avatarId??'explorer',color:player.color};}
let timer:ReturnType<typeof setTimeout>|undefined;
const travelers=ref<HTMLElement[]>([]);let flights:Animation[]=[];
// 输入实际逐格路径与棋盘尺寸，沿原路径更新副本，缩放时保留进度。
watch([travelers,()=>props.segment.path,()=>props.tileSize,prefersReduced],()=>{
  const elapsed=Number(flights[0]?.currentTime??0);flights.forEach(a=>a.cancel());flights=[];
  if(prefersReduced.value||!props.segment.path.length)return;
  for(const node of travelers.value){if(node.dataset.player!==props.segment.playerId)continue;const points=[actor.value,...props.segment.path.map(id=>props.point(id))];
    const frames=points.map((p,i)=>({transform:`translate(${p.x-actor.value.x}px,${p.y-actor.value.y}px)`,offset:.2+.65*i/(points.length-1),opacity:1}));
    const flight=node.animate([{transform:'translate(0,0)',offset:0,opacity:1},...frames,{...frames.at(-1)!,offset:1}],{duration:card.value.duration,fill:'both',easing:'linear'});flight.currentTime=elapsed;flights.push(flight);
  }
},{flush:'post'});
/** 无入参，整段结束通知原队列，结果与目标均由服务端提供。 */
onMounted(()=>{media.addEventListener('change',preference);timer=setTimeout(()=>emit('done',props.segment.id),prefersReduced.value?1000:card.value.duration)});
/** 无入参，取消离页或同步后的旧完成回调。 */
onBeforeUnmount(()=>{media.removeEventListener('change',preference);clearTimeout(timer);flights.forEach(a=>a.cancel());});
</script>
<template>
  <div class="full-card-spell" :class="[family,phase,{reduced:prefersReduced}]" :style="{'--duration':`${card.duration}ms`,'--unit':`${tileSize}px`}" role="status" :aria-label="card.name">
    <template v-if="ur"><UrSpellLayer v-for="(p,i) in anchors" :key="i" :sample="ur" :actor="actor" :target="p" :target-appearance="targetAppearance(i)" :width="width" :height="height" :tile-size="tileSize" :reduced="prefersReduced" :show-name="false" :hide-backdrop="i>0"/></template>
    <UniqueUrSpellLayer v-else-if="unique" :card="card" :actor="actor" :targets="anchors" :effect="effect" :target-players="targetPlayers" :width="width" :height="height" :tile-size="tileSize" :compact="phase==='light'" :reduced="prefersReduced"/>
    <template v-else>
    <div v-if="phase==='ultimate'" class="backdrop"/>
    <svg v-if="card.actions.some(action=>['T02','T03','T10'].includes(action))" class="links" :viewBox="`0 0 ${width} ${height}`" aria-hidden="true"><path v-for="(p,i) in anchors" :key="i" :d="`M${actor.x} ${actor.y} Q${(actor.x+p.x)/2} ${Math.max(tileSize*.2,Math.min(actor.y,p.y)-tileSize)} ${p.x} ${p.y}`" pathLength="1"/></svg>
    <div v-for="(p,i) in anchors" :key="i" class="anchor" :style="{left:`${p.x}px`,top:`${p.y}px`,'--dx':`${actor.x-p.x}px`,'--dy':`${actor.y-p.y}px`,'--i':i}">
      <div class="aura"/><div v-if="!carriedProp" class="subject" :class="family" :style="{...subjectPosition(p),animationDuration:secondaryAction?`${card.duration*.52}ms`:undefined}"><FullSpellProp :card="card"/></div>
      <div v-if="secondaryAction" class="subject secondary" :class="secondaryAction" :style="{...subjectPosition(p),animationDuration:`${card.duration*.48}ms`,animationDelay:`${card.duration*.52}ms`}"><FullSpellProp :card="card"/></div>
      <i v-for="n in 6" :key="n" class="particle" :style="{'--n':n}"/>
    </div>
    <div v-if="effect.paymentBefore!=null && effect.paymentAfter!=null" class="native-bill" :style="{left:`${width*.5}px`,top:`${height*.45}px`}"><s>¥{{effect.paymentBefore.toLocaleString()}}</s><strong>¥{{effect.paymentAfter.toLocaleString()}}</strong></div>
    <div v-for="move in effect.moves" :key="move.playerId" ref="travelers" :data-player="move.playerId" class="traveler" :style="{left:`${playerPoint(move.playerId,move.from).x}px`,top:`${playerPoint(move.playerId,move.from).y}px`,'--tx':`${playerPoint(move.playerId,move.to,true).x-playerPoint(move.playerId,move.from).x}px`,'--ty':`${playerPoint(move.playerId,move.to,true).y-playerPoint(move.playerId,move.from).y}px`}"><div v-if="carriedProp&&move.playerId===segment.playerId" class="carried-prop"><FullSpellProp :card="card"/></div><CharacterPawn variant="pawn" :avatar-id="players.find(p=>p.id===move.playerId)?.avatarId" :color="players.find(p=>p.id===move.playerId)?.color"/></div>
    </template>
  </div>
</template>
<style scoped>
.full-card-spell{position:absolute;inset:0;z-index:20;pointer-events:none;overflow:hidden;border-radius:12px;--accent:#e8bd78}.backdrop{position:absolute;inset:0;background:#231b3e77;animation:fade var(--duration) both}.links{position:absolute;inset:0;width:100%;height:100%;fill:none;stroke:var(--accent);stroke-width:3;stroke-dasharray:1;animation:ray var(--duration) both}.anchor{position:absolute;width:0;height:0}.subject{position:absolute;width:calc(var(--unit)*1.8);height:calc(var(--unit)*1.8);left:calc(var(--unit)*-.9);top:calc(var(--unit)*-.9);filter:drop-shadow(0 3px 2px #34254544);animation:appear var(--duration) both}.aura{position:absolute;width:var(--unit);height:var(--unit);left:calc(var(--unit)*-.5);top:calc(var(--unit)*-.5);border:3px double var(--accent);border-radius:50%;animation:ring var(--duration) both}.particle{position:absolute;width:calc(var(--unit)*.12);height:calc(var(--unit)*.08);background:var(--accent);border-radius:3px;animation:scatter var(--duration) both;--x:calc((var(--n) - 3.5)*var(--unit)*.25);--y:calc((var(--n) - 4)*var(--unit)*.2)}.traveler{position:absolute;width:calc(var(--unit)*.7);height:calc(var(--unit)*.7);margin:calc(var(--unit)*-.35);animation:travel var(--duration) both}.traveler :deep(.character-pawn){width:100%;height:100%}.light .subject{width:var(--unit);height:var(--unit);left:calc(var(--unit)*-.5);top:calc(var(--unit)*-.5)}.light .links,.light .particle{display:none}
.subject.T01{animation-name:drop}.subject.T02{animation-name:projectile}.subject.T03{animation-name:fetch}.subject.T04{animation-name:grow}.subject.T05{animation-name:cut}.subject.T06{animation-name:blink}.subject.T07{animation-name:vortex}.subject.T08{animation-name:ram}.subject.T09{animation-name:orbit}.subject.T10{animation-name:spirit}.subject.T11{animation-name:stamp}.subject.T12{animation-name:wall}.subject.T13{transform-origin:15% 80%;animation-name:strike}.subject.T14{animation-name:flight}.subject.T15{animation-name:domain}.subject.T16{animation-name:charge}
.secondary.T08{animation-name:impact}.reduced>.anchor .subject{animation:none!important}.reduced>.links,.reduced>.anchor .particle,.reduced>.anchor .aura,.reduced>.traveler{display:none}
.carried-prop{position:absolute;width:calc(var(--unit)*1.2);height:calc(var(--unit)*1.2);left:calc(var(--unit)*-.25);top:calc(var(--unit)*-.1);filter:drop-shadow(0 3px 2px #34254544)}.traveler:has(.carried-prop) :deep(.character-pawn){position:relative}
.native-bill{position:absolute;min-width:calc(var(--unit)*1.2);padding:8px;border:2px dashed #ba9c75;border-radius:6px;background:#fff6de;color:#876647;transform:translate(-50%,-50%);font-size:clamp(11px,calc(var(--unit)*.18),18px);animation:appear var(--duration) both}.native-bill s,.native-bill strong{display:block}.native-bill strong{color:#49876f;animation:stamp var(--duration) both}
@media(prefers-reduced-motion:reduce){.full-card-spell *{animation:none!important}.links,.particle,.aura,.backdrop,.traveler{display:none}}
@keyframes impact{0%{opacity:0;scale:.7}25%{opacity:1;scale:1.25}40%,65%{opacity:1;scale:1}100%{opacity:0;scale:1.5}}
@keyframes fade{0%,100%{opacity:0}15%,85%{opacity:1}}@keyframes appear{0%,100%{opacity:0;scale:.3}25%,80%{opacity:1;scale:1}}@keyframes ray{0%,100%{opacity:0;stroke-dashoffset:1}30%,70%{opacity:.8;stroke-dashoffset:0}}@keyframes ring{0%,40%{opacity:0;scale:.2}55%{opacity:1}100%{opacity:0;scale:1.8}}@keyframes scatter{0%,45%{opacity:0;transform:translate(0,0)}55%{opacity:1}100%{opacity:0;transform:translate(var(--x),var(--y)) rotate(100deg)}}
@keyframes drop{0%{opacity:0;transform:translateY(calc(var(--unit)*-2)) rotate(-20deg)}40%{opacity:1;transform:scale(1.1,.8)}60%,85%{opacity:1;transform:none}100%{opacity:0}}@keyframes projectile{0%{opacity:0;transform:translate(var(--dx),var(--dy)) scale(.5)}20%{opacity:1}50%,80%{opacity:1;transform:none}100%{opacity:0;scale:1.3}}@keyframes fetch{0%{opacity:0;transform:translate(var(--dx),var(--dy))}20%{opacity:1}45%,60%{transform:none;opacity:1}95%,100%{opacity:0;transform:translate(var(--dx),var(--dy))}}@keyframes grow{0%,15%{opacity:0;clip-path:inset(100% 0 0);transform:translateY(20px)}60%,85%{opacity:1;clip-path:inset(0);transform:none}100%{opacity:0}}@keyframes cut{0%{opacity:0;transform:translateX(-60px) rotate(-30deg)}30%{opacity:1}55%{transform:rotate(20deg)}100%{opacity:0;transform:translateX(60px)}}@keyframes blink{0%,20%,45%,100%{opacity:0;scale:.2}32%,65%,85%{opacity:1;scale:1}}@keyframes vortex{0%{opacity:0;scale:.2;rotate:-100deg}35%,65%{opacity:1;scale:1;rotate:0deg}100%{opacity:0;scale:0;rotate:240deg}}@keyframes ram{0%{opacity:0;transform:translate(var(--dx),var(--dy)) scale(.4)}42%{opacity:1;transform:scale(1.2)}45%,55%{opacity:1;transform:scale(1)}100%{opacity:0;transform:scale(1.2)}}@keyframes orbit{0%{opacity:0;transform:rotate(-90deg) translateX(var(--unit))}25%,65%{opacity:1}100%{opacity:0;transform:rotate(90deg) translateX(0)}}@keyframes spirit{0%{opacity:0;transform:translateY(var(--unit)) scale(.7)}30%,75%{opacity:.9;transform:none}100%{opacity:0;transform:translateY(calc(var(--unit)*-.4))}}@keyframes stamp{0%{opacity:0;scale:1.6;rotate:-15deg}35%,70%{opacity:1;scale:1;rotate:0deg}100%{opacity:0;scale:.9}}@keyframes wall{0%{opacity:0;transform:scaleX(.1)}40%,85%{opacity:.9;transform:scaleX(1)}100%{opacity:0}}@keyframes strike{0%{opacity:0;rotate:-65deg}30%{opacity:1;rotate:-65deg}45%,55%{opacity:1;rotate:25deg}100%{opacity:0;rotate:-20deg}}@keyframes flight{0%{opacity:0;transform:translate(var(--dx),var(--dy))}20%{opacity:1}80%{opacity:1;transform:translateY(calc(var(--unit)*-.2))}100%{opacity:0;transform:none}}@keyframes domain{0%{opacity:0;scale:.1}30%,75%{opacity:.85;scale:1.5}100%{opacity:0;scale:1.7}}@keyframes charge{0%{opacity:0;scale:.3;rotate:-30deg}30%{opacity:1;scale:.6}65%{opacity:1;scale:1.2;rotate:0deg}100%{opacity:0;scale:.8}}@keyframes travel{0%,25%{opacity:1;transform:none}40%{opacity:0;transform:none}55%{opacity:0;transform:translate(var(--tx),var(--ty))}70%,100%{opacity:1;transform:translate(var(--tx),var(--ty))}}
</style>
