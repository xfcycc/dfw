<script setup lang="ts">
import { activeHoliday } from '../holiday-theme';
defineProps<{ rolling: boolean }>();
</script>

<template>
  <span v-if="activeHoliday" class="holiday-dice-effects" :class="rolling ? 'is-rolling' : 'is-settled'" aria-hidden="true">
    <i class="holiday-dice-halo"></i>
    <i v-for="spark in 6" :key="spark" class="holiday-dice-spark" :style="{ '--spark-index': spark }"></i>
  </span>
</template>

<style scoped>
.holiday-dice-effects{position:absolute;inset:-25px;pointer-events:none;color:var(--holiday-dice-spark);z-index:1}
.holiday-dice-halo{position:absolute;inset:12%;border:2px solid currentColor;border-radius:50%;opacity:0;box-shadow:0 0 18px var(--holiday-dice-glow),inset 0 0 12px var(--holiday-dice-glow)}
.is-settled .holiday-dice-halo{animation:holiday-dice-land .5s ease-out both}
.holiday-dice-spark{position:absolute;left:50%;top:50%;font-style:normal;font-size:18px;line-height:1;text-shadow:0 0 8px var(--holiday-dice-glow);opacity:0}
.holiday-dice-spark::before{content:var(--holiday-dice-symbol)}
.holiday-dice-spark:nth-of-type(2){--spark-x:-58px;--spark-y:-36px}
.holiday-dice-spark:nth-of-type(3){--spark-x:12px;--spark-y:-65px}
.holiday-dice-spark:nth-of-type(4){--spark-x:58px;--spark-y:-24px}
.holiday-dice-spark:nth-of-type(5){--spark-x:48px;--spark-y:44px}
.holiday-dice-spark:nth-of-type(6){--spark-x:-10px;--spark-y:60px}
.holiday-dice-spark:nth-of-type(7){--spark-x:-58px;--spark-y:26px}
.is-rolling .holiday-dice-spark{animation:holiday-dice-spark .75s ease-out calc(var(--spark-index) * -.11s) infinite}
.is-settled .holiday-dice-spark{animation:holiday-dice-spark .55s ease-out both}
@keyframes holiday-dice-spark{0%{opacity:0;transform:translate(-50%,-50%) scale(.3) rotate(0)}25%{opacity:.95}100%{opacity:0;transform:translate(calc(var(--spark-x) - 50%),calc(var(--spark-y) - 50%)) scale(.85) rotate(60deg)}}
@keyframes holiday-dice-land{from{opacity:.8;transform:scale(.45)}to{opacity:0;transform:scale(1.9)}}
@media(prefers-reduced-motion:reduce){.holiday-dice-effects{display:none}}
</style>
