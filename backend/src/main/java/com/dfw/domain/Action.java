package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 领域操作，不依赖网络协议。 */
@Getter
@Setter
public class Action {
    /** 本人修改后的可选昵称，留空恢复默认名。 */
    public String name;
    /** 玩家发送的公开聊天正文。 */
    public String message;
    /** 聊天正文中选中的玩家和地块引用，旧客户端可省略。 */
    public List<ChatReference> references;
    /** 房主批准的角色申请标识。 */
    public String claimId;
    /** 待选身份，防止恢复后提交旧选择。 */
    public String decisionId;
    /** 手牌超限时按本人当前上限恰好保留的牌。 */
    public List<String> keepCardIds;
    /** 制衡确认弃掉的本人旧牌，不含发动卡。 */
    public List<String> discardCardIds;
    /** 重塑材料身份到所选固定候选身份。 */
    public Map<String,String> cardSelections;
    /** 本人选择的内置角色形象。 */
    public String avatarId;
    /** 本次活动身份。 */
    public String activityId;
    /** 本次活动输入轮次。 */
    public Integer activityRound;
    /** 玩法选择或手势编码。 */
    public Integer selection;
    /** 房主选择的机器人难度，easy、normal、hard。 */
    public String botDifficulty;
    /** 操作类型。 */
    public String type;
    /** 大厅地图设置，small、medium 或 large。 */
    public String mapSize;
    /** 准备状态。 */
    public Boolean value;
    /** 免租选择。 */
    public Boolean useCard;
    /** 本笔正租金是否消耗一次十万年魂环，与付款防御卡互斥。 */
    public Boolean useSoulRing;
    /** 道具标识。 */
    public String cardId;
    /** 点数。 */
    public Integer point;
    /** 地块索引。 */
    public Integer tileId;
    /** 目标玩家。 */
    public String playerId;
    /** 斗转星移的第二名玩家，其他操作留空。 */
    public String secondPlayerId;
    /** 七剑最多三道剑气可重复集火；神威为同边1～3处不同地产，可混选归属且无需连续。 */
    public List<Integer> tileIds;
    /** 虚空之泪第二个地产端点。 */
    public Integer secondTileId;
    /** 贯穿雷击方向，1顺时针，-1逆时针。 */
    public Integer direction;
    /** 交易接收人。 */
    public String toId;
    /** 提供现金。 */
    public Long giveCash;
    /** 索取现金。 */
    public Long wantCash;
    /** 提供地产。 */
    public List<Integer> giveTiles;
    /** 索取地产。 */
    public List<Integer> wantTiles;
    /** 报价中的协商抵债金额，省略视为普通资产交换。 */
    public Long debtCredit;
    /** 报价回应。 */
    public Boolean accept;
}
