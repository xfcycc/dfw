package com.dfw.domain;

import java.util.*;
import com.dfw.domain.rules.RuleCatalog;
import com.dfw.domain.rules.RuleRuntime;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 房间生命周期和操作路由；各规则由对应领域服务执行。 */
public class GameEngine {
    public final TurnService turns;
    /** 通用流程驱动的新局效果结算器。 */
    public final EffectResolver effects;
    /** 配置版本的阶段与道具解释器。 */
    public final RuleRuntime ruleRuntime;
    private final RuleCatalog rules;
    private final MapService maps;
    private final EconomyService economy;
    private final ItemService items;
    private final TradeService trades;
    private final RandomSource random;
    /** 输入独立随机源，组装无网络与数据库依赖的游戏规则。 */
    public GameEngine(RandomSource random, RuleCatalog rules) {
        this.random = random; this.rules = rules; maps = new MapService(); economy = new EconomyService();
        effects = new EffectResolver(random);
        turns = new TurnService(economy, random, effects, rules);
        ruleRuntime = new RuleRuntime(rules, effects, turns, random);
        items = new ItemService(turns, random, effects, rules, ruleRuntime); trades = new TradeService(economy, random, effects);
    }
    /** 输入房间，返回是否需由通用流程继续配置阶段。 */
    public boolean needsAdvance(Room room) { return rules.configured(room) && ruleRuntime.needsAdvance(room); }
    /** 输入房间，按绑定版本推进至下一等待点，保留 classic-v1 的旧结算路径。 */
    public void advanceUntilWait(Room room) {
        if (rules.configured(room)) ruleRuntime.advanceUntilWait(room);
        else { effects.drain(room); turns.refresh(room, System.currentTimeMillis()); }
    }
    /** 输入昵称和颜色索引，返回新的大厅玩家。 */
    public Player createPlayer(String name, int index) {
        Player player = new Player(); player.id = random.id(); player.name = name.strip().isEmpty() ? "玩家 " + (index + 1) : name.strip(); player.color = Rules.COLORS[index]; return player;
    }
    /** 输入房间号和首位玩家，返回大厅房间。 */
    public Room createRoom(String code, Player player) { Room room = new Room(); room.code = code; room.hostId = player.id; room.players.add(player); return room; }
    /** 输入房间，初始化地图、行动顺序、现金及私人手牌。 */
    private void start(Room room) {
        require(room.players.size() >= Rules.MIN_PLAYERS && room.players.size() <= Rules.MAX_PLAYERS, "需要 2～6 名玩家");
        require(room.players.stream().allMatch(p -> p.ready && p.online), "请等待所有玩家连接并准备");
        room.status = "playing"; room.gameId = random.id(); room.seed = random.id(); room.board = maps.generate(room.players.size(), room.seed);
        room.turnOrder = new ArrayList<>(room.players.stream().map(p -> p.id).toList());
        for (int i = room.turnOrder.size() - 1; i > 0; i--) Collections.swap(room.turnOrder, i, random.nextInt(i + 1));
        room.turnIndex = 0; room.turnNumber = 1; room.phase = "pre_roll"; room.pending = null; room.trade = null;
        room.dice = null; room.motion = null; room.activeCardUsed = false; room.eventDrawn = false; room.upgradedTiles.clear();
        for (Player player : room.players) { reset(player); player.cash = Rules.INITIAL_CASH; turns.giveCards(player, Rules.INITIAL_CARDS); }
        log(room, "新地图已生成，" + room.players.size() + " 人的财富旅程开始！");
    }
    /** 输入玩家，重置对局资产与淘汰状态，保留身份与连接。 */
    private void reset(Player player) { player.cash = 0; player.position = 0; player.bankrupt = false; player.skipTurns = 0; player.jailed = false; player.eliminationOrder = null; player.hand.clear(); }
    /** 输入房间和身份操作，在候选房间中应用规则，由应用层持久化。 */
    public void apply(Room room, String playerId, Action action) {
        Player player = player(room, playerId); require(player != null, "你的席位已经失效");
        switch (action.type) {
            case "ready" -> { require(room.status.equals("lobby"), "游戏已开始"); player.ready = action.value; return; }
            case "start" -> { require(room.hostId.equals(player.id) && room.status.equals("lobby"), "只有房主可开始游戏"); start(room); return; }
            case "newGame" -> {
                require(room.hostId.equals(player.id) && room.status.equals("finished"), "只有房主可在结束后再开一局");
                room.status = "lobby"; room.board.clear(); room.gameId = null; room.turnOrder.clear(); room.turnIndex = 0; room.turnNumber = 0;
                room.phase = "pre_roll"; room.pending = null; room.trade = null; room.winnerId = null; room.dice = null; room.motion = null; room.logs.clear();
                for (Player p : room.players) { reset(p); p.ready = false; } return;
            }
            case "removePlayer" -> {
                require(room.hostId.equals(player.id), "只有房主可移出玩家"); require(!room.status.equals("finished"), "请先返回大厅，再管理已结束对局的席位");
                Player target = player(room, action.playerId); require(target != null && !target.id.equals(player.id) && !target.online, "只能移出其他离线玩家");
                if (room.status.equals("lobby")) room.players.remove(target);
                else {
                    require(!target.bankrupt, "该玩家已退出竞争"); boolean isCurrent = current(room) == target;
                    PendingDecision debt = room.pending != null && room.pending.playerId.equals(target.id) && room.pending.type.equals("debt") ? room.pending : null;
                    economy.eliminate(room, target, debt == null ? null : debt.creditorId, debt == null ? 0 : debt.amount);
                    if (isCurrent) turns.advance(room); else turns.checkWinner(room);
                } return;
            }
            default -> { }
        }
        require(room.status.equals("playing") && !player.bankrupt, "你当前不能进行游戏操作"); require(room.pausedReason == null, room.pausedReason == null ? "游戏已暂停" : room.pausedReason);
        if (room.trade != null) { trades.respond(room, player, action); return; }
        require(current(room) == player, "还没有轮到你");
        boolean configured = rules.configured(room);
        if (configured) ruleRuntime.validateAction(room, action.type);
        switch (action.type) {
            case "roll" -> {
                require(room.phase.equals("pre_roll"), "本回合已移动");
                if (configured) room.runtime.rollMode = "RANDOM";
                else { room.dice = random.nextInt(6) + 1; log(room, player.name + " 掷出 " + room.dice + " 点"); turns.moveAndSettle(room, player, room.dice, null); }
            }
            case "card" -> items.use(room, player, action);
            case "buy", "upgrade" -> {
                require(room.phase.equals(action.type) && room.pending != null && room.pending.tileId != null, "当前没有这项操作");
                Tile tile = room.board.get(room.pending.tileId); long cost = action.type.equals("buy") ? tile.price : Rules.buildCost(tile);
                require(player.cash >= cost, "现金不足，可先管理资产或放弃");
                if (action.type.equals("buy")) { require(tile.ownerId == null, "地产已被购买"); tile.ownerId = player.id; log(room, player.name + " 买下 " + tile.name); }
                else { require(Objects.equals(tile.ownerId, player.id) && !tile.mortgaged && tile.level < Rules.MAX_LEVEL && !room.upgradedTiles.contains(tile.id), "这块地产当前不可升级"); tile.level++; room.upgradedTiles.add(tile.id); log(room, player.name + " 将 " + tile.name + " 升至 " + tile.level + " 级"); }
                player.cash -= cost; room.phase = "manage"; room.pending = null;
            }
            case "pass" -> { require(room.phase.equals("buy") || room.phase.equals("upgrade"), "当前没有可放弃的选项"); room.phase = "manage"; room.pending = null; }
            case "rentDecision" -> { require(room.phase.equals("rent") && room.pending != null, "当前没有待支付的租金"); if (action.useCard) items.immune(room, player); else turns.charge(room, player, room.pending.amount, room.pending.creditorId, room.pending.tileId); }
            case "sellBuilding", "mortgage", "redeem", "sellLand" -> {
                economy.manage(room, player, action.type, action.tileId);
                if (room.phase.equals("debt") && room.pending != null && player.cash >= room.pending.amount) turns.charge(room, player, room.pending.amount, room.pending.creditorId, room.pending.tileId);
                else if (room.pending != null && room.pending.type.equals("upgrade")) {
                    Tile tile = room.board.get(room.pending.tileId); if (!Objects.equals(tile.ownerId, player.id) || tile.mortgaged || tile.level >= Rules.MAX_LEVEL) { room.pending = null; room.phase = "manage"; }
                }
            }
            case "tradeOffer" -> trades.offer(room, player, action);
            case "endTurn" -> { require(room.phase.equals("manage"), "请先移动并处理当前事项"); if (!configured) turns.advance(room); }
            default -> throw new GameError("当前操作不可用");
        }
        if (configured) ruleRuntime.accepted(room, action.type);
    }
}
