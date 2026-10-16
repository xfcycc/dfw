package com.dfw.domain;

import java.util.*;
import static com.dfw.domain.GameError.require;
import static com.dfw.domain.GameSupport.*;

/** 活动角轮换、多人输入和固定随机结果；调用方负责房间串行事务与广播。 */
public final class ActivityService {
    public static final List<String> KINDS = List.of("casino", "horse", "roulette", "packet", "rps");
    public static final Map<String, String> NAMES = Map.of("casino", "暖暖赌场", "horse", "小镇赛马", "roulette", "轮盘对决", "packet", "拼手气红包", "rps", "猜拳擂台", "treasure", "贪心宝箱", "bridge", "三关玻璃桥", "minority", "少数派夜市", "blindbox", "丰收盲盒");
    private static final Map<String, String> VARIANTS = Map.of("casino", "treasure", "horse", "bridge", "rps", "minority", "packet", "blindbox");
    private static final Map<String, Long> BASE = Map.ofEntries(Map.entry("casinoSmallStake",250L), Map.entry("casinoSmallPrize",500L), Map.entry("casinoPointStake",250L), Map.entry("casinoPointPrize",1500L), Map.entry("horseStake",250L), Map.entry("horsePrize",2500L), Map.entry("rouletteDebt",1000L), Map.entry("packetBase",500L), Map.entry("packetPerPlayer",500L), Map.entry("rpsPrize",1500L), Map.entry("treasureStake",250L), Map.entry("blindboxRegular",250L), Map.entry("blindboxHiddenBase",500L), Map.entry("blindboxHiddenPerPlayer",250L));
    private final RandomSource random;
    private final EconomyService economy;
    /** 输入服务端随机源及现有费用服务，无返回值。 */
    public ActivityService(RandomSource random, EconomyService economy) { this.random = random; this.economy = economy; }

