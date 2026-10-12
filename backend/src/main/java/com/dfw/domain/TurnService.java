package com.dfw.domain;

import java.util.*;
import com.dfw.domain.rules.RuleCatalog;
import static com.dfw.domain.GameSupport.*;

/** 回合、移动、落点和在线暂停规则。 */
public class TurnService {
    private final EconomyService economy;
    private final RandomSource random;
    private final EffectResolver effects;
    private final RuleCatalog rules;
    /** 输入经济规则与独立随机源，构造回合服务。 */
    public TurnService(EconomyService economy, RandomSource random, EffectResolver effects, RuleCatalog rules) {
        this.economy = economy; this.random = random; this.effects = effects; this.rules = rules;
        effects.register("MOVE", (room, effect) -> { move(room, player(room, effect.playerId), (int) effect.amount); if (effect.message != null) log(room, effect.message); });
        effects.register("SETTLE_TILE", (room, effect) -> settle(room, player(room, effect.playerId)));
        effects.register("PAY_FEE", (room, effect) -> { if (economy.charge(room, player(room, effect.playerId), effect.amount, effect.targetId, effect.tileId)) advance(room); });
    }
    /** 输入玩家和数量，增加私人道具，不记录种类与数量。 */
    public void giveCards(Player player, int count) {
        for (int i = 0; i < count; i++) { Card card = new Card(); card.id = random.id(); card.kind = Rules.ITEMS[random.nextInt(Rules.ITEMS.length)]; player.hand.add(card); }
    }
    /** 输入房间，仅余一人时结束游戏并返回真。 */
    public boolean checkWinner(Room room) {
        List<Player> alive = room.players.stream().filter(p -> !p.bankrupt).toList();
        if (alive.size() > 1) return false;
        room.status = "finished"; room.winnerId = alive.isEmpty() ? null : alive.getFirst().id;
        room.pending = null; room.trade = null; room.pausedReason = null;
        log(room, alive.isEmpty() ? "本局结束" : alive.getFirst().name + " 成为最后的赢家！"); return true;
    }
    /** 输入房间，切换下一行动者并重置回合标志。 */
    public void advance(Room room) {
        if (checkWinner(room)) return;
        do { room.turnIndex = (room.turnIndex + 1) % room.turnOrder.size(); } while (current(room).bankrupt);
        room.turnNumber++; room.phase = "pre_roll"; room.pending = null; room.dice = null;
        room.activeCardUsed = false; room.eventDrawn = false; room.upgradedTiles.clear();
    }
    /** 输入费用参数，支付、偿债或破产后推进回合。 */
    public void charge(Room room, Player player, long amount, String creditorId, Integer tileId) {
        if (room.runtime == null) { if (economy.charge(room, player, amount, creditorId, tileId)) advance(room); return; }
        GameEffect effect = effects.effect("PAY_FEE", player); effect.amount = amount; effect.targetId = creditorId; effect.tileId = tileId;
        effect.resumesDecision = room.pending != null && (room.phase.equals("debt") || room.phase.equals("rent")); effects.submit(room, effect);
    }
    /** 输入步数和可选移动后说明，按移动、经过判定、落点顺序展开效果。 */
    public void moveAndSettle(Room room, Player player, int steps, String message) {
        if (room.runtime == null) { move(room, player, steps); if (message != null) log(room, message); settle(room, player); return; }
        GameEffect movement = effects.effect("MOVE", player); movement.amount = steps; movement.message = message;
        effects.submit(room, movement); effects.submit(room, effects.effect("SETTLE_TILE", player));
    }
    /** 输入玩家和有符号步数，逐格处理路障和正向起点收入。 */
    public void move(Room room, Player player, int steps) {
        Motion motion = new Motion(); motion.id = random.id(); motion.playerId = player.id; motion.from = player.position;
        for (int i = 0; i < Math.abs(steps); i++) {
            player.position = Math.floorMod(player.position + Integer.signum(steps), room.board.size()); motion.path.add(player.position);
            if (steps > 0 && player.position == 0) { player.cash += Rules.START_INCOME; log(room, player.name + " 经过起点，获得 " + Rules.START_INCOME); }
            Tile tile = room.board.get(player.position);
            if (tile.barrier) { tile.barrier = false; log(room, player.name + " 在 " + tile.name + " 被路障拦下"); break; }
        }
        room.motion = motion;
    }
    /** 输入落点玩家，处理地产或特殊格，每回合最多抽一次事件。 */
    public void settle(Room room, Player player) {
        Tile tile = room.board.get(player.position); room.phase = "manage"; room.pending = null;
        switch (tile.type) {
            case "jail" -> { player.skipTurns = 1; player.jailed = true; log(room, player.name + " 进入监狱，下次行动休息"); }
            case "chance", "fortune" -> {
                if (room.eventDrawn) return; room.eventDrawn = true; int event = random.nextInt(6);
                if (tile.type.equals("fortune")) {
                    if (event < 4) { int amount = new int[]{300, 500, 800, 1000}[event]; player.cash += amount; log(room, player.name + " 在财富格获得 " + amount); }
                    else { giveCards(player, event == 4 ? 1 : 2); log(room, player.name + " 收到一份私人补给"); }
                } else switch (event) {
                    case 0 -> { player.cash += 300; log(room, player.name + " 在机会格获得 300"); }
                    case 1 -> charge(room, player, 300, null, tile.id);
                    case 2, 3 -> moveAndSettle(room, player, event == 2 ? 3 : -3, player.name + " 因随机事件" + (event == 2 ? "前进" : "后退") + "三格");
                    case 4 -> { player.skipTurns = 1; log(room, player.name + " 下次行动休息"); }
                    default -> { giveCards(player, 1); log(room, player.name + " 收到一份私人补给"); }
                }
            }
            case "property" -> {
                if (tile.ownerId == null) { room.phase = "buy"; room.pending = pending("buy", player, tile.id, tile.price, null); }
                else if (tile.ownerId.equals(player.id)) {
                    if (!tile.mortgaged && tile.level < Rules.MAX_LEVEL && !room.upgradedTiles.contains(tile.id)) { room.phase = "upgrade"; room.pending = pending("upgrade", player, tile.id, Rules.buildCost(tile), null); }
                } else if (!tile.mortgaged) {
                    if (hasRentDefense(room, player)) { room.phase = "rent"; room.pending = pending("rent", player, tile.id, Rules.rent(tile), tile.ownerId); }
                    else charge(room, player, Rules.rent(tile), tile.ownerId, tile.id);
                }
            }
            default -> { }
        }
    }
    /** 输入房间和玩家，按绑定版本确定手牌是否存在租金防御能力。 */
    private boolean hasRentDefense(Room room, Player player) {
        if (!rules.configured(room)) return player.hand.stream().anyMatch(card -> card.kind.equals("immunity"));
        var cards = rules.bound(room).getCards();
        return player.hand.stream().anyMatch(card -> cards.containsKey(card.kind) && cards.get(card.kind).activation().equals("RENT"));
    }
    /** 输入房间和时间，计算房主、断线暂停、服刑跳过和交易计时。 */
    public void refresh(Room room, long now) {
        Player host = player(room, room.hostId);
        if (host == null || !host.online) room.players.stream().filter(p -> p.online).findFirst().ifPresent(p -> room.hostId = p.id);
        if (!room.status.equals("playing")) { room.pausedReason = null; return; }
        for (int i = 0; !rules.configured(room) && i < room.players.size() * 2; i++) {
            Player actor = current(room);
            if (actor == null || !actor.online || !room.phase.equals("pre_roll") || actor.skipTurns == 0 || room.trade != null) break;
            actor.skipTurns--; actor.jailed = false; log(room, actor.name + " 休息一回合"); advance(room);
            if (!room.status.equals("playing")) return;
        }
        Player actor = current(room);
        Player offline = room.players.stream().filter(p -> !p.online && ((actor != null && p.id.equals(actor.id)) || (room.trade != null && p.id.equals(room.trade.toId)))).findFirst().orElse(null);
        room.pausedReason = offline == null ? null : "等待 " + offline.name + " 重新连接";
        if (room.trade != null) {
            if (room.trade.runningSince != null) room.trade.remainingMs = Math.max(0, room.trade.remainingMs - (now - room.trade.runningSince));
            room.trade.runningSince = room.pausedReason == null ? now : null;
            if (room.trade.remainingMs <= 0) { room.trade = null; log(room, "交易等待超时，报价已取消"); refresh(room, now); }
        }
    }
    /** 输入配置对局，在 TURN_START 按原规则跳过在线玩家的一次休息回合。 */
    public boolean skipCurrentTurn(Room room) {
        Player actor = current(room);
        if (actor == null || !actor.online || actor.skipTurns == 0 || room.trade != null) return false;
        actor.skipTurns--; actor.jailed = false; log(room, actor.name + " 休息一回合"); advance(room); return true;
    }
}
