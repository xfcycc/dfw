<script setup lang="ts">
import { AVATARS } from '../../shared/avatars';
import { useGameStore } from '../stores/game';
import CharacterPawn from './CharacterPawn.vue';
const game = useGameStore();
/** 输入内置形象标识，通过原命令通道修改本人公开外观，返回值由服务端快照更新。 */
async function select(avatarId: string): Promise<void> { await game.act({ type: 'setAvatar', avatarId }); }
</script>
<template><div class="avatar-picker" role="group" aria-label="选择角色形象"><button v-for="avatar in AVATARS" :key="avatar.id" class="avatar-option" :class="{ selected: (game.me?.avatarId ?? 'explorer') === avatar.id }" :aria-pressed="(game.me?.avatarId ?? 'explorer') === avatar.id" :disabled="game.connection !== 'online' || game.busy || !game.me" @click="select(avatar.id)"><CharacterPawn :avatar-id="avatar.id" :color="game.me?.color" /><span>{{ avatar.name }}</span></button></div></template>
<style scoped>
.avatar-picker { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 8px; }
.avatar-option .character-pawn { width: 64px; height: 64px; border-radius: 15px; }
.avatar-option { display: flex; align-items: center; flex-direction: column; gap: 5px; padding: 10px 4px; border: 2px solid #e3e6d5; border-radius: 11px; background: #fffdf3; color: #476551; font-size: 12px; }
.avatar-option.selected { border-color: #58896a; background: #ebf4de; }
</style>