    /** 输入新局，固定周期、活动角和最终金额；旧局不调用。 */
    public void initialize(Room room) {
        ActivityRotation r = new ActivityRotation(); r.period = room.players.size() * 5; r.variantsEnabled = true;
        for (Tile t : room.board) if (t.type.equals("activity")) r.tileIds.add(t.id);
        require(r.tileIds.size() == 2, "活动角数量不正确");
        BASE.forEach((key, base) -> r.money.put(key, room.economy.money("activity." + key, base)));
        require(r.money.get("packetPerPlayer") >= 1, "红包金额至少为一元");
        r.current = pair(r, List.of()); r.next = pair(r, r.current); room.activityRotation = r; room.activity = null; applyTiles(room);
    }
    /** 输入轮换队列及上期分配，返回不同且各角不重复上期的两项玩法。 */
    private List<String> pair(ActivityRotation r, List<String> previous) {
        List<String> result = new ArrayList<>();
        for (int slot = 0; slot < 2; slot++) {
            int found = -1;
            while (found < 0) {
                for (int i = 0; i < r.queue.size(); i++) if (!result.contains(r.queue.get(i)) && (previous.isEmpty() || !category(previous.get(slot)).equals(r.queue.get(i)))) { found = i; break; }
                if (found < 0) { List<String> batch = new ArrayList<>(KINDS); shuffle(batch); r.queue.addAll(batch); }
            }
            result.add(r.queue.remove(found));
        }
        return result.stream().map(kind -> r.variantsEnabled && VARIANTS.containsKey(kind) && random.nextInt(2) == 1 ? VARIANTS.get(kind) : kind).toList();
    }
    /** 输入具体玩法标识，返回原五类队列键，避免变体改变类别权重。 */
    private String category(String kind) { return VARIANTS.entrySet().stream().filter(entry -> entry.getValue().equals(kind)).map(Map.Entry::getKey).findFirst().orElse(kind); }
    /** 输入列表，使用服务端随机源原地洗牌，无返回值。 */
    private <T> void shuffle(List<T> values) { for (int i = values.size() - 1; i > 0; i--) Collections.swap(values, i, random.nextInt(i + 1)); }
    /** 输入房间，刷新两个角的公开名称和玩法标识。 */
    private void applyTiles(Room room) { for (int i = 0; i < 2; i++) { Tile tile = room.board.get(room.activityRotation.tileIds.get(i)); tile.activityKind = room.activityRotation.current.get(i); tile.name = NAMES.get(tile.activityKind); } }
    /** 输入已完整结束回合的房间，剩余三至一回合记录公开预告，并在周期边界原子替换活动，无返回值。 */
    public void endTurn(Room room) {
        ActivityRotation r = room.activityRotation; if (r == null) return;
        require(room.activity == null && room.pending == null && room.trade == null, "请先完成活动和结算");
        r.completedTurns++;
        if (r.completedTurns % r.period == 0) { r.revision++; r.current = r.next; r.next = pair(r, r.current); applyTiles(room); log(room, "活动角焕新：" + String.join("、", r.current.stream().map(NAMES::get).toList())); }
        int remaining = r.period - r.completedTurns % r.period;
        if (remaining <= 3) log(room, "活动预告：还有 " + remaining + " 个回合轮换，" + r.tileIds.get(0) + " 号活动角将变为「" + NAMES.get(r.next.get(0)) + "」，" + r.tileIds.get(1) + " 号活动角将变为「" + NAMES.get(r.next.get(1)) + "」");
    }
    /** 输入房间及金额键，返回开局冻结的实际金额。 */
    private long money(Room room, String key) { return room.activityRotation.money.get(key); }
    /** 输入落点玩家及活动格，建立独立活动，不移动正常回合游标。 */
    public void open(Room room, Player trigger, Tile tile) {
        require(room.activityRotation != null && room.activity == null && room.pending == null, "活动结算上下文不可用");
        CornerActivity a = new CornerActivity(); a.id = random.id(); a.kind = tile.activityKind; a.tileId = tile.id; a.triggerId = trigger.id; a.returnPhase = room.phase;
        a.participants.addAll(room.players.stream().filter(p -> !p.bankrupt).map(p -> p.id).toList()); a.contenders.addAll(a.participants); a.startedAt = System.currentTimeMillis();
        if (a.kind.equals("casino") || a.kind.equals("treasure")) { a.participants.clear(); a.participants.add(trigger.id); a.contenders = new ArrayList<>(a.participants); }
        if (a.kind.equals("packet")) packets(room, a);
        if (a.kind.equals("blindbox")) { a.hiddenBox = random.nextInt(20) + 1; a.limitedBox = random.nextInt(100) < 10; }
        room.activity = a; log(room, trigger.name + " 开启「" + NAMES.get(a.kind) + "」");
    }
    /** 输入房间及红包活动，按固定底金加人数金额生成隐藏红包池；参与者顺序对应红包序号，旧快照缺失底金时按零计算。 */
    private void packets(Room room, CornerActivity a) {
        int n = a.participants.size(); long total = Math.addExact(room.activityRotation.money.getOrDefault("packetBase", 0L), Math.multiplyExact(money(room,"packetPerPlayer"), n)), min = (total + 99) / 100, max = total * 9 / 10, remaining = total;
        require(n >= 2 && min * n <= total && max * n >= total, "红包份额范围不可用");
        List<Long> amounts = new ArrayList<>();
        for (int i = 0; i < n; i++) { int rest = n - i - 1; long low = Math.max(min, remaining - max * rest), high = Math.min(max, remaining - min * rest); long amount = low + randomAmount(high - low + 1); amounts.add(amount); remaining -= amount; }
        shuffle(amounts);
        for (int i = 0; i < n; i++) { String id = a.participants.get(i); a.packets.put(id, amounts.get(i)); a.bonuses.put(id, random.nextInt(100) < 10 ? amounts.get(i) : 0L); }
    }
    /** 输入正整数上界，返回范围内随机金额；大额配置使用53位随机样本。 */
    private long randomAmount(long bound) { if (bound <= Integer.MAX_VALUE) return random.nextInt((int)bound); long bits = ((long)random.nextInt(1 << 26) << 27) | random.nextInt(1 << 27); return (long)(bits / 9007199254740992.0 * bound); }
    /** 输入活动操作类型，返回是否属于活动专用输入。 */
    public static boolean isAction(String type) { return type.equals("activityInput"); }
    /** 输入房间、玩家与活动选择，验证独立轮次后提交，重复提交不改变原结果。 */
    public void input(Room room, Player actor, Action action) {
        CornerActivity a = room.activity;
        require(a != null && Objects.equals(a.id, action.activityId) && Objects.equals(action.activityRound, a.round), "活动已进入下一阶段，请同步后操作");
        require(!actor.bankrupt && a.participants.contains(actor.id), "你不在本次活动参与名单内");
        require(room.pending == null && room.trade == null && a.endsAt == null, "请等待当前展示或结算完成");
        if (a.kind.equals("roulette")) { rouletteInput(room, a, actor, action); return; }
        if (a.kind.equals("treasure")) { treasureInput(room, a, actor, action.selection); return; }
        if (a.kind.equals("bridge") && a.stage.equals("bridge_select")) { bridgeInput(room, a, actor, action.selection); return; }
        require(a.stage.equals("select") && !a.choices.containsKey(actor.id), "本轮选择已提交");
        int selection = action.selection == null ? -1 : action.selection;
        switch (a.kind) {
            case "casino" -> {
                require(actor.id.equals(a.triggerId), "只有落点玩家能参加赌场");
                if (selection == 0) { cancelChoice(room,a,"放弃本次赌场活动"); return; }
                require(selection == 1 || selection == 2, "请选择赌场模式");
                require(action.point != null && (selection == 1 ? action.point >= 1 && action.point <= 2 : action.point >= 1 && action.point <= 6), "请选择有效猜测");
                stake(room,a,actor,selection == 1 ? "casinoSmallStake" : "casinoPointStake");
                a.choices.put(actor.id,selection * 10 + action.point); a.sample = random.nextInt(6) + 1; timed(a,"rolling",1200);
            }
            case "horse" -> {
                require(selection >= 0 && selection <= 8, "请选择一至八号马或放弃");
                if (selection > 0) stake(room,a,actor,"horseStake"); a.choices.put(actor.id,selection);
                log(room,actor.name + (selection == 0 ? " 放弃赛马下注" : " 下注 ¥" + money(room,"horseStake") + "，选择 " + selection + " 号马"));
                if (a.choices.size() == a.participants.size()) { if (a.wagers.isEmpty()) cancelChoice(room,a,"全员放弃，本次赛马结束"); else { a.sample = random.nextInt(8) + 1; timed(a,"racing",8000); } }
            }
            case "packet" -> claimPacket(room, a, actor, selection);
            case "blindbox" -> claimBlindBox(room, a, actor, selection);
            case "minority" -> {
                require(selection == 0 || selection == 1, "请选择火锅摊或烧烤摊");
                a.choices.put(actor.id, selection);
                if (a.choices.size() == a.participants.size()) timed(a, "minority_reveal", 1200);
            }
            case "bridge" -> {
                require(selection == 0 || selection == 1, "请选择参加或放弃");
                if (selection == 1) stake(room, a, actor, "horseStake");
                a.choices.put(actor.id, selection);
                if (a.choices.size() == a.participants.size()) {
                    a.contenders = new ArrayList<>(a.wagers.keySet());
                    if (a.contenders.isEmpty()) cancelChoice(room, a, "全员放弃，本次玻璃桥结束");
                    else { a.level = 1; nextBridgeRound(a); }
                }
            }
            case "rps" -> { require(a.contenders.contains(actor.id) && selection >= 0 && selection <= 2,"请选择石头、剪刀或布"); a.choices.put(actor.id,selection); if (a.choices.size() == a.contenders.size()) timed(a,"revealing",1200); }
            default -> throw new GameError("未知活动");
        }
    }
    /** 输入宝箱玩家及选择，首次选箱扣本金；零为放弃或领取当前奖励，一至二为下一层选箱，无返回值。 */
    private void treasureInput(Room room, CornerActivity a, Player actor, Integer selection) {
        require(actor.id.equals(a.triggerId) && (a.stage.equals("select") || a.stage.equals("treasure_choice")), "请等待宝箱揭晓");
        require(selection != null && selection >= 0 && selection <= 2, "请选择宝箱或收手");
        if (selection == 0) {
            if (a.level == 0) cancelChoice(room, a, "放弃本次贪心宝箱");
            else { long prize = Math.multiplyExact(money(room, "treasureStake"), 1L << a.level); grant(room, a, actor.id, prize); result(room, a, actor.name + " 见好就收，领取 ¥" + prize); }
            return;
        }
        if (a.wagers.isEmpty()) stake(room, a, actor, "treasureStake");
        a.choices.put(actor.id, selection); a.sample = random.nextInt(2) + 1; timed(a, "treasure_open", 1200);
    }
    /** 输入玻璃桥当前关及玩家方向，全部在场玩家锁定后才生成安全方向，进入揭晓，无返回值。 */
    private void bridgeInput(Room room, CornerActivity a, Player actor, Integer selection) {
        require(a.contenders.contains(actor.id) && !a.choices.containsKey(actor.id), "你已提交或已离开玻璃桥");
        require(selection != null && (selection == 1 || selection == 2), "请选择左桥或右桥");
        a.choices.put(actor.id, selection);
        if (a.choices.size() == a.contenders.size()) { a.sample = random.nextInt(2) + 1; timed(a, "bridge_reveal", 1200); }
    }
    /** 输入已准备好关数的玻璃桥，清空本关选择并开启下一次秘密提交，无返回值。 */
    private void nextBridgeRound(CornerActivity a) { a.choices.clear(); a.stage = "bridge_select"; a.round++; a.endsAt = null; }
    /** 输入盲盒序号及领取人，独占领取一个盒子并立即到账；只有唯一隐藏款可能翻倍，无返回值。 */
    private void claimBlindBox(Room room, CornerActivity a, Player actor, int selection) {
        require(selection >= 1 && selection <= 20, "请选择一至二十号盲盒");
        require(!a.packetSelections.containsValue(selection), "这个盲盒已被领取，请选择其他盲盒");
        boolean hidden = selection == a.hiddenBox;
        long base = hidden ? Math.addExact(money(room, "blindboxHiddenBase"), Math.multiplyExact(money(room, "blindboxHiddenPerPlayer"), a.participants.size())) : money(room, "blindboxRegular");
        long bonus = hidden && a.limitedBox ? base : 0L;
        a.packetSelections.put(actor.id, selection); a.choices.put(actor.id, selection); a.packets.put(actor.id, base); a.bonuses.put(actor.id, bonus);
        a.revealed.put(actor.id, hidden ? bonus > 0 ? 2 : 1 : 0);
        grant(room, a, actor.id, Math.addExact(base, bonus));
        log(room, actor.name + " 打开 " + selection + " 号盲盒：" + (hidden ? bonus > 0 ? "限定款，隐藏奖励翻倍" : "隐藏款" : "普通款") + "，到账 ¥" + (base + bonus));
        if (a.choices.size() == a.participants.size()) result(room, a, "丰收盲盒已全部领取，未选中的盲盒不发放");
    }
    /** 输入房间、活动、领取人和红包序号，独占领取后立即结算；全部领取后播放结果三秒，再保留五秒。 */
    private void claimPacket(Room room, CornerActivity a, Player actor, int selection) {
        require(selection >= 1 && selection <= a.participants.size(), "请选择有效红包");
        // 旧活动已提交的领取保留原分配，待下次有效领取时一并结算，避免重复发奖。
        if (a.packetSelections.isEmpty()) for (String id : a.choices.keySet()) a.packetSelections.put(id, a.participants.indexOf(id) + 1);
        require(!a.packetSelections.containsValue(selection), "这个红包已被领取，请选择其他红包");
        a.packetSelections.put(actor.id, selection); a.choices.put(actor.id, selection);
        for (String id : a.choices.keySet()) if (!a.payouts.containsKey(id)) settlePacket(room, a, id);
        if (a.choices.size() == a.participants.size()) finishPackets(room, a);
    }
    /** 输入房间、活动与领取人，按其选中红包发放基础金额和幸运加奖，无返回值。 */
    private void settlePacket(Room room, CornerActivity a, String id) {
        int selection = a.packetSelections.getOrDefault(id, a.participants.indexOf(id) + 1);
        String slot = a.participants.get(selection - 1);
        long base = a.packets.get(slot), bonus = a.bonuses.get(slot);
        grant(room, a, id, Math.addExact(base, bonus));
        log(room, player(room, id).name + " 拆开 " + selection + " 号红包，基础 ¥" + base + (bonus > 0 ? "，幸运加奖 ¥" + bonus : ""));
    }
    /** 输入房间及已全部领取的红包活动，公布手气最佳并设置含播放时间的自动关闭截止时间。 */
    private void finishPackets(Room room, CornerActivity a) {
        String best = a.participants.getFirst(); long largest = -1;
        for (String id : a.participants) {
            int selection = a.packetSelections.getOrDefault(id, a.participants.indexOf(id) + 1);
            long amount = a.packets.get(a.participants.get(selection - 1));
            if (amount > largest) { largest = amount; best = id; }
        }
        result(room, a, "红包已全部拆开，手气最佳：" + player(room, best).name);
        a.endsAt = a.startedAt + 8000;
    }
    /** 输入轮盘活动及操作者，处理发起、放弃与当前玩家的一次开枪。 */
    private void rouletteInput(Room room, CornerActivity a, Player actor, Action action) {
        if (a.stage.equals("select")) {
            require(actor.id.equals(a.triggerId),"请等待落点玩家选择对手");
            if (Objects.equals(action.selection,0)) { cancelChoice(room,a,"放弃发起轮盘对决"); return; }
            Player opponent = player(room,action.playerId); require(opponent != null && !opponent.bankrupt && !opponent.id.equals(actor.id),"请选择另一名未破产玩家");
            a.opponentId = opponent.id; a.participants = new ArrayList<>(List.of(actor.id,opponent.id)); a.bullet = random.nextInt(6); a.currentPlayerId = a.participants.get(random.nextInt(2)); timed(a,"loading",1000);
            log(room,actor.name + " 指定 " + opponent.name + " 进行轮盘对决，" + player(room,a.currentPlayerId).name + " 先手");
        } else {
            require(a.stage.equals("duel") && actor.id.equals(a.currentPlayerId) && Objects.equals(action.selection,1),"请等待当前玩家开枪");
            a.lastShooterId = actor.id; a.hit = a.shots == a.bullet; a.shots++; timed(a,"shot",800);
        }
    }
    /** 输入下注人及金额键，使用当前现金一次扣款并记录，无返回值。 */
    private void stake(Room room, CornerActivity a, Player actor, String key) { long amount = money(room,key); require(actor.cash >= amount,"现金不足，仅可用已有现金下注"); actor.cash -= amount; a.wagers.put(actor.id,amount); log(room,actor.name + " 支付活动本金 ¥" + amount); }
    /** 输入活动与展示阶段时长，保存服务端截止时间并使旧输入失效。 */
    private void timed(CornerActivity a, String stage, long duration) { a.stage = stage; a.round++; a.startedAt = System.currentTimeMillis(); a.endsAt = a.startedAt + duration; }
    /** 输入活动取消说明，生成无需经济结算的结束展示。 */
    private void cancelChoice(Room room, CornerActivity a, String summary) { a.settled = true; a.summary = summary; log(room,summary); timed(a,"result",VARIANTS.containsValue(a.kind) ? 8000 : 1500); }
    /** 输入房间，返回当前活动是否到达自动阶段推进点。 */
    public boolean due(Room room) { CornerActivity a=room.activity; return a != null && (a.stage.equals("payment") && room.pending == null && room.trade == null || a.endsAt != null && a.endsAt <= System.currentTimeMillis()); }
    /** 输入房间，完成到期揭晓或父费用续接；由现有事务内调用。 */
    public void refresh(Room room) {
        CornerActivity a=room.activity; if (a == null || !due(room)) return;
        switch (a.stage) {
            case "treasure_open" -> revealTreasure(room, a);
            case "bridge_reveal" -> revealBridge(room, a);
            case "bridge_round" -> { a.level++; nextBridgeRound(a); }
            case "minority_reveal" -> revealMinority(room, a);
            case "rolling" -> { int choice=a.choices.get(a.triggerId), mode=choice / 10, guess=choice % 10; boolean won=mode == 1 ? guess == (a.sample <= 3 ? 1 : 2) : guess == a.sample; if(won) grant(room,a,a.triggerId,money(room,mode == 1 ? "casinoSmallPrize" : "casinoPointPrize")); result(room,a,"骰子为 " + a.sample + " 点，" + (won ? "猜中获奖" : "本次未猜中")); }
            case "racing" -> { for(String id:a.participants) if(a.choices.get(id)==a.sample) grant(room,a,id,money(room,"horsePrize")); result(room,a,a.sample + " 号马夺冠"); }
            case "opening" -> { for (String id : a.participants) if (!a.payouts.containsKey(id)) settlePacket(room, a, id); finishPackets(room, a); }
            case "revealing" -> revealRps(room,a);
            case "round_result" -> { a.choices.clear(); a.stage="select"; a.round++; a.endsAt=null; }
            case "loading" -> { a.stage="duel"; a.round++; a.endsAt=null; }
            case "shot" -> {
                if(a.hit) { String winner=a.participants.stream().filter(id -> !id.equals(a.lastShooterId)).findFirst().orElseThrow(); a.settled=true; a.stage="payment"; a.round++; a.endsAt=null; a.summary=player(room,a.lastShooterId).name + " 轮盘判负，对 " + player(room,winner).name + " 产生待处理费用 ¥" + money(room,"rouletteDebt"); log(room,a.summary); economy.charge(room,player(room,a.lastShooterId),money(room,"rouletteDebt"),winner,null); }
                else { log(room,player(room,a.lastShooterId).name + " 第 " + a.shots + " 次开枪为空响"); a.currentPlayerId=a.participants.stream().filter(id -> !id.equals(a.lastShooterId)).findFirst().orElseThrow(); a.stage="duel"; a.round++; a.endsAt=null; }
            }
            case "payment" -> timed(a,"result",1500);
            case "result" -> { room.activity=null; room.phase=a.returnPhase; }
            default -> throw new GameError("活动展示阶段不可用");
        }
    }
    /** 输入宝箱已冻结的安全方向，揭晓本层并发放最终奖励或保留继续选择，无返回值。 */
    private void revealTreasure(Room room, CornerActivity a) {
        a.safePaths.add(a.sample); a.revealed = new LinkedHashMap<>(a.choices);
        if (a.choices.get(a.triggerId) != a.sample) { result(room, a, "第 " + (a.level + 1) + " 层触发机关，本次未获奖励"); return; }
        a.level++; a.choices.clear();
        if (a.level == 3) { long prize = Math.multiplyExact(money(room, "treasureStake"), 8L); grant(room, a, a.triggerId, prize); result(room, a, "三层全部通关，领取 ¥" + prize); }
        else { a.stage = "treasure_choice"; a.round++; a.endsAt = null; a.summary = "第 " + a.level + " 层安全！可以收手或继续挑战"; log(room, a.summary); }
    }
    /** 输入玻璃桥本关选择及安全方向，淘汰选错者，第三关统一派奖，无返回值。 */
    private void revealBridge(Room room, CornerActivity a) {
        a.revealed = new LinkedHashMap<>(a.choices); a.safePaths.add(a.sample);
        a.contenders.removeIf(id -> a.choices.get(id) != a.sample);
        log(room, "玻璃桥第 " + a.level + " 关：" + (a.sample == 1 ? "左桥" : "右桥") + " 安全，剩余 " + a.contenders.size() + " 人");
        if (a.contenders.isEmpty()) result(room, a, "第 " + a.level + " 关全员掉落，本次无人通关");
        else if (a.level == 3) {
            for (String id : a.contenders) grant(room, a, id, money(room, "horsePrize"));
            result(room, a, a.contenders.size() + " 人通过三关玻璃桥，每人领取 ¥" + money(room, "horsePrize"));
        } else { a.summary = "第 " + a.level + " 关结束，" + a.contenders.size() + " 人继续挑战"; timed(a, "bridge_round", 1600); }
    }
    /** 输入夜市全员秘密选择，少数派抽一人；同人数或同摊时全员抽一人，固定发一次冠军奖，无返回值。 */
    private void revealMinority(Room room, CornerActivity a) {
        a.revealed = new LinkedHashMap<>(a.choices);
        List<String> hotpot = a.participants.stream().filter(id -> a.choices.get(id) == 0).toList();
        List<String> barbecue = a.participants.stream().filter(id -> a.choices.get(id) == 1).toList();
        boolean minority = !hotpot.isEmpty() && !barbecue.isEmpty() && hotpot.size() != barbecue.size();
        List<String> eligible = minority ? hotpot.size() < barbecue.size() ? hotpot : barbecue : a.participants;
        a.contenders = new ArrayList<>(eligible); String winner = eligible.get(random.nextInt(eligible.size()));
        for (String id : a.participants) log(room, player(room, id).name + " 去了" + (a.choices.get(id) == 0 ? "火锅摊" : "烧烤摊"));
        grant(room, a, winner, money(room, "rpsPrize"));
        result(room, a, "火锅 " + hotpot.size() + " 人 / 烧烤 " + barbecue.size() + " 人，" + (minority ? "少数派" : "全员") + "抽奖：" + player(room, winner).name + " 获得 ¥" + money(room, "rpsPrize"));
    }
    /** 输入猜拳活动，统一揭晓并按两种手势淘汰，单人胜出才派奖。 */
    private void revealRps(Room room, CornerActivity a) {
        a.revealed = new LinkedHashMap<>(a.choices); Set<Integer> moves=new HashSet<>(a.choices.values()); String[] names={"石头","剪刀","布"};
        for(String id:a.contenders) log(room,player(room,id).name + " 出了" + names[a.choices.get(id)]);
        if(moves.size()==2) { int winner=moves.stream().filter(move -> moves.contains((move+1)%3)).findFirst().orElseThrow(); a.contenders.removeIf(id -> a.choices.get(id)!=winner); }
        if(a.contenders.size()==1) { String id=a.contenders.getFirst(); grant(room,a,id,money(room,"rpsPrize")); result(room,a,player(room,id).name + " 赢得猜拳擂台"); }
        else { a.summary=moves.size()==2 ? "本轮胜出 " + a.contenders.size() + " 人，继续争夺冠军" : "本轮平局，重新出拳"; log(room,a.summary); timed(a,"round_result",1800); }
    }
    /** 输入收款人及总到账金额，增加现金并记录活动奖励。 */
    private void grant(Room room, CornerActivity a, String id, long amount) { Player p=player(room,id); require(p!=null && !p.bankrupt,"活动收款人已退出"); p.cash=Math.addExact(p.cash,amount); a.payouts.put(id,amount); log(room,p.name + " 获得「" + NAMES.get(a.kind) + "」奖励 ¥" + amount); }
    /** 输入活动结果，标记经济结算完成并开启结果展示。 */
    private void result(Room room, CornerActivity a, String summary) { a.settled=true; a.summary=summary; log(room,summary); timed(a,"result",VARIANTS.containsValue(a.kind) ? 8000 : 1800); }
    /** 输入将被房主移出的玩家，未结算活动退款中止；既有费用继续保留。 */
    public void removing(Room room, String playerId) {
        CornerActivity a=room.activity; if(a==null || !a.participants.contains(playerId)) return;
        if(!a.settled) { a.wagers.forEach((id,amount) -> { Player p=player(room,id); if(p!=null && !p.bankrupt) { p.cash=Math.addExact(p.cash,amount); log(room,p.name + " 收回中止活动本金 ¥" + amount); } }); room.activity=null; room.phase=a.returnPhase; log(room,"参与者被移出，未结算活动中止"); }
    }
    /** 输入房间，返回尚未回应且离线的必需参与者；观战者不影响活动。 */
    public Player offlineRequired(Room room) {
        CornerActivity a=room.activity; if(a==null || a.endsAt!=null || a.stage.equals("payment")) return null;
        List<String> required=a.kind.equals("roulette") ? List.of(a.stage.equals("select") ? a.triggerId : a.currentPlayerId) : a.kind.equals("rps") || a.kind.equals("bridge") && a.stage.equals("bridge_select") ? a.contenders : a.participants;
        return required.stream().filter(id -> !a.choices.containsKey(id)).map(id -> player(room,id)).filter(p -> p!=null && !p.bankrupt && !p.online).findFirst().orElse(null);
    }
}
