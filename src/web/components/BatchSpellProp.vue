<script setup lang="ts">
import type { RegularSpellSample } from '../regular-spell-preview';
/** 输入第二批道具身份；输出统一描边的轻量主体。 */
defineProps<{ kind: RegularSpellSample['theme'] }>();
</script>

<template>
  <svg viewBox="0 0 120 120" fill="none" stroke="#59463e" stroke-width="3.5" stroke-linejoin="round" stroke-linecap="round" aria-hidden="true">
    <g v-if="kind === 'door'">
      <path d="M24 110V10H96V110Z" fill="#ec8bb0" /><path d="M33 110V20H87V110" fill="#654b79" />
      <g class="door-leaf"><path d="M33 20H87V110H33Z" fill="#f4abc6"/><path d="M41 29H79V64H41ZM41 73H79V100H41Z" stroke="#c76993"/><circle cx="77" cy="69" r="3" fill="#fbe0a2"/></g>
    </g>
    <g v-else-if="kind === 'copter'" fill="#e3b55e"><path d="M57 48H64V100H57Z"/><g class="rotor"><ellipse cx="60" cy="44" rx="51" ry="9" fill="#f4d990"/><path d="M10 44H110"/></g><circle cx="60" cy="44" r="9" fill="#bc894b"/></g>
    <g v-else-if="kind === 'cloud'" fill="#f8ce67"><path d="M14 79Q-3 55 24 48Q26 20 50 33Q69 10 86 37Q117 30 110 60Q128 88 93 91H29Q11 93 14 79Z"/><path d="M24 65Q40 48 48 63Q53 78 35 77M62 44Q82 34 86 53Q87 65 73 64M57 82Q65 67 89 77" stroke="#c3923d"/><path d="M22 89Q13 105 3 100M31 92Q23 113 12 110"/></g>
    <g v-else-if="kind === 'cloak'" fill="#a997c4"><path d="M44 17L60 27L76 17L108 53L90 65L84 48L96 108H24L36 48L30 65L12 53Z"/><path d="M44 17L60 60L76 17M60 60V108M33 84L48 91M86 82L72 93"/><path d="M43 67H77" stroke="#f7d78f" stroke-width="8"/></g>
    <g v-else-if="kind === 'groupon'"><rect x="28" y="5" width="64" height="110" rx="12" fill="#667580"/><rect x="34" y="19" width="52" height="79" rx="4" fill="#ddf1e7"/><path d="M50 12H70M55 106H65" stroke="#e9e7d9"/><g class="voucher"><path d="M17 41H103V52Q91 60 103 68V80H17V68Q29 60 17 52Z" fill="#ffe2a2"/><path d="M75 46V76" stroke-dasharray="3 5"/><path d="M38 68L59 48"/><circle cx="40" cy="50" r="5"/><circle cx="58" cy="68" r="5"/></g></g>
    <g v-else-if="kind === 'sap'" fill="#af7b50"><path d="M25 103L39 113L93 40Q111 13 92 8Q78 3 66 23Z"/><path d="M34 91L47 99M40 80L54 88M46 69L60 77M77 24L65 48" stroke="#efd1a1"/></g>
    <g v-else-if="kind === 'atField'" stroke="#ee964b" fill="#ffc06b33"><path d="M36 7H84L113 36V84L84 113H36L7 84V36Z"/><path d="M43 23H77L97 43V77L77 97H43L23 77V43Z"/><path d="M49 39H71L81 49V71L71 81H49L39 71V49Z" fill="#fff0b777"/></g>
    <g v-else-if="kind === 'vines'" stroke="#467b99"><path d="M29 109Q94 88 37 69Q5 49 77 30Q105 10 66 7M90 109Q25 91 84 66Q114 44 47 25" stroke="#69c2da" stroke-width="8"/><path d="M38 91Q8 90 15 71Q36 68 38 91ZM77 69Q104 80 111 57Q89 49 77 69ZM45 36Q21 39 23 17Q42 16 45 36Z" fill="#92d9d5"/></g>
    <g v-else-if="kind === 'substitute'"><path d="M33 28H88V103Q60 117 33 103Z" fill="#bd895e"/><ellipse cx="60" cy="28" rx="28" ry="12" fill="#e7c497"/><ellipse cx="60" cy="28" rx="15" ry="5"/><path d="M43 71V99M59 79L56 107M77 73L79 98"/><path d="M31 42H90V65H31Z" fill="#687f9b"/><rect x="42" y="44" width="36" height="19" rx="4" fill="#d6e2e4"/><path d="M55 56Q49 47 62 48Q72 54 61 59L53 60M90 49L110 70L90 64"/></g>
    <g v-else-if="kind === 'room'" stroke="#69b7ce" fill="#9de3ed44"><path d="M7 84A53 66 0 0 1 113 84Z"/><ellipse cx="60" cy="84" rx="53" ry="20"/><path d="M25 87Q24 20 60 18Q95 20 95 87M8 67Q60 84 112 67M22 39Q60 55 98 39M60 18V104"/><path d="M37 78H83L75 70M83 90H37L45 98" stroke="#fff"/></g>
    <g v-else-if="kind === 'staff'"><rect x="6" y="50" width="108" height="20" rx="4" fill="#c75e4f"/><path d="M8 50H29V70H8ZM91 50H112V70H91Z" fill="#edc367"/><path d="M16 54V66M24 54V66M98 54V66M106 54V66" stroke="#b7843d"/><path d="M35 55H85" stroke="#ef9a78"/></g>
    <g v-else-if="kind === 'banana'" fill="#f4d45f"><path d="M56 28L67 27L69 49Q78 72 109 81Q92 101 66 69Q67 95 48 109Q30 92 51 64Q31 87 8 76Q37 66 53 47Z"/><path d="M56 28L55 16L65 13L67 27" fill="#b08c52"/><path d="M57 52L49 88M65 51L88 80M50 55L29 71" stroke="#fff0aa"/></g>
  </svg>
</template>

<style scoped>
svg { display:block; width:100%; height:100%; overflow:visible; }
</style>
