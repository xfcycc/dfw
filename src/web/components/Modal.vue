<script setup lang="ts">
import { onBeforeUnmount, nextTick, onMounted, ref, watch } from 'vue';
const props = withDefaults(defineProps<{ title: string; wide?: boolean; panelClass?: string | string[]; closeDisabled?: boolean; open?: boolean; hideHeading?: boolean; mapPicking?: boolean }>(), { open: true });
const emit = defineEmits<{ close: [] }>();
const dialog = ref<HTMLElement>();
let previous: HTMLElement | null = null;
/** 输入键盘事件，关闭弹窗或将焦点限制在弹窗内。 */
function keydown(event: KeyboardEvent): void {
  const visibleDialogs = Array.from(document.querySelectorAll<HTMLElement>('[role="dialog"]')).filter(node => node.getClientRects().length > 0);
  if (props.mapPicking) {
    if (props.open && event.key === 'Escape' && !props.closeDisabled && !visibleDialogs.length) emit('close');
    return;
  }
  if (!props.open || visibleDialogs.at(-1) !== dialog.value) return;
  if (event.key === 'Escape' && !props.closeDisabled) emit('close');
  if (event.key !== 'Tab' || !dialog.value) return;
  const nodes = Array.from(dialog.value.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), textarea:not(:disabled), select:not(:disabled), a[href], [tabindex="0"]'));
  if (!nodes.length) { event.preventDefault(); return; }
  const first = nodes[0]; const last = nodes.at(-1);
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
}
/** 显示时获取焦点，收起时归还焦点；内容保留在组件内以便继续编辑。 */
watch(() => props.open, async open => {
  if (open) { previous = document.activeElement as HTMLElement; await nextTick(); if (props.open) dialog.value?.focus(); }
  else if (previous?.isConnected && (dialog.value?.contains(document.activeElement) || document.activeElement === document.body)) previous.focus();
}, { immediate: true });
/** 输入地图选择状态，回到列表时恢复弹框焦点，无返回值。 */
watch(() => props.mapPicking, async picking => { if (!picking && props.open) { await nextTick(); dialog.value?.focus(); } });
onMounted(() => { document.addEventListener('keydown', keydown); });
onBeforeUnmount(() => { document.removeEventListener('keydown', keydown); if (previous?.isConnected && dialog.value?.contains(document.activeElement)) previous.focus(); });
</script>
<template>
  <Teleport to="body" :disabled="mapPicking">
    <div v-show="open" class="modal-backdrop" :class="{ 'map-picking-backdrop': mapPicking }" @click.self="!mapPicking && !closeDisabled && emit('close')">
      <section ref="dialog" class="modal chibi-modal" :class="[{ wide }, panelClass]" :role="mapPicking ? 'region' : 'dialog'" :aria-modal="mapPicking ? undefined : true" :aria-label="title" tabindex="-1">
        <div v-if="!hideHeading" class="modal-heading"><h2>{{ title }}</h2><button class="icon-button" :aria-label="mapPicking ? '返回列表选择' : '关闭弹窗'" :disabled="closeDisabled" @click="emit('close')">✕</button></div>
        <div class="modal-content"><slot /></div>
        <div v-if="$slots.footer" class="modal-footer"><slot name="footer" /></div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-backdrop { padding: 24px; background: #52614980; backdrop-filter: blur(5px); }
/* 卡牌演出与历史回看共用全屏外壳，复用焦点约束及关闭行为。 */
.modal-backdrop:has(> .card-reveal-modal) { padding: 0; z-index: 200; background: #3b30294d; backdrop-filter: blur(12px) saturate(.8); -webkit-backdrop-filter: blur(12px) saturate(.8); }
.modal.chibi-modal.card-reveal-modal { width: 100%; height: 100dvh; max-height: 100dvh; padding: 0; border: 0; border-radius: 0; background: transparent; box-shadow: none; }
.card-reveal-modal > .modal-content { flex: 1; position: relative; margin: 0; padding: 0; overflow: hidden; scrollbar-gutter: auto; }
/* 公共外壳覆盖各页面旧的白色弹框，内容与业务操作仍由原组件承载。 */
.modal.chibi-modal { --dialog-padding: 24px; display: flex; flex-direction: column; min-width: 0; width: min(100%, 520px); max-height: calc(100dvh - 48px); overflow: hidden; padding: var(--dialog-padding); border: 3px solid #cfb58e; border-radius: 30px; background: #fff9eb; color: #705a43; box-shadow: inset 0 0 0 5px #fffdf5, 0 7px 0 #9c7c4e38, 0 24px 70px #43361c30; }
/* 滚动限制在正文内，给圆角边框及内容焦点、按钮阴影留出间距。 */
.modal-content { min-height: 0; min-width: 0; overflow: auto; overscroll-behavior: contain; scrollbar-width: thin; scrollbar-color: #cfb58e transparent; scrollbar-gutter: stable; margin: -5px; padding: 5px; }
.modal-content::-webkit-scrollbar { width: 6px; height: 6px; }
.modal-content::-webkit-scrollbar-track { background: transparent; }
.modal-content::-webkit-scrollbar-thumb { border-radius: 999px; background: #cfb58e; }
.modal-content :deep(> .button) { margin-top: 10px; }
.modal-footer { flex-shrink: 0; min-width: 0; margin-top: 18px; padding-top: 14px; border-top: 2px dashed #e5d5b7; }
.modal-backdrop.map-picking-backdrop { position: static; inset: auto; z-index: auto; display: flex; align-items: flex-start; min-width: 0; min-height: 0; padding: 0; background: none; backdrop-filter: none; overflow: hidden; }
.map-picking-backdrop .modal.chibi-modal { width: 100%; max-height: calc(100dvh - 100px); padding: 16px; border-radius: 22px; }
.map-picking-backdrop .chibi-modal > .modal-heading { margin-bottom: 12px; padding-bottom: 10px; }
.map-picking-backdrop .chibi-modal > .modal-heading h2 { font-size: 16px; }
.map-picking-backdrop .modal-footer { margin-top: 10px; padding-top: 10px; }
@media (max-width: 800px) {
  .map-picking-backdrop .modal.chibi-modal { max-height: 50dvh; padding: 12px; }
}
@media (max-width: 800px) and (max-height: 500px) {
  .map-picking-backdrop .modal.chibi-modal { max-height: calc(100dvh - 100px); }
}
.modal.chibi-modal.wide { width: min(100%, 920px); }
.modal.chibi-modal.home-archive-modal { width: min(100%, 960px); }
.modal.chibi-modal.game-tools-modal { width: min(100%, 420px); }
.chibi-modal > .modal-heading { position: relative; top: auto; flex-shrink: 0; z-index: 3; gap: 12px; margin: 0 0 20px; padding: 0 0 16px; border-bottom: 2px dashed #e5d5b7; background: #fff9eb; }
.chibi-modal > .modal-heading h2 { min-width: 0; margin: 0; color: #806344; font-size: 21px; font-weight: 800; line-height: 1.5; overflow-wrap: anywhere; }
.chibi-modal > .modal-heading .icon-button { width: 36px; height: 36px; flex-shrink: 0; border: 2px solid #dcc39a; border-radius: 14px; background: linear-gradient(#fff9e8, #f3e1bd); color: #967650; box-shadow: inset 0 2px 0 #fffdf5, 0 3px 0 #dcc39a70; font-size: 17px; }
.chibi-modal:focus-visible { outline: 3px solid #b5c99a; outline-offset: 5px; }
.chibi-modal :deep(.button) { min-height: 42px; padding: 10px 14px; border: 2px solid #d6c49e; border-radius: 15px; color: #8b7150; font-size: 12px; font-weight: 700; line-height: 1.5; box-shadow: inset 0 2px 0 #fffdf0, 0 3px 0 #d6c49e60; }
.chibi-modal :deep(.button.primary) { border-color: #93ad74; background: linear-gradient(#b8cf94, #9ab775); color: #344c2c; box-shadow: inset 0 2px 0 #e4efcf, 0 3px 0 #7e9a5c70; }
.chibi-modal :deep(.button.secondary), .chibi-modal :deep(.button.ghost) { border-color: #d9c49c; background: linear-gradient(#fffdf1, #faedcf); color: #8a704b; }
.chibi-modal :deep(.button.danger) { border-color: #d3a08b; background: linear-gradient(#fff0e7, #f6d7ca); color: #995d4e; box-shadow: inset 0 2px 0 #fff7ef, 0 3px 0 #d3a08b60; }
.chibi-modal :deep(.button:not(:disabled):hover), .chibi-modal > .modal-heading .icon-button:not(:disabled):hover { filter: brightness(1.03); transform: translateY(-1px); }
.chibi-modal :deep(.button:not(:disabled):active), .chibi-modal > .modal-heading .icon-button:not(:disabled):active { transform: translateY(2px); box-shadow: inset 0 2px 0 #fffdf080; }
.chibi-modal :deep(.button:disabled) { border-color: #d8d0bd; background: #eae5d8; color: #9b927f; box-shadow: inset 0 2px 0 #fff9e9; opacity: 1; transform: none; }
.chibi-modal :deep(.button.small) { min-height: 36px; padding: 7px 10px; font-size: 11px; }
.chibi-modal :deep(.button-row) { gap: 12px; flex-wrap: wrap; }
.chibi-modal :deep(.button-row > .button) { min-width: min(140px, 100%); }
.chibi-modal :deep(.field) { margin: 16px 0; color: #8c7457; font-size: 12px; }
.chibi-modal :deep(.field input), .chibi-modal :deep(.field select), .chibi-modal :deep(.field textarea) { min-height: 44px; max-width: 100%; padding: 10px 12px; border: 2px solid #e0cfaf; border-radius: 14px; background: #fffdf5; color: #705a43; font-size: 13px; box-shadow: inset 0 2px 3px #bfa47612; }
.chibi-modal :deep(.field input:focus), .chibi-modal :deep(.field select:focus), .chibi-modal :deep(.field textarea:focus) { border-color: #a5bb8b; background: #fffef8; }
.chibi-modal :deep(input[type='checkbox']), .chibi-modal :deep(input[type='range']) { accent-color: #8da96c; }
.chibi-modal :deep(.muted), .chibi-modal :deep(.optional) { color: #8c7c66; font-size: 12px; line-height: 1.8; }
.chibi-modal :deep(.notice) { padding: 12px 14px; border: 2px solid #c9d7b4; border-radius: 16px; background: #f0f6e5; color: #637a50; font-size: 12px; line-height: 1.8; }
.chibi-modal :deep(.notice.warning) { border-color: #e3c791; background: #fff2d5; color: #92703d; }
.chibi-modal :deep(.error-text) { color: #ad6655; }
/* 购买与升级复用地图房屋插画，金额和结果分别放进票券与信息卡。 */
.chibi-modal :deep(.landing-info) { gap: 16px; padding: 12px; border: 2px solid #e3d3b3; border-radius: 22px; background: #fffdf3; }
.chibi-modal :deep(.landing-info > .decision-property-art) { display: block; flex-shrink: 0; width: 82px; height: 82px; padding: 0; border-radius: 18px; background-color: #f5edda; background-position: 0 0; background-size: 200% 200%; background-repeat: no-repeat; }
.chibi-modal :deep(.decision-property-art.level-1) { background-position: 100% 0; }
.chibi-modal :deep(.decision-property-art.level-2) { background-position: 0 100%; }
.chibi-modal :deep(.decision-property-art.level-3) { background-position: 100% 100%; }
.chibi-modal :deep(.landing-info strong) { color: #775a3c; font-size: 18px; }
.chibi-modal :deep(.landing-info small) { margin-top: 6px; color: #998365; font-size: 12px; }
.chibi-modal :deep(.decision-price) { display: flex; align-items: baseline; flex-wrap: wrap; gap: 8px; margin: 18px 0; padding: 16px; border: 2px dashed #e2c385; border-radius: 18px; background: #fff0c9; color: #9b7132; font-size: 34px; font-weight: 800; font-variant-numeric: tabular-nums; }
.chibi-modal :deep(.decision-price small) { color: #9d8257; font-size: 12px; }
.chibi-modal :deep(.decision-outcome) { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin: 0 0 22px; padding: 0; border-radius: 0; background: transparent; }
.chibi-modal :deep(.decision-outcome > div) { min-width: 0; padding: 14px; border: 2px solid #c6d6ad; border-radius: 18px; background: #f0f6e2; box-shadow: inset 0 0 0 3px #fffdf580; }
.chibi-modal :deep(.decision-outcome > div + div) { border-color: #e4c88e; background: #fff4d9; }
.chibi-modal :deep(.decision-outcome dt) { color: #8b7e62; font-size: 11px; }
.chibi-modal :deep(.decision-outcome dd) { margin: 6px 0 0; color: #637c49; font-size: 22px; font-weight: 800; overflow-wrap: anywhere; }
/* 交易、资产、规则和选择卡片共享柔和边框，保留各自布局和选中含义。 */
.chibi-modal :deep(.trade-identity), .chibi-modal :deep(.archive-code-box), .chibi-modal :deep(.rules-goal), .chibi-modal :deep(.rules-reminder), .chibi-modal :deep(.rules-example) { border: 2px solid #d7c391; border-radius: 20px; background: #fff3d6; box-shadow: inset 0 0 0 3px #fffdf580; }
.chibi-modal :deep(.trade-side), .chibi-modal :deep(.panel), .chibi-modal :deep(.portfolio-card) { border: 2px solid #dfcfaf; border-radius: 20px; background: #fffdf4; box-shadow: inset 0 0 0 3px #fffaf0; }
.chibi-modal :deep(.trade-preview .trade-me) { border-color: #a5bf89; background: #f0f6e2; }
.chibi-modal :deep(.target-option), .chibi-modal :deep(.keep-card), .chibi-modal :deep(.avatar-option) { border: 2px solid #ddccaa; border-radius: 18px; background: #fffdf4; color: #7b664b; }
.chibi-modal :deep(.target-option.selected), .chibi-modal :deep(.keep-card.selected), .chibi-modal :deep(.avatar-option.selected) { border-color: #97b477; background: #edf5dd; box-shadow: inset 0 0 0 2px #fffdf5, 0 3px 0 #97b47740; }
.chibi-modal :deep(.target-option.unavailable) { opacity: .5; }
.chibi-modal :deep(.tabs) { gap: 6px; padding: 5px; border: 2px solid #dfcfad; border-radius: 18px; background: #f3ead7; }
.chibi-modal :deep(.tabs button) { border-radius: 12px; color: #8e7958; }
.chibi-modal :deep(.tabs button.active) { background: #fffdf1; color: #647f4d; box-shadow: 0 2px 0 #cfbc95; }
.chibi-modal :deep(.tile-detail-hero), .chibi-modal :deep(.tile-detail-title-deed), .chibi-modal :deep(.tile-detail-card .tile-detail-metrics > div), .chibi-modal :deep(.tile-detail-card > .tile-detail-description), .chibi-modal :deep(.tile-detail-card > .tile-detail-barrier) { border-width: 2px; border-style: solid; border-color: #e0cfae; border-radius: 18px; background-color: #fff3d9; }
.chibi-modal :deep(.tile-detail-title-deed) { border-color: color-mix(in srgb, var(--deed-color) 40%, #e0cfae); background: color-mix(in srgb, var(--deed-color) 8%, #fffdf1); }
.chibi-modal :deep(.tile-detail-card > .tile-detail-barrier.active) { border-color: #dfb97b; background: #fff0cc; }
.chibi-modal :deep(.audio-setting) { gap: 12px; margin-top: 12px; padding: 12px; border: 2px solid #dcc9e8; border-radius: 18px; background: #f7effb; }
.chibi-modal :deep(.audio-setting + .audio-setting) { border-color: #c4d6ad; background: #f0f6e3; }
@media (max-width: 600px) {
  .modal-backdrop { padding: 12px; }
  .modal.chibi-modal { --dialog-padding: 18px; max-height: calc(100dvh - 24px); border-radius: 24px; }
  .chibi-modal > .modal-heading h2 { font-size: 18px; }
  .chibi-modal :deep(.landing-info > .decision-property-art) { width: 64px; height: 64px; }
  .chibi-modal :deep(.decision-price) { font-size: 28px; }
  .chibi-modal :deep(.decision-outcome) { gap: 8px; }
  .chibi-modal :deep(.decision-outcome > div) { padding: 12px 10px; }
  .chibi-modal :deep(.decision-outcome dd) { font-size: 19px; }
  .chibi-modal :deep(.audio-setting) { flex-wrap: wrap; }
  .chibi-modal :deep(.audio-setting input) { flex-basis: 100%; }
}
</style>
