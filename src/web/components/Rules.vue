<script setup lang="ts">
import { computed, ref } from 'vue';
import { buildCost, ITEMS, rentFor, RULES, ruleMoney } from '../../shared/rules';
import type { EconomySettings, Tile } from '../../shared/types';

const props = defineProps<{ spectator?: boolean; chanceEvents?: string[]; economy?: EconomySettings | null }>();
const version = ref<'brief' | 'detailed'>('brief');
// 当前补给池没有旧版停留卡，速查只展示本局可获得的道具。
const availableItems = Object.entries(ITEMS).filter(([kind]) => kind !== 'stay').map(([, item]) => item);
/** 根据本局经济配置生成示例地产及各级路费，固定金额覆盖与实际玩法保持一致。 */
const example = computed(() => {
  const tile: Tile = { id: 0, type: 'property', name: '示例地产', district: 0, price: ruleMoney(props.economy, 'landPrice.1', 600), ownerId: null, level: 0, mortgaged: false, barrier: false };
  return { price: tile.price, build: buildCost(tile, props.economy), rents: RULES.rentMultipliers.map((_, level) => rentFor({ ...tile, level }, props.economy)) };
});
</script>
<template>
  <div class="rules-content">
    <div class="tabs rules-switch" role="group" aria-label="玩法说明版本">
      <button :class="{ active: version === 'brief' }" :aria-pressed="version === 'brief'" @click="version = 'brief'">简略版 · 局内玩法</button>
      <button :class="{ active: version === 'detailed' }" :aria-pressed="version === 'detailed'" @click="version = 'detailed'">详细版 · 查看规则</button>
    </div>
    <div class="rules-goal">
      <h3>买地收路费，成为最后的赢家</h3>
      <p>2～6 人轮流行动。<strong>最后一名未破产的玩家获胜</strong>，资产榜第一名只是当前领先。</p>
      <div class="rules-facts"><span>起始现金 <strong>¥{{ ruleMoney(economy, 'initialCash', 6000).toLocaleString() }}</strong></span><span>向前经过起点 <strong>+¥{{ ruleMoney(economy, 'startIncome', 1000).toLocaleString() }} ＋1 张道具</strong></span><span>开局手牌 <strong>2 张</strong></span></div>
    </div>

    <div v-if="version === 'brief'">
      <p class="rules-intro"><strong>用道具（可选） → 掷骰移动 → 处理落点 → 管理资产 / 交易 → 结束回合。</strong></p>
      <ol class="rules-steps">
        <li><strong>买地建房</strong><p>停在无人地产可买地，以后停在自己的未抵押地产可建房，最高 3 级，刚买下本次不能建房。别人停在你的未抵押地产要付路费，建房通常能提高路费。</p></li>
        <li><strong>卖房卖地</strong><p>在「我的地产」可逐级卖出未抵押房屋，卖完房才能卖地。卖出可回收部分现金，已抵押空地卖出不再返款。</p></li>
        <li><strong>玩家交易</strong><p>现金和地产可以互换，价格双方商量，对方接受才成交。建筑和抵押状态随地产转移；欠玩家的钱，也可协商用地产抵债。</p></li>
        <li><strong>抵押赎回</strong><p>土地和建筑可整体抵押换现金，建筑保留，但暂停收租、建房，不能单独卖房。有钱后可付费赎回，恢复收租。</p></li>
        <li><strong>使用道具</strong><p>掷骰前每回合最多用一张主动道具，可控制移动、升级、赚钱或影响对手。免租、付款减半和解控在对应提示时使用，不占主动次数。</p></li>
        <li><strong>事件与休息</strong><p>机会 / 财富格触发奖励、费用或其他事件，按卡面提示处理。落入监狱会休息一次，有解控卡可当场解除。</p></li>
      </ol>
      <div class="rules-reminder">
        <h3>钱不够时，先筹钱</h3>
        <p><strong>先选防御 → 抵押 / 卖资产筹钱 → 确认付款。</strong>现金不足不会立即出局，欠玩家的钱还可协商抵债。</p>
      </div>
      <p class="rules-tip">金额看地块和操作提示，具体计算与限制见「详细版」。</p>
    </div>

    <div v-else>
      <p class="rules-intro">金额使用本局配置；具体费用和道具参数以地块详情、卡面及操作提示为准。</p>
      <section>
        <h3>1. 一回合怎么走</h3>
        <p><strong>掷骰前用道具（可跳过） → 掷骰 / 道具移动 → 处理落点 → 管理资产或交易（可跳过） → 结束回合。</strong></p>
        <ul class="rules-list"><li>通常顺时针移动。向前经过或到达起点，获得 ¥{{ ruleMoney(economy, 'startIncome', 1000).toLocaleString() }}，并补给一张道具；超过手牌上限时先整理手牌，再继续落点结算。后退经过起点不领钱或补给。</li><li>普通移动只处理最终落点；路障会让首个经过者提前停下，路障随即消失。带有路障免疫状态时会继续通过，路障保留。</li><li>玩法选择、偿债和交易报价都不限时。先处理当前待办，才能继续后续操作。</li></ul>
      </section>
      <section>
        <h3>2. 买地、建房和收路费</h3>
        <ul class="rules-list"><li><strong>买地：</strong>停在无主地产时，可支付地价购买，也可放弃。本次买下后不立即建房。</li><li><strong>普通建房：</strong>停在自己的未抵押地产时，可付费升一级，最高 3 级。每块地产每回合最多升级一次，免费升级也计入。</li><li><strong>免费升级：</strong>道具或事件可选择合法的自家地产，无需停在该地块；已抵押、满级或本回合已升级的地产不可选。</li><li><strong>收路费：</strong>别人停在你的未抵押地产时付费。基础路费为地价的 {{ economy?.rentBasePercent ?? 10 }}%（向下取整），0～3 级依次乘以 1、3、6、10；本局若配置了固定路费，以固定金额为准。</li><li><strong>建房费用：</strong>每级默认为地价的一半；免费建成的房屋也按正常建设成本计入资产。</li></ul>
        <div class="rules-example">
          <p><strong>本局示例：</strong>地价 ¥{{ example.price.toLocaleString() }}，每升一级花 ¥{{ example.build.toLocaleString() }}。</p>
          <table><caption>这块示例地产的各级路费</caption><thead><tr><th scope="col">等级</th><th scope="col">0 级</th><th scope="col">1 级</th><th scope="col">2 级</th><th scope="col">3 级</th></tr></thead><tbody><tr><th scope="row">路费</th><td v-for="(rent, level) in example.rents" :key="level">¥{{ rent.toLocaleString() }}</td></tr></tbody></table>
        </div>
      </section>
      <section>
        <h3>3. 需要付款时，按这个顺序处理</h3>
        <ol class="rules-list"><li><strong>先选防御：</strong>免租只免本次路费；付款减半可用于路费、罚款等强制费用。每笔至多选一种，也可不使用。防御不占主动道具次数。</li><li><strong>再筹钱：</strong>现金不足时管理资产。向银行付款，可自行选择地产抵押，也可出售未抵押建筑或空地；欠玩家的钱还可向债权人提出地产抵债，或向其他玩家出售资产筹款。</li><li><strong>最后确认支付：</strong>即使现金足够，也要先完成防御选择，再手动确认现金付款；变现不会自动替你还债。全额减免或全额抵债后，这笔费用直接结清。</li></ol>
        <p class="rules-tip">付款减半的减免额向下取整：应付 ¥301 时减免 ¥150，仍付 ¥151。房东只收到减免后的实际路费。</p>
        <p>若已有「下次免付款」状态，会自动免除本笔费用；若有「下次租金翻倍」状态，先对原路费翻倍，再处理减免。</p>
      </section>
      <section>
        <h3>4. 现金不足：抵押、卖出与赎回</h3>
        <ul class="rules-list"><li><strong>整体抵押：</strong>土地和建筑一起抵押，取得成本估值的 50%。建筑保留，暂停收租、建房，抵押期间不能单独卖房。</li><li><strong>赎回：</strong>支付抵押时成本估值的 55%，恢复收租。赎回费用在抵押时固定，之后拆房或转让都不改变它；费用结算期间不能赎回。</li><li><strong>卖房：</strong>未抵押建筑可逐级出售，每级默认返还建设成本的一半，实际金额按本局配置。</li><li><strong>卖地：</strong>先卖完建筑才能卖地。未抵押空地默认返还地价的 50%，实际金额按本局配置；已抵押空地卖出不再返款。</li></ul>
        <p class="rules-tip">例如：抵押时土地加建筑估值 ¥2,000，可拿到 ¥1,000，之后赎回需 ¥1,100。</p>
      </section>
      <section>
        <h3>5. 交易和资产抵债</h3>
        <ul class="rules-list"><li>在自己掷骰前、落点处理后，或处理玩家债务时，可发起现金与地产报价。对方接受后才成交；拒绝或发起人取消后继续原流程。</li><li>地产上的建筑、抵押状态及固定赎回费用随地产一起转移。</li><li>资产抵债须向债权人提供地产，抵扣金额由双方确认，可抵清或只抵一部分。抵扣部分不转移现金，剩余欠款继续处理。</li><li>银行费用不能通过玩家抵债报价结算。</li></ul>
      </section>
      <section>
        <h3>6. 道具什么时候用</h3>
        <ul class="rules-list"><li><strong>主动道具：</strong>自己每回合掷骰前最多使用一张。固定步数、主动位移、遥控骰子会替代掷骰；加码与骰子合成一次移动，只结算最终落点。换位后仍正常掷骰。</li><li><strong>防御 / 解控：</strong>免租、付款减半在费用提示时使用；解控在监禁或休息刚施加时使用。它们都不占主动次数，也不能补回已跳过的行动。</li><li><strong>取消与无目标：</strong>免费升级卡在确认前取消，会保留卡和主动次数；没有合法升级目标，或抢夺的对手没有手牌时，也保留对应卡与次数。</li><li><strong>手牌上限：</strong>最多保留 6 张。超过时由本人从原牌和全部补给中选留 6 张，其余每张折现 ¥{{ ruleMoney(economy, 'handDiscard', 200).toLocaleString() }}。具体卡面仅本人可见。</li></ul>
        <template v-if="!spectator"><h4>道具速查</h4><div v-for="item in availableItems" :key="item.name" class="rule-item"><span aria-hidden="true">{{ item.icon }}</span><p><strong>{{ item.name }}</strong> · {{ item.description }}</p></div></template>
      </section>
      <section>
        <h3>7. 机会、财富和休息</h3>
        <ul class="rules-list"><li>机会与财富可能给钱、扣款、补给手牌、移动棋子或施加状态，按实际卡面处理。稀有度只影响卡面，不代表事件概率。</li><li>每回合最多抽一次机会或财富事件；事件让你移动到另一事件格时，不再次抽取。本局停用财富的「再次行动」模板。</li><li>免费升级事件可以放弃；仅确实没有合法地产时，改给现金。赌博也可以放弃，参加时只能用已有现金下注。</li><li>排名事件在开始时锁定净资产最高或最低者，并列时随机选一人。灾害按实际建筑损失判定补偿，已抵押地产也可能受损。</li><li>落入监狱会跳过下次行动；其他休息事件按卡面次数跳过。有解控卡时可在限制刚施加时选择解除。</li></ul>
        <template v-if="chanceEvents?.length"><h4>本局补充说明</h4><ul class="rules-list"><li v-for="description in chanceEvents" :key="description">{{ description }}</li></ul></template>
      </section>
      <section>
        <h3>8. 资产榜、破产和胜利</h3>
        <ul class="rules-list"><li><strong>公开资产榜：</strong>所有人可查看现金、净资产和名次。净资产 = 现金 + 土地及建筑成本；抵押地产扣除固定赎回费用，单块地产剩余权益最低为零。同净资产并列。</li><li><strong>现金不足不等于破产：</strong>现金加银行可变现资产足以偿清时，不能申请清算。仍不足时，可结束协商并确认清算；欠银行的钱须先自行抵押全部可用地产。</li><li><strong>清算后：</strong>退出竞争，现金、手牌和状态清空，地产归还银行、建筑清除。可以继续观看朋友的对局。</li><li><strong>胜利条件：</strong>只剩一名未破产玩家时，本局结束，该玩家获胜。</li></ul>
      </section>
      <section>
        <h3>9. 断线或刷新怎么办</h3>
        <p>使用同一浏览器刷新或重新输入房间号，可恢复原座位，请保留浏览器数据。轮到离线玩家处理行动、选择或报价时，会暂停等待重连；房主可移出离线玩家并清算。</p>
      </section>
    </div>
  </div>
