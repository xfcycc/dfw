package com.dfw.interfaces;

import com.dfw.domain.GameError;
import com.dfw.domain.Rules;
import com.dfw.interfaces.dto.*;
import com.dfw.infrastructure.JsonCodec;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** 保留原接口的输入边界，拒绝类型强制转换。 */
public final class InputValidation {
    private static final String UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";
    /** 输入校验条件，不满足时返回统一参数错误。 */
    private static void check(boolean condition) { if (!condition) throw new GameError("请求参数不正确", "INVALID_INPUT"); }
    /** 输入 JSON 节点，确认对象类型。 */
    private static void object(JsonNode value) { check(value != null && value.isObject()); }
    /** 输入对象和字段，确认布尔类型。 */
    private static void bool(JsonNode value, String field) { check(value.has(field) && value.get(field).isBoolean()); }
    /** 输入对象和字段，确认有效 UUID。 */
    private static void uuid(JsonNode value, String field) { check(value.has(field) && value.get(field).isString() && (value.get(field).asString().matches(UUID_PATTERN) || value.get(field).asString().equals("00000000-0000-0000-0000-000000000000") || value.get(field).asString().equalsIgnoreCase("ffffffff-ffff-ffff-ffff-ffffffffffff"))); }
    /** 输入数值节点与上下界，确认 JSON 整数范围。 */
    private static void number(JsonNode value, long min, long max) { check(value != null && value.isIntegralNumber() && value.canConvertToLong() && value.asLong() >= min && value.asLong() <= max); }
    /** 输入地产列表，确认最多六十四个索引；实际地图范围仍由领域层校验。 */
    private static void tiles(JsonNode value) { check(value != null && value.isArray() && value.size() <= Rules.MAX_TILES); for (JsonNode id : value) number(id, 0, Rules.MAX_TILES - 1); }
    /** 输入请求 JSON 及是否要求房间号，返回兼容的房间输入。 */
    public static RoomInput room(JsonNode value, boolean codeRequired, JsonCodec json) {
        object(value); if (value.has("name")) check(value.get("name").isString());
        if (codeRequired) check(value.has("code") && value.get("code").isString() && value.get("code").asString().matches("[1-9][0-9]{5}"));
        var clean = json.mapper.createObjectNode();
        if (value.has("name")) clean.set("name", value.get("name"));
        if (codeRequired) clean.set("code", value.get("code"));
        return json.mapper.treeToValue(clean, RoomInput.class);
    }
    /** 输入命令 JSON，校验公共信封和对应操作必需参数。 */
    public static GameCommand command(JsonNode value, JsonCodec json) {
        object(value); uuid(value, "id"); number(value.get("expectedVersion"), 0, 9007199254740991L);
        JsonNode action = value.get("action"); object(action); check(action.has("type") && action.get("type").isString()); String type = action.get("type").asString();
        switch (type) {
            case "addBot", "setBotDifficulty" -> { check(action.has("botDifficulty") && action.get("botDifficulty").isString() && List.of("easy", "normal", "hard").contains(action.get("botDifficulty").asString())); if (type.equals("setBotDifficulty")) uuid(action, "playerId"); }
            case "activityInput" -> { uuid(action,"activityId"); number(action.get("activityRound"),1,Integer.MAX_VALUE); if(action.has("selection")) number(action.get("selection"),0,20); if(action.has("point")) number(action.get("point"),1,6); if(action.has("playerId")) uuid(action,"playerId"); }
            case "chat" -> {
                check(action.has("message") && action.get("message").isString() && !action.get("message").asString().isBlank() && action.get("message").asString().length() <= 300);
                if (action.has("references")) {
                    JsonNode references = action.get("references"); check(references.isArray() && references.size() <= 100);
                    for (JsonNode reference : references) {
                        object(reference); number(reference.get("start"), 0, 299); number(reference.get("end"), 1, 300);
                        check(reference.get("start").asInt() < reference.get("end").asInt() && reference.get("end").asInt() <= action.get("message").asString().length());
                        check(reference.has("playerId") != reference.has("tileId"));
                        if (reference.has("playerId")) uuid(reference, "playerId"); else number(reference.get("tileId"), 0, Rules.MAX_TILES - 1);
                    }
                }
            }
            case "constructionBuild", "start", "roll", "buy", "upgrade", "pass", "endTurn", "tradeCancel", "newGame", "payDebt", "declareBankruptcy", "continueRecovery" -> { }
            case "assignSeat" -> uuid(action, "claimId");
            case "contentDecision" -> { uuid(action,"decisionId");
                if(action.has("tileIds")) { JsonNode ids=action.get("tileIds"); check(ids.isArray() && ids.size()>=1 && ids.size()<=2); for(JsonNode id:ids) number(id,0,Rules.MAX_TILES-1); }
                if(action.has("selection")) number(action.get("selection"),0,Rules.MAX_LEVEL); if(action.has("playerId")) uuid(action,"playerId"); if(action.has("accept")) bool(action,"accept"); if(action.has("tileId")) number(action.get("tileId"),0,Rules.MAX_TILES-1); if(action.has("cardId")) uuid(action,"cardId");
                if(action.has("keepCardIds")) { JsonNode ids=action.get("keepCardIds"); check(ids.isArray() && ids.size()>=1 && ids.size()<=6); for(JsonNode id:ids) check(id.isString() && (id.asString().matches(UUID_PATTERN) || id.asString().equals("00000000-0000-0000-0000-000000000000") || id.asString().equalsIgnoreCase("ffffffff-ffff-ffff-ffff-ffffffffffff"))); }
                if(action.has("discardCardIds")) { JsonNode ids=action.get("discardCardIds"); check(ids.isArray() && ids.size()>=1 && ids.size()<=6); for(JsonNode id:ids) check(id.isString() && id.asString().matches(UUID_PATTERN)); }
            }
            case "paymentDefense" -> { if(action.has("decisionId")) uuid(action,"decisionId"); if(action.has("cardId")) uuid(action,"cardId"); }
            case "ready" -> bool(action, "value");
            case "setName" -> check(action.has("name") && action.get("name").isString());
            case "setAvatar" -> check(action.has("avatarId") && action.get("avatarId").isString() && com.dfw.domain.PlayerAvatars.IDS.contains(action.get("avatarId").asString()));
            case "setMapSize" -> check(action.has("mapSize") && action.get("mapSize").isString() && List.of("small", "medium", "large").contains(action.get("mapSize").asString()));
            case "rentDecision" -> bool(action, "useCard");
            case "tradeRespond" -> bool(action, "accept");
            case "removePlayer" -> uuid(action, "playerId");
            case "sellBuilding", "mortgage", "redeem", "sellLand" -> number(action.get("tileId"), 0, Rules.MAX_TILES - 1);
            case "card" -> { uuid(action, "cardId"); if(action.has("secondTileId")) number(action.get("secondTileId"),0,Rules.MAX_TILES-1); if(action.has("direction")) { number(action.get("direction"),-1,1); if(action.get("direction").asInt()==0) throw new GameError("方向必须为1或-1"); } if(action.has("tileIds")) { if(!action.get("tileIds").isArray() || action.get("tileIds").size()>3) throw new GameError("最多分配三道剑气"); for(var id:action.get("tileIds")) number(id,0,Rules.MAX_TILES-1); } if (action.has("point")) number(action.get("point"), 1, 6); if (action.has("tileId")) number(action.get("tileId"), 0, Rules.MAX_TILES - 1); if (action.has("playerId")) uuid(action, "playerId"); if (action.has("secondPlayerId")) uuid(action, "secondPlayerId"); }
            case "tradeOffer" -> { uuid(action, "toId"); number(action.get("giveCash"), 0, 9007199254740991L); number(action.get("wantCash"), 0, 9007199254740991L); tiles(action.get("giveTiles")); tiles(action.get("wantTiles")); if (action.has("debtCredit")) number(action.get("debtCredit"), 0, 9007199254740991L); }
            default -> check(false);
        }
        List<String> fields = switch (type) {
            case "addBot" -> List.of("botDifficulty");
            case "setBotDifficulty" -> List.of("playerId", "botDifficulty");
            case "activityInput" -> List.of("activityId","activityRound","selection","point","playerId");
            case "chat" -> List.of("message", "references");
            case "assignSeat" -> List.of("claimId");
            case "contentDecision" -> List.of("decisionId","accept","tileId","cardId","keepCardIds","discardCardIds","tileIds","selection","playerId");
            case "paymentDefense" -> List.of("decisionId","cardId");
            case "ready" -> List.of("value");
            case "setName" -> List.of("name");
            case "setAvatar" -> List.of("avatarId");
            case "setMapSize" -> List.of("mapSize");
            case "rentDecision" -> List.of("useCard");
            case "tradeRespond" -> List.of("accept");
            case "removePlayer" -> List.of("playerId");
            case "sellBuilding", "mortgage", "redeem", "sellLand" -> List.of("tileId");
            case "card" -> List.of("cardId", "point", "tileId", "playerId", "secondPlayerId", "secondTileId", "tileIds", "direction");
            case "tradeOffer" -> List.of("toId", "giveCash", "wantCash", "giveTiles", "wantTiles", "debtCredit");
            default -> List.of();
        };
        var cleanAction = json.mapper.createObjectNode(); cleanAction.set("type", action.get("type"));
        for (String field : fields) if (action.has(field)) cleanAction.set(field, action.get(field));
        var clean = json.mapper.createObjectNode(); clean.set("id", value.get("id")); clean.set("expectedVersion", value.get("expectedVersion")); clean.set("action", cleanAction);
        return json.mapper.treeToValue(clean, GameCommand.class);
    }
}
