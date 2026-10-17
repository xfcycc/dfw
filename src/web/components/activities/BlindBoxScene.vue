<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import type { CornerActivity } from '../../../shared/types';
import type { ActivityInput } from '../../activity-art';
import { useGameStore } from '../../stores/game';
import { playSound } from '../../audio';
import CharacterPawn from '../CharacterPawn.vue';
import BlindBoxArt from './BlindBoxArt.vue';
const props = defineProps<{ activity: CornerActivity; enabled: boolean }>();
const emit = defineEmits<{ input: [value: ActivityInput]; revealing: [active: boolean] }>();
const game = useGameStore();
const money = computed(() => game.room?.activityRotation?.money ?? {});
const hiddenPrize = computed(() => (money.value.blindboxHiddenBase ?? 0) + props.activity.participants.length * (money.value.blindboxHiddenPerPlayer ?? 0));
const owners = computed(() => Object.fromEntries(Object.entries(props.activity.packetSelections).map(([id, slot]) => [slot, id])));
const players = computed(() => Object.fromEntries((game.room?.players ?? []).map(player => [player.id, player])));
const flavors = ['午后奶茶', '薄荷花园', '香草烘焙', '晚安好梦', '蜜桃雨季', '海盐书屋'];
const tiers = [{ index: 0, name: '日常普通款', detail: '六种日常穿搭 · 轻巧小配件' }, { index: 6, name: '蔷薇花园 · 隐藏', detail: '蕾丝裙装 · 蔷薇王座 · 蝶翼' }, { index: 7, name: '星月礼赞 · 限定', detail: '星绣礼服 · 珠光水晶 · 月亮底座' }];
const openedAt = ref<Record<string, number>>({});
const now = ref(Date.now());
const clock = setInterval(() => { now.value = Date.now(); }, 100);
const timers: ReturnType<typeof setTimeout>[] = [];
let revealTimer: ReturnType<typeof setTimeout> | undefined;
/** 输入领取者编号，返回本次拆盒已播放毫秒数；初次加载的已领盒不重复播放。 */
function elapsed(id: string | undefined): number { return id && openedAt.value[id] !== undefined ? Math.max(0, now.value - openedAt.value[id]!) : Infinity; }
/** 输入盲盒序号，返回已揭晓的公仔款式；普通外观由活动身份与序号稳定映射，奖金仍由服务端决定。 */
function figure(slot: number): number {
  const id = owners.value[slot];
  if (id && props.activity.revealed[id]) return props.activity.revealed[id] === 2 && elapsed(id) >= 2400 ? 7 : 6;
  let hash = slot;
  for (const character of props.activity.id) hash = (hash * 31 + character.charCodeAt(0)) >>> 0;
  return hash % flavors.length;
}
/** 输入盲盒序号，返回拆封进度或已揭晓公仔名称，不提前显示隐藏与限定款。 */
function caption(slot: number): string {
  const id = owners.value[slot], age = elapsed(id);
  if (!id) return '甜心日常系列';
  if (age < 450) return '就是这盒，摇一摇…';
  if (age < 850) return '打开纸盒…';
  if (age < 1300) return '撕开神秘内袋…';
  if (age < 1900) return '你的公仔来啦！';
  return ['蔷薇花园', '星月礼赞'][figure(slot) - 6] ?? flavors[figure(slot)]!;
}
/** 无入参，清除本次拆盒动画与延迟音效；重连和组件卸载时不补播。 */
function clearAnimation(): void { openedAt.value = {}; timers.forEach(clearTimeout); timers.length = 0; clearTimeout(revealTimer); emit('revealing', false); }
watch(() => ({ ids: Object.keys(props.activity.payouts), epoch: game.presentationEpoch }), (current, previous) => {
  if (current.epoch !== previous.epoch) { clearAnimation(); return; }
  const fresh = current.ids.filter(id => !previous.ids.includes(id));
  if (!fresh.length || game.connection !== 'online' || game.room?.recoveryPending || document.hidden || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  now.value = Date.now();
  for (const id of fresh) openedAt.value[id] = now.value;
  clearTimeout(revealTimer);
  emit('revealing', true);
  revealTimer = setTimeout(() => { emit('revealing', false); }, 2600);
  playSound('packet_open');
  timers.push(setTimeout(() => { if (game.connection === 'online') playSound('coin'); }, 1900));
  if (fresh.some(id => props.activity.bonuses[id])) timers.push(setTimeout(() => { if (game.connection === 'online') playSound('packet_lucky'); }, 2400));
});
watch(() => game.connection, state => { if (state !== 'online') clearAnimation(); });
const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
/** 无入参，后台或减少动态效果时直接恢复已到账的静态款式与金额。 */
function suspendAnimation(): void { if (document.hidden || reduced.matches) clearAnimation(); }
document.addEventListener('visibilitychange', suspendAnimation);
reduced.addEventListener('change', suspendAnimation);
onBeforeUnmount(() => { clearInterval(clock); clearAnimation(); document.removeEventListener('visibilitychange', suspendAnimation); reduced.removeEventListener('change', suspendAnimation); });
</script>
<template>
  <div class="blindbox-scene">
    <p>20 个盲盒 · 19 个普通款 <strong>¥{{ money.blindboxRegular?.toLocaleString() }}</strong> · 唯一隐藏款 <strong>¥{{ hiddenPrize.toLocaleString() }}</strong></p>
    <p class="activity-help">免费每人选一盒。隐藏款有 10% 概率升级限定款、奖励翻倍；选中即到账，拆盒后展示。</p>
    <div class="macaron-shop">
      <div class="macaron-sign"><span class="toy-shop-seal" aria-hidden="true">N<span>✦</span></span><div><small>NUANNUAN · SWEET LITTLE MOMENTS</small><strong>暖暖潮玩盲盒</strong><span>把今天的小幸运，拆出来</span></div><span class="toy-stock">{{ 20 - Object.keys(owners).length }}<small>盒待拆</small></span></div>
      <div class="macaron-swatches" aria-label="六种日常普通款、蔷薇花园隐藏款和星月礼赞限定款"><span v-for="(flavor, index) in flavors" :key="flavor" :class="`flavor-${index}`" :title="flavor"></span><small>日常普通款 · 蔷薇隐藏款 · 星月限定款</small></div>
      <div class="toy-series-showcase" aria-label="公仔款式图鉴，不对应盒内位置"><div v-for="tier in tiers" :key="tier.index" :class="{ rare: tier.index === 6, exclusive: tier.index === 7 }"><BlindBoxArt :index="tier.index" /><strong>{{ tier.name }}</strong><small>{{ tier.detail }}</small></div></div>
      <div class="blindbox-grid"><button v-for="slot in 20" :key="slot" class="blindbox-choice" :class="{ claimed: !!owners[slot], unboxing: elapsed(owners[slot]) < 1900, opened: owners[slot] && elapsed(owners[slot]) >= 1900, hidden: owners[slot] && (activity.revealed[owners[slot]!] ?? 0) > 0 && elapsed(owners[slot]) >= 1900, limited: owners[slot] && activity.revealed[owners[slot]!] === 2 && elapsed(owners[slot]) >= 2400, celebrating: elapsed(owners[slot]) >= 1900 && elapsed(owners[slot]) < 3300, own: owners[slot] === game.me?.id }" :disabled="!enabled || !!owners[slot]" :aria-label="`${slot}号盲盒${owners[slot] ? '，' + caption(slot) + '，已领取' : enabled ? '，点击拆开' : '，未领取'}`" @click="emit('input', { selection: slot })">
        <span class="box-number">NO. {{ String(slot).padStart(2, '0') }}</span><span v-if="owners[slot] && elapsed(owners[slot]) >= 1900" class="toy-rarity">{{ figure(slot) === 7 ? '限定' : figure(slot) === 6 ? '隐藏' : '常规' }}</span>
        <span class="toy-stage" aria-hidden="true"><span class="toy-halo">✧ ✦ ✧</span><span class="toy-plinth"></span><span class="toy-reveal"><BlindBoxArt :index="owners[slot] ? figure(slot) : 0" /></span><span class="toy-wrapper"><i></i><i></i><b>?</b></span><span class="toy-carton"><BlindBoxArt kind="box" :index="owners[slot] && elapsed(owners[slot]) >= 450 ? 1 : 0" /></span><span class="toy-sparkles">✦ · ✧</span></span>
        <span v-if="elapsed(owners[slot]) >= 1900 && elapsed(owners[slot]) < 3300" class="toy-prize-burst" aria-hidden="true"><i v-for="n in 7" :key="n" :style="{ '--particle': n }">{{ n % 2 ? '✧' : '✿' }}</i></span>
        <strong>{{ caption(slot) }}</strong>
        <template v-if="owners[slot]"><span class="box-owner"><CharacterPawn :avatar-id="players[owners[slot]!]?.avatarId" :color="players[owners[slot]!]?.color" /><small>{{ players[owners[slot]!]?.name }}{{ owners[slot] === game.me?.id ? ' · 你' : '' }}</small></span><b v-if="elapsed(owners[slot]) >= 1900" :key="elapsed(owners[slot]) < 2400 ? 'base' : 'final'">+¥{{ (elapsed(owners[slot]) < 2400 ? activity.packets[owners[slot]!] : activity.payouts[owners[slot]!])?.toLocaleString() }}</b><span v-if="activity.bonuses[owners[slot]!] && elapsed(owners[slot]) >= 2400" class="lucky-badge">星月限定 · 奖励翻倍</span></template>
        <small v-else>{{ activity.settled ? '本场未领取' : enabled ? '挑一盒，拆开小幸运' : '等待有缘人' }}</small>
      </button></div>
    </div>
    <p class="activity-help">{{ activity.submitted.length }} / {{ activity.participants.length }} 人已领取 · {{ game.me && activity.submitted.includes(game.me.id) ? '你的公仔已领取，奖励已到账' : '每人限领一盒，未领取的盒子不发奖' }}</p>
  </div>
</template>
