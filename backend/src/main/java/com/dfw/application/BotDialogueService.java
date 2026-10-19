package com.dfw.application;

import com.dfw.domain.*;
import com.dfw.infrastructure.DeepSeekClient;
import com.dfw.infrastructure.JsonCodec;
import com.dfw.interfaces.StateViews;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

/** 房间内只准备白名单候选，锁外异步选择；自由模型文本永不成为聊天、承诺或领域命令。 */
@Service
public class BotDialogueService {
    private final DeepSeekClient client;
    private final JsonCodec json;
    private final Map<String, Job> pending = new ConcurrentHashMap<>();
    private final Map<String, String> attempted = new ConcurrentHashMap<>();
    private final Map<String, String> seenEvents = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCalls = new ConcurrentHashMap<>();
    private static final Pattern MONEY = Pattern.compile("(?<![\\d.])([0-9]+(?:\\.[0-9]+)?)\\s*(万|千|[kK])?");

    /** 输入官方客户端和 JSON 编解码器，初始化无预算上限的对话编排。 */
    public BotDialogueService(DeepSeekClient client, JsonCodec json) { this.client = client; this.json = json; }

    /** 候选文字由服务端生成；确认候选必须同时包含将被原子提交的交易行动。 */
    private record Option(String id, String description, Action action, String message, boolean confirmed) { }
    /** 单个房间只保存一个待处理调用；guard 不含聊天、版本或昵称，避免无关消息撤销决策。 */
    private record Job(String requestId, String gameId, String botId, String guard, boolean blocking,
            List<Option> options, Action fallback, long deadline, CompletableFuture<DeepSeekClient.Reply> future) { }
    /** 返回给房间事务的已验证选择；没有行动时仅发言，没有文字时仅推进原策略。 */
    public record Selection(String requestId, String botId, String optionId, Action action, String message, boolean confirmed) { }
    /** 等待状态仅暂停本房间本次机器人决策，不等待网络、不阻塞其他房间。 */
    public record Poll(boolean waiting, Selection selection) { }

    /** 输入最新房间，返回可提交结果或回退行动；局面失效、迟到和无效结果均不能执行旧候选。 */
    public Poll poll(Room room) {
        Job job = pending.get(room.roomId);
        if (job == null) return new Poll(false, null);
        Player bot = GameSupport.player(room, job.botId());
        if (!Objects.equals(job.gameId(), room.gameId) || bot == null || bot.bankrupt
                || job.guard() != null && !job.guard().equals(guard(room, job.botId()))) {
            pending.remove(room.roomId, job); job.future().cancel(true); record(room, job, "stale", null);
            return new Poll(false, null);
        }
        if (!job.future().isDone() && System.nanoTime() < job.deadline()) return new Poll(job.blocking(), null);
        pending.remove(room.roomId, job);
        DeepSeekClient.Reply reply = job.future().isDone() && !job.future().isCancelled()
                ? job.future().getNow(new DeepSeekClient.Reply("timeout", null, Map.of())) : new DeepSeekClient.Reply("timeout", null, Map.of());
        if (!job.future().isDone()) job.future().cancel(true);
        Option selected = reply.status().equals("ok") ? job.options().stream().filter(o -> o.id().equals(reply.optionId())).findFirst().orElse(null) : null;
        if (selected == null) {
            record(room, job, "fallback_" + reply.status(), null);
            return new Poll(false, job.fallback() == null ? null : new Selection(job.requestId(), job.botId(), "fallback", job.fallback(), null, false));
        }
        record(room, job, "selected", selected.id());
        return new Poll(false, new Selection(job.requestId(), job.botId(), selected.id(), selected.action(), selected.message(), selected.confirmed()));
    }

