<script setup lang="ts">
import { computed, ref } from 'vue';
import { AVATARS } from '../../shared/avatars';
import { useGameStore } from '../stores/game';
import CharacterPawn from './CharacterPawn.vue';

type Category = 'all' | typeof AVATARS[number]['category'];
const categoryLabels = { all: '全部', animal: '动物', anime: '动漫', game: '游戏', other: '其他' } satisfies Record<Category, string>;
const category = ref<Category>('all');
const visibleAvatars = computed(() => category.value === 'all' ? AVATARS : AVATARS.filter(avatar => avatar.category === category.value));
const game = useGameStore();
/** 输入内置形象标识，通过原命令通道修改本人公开外观，返回值由服务端快照更新。 */
async function select(avatarId: string): Promise<void> { await game.act({ type: 'setAvatar', avatarId }); }
</script>
<template>
  <div class="avatar-selection">
    <div class="tabs avatar-categories" role="group" aria-label="头像分类">
      <button v-for="(label, value) in categoryLabels" :key="value" type="button" :class="{ active: category === value }" :aria-pressed="category === value" @click="category = value">{{ label }}</button>
    </div>
    <div :key="category" class="avatar-picker" role="group" aria-label="选择角色形象">
      <button
        v-for="avatar in visibleAvatars"
        :key="avatar.id"
        type="button"
        class="avatar-option"
        :class="{ selected: (game.me?.avatarId ?? 'explorer') === avatar.id }"
        :aria-pressed="(game.me?.avatarId ?? 'explorer') === avatar.id"
        :disabled="game.connection !== 'online' || game.busy || !game.me"
        @click="select(avatar.id)"
      >
        <CharacterPawn :avatar-id="avatar.id" :color="game.me?.color" />
        <span>{{ avatar.name }}</span>
      </button>
    </div>
  </div>
</template>
<style scoped>
.avatar-categories { margin: 0 0 8px; }
.avatar-categories button { min-height: 32px; padding: 5px 2px; font-size: 12px; white-space: nowrap; }
.avatar-categories button:not(.active) { color: #5f7052; }
.avatar-picker { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); align-content: start; gap: 8px; max-height: min(48dvh, 380px); overflow-y: auto; overscroll-behavior: contain; scrollbar-width: thin; }
.avatar-option .character-pawn { width: 64px; height: 64px; border-radius: 15px; }
.avatar-option { display: flex; align-items: center; flex-direction: column; gap: 5px; padding: 10px 4px; border: 2px solid #e3e6d5; border-radius: 11px; background: #fffdf3; color: #476551; font-size: 12px; }
.avatar-option.selected { border-color: #58896a; background: #ebf4de; }
</style>
