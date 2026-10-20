package com.dfw.domain;

import java.math.BigInteger;
import java.util.Objects;
import static com.dfw.domain.GameSupport.*;

/** 地产附加效果、无修正租金与神威生命周期，沿房间快照原子保存。 */
public final class PropertyEffects {
    private PropertyEffects() {}
    /** 输入地产，返回是否处于有效神威冻结中。 */
    public static boolean frozen(Tile tile) { return tile != null && tile.kamuiCasterId != null && tile.kamuiTurns > 0; }
    /** 输入玩家，返回本局有效手牌上限，最低一张。 */
    public static int handLimit(Player player) { return Math.max(1, 6 - player.handLimitReduction); }
    /** 输入房间和地产，返回不含抵押、房东及临时状态修正的等级租金。 */
    public static long baseRent(Room room, Tile tile) {
        EconomyRules economy = Rules.economy(room); int level = tile.ownerId == null ? 0 : tile.level;
        return economy.derived("rent." + tile.price + "." + level, Math.multiplyExact(tile.price, economy.rentBasePercent) / 100 * Rules.RENT_MULTIPLIERS[level]);
    }
    /** 输入基础租金与叠加层数，以精确分数计算乘数并在最后向下取整。 */
    public static long concertRent(long base, int stacks) {
        return BigInteger.valueOf(base).multiply(BigInteger.valueOf(6).pow(stacks)).divide(BigInteger.valueOf(5).pow(stacks)).longValueExact();
    }
    /** 输入地产，返回剩余护盾层数，兼容旧快照的一次护盾标记。 */
    public static int shieldCharges(Tile tile) { return Math.max(tile.demolitionShieldCharges, tile.demolitionShield ? 1 : 0); }
    /** 输入房间及将受损地产，返回是否挡住本次建筑损失；每次消耗一层，冻结不消费。 */
    public static boolean preventDemolition(Room room, Tile tile) {
        if (frozen(tile)) return true;
        if (tile.level > 0 && shieldCharges(tile) > 0) {
            tile.demolitionShieldCharges = shieldCharges(tile) - 1; tile.demolitionShield = tile.demolitionShieldCharges > 0;
            log(room, tile.name + " 的防拆护盾抵挡了本次建筑拆除，剩余 " + tile.demolitionShieldCharges + " 层"); return true;
        }
        return false;
    }
    /** 输入房间和行动结束者，递减其后续个人回合的神威；休息也计数。 */
    public static void endTurn(Room room, Player owner) {
        for (Tile tile : room.board) if (frozen(tile) && Objects.equals(tile.kamuiCasterId, owner.id) && room.turnNumber > tile.kamuiAppliedTurn) {
            tile.kamuiTurns--; if (tile.kamuiTurns == 0) release(room, tile);
        }
    }
    /** 输入房间和已淘汰施术者，立即解除其全部神威。 */
    public static void eliminateCaster(Room room, String id) {
        for (Tile tile : room.board) {
            if (Objects.equals(tile.kamuiCasterId, id)) release(room, tile);
            if(Objects.equals(tile.zeroCasterId,id)) { tile.zeroCasterId=null; tile.zeroUntilTurn=0; }
        }
    }
    /** 输入房间及到期地产，恢复保留的附加效果并公开记录。 */
    private static void release(Room room, Tile tile) {
        MotionSegment segment = BoardEffect.begin("kamuiRelease", player(room, tile.kamuiCasterId));
        segment.boardEffect.beforeTiles.add(BoardEffect.appearance(tile));
        tile.kamuiCasterId = null; tile.kamuiTurns = 0; log(room, tile.name + " 已离开神威空间，地产及原有附加效果恢复");
        segment.boardEffect.afterTiles.add(BoardEffect.appearance(tile)); BoardEffect.publish(room, segment);
    }
}