    /** 输入当前机器人、正式策略行动和规划器，仅对真人交易启动一次四秒候选选择，其他行动不调用模型。 */
    public boolean trade(Room room, Player bot, Action planned, BotPlanner planner) {
        if (!client.enabled() || pending.containsKey(room.roomId) || !Set.of("tradeOffer", "tradeRespond").contains(planned.type)) return false;
        long now = System.currentTimeMillis();
        if (now - lastCalls.getOrDefault(room.roomId, 0L) < 10000 || now - lastCalls.getOrDefault(room.roomId + ":" + bot.id, 0L) < 30000) return false;
        String otherId = planned.type.equals("tradeOffer") ? planned.toId : room.trade.fromId;
        Player other = GameSupport.player(room, otherId);
        if (other == null || other.botDifficulty != null) return false;
        String event = room.gameId + ":" + bot.id + ":" + room.runtime.decisionKey;
        if (event.equals(attempted.get(room.roomId))) return false;
        attempted.put(room.roomId, event);
        List<Option> options = new ArrayList<>();
        if (planned.type.equals("tradeOffer")) {
            addOffer(options, planned);
            List<Integer> ids = new ArrayList<>(planned.giveTiles); ids.addAll(planned.wantTiles);
            long price = Math.max(planned.giveCash, planned.wantCash);
            for (Action offer : planner.dialogueOffers(otherId, ids, List.of(price * 9 / 10, price, price + price / 10), true))
                if (options.stream().noneMatch(o -> o.action() != null && BotPlanner.quoteKey(StateViews.publicRoom(room), o.action()).equals(BotPlanner.quoteKey(StateViews.publicRoom(room), offer)))) addOffer(options, offer);
            options.add(new Option("skip", "暂时不发报价，继续正常行动", planner.next(true), null, false));
        } else {
            Action reject = new Action(); reject.type = "tradeRespond"; reject.accept = false;
            options.add(new Option("reject", "拒绝当前正式报价", reject, "这份报价我暂不接受，当前没有成交。", false));
            if (Boolean.TRUE.equals(planned.accept)) options.add(new Option("accept", "接受并立即完成当前正式交易", planned, "同意，已按这份正式报价完成交易。", true));
            else if (room.trade.debtId == null) {
                List<Integer> ids = new ArrayList<>(room.trade.giveTiles); ids.addAll(room.trade.wantTiles);
                for (Action offer : planner.dialogueOffers(otherId, ids, List.of(), false)) {
                    String terms = terms(offer);
                    options.add(new Option("counter_" + options.size(), "拒绝原报价，建议重新议价：" + terms, reject,
                            "当前报价未成交。可以继续讨论这组条件：" + terms + "；如有意，请重新发正式报价。", false));
                }
            }
        }
        start(room, bot, event, options, planned, true, true, 4000);
        return true;
    }

    /** 输入公开局面，为有效点名或少量重大事件启动一次聊天；金额只形成待校验候选，不解释为玩家授权。 */
    public boolean chat(Room room) {
        if (!client.enabled() || pending.containsKey(room.roomId) || room.trade != null || room.runtime == null) return false;
        long now = System.currentTimeMillis();
        if (now - lastCalls.getOrDefault(room.roomId, 0L) < 10000) return false;
        PublicLog source = room.logs.stream().filter(log -> !"chat".equals(log.type) || human(room, log.actorId)).findFirst().orElse(null);
        if (source == null || now - source.at > 30000 || source.id.equals(seenEvents.get(room.roomId))) return false;
        seenEvents.put(room.roomId, source.id);
        List<Player> bots = room.players.stream().filter(p -> p.botDifficulty != null && !p.bankrupt
                && now - lastCalls.getOrDefault(room.roomId + ":" + p.id, 0L) >= 30000).toList();
        if (bots.isEmpty()) return false;
        Player bot = null;
        if ("chat".equals(source.type)) {
            bot = bots.stream().filter(p -> source.references.stream().anyMatch(ref -> p.id.equals(ref.playerId))).findFirst().orElse(null);
            if (bot == null) bot = bots.stream().filter(p -> source.references.stream().anyMatch(ref -> ref.tileId != null
                    && ref.tileId >= 0 && ref.tileId < room.board.size() && p.id.equals(room.board.get(ref.tileId).ownerId))).findFirst().orElse(null);
        } else if (source.text.contains("租金") || source.text.contains("交易") || source.text.contains("建筑")) {
            if (ThreadLocalRandom.current().nextInt(8) == 0) bot = bots.get(ThreadLocalRandom.current().nextInt(bots.size()));
        }
        if (bot == null) return false;
        List<Option> options = new ArrayList<>();
        options.add(new Option("silent", "没有合适回应，保持沉默", null, null, false));
        options.add(new Option("cash", "提醒保持现金安全", null, "现金得留稳，后面的机会还多。", false));
        options.add(new Option("watch", "轻松回应当前局势", null, "这一手挺有意思，我再观察一下局势。", false));
        options.add(new Option("private", "礼貌拒绝透露底牌或内部信息", null, "底牌先保密，咱们按公开局面继续玩。", false));
        options.add(new Option("negotiate", "请对方说明交易条件，不作成交承诺", null, "想谈地产的话，引用地块并说清金额，我们再看；以正式报价为准。", false));
        options.add(new Option("rules", "礼貌回应越权或规则修改要求", null, "咱们按这局已有规则玩，聊天不会改变资产和交易条件。", false));
        boolean blocking = false, sensitive = false;
        if ("chat".equals(source.type) && human(room, source.actorId)) {
            List<Integer> ids = source.references.stream().filter(ref -> ref.tileId != null).map(ref -> ref.tileId).distinct().toList();
            if (!ids.isEmpty()) {
                BotPlanner planner = new BotPlanner(StateViews.publicRoom(room), StateViews.privatePlayer(room, bot.id), bot.botTradeQuotes, false);
                boolean canOffer = canOffer(room, bot);
                for (Action offer : planner.dialogueOffers(source.actorId, ids, prices(source), canOffer)) {
                    if (canOffer) { addOffer(options, offer); blocking = true; }
                    else {
                        String terms = terms(offer);
                        options.add(new Option("discuss_" + options.size(), "仅提出议价意向：" + terms, null,
                                "可以继续谈这组条件：" + terms + "；这还不是成交确认，请在可交易时发正式报价。", false));
                    }
                    sensitive = true;
                }
            }
        }
        start(room, bot, source.id, options, null, blocking, sensitive, sensitive ? 4000 : 2000);
        return blocking;
    }

