package com.dfw.domain;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.ToLongFunction;
import static com.dfw.domain.GameSupport.*;

/** 只在现有事务中累计实际结果并冻结结算，不读取数据库或推断历史日志。 */
public final class SettlementService {
    private SettlementService() { }

    /** 输入房间，采集当前资产峰值与个人回合；旧局从首次观察起统计并标明不完整。 */
    public static void observe(Room room) {
        if (!room.status.equals("playing")) return;
        AnimeService.observeOwnership(room);
        Player actor = current(room);
        for (Player p : room.players) {
            if (p.bankrupt || p.matchStats != null && p.matchStats.settled) continue;
            if (p.matchStats == null) { p.matchStats = new MatchStats(); p.matchStats.initialWorth = Rules.netWorth(room, p); }
            MatchStats s = p.matchStats;
            s.peakNetWorth = Math.max(s.peakNetWorth, Rules.netWorth(room, p));
            s.peakProperties = Math.max(s.peakProperties, (int) room.board.stream().filter(t -> Objects.equals(t.ownerId, p.id)).count());
            if (p == actor && s.lastTurn != room.turnNumber) { s.turns++; s.lastTurn = room.turnNumber; }
        }
    }

    /** 输入已发生的现金偿付，累计租金或活动收支；减免、抵债和未付欠款不调用此方法。 */
    public static void payment(Room room, Player payer, PendingDecision debt, long amount) {
        if (amount <= 0 || debt == null) return;
        observe(room);
        Player creditor = player(room, debt.creditorId);
        if ("rent".equals(debt.type)) {
            payer.matchStats.rentPaid += amount;
            Tile land = tile(room, debt.tileId);
            if (amount > payer.matchStats.largestRent) {
                payer.matchStats.largestRent = amount;
                payer.matchStats.largestRentTile = land == null ? "地产" : land.name + "（" + land.id + "号）";
            }
            if (creditor != null && !creditor.bankrupt && creditor.matchStats != null && !creditor.matchStats.settled) {
                MatchStats s = creditor.matchStats; s.rentReceived += amount;
                s.rentByPayer.merge(payer.id, amount, Long::sum);
            }
        }
        if (debt.activityPayment) {
            payer.matchStats.activityNet -= amount;
            if (creditor != null && !creditor.bankrupt && creditor.matchStats != null && !creditor.matchStats.settled) creditor.matchStats.activityNet += amount;
        }
    }

    /** 输入实际活动现金增减，累计净收益；本金退款使用正金额冲回，无返回值。 */
    public static void activityCash(Room room, Player p, long amount) {
        observe(room);
        if (p.matchStats != null && !p.matchStats.settled) p.matchStats.activityNet += amount;
    }

    /** 输入清算前玩家、原债务与移出标记，在资产清空前冻结峰值、原因和个人称号。 */
    public static void eliminate(Room room, Player p, PendingDecision debt, boolean removed) {
        observe(room); MatchStats s = p.matchStats;
        s.removed = removed; s.finalNetWorth = 0;
        if (removed) { s.cause = "房主将你移出对局并清算资产。"; s.causeQuote = "本次行程提前结束，战绩保留到离场。"; }
        else if (debt != null) {
            String source = debt.sourceName == null ? paymentDescription(room, debt) : "「" + debt.sourceName + "」产生的" + paymentDescription(room, debt);
            s.cause = "最后一根稻草 · " + source + "，清算前待付 ¥" + money(debt.amount) + "。";
            s.causeQuote = "rent".equals(debt.type) ? "导航说前方畅通，余额说前方绝路。" : "命运关上了一扇门，顺手把押金也扣了。";
        }
        award(room, p); highlight(room, p); s.settled = true;
    }

    /** 输入已确定的赢家，在最终状态切换前冻结其净资产与战报；不会重复结算。 */
    public static void winner(Room room, Player p) {
        observe(room);
        if (p == null || p.matchStats == null || p.matchStats.settled) return;
        MatchStats s = p.matchStats; s.finalNetWorth = Rules.netWorth(room, p);
        s.cause = "成为最后一名未破产玩家，最终净资产 ¥" + money(s.finalNetWorth) + "。";
        s.causeQuote = s.rentReceived > 0 ? "你的财富自由，离不开朋友们的一脚一个准。" : "稳住，活到最后也是一种实力。";
        award(room, p); highlight(room, p); s.settled = true;
    }

