<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { PublicPlayer, PublicRoomState } from '../../shared/types';
import Modal from './Modal.vue';
import CharacterPawn from './CharacterPawn.vue';
import PlayerText from './PlayerText.vue';

const props = defineProps<{ room: PublicRoomState; meId?: string; paused: boolean; canRestart: boolean; busy: boolean }>();
const emit = defineEmits<{ opened: []; restart: [] }>();
const open = ref(false);
const selectedId = ref('');
const finished = computed(() => props.room.status === 'finished');
const me = computed(() => props.room.players.find(p => p.id === props.meId));
const available = computed(() => finished.value || !!me.value?.bankrupt);
const selected = computed(() => props.room.players.find(p => p.id === selectedId.value));
const stats = computed(() => selected.value?.settlement);
const won = computed(() => !!selected.value && selected.value.id === props.room.winnerId);
const standings = computed(() => [...props.room.players].sort((a, b) => (rank(a) ?? Infinity) - (rank(b) ?? Infinity)));
const headline = computed(() => {
  if (stats.value?.removed) return '本次行程提前结束';
  if (!won.value && !stats.value) return '本局旅程已结束';
  if (won.value) return stats.value && stats.value.peakProperties >= 3 ? '北上广没逃出去，先买下来了' : stats.value && stats.value.rentReceived > 0 ? '恭喜，你成为了自己最讨厌的房东' : '恭喜，你笑到了最后';
  return stats.value && stats.value.rentPaid > stats.value.rentReceived ? '本次财富转移，你负责转出' : '带着梦想进城，带着账单离场';
});
const caption = computed(() => stats.value?.removed ? '战绩保留到离场，故事留给下一局。' : won.value ? '财富旅程到站，掌声送给最后的赢家。' : '财务自由体验卡已到期，欢迎进入朋友观察模式。');
const autoKey = computed(() => available.value ? `dfw:settlement:${props.room.roomId ?? props.room.code}:${props.room.gameId}:${props.meId ?? 'spectator'}:${finished.value ? 'finished' : 'out'}` : '');

/** 输入公开玩家，返回胜者或由淘汰顺序确定的名次；缺少历史顺序时返回空。 */
function rank(p: PublicPlayer): number | null {
  if (p.id === props.room.winnerId) return 1;
  return p.bankrupt && p.eliminationOrder != null ? props.room.players.length - p.eliminationOrder + 1 : null;
}
/** 无入参，打开个人离场账单或全场胜者账单，并通知房间收起其他弹窗。 */
function show(): void {
  if (!available.value) return;
  selectedId.value = finished.value ? props.room.winnerId ?? me.value?.id ?? standings.value[0]?.id ?? '' : me.value?.id ?? '';
  open.value = true; emit('opened');
}
/** 输入实际金额，返回千分位显示，不将缺失历史数据转换成零。 */
function money(amount: number): string { return `¥${amount.toLocaleString()}`; }

watch([autoKey, () => props.paused], ([key, paused]) => {
  if (!key) { open.value = false; return; }
  if (paused || sessionStorage.getItem(key)) return;
  show(); sessionStorage.setItem(key, 'shown');
}, { immediate: true, flush: 'post' });
</script>

