/** 一张 UR 的分层素材布局；坐标沿用 1000×1000 原图画布。 */
export interface UrCardScene {
  /** 已有卡面皮肤身份。 */
  skinId: string;
  /** 对应本张卡面的动画主题。 */
  theme: 'coin' | 'ticket' | 'wood' | 'alchemy' | 'kamui' | 'reaper';
  /** 预览页的动作说明。 */
  label: string;
  /** 人物层在画布中的位置与缩放。 */
  placement: string;
  /** 需要独立运动的硬币、主持人或剑的裁切区域。 */
  secondaryPath?: string;
  /** 独立图层在画布中的位置与缩放。 */
  secondaryPlacement?: string;
}

export const UR_CARD_SCENES: Record<string, UrCardScene> = {
  card_fate_coin: { skinId: 'card_fate_coin', theme: 'coin', label: '硬币翻转 · 人物晃动 · 圣光与雷光交替', placement: 'none', secondaryPath: 'M350 100H700V420H520L360 290Z' },
  card_all_in_ticket: { skinId: 'card_all_in_ticket', theme: 'ticket', label: '紧张晃动 · 主持人演讲 · 会场灯光', placement: 'translate(5.5%, -1%) scale(.88)', secondaryPath: 'M440 0H1000V600H565L565 555 550 520 515 470 460 430 440 400Z', secondaryPlacement: 'translate(13%, 1%) scale(.85)' },
  card_naruto_wood_houses: { skinId: 'card_naruto_wood_houses', theme: 'wood', label: '结印蓄力 · 木屋生长 · 木屑上扬', placement: 'translate(0, -2%)' },
  card_fma_exchange: { skinId: 'card_fma_exchange', theme: 'alchemy', label: '俯身施术 · 炼成阵流光 · 剑与碎石悬浮', placement: 'translate(2%, -.5%) scale(.84)', secondaryPath: 'M840 250H1000V1000H750V820L780 540Z', secondaryPlacement: 'translate(13%, -6%) scale(.88)' },
  card_naruto_kamui: { skinId: 'card_naruto_kamui', theme: 'kamui', label: '人物前倾 · 建筑卷动 · 异空间漩涡', placement: 'translate(2.5%, 14%) scale(.77)' },
  card_naruto_reaper_seal: { skinId: 'card_naruto_reaper_seal', theme: 'reaper', label: '施术起伏 · 死神虚影 · 封印光带流动', placement: 'translate(-1%, 15%) scale(.73)' },
};
