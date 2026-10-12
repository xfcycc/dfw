package com.dfw.domain.rules;

import com.dfw.domain.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

/** 有限条件 AST，禁止反射、任意字段访问或脚本执行。 */
public final class RuleExpression {
    private static final Set<String> NUMBER_REFS = Set.of("action.point", "action.tileId", "actor.cash", "actor.position", "targetTile.id", "targetTile.level", "targetTile.forwardDistance", "turn.dice");
    private static final Set<String> BOOLEAN_REFS = Set.of("targetTile.exists", "targetTile.barrier", "targetPlayer.exists", "targetPlayer.bankrupt", "targetPlayer.jailed", "turn.activeCardUsed");
    private static final Set<String> STRING_REFS = Set.of("actor.id", "actor.name", "action.playerId", "targetTile.type", "targetTile.ownerId", "targetPlayer.id", "turn.phase");
    private RuleExpression() {}
    /** 条件求值期间的领域上下文，不持有数据库或 Spring 对象。 */
    public record Context(Room room, Player actor, Action action) {}
    /** 编译后的不可变类型和求值函数。 */
    public record Node(String type, Function<Context, Object> evaluate) {
        /** 输入当前领域上下文，返回表达式的值。 */
        public Object value(Context context) { return evaluate.apply(context); }
        /** 输入当前领域上下文，返回布尔条件是否成立。 */
        public boolean matches(Context context) { return Boolean.TRUE.equals(value(context)); }
    }
    /** 输入 JSON AST，校验并编译为只读表达式。 */
    public static Node compile(Object expression) { return compile(expression, 0); }
    /** 输入 AST 和深度，递归校验类型、字段白名单及有限节点大小。 */
    private static Node compile(Object expression, int depth) {
        check(depth < 24, "表达式嵌套过深");
        if (expression == null) return new Node("NULL", context -> null);
        if (expression instanceof Boolean) return new Node("BOOLEAN", context -> expression);
        if (expression instanceof Number) return new Node("NUMBER", context -> expression);
        if (expression instanceof String) return new Node("STRING", context -> expression);
        check(expression instanceof Map<?, ?>, "表达式必须是常量或 AST 对象");
        Map<?, ?> map = (Map<?, ?>) expression;
        if (map.containsKey("ref")) {
            check(map.keySet().equals(Set.of("ref")), "ref 不允许其他字段");
            String ref = String.valueOf(map.get("ref")); String type = referenceType(ref);
            return new Node(type, context -> reference(context, ref));
        }
        String op = String.valueOf(map.get("op"));
        if (op.equals("ALL") || op.equals("ANY")) {
            check(map.keySet().equals(Set.of("op", "args")) && map.get("args") instanceof List<?>, "ALL/ANY 参数不正确");
            List<?> args = (List<?>) map.get("args"); check(!args.isEmpty() && args.size() <= 32, "条件数量不正确");
            List<Node> nodes = args.stream().map(value -> compile(value, depth + 1)).toList(); nodes.forEach(node -> expect(node, "BOOLEAN"));
            return new Node("BOOLEAN", context -> op.equals("ALL") ? nodes.stream().allMatch(node -> node.matches(context)) : nodes.stream().anyMatch(node -> node.matches(context)));
        }
        if (op.equals("NOT")) {
            check(map.keySet().equals(Set.of("op", "value")), "NOT 参数不正确"); Node node = compile(map.get("value"), depth + 1); expect(node, "BOOLEAN");
            return new Node("BOOLEAN", context -> !node.matches(context));
        }
        check(Set.of("EQ", "NE", "GT", "GTE", "LT", "LTE").contains(op), "未知表达式操作：" + op);
        check(map.keySet().equals(Set.of("op", "left", "right")), "比较参数不正确");
        Node left = compile(map.get("left"), depth + 1), right = compile(map.get("right"), depth + 1);
        if (op.equals("EQ") || op.equals("NE")) check(left.type.equals(right.type) || left.type.equals("NULL") || right.type.equals("NULL"), "比较类型不匹配");
        else { expect(left, "NUMBER"); expect(right, "NUMBER"); }
        return new Node("BOOLEAN", context -> compare(op, left.value(context), right.value(context)));
    }
    /** 输入比较类型和值，空数值只允许相等比较，不强制转换字符串。 */
    private static boolean compare(String op, Object left, Object right) {
        boolean equal = left instanceof Number a && right instanceof Number b ? decimal(a).compareTo(decimal(b)) == 0 : Objects.equals(left, right);
        if (op.equals("EQ")) return equal;
        if (op.equals("NE")) return !equal;
        if (!(left instanceof Number a) || !(right instanceof Number b)) return false;
        int value = decimal(a).compareTo(decimal(b));
        return switch (op) { case "GT" -> value > 0; case "GTE" -> value >= 0; case "LT" -> value < 0; default -> value <= 0; };
    }
    /** 输入白名单路径，返回其声明类型；未知或敏感路径拒绝。 */
    public static String referenceType(String ref) {
        if (NUMBER_REFS.contains(ref)) return "NUMBER";
        if (BOOLEAN_REFS.contains(ref)) return "BOOLEAN";
        if (STRING_REFS.contains(ref)) return "STRING";
        throw new IllegalArgumentException("不允许的规则引用：" + ref);
    }
    /** 输入上下文和白名单路径，显式读取允许的领域字段。 */
    public static Object reference(Context context, String ref) {
        Room room = context.room; Player actor = context.actor; Action action = context.action;
        Tile tile = GameSupport.tile(room, action.tileId); Player target = GameSupport.player(room, action.playerId);
        return switch (ref) {
            case "actor.id" -> actor.id; case "actor.name" -> actor.name; case "actor.cash" -> actor.cash; case "actor.position" -> actor.position;
            case "action.point" -> action.point; case "action.tileId" -> action.tileId; case "action.playerId" -> action.playerId;
            case "targetTile.exists" -> tile != null; case "targetTile.id" -> tile == null ? null : tile.id;
            case "targetTile.type" -> tile == null ? null : tile.type; case "targetTile.ownerId" -> tile == null ? null : tile.ownerId;
            case "targetTile.level" -> tile == null ? null : tile.level; case "targetTile.barrier" -> tile != null && tile.barrier;
            case "targetTile.forwardDistance" -> tile == null || room.board.isEmpty() ? null : Math.floorMod(tile.id - actor.position, room.board.size());
            case "targetPlayer.exists" -> target != null; case "targetPlayer.id" -> target == null ? null : target.id;
            case "targetPlayer.bankrupt" -> target != null && target.bankrupt; case "targetPlayer.jailed" -> target != null && target.jailed;
            case "turn.phase" -> room.phase; case "turn.activeCardUsed" -> room.activeCardUsed; case "turn.dice" -> room.dice;
            default -> throw new IllegalArgumentException("不允许的规则引用：" + ref);
        };
    }
    /** 输入数值，返回精确十进制比较值。 */
    private static BigDecimal decimal(Number number) { return new BigDecimal(number.toString()); }
    /** 输入已编译表达式与预期类型，拒绝类型不符配置。 */
    public static void expect(Node node, String type) { check(node.type.equals(type), "表达式应为 " + type); }
    /** 输入声明值，返回整数参数，不截断小数或接受空引用。 */
    public static long integer(Object value) {
        if (!(value instanceof Number number)) throw new GameError("规则数值参数不可用");
        try { return decimal(number).longValueExact(); } catch (ArithmeticException error) { throw new GameError("规则数值参数必须是整数"); }
    }
    /** 输入配置约束及诊断，不成立时拒绝加载。 */
    public static void check(boolean valid, String message) { if (!valid) throw new IllegalArgumentException(message); }
}
