<script setup lang="ts">
defineProps<{ barrier: boolean; meteorCharges: number; paused: boolean }>();
</script>

<template>
  <span class="tile-trap-marker" :class="{ 'is-paused': paused }" aria-hidden="true">
    <span v-if="barrier" class="trap-symbol barrier-symbol">
      <svg viewBox="0 0 40 40" fill="none">
        <path d="M10 22V34M30 22V34M6 34H14M26 34H34" stroke="#fff8e6" stroke-width="7" stroke-linecap="round" />
        <path d="M10 22V34M30 22V34M6 34H14M26 34H34" stroke="#6b4830" stroke-width="3" stroke-linecap="round" />
        <rect x="3" y="10" width="34" height="15" rx="3" fill="#ffd45e" stroke="#fff8e6" stroke-width="5" />
        <path d="M6 23L17 12H24L13 23H6ZM26 23L35 14V21L33 23H26Z" fill="#62432f" />
        <rect x="3" y="10" width="34" height="15" rx="3" stroke="#80502c" stroke-width="2" />
        <path d="M10 6V8M30 6V8" stroke="#e78b27" stroke-width="3" stroke-linecap="round" />
      </svg>
    </span>
    <span v-if="meteorCharges > 0" class="trap-symbol meteor-symbol">
      <svg viewBox="0 0 40 40" fill="none">
        <path d="M35 4L31 20L36 17C34 29 25 36 15 35C5 34 2 26 7 18L19 6L18 15L27 5L25 14L35 4Z" fill="#f0782f" stroke="#fff8e6" stroke-width="4" stroke-linejoin="round" />
        <path d="M35 4L31 20L36 17C34 29 25 36 15 35C5 34 2 26 7 18L19 6L18 15L27 5L25 14L35 4Z" stroke="#ad482d" stroke-width="1.5" stroke-linejoin="round" />
        <path d="M27 16L22 27L12 31L10 22L19 16L17 24L27 16Z" fill="#ffd565" />
        <path d="M10 21L18 19L24 25L22 32L14 35L7 30L6 25L10 21Z" fill="#775069" stroke="#593a50" stroke-width="1.5" />
        <path d="M10 23L15 22L18 26L14 29L9 28L10 23Z" fill="#b88a94" />
        <path d="M19 29L21 28M11 32L14 33" stroke="#efaa70" stroke-width="2" stroke-linecap="round" />
      </svg>
      <b class="trap-count">{{ meteorCharges }}</b>
    </span>
    <span v-if="paused" class="trap-paused">Ⅱ</span>
  </span>
</template>

<style scoped>
.tile-trap-marker { position:relative; display:flex; flex-direction:var(--trap-direction,row); align-items:center; gap:1px; width:max-content; pointer-events:none; filter:drop-shadow(0 1px 2px #66402c50); }
.trap-symbol { position:relative; display:block; flex:none; width:var(--trap-size,26px); height:var(--trap-size,26px); }
.trap-symbol svg { display:block; width:100%; height:100%; overflow:visible; }
.trap-count { position:absolute; right:-2px; bottom:-1px; min-width:13px; padding:0 3px; border:1px solid #fff6de; border-radius:7px; background:#9e3e29; color:#fff9e8; font-size:10px; font-weight:900; line-height:14px; text-align:center; font-variant-numeric:tabular-nums; }
.is-paused .trap-symbol { filter:grayscale(.8); opacity:.65; }
.trap-paused { position:absolute; top:-4px; left:-3px; padding:0 3px; border:1px solid #fff6de; border-radius:4px; background:#6d5982; color:#fff; font-size:9px; line-height:12px; }
</style>
