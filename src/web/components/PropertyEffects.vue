<script setup lang="ts">
import { computed } from 'vue';
import type { EconomySettings, Tile } from '../../shared/types';
import { baseRentFor, isKamui } from '../../shared/rules';
const props = defineProps<{ tile: Tile; economy?: EconomySettings | null }>();
/** 输入实时地产，返回可见附加效果及暂停说明，不推测隐藏牌。 */
const descriptions = computed(() => {
  const t = props.tile, result: string[] = [], paused = isKamui(t) ? '（暂停）' : '';
  if (isKamui(t)) result.push(`神威空间：剩余 ${t.kamuiTurns} 个施术者后续个人回合；停止经营、收租及全部附加效果。`);
  if (t.demolitionShield) result.push(`A.T.力场：可抵挡下一次所有类型建筑拆除或降级，剩余一次${paused}。`);
  if (t.treasure) result.push(`大海贼的宝藏：首个正常停留者按当前等级预计获得 ¥${baseRentFor(t, props.economy).toLocaleString()}，本次免租且不消耗租金状态${paused}。`);
  if (t.concertStacks) result.push(`乌塔演唱会：${t.concertStacks} 层，下次正租金 ×${Number((1.2 ** t.concertStacks).toPrecision(8))}，触发时全部消耗${paused}。`);
  if (t.barrier) result.push(`普通路障：剩余一次截停，优先于陨星消耗${paused}。`);
  if (t.meteorCharges) result.push(`陨星：剩余 ${t.meteorCharges} 次截停，每次仅消耗一颗${paused}。`);
  return result;
});
</script>
<template><section v-if="descriptions.length" class="property-effects"><strong>附加效果</strong><p v-for="description in descriptions" :key="description" class="muted">{{ description }}</p></section></template>
<style scoped>.property-effects { margin-top: 12px; padding: 10px 12px; border-radius: 10px; background: #fbf3df; }.property-effects p { margin: 6px 0 0; line-height: 1.6; }</style>
