# 回合阶段与道具规则配置

当前实现采用版本文件作为规则权威来源。默认新局为 `classic-v2`，文件位于 `backend/src/main/resources/game-rules/classic-v2.json`。该版本保持现有玩法和前端协议；没有 `runtime` 的旧快照及 `classic-v1` Flowable 对局继续原 Java 路径。

## 版本加载与发布

| 配置 | 默认值 | 用途 |
| --- | --- | --- |
| `GAME_RULES_ACTIVE_VERSION` | `classic-v2` | 只影响之后新开局 |
| `GAME_RULES_RESOURCES` | `classpath:game-rules/classic-v2.json` | 逗号分隔的版本资源，支持 classpath、file 和资源模式 |

`GameRulesConfig` 在启动时加载并编译全部版本，拒绝重复版本、未知字段、未知处理器、非法引用及参数类型。编译后的阶段、条件、参数及集合不可变，不在游戏中读取文件或热替换规则。

开局保存 `ruleSetVersion` 与原文件 SHA-256 `ruleSetDigest`。恢复进行中的配置对局时，文件必须仍存在且摘要相同。调整文件格式也会改变摘要，因此发布规则应复制为新版本并保留原文件。例如复制为 `classic-v3.json`、修改其中的 `version` 后：

```sh
GAME_RULES_ACTIVE_VERSION=classic-v3 \
GAME_RULES_RESOURCES='classpath:game-rules/classic-v2.json,file:/srv/dfw/rules/classic-v3.json' \
npm start
```

也可使用 Java 命令行配置：

```sh
java -jar backend/target/party-monopoly.jar \
  --game.rules.active-version=classic-v3 \
  --game.rules.resources=classpath:game-rules/classic-v2.json,file:/srv/dfw/rules/classic-v3.json
```

外部版本文件必须随数据库备份保留。旧 `classic-v1` 是保留版本，不能用 JSON 文件重新定义。上述命令只说明规则配置；实际数据源仍由当前项目配置或显式 `spring.datasource.*` 参数决定。

## 配置回合阶段

默认阶段顺序为：

```text
TURN_START 自动阶段
→ before_roll 行动窗口（pre_roll）
→ BEFORE_DICE 自动阶段
→ ROLL
→ AFTER_DICE 自动阶段
→ MOVEMENT
→ LANDING
→ manage 行动窗口（manage）
→ TURN_END 自动阶段
→ END_TURN
```

| handler | 行为 | 配置字段 |
| --- | --- | --- |
| `AUTO_HOOK` | 进入一次时解析并入队步骤 | `id`、`hook`、`steps` |
| `ACTION_WINDOW` | 接受原命令并等待退出结果 | `id`、`phase`、`actions`、`exitOn` |
| `ROLL` | 使用已接受的随机或替代骰子结果 | `id` |
| `MOVEMENT` | 执行一次阶段移动 | `id` |
| `LANDING` | 执行落点及后续事件结算 | `id` |
| `END_TURN` | 切换下一行动者 | `id` |

行动窗口的 `phase` 使用前端已有的 `pre_roll` 或 `manage`。`actions` 使用 `roll`、`card`、`tradeOffer`、`sellBuilding`、`mortgage`、`redeem`、`sellLand`、`endTurn`；`exitOn` 可以引用允许命令，或在出牌窗口使用 `replace_roll`。落点的买地、升级、租金、偿债选择仍使用原 `pending`，不会被窗口配置绕过。

静态校验要求首阶段为 `TURN_START`，唯一的 `ROLL`、`MOVEMENT`、`LANDING`、`END_TURN` 按依赖顺序排列，最后为 `END_TURN`，掷骰与回合结束窗口能到达对应自动段。可以插入自动阶段及配置步骤，无需修改 BPMN。阶段 handler 提供能力，`hook` 是当前阶段的发生名称，本轮尚未实现独立的被动规则订阅总线。

例如在 `ROLL` 之后加入自动阶段：

```json
{
  "id": "after_dice_notice",
  "handler": "AUTO_HOOK",
  "hook": "AFTER_DICE_NOTICE",
  "steps": [
    { "op": "APPLY_EFFECT", "type": "LOG", "message": "{actor.name} 的骰子已确定为 {turn.dice} 点" }
  ]
}
```

回合位置、回合编号、窗口退出结果、掷骰来源和移动步数保存在 `Room.runtime`。自动阶段先保存下一位置，再结算步骤；等待或重启不会重做已提交阶段。按原玩法，监狱/休息跳过在 `TURN_START` 执行，被跳过的回合不进入后续阶段。离线行动者暂停，重新认证后通过通用流程继续。

## 道具定义

每张道具定义 `id`、`activation`、`stages`、`countsAsActive`、`checks`、`steps`。`id` 对应前端既有的卡牌 kind；`ACTIVE` 为主动出牌，`RENT` 在 `rent_choice` 阶段发动且不占主动次数。租金防御组合必须先使用 `WAIVE_RENT` 解决本次费用。