</template>
<style scoped>
.rules-switch { margin: 0 0 18px; }
.rules-switch button { font-size: 13px; padding: 12px 6px; }
.rules-goal, .rules-reminder, .rules-example { background: #f3f6ed; border: 1px solid #e3e9d9; border-radius: 13px; padding: 16px; }
.rules-content h3 { line-height: 1.5; }
.rules-goal h3, .rules-reminder h3 { margin: 0 0 8px; }
.rules-content p, .rules-list, .rules-steps { font-size: 13px; color: #617457; line-height: 1.85; }
.rules-content strong, .rules-content b { color: #45604c; }
.rules-goal p, .rules-steps p { margin: 6px 0 0; }
.rules-facts { display: flex; flex-wrap: wrap; gap: 8px 18px; margin-top: 12px; font-size: 12px; color: #617457; }
.rules-facts strong { white-space: nowrap; }
.rules-intro { margin: 18px 0; }
.rules-steps, .rules-list { padding-left: 22px; }
.rules-steps > li { margin-bottom: 18px; padding-left: 4px; }
.rules-steps > li::marker { color: #56744b; font-weight: 700; }
.rules-list { margin: 8px 0 12px; }
.rules-list li + li { margin-top: 7px; }
.rules-reminder .rules-list { margin-bottom: 0; }
.rules-content .rules-tip { margin: 14px 0; font-size: 12px; }
.rules-example { margin: 14px 0; padding: 12px; }
.rules-example p { margin: 0 0 8px; }
.rules-example table { width: 100%; border-collapse: collapse; font-size: 12px; color: #45604c; }
.rules-example caption { text-align: left; font-size: 11px; color: #617457; padding-bottom: 8px; }
.rules-example th, .rules-example td { padding: 8px 3px; text-align: center; border-top: 1px solid #e3e9d9; }
.rules-content h4 { font-size: 13px; color: #56744b; margin: 16px 0 10px; }
.rule-item { align-items: flex-start; }
.rule-item > span { flex-shrink: 0; padding-top: 3px; }
@media (max-width: 480px) {
  .rules-switch button, .rules-example table { font-size: 11px; }
  .rules-goal, .rules-reminder { padding: 13px; }
  .rules-facts { gap: 7px 12px; }
}
</style>
