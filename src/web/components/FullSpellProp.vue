<script setup lang="ts">
import { computed } from 'vue';
import { fullSpellArt, type FullCardSpell } from '../full-card-spells';
import AtlasArt from './AtlasArt.vue';
const props = defineProps<{ card: FullCardSpell }>();
// 按稳定卡牌身份读取单独的取图范围，不再统一收缩主体边缘。
const art = computed(() => fullSpellArt(props.card));
</script>
<template><div class="full-spell-prop" aria-hidden="true"><AtlasArt :url="art.url" :crop="art.crop" /></div></template>
<style scoped>.full-spell-prop { width:100%; height:100%; }</style>
