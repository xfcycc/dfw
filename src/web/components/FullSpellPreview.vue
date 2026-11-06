<script setup lang="ts">
import {computed,onBeforeUnmount,ref} from 'vue';
import type {MotionSegment,PublicRoomState} from '../../shared/types';
import Board from './Board.vue';
import FullCardSpellLayer from './FullCardSpellLayer.vue';
import {FULL_CARD_SPELLS,prepareFullSpell,type FullCardSpell} from '../full-card-spells';
import { urDirection } from '../ur-spell-directions';
const search=ref(''); const pool=ref(new URLSearchParams(location.search).get('rarity')==='UR'?'UR':'all');
const selected=ref(FULL_CARD_SPELLS.find(card=>card.skinId===new URLSearchParams(location.search).get('skin'))??FULL_CARD_SPELLS.find(card=>pool.value==='all'||card.rarity==='UR')!);const playing=ref(false);const loading=ref(false);const error=ref('');let epoch=0;
const direction=computed(()=>urDirection(selected.value.skinId));
const visible=computed(()=>FULL_CARD_SPELLS.filter(card=>(pool.value==='all'||(pool.value==='UR'?card.rarity==='UR':card.skinId.startsWith(pool.value)))&&`${card.name} ${card.skinId} ${card.design} ${urDirection(card.skinId)?.beats.join(' ')??''}`.includes(search.value)));
const room:PublicRoomState={roomId:'full-spell-preview',code:'PREVIEW',gameId:'full-spell-preview',version:1,status:'playing',hostId:'a',players:[{id:'a',name:'阿洛',color:'#739d81',avatarId:'explorer',cash:12000,position:3,ready:true,online:true,bankrupt:false,skipTurns:0,jailed:false,eliminationOrder:null},{id:'b',name:'小桃',color:'#d8978b',avatarId:'cat',cash:12000,position:9,ready:true,online:true,bankrupt:false,skipTurns:0,jailed:false,eliminationOrder:null}],board:Array.from({length:24},(_,id)=>({id,type:id===0?'start':'property',name:id===9?'云朵商店':`花园街 ${id}`,district:Math.floor(id/6),price:2000,ownerId:id===9?'b':null,level:id===9?2:0,mortgaged:false,barrier:false,demolitionShield:false,treasure:false,concertStacks:0,meteorCharges:0,kamuiCasterId:null,kamuiTurns:0,kamuiAppliedTurn:0})),turnOrder:['a','b'],turnIndex:0,turnNumber:1,phase:'pre_roll',pending:null,tradePending:false,pausedReason:null,dice:null,motion:null,winnerId:null,logs:[]};
const outcome=ref<'none'|'win'|'lose'>('none');
const reduced=ref(false);
// 输入选中卡的逐卡分镜，构造明确的预览事实；升级与拆除不再共用降级局面。
const segment=computed<MotionSegment>(()=>{
  const scene=direction.value?.scene;
  const persons=['coin','ticket','gates','projection','breaker','reflection','return','command','cardseal','bankai','fortress','fusion','core','sacrifice','answer','bill','inheritance','crash','genius','wardrobe','reaper'];
  const multiple=['repulsion','timber','dragon','mending','crane','pigs','tornado','heaven','oldgod','firefly'];
  const upgrade=['timber','dragon','mending','crane','pigs'];
  const demolish=['repulsion','punch','explosion','claybird','tornado','heaven'];
  const moving=!scene&&selected.value.actions.includes('T14');
  const beforeTiles=moving||scene&&persons.includes(scene)?[]:(scene&&multiple.includes(scene)?[8,9,10]:[9]).map(id=>({...room.board[id]!}));
  const afterTiles=beforeTiles.map(tile=>({...tile,level:scene&&upgrade.includes(scene)?Math.min(3,tile.level+1):scene&&demolish.includes(scene)?0:tile.level}));
  return{id:`full-${epoch}`,sourceSkinId:selected.value.skinId,sourceCardId:'preview-card',playerId:'a',from:3,path:moving?[4,5,6,7]:[],teleport:false,boardEffect:{kind:scene&&['coin','ticket'].includes(scene)&&outcome.value!=='none'?(outcome.value==='win'?'gambleWin':'gambleLose'):'spell',playerIds:['a','b'],moves:moving?[{playerId:'a',from:3,to:7}]:[],beforeTiles,afterTiles}};
});
/** 输入卡牌，取消旧预览并选择身份；不发送任何对局命令。 */
function select(card:FullCardSpell):void{epoch++;playing.value=false;loading.value=false;error.value='';selected.value=card;}
/** 输入翻页方向，选择当前筛选结果中的相邻卡牌，不自动播放。 */
function next(delta:number):void{const list=visible.value;if(!list.length)return;const index=list.findIndex(card=>card.skinId===selected.value.skinId);select(list[(index+delta+list.length)%list.length]!);}
/** 无入参，加载当前主体后播放；切换后丢弃旧加载结果。 */
async function play():Promise<void>{const key=++epoch;playing.value=false;loading.value=true;error.value='';try{await prepareFullSpell(selected.value);if(epoch===key)playing.value=true;}catch{if(epoch===key)error.value='主体素材加载失败，请重试。';}finally{if(epoch===key)loading.value=false;}}
/** 无入参，后台取消旧播放与加载回调。 */
function hidden():void{if(document.hidden){epoch++;playing.value=false;loading.value=false;}}
document.addEventListener('visibilitychange',hidden);
onBeforeUnmount(()=>{epoch++;document.removeEventListener('visibilitychange',hidden);});
</script>
<template>
  <section class="full-preview">
    <div class="full-controls">
      <input v-model="search" aria-label="搜索卡牌" placeholder="搜索卡名、身份或招式形态"/>
      <select v-model="pool" aria-label="卡牌范围"><option value="all">全部 331 张</option><option value="UR">UR · 48 张独立分镜</option><option value="card_">道具</option><option value="chance_">机会</option><option value="fortune_">财富</option></select>
      <select :value="selected.skinId" aria-label="选择卡牌" @change="select(FULL_CARD_SPELLS.find(c=>c.skinId===($event.target as HTMLSelectElement).value)!)"><option v-for="card in visible" :key="card.skinId" :value="card.skinId">{{card.name}} · {{card.rarity}}</option></select>
      <select v-if="direction && ['coin','ticket'].includes(direction.scene)" v-model="outcome" aria-label="固定预览结果" @change="select(selected)"><option value="none">结果未知：只展示起手</option><option value="win">固定样例：成功</option><option value="lose">固定样例：失败</option></select>
      <button type="button" class="button" @click="next(-1)">上一张</button>
      <button type="button" class="button primary" :disabled="loading" @click="play">{{loading?'加载素材…':playing?'重播':'播放'}}</button>
      <button type="button" class="button" @click="next(1)">下一张</button>
      <label v-if="direction"><input v-model="reduced" type="checkbox"/> 静态查看（减少动态效果）</label>
    </div>
    <p>{{selected.name}} · {{direction?direction.beats.join(' → '):selected.design}}</p>
    <p v-if="error" role="alert">{{error}}</p>
    <Board :room="room"><template #effect-overlay="geometry"><FullCardSpellLayer v-if="playing" :key="epoch" :segment="segment" :players="room.players" :point="geometry.point" :player-point="geometry.playerPoint" :tile-size="geometry.tileSize" :width="geometry.width" :height="geometry.height" :reduced="reduced" @done="playing=false"/></template></Board>
    <small>固定局面仅用于看招式；实战从服务端读取目标和结果。无演出提示框。</small>
  </section>
</template>
<style scoped>.full-preview{max-width:1100px;margin:auto}.full-controls{display:flex;flex-wrap:wrap;gap:8px}.full-controls input,.full-controls select{padding:8px;border:1px solid #cdbb9f;border-radius:8px;background:#fffdf5;max-width:100%}.full-preview p{color:#795d49;font-size:13px}</style>