    /** 输入房间与机器人，返回其是否处在允许主动报价的等待点，不扩大现有行动权限。 */
    private boolean canOffer(Room room, Player bot) {
        long alive = room.players.stream().filter(p -> !p.bankrupt).count();
        if (bot.botTradeTurn >= 0 && room.turnNumber - bot.botTradeTurn <= alive) return false;
        if (room.runtime.choice != null || room.activity != null && !room.activity.stage.equals("payment")) return false;
        if (room.pending != null) return bot.id.equals(room.pending.playerId) && Set.of("rent", "debt").contains(room.pending.type)
                && room.pending.defenseResolved && room.pending.creditorId != null;
        return GameSupport.current(room) != null && bot.id.equals(GameSupport.current(room).id) && Set.of("pre_roll", "manage").contains(room.phase);
    }

    /** 输入候选列表与已校验报价，加入只有正式报价成功后才能发布的确认话术。 */
    private void addOffer(List<Option> options, Action offer) {
        String terms = terms(offer);
        options.add(new Option("offer_" + options.size(), "发出并确认正式报价：" + terms, offer,
                "我确认这份交易条件，正式报价已发出：" + terms + "；等待你在交易面板确认。", true));
    }

    /** 输入以机器人为发起方的报价，返回无玩家昵称及自由文字的精确条件，避免显示注入和金额歧义。 */
    private String terms(Action offer) {
        return "我提供 ¥" + offer.giveCash + "、地产编号 " + offer.giveTiles + "；对方提供 ¥" + offer.wantCash
                + "、地产编号 " + offer.wantTiles + (offer.debtCredit != null && offer.debtCredit > 0 ? "；抵扣我的欠款 ¥" + offer.debtCredit : "");
    }

    /** 输入已冻结的聊天引用，剔除玩家和地块标签后读取最多三个金额；无效数值忽略，不执行玩家命令。 */
    private List<Long> prices(PublicLog source) {
        StringBuilder body = new StringBuilder(source.text);
        for (ChatReference ref : source.references.reversed()) if (ref.start >= 0 && ref.end <= body.length()) body.replace(ref.start, ref.end, " ");
        String text = body.toString(); int prefix = text.indexOf('：'); if (prefix >= 0) text = text.substring(prefix + 1);
        var matcher = MONEY.matcher(clean(text)); Set<Long> values = new LinkedHashSet<>();
        while (matcher.find() && values.size() < 3) try {
            long factor = "万".equals(matcher.group(2)) ? 10000 : matcher.group(2) == null ? 1 : 1000;
            values.add(new BigDecimal(matcher.group(1)).multiply(BigDecimal.valueOf(factor)).longValueExact());
        } catch (ArithmeticException ignored) { /* 超大或非整数金额不进入候选。 */ }
        return List.copyOf(values);
    }