<template>
  <button v-if="available" type="button" class="button secondary small settlement-entry" :disabled="paused" @click="show">{{ finished ? '查看本局结算' : '查看我的结算' }}</button>
  <Modal v-if="open && selected" panel-class="settlement-modal" :title="finished ? '本局人生账单' : '你的本局结算'" wide @close="open = false">
    <article class="settlement-report" :class="{ 'is-winner': won }">
      <header class="settlement-hero">
        <span class="settlement-stamp" aria-hidden="true">{{ won ? '👑' : stats?.removed ? '🧳' : '🧾' }}</span>
        <p class="settlement-eyebrow">{{ won ? '城市财富赢家' : stats?.removed ? '提前离场 · 战绩留念' : '离场账单 · 下局再战' }}</p>
        <h3>{{ headline }}</h3>
        <p class="settlement-caption">{{ caption }}</p>
        <div class="settlement-identity">
          <CharacterPawn :avatar-id="selected.avatarId" :color="selected.color" />
          <div><strong><PlayerText :text="selected.name" :players="room.players" /></strong><span>{{ rank(selected) === null ? '已离场' : `本局第 ${rank(selected)} / ${room.players.length} 名` }}<template v-if="stats"> · {{ stats.complete ? '经历' : '已记录' }} {{ stats.turns }} 次个人回合</template></span></div>
        </div>
        <div v-if="won" class="settlement-fortune"><small>最终净资产</small><strong>{{ money(stats?.finalNetWorth ?? selected.netWorth ?? selected.cash) }}</strong></div>
      </header>

      <template v-if="stats">
        <section class="settlement-award" aria-label="本局称号"><small>{{ won ? '获胜时称号' : '离场时称号' }}</small><strong>{{ stats.title }}</strong><p>{{ stats.titleQuote }}</p></section>
        <p v-if="!stats.complete" class="settlement-note">这份存档的早期战绩未记录。以下收支、峰值与回合仅覆盖恢复统计后的经历，不参与全场最高称号评选。</p>
        <dl class="settlement-metrics">
          <div><dt>我也阔过</dt><dd>{{ money(stats.peakNetWorth) }}</dd><small>{{ stats.complete ? '最高净资产' : '已记录最高净资产' }}</small></div>
          <div><dt>躺着赚了</dt><dd>{{ money(stats.rentReceived) }}</dd><small>实际现金收租</small></div>
          <div><dt>替人还贷</dt><dd>{{ money(stats.rentPaid) }}</dd><small>实际现金付租</small></div>
          <div><dt>家里有矿</dt><dd>{{ stats.peakProperties }} <span>块</span></dd><small>同时持有地产峰值</small></div>
          <div><dt>副业战绩</dt><dd :class="{ 'is-negative': stats.activityNet < 0 }">{{ stats.activityNet < 0 ? '−' : stats.activityNet > 0 ? '+' : '' }}{{ money(Math.abs(stats.activityNet)) }}</dd><small>角落小游戏现金净收益</small></div>
          <div><dt>城市足迹</dt><dd>{{ stats.builtLevels }} <span>级建筑</span></dd><small>成交 {{ stats.trades }} 次 · 入狱 {{ stats.jailCount }} 次</small></div>
        </dl>
        <section v-if="stats.cause" class="settlement-moment"><small>{{ won ? '胜利时刻' : stats.removed ? '离场记录' : '破产实录' }}</small><p><PlayerText :text="stats.cause" :players="room.players" /></p><em>{{ stats.causeQuote }}</em></section>
        <section v-if="stats.highlight" class="settlement-moment"><small>本局名场面</small><p><PlayerText :text="stats.highlight" :players="room.players" /></p><em>{{ stats.highlightQuote }}</em></section>
        <p class="settlement-note">战报在本人离场或获胜时冻结，称号按当时数据评选，并列最高可共同获得。租金只计实际现金，不含减免、未付欠款和地产抵债；小游戏净收益已扣本金并计退款；个人回合含休息跳过，建筑含免费升级。</p>
      </template>
      <p v-else class="settlement-note settlement-legacy">这份历史存档未保存个人战报，仅展示已知结局和名次。下一段旅程，再把故事记完整。</p>

      <section v-if="finished" class="settlement-standings" aria-label="最终排名">
        <div class="settlement-section-title"><h4>全场战报</h4><span>点选玩家，查看人生账单</span></div>
        <button v-for="p in standings" :key="p.id" type="button" class="settlement-standing" :class="{ selected: selectedId === p.id }" :aria-pressed="selectedId === p.id" @click="selectedId = p.id">
          <b>{{ p.id === room.winnerId ? '👑' : rank(p) ?? '—' }}</b><CharacterPawn :avatar-id="p.avatarId" :color="p.color" /><span><strong><PlayerText :text="p.name" :players="room.players" />{{ p.id === meId ? '（我）' : '' }}</strong><small>{{ p.settlement?.title ?? (p.id === room.winnerId ? '本局赢家' : '已离场') }}</small></span><span class="settlement-standing-result">{{ p.id === room.winnerId ? '胜利' : p.settlement?.removed ? '被移出' : p.settlement ? '破产' : '已清算' }}</span>
        </button>
      </section>
      <footer class="button-row settlement-actions">
        <button type="button" class="button secondary" @click="open = false">{{ finished ? '收起战报' : '继续围观' }}</button>
        <button v-if="finished && meId && selectedId !== meId" type="button" class="button secondary" @click="selectedId = meId">看看我的账单</button>
        <button v-if="finished && canRestart" type="button" class="button primary" :disabled="busy" @click="emit('restart')">再来一局</button>
      </footer>
    </article>
  </Modal>
</template>

