package com.dfw.interfaces.dto;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 命令操作参数；由接口校验后交给领域处理。 */
@Getter
@Setter
public class GameAction {
    /** 本人修改后的可选昵称，留空恢复默认名。 */
    public String name;
    /** 玩家发送的公开聊天正文，最多三百字。 */
    public String message;
    /** 聊天正文中选中的玩家和地块引用，旧客户端可省略。 */
    public List<ChatReferenceInput> references;
    /** 房主批准的角色申请标识。 */
    public String claimId;
    /** 待选身份，防止恢复后提交旧选择。 */
    public String decisionId;
    /** 手牌超限时按本人当前上限恰好保留的牌。 */
    public List<String> keepCardIds;
    /** 制衡确认弃掉的本人旧牌，不含发动卡。 */
    public List<String> discardCardIds;
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
    /** 房主在大厅选择的地图大小，small、medium 或 large。 */
    public String mapSize;
    /** 准备状态。 */
    public Boolean value;
    /** 是否使用免租。 */
    public Boolean useCard;
    /** 主动道具标识。 */
    public String cardId;
    /** 指定点数。 */
    public Integer point;
    /** 地产索引。 */
    public Integer tileId;
    /** 目标玩家。 */
    public String playerId;
    /** 斗转星移的第二名玩家，其他操作留空。 */
    public String secondPlayerId;
    /** 剑气分配的地产列表，可重复。 */
    public List<Integer> tileIds;
    /** 空间裂隙另一端。 */
    public Integer secondTileId;
    /** 贯穿攻击方向。 */
    public Integer direction;
    /** 交易接收人。 */
    public String toId;
    /** 发起人现金。 */
    public Long giveCash;
    /** 接收人现金。 */
    public Long wantCash;
    /** 发起人地产。 */
    public List<Integer> giveTiles;
    /** 接收人地产。 */
    public List<Integer> wantTiles;
    /** 接收人同意抵扣的债务金额，可选。 */
    public Long debtCredit;
    /** 是否接受交易。 */
    public Boolean accept;
}
