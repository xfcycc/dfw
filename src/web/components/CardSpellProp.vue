<script setup lang="ts">
/** 输入道具身份，绘制同一粗描边 Q 版风格的轻量矢量素材。 */
import type { RegularSpellSample } from '../regular-spell-preview';
import BatchSpellProp from './BatchSpellProp.vue';
defineProps<{ kind: RegularSpellSample['theme'] }>();
</script>

<template>
  <BatchSpellProp v-if="!['concrete', 'slowBeam', 'snatch', 'repair', 'coupon', 'thunder'].includes(kind)" :kind="kind" />
  <svg v-else viewBox="0 0 120 120" class="card-spell-prop" aria-hidden="true" fill="none" stroke="#59463e" stroke-width="4" stroke-linecap="round" stroke-linejoin="round">
    <g v-if="kind === 'concrete'">
      <path d="M19 99L40 24Q60 10 80 24L101 99Z" fill="#f5a14c" /><path d="M31 60H89L94 79H26Z" fill="#fff5db" /><path d="M38 33Q60 46 82 33L80 24Q60 11 40 24Z" fill="#9faaa5" /><path d="M47 26L57 30M67 25L72 29" stroke="#d8e0d9" /><rect x="9" y="96" width="102" height="15" rx="7" fill="#67796e" /><path d="M43 48L35 86" stroke="#ffcd8a" />
    </g>
    <g v-else-if="kind === 'slowBeam'" stroke="#944c91">
      <ellipse cx="25" cy="60" rx="9" ry="17" fill="#ffc2eb" /><ellipse cx="57" cy="60" rx="13" ry="28" fill="#ffd8f0" /><ellipse cx="95" cy="60" rx="17" ry="41" fill="#fff0fa" /><path d="M7 60H106" stroke="#ed83c8" stroke-width="7" /><path d="M87 38Q101 45 102 60" stroke="#fff" />
    </g>
    <g v-else-if="kind === 'snatch'">
      <path d="M8 65H39L60 46Q69 40 73 46L89 39Q98 38 99 47L86 55L103 50Q112 51 109 60L91 67L103 64Q113 66 108 75L87 85Q73 98 51 90L36 87H8Z" fill="#fff0ce" /><path d="M42 68L61 59Q71 56 74 63L62 75M83 56L91 52M85 69L93 65" /><path d="M7 64H32V91H7" fill="#8aa8bd" /><path d="M15 69V86" stroke="#d7ebf2" />
    </g>
    <g v-else-if="kind === 'repair'">
      <path d="M19 105L85 32L94 40L28 113Z" fill="#865a42" /><path d="M76 42L86 31L95 39L85 51Z" fill="#f4d18e" /><path d="M90 8L96 23L112 26L97 33L94 49L86 34L70 30L84 23Z" fill="#ffe18a" stroke="#b78545" /><path d="M38 21V37M30 29H46M103 65V77M97 71H109" stroke="#c99a47" /><path d="M25 99L64 56" stroke="#b98a63" />
    </g>
    <g v-else-if="kind === 'coupon'">
      <path d="M51 66L105 17Q113 12 110 22L74 78Z" fill="#d5e5e8" /><path d="M54 56L106 105Q115 113 104 110L65 81Z" fill="#edf4ef" /><circle cx="62" cy="70" r="7" fill="#aabdc1" /><ellipse cx="33" cy="47" rx="18" ry="15" transform="rotate(28 33 47)" fill="#f79293" /><ellipse cx="32" cy="92" rx="19" ry="15" transform="rotate(-24 32 92)" fill="#f79293" /><ellipse cx="33" cy="47" rx="9" ry="7" fill="#fff5e4" /><ellipse cx="32" cy="92" rx="10" ry="7" fill="#fff5e4" />
    </g>
    <g v-else>
      <path d="M61 7L73 40L65 58L52 58L45 40Z" fill="#b5c8cd" /><path d="M44 34L23 45L13 70L43 57L52 47M76 34L97 45L107 70L77 57L68 47" fill="#91a7af" /><path d="M53 56H67V93H53Z" fill="#b88a5c" /><path d="M54 65L66 69M54 76L66 80M54 87L66 91" /><circle cx="60" cy="104" r="9" fill="#f5cf74" /><path d="M60 17V43" stroke="#f1f7f6" />
    </g>
  </svg>
</template>

<style scoped>
.card-spell-prop { display:block; width:100%; height:100%; overflow:visible; }
</style>
