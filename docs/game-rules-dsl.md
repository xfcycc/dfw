# 回合阶段与道具规则配置

当前实现采用版本文件作为规则权威来源。默认新局为 `classic-v3`，文件位于 `backend/src/main/resources/game-rules/classic-v3.json`。该版本保留六种道具和财富格，在机会格追加全体扣款、陷阱免疫和龙卷风；原 `classic-v2.json` 不变并继续加载，进行中对局仍绑定原版本。前端命令保持原协议；没有 `runtime` 的旧快照及 `classic-v1` Flowable 对局继续原 Java 路径。

## 版本加载与发布

| 配置 | 默认值 | 用途 |
| --- | --- | --- |
| `GAME_RULES_ACTIVE_VERSION` | `classic-v3` | 只影响之后新开局 |
| `GAME_RULES_RESOURCES` | `classpath:game-rules/classic-v*.json` | 逗号分隔的版本资源，支持 classpath、file 和资源模式 |

`GameRulesConfig` 在启动时加载并编译全部版本，拒绝重复版本、未知字段、未知处理器、非法引用及参数类型。编译后的阶段、条件、参数及集合不可变，不在游戏中读取文件或热替换规则。

开局保存 `ruleSetVersion` 与原文件 SHA-256 `ruleSetDigest`。恢复进行中的配置对局时，文件必须仍存在且摘要相同。调整文件格式也会改变摘要，因此发布规则应复制为新版本并保留原文件。例如复制为 `custom-v4.json`、修改其中的 `version` 后：

```sh
GAME_RULES_ACTIVE_VERSION=custom-v4 \
GAME_RULES_RESOURCES='classpath:game-rules/classic-v*.json,file:/srv/dfw/rules/custom-v4.json' \
npm start
```

也可使用 Java 命令行配置：

