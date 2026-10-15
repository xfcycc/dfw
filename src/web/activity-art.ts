import type { ActivityKind } from '../shared/types';

export const ACTIVITY_NAMES: Record<ActivityKind, string> = { casino: '暖暖赌场', horse: '小镇赛马', roulette: '轮盘对决', packet: '拼手气红包', rps: '猜拳擂台' };
export const ACTIVITY_HINTS: Record<ActivityKind, string> = { casino: '猜大小或猜点数，自愿下注', horse: '八匹萌马，全场自愿竞猜', roulette: '指定对手，六弹巢轮流对决', packet: '人人有份，10%幸运翻倍', rps: '全场出拳，最后一人独揽奖金' };
/** 输入资源名，返回随应用基础路径部署的活动素材地址。 */
export function activityArt(name: string): string { return `${import.meta.env.BASE_URL}images/activities/${name}.webp`; }

/** 专属场景向活动容器提交的输入；活动身份由容器补齐。 */
export interface ActivityInput { selection?: number; point?: number; playerId?: string }