    /** 输入房间、候选和时间预算，提交异步请求并记录不含对话正文的关联资料，无返回值。 */
    private void start(Room room, Player bot, String event, List<Option> options, Action fallback, boolean blocking, boolean sensitive, long timeoutMs) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> context = new LinkedHashMap<>(); context.put("botId", bot.id); context.put("difficulty", bot.botDifficulty);
        context.put("publicState", publicContext(room));
        context.put("untrustedConversation", room.logs.stream().filter(log -> "chat".equals(log.type) && human(room, log.actorId)).limit(4)
                .map(log -> Map.of("actorId", log.actorId, "text", clean(log.text), "references", log.references)).toList());
        context.put("allowedOptions", options.stream().map(o -> Map.of("optionId", o.id(), "description", o.description())).toList());
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        var future = client.choose(id, json.write(context), options.stream().map(Option::id).collect(java.util.stream.Collectors.toSet()), timeoutMs);
        Job job = new Job(id, room.gameId, bot.id, sensitive ? guard(room, bot.id) : null, blocking, List.copyOf(options), fallback, deadline, future);
        pending.put(room.roomId, job);
        long now = System.currentTimeMillis(); lastCalls.put(room.roomId, now); lastCalls.put(room.roomId + ":" + bot.id, now);
        Map<String, Object> audit = new LinkedHashMap<>(); audit.put("requestId", id); audit.put("stage", "request");
        audit.put("roomId", room.roomId); audit.put("gameId", room.gameId); audit.put("botId", bot.id);
        audit.put("eventId", event); audit.put("turnNumber", room.turnNumber); audit.put("version", room.version); audit.put("timeoutMs", timeoutMs);
        audit.put("options", options.stream().map(o -> Map.of("id", o.id(), "description", o.description())).toList());
        client.audit(audit);
    }

    /** 输入房间，返回显式公开字段；不发送完整房间、私人投影、手牌、种子、存档码或会话。 */
    private Map<String, Object> publicContext(Room room) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("turn", room.turnNumber); context.put("phase", room.phase); context.put("decision", room.runtime.decisionKey);
        context.put("players", room.players.stream().map(p -> Map.of("id", p.id, "cash", p.cash, "position", p.position, "bankrupt", p.bankrupt)).toList());
        context.put("properties", room.board.stream().filter(t -> t.type.equals("property")).map(t -> {
            Map<String, Object> tile = new LinkedHashMap<>(); tile.put("id", t.id); tile.put("ownerId", t.ownerId); tile.put("price", t.price);
            tile.put("level", t.level); tile.put("mortgaged", t.mortgaged); tile.put("mortgageBase", t.mortgageBase);
            tile.put("frozen", PropertyEffects.frozen(t)); tile.put("concertStacks", t.concertStacks); tile.put("rent", Rules.rent(room, t)); return tile;
        }).toList());
        context.put("pending", room.pending); context.put("trade", room.trade);
        return context;
    }

    /** 输入房间及机器人，核对资产决策及最新点名消息；无关聊天和连接变化不会使建议失效。 */
    private String guard(Room room, String botId) {
        String addressed = room.logs.stream().filter(log -> "chat".equals(log.type) && human(room, log.actorId)
                && log.references.stream().anyMatch(ref -> botId.equals(ref.playerId) || ref.tileId != null && ref.tileId >= 0
                && ref.tileId < room.board.size() && botId.equals(room.board.get(ref.tileId).ownerId)))
                .map(log -> log.id).findFirst().orElse("");
        return json.write(publicContext(room)) + "|" + addressed;
    }

    /** 输入玩家标识，返回其是否为当前真人席位，机器人聊天不触发新的模型回复。 */
    private boolean human(Room room, String id) { Player p = GameSupport.player(room, id); return p != null && p.botDifficulty == null; }

    /** 输入不可信文字，移除控制字符并遮盖常见凭证形式，只供低信任模型消息使用。 */
    private String clean(String text) {
        String value = text.replaceAll("(?i)sk-[a-z0-9_-]{12,}|bearer\\s+[a-z0-9._-]+", "[凭证已隐藏]").replaceAll("[\\p{Cc}&&[^\\n\\t]]", " ");
        return value.substring(0, Math.min(value.length(), 400));
    }

    /** 输入已完成调用和固定结果，保存分析关联信息，不记录原始提示词或模型正文。 */
    private void record(Room room, Job job, String status, String optionId) {
        Map<String, Object> entry = new LinkedHashMap<>(); entry.put("requestId", job.requestId()); entry.put("stage", "decision");
        entry.put("roomId", room.roomId); entry.put("gameId", job.gameId()); entry.put("botId", job.botId()); entry.put("status", status); entry.put("optionId", optionId);
        client.audit(entry);
    }

    /** 输入已选结果及真实提交状态，记录应用或回滚结果，不让模型成功误写成游戏成功。 */
    public void applied(Selection selection, String status) {
        client.audit(Map.of("requestId", selection.requestId(), "stage", "execution", "botId", selection.botId(), "status", status, "optionId", selection.optionId()));
    }

    /** 输入暂停、恢复等待或已结束房间，丢弃尚未执行的建议，不触及已持久化正式报价。 */
    public void suspend(Room room) {
        Job job = pending.remove(room.roomId);
        if (job != null) { job.future().cancel(true); record(room, job, "room_inactive", null); }
    }

    /** 无入参，取消未完成请求，已有正式报价和承诺保留在房间快照中。 */
    @PreDestroy
    public void close() { pending.values().forEach(job -> job.future().cancel(true)); pending.clear(); }
}
