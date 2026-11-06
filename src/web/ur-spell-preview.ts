import { imageUrl } from './image-assets';

/** 独立预览使用的招式定义，不参与正式卡牌结算。 */
export interface UrSpellSample {
  /** 稳定的卡面身份。 */
  skinId: string;
  /** 招式动作模板。 */
  theme: 'kamui' | 'punch' | 'sakura' | 'reaper';
  /** 预览标题。 */
  name: string;
  /** 演出说明。 */
  description: string;
  /** 整段时间轴，单位毫秒。 */
  duration: number;
  /** 固定预览完成后的说明，不冒充服务端结算。 */
  result: string;
}

export const UR_SPELL_ATLAS = imageUrl('/images/ur-spell-preview/spell-atlas.png');
export const UR_SPELL_SAMPLES: UrSpellSample[] = [
  { skinId: 'card_naruto_kamui', theme: 'kamui', name: '神威', duration: 3600, description: '空间旋臂展开，建筑卷入裂隙，封印留在原地。', result: '演示结果：云朵商店进入空间封印，停止收租 3 回合。' },
  { skinId: 'card_onepunch_serious_punch', theme: 'punch', name: '认真一拳', duration: 3400, description: '巨拳蓄势突进，命中停顿，冲击波跨越棋盘。', result: '演示结果：云朵商店的建筑从 3 级降至 0 级。' },
  { skinId: 'card_neta_senbonzakura', theme: 'sakura', name: '千本樱', duration: 3800, description: '刀刃散为樱色碎片，花瓣成束，环绕目标掠过。', result: '招式展示完成：樱刃掠过目标；本样板不模拟持续卡的业务结算。' },
  { skinId: 'card_naruto_reaper_seal', theme: 'reaper', name: '尸鬼封尽', duration: 3900, description: '死神虚影显现，封印光带连接双方，印记收束。', result: '演示结果：双方手牌上限各减少 1。' },
];

let atlasReady: Promise<void> | undefined;
/** 无入参，解码云端透明素材并返回就绪承诺；失败清除缓存供重试。 */
export function prepareUrSpellAtlas(): Promise<void> {
  if (!atlasReady) {
    const image = new Image();
    image.src = UR_SPELL_ATLAS;
    atlasReady = image.decode().catch((error: unknown) => { atlasReady = undefined; throw error; });
  }
  return atlasReady;
}
