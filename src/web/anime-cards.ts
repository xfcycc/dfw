import type { ItemKind } from '../shared/types';
/** 复用原卡牌弹框，额外参数由专属表单填写。 */
export const ANIME_ACTIVE: ItemKind[] = ['projection', 'starburst', 'timeRestore', 'explosion', 'corruption', 'contract', 'commandSpell', 'sevenSwords', 'railgun', 'swordRain', 'voidTears', 'absoluteZero', 'kamui'];
/** 仅在对应私人响应或清算窗口使用。 */
export const RESPONSE_CARDS: ItemKind[] = ['immunity', 'halfPayment', 'release', 'timeRewind', 'imagineBreaker', 'vectorReflect', 'sealCard', 'deathReturn'];
