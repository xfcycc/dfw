<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { BoardEffect, PublicPlayer } from '../../shared/types';
import { FIREFLY_PARTS_ATLAS, UR_PARTS_ATLAS, type FullCardSpell } from '../full-card-spells';
import { urDirection } from '../ur-spell-directions';
import { urChoreography, UR_PART_CELLS, type UrPoint, type UrTrack } from '../ur-choreography';
import FullSpellProp from './FullSpellProp.vue';
import CharacterPawn from './CharacterPawn.vue';

const props=defineProps<{card:FullCardSpell;actor:UrPoint;targets:UrPoint[];width:number;height:number;tileSize:number;effect?:BoardEffect;targetPlayers?:PublicPlayer[];compact?:boolean;reduced?:boolean}>();
const direction=computed(()=>urDirection(props.card.skinId)!);
const mediaReduced=ref(window.matchMedia('(prefers-reduced-motion: reduce)').matches);
const reduced=computed(()=>props.reduced||mediaReduced.value);
const hidden=ref(false);
const stage=ref<HTMLElement>();const nodes=ref<HTMLElement[]>([]);let animations:Animation[]=[];
const unit=computed(()=>Math.min(props.tileSize,Math.min(props.width,props.height)/6));
const center=computed(()=>props.compact?{x:Math.max(unit.value*1.7,Math.min(props.width-unit.value*1.7,props.actor.x)),y:Math.max(unit.value*1.7,Math.min(props.height-unit.value*1.7,props.actor.y))}:{x:props.width*.5,y:props.height*.46});
const tracks=computed(()=>urChoreography(direction.value.scene,{actor:props.actor,center:center.value,targets:props.targets,unit:unit.value*(props.compact?.65:1),outcome:props.effect?.kind==='gambleWin'?'win':props.effect?.kind==='gambleLose'?'lose':undefined}));
/** 输入轨道，返回目标的真实前/后外观；缺少结果时继续显示原外观。 */
function appearance(track:UrTrack){return track.shape==='result'?props.effect?.afterTiles.find(tile=>tile.id===props.effect?.beforeTiles[track.target]?.id)??props.effect?.beforeTiles[track.target]:props.effect?.beforeTiles[track.target];}
/** 输入分层部件名，返回独立图格定位；主体与辅助道具不再烘焙为同一张图。 */
function partStyle(shape:UrTrack['shape']):Record<string,string>{const cell=UR_PART_CELLS[shape as keyof typeof UR_PART_CELLS];return{backgroundPosition:`${cell%4/3*100}% ${Math.floor(cell/4)/3*100}%`};}
/** 输入道具轨道及帧，将大主体向棋盘内侧让位；目标副本、光束和命中圈仍在真实坐标。 */
function visibleFrames(track:UrTrack):Keyframe[]{
  if(!['art','wing','jet','slash',...Object.keys(UR_PART_CELLS)].includes(track.shape))return track.frames;
  return track.frames.map(frame=>{
    const transform=String(frame.transform);const angle=Number(transform.match(/rotate\(([-\d.]+)deg\)/)?.[1]??0)*Math.PI/180;const scale=Number(transform.match(/scale\(([-\d.]+)\)/)?.[1]??1);
    const margin=track.size*.5*scale*(Math.abs(Math.cos(angle))+Math.abs(Math.sin(angle)));
    return {...frame,transform:transform.replace(/translate\(([-\d.]+)px,([-\d.]+)px\)/,(_,x,y)=>`translate(${Math.max(margin,Math.min(props.width-margin,track.x+Number(x)))-track.x}px,${Math.max(margin,Math.min(props.height-margin,track.y+Number(y)))-track.y}px)`)};
  });
}
/** 无入参，更新原生时间轴；几何变化保留当前时间，减少动态效果仅保留静态主体。 */
function animate():void{
  const elapsed=Number(animations[0]?.currentTime??0);animations.forEach(animation=>animation.cancel());animations=[];
  if(reduced.value||hidden.value||!stage.value)return;
  const duration=props.effect?.triggered?650:direction.value.duration;
  nodes.value.forEach((node,i)=>{const track=tracks.value[i];if(!track)return;const animation=node.animate(visibleFrames(track),{duration,fill:'both',easing:'linear'});animation.currentTime=elapsed;animations.push(animation);});
}
/** 无入参，媒体偏好变化时立刻停止或恢复时间轴，不保留过期动画。 */
function preference():void{mediaReduced.value=window.matchMedia('(prefers-reduced-motion: reduce)').matches;animate();}
const media=window.matchMedia('(prefers-reduced-motion: reduce)');
/** 无入参，进入后台后撤下本段，不在回到前台时补播过期演出。 */
function visibility():void{if(document.hidden){hidden.value=true;animations.forEach(animation=>animation.cancel());}}
watch([tracks,nodes],animate,{flush:'post'});
onMounted(()=>{media.addEventListener('change',preference);document.addEventListener('visibilitychange',visibility);animate();});
onBeforeUnmount(()=>{media.removeEventListener('change',preference);document.removeEventListener('visibilitychange',visibility);animations.forEach(animation=>animation.cancel());});
</script>
<template>
  <div v-if="!hidden" ref="stage" class="unique-ur" :class="[{compact,reduced},`scene-${direction.scene}`]" :style="{'--ink':direction.color,'--duration':`${direction.duration}ms`,'--firefly-atlas':`url(${FIREFLY_PARTS_ATLAS})`,'--ur-parts-atlas':`url(${UR_PARTS_ATLAS})`}" :data-ur-scene="direction.scene" aria-hidden="true">
    <div v-if="!compact && !effect?.triggered" class="ur-shade"/>
    <svg v-if="!compact" class="ur-targets" :viewBox="`0 0 ${width} ${height}`"><circle v-for="(target,i) in targets" :key="i" :cx="target.x" :cy="target.y" :r="tileSize*.43"/></svg>
    <template v-if="!reduced">
      <div v-for="(track,i) in tracks" :key="i" class="track-anchor" :style="{left:`${track.x}px`,top:`${track.y}px`}">
        <div ref="nodes" class="ur-track" :class="`shape-${track.shape}`" :style="{width:`${track.size}px`,height:`${track.size}px`,color:track.color??direction.color}">
          <div class="track-content">
            <FullSpellProp v-if="track.shape==='art'" :card="card"/>
            <span v-else-if="track.shape in UR_PART_CELLS" class="ur-part" :style="partStyle(track.shape)"/>
            <span v-else-if="['wing','jet','slash'].includes(track.shape)" class="firefly-part" :class="track.shape"/>
            <template v-else-if="track.shape==='copy'||track.shape==='result'">
              <span v-if="appearance(track)" class="ur-building" :style="{backgroundPosition:`${(appearance(track)!.level%2)*100}% ${Math.floor(appearance(track)!.level/2)*100}%`}"/>
              <CharacterPawn v-else-if="targetPlayers?.[track.target]" variant="pawn" :avatar-id="targetPlayers[track.target]!.avatarId" :color="targetPlayers[track.target]!.color"/>
            </template>
            <svg v-else viewBox="0 0 100 100" focusable="false">
              <circle v-if="track.shape==='ring'" cx="50" cy="50" r="44" fill="none" stroke="currentColor" stroke-width="3"/>
              <template v-else-if="track.shape==='rune'"><circle cx="50" cy="50" r="45"/><circle cx="50" cy="50" r="36"/><path d="M50 9L85 70H15ZM50 91L15 30H85Z"/></template>
              <circle v-else-if="track.shape==='orb'" cx="50" cy="50" r="35" fill="currentColor" stroke="#fff4d8" stroke-width="5"/>
              <path v-else-if="track.shape==='shard'" d="M50 3L76 48L50 97L24 48Z" fill="currentColor" stroke="#fff4df" stroke-width="2"/>
              <path v-else-if="track.shape==='bolt'" d="M55 2L25 37L51 39L31 66L57 61L41 98L80 50L55 52L74 22L50 27Z" fill="currentColor"/>
              <path v-else-if="track.shape==='arc'" d="M91 16C5 4 4 89 88 86C33 99-9 48 30 14C49-2 78 5 91 16Z" fill="currentColor"/>
              <path v-else-if="track.shape==='smoke'" d="M20 76C-3 69 0 40 22 35C18 9 50 3 63 22C87 10 104 39 88 55C108 78 73 94 58 82C42 97 24 94 20 76Z" fill="currentColor" opacity=".75"/>
              <template v-else-if="track.shape==='panel'"><rect x="22" y="5" width="56" height="90" rx="5" fill="currentColor" stroke="#6f5c75" stroke-width="3"/><path d="M33 23H67M33 35H61M33 72H67" stroke="#fff3d9" stroke-width="3"/></template>
            </svg>
          </div>
        </div>
      </div>
    </template>
    <div v-else class="static-ur" :style="{left:`${center.x}px`,top:`${center.y}px`,width:`${unit*1.7}px`,height:`${unit*1.7}px`}"><FullSpellProp :card="card"/></div>
  </div>
