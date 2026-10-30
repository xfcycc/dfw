<script setup lang="ts">
import { activeHoliday } from '../holiday-theme';
</script>

<template>
  <span v-if="activeHoliday" class="holiday-decorations" aria-hidden="true">
    <svg v-for="corner in ['left', 'right']" :key="corner" :class="`holiday-ornament holiday-ornament-${corner}`" viewBox="0 0 100 120" fill="none">
      <g v-show="activeHoliday === 'halloween'">
        <path d="M50 53q-7-17 4-24" stroke="#83926b" stroke-width="7" stroke-linecap="round" />
        <ellipse cx="50" cy="77" rx="39" ry="30" fill="#ef943f" stroke="#bb652c" stroke-width="2" />
        <ellipse cx="50" cy="77" rx="22" ry="30" stroke="#cf752f" stroke-width="2" />
        <path d="m26 73 10-10 6 12Zm32 2 6-12 10 10ZM32 88q18 20 36 0l-12 4-6-5-6 5Z" fill="#744337" />
        <path d="M18 24q8-11 16 0l6-9 6 9q8-11 16 0L46 35H34Z" fill="#75628b" />
        <path d="m77 42 2-6 2 6 6 2-6 2-2 6-2-6-6-2Z" fill="#ffe4a0" />
      </g>
      <g v-show="activeHoliday === 'christmas'">
        <path d="M50 0v25" stroke="#e6c98b" stroke-width="2" />
        <path d="m50 17 6 13 14 2-10 10 2 14-12-7-12 7 2-14-10-10 14-2Z" fill="#f5d78d" />
        <path d="m30 61 20-20 20 20H59l17 18H62l19 19H19l19-19H24l17-18Z" fill="#709985" stroke="#4c7769" stroke-width="2" stroke-linejoin="round" />
        <path d="M30 67q20 15 40 9M25 89q25 11 49 1" stroke="#efd99b" stroke-width="3" />
        <circle cx="42" cy="65" r="4" fill="#df8b7f" /><circle cx="61" cy="86" r="4" fill="#df8b7f" />
        <path d="M45 100h10v13H45Z" fill="#a57c5b" />
      </g>
      <g v-show="activeHoliday === 'spring'">
        <path d="M50 0v22M50 87v23" stroke="#ddb56b" stroke-width="3" />
        <rect x="37" y="21" width="26" height="7" rx="3" fill="#f0cd83" />
        <ellipse cx="50" cy="57" rx="32" ry="31" fill="#d96355" stroke="#b64c43" stroke-width="2" />
        <ellipse cx="50" cy="57" rx="17" ry="31" stroke="#edaf72" stroke-width="2" />
        <path d="m50 42 12 15-12 15-12-15Z" fill="#f4d68d" />
        <rect x="37" y="85" width="26" height="7" rx="3" fill="#f0cd83" />
        <path d="M43 105v12m7-12v15m7-15v12" stroke="#d96355" stroke-width="3" stroke-linecap="round" />
        <path d="m84 24 2-6 2 6 6 2-6 2-2 6-2-6-6-2Z" fill="#f4d68d" />
      </g>
    </svg>
  </span>
</template>
