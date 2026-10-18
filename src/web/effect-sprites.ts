import type { BoardEffect } from '../shared/types';
import artwork from './effect-sprites.json';
import { BOARD_EFFECTS } from './board-effect-presentation';

export type EffectSpriteKind = keyof typeof artwork;
const decoded = new Map<EffectSpriteKind, Promise<void>>();

/** 输入棋盘效果类型，返回使用的逐帧特效素材身份。 */
export function effectSpriteKind(kind: BoardEffect['kind']): EffectSpriteKind {
  return BOARD_EFFECTS[kind]?.sprite ?? 'control';
}

/** 输入素材身份，返回同一张透明四行四列图集的地址。 */
export function effectSpriteUrl(kind: EffectSpriteKind): string { return artwork[kind]; }

/** 输入效果类型，预先解码对应图集后再开始播放；失败记录日志并允许下一次重试。 */
export function prepareEffectSprite(kind: BoardEffect['kind']): Promise<void> {
  const sprite = effectSpriteKind(kind);
  const cached = decoded.get(sprite);
  if (cached) return cached;
  const image = new Image(); image.src = effectSpriteUrl(sprite);
  const ready = image.decode().catch((error: unknown) => { decoded.delete(sprite); console.warn('棋盘特效素材加载失败', sprite, error); });
  decoded.set(sprite, ready);
  return ready;
}
