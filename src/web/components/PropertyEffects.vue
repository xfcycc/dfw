<script setup lang="ts">
import { computed } from 'vue';
import { useGameStore } from '../stores/game';
import type { EconomySettings, Tile } from '../../shared/types';
import { isKamui, treasureReward } from '../../shared/rules';
const game = useGameStore();
const props = defineProps<{ tile: Tile; economy?: EconomySettings | null }>();
/** 输入实时地产，返回可见附加效果及暂停说明，不推测隐藏牌。 */
const descriptions = computed(() => {
  const t = props.tile, result: string[] = [], paused = isKamui(t) ? '（暂停）' : '';
  if (t.permanentRentPercent) result.push(`永久租金加成：+${t.permanentRentPercent}%，仅提高最终租金；不改变地价、抵押、建造或其他资产金额，不可被清除，停租与付款减免仍有效。`);
  if (isKamui(t)) result.push(`神威空间：剩余 ${t.kamuiTurns} 个施术者后续个人回合；停止经营、收租及全部附加效果。`);
  if (t.demolitionShield) result.push(`防拆护盾：剩余 ${t.demolitionShieldCharges || 1} 层，每层抵挡一次建筑拆除或降级，连击逐层消耗${paused}。`);
  if (t.treasure) result.push(`大海贼的宝藏：首个正常停留者按当前等级预计获得 ¥${treasureReward(t, props.economy).toLocaleString()}（现金保底 ¥2,000），本次免租且不消耗租金状态${paused}。`);
  if (t.concertStacks) result.push(`乌塔演唱会：${t.concertStacks} 层，下次正租金 ×${Number((1.2 ** t.concertStacks).toPrecision(8))}，触发时全部消耗${paused}。`);
  if (t.barrier) result.push(`普通路障：剩余一次截停，优先于陨星消耗${paused}。`);
  if (t.meteorCharges) result.push(`陨星：剩余 ${t.meteorCharges} 次截停，每次仅消耗一颗${paused}。`);
  if (t.corruptionTurns) result.push(`猩红腐败：剩余 ${t.corruptionTurns} 次侵蚀${paused}。业主回合结束时建筑降 1 级，护盾可挡。`);
  if (t.zeroCasterId) result.push(`绝对零域：暂时停止收租和升级，其他附加效果照常，持续至施术者下一个人回合结束${paused}。`);
  if (t.portalTileId != null) result.push(`虚空之泪：与 ${t.portalTileId} 号地产相连，首个正常步行进入者转移并继续剩余步数，两端随后消失${paused}。`);
  for (const label of game.room?.constructionEffects?.[t.id] ?? []) result.push(`${label}${paused}。`);
  return result;
});
</script>
<template><section v-if="descriptions.length" class="property-effects"><strong>附加效果</strong><p v-for="description in descriptions" :key="description" class="muted">{{ description }}</p></section></template>
<style scoped>.property-effects { margin-top: 12px; padding: 10px 12px; border-radius: 10px; background: #fbf3df; }.property-effects p { margin: 6px 0 0; line-height: 1.6; }</style>
