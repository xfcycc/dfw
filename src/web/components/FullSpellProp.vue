<script setup lang="ts">
import { computed } from 'vue';
import { fullSpellArt, type FullCardSpell } from '../full-card-spells';
const props = defineProps<{ card: FullCardSpell }>();
const style = computed(() => { const a=fullSpellArt(props.card); return { backgroundImage:`url("${a.url}")`,backgroundSize:`${a.columns*100}% ${a.rows*100}%`,backgroundPosition:`${a.cell%a.columns/(a.columns-1)*100}% ${Math.floor(a.cell/a.columns)/(a.rows-1)*100}%` }; });
</script>
<template><div class="full-spell-prop" :style="style" aria-hidden="true" /></template>
<style scoped>.full-spell-prop { width:100%; height:100%; background-repeat:no-repeat; }</style>
