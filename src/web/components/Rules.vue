<script setup lang="ts">
import { ITEMS, ruleMoney } from '../../shared/rules';
import type { EconomySettings } from '../../shared/types';
defineProps<{ spectator?: boolean; chanceEvents?: string[]; economy?: EconomySettings | null }>();
</script>
<template>
  <div class="rules-content">
    <h3>沿着城市，积累财富</h3><p>每人从 ¥{{ ruleMoney(economy, 'initialCash', 6000).toLocaleString() }} 开始。<span v-if="!spectator">开局获得两张随机道具。</span>顺时针前进，经过起点获得 ¥{{ ruleMoney(economy, 'startIncome', 1000).toLocaleString() }}。最后一名未破产的玩家获胜。</p>
    <h3>买地、建房、收路费</h3><p>走到无主地产可以买地；停在自有地产才可升一级，最高三级，每级默认造价为地价的一半，具体金额使用本局经济配置。本局基础路费为地价的 {{ economy?.rentBasePercent ?? 10 }}%，0～3 级分别乘以 1、3、6、10；可按地价和等级配置固定路费。新购入地产本次不立即建房。</p>
    <h3>公开资产榜</h3><p>所有玩家都能看到现金、净资产与名次。净资产按土地和建筑成本计算；抵押地产扣除固定赎回费用，单块地产剩余权益最低为零。相同净资产并列排名。</p>
    <h3>现金周转</h3><p>土地加建筑可整体抵押，获得其成本估值的 50%，保留建筑并暂停收租。赎回需付抵押时估值的 55%，拆房或转让不改变这笔费用。抵押期间不能单独卖房。未抵押建筑出售一级默认返还建设成本的一半；无建筑地产可卖出，未抵押默认返还地价的 50%，具体金额以本局配置为准，已抵押不再返款。</p>
    <h3>选择支付方式</h3><p>租金、罚款和全体扣款等强制费用，即使现金足够也先给支付选择。资产变现后由你手动确认现金付款。系统费用现金不足时自行选择地产抵押；玩家债务可向债权人提出资产抵债，也可卖资产给其他玩家筹款。抵扣金额由双方确认，可抵清或部分抵扣后继续处理余额，抵扣部分不转移现金。</p>
    <h3>清算与休息</h3><p>现金不足不会立即淘汰。有能力用现金和银行变现偿清时不能申请破产；仍不足时可以结束协商并确认清算，向银行欠款须先自行抵押可用地产。玩法选择、偿债和交易报价都不限时，拒绝或主动取消报价后继续原债务；相关玩家离线时等待重连。监狱和休息事件会跳过下一次行动。</p>
    <template v-if="!spectator"><h3>道具与交易</h3><div v-for="item in ITEMS" :key="item.name" class="rule-item"><span>{{ item.icon }}</span><p><strong>{{ item.name }}</strong> · {{ item.description }}</p></div><p>每回合掷骰前最多主动使用一张道具，免租在收费时单独使用。现金和地产可以双方确认交易，建筑及固定赎回费用随地产转移。</p></template>
    <template v-if="chanceEvents?.length"><h3>本局机会规则说明</h3><p>机会格按当前服务端事件池等权抽取。以下是其中 {{ chanceEvents.length }} 项公开说明；现金数额使用本局配置，每回合最多抽取一次机会或财富事件。</p><p v-for="description in chanceEvents" :key="description">{{ description }}</p></template>
    <h3>离线后怎么办</h3><p>刷新可回到原座位。轮到离线玩家时暂停等待，房主可移出离线玩家并清算。恢复身份依赖当前浏览器保存的令牌，请保留浏览器数据。</p>
  </div>
</template>
