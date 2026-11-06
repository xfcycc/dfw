import type { MotionSegment } from '../shared/types';
import { REGULAR_SPELL_SAMPLES } from './regular-spell-preview';
import { UR_SPELL_SAMPLES } from './ur-spell-preview';
import { FULL_CARD_SPELLS, fullCardSpellFor } from './full-card-spells';

const kinds: Record<string, string> = {
  card_naruto_flying_thunder: 'swap', card_dora_takecopter: 'movement',
  card_journey_golden_staff: 'demolish', card_eva_at_field: 'shield',
  card_meme_groupon: 'halfPayment', card_naruto_kamui: 'kamui',
};
/** 输入服务端片段，六张身份与真实事实匹配时返回招式；其他沿用通用效果。 */
export function cardSpellFor(segment: MotionSegment) {
  const effect = segment.boardEffect;
  if (!segment.sourceSkinId || !effect || kinds[segment.sourceSkinId] !== effect.kind) return undefined;
  if (effect.kind === 'swap' && !effect.moves.length) return undefined;
  if (effect.kind === 'movement' && !segment.path.length) return undefined;
  if (['demolish', 'shield', 'kamui'].includes(effect.kind) && (!effect.beforeTiles.length || effect.beforeTiles.some(before => !effect.afterTiles.some(after => after.id === before.id)))) return undefined;
  if (effect.kind === 'halfPayment' && (effect.paymentBefore == null || effect.paymentAfter == null)) return undefined;
  return [...REGULAR_SPELL_SAMPLES, ...UR_SPELL_SAMPLES].find(sample => sample.skinId === segment.sourceSkinId);
}
/** 输入原始移动片段，为竹蜻蜓复用效果队列；保留原路径与事件，不重算移动。 */
export function cardSpellSegment(segment: MotionSegment): MotionSegment {
  if (!FULL_CARD_SPELLS.some(card => card.skinId === segment.sourceSkinId) || segment.boardEffect || !segment.path.length) return segment;
  return { ...segment, boardEffect: { kind: 'movement', playerIds: [segment.playerId], moves: [{ playerId: segment.playerId, from: segment.from, to: segment.path.at(-1)! }], beforeTiles: [], afterTiles: [] } };
}
/** 输入已确认的非 UR 多段卡片段，返回主段优先级；建筑变化、换位与闷棍命中先于后续反馈。 */
function regularSpellPriority(part:MotionSegment,mechanism:string):number {
  const effect=part.boardEffect!;
  if(effect.beforeTiles.some(before=>effect.afterTiles.some(after=>after.id===before.id&&(after.level!==before.level||after.ownerId!==before.ownerId))))return 3;
  if(effect.kind==='swap'||mechanism==='sap'&&effect.kind==='rest')return 2;
  return 0;
}
/** 输入一次公开结算的有序片段，UR 及已确认的非 UR 多段卡只在主段完整演出，其余保留原事实。 */
export function cardSpellSequence(segments:MotionSegment[]):MotionSegment[] {
  const parts=segments.map(cardSpellSegment);
  const primary=new Map<string,{part:MotionSegment;changesBuilding:boolean}>();
  const regularPrimary=new Map<string,{part:MotionSegment;priority:number}>();
  const regularSources=new Map<string,string|null>();
  for(const part of parts){
    if(!part.sourceCardId)continue;
    const card=fullCardSpellFor(part);
    const regular=FULL_CARD_SPELLS.find(candidate=>candidate.skinId===part.sourceSkinId&&candidate.rarity!=='UR'&&['keqingRush','sap','swap','swapAny'].includes(candidate.mechanism));
    if(regular){
      const known=regularSources.get(regular.skinId);
      regularSources.set(regular.skinId,known===undefined||known===part.sourceCardId?part.sourceCardId:null);
    }
    if(card?.rarity==='UR'){
      const previous=primary.get(part.sourceCardId);
      const changesBuilding=part.boardEffect!.beforeTiles.some(before=>part.boardEffect!.afterTiles.some(after=>after.id===before.id&&(after.level!==before.level||after.ownerId!==before.ownerId)));
      if(!previous||changesBuilding&&!previous.changesBuilding)primary.set(part.sourceCardId,{part,changesBuilding});
    }else if(card&&regular){
      const previous=regularPrimary.get(part.sourceCardId),priority=regularSpellPriority(part,card.mechanism);
      if(!previous||priority>previous.priority)regularPrimary.set(part.sourceCardId,{part,priority});
    }
  }
  return parts.map((part,index)=>{
    if(part.sourceCardId&&primary.has(part.sourceCardId)&&primary.get(part.sourceCardId)!.part!==part)return {...segments[index],sourceSkinId:null};
    // 旧路径缺少实例编号时，仅在同皮肤只有一个明确来源的本次结算内关联，避免跨来源合并。
    const sourceId=part.sourceCardId??(part.path.length&&part.sourceSkinId?regularSources.get(part.sourceSkinId):null);
    return sourceId&&regularPrimary.has(sourceId)&&regularPrimary.get(sourceId)!.part!==part?{...segments[index],sourceSkinId:null}:part;
  });
}
