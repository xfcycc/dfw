import catalog from './full-card-spell-catalog.json';
import type { MotionSegment } from '../shared/types';
import { urDirection } from './ur-spell-directions';
import { imageUrl } from './image-assets';

export const FIREFLY_PARTS_ATLAS=imageUrl('/images/full-card-spells/firefly-parts.png');
export const UR_PARTS_ATLAS=imageUrl('/images/full-card-spells/ur-parts.png');

export const FULL_CARD_SPELLS = catalog.map(card => ({ ...card, duration: Number(urDirection(card.skinId)?.duration ?? (card.actions.includes('T11') ? 900 : 1800)) }));
export type FullCardSpell = typeof FULL_CARD_SPELLS[number];
const bySkin = new Map(FULL_CARD_SPELLS.map(card => [card.skinId, card]));
const complex = FULL_CARD_SPELLS.filter(card => card.material === 'C' && !['card_naruto_kamui','card_onepunch_serious_punch','card_naruto_reaper_seal','card_neta_senbonzakura'].includes(card.skinId));
const simple = FULL_CARD_SPELLS.filter(card => card.material !== 'C');
const ready = new Map<string, Promise<void>>();

/** 输入公开片段，返回稳定身份对应的逐卡配置；拦截和解除仍沿用原通用表现。 */
export function fullCardSpellFor(segment: MotionSegment): FullCardSpell | undefined {
  if (!segment.sourceSkinId || !segment.boardEffect || ['shieldBlock','kamuiRelease','barrierHit','concertRent','treasureClaim','payment'].includes(segment.boardEffect.kind)) return undefined;
  const card=bySkin.get(segment.sourceSkinId);
  return card && segment.boardEffect.triggered ? {...card,duration:650} : card;
}
/** 输入逐卡配置，返回透明主体图集坐标，不读取私人卡面。 */
export function fullSpellArt(card: FullCardSpell): { url: string; columns: number; rows: number; cell: number } {
  if(card.skinId==='card_neta_ignite_sea')return {url:FIREFLY_PARTS_ATLAS,columns:2,rows:2,cell:0};
  if(card.skinId==='card_anime_u02')return {url:imageUrl('/images/full-card-spells/complex-0.png'),columns:7,rows:7,cell:28};
  const c = complex.findIndex(value => value.skinId === card.skinId);
  if (c >= 0) return { url: imageUrl(`/images/full-card-spells/complex-${Math.floor(c/44)}.png`), columns: 7, rows: 7, cell: c%44 };
  const s = simple.findIndex(value => value.skinId === card.skinId);
  if (s >= 0) return { url: imageUrl(`/images/full-card-spells/props-${Math.floor(s/40)}.png`), columns: 7, rows: 6, cell: s%40 };
  return { url: imageUrl('/images/ur-spell-preview/spell-atlas.png'), columns: 2, rows: 2, cell: ['card_naruto_kamui','card_onepunch_serious_punch','card_neta_senbonzakura','card_naruto_reaper_seal'].indexOf(card.skinId) };
}
/** 输入云端图集地址，返回缓存的解码任务；失败允许下次重试。 */
function prepareAtlas(url:string):Promise<void>{
  if (!ready.has(url)) { const image = new Image(); image.src = url; ready.set(url,image.decode().catch(error => { ready.delete(url); throw error; })); }
  return ready.get(url)!;
}
/** 输入卡牌，主体和分镜所需的分层部件均解码后才允许播放。 */
export async function prepareFullSpell(card: FullCardSpell): Promise<void> {
  const urls=[fullSpellArt(card).url];
  if(urDirection(card.skinId))urls.push(UR_PARTS_ATLAS);
  await Promise.all(urls.map(prepareAtlas));
}