    /** 输入房间、本人及指标，仅完整统计且正数并列最高时返回真。 */
    private static boolean leading(Room room, Player p, ToLongFunction<MatchStats> metric) {
        return p.matchStats.complete && metric.applyAsLong(p.matchStats) > 0
                && room.players.stream().allMatch(other -> other.matchStats != null && other.matchStats.complete
                && metric.applyAsLong(other.matchStats) <= metric.applyAsLong(p.matchStats));
    }

    /** 输入离场时房间与本人，按真实经历选择一个称号；不完整数据不参与全场最高评选。 */
    private static void award(Room room, Player p) {
        MatchStats s = p.matchStats;
        if (s.removed) { s.title = "提前到站"; s.titleQuote = "本次旅程提前下车，已发生的故事照常记账。"; }
        else if (leading(room, p, v -> v.rentReceived)) { s.title = "收租圣体"; s.titleQuote = "你只是站在那里，朋友的钱就自己过来了。"; }
        else if (leading(room, p, v -> v.rentPaid)) { s.title = "房东严选租客"; s.titleQuote = "每一位成功房东背后，都有一个努力路过的你。"; }
        else if (leading(room, p, v -> v.activityNet)) { s.title = "副业养家"; s.titleQuote = "骰子未必听话，副业收入已经到账。"; }
        else if (s.builtLevels >= 3 && leading(room, p, v -> v.builtLevels)) { s.title = "基建狂魔"; s.titleQuote = "钱可以没有，工地不能停。"; }
        else if (s.trades > 0 && leading(room, p, v -> v.trades)) { s.title = "商量一下都好说"; s.titleQuote = "别人靠掷骰，你靠会谈。"; }
        else if (s.jailCount > 0 && leading(room, p, v -> v.jailCount)) { s.title = "进去冷静一下"; s.titleQuote = "城市太浮躁，你选择进去沉淀。"; }
        else if (s.peakProperties >= 3 && leading(room, p, v -> v.peakProperties)) { s.title = "地产收藏家"; s.titleQuote = "地契可以不赚钱，但必须集齐。"; }
        else if (s.complete && s.finalNetWorth == 0 && s.initialWorth > 0 && s.peakNetWorth >= s.initialWorth * 2) { s.title = "曾经也是体面人"; s.titleQuote = "如果人生停在最高点，这局已经封神。"; }
        else if (s.complete && s.finalNetWorth == 0 && room.players.stream().noneMatch(other -> other.bankrupt)) { s.title = "带薪围观"; s.titleQuote = "提前结束财富焦虑，进入朋友观察模式。"; }
        else { s.title = "城市生存体验官"; s.titleQuote = "这一趟，有自己的故事，也有自己的账单。"; }
    }

    /** 输入离场玩家，优先选真实收租贡献者，其次选最大实付租金；零记录不生成名场面。 */
    private static void highlight(Room room, Player p) {
        MatchStats s = p.matchStats;
        var payer = s.rentByPayer.entrySet().stream().max(Comparator.comparingLong(e -> e.getValue()));
        if (payer.isPresent()) {
            Player other = player(room, payer.get().getKey());
            s.highlight = "重点感谢 · " + (other == null ? "一位同行玩家" : other.name) + "累计向你支付现金租金 ¥" + money(payer.get().getValue()) + "。";
            s.highlightQuote = "你们之间没有合同，但合作非常稳定。";
        } else if (s.largestRent > 0) {
            s.highlight = "最大一笔心痛 · 在「" + s.largestRentTile + "」实付现金租金 ¥" + money(s.largestRent) + "。";
            s.highlightQuote = "路过只花了一秒，钱包缓了很久。";
        } else if (s.builtLevels > 0) {
            s.highlight = "施工记录 · 累计建成 " + s.builtLevels + " 级建筑（含免费升级）。";
            s.highlightQuote = "这座城市的天际线，有你的一份贡献。";
        }
    }

    /** 输入实际金额，返回带千分位的展示文字。 */
    private static String money(long amount) { return String.format("%,d", amount); }
}