```sh
java -jar backend/target/party-monopoly.jar \
  --game.rules.active-version=custom-v4 \
  --game.rules.resources='classpath:game-rules/classic-v*.json,file:/srv/dfw/rules/custom-v4.json'
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
| `CHARGE_ALL` | `amount` | 按发动时座位顺序固定未破产目标，逐人向银行付费，复用偿债/清算 |
| `ADD_STATUS` | `statusId: "TRAP_IMMUNITY"`、`amount: 3` | 立即生效，三个后续个人回合结束后到期，重复刷新 |
| `DESTROY_BUILDINGS` | `tileId` 或 `selector: "RANDOM_BUILT_PROPERTY"`，以及 `probability`、`compensationPercent` | 指定或随机一块地产全部建筑，按实际建筑成本损失进行一次概率补偿；百分比为 0～100 的整数 |
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

组合遇到买地、升级、租金或偿债选择时，`effectsWaiting` 和剩余效果持久化。解决当前选择的效果先于暂停组合，选择完成后继续。清算或房主管理导致回合换人时，旧回合的单玩家剩余组合取消。`CHARGE_ALL` 在 `globalEvent` 中保存固定目标、下一目标索引、金额和返回阶段；当前债务保存在原 `pending`，解决后继续队列。批次内淘汰不换回合、不提前判胜，全部目标处理完才判胜或切换已淘汰的行动者。目标被房主移出时清除其待偿债，后续已淘汰目标跳过。

## 机会事件与持续状态

`chanceEvents` 为可选追加事件数组，每项提供唯一 `id`、公开 `description` 和非空 `steps`。原六种机会结果与每个追加事件等权：`classic-v3` 共九种结果，每种概率为 1/9。每回合机会/财富共用原来的单次抽取限制，财富格仍为原六种结果。事件步骤只开放 `CHARGE_ALL`、`ADD_STATUS`、`DESTROY_BUILDINGS` 和 `LOG`；自动引用限 `actor`/`turn`，不接受缺失的客户端目标输入。

```json
{
  "id": "global_levy",
  "description": "全体各向银行支付 300",
  "steps": [{ "op": "APPLY_EFFECT", "type": "CHARGE_ALL", "params": { "amount": 300 } }]
}
```

指定地块的基础效果示例（`tileId` 必须是当前地图有效地产索引）：

```json
{
  "op": "APPLY_EFFECT",
  "type": "DESTROY_BUILDINGS",
  "params": { "tileId": 1, "probability": 30, "compensationPercent": 50 }
}
```

新版本龙卷风使用随机有建筑地产选择器，允许选中自己的地产；产权、抵押和路障保留。只对实际摧毁的建筑级数计算造价损失，无建筑不抽补偿样本、不赔付。单地块对应一个受损业主，以 0～99 样本 `< 30` 判定补偿，赔付为损失的 50% 向下取整。目标、产权人、级数、损失、样本和赔付作为 `lastDisaster` 保存在内部运行时；这是最近一次实际损失记录，不是长期事件审计。未实现建筑保护状态，不将陷阱免疫误当灾害保护。

`Player.statuses` 保存状态类型、来源效果、施放/刷新回合和剩余后续个人回合。期限仅在持有人结束回合时扣减，施放当回合不扣，跳过行动照常计数；第三个后续个人回合结束时移除。路障标签按 `TRAP` 判定免疫，免疫时继续移动且保留路障；租金、罚款、监禁和灾害不受影响。重复获取刷新为三回合，破产或再开局清除状态。

偿债权限来自原 `pending.playerId`，Flowable 等待者和离线暂停也跟随该玩家；其他人离线不阻塞这批自动扣费。前端继续使用原资产管理弹框和 `sellBuilding`/`mortgage`/`sellLand` 命令，`turnIndex` 仍表示原回合持有人。公开快照只追加 `chanceEventDescriptions` 和 `players[].trapImmunityTurns`，旧快照缺省为空或零，内部队列、目标游标、随机样本、来源 ID 不对外发送。旧客户端命令兼容，但完整操作新事件需刷新前端以识别他人回合中的偿债。

## 当前实现边界与验证

四类首期能力已通过受控 `APPLY_EFFECT` 和专用全体扣款执行帧接入，不增加 BPMN 节点。完整 `FOR_EACH`、任意嵌套执行帧、通用随机分支、建筑保护、多地块损失聚合、被动事件总线、长期结构化事件审计和规则发布管理仍未实现。补偿目前属于单地块灾害处理器的原子结果，尚未提供通用的独立赔付订阅器；不能将完整架构设计示例当成已支持的 DSL。

实现阶段已通过后端编译、前后端类型检查，以及生产 `GameRulesConfig` 的 v2/v3 加载校验。2026-10-12 又以 `mvn -o -f backend/pom.xml -Dmaven.test.skip=true package` 打包当前代码，在独立 MySQL 库 `dfw_acceptance_20261012`、3012 端口和房间 `357584` 完成真实 HTTP/WebSocket 与持久化验收，结果如下。未编写单元测试或冒烟测试，未执行前端 build、已有冒烟脚本或自动提交。


### 2026-10-12 运行验收记录

验收通过真实创建/加入、WebSocket 认证和原业务命令进行。为了稳定到达边界，仅在独立验收服务停止后准备手牌、现金、产权、建筑、休息次数、位置或待执行效果；没有修改生产规则文件、注入随机源、篡改 Flowable 任务或在运行中改库。定向准备的效果队列用于检查暂停/恢复边界，随后另开一局，以原有停留命令结算真实机会格，确认未修改的 `classic-v3` 事件池实际触发三种新增事件。

| 场景 | 实际结果 |
| --- | --- |
| 全体扣款中途偿债 | A/B/C 初始现金为 1000/100/1000，B 持有一座地价 800 的一级建筑。A 先付 300，B 等待偿债 300，C 尚未付；快照现金为 700/100/1000，下一目标索引为 2。B 卖房取得 200 后自动付清，C 随后付费，最终为 700/0/700；批次和 pending 清空 |
| 非决策人权限与幂等 | C 替 B 卖房被拒绝，提示还没有轮到你；重复提交同一个 B 卖房命令 ID 返回原成功版本 21，没有再次卖房或扣款 |
| 偿债人断线重连 | B 断线后仅等待 B，Flowable assignee 为 B；重连后债务、任务 2509、目标游标和现金不变 |
| 正常重启恢复 | 在 B 的债务等待点停止并启动服务；任务 2509、队列、下一目标 2 和现金保持一致，B 可继续卖房偿债 |
| 三个后续个人回合 | C 在全局回合 1 获得免疫；施放当回合不计。后续第一个个人回合（回合 4）休息也计数；结束回合 4、7、10 后剩余数分别为 2、1、0，到回合 13 已无免疫 |
| 路障与租金 | 有免疫时从位置 0 移动三格，穿过位置 2 路障到位置 3，路障保留，落点租金 80 正常支付。免疫到期后以相同起点和步数移动，停在位置 2，路障消耗 |
| 持续状态和随机结果恢复 | 免疫生效后重启，剩余数仍为 3；已经完成的灾害仍保存样本 43、损失 2400、补偿 0，未重抽或重复支付 |
| 指定地块摧毁与补偿 | 准备的三层建筑全部降至零，产权保留。真实 30% 抽样同时出现成功和失败；其中损失 2400 获赔 1200，业主现金从 700 增至 1900。再次摧毁同一空地块，即便准备的补偿概率为 100%，也无实际损失、无额外补偿 |
| 当前行动者先破产 | A 先清算后仍保留批次，B 进入偿债，C 现金仍为 1000；房主移出离线债务人 B 后，C 先支付 300，再成为赢家。结束时无 pending、无全图帧、无运行任务 |
| 实际机会格入口 | 使用未修改的 v3，通过停留卡触发全体各付 300、陷阱免疫和重复刷新、随机有建筑地产摧毁。一次自然灾害选中地块 15，摧毁三级、损失 1800，样本 23 命中 30% 阈值，赔付 900 |
| 非决策人离线和进程崩溃 | B 偿债时原回合玩家 C 断线，pausedReason 仍为空。强制结束验收进程后重启，任务 17509、已结算数 43、下一目标 2 和现金 700/100/1000 不变。只连接 A/B，B 仍可完成卖房；C 自动支付后才暂停等待 C 恢复自己的回合。C 重连后解除暂停 |

以上场景未发现业务缺陷，本轮没有修改业务代码。验收服务结束后停止，独立库和最后快照保留用于追溯。此记录是 HTTP/WebSocket、真实数据库和进程恢复验收，不包含浏览器页面点选/布局验收，也没有执行数据库写入失败的事务回滚演练；不能把这些未执行项写成通过。