<style scoped>
.settlement-entry { margin: 8px 0; }
.settlement-report { color: #70583e; }
.settlement-hero { position: relative; padding: 16px 12px 22px; text-align: center; border-bottom: 2px dashed #d9bf95; }
.settlement-stamp { display: block; font-size: 44px; line-height: 1.3; }
.settlement-eyebrow { margin: 8px 0; font-size: 11px; font-weight: 800; letter-spacing: 2px; color: #9e7651; }
.settlement-hero h3 { margin: 10px 0; color: #8f553d; font-size: clamp(21px, 4vw, 30px); line-height: 1.45; }
.is-winner .settlement-hero h3 { color: #99601e; }
.settlement-caption { margin: 8px 0 20px; color: #968067; font-size: 12px; line-height: 1.7; }
.settlement-identity { display: flex; justify-content: center; align-items: center; gap: 10px; }
.settlement-identity :deep(.character-pawn) { flex-shrink: 0; }
.settlement-identity > div { min-width: 0; text-align: left; }
.settlement-identity strong { display: block; font-size: 15px; overflow-wrap: anywhere; }
.settlement-identity > div > span { display: block; margin-top: 4px; font-size: 11px; color: #927d62; }
.settlement-fortune { display: flex; flex-direction: column; gap: 5px; margin-top: 20px; }
.settlement-fortune small { font-size: 12px; color: #a47e3f; }
.settlement-fortune strong { color: #ab6a20; font-size: 34px; font-variant-numeric: tabular-nums; }
.settlement-award { display: grid; gap: 8px; margin: 20px 0; padding: 18px; border: 2px solid #dfc18a; border-radius: 18px; background: #fff0ca; text-align: center; }
.settlement-award small { color: #a3804e; font-size: 11px; }
.settlement-award strong { color: #976426; font-size: 23px; }
.settlement-award p { margin: 0; font-size: 12px; line-height: 1.7; }
.settlement-metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin: 18px 0; }
.settlement-metrics > div { min-width: 0; padding: 15px 10px; border: 1px solid #e5d5b7; border-radius: 15px; background: #fffaf0; }
.settlement-metrics dt { font-size: 12px; font-weight: 700; }
.settlement-metrics dd { margin: 8px 0 5px; color: #916733; font-size: 21px; font-weight: 800; overflow-wrap: anywhere; font-variant-numeric: tabular-nums; }
.settlement-metrics dd span { font-size: 12px; }
.settlement-metrics small { color: #9a896f; font-size: 10px; line-height: 1.6; }
.settlement-metrics .is-negative { color: #ae6555; }
.settlement-moment { margin: 12px 0; padding: 16px; border-left: 4px solid #d7a966; border-radius: 4px 16px 16px 4px; background: #f6ebd5; }
.settlement-moment small { color: #9d7846; font-size: 11px; font-weight: 800; }
.settlement-moment p { margin: 8px 0; font-size: 13px; line-height: 1.8; overflow-wrap: anywhere; }
.settlement-moment em { color: #a08765; font-size: 12px; font-style: normal; line-height: 1.7; }
.settlement-note { margin: 14px 0; color: #9b8b73; font-size: 10px; line-height: 1.9; }
.settlement-legacy { padding: 24px 0; font-size: 12px; }
.settlement-standings { padding-top: 12px; }
.settlement-section-title { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 6px; margin-bottom: 10px; }
.settlement-section-title h4 { margin: 0; font-size: 14px; }
.settlement-section-title > span { color: #9b8b73; font-size: 10px; }
.settlement-standing { display: flex; align-items: center; gap: 10px; width: 100%; padding: 10px 12px; margin-top: 7px; border: 2px solid #e6d9c3; border-radius: 14px; background: #fffaf0; color: inherit; text-align: left; cursor: pointer; }
.settlement-standing.selected { border-color: #c5a263; background: #fff1cf; }
.settlement-standing > b { width: 22px; text-align: center; flex-shrink: 0; }
.settlement-standing > span:not(.settlement-standing-result) { flex: 1; min-width: 0; }
.settlement-standing strong { display: block; overflow-wrap: anywhere; font-size: 12px; }
.settlement-standing small { display: block; margin-top: 3px; color: #a08765; font-size: 10px; }
.settlement-standing-result { flex-shrink: 0; color: #a27a48; font-size: 11px; }
.settlement-actions { margin-top: 20px; flex-wrap: wrap; }
@media (max-width: 600px) {
  .settlement-hero { padding: 6px 0 20px; }
  .settlement-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
  .settlement-metrics dd { font-size: 20px; }
  .settlement-actions > button { flex: 1 1 120px; }
}
</style>
