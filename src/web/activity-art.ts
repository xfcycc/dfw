import { imageUrl } from './image-assets';
import type { ActivityKind } from '../shared/types';

export const ACTIVITY_NAMES: Record<ActivityKind, string> = { casino: '暖暖赌场', horse: '小镇赛马', roulette: '轮盘对决', packet: '拼手气红包', rps: '猜拳擂台', treasure: '贪心宝箱', bridge: '三关玻璃桥', minority: '少数派夜市', blindbox: '丰收盲盒' };
export const ACTIVITY_HINTS: Record<ActivityKind, string> = { casino: '猜大小或猜点数，自愿下注', horse: '八匹萌马，全场自愿竞猜', roulette: '指定对手，六弹巢轮流对决', packet: '人人有份，10%幸运翻倍', rps: '全场出拳，最后一人独揽奖金', treasure: '三层宝箱，随时见好就收', bridge: '秘密选边，连过三关各自领奖', minority: '去人少的摊位，争夺唯一大奖', blindbox: '二十选一，唯一隐藏款有机会变限定' };
/** 输入资源名，返回对应的云端活动素材地址。 */
export function activityArt(name: string): string {
  const variants: Record<string, string> = { treasure: 'casino', bridge: 'horse', minority: 'rps', blindbox: 'packet' };
  const [kind, ...suffix] = name.split('-');
  const asset = kind && variants[kind] && ['tile', 'background'].includes(suffix.join('-')) ? `${variants[kind]}-${suffix.join('-')}` : name;
  return imageUrl(`/images/activities/${asset}.webp`);
}

/** 专属场景向活动容器提交的输入；活动身份由容器补齐。 */
export interface ActivityInput { selection?: number; point?: number; playerId?: string }