遥控骰子的实际配置结构如下：

```json
{
  "id": "remote",
  "activation": "ACTIVE",
  "stages": ["before_roll"],
  "countsAsActive": true,
  "checks": [
    {
      "condition": {
        "op": "ALL",
        "args": [
          { "op": "GTE", "left": { "ref": "action.point" }, "right": 1 },
          { "op": "LTE", "left": { "ref": "action.point" }, "right": 6 }
        ]
      },
      "error": "请选择 1～6 点"
    }
  ],
  "steps": [
    {
      "op": "APPLY_EFFECT",
      "type": "REPLACE_ROLL",
      "params": { "amount": { "ref": "action.point" } },
      "message": "{actor.name} 本次移动 {action.point} 格"
    }
  ]
}
```

`REPLACE_ROLL` 只设置点数和替代结果，实际移动与落点由阶段执行；含该原语的卡牌不能再配置 `MOVE` 或 `SETTLE_TILE`。停留同样使用 `REPLACE_ROLL`，`amount` 为零。换位、路障及拆房不退出掷骰窗口，仍需随后掷骰。

所有条件和参数在扣卡前检查并解析。基础处理器在生效时复核地产、建筑或玩家仍可用，配置不能绕过已有认证、回合归属、交易身份或资产归属校验。

## 受控 DSL

当前支持有序 `APPLY_EFFECT`，步骤不是任意函数调用。

| 效果类型 | 参数 | 行为 |
| --- | --- | --- |
| `REPLACE_ROLL` | `amount` | 设置 0～6 点的替代结果 |
| `PLACE_BARRIER` | `tileId` | 设置地产路障 |
| `DEMOLISH` | `tileId` | 建筑降低一级，无退款 |
| `SWAP_POSITION` | `targetId` | 交换与目标的位置，不结算落点 |
| `WAIVE_RENT` | 无 | 解决本次租金选择 |
| `MOVE` | `amount` | 有符号移动，一步参数范围为 −40～40 |
| `SETTLE_TILE` | 无 | 结算当前地块 |
| `PAY_FEE` | `amount`，可选 `targetId`、`tileId` | 非负费用，未指定收款人为银行 |
| `LOG` | `message` | 写入公开日志，遵循原日志保留方式 |

表达式支持常量、`{"ref":"路径"}`、`ALL`、`ANY`、`NOT`、`EQ`、`NE`、`GT`、`GTE`、`LT`、`LTE`。`ALL`/`ANY` 使用 `args`，`NOT` 使用 `value`，比较使用 `left`/`right`。数值按数值比较，不把字符串强转为数字；参数必须解析为整数。不存在的目标返回空字段或 `exists=false`，数值比较返回 false，不因缺少可选目标而抛出空指针。

| 上下文 | 允许引用 |
| --- | --- |
| `actor` | `id`、`name`、`cash`、`position` |
| `action` | `point`、`tileId`、`playerId` |
| `targetTile` | `exists`、`id`、`type`、`ownerId`、`level`、`barrier`、`forwardDistance` |
| `targetPlayer` | `exists`、`id`、`bankrupt`、`jailed` |
| `turn` | `phase`、`activeCardUsed`、`dice` |

`message` 使用相同白名单的 `{actor.name}` 等占位符；不能访问手牌、会话令牌或流程对象。自动阶段只允许 `actor`/`turn` 引用，且骰子引用必须位于 `ROLL` 之后。没有 Spring EL、反射、脚本、SQL 或网络访问能力。

每文件最多 256 KiB，阶段/道具/组合数量及表达式深度有限；效果和阶段自动推进均有工作量上限。配置非法时拒绝启动，动态输入或处理器失败时回滚当前短事务，返回原错误协议。

组合遇到买地、升级、租金或偿债选择时，`effectsWaiting` 和剩余效果持久化。解决当前选择的效果先于暂停组合，选择完成后继续。清算或房主管理导致回合换人时，旧回合的单玩家剩余组合取消；多目标事件需要后续实现明确的目标游标和完成边界。

## 当前实现边界与验证

基础经济、地图、机会/财富事件池、道具抽取池仍使用已有 Java 定义。本轮配置化的是回合阶段、道具窗口/次数、发动条件与效果组合。`FOR_EACH`、随机分支、数值公式、持续状态、事件总线、全图结算和新增前端决策形式仍未实现；完整架构文档中的这些示例不能直接加载。

本轮后端编译跳过测试，前后端类型检查通过。原 HTTP/WebSocket 下查看了新局、交易、六种道具、次数拒绝与阶段恢复；部分道具使用停服后准备的临时库手牌和建筑状态。加载附加版本后，原 `classic-v2` 对局继续原规则，新局执行新增的回合开始及掷骰后自动日志步骤，BPMN 未修改。没有编写单元测试或冒烟测试，没有执行前端 build 或已有冒烟脚本，没有自动提交。
