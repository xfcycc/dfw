<script setup lang="ts">
import { computed, ref } from 'vue';
import { buildCost, initialCards, initialCash, rentFor, RULES, ruleMoney, startCards } from '../../shared/rules';
import type { EconomySettings, MapSize, Tile } from '../../shared/types';

const props = defineProps<{ spectator?: boolean; chanceEvents?: string[]; economy?: EconomySettings | null; playerCount: number; mapSize?: MapSize; activityMoney?: Record<string, number> | null }>();
const version = ref<'brief' | 'detailed'>('brief');
/** 输入本局经济配置，返回五档地价的单级建造费及 0～3 级基础租金，包含固定金额覆盖。 */
const propertyPrices = computed(() => RULES.prices.map((base, index) => {
  const tile: Tile = { id: 0, type: 'property', name: '租金速查', district: 0, price: ruleMoney(props.economy, `landPrice.${index}`, base), ownerId: 'example', level: 0, mortgaged: false, barrier: false };
  return { price: tile.price, build: buildCost(tile, props.economy), rents: RULES.rentMultipliers.map((_, level) => rentFor({ ...tile, level }, props.economy)) };
}));
</script>
<template>
  <div class="rules-content">
    <div class="tabs rules-switch" role="group" aria-label="玩法说明版本">
      <button :class="{ active: version === 'brief' }" :aria-pressed="version === 'brief'" @click="version = 'brief'">简略版 · 快速上手</button>
      <button :class="{ active: version === 'detailed' }" :aria-pressed="version === 'detailed'" @click="version = 'detailed'">详细版 · 完整玩法</button>
    </div>
    <div class="rules-goal">
      <h3>买地、升级、收租，留到最后获胜</h3>
      <p>2～6 人轮流行动，<strong>最后一名未破产的玩家获胜</strong>。资产榜展示当前排名。</p>
      <div class="rules-facts"><span>起始现金 <strong>¥{{ initialCash(economy, playerCount, mapSize).toLocaleString() }}</strong></span><span>起点奖励 <strong>¥{{ ruleMoney(economy, 'startIncome', 1000).toLocaleString() }} ＋{{ startCards(playerCount) }} 张卡牌</strong></span><span>开局手牌 <strong>{{ initialCards(playerCount) }} 张</strong></span></div>
    </div>
    <p class="rules-intro"><strong>掷骰前出牌（可跳过） → 掷骰移动 → 处理落点 → 管理资产 / 交易（可跳过） → 结束回合。</strong></p>

    <div v-if="version === 'brief'">
      <ol class="rules-steps">
        <li><strong>卡牌要在掷骰前使用</strong><p>轮到自己时，通常每回合最多使用 1 张主动卡牌，掷骰后不能补用。防御和响应类卡牌按弹窗时机使用，不占普通主动次数；特殊效果按当前提示处理。</p></li>
        <li><strong>踩空地买地，再踩自家地升级</strong><p>停在无主地产可买下；以后停在自己的未抵押地产，可付费升 1 级，最高 3 级。刚买下本次不能升级，普通升级需要踩到地块；每块地产通常每回合最多升级一次。</p></li>
        <li><strong>踩到别人的地要付租金</strong><p>空地买下后即有 0 级租金，房屋升级后按对应等级收费，五档金额见下表。抵押地产暂停收租，房东服刑期间也不收租；临时状态可能改变最终金额。</p></li>
        <li><strong>前三回合有一次自选骰点</strong><p>前三个个人回合内，普通掷骰后可把骰点改为 1～6 任意一点，每人每局仅 1 次，不耗手牌或主动次数；保留原点数不消耗机会，到期失效。</p></li>
        <li><strong>起点拿补给，特殊格看提示</strong><p>正向经过或最终停在起点可领取现金和卡牌。机会 / 财富格触发事件，活动角停下才参加。入狱跳过下次个人行动且服刑期间不收租，普通休息仍可收租。</p></li>
        <li><strong>钱不够先筹钱，再确认付款</strong><p>先完成防御选择，再抵押、卖房或卖地筹款，最后手动确认支付。玩家间可协商现金和地产交易，也可用地产抵债，对方接受才成交。现金不足不会立即破产。</p></li>
      </ol>
    </div>

    <template v-else>
      <section>
        <h3>1. 回合、移动与起点奖励</h3>
        <ul class="rules-list">
          <li>轮到自己后，先决定是否使用主动卡牌，再掷骰移动。完成落点事件、购买或升级选择后，可以管理资产、发起交易，最后结束回合。当前待办未处理完时，先按提示完成选择。</li>
          <li>通常顺时针移动，普通移动只结算最终落点；路径上的障碍可能提前截停。特殊移动是否替代掷骰、是否结算沿途与落点，以当前提示为准。</li>
          <li>正向经过或最终停在起点，领取 ¥{{ ruleMoney(economy, 'startIncome', 1000).toLocaleString() }} 和 {{ startCards(playerCount) }} 张卡牌。后退途中经过起点不领奖，后退最终停在起点可领奖一次；特殊传送和换位按各自提示处理。</li>
          <li>玩法选择、偿债和交易报价不限时。获得补给超过手牌上限时，先整理手牌，再继续结算。</li>
        </ul>
      </section>
      <section>
        <h3>2. 出牌时机与手牌管理</h3>
        <ul class="rules-list">
          <li><strong>主动出牌：</strong>通常只能在自己的回合、掷骰前使用，每回合最多 1 张。掷骰后结束普通主动出牌窗口；额外出牌次数或其他特殊窗口以当前提示为准。</li>
          <li><strong>防御与响应：</strong>在费用、受控或受攻击等对应弹窗出现时选择，不占普通主动次数。已经跳过的行动无法补回；可用牌和合法目标以界面为准。</li>
          <li><strong>手牌上限：</strong>默认 6 张，效果可能改变本局上限。补给超限时，从原手牌和新补给中选择保留，其余每张折现 ¥{{ ruleMoney(economy, 'handDiscard', 200).toLocaleString() }}；主动弃牌按当前提示处理。具体卡面仅本人可见。</li>
        </ul>
      </section>
      <section>
        <h3>3. 买地、踩地升级与收租</h3>
        <ul class="rules-list">
          <li><strong>买地：</strong>停在无主地产时可按地价购买，也可放弃。买下后为 0 级，本次不能立即建房。</li>
          <li><strong>普通升级：</strong>再次停在自己的未抵押地产，可支付建造费升 1 级，最高 3 级。每级建造费默认是地价的一半，具体金额见下表；普通升级不能在资产面板里随时远程进行。</li>
          <li><strong>升级次数：</strong>每块地产通常每回合最多升级一次，普通免费升级也计入。事件或特殊效果可以提供额外升级方式，能否远程升级、是否占次数按当次提示处理；抵押、满级或受到限制的地产不能普通升级。</li>
          <li><strong>租金计算：</strong>基础租金为地价的 {{ economy?.rentBasePercent ?? 10 }}%（向下取整），0～3 级分别乘以 1、3、6、10；配置了固定金额时使用固定金额。免费建成的房屋也按正常建设成本计入资产。</li>
          <li><strong>暂停收租：</strong>地产抵押或受到停租效果时不收租；房东服刑期间也不收租，普通休息仍可收租。地名后出现 ✦ 表示有附加效果，可打开地产详情查看，最终费用以结算提示为准。</li>
        </ul>
      </section>
    </template>

    <div class="rules-example">
      <p><strong>本局地价、升级费与租金速查</strong></p>
      <table>
        <caption>单位：元；升级费为每升 1 级的费用，租金未计临时效果与防御减免。</caption>
        <thead><tr><th scope="col">地价</th><th scope="col">升级费</th><th scope="col">0 级租金</th><th scope="col">1 级租金</th><th scope="col">2 级租金</th><th scope="col">3 级租金</th></tr></thead>
        <tbody><tr v-for="(property, index) in propertyPrices" :key="index"><th scope="row">{{ property.price.toLocaleString() }}</th><td>{{ property.build.toLocaleString() }}</td><td v-for="(rent, level) in property.rents" :key="level">{{ rent.toLocaleString() }}</td></tr></tbody>
      </table>
    </div>
    <p v-if="version === 'brief'" class="rules-tip">买地后留些现金应付租金；抵押、出售、偿债和活动的具体规则见「详细版」。</p>

    <div v-else>
      <section>
        <h3>4. 开局自选骰点</h3>
        <p>每人前三个个人回合内，普通骰点公开后可使用一次机会，改为 1～6 中任意一点，不消耗手牌或主动次数。保留原骰点不消耗机会，第 3 个个人回合结束后过期；休息或入狱跳过的回合也计数。</p>
        <p>自选后仍正常结算步数修正、障碍和落点。特殊状态可能限制自选；选择自选后不再进入本次重掷窗口，保留原骰点时可继续按提示处理。</p>
      </section>
      <section>
        <h3>5. 付款、筹款与资产变现</h3>
        <p><strong>先完成防御选择 → 现金不足时筹款 → 手动确认付款。</strong>抵押、出售或部分抵债不会自动结清剩余欠款；全额减免或全额抵债后直接结清。</p>
        <ul class="rules-list">
          <li><strong>整体抵押：</strong>土地和建筑一起抵押，取得成本估值的 50%。建筑保留，但暂停收租、升级，不能单独卖房。</li>
          <li><strong>赎回：</strong>支付抵押时成本估值的 55%。赎回费在抵押时固定，之后建筑变化或地产转让都不改变；费用结算期间不能赎回。</li>
          <li><strong>逐级卖房：</strong>未抵押建筑可以逐级出售，每级默认返还单级建造费的一半，实际金额按本局配置。</li>
          <li><strong>整套卖地：</strong>土地和现有建筑可一起卖给系统，默认回收当前成本估值的 70%。已抵押地产先扣固定赎回费，余额到账；不足时需用现金补差价。出售后失去产权，建筑清除，地产恢复待购买。</li>
          <li><strong>成本估值：</strong>地价 + 当前建筑等级 × 单级建造费。配置了固定建造或回收金额时，以本局实际显示为准。</li>
        </ul>
        <p class="rules-tip">例如按默认比例：土地加建筑估值 ¥2,000，抵押得到 ¥1,000，赎回需 ¥1,100；直接整套出售得 ¥1,400，先抵押再整套出售额外得到 ¥300。</p>
      </section>
      <section>
        <h3>6. 玩家交易与地产抵债</h3>
        <ul class="rules-list">
          <li>自己掷骰前、落点处理后的管理阶段，或处理玩家债务时，可以发起现金与地产报价。价格双方协商，对方接受后才成交；拒绝或取消后继续原流程。</li>
          <li>建筑、抵押状态和固定赎回费随地产转移。受到限制的地产是否可交易，以当前可选范围为准。</li>
          <li>欠玩家的钱，可以向债权人提供地产协商抵债，也可向其他玩家出售资产筹现金。抵债金额双方确认，可抵全部或部分；抵扣部分不转移现金，剩余欠款继续支付。</li>
          <li>银行费用不能用玩家抵债报价结算，需要通过现金和资产变现偿付。</li>
        </ul>
      </section>
      <section>
        <h3>7. 事件、监禁与休息</h3>
        <ul class="rules-list">
          <li>停在机会或财富格，会立即抽取并处理对应事件，不进入手牌。事件可能带来现金、费用、移动、建筑变化或其他状态；事件移动到另一事件格时，继续处理新落点。</li>
          <li>涉及选目标、是否参加或是否升级时，按弹窗处理。稀有度高不代表一定有利；需要下注的玩法只能使用现有现金。</li>
          <li>落入监狱会跳过下一次个人行动，服刑期间暂停收租，其他小游戏仍可参加。普通休息按提示跳过对应次数，期间仍可收租。</li>
          <li>受到外部休息或监禁后，在受控行动跳过后的两个个人回合内，免疫再次休息或入狱。成功解除限制不触发保护；主动承担的休息、监禁等代价不受保护减免。</li>
        </ul>
      </section>
      <section>
        <h3>8. 轮换活动与小游戏</h3>
        <p>活动角需要停下才触发，经过不参加；参与范围、下注和奖励以当场提示为准。</p>
        <details v-if="activityMoney" class="rules-reminder"><summary><strong>轮换活动角</strong></summary><p>起点、监狱固定；另外两个角每隔开局人数×5个已结束回合一起轮换，最后3回合预告，只在回合结束后换场。经过不触发，停下才参加。</p><ul class="rules-list"><li>赌场：本金¥{{ activityMoney.casinoSmallStake }}猜大小，猜中总到账¥{{ activityMoney.casinoSmallPrize }}；本金¥{{ activityMoney.casinoPointStake }}猜一个点数，猜中总到账¥{{ activityMoney.casinoPointPrize }}。可放弃。</li><li>赛马：八匹马等胜率，每人自愿下注¥{{ activityMoney.horseStake }}，可押同一匹，中奖各总到账¥{{ activityMoney.horsePrize }}。</li><li>轮盘：可放弃发起，指定对手后必须应战。一次装填六弹巢一颗子弹、随机先手，败者产生¥{{ activityMoney.rouletteDebt }}玩家费用，沿用防御与偿债。</li><li>红包：所有未破产玩家领取，银行基础奖池为¥{{ activityMoney.packetBase ?? 0 }}＋参与人数×¥{{ activityMoney.packetPerPlayer }}，不扣玩家的钱；每人自选一个未领取红包，点击立即开包到账，基础份额1%～90%，每人独立10%幸运翻倍并额外庆祝。</li><li>猜拳：全场秘密出拳，两种手势淘汰败方，一种或三种则重来，最后一人独得¥{{ activityMoney.rpsPrize }}。</li></ul></details>
        <details v-if="activityMoney?.treasureStake !== undefined" class="rules-reminder">
          <summary><strong>更多活动玩法</strong></summary>
          <ul class="rules-list">
            <li>贪心宝箱：仅落点玩家自愿支付¥{{ activityMoney.treasureStake }}，每层二选一，成功后可收手领取本金的2／4／8倍，或继续挑战；触发机关则本次到账为零。最多三层，奖励只在收手或全部通关时发放。</li>
            <li>三关玻璃桥：每人可自愿支付¥{{ activityMoney.horseStake }}上桥；每关秘密选左或右，全部锁定后统一揭晓。选错淘汰，连续三关成功者各得¥{{ activityMoney.horsePrize }}，多人通关不分摊奖金。</li>
            <li>少数派夜市：全员免费秘密选择火锅摊或烧烤摊，人数较少的一边抽一人独得¥{{ activityMoney.rpsPrize }}；两边人数相同或全在同一摊时，全员抽一人。每场只发一份奖金。</li>
            <li>暖暖潮玩盲盒：固定20个盒子，每人免费领取一个。19个普通款各¥{{ activityMoney.blindboxRegular }}，唯一隐藏款为¥{{ activityMoney.blindboxHiddenBase }}＋参与人数×¥{{ activityMoney.blindboxHiddenPerPlayer }}；仅隐藏款有10%概率升级为双倍限定款。选中立即到账，未选中的盒子不发奖。</li>
            <li>收起窗口不会放弃参与或重复领取奖励，重新打开可继续处理。</li>
          </ul>
        </details>
        <p v-if="activityMoney" class="rules-intro">活动额外道具：赛马中奖、猜拳夺冠、轮盘获胜、宝箱三层全部通关、玻璃桥通关、少数派中奖、红包手气最佳，以及盲盒隐藏款／限定款，各获得 1 张随机道具。红包按基础金额评选手气最佳，并列沿用活动名单顺序取一人，全部领取后发牌；宝箱提前收手不发牌。具体卡面仅本人可见，活动结束后整理超限手牌。</p>
      </section>
      <section>
        <h3>9. 资产榜、破产与胜利</h3>
        <ul class="rules-list">
          <li><strong>净资产：</strong>现金 + 土地及建筑成本估值；抵押地产扣除固定赎回费，单块地产剩余权益最低为零。同净资产并列，榜首只代表当前领先。</li>
          <li><strong>申请清算：</strong>现金加银行可变现资产仍不足以偿债时，才可确认破产清算；欠银行费用还须先自行抵押或出售全部可抵押地产。可变现价值取抵押、整套出售、先卖房再变现中的最高所得，已抵押地产只计扣赎回费后的正净额。</li>
          <li><strong>清算结果：</strong>退出竞争，现金、手牌和状态清空，地产归还银行、建筑清除；仍可继续观战。</li>
          <li><strong>胜利条件：</strong>只剩一名未破产玩家时，本局结束，该玩家获胜。</li>
        </ul>
      </section>
      <section>
        <h3>10. 断线与刷新</h3>
        <p>使用同一浏览器刷新或重新输入房间号，可以恢复原座位，请保留浏览器数据。轮到离线玩家处理行动、选择或报价时，会暂停等待重连；房主可移出离线玩家并清算。</p>
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
.rules-example { margin: 14px 0; padding: 12px; overflow-x: auto; }
.rules-example p { margin: 0 0 8px; }
.rules-example table { width: 100%; min-width: 440px; border-collapse: collapse; font-size: 12px; color: #45604c; }
.rules-example caption { text-align: left; font-size: 11px; color: #617457; padding-bottom: 8px; }
.rules-example th, .rules-example td { padding: 8px 3px; text-align: center; border-top: 1px solid #e3e9d9; }
@media (max-width: 480px) {
  .rules-switch button, .rules-example table { font-size: 11px; }
  .rules-goal, .rules-reminder { padding: 13px; }
  .rules-facts { gap: 7px 12px; }
}
</style>
