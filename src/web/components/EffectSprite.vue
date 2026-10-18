<script setup lang="ts">
import { effectSpriteUrl, type EffectSpriteKind } from '../effect-sprites';
defineProps<{ kind: EffectSpriteKind; reverse?: boolean }>();
</script>

<template>
  <span class="generated-effect" aria-hidden="true"><span class="generated-frames" :class="{ reverse }" :style="{ backgroundImage: `url(${effectSpriteUrl(kind)})` }"></span></span>
</template>

<style scoped>
.generated-effect { position:absolute; left:50%; top:50%; width:var(--vfx-size,180px); height:var(--vfx-size,180px); transform:translate(-50%,-50%); pointer-events:none; }
.generated-frames { display:block; width:100%; height:100%; background-repeat:no-repeat; background-size:400% 400%; animation:generated-frames 1000ms step-end infinite; }
.generated-frames.reverse { animation-direction:reverse; }
@keyframes generated-frames {
  0%,100% { background-position:0 0; }
  6.25% { background-position:33.333333% 0; }
  12.5% { background-position:66.666667% 0; }
  18.75% { background-position:100% 0; }
  25% { background-position:0 33.333333%; }
  31.25% { background-position:33.333333% 33.333333%; }
  37.5% { background-position:66.666667% 33.333333%; }
  43.75% { background-position:100% 33.333333%; }
  50% { background-position:0 66.666667%; }
  56.25% { background-position:33.333333% 66.666667%; }
  62.5% { background-position:66.666667% 66.666667%; }
  68.75% { background-position:100% 66.666667%; }
  75% { background-position:0 100%; }
  81.25% { background-position:33.333333% 100%; }
  87.5% { background-position:66.666667% 100%; }
  93.75% { background-position:100% 100%; }
}
@media(prefers-reduced-motion:reduce) { .generated-frames { animation:none; } }
</style>
