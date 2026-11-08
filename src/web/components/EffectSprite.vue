<script setup lang="ts">
import { effectSpriteUrl, type EffectSpriteKind } from '../effect-sprites';
import { computed } from 'vue';
import { effectFrameCrops } from '../effect-atlas-crops';
import AtlasArt from './AtlasArt.vue';
const props = defineProps<{ kind: EffectSpriteKind; reverse?: boolean }>();
// 陨星和宝藏使用逐帧隔离范围；其余无串格的图集保留原播放和取图方式。
const frames = computed(() => props.kind === 'meteor' || props.kind === 'treasure' ? effectFrameCrops(props.kind) : undefined);
</script>

<template>
  <span class="generated-effect" aria-hidden="true"><span class="generated-frames" :class="{ reverse, isolated: frames }" :style="frames ? undefined : { backgroundImage: `url(${effectSpriteUrl(kind)})` }"><template v-if="frames"><AtlasArt v-for="(frame,index) in frames" :key="index" class="generated-frame" :url="effectSpriteUrl(kind)" :crop="frame" /></template></span></span>
</template>

<style scoped>
.generated-effect { position:absolute; left:50%; top:50%; width:var(--vfx-size,180px); height:var(--vfx-size,180px); transform:translate(-50%,-50%); pointer-events:none; overflow:hidden; }
.generated-frames { display:block; width:100%; height:100%; background-repeat:no-repeat; background-size:400% 400%; animation:generated-frames 1000ms step-end infinite; }
.generated-frames.isolated { height:1600%; animation-name:isolated-frames; }
.generated-frame { height:6.25%; }
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
@keyframes isolated-frames {
  0%,100% { transform:translateY(0); }
  6.25% { transform:translateY(-6.25%); }
  12.5% { transform:translateY(-12.5%); }
  18.75% { transform:translateY(-18.75%); }
  25% { transform:translateY(-25%); }
  31.25% { transform:translateY(-31.25%); }
  37.5% { transform:translateY(-37.5%); }
  43.75% { transform:translateY(-43.75%); }
  50% { transform:translateY(-50%); }
  56.25% { transform:translateY(-56.25%); }
  62.5% { transform:translateY(-62.5%); }
  68.75% { transform:translateY(-68.75%); }
  75% { transform:translateY(-75%); }
  81.25% { transform:translateY(-81.25%); }
  87.5% { transform:translateY(-87.5%); }
  93.75% { transform:translateY(-93.75%); }
}
@media(prefers-reduced-motion:reduce) { .generated-frames { animation:none; } }
</style>
