<script setup lang="ts">
import { computed, ref } from 'vue';
import { buildCost, initialCards, initialCashBase, ITEMS, rentFor, RULES, ruleMoney, startCards } from '../../shared/rules';
import type { EconomySettings, Tile } from '../../shared/types';

const props = defineProps<{ spectator?: boolean; chanceEvents?: string[]; economy?: EconomySettings | null; playerCount: number; activityMoney?: Record<string, number> | null }>();
const version = ref<'brief' | 'detailed'>('brief');
// 当前补给池没有旧版停留卡，速查只展示本局可获得的道具。
const availableItems = Object.entries(ITEMS).filter(([kind]) => kind !== 'stay').map(([, item]) => item);
/** 根据本局经济配置生成示例地产及各级路费，固定金额覆盖与实际玩法保持一致。 */
const example = computed(() => {
  const tile: Tile = { id: 0, type: 'property', name: '示例地产', district: 0, price: ruleMoney(props.economy, 'landPrice.1', 600), ownerId: null, level: 0, mortgaged: false, barrier: false };
  return { price: tile.price, build: buildCost(tile, props.economy), rents: RULES.rentMultipliers.map((_, level) => rentFor({ ...tile, ownerId: 'example', level }, props.economy)) };
});
</script>
<template>
  <div class="rules-content">
    <div class="tabs rules-switch" role="group" aria-label="玩法说明版本">
      <button :class="{ active: version === 'brief' }" :aria-pressed="version === 'brief'" @click="version = 'brief'">简略版 · 局内玩法</button>
      <button :class="{ active: version === 'detailed' }" :aria-pressed="version === 'detailed'" @click="version = 'detailed'">详细版 · 查看规则</button>
    </div>
    <details v-if="activityMoney" class="rules-reminder"><summary><strong>轮换活动角 · 苏暖暖游园会</strong></summary><p>起点、监狱固定；另外两个角每隔开局人数×5个已结束回合一起轮换，最后3回合预告，只在回合结束后换场。经过不触发，停下才参加。</p><ul class="rules-list"><li>赌场：本金¥{{ activityMoney.casinoSmallStake }}猜大小，猜中总到账¥{{ activityMoney.casinoSmallPrize }}；本金¥{{ activityMoney.casinoPointStake }}猜一个点数，猜中总到账¥{{ activityMoney.casinoPointPrize }}。可放弃。</li><li>赛马：八匹马等胜率，每人自愿下注¥{{ activityMoney.horseStake }}，可押同一匹，中奖各总到账¥{{ activityMoney.horsePrize }}。</li><li>轮盘：可放弃发起，指定对手后必须应战。一次装填六弹巢一颗子弹、随机先手，败者产生¥{{ activityMoney.rouletteDebt }}玩家费用，沿用防御与偿债。</li><li>红包：所有未破产玩家领取，银行基础奖池为¥{{ activityMoney.packetBase ?? 0 }}＋参与人数×¥{{ activityMoney.packetPerPlayer }}，不扣玩家的钱；每人自选一个未领取红包，点击立即开包到账，基础份额1%～90%，每人独立10%幸运翻倍并额外庆祝。</li><li>猜拳：全场秘密出拳，两种手势淘汰败方，一种或三种则重来，最后一人独得¥{{ activityMoney.rpsPrize }}。</li></ul></details>
    <details v-if="activityMoney?.treasureStake !== undefined" class="rules-reminder">
      <summary><strong>趣味新玩法 · 同类别轮换</strong></summary>
      <ul class="rules-list">
        <li>贪心宝箱：仅落点玩家自愿支付¥{{ activityMoney.treasureStake }}，每层二选一，成功后可收手领取本金的2／4／8倍，或继续挑战；触发机关则本次到账为零。最多三层，奖励只在收手或全部通关时发放。</li>
        <li>三关玻璃桥：每人可自愿支付¥{{ activityMoney.horseStake }}上桥；每关秘密选左或右，全部锁定后统一揭晓。选错淘汰，连续三关成功者各得¥{{ activityMoney.horsePrize }}，多人通关不分摊奖金。</li>
        <li>少数派夜市：全员免费秘密选择火锅摊或烧烤摊，人数较少的一边抽一人独得¥{{ activityMoney.rpsPrize }}；两边人数相同或全在同一摊时，全员抽一人。每场只发一份奖金。</li>
        <li>暖暖潮玩盲盒：固定20个盒子，每人免费领取一个。19个普通款各¥{{ activityMoney.blindboxRegular }}，唯一隐藏款为¥{{ activityMoney.blindboxHiddenBase }}＋参与人数×¥{{ activityMoney.blindboxHiddenPerPlayer }}；仅隐藏款有10%概率升级为双倍限定款。选中立即到账，未选中的盒子不发奖。</li>
        <li>收起窗口不会放弃或重复领取奖励。轮转保留原五类权重，在对应类别内选择新旧玩法，已有旧局继续原活动池。</li>
      </ul>
    </details>
    <p v-if="activityMoney" class="rules-intro">活动额外道具：赛马中奖、猜拳夺冠、轮盘获胜、宝箱三层全部通关、玻璃桥通关、少数派中奖、红包手气最佳，以及盲盒隐藏款／限定款，各获得 1 张随机道具。红包按基础金额评选手气最佳，并列沿用活动名单顺序取一人，全部领取后发牌；宝箱提前收手不发牌。具体卡面仅本人可见，活动结束后整理超限手牌。</p>
    <details class="rules-reminder"><summary><strong>地产附加效果与新道具</strong></summary><p>地名后的 ✦ 表示有附加效果，点开详情查看。A.T.力场挡一次建筑拆除；寻宝按领取时无修正租金发奖，本次免租且不消耗租金状态；演唱会每层×1.2，产生正租金时全部消耗，再处理翻倍和防御。普通路障与陨星每次只消耗一层。</p><p>神威暂停地产三个施术者后续个人回合，停止经营和全部附加效果，但允许破产清算；施术者淘汰立即解除。黑洞清除其他附加效果，神威除外。尸鬼封尽使双方本局手牌上限各减一，最低一张。迟缓先对基础步数减半并向下取整，再算加码与塔塔开。</p><p>暗影步和传送替代掷骰并正常结算，暗影步到起点领一次奖励；斗转星移、ROOM换位、地爆天星不结算落点或起点。制衡与发现使用原选牌窗口，弃牌不折现；手牌抹杀不把发动卡计入重抽数量。</p></details>
    <div class="rules-goal">
      <h3>买地收路费，成为最后的赢家</h3>
      <p>2～6 人轮流行动。<strong>最后一名未破产的玩家获胜</strong>，资产榜第一名只是当前领先。</p>
      <div class="rules-facts"><span>起始现金 <strong>¥{{ ruleMoney(economy, 'initialCash', initialCashBase(playerCount)).toLocaleString() }}</strong></span><span>经过或停留起点 <strong>+¥{{ ruleMoney(economy, 'startIncome', 1000).toLocaleString() }} ＋{{ startCards(playerCount) }} 张道具</strong></span><span>开局手牌 <strong>{{ initialCards(playerCount) }} 张</strong></span></div>
    </div>

    <div v-if="version === 'brief'">
      <p class="rules-intro"><strong>用道具（可选） → 掷骰移动 → 处理落点 → 管理资产 / 交易 → 结束回合。</strong></p>
      <ol class="rules-steps">
        <li><strong>买地建房</strong><p>停在无人地产可买地，以后停在自己的未抵押地产可建房，最高 3 级，刚买下本次不能建房。别人停在你的未抵押地产要付路费，建房通常能提高路费。</p></li>
        <li><strong>卖房卖地</strong><p>在「我的地产」可逐级卖出未抵押房屋，也可把土地和建筑整套卖给系统，默认回收当前成本估值的70%。已抵押时先扣固定赎回费；出售后失去产权，建筑清除。</p></li>
        <li><strong>玩家交易</strong><p>现金和地产可以互换，价格双方商量，对方接受才成交。建筑和抵押状态随地产转移；欠玩家的钱，也可协商用地产抵债。</p></li>
        <li><strong>抵押赎回</strong><p>土地和建筑可整体抵押换现金，建筑保留，但暂停收租、建房，不能单独卖房。有钱后可付费赎回，恢复收租。</p></li>
        <li><strong>使用道具</strong><p>掷骰前每回合最多用一张主动道具，可控制移动、升级、赚钱或影响对手。免租、付款减半和解控在对应提示时使用，不占主动次数。</p></li>
        <li><strong>事件与休息</strong><p>机会 / 财富格触发奖励、费用或其他事件，按卡面提示处理。入狱会跳过下一次个人行动，服刑期间不收租，其他小游戏正常参加；普通休息仍可收租。有解控卡可当场解除限制。</p></li>
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
        <ul class="rules-list"><li>通常顺时针移动。经过或停留在起点，获得 ¥{{ ruleMoney(economy, 'startIncome', 1000).toLocaleString() }}，并补给 {{ startCards(playerCount) }} 张道具；超过手牌上限时先整理手牌，再继续落点结算。</li><li>普通移动只处理最终落点；路障会让首个经过者提前停下，路障随即消失。带有路障免疫状态时会继续通过，路障保留。</li><li>玩法选择、偿债和交易报价都不限时。先处理当前待办，才能继续后续操作。</li></ul>
      </section>
      <section>
        <h3>2. 买地、建房和收路费</h3>
        <ul class="rules-list"><li><strong>买地：</strong>停在无主地产时，可支付地价购买，也可放弃。本次买下后不立即建房。</li><li><strong>普通建房：</strong>停在自己的未抵押地产时，可付费升一级，最高 3 级。每块地产每回合最多升级一次，免费升级也计入。</li><li><strong>免费升级：</strong>道具或事件可选择合法的自家地产，无需停在该地块；已抵押、满级或本回合已升级的地产不可选。</li><li><strong>收路费：</strong>别人停在你的未抵押地产时付费。基础路费为地价的 {{ economy?.rentBasePercent ?? 10 }}%（向下取整），0～3 级依次乘以 1、3、6、10；本局若配置了固定路费，以固定金额为准。房东服刑期间免收租金，出狱后恢复；普通休息不影响收租，也不改变地产原有租金基数。</li><li><strong>建房费用：</strong>每级默认为地价的一半；免费建成的房屋也按正常建设成本计入资产。</li></ul>
        <div class="rules-example">
          <p><strong>本局示例：</strong>地价 ¥{{ example.price.toLocaleString() }}，每升一级花 ¥{{ example.build.toLocaleString() }}。</p>
          <table><caption>这块示例地产的各级路费</caption><thead><tr><th scope="col">等级</th><th scope="col">0 级</th><th scope="col">1 级</th><th scope="col">2 级</th><th scope="col">3 级</th></tr></thead><tbody><tr><th scope="row">路费</th><td v-for="(rent, level) in example.rents" :key="level">¥{{ rent.toLocaleString() }}</td></tr></tbody></table>
        </div>
      </section>
      <section>
        <h3>3. 需要付款时，按这个顺序处理</h3>
        <ol class="rules-list"><li><strong>先选防御：</strong>免租只免本次路费；付款减半可用于路费、罚款等强制费用。每笔至多选一种，也可不使用。防御不占主动道具次数。</li><li><strong>再筹钱：</strong>现金不足时管理资产。向银行付款，可自行选择地产抵押，也可逐级卖出未抵押建筑或整套出售地产；欠玩家的钱还可向债权人提出地产抵债，或向其他玩家出售资产筹款。</li><li><strong>最后确认支付：</strong>即使现金足够，也要先完成防御选择，再手动确认现金付款；变现不会自动替你还债。全额减免或全额抵债后，这笔费用直接结清。</li></ol>
        <p class="rules-tip">付款减半的减免额向下取整：应付 ¥301 时减免 ¥150，仍付 ¥151。房东只收到减免后的实际路费。</p>
        <p>若已有「下次免付款」状态，会自动免除本笔费用；若有「下次租金翻倍」状态，先对原路费翻倍，再处理减免。</p>
      </section>
      <section>
        <h3>4. 现金不足：抵押、卖出与赎回</h3>
        <ul class="rules-list"><li><strong>整体抵押：</strong>土地和建筑一起抵押，取得成本估值的 50%。建筑保留，暂停收租、建房，抵押期间不能单独卖房。</li><li><strong>赎回：</strong>支付抵押时成本估值的 55%，恢复收租。赎回费用在抵押时固定，之后拆房或转让都不改变它；费用结算期间不能赎回。</li><li><strong>卖房：</strong>未抵押建筑可逐级出售，每级默认返还建设成本的一半，实际金额按本局配置。</li><li><strong>整套出售：</strong>土地和现有建筑一起卖给系统，默认回收当前成本估值的 70%，无需先卖房。已抵押时自动扣除固定赎回费用，余额到账；回收款不足时需用现金补差价，现金不足不能出售。出售后建筑清除、地产恢复待购入，土地部分的固定回收金额按本局配置。</li></ul>
        <p class="rules-tip">例如：土地加建筑估值 ¥2,000，抵押拿到 ¥1,000，赎回需 ¥1,100；直接整套出售默认取得 ¥1,400，先抵押再出售额外到账 ¥300，合计取得 ¥1,300。</p>
      </section>
      <section>
        <h3>5. 交易和资产抵债</h3>
        <ul class="rules-list"><li>在自己掷骰前、落点处理后，或处理玩家债务时，可发起现金与地产报价。对方接受后才成交；拒绝或发起人取消后继续原流程。</li><li>地产上的建筑、抵押状态及固定赎回费用随地产一起转移。</li><li>资产抵债须向债权人提供地产，抵扣金额由双方确认，可抵清或只抵一部分。抵扣部分不转移现金，剩余欠款继续处理。</li><li>银行费用不能通过玩家抵债报价结算。</li></ul>
      </section>
      <section>
        <h3>6. 道具什么时候用</h3>
        <ul class="rules-list"><li><strong>主动道具：</strong>自己每回合掷骰前最多使用一张。固定步数、主动位移、遥控骰子会替代掷骰；加码与骰子合成一次移动，只结算最终落点。换位后仍正常掷骰。</li><li><strong>防御 / 解控：</strong>免租、付款减半在费用提示时使用；解控在监禁或休息刚施加时使用。它们都不占主动次数，也不能补回已跳过的行动。</li><li><strong>取消与无目标：</strong>免费升级卡在确认前取消，会保留卡和主动次数；没有合法升级目标，或抢夺的对手没有手牌时，也保留对应卡与次数。</li><li><strong>手牌上限：</strong>默认最多保留 6 张，尸鬼封尽可永久减低本局上限，最低一张。超过时由本人从原牌和全部补给中选留当前上限数量，其余每张折现 ¥{{ ruleMoney(economy, 'handDiscard', 200).toLocaleString() }}。具体卡面仅本人可见。</li></ul>
        <p>SSR「谕示裁定」道具可指定一名未破产玩家（可选自己）直接入狱；SSR「水神的审判」机会事件由抽到者自己入狱。两者均跳过下一次个人行动，服刑期间暂停收租，其他小游戏照常参与。直接入狱不领取沿途奖励、不消耗塔塔开，道具对自己使用会替代本次掷骰。解控卡可当场解除休息与停租，已完成的传送不撤销。</p>
        <template v-if="!spectator"><h4>道具速查</h4><div v-for="item in availableItems" :key="item.name" class="rule-item"><span aria-hidden="true">{{ item.icon }}</span><p><strong>{{ item.name }}</strong> · {{ item.description }}</p></div></template>
      </section>
      <section>
        <h3>7. 机会、财富和休息</h3>
        <ul class="rules-list"><li>机会、财富及随机道具先按完整效果的独立权重抽取，再等概率选匹配卡面；稀有度只标识影响等级，不参与第二次抽取。道具池UR合计5%，同效果的名称和插画数量不增加出货机会。</li><li>R 为轻微影响，SR 为中等影响，SSR 为强力或多人影响，UR 为大成功／大失败；稀有不代表一定有利。UR 机会和财富可能大额获利或支付银行费用；UR 风险道具确认下注后才消耗，中奖和失败均按卡面结算。</li><li>定向补给保持指定牌型，只在该机制的完整效果中归一化权重抽取。每次落在机会或财富格都会抽取对应事件；事件移动到另一事件格时继续抽取并结算。本局停用财富的「再次行动」模板。</li><li>免费升级事件可以放弃；仅确实没有合法地产时，改给现金。赌博也可以放弃，参加时只能用已有现金下注。</li><li>排名事件在开始时锁定净资产最高或最低者，并列时随机选一人。自家全毁、随机灾害和UR爆破狂人有实际建筑损毁时，以 50% 概率向受损业主补偿摧毁前单次租金的 50%（向下取整）；已抵押地产也可能受损，其租金为 0，补偿为 0。</li><li>UR「爆破狂人」可指定对手一块有建筑地产全部拆除，保留产权，无目标则结束。UR「基建狂魔」可选择自己的合法地产免费直升 3 级，计入本回合升级；可放弃，无合法目标才给替代现金。两种事件抽到即处理，不进入手牌。</li><li>SSR「爆破鬼才」指定未破产玩家（可选自己）随机直接传送，不触发沿途奖励或此次落点。SSR「塔塔开」使全体未破产玩家下次最终移动距离×2，重复不叠加，传送与换位不消耗。</li><li>UR「人才引进」随机赠送一块无主地产、实际生活费¥5,000和两张道具，无地只跳过领地。UR「龙卷风摧毁停车场」使全图所有有建筑地产各降一级，按降级前单次租金的10%逐地补偿，不叠加全毁事件的概率补偿。</li><li>落入监狱会跳过下次行动；其他休息事件按卡面次数跳过。有解控卡时可在限制刚施加时选择解除。</li></ul>
        <template v-if="chanceEvents?.length"><h4>本局补充说明</h4><ul class="rules-list"><li v-for="description in chanceEvents" :key="description">{{ description }}</li></ul></template>
      </section>
      <section>
        <h3>8. 资产榜、破产和胜利</h3>
        <ul class="rules-list"><li><strong>公开资产榜：</strong>所有人可查看现金、净资产和名次。净资产 = 现金 + 土地及建筑成本；抵押地产扣除固定赎回费用，单块地产剩余权益最低为零。同净资产并列。</li><li><strong>现金不足不等于破产：</strong>现金加银行可变现资产足以偿清时，不能申请清算。仍不足时，可结束协商并确认清算；欠银行的钱须先自行抵押或出售全部可抵押地产。可变现资产按抵押、整售及先卖房再变现的最高所得计算；已抵押地产仅计扣除固定赎回费用后的正净额。</li><li><strong>清算后：</strong>退出竞争，现金、手牌和状态清空，地产归还银行、建筑清除。可以继续观看朋友的对局。</li><li><strong>胜利条件：</strong>只剩一名未破产玩家时，本局结束，该玩家获胜。</li></ul>
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
