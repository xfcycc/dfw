<script setup lang="ts">
import { ITEMS } from '../../shared/rules';
defineProps<{ spectator?: boolean; chanceEvents?: string[] }>();
</script>
<template>
  <div class="rules-content">
    <h3>沿着城市，积累财富</h3><p>每人从 ¥6,000 开始。<span v-if="!spectator">开局获得两张随机道具。</span>顺时针前进，经过起点获得 ¥1,000。最后一名未破产的玩家获胜。</p>
    <h3>买地、建房、收路费</h3><p>走到无主地产可以买地；停在自有地产才可升一级，最高三级，每级造价为地价的一半。基础路费为地价的 10%，0～3 级分别乘以 1、3、6、10。新购入地产本次不立即建房。</p>
    <h3>现金周转</h3><p>卖一级房返还地价的 25%。卖完建筑后可抵押获得地价的 50%，赎回需支付 55%。抵押地不收路费。无建筑地产可卖出，未抵押返还 50%，已抵押不再返款。</p>
    <h3>偿债与休息</h3><p>资金不足但资产够用时，先卖房、抵押或卖地，筹足后自动支付；全部变现仍不足则清算淘汰。监狱和休息事件会跳过下一次行动。</p>
    <template v-if="!spectator"><h3>道具与交易</h3><div v-for="item in ITEMS" :key="item.name" class="rule-item"><span>{{ item.icon }}</span><p><strong>{{ item.name }}</strong> · {{ item.description }}</p></div><p>每回合掷骰前最多主动使用一张道具，免租在收费时单独使用。现金和地产可以双方确认交易，建筑与抵押状态随地产转移。</p></template>
    <template v-if="chanceEvents?.length"><h3>本局新增机会事件</h3><p>机会格保留原六种结果，并追加以下 {{ chanceEvents.length }} 种等权结果。每回合仍最多抽取一次事件，财富格保持原规则。</p><p v-for="description in chanceEvents" :key="description">{{ description }}</p></template>
    <h3>离线后怎么办</h3><p>刷新可回到原座位。轮到离线玩家时暂停等待，房主可移出离线玩家并清算。恢复身份依赖当前浏览器保存的令牌，请保留浏览器数据。</p>
  </div>
</template>
