import type { ContentResult, ItemKind } from '../shared/types';
import { ITEMS } from '../shared/rules';

/** 输入冻结卡面，返回统一完整效果；数值沿用卡面参数，缺参旧卡保留原效果。 */
export function cardEffect(card: ContentResult): string {
  const kind = card.kind ?? (card.pool === 'card' ? card.mechanism as ItemKind : undefined);
  if (!kind || !ITEMS[kind]) return card.effect ?? '';
  const p = card.parameters ?? {};
  const fallback = card.effect ?? ITEMS[kind].description;
  switch (kind) {
    case 'bankai': case 'igniteSea': case 'senbonzakura': case 'kingsPressure': return card.effect ?? ITEMS[kind].description;
    case 'fixed': return p.steps === undefined ? fallback : `移动 ${p.steps} 格并结算落点，替代掷骰。`;
    case 'move': return p.steps === undefined ? fallback : `${p.steps === 0 ? '原地停留并重新结算当前格' : `${p.steps < 0 ? '后退' : '前进'} ${Math.abs(p.steps)} 格并结算落点`}，替代掷骰。`;
    case 'bonus': return p.steps === undefined ? fallback : `本次掷骰额外前进 ${p.steps} 格，合并移动并只结算最终落点。`;
    case 'cash': return p.amount === undefined ? fallback : `获得现金 ¥${p.amount}。`;
    case 'gamble': return p.wager === undefined || p.probability === undefined || p.prize === undefined ? fallback : `用现有现金下注 ¥${p.wager}，${p.probability}%概率获得 ¥${p.prize}，本金不返还；取消不消耗。`;
    case 'shield': return `为1处自家地产添加 ${p.charges ?? 1} 层护盾，每层抵挡1次建筑拆除或降级，连击逐层消耗；不叠加，不保护主动出售或清算。`;
    default: return ITEMS[kind].description;
  }
}

