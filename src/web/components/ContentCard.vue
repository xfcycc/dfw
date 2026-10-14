<script setup lang="ts">
import { computed } from 'vue';
import type { ContentResult } from '../../shared/types';
import { ITEMS } from '../../shared/rules';
const props = defineProps<{ content: ContentResult; compact?: boolean }>();
const kind = computed(() => props.content.kind ?? null);
const fallback = computed(() => kind.value ? ITEMS[kind.value] : null);
const timing = computed(() => kind.value === 'release' ? '限制施加时' : kind.value === 'immunity' || kind.value === 'halfPayment' ? '付款防御' : kind.value ? '掷骰前' : props.content.pool === 'chance' ? '机会事件' : '财富事件');
</script>
<template>
  <article class="content-card" :class="[{ compact }, `rarity-${content.rarity ?? 'R'}`]">
    <div class="content-card-heading"><span>{{ fallback?.icon ?? (content.pool === 'chance' ? '✦' : '🎁') }}</span><strong>{{ content.name ?? fallback?.name ?? '事件' }}</strong><small v-if="content.rarity" class="rarity-tag">{{ content.rarity }}</small></div>
    <small class="content-timing">{{ content.cardType ? `${content.cardType} · ` : '' }}{{ timing }}</small>
    <blockquote v-if="content.quote">“{{ content.quote }}”</blockquote>
    <p v-if="!compact">{{ content.description ?? fallback?.description }}</p>
    <p class="content-effect">{{ content.effect ?? fallback?.description }}</p>
  </article>
</template>
