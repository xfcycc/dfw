package com.dfw.domain;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

/** 私人选择及父效果返回位置，随同房间快照保存，不直接公开投影。 */
@Getter
@Setter
public class ContentChoice {
    /** 唯一选择身份。 */
    public String id;
    /** free_upgrade、full_upgrade、self_destroy、opponent_destroy、gamble、release、movement_release、hand_limit、recycle 或 discover。 */
    public String type;
    /** 实际做选择的玩家。 */
    public String playerId;
    /** 位移解控关联的待结算效果身份，仅内部保存，不向客户端暴露队列。 */
    public List<String> effectIds = new ArrayList<>();
    /** 仅选择者可见的玩家候选。 */
    public List<String> playerIds = new ArrayList<>();
    /** 私人选择的补充提示。 */
    public String prompt;
    /** 可选地产，确认时再次校验合法性。 */
    public List<Integer> tileIds = new ArrayList<>();
    /** 超限、制衡或发现的完整候选牌，补给结果已确定。 */
    public List<Card> cards = new ArrayList<>();
    /** 触发选择的主动卡，免费升级确认成功才消费。 */
    public String cardId;
    /** 选择对应的已抽取事件结果。 */
    public ContentResult content;
    /** 父等待位置，超限整理后恢复，费用和交易不被覆盖。 */
    public PendingDecision returnPending;
    /** 父操作阶段。 */
    public String returnPhase;
    /** 尚未接受的限制次数。 */
    public int skipTurns;
    /** 是否监禁。 */
    public boolean jailed;
    /** 多张重塑材料各自的固定候选，仅选择者可见。 */
    public Map<String,List<Card>> cardOptions = new LinkedHashMap<>();
    /** 已锁定的有序材料，防止重连重抽。 */
    public List<String> materialIds = new ArrayList<>();
    /** 蓝银缠绕的独立解控入口，不跳过资产经营。 */
    public boolean binding;
    /** 本次休息是否为自愿代价，不产生连续控制保护。 */
    public boolean voluntaryRestriction;
    /** 弃置一张牌的实际现金，选择时冻结。 */
    public long discardCash;
    /** 赌博已提交的随机样本，放弃时为空。 */
    public Integer sample;
    /** 已消费天锁斩月后固定的两枚骰点。 */
    public List<Integer> numbers = new ArrayList<>();
}