/** 输入冻结卡面，返回手牌用的简短效果；缺少参数的旧卡使用不含数值的说明。 */
export function cardSummary(card: ContentResult): string {
  if (!card.kind && card.pool !== 'card') return card.effect ?? '点击查看完整效果';
  const p = card.parameters ?? {};
  if (card.kind === 'bankai' && (p.version ?? 0) < 2) return '掷两枚骰，选一点前进或后退，只结算一次';
  if (card.kind === 'kingsPressure' && (p.version ?? 0) < 2) return card.effect ?? '禁止对手使用UR主动牌，持续3个个人回合';
  const steps = p.steps;
  const summaries: Record<ItemKind, string> = {
    asunaFlash: "突进1～6格，先清路径效果和敌方护盾，再斩击敌楼",
    defenseSpecialization: "含本回合共3个个人回合免疫敌方主动牌及负面事件、免付强制费用；基础走1格",
    revealingSwords: "封锁1名对手接下来3个个人回合的全部手牌",
    fusion: "消耗本牌及额外任意2张手牌，融合为1张普通UR；优先在第一张材料的玩法类别内均分抽取，无同类UR才抽全部UR，不额外赠送使用次数。",
    shadowBlade: "选1～2处敌方建筑地各斩1击，两处最多相距2格",
    doubleBoost: "普通掷骰移动后可喷射最多2次，每次前进1格，只结算终点",
    blueSilverBind: "前后6格选1名对手，下个个人回合不能主动移动",
    goldenCore: "本回合闭关不移动，结丹后手牌上限+1并获1张随机UR卡",
    nuwaMend: "下个本人回合结束前，修复最近同一次敌方攻击或灾害损坏的最多3处自家地产，恢复损坏前等级；产权须未变，不恢复主动卖房，不回滚付款、抵押和其他状态。",
    endlessClouds: "净化自己和自家地产。未破产玩家各得 ¥400。被迫跳过回合前可用。",
    namelessSword: "本回合蓄力，无法主动移动；下个本人回合掷骰前可对前后6格内1处对手地产连斩3次，各降1级、护盾逐击挡。释放不占主动次数，次回合正常移动；休息、监禁或放弃则失效。",
    kingsPressure: "指定1名对手，接下来3个个人回合禁止发动所有等级的主动攻击；非攻击主动牌及所有等级防御、解控、救援响应照常。重复刷新不叠加。",

    keqingRush: '突入前方1～6格对手地产，命中降1级，被挡也半租',
    thunderLord: '神君蓄势2～4击，下个本人回合开始追击指定地产',
    hollowfication: '抵消一次敌方主动牌对本人的位移、休息和监禁',
    fireBurial: '献祭同一处自家地1～2级建筑，砍敌方2～3级，穿盾',
    bankai: '本回合最多再出2张其他主动牌，含UR；禁止卍解和投影连锁',
    igniteSea: '移除对手地产全部护盾，再降1＋盾数级，最多3级',
    xiaowuSacrifice: '献祭2处自家地产，获得十万年魂环：含本回合8个本人回合内，自选免3笔租金',
    senbonzakura: '3片共享花瓣，可攻击不同地产或防御建筑破坏，持续3个后续回合',
    geoResonance: "任意格作中心，中心及左右可升的自家地产各免费升1级",
    growthLink: "连接2处自家地产，无回合期限，付费建造联动免费升级，最多2次",
    sereniteaPot: "重分2处自家建筑等级，再免费升1级，不占升级次数",
    createBuild: "弃1～2张其他牌，换不同自家地产各免费升1级",
    remoteConstruction: "本回合和下个个人回合移动结算后，各弹窗选地升1级（七五折），可跳过",
    shrineGrowth: "标记自家地产，其他玩家停留结算后免费升1级，最多2次",
    timeRewind: "普通骰点公开后重掷1次，必须接受新结果",
    projection: "复制自己上回合或全场刚结束回合用过的1张主动道具，本回合额外用1次。",
    starburst: "对手1处地产连续降级2次，每层护盾抵挡1击",
    imagineBreaker: "抵消主动牌对自己及自家地产的效果，含UR",
    timeRestore: "下个个人回合结束前，恢复自家地产最近一次被破坏的建筑",
    vectorReflect: "反弹主动牌对自己的效果，含UR；不保护地产",
    deathReturn: "清算前免除当前1笔债务并回归，获¥3000，自己休息1回合",
    explosion: "连续3格建筑全拆，含自家；护盾可挡，自己休息1回合",
    corruption: "侵蚀1处对手地产。业主每回合结束降1级，共3次，护盾可挡。",
    contract: "获得¥5000，本局手牌上限永久减1，最低1张",
    commandSpell: "指定另一玩家额外移动1～6格并结算，点数自选",
    sevenSwords: "分配3道剑气，每道降1级；每层护盾抵挡1道",
    railgun: "贯穿前方6格，无视护盾；清除沿途护盾，对手地产各降1级",
    sealCard: "抵消并收服针对自己或自家地产的单目标主动牌，含UR",
    swordRain: "前后6格内1名对手休息1回合；可用2次，每回合限1次",
    voidTears: "连接2处地产，首位步行入端点者传至另一端继续移动",
    absoluteZero: "冰封一条边至自己下回合结束，含自家；停租、禁升级",
    shield: `自家地产获得 ${p.charges ?? 1} 层护盾，每层抵挡1次拆除或降级`,
    recycle: "弃至少1张其他手牌并等量重抽，弃牌不折现",
    discover: "从3张不同效果的随机道具中选1张",
    sap: "前后3格内1名对手休息1回合（含同格），并随机偷取其1张手牌",
    exchange: "用现有现金支付无修正租金（最低地价），夺取对手地产",
    slow: "指定对手下次基础步数减半，向下取整",
    swapAny: "任意2名玩家换位，可含自己，不结算落点或起点奖励",
    kamui: "封印同边1～3处地产3个后续个人回合，暂停经营和附加效果",
    handDestruction: "全员剩余手牌等量重抽，含自己，弃牌不折现",
    gravity: "其他所有玩家传送到自己所在格，不结算落点或起点奖励",
    reaper: "自己与指定对手本局手牌上限永久各减1，最低1张",
    shadowstep: "传送至前后6格内另一玩家并结算，含同格，替代掷骰",
    homeTeleport: "传送到自家地产并结算，替代掷骰",
    blackhole: "清除地产全部附加效果，包含护盾和建设标记",
    meteor: "随机1～3处地产各增加1次截停，可叠加",

    remote: "自选1～6点移动，替代掷骰",
    fixed: steps === undefined ? '按卡面步数移动，替代掷骰' : `移动 ${steps} 格，替代掷骰`,
    move: steps === undefined ? '前进或后退，替代掷骰' : steps === 0 ? '原地停留并重新结算，替代掷骰' : `${steps < 0 ? '后退' : '前进'} ${Math.abs(steps)} 格，替代掷骰`,
    bonus: steps === undefined ? '增加本次掷骰步数' : `本次掷骰额外 +${steps} 格`,
    barrier: "前后6格设路障，不含当前格；首位受影响者截停并结算",
    stay: "原地停留并重新结算，替代掷骰",
    immunity: "免除本笔全部租金",
    demolish: "对手1处建筑降1级，护盾可挡",
    swap: "与未服刑对手换位，不结算落点，随后正常掷骰",
    blast: "指定玩家随机传送并结算落点，不发起点奖励",
    jail: "指定玩家入狱，可选自己；跳过下次行动并停租",
    fullUpgrade: "自家1处地产免费直升3级，计入本回合升级",
    fullDemolish: "对手1处建筑全拆，无补偿，无视护盾",
    freeUpgrade: "自家1处地产免费升1级，计入本回合升级",
    halfPayment: "本笔强制费用减免50%，减免额向下取整",
    release: "解除监禁或休息，或抵消1次对自己的强制位移",
    steal: "随机抢走对手1张手牌，无牌不消耗",
    cash: p.amount === undefined ? '获得卡面现金' : `获得现金 ¥${p.amount.toLocaleString()}`,
    gamble: p.wager === undefined || p.probability === undefined || p.prize === undefined ? '现金下注，按卡面概率获得奖金，本金不返还' : `下注 ¥${p.wager.toLocaleString()}，${p.probability}% 获奖 ¥${p.prize.toLocaleString()}；本金不返还`,
  };
  return summaries[(card.kind ?? card.mechanism) as ItemKind] ?? card.effect ?? '点击查看完整效果';
}
