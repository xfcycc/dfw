<script setup lang="ts">
import { imageUrl } from '../image-assets';
import { computed } from 'vue';
import { AVATARS } from '../../shared/avatars';
const props = withDefaults(defineProps<{ avatarId?: string; color?: string; moving?: boolean; landing?: boolean; variant?: 'avatar' | 'pawn' }>(), { avatarId: 'explorer', color: '#58896a', variant: 'avatar' });
/** 输入公开角色标识，返回内置头像；旧存档缺失或未知标识沿用阿洛。 */
const avatar = computed(() => AVATARS.find(avatar => avatar.id === props.avatarId) ?? AVATARS[0]);
</script>
<template>
  <span class="character-pawn" :class="{ moving, landing, 'pawn-token': variant === 'pawn' }" :style="{ backgroundColor: color }" aria-hidden="true">
    <img :src="imageUrl(`/images/avatars/${avatar.id}.png`)" alt="" width="512" height="512" decoding="async" draggable="false" />
  </span>
</template>
<style scoped>
.character-pawn { display: inline-flex; align-items: center; justify-content: center; width: 38px; height: 38px; flex-shrink: 0; overflow: hidden; border: 2px solid #fff9ed; border-radius: 11px; vertical-align: middle; box-shadow: 0 2px 4px #243d3430; }
.character-pawn img { display: block; width: 100%; height: 100%; object-fit: contain; }
.pawn-token { border-radius: 50%; }
.pawn-token img { width: 86%; height: 86%; }
.moving { animation: pawn-hop var(--step-duration, 190ms) ease-in-out infinite; transform-origin: center bottom; }
.landing { animation: pawn-land .36s ease-out both; transform-origin: center bottom; }
@keyframes pawn-hop { 0%, 100% { transform: translateY(0) scale(1.03, .97); } 50% { transform: translateY(-12px) rotate(-4deg) scale(.97, 1.03); } }
@keyframes pawn-land { 0% { transform: scale(1.12, .86); } 45% { transform: translateY(-5px) scale(.96, 1.05); } 100% { transform: none; } }
@media (prefers-reduced-motion: reduce) { .moving, .landing { animation: none !important; } }
</style>