</template>
<style scoped>
.ur-track{transform-origin:0 0}
.unique-ur{position:absolute;inset:0;pointer-events:none;overflow:hidden;isolation:isolate;border-radius:12px;z-index:21}.ur-shade{position:absolute;inset:0;background:radial-gradient(ellipse,#20203677,#151426c9);animation:ur-shade var(--duration) both}.ur-targets{position:absolute;inset:0;width:100%;height:100%;fill:#fff2c40a;stroke:var(--ink);stroke-width:2;stroke-dasharray:5 4}.track-anchor{position:absolute;width:0;height:0}.ur-track{position:absolute;opacity:0;will-change:transform,opacity}.track-content{position:absolute;inset:0;transform:translate(-50%,-50%);filter:drop-shadow(0 2px 2px #28203955)}.track-content>svg{width:100%;height:100%;overflow:visible;fill:none;stroke:currentColor;stroke-width:2}.shape-ray{height:4px!important;transform-origin:0 50%;border-radius:100%;background:currentColor;box-shadow:0 0 8px currentColor}.shape-ray .track-content{display:none}.shape-copy,.shape-result{z-index:3}.ur-building{display:block;width:100%;height:100%;background:var(--property-levels) no-repeat;background-size:200% 200%}.track-content :deep(.character-pawn){width:100%;height:100%}.firefly-part,.ur-part{display:block;width:100%;height:100%;background-repeat:no-repeat}.firefly-part{background-image:var(--firefly-atlas);background-size:200% 200%}.ur-part{background-image:var(--ur-parts-atlas);background-size:400% 400%}.wing{background-position:100% 0}.jet{background-position:0 100%}.slash{background-position:100% 100%}.static-ur{position:absolute;transform:translate(-50%,-50%)}.reduced .ur-shade{display:none}.compact .ur-track{filter:none}
@keyframes ur-shade{0%,100%{opacity:0}12%,85%{opacity:1}}
</style>
