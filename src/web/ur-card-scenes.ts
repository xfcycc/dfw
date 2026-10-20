/** 一张 UR 的分层素材布局；坐标沿用 1000×1000 原图画布。 */
export interface UrCardScene {
  /** 已有卡面皮肤身份。 */
  skinId: string;
  /** 对应本张卡面的动画主题。 */
  theme: 'coin' | 'ticket' | 'wood' | 'alchemy' | 'kamui' | 'reaper' | 'wukong' | 'visits' | 'pigs' | 'wish' | 'punch' | 'teapot';
  /** 预览页的动作说明。 */
  label: string;
  /** 人物层在画布中的位置与缩放。 */
  placement: string;
  /** 需要独立运动的硬币、主持人或剑的裁切区域。 */
  secondaryPath?: string;
  /** 独立图层在画布中的位置与缩放。 */
  secondaryPlacement?: string;
}

/** 登记的动漫 UR 保留原版插画，只启用通用呼吸和流光，不使用分层场景。 */
export const ORIGINAL_UR_SKINS = new Set([
  'card_naruto_triple_rashomon',
  'card_anime_u04', 'card_anime_u07', 'card_anime_u08', 'card_anime_u09', 'card_anime_u12',
  'card_anime_u13', 'card_anime_u14', 'card_anime_u15', 'card_anime_u18', 'card_anime_g08',
]);

export const UR_CARD_SCENES: Record<string, UrCardScene> = {
  card_genshin_serenitea_pot: { skinId: 'card_genshin_serenitea_pot', theme: 'teapot', label: '亭阁漂移 · 仙力流转 · 洞天重构', placement: 'none', secondaryPath: 'M600 0H1000V390H600Z' },
  fortune_wukong_heaven_riot: { skinId: 'fortune_wukong_heaven_riot', theme: 'wukong', label: '悟空舞棒 · 天宫震颤 · 碎瓦飘落', placement: 'none' },
  fortune_three_visits: { skinId: 'fortune_three_visits', theme: 'visits', label: '登门作揖 · 礼物摇晃 · 晨光流动', placement: 'none' },
  fortune_three_little_pigs: { skinId: 'fortune_three_little_pigs', theme: 'pigs', label: '小猪施工 · 砖屋起伏 · 木屑上扬', placement: 'none' },
  card_dragonball_shenron_wish: { skinId: 'card_dragonball_shenron_wish', theme: 'wish', label: '神龙盘旋 · 龙珠脉冲 · 许愿争先', placement: 'none' },
  card_onepunch_serious_punch: { skinId: 'card_onepunch_serious_punch', theme: 'punch', label: '挥拳突进 · 冲击扩散 · 碎石扬起', placement: 'none' },
  card_fate_coin: { skinId: 'card_fate_coin', theme: 'coin', label: '硬币翻转 · 人物晃动 · 圣光与雷光交替', placement: 'none', secondaryPath: 'M350 100H700V420H520L360 290Z' },
  card_all_in_ticket: { skinId: 'card_all_in_ticket', theme: 'ticket', label: '紧张晃动 · 主持人演讲 · 会场灯光', placement: 'translate(5.5%, -1%) scale(.88)', secondaryPath: 'M440 0H1000V600H565L565 555 550 520 515 470 460 430 440 400Z', secondaryPlacement: 'translate(13%, 1%) scale(.85)' },
  card_naruto_wood_houses: { skinId: 'card_naruto_wood_houses', theme: 'wood', label: '结印蓄力 · 木屋生长 · 木屑上扬', placement: 'translate(0, -2%)' },
  card_fma_exchange: { skinId: 'card_fma_exchange', theme: 'alchemy', label: '俯身施术 · 炼成阵流光 · 剑与碎石悬浮', placement: 'translate(2%, -.5%) scale(.84)', secondaryPath: 'M840 250H1000V1000H750V820L780 540Z', secondaryPlacement: 'translate(13%, -6%) scale(.88)' },
  card_naruto_kamui: { skinId: 'card_naruto_kamui', theme: 'kamui', label: '人物前倾 · 建筑卷动 · 异空间漩涡', placement: 'translate(2.5%, 14%) scale(.77)' },
  card_naruto_reaper_seal: { skinId: 'card_naruto_reaper_seal', theme: 'reaper', label: '施术起伏 · 死神虚影 · 封印光带流动', placement: 'translate(-1%, 15%) scale(.73)' },
};
