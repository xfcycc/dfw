<script setup lang="ts">
import { ref, watch } from 'vue';
const props = defineProps<{ disabled?: boolean }>();
const area = ref<HTMLElement>();
const content = ref<HTMLElement>();
const scale = ref(1);
// 输入容器与自然内容元素，适配高度时补偿布局宽度，始终铺满中间可用区域。
watch([area, content, () => props.disabled], ([viewport, body, disabled], _, onCleanup) => {
  if (!viewport || !body) return;
  if (disabled) { scale.value = 1; body.style.width = ''; body.style.height = ''; return; }
  let frame = 0;
  /** 无入参，在完整显示内容的前提下确定最大比例；补偿缩放产生的左右空白。 */
  function fit(): void {
    if (!viewport || !body) return;
    const width = viewport.clientWidth; const height = viewport.clientHeight;
    if (!width || !height) return;
    /** 输入候选比例，补偿逻辑宽高，返回内容缩放后是否能完整容纳。 */
    function fits(value: number): boolean {
      body!.style.width = `${width / value}px`;
      body!.style.height = `${height / value}px`;
      return body!.offsetHeight * value <= height + 0.5;
    }
    let next = 1;
    if (!fits(1)) {
      let lower = 0; let upper = 1;
      for (let step = 0; step < 8; step++) {
        const candidate = (lower + upper) / 2;
        if (fits(candidate)) lower = candidate;
        else upper = candidate;
      }
      next = lower || upper;
      fits(next);
    }
    scale.value = next;
  }
  /** 无入参，将同一帧的尺寸变化合并处理，避免金额或卡面更新时反复重排。 */
  function scheduleFit(): void { cancelAnimationFrame(frame); frame = requestAnimationFrame(fit); }
  const observer = new ResizeObserver(scheduleFit);
  observer.observe(viewport);
  observer.observe(body);
  for (const panel of body.querySelectorAll('.workspace-panel')) observer.observe(panel);
  fit();
  onCleanup(() => { observer.disconnect(); cancelAnimationFrame(frame); });
}, { flush: 'post' });
</script>
<template>
  <div ref="area" class="fit-panel" :class="{ 'natural-panel': disabled }"><div ref="content" class="fit-panel-content" :style="{ transform: `scale(${scale})` }"><slot /></div></div>
</template>
<style scoped>
.fit-panel { position: relative; flex: 1; min-width: 0; min-height: 0; overflow: clip; }
.fit-panel-content { display: flex; flex-direction: column; width: 100%; height: 100%; min-height: min-content; transform-origin: top left; }
.natural-panel { flex: none; overflow: visible; }
.natural-panel .fit-panel-content { height: auto; }
</style>
