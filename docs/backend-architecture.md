# Spring Boot 后端分层说明

运行基线：Spring Boot 4.1.1、Java 21、Maven、Spring JDBC、MySQL Connector/J、Flowable 8.0.0。单进程管理多个房间，每个房间是一份可独立提交的聚合快照。

## 调用关系

```text
HTTP Controller / WebSocket Handler
                ↓
         RoomService 应用编排
          ↙             ↘
 GameWorkflow 通用流程 RoomRepository / StatePublisher 端口
    ↙             ↘                 ↓
 GameEngine / EffectResolver   Flowable 引擎   MySQL / WebSocket 适配器
          ↓                       ↓
 地图/回合/经济/道具/交易     同库流程与任务表
```

| 包 | 责任与修改入口 |
| --- | --- |
| `interfaces` | HTTP 路由、消息信封、输入类型校验、错误结果及白名单 VO 投影；新增联机消息先改这里 |
| `application` | 身份、房间锁、快照候选、幂等回执、短事务编排、连接与定时推进；不编写 SQL |
| `domain` | 无 Spring、HTTP、WebSocket 和数据库依赖的可变房间聚合；规则只改变应用层传入的候选副本 |
| `infrastructure` | MySQL 参数化 JDBC、JSON 快照、随机令牌、服务端随机源、有效连接与投递适配器 |
| `config` | 数据源、允许的 Origin、WebSocket 注册和运行参数 |

## 规则修改

- 经济默认倍率与固定金额：`game-rules/economy.json`、`EconomyRules`；本局快照在 `Room.economy`；估值公式在 `Rules`。地区地名池：`game-rules/map-regions.json`、`MapRegions`；房间 `mapSize` 由房主在大厅修改；按大小、人数和种子生成矩形闭环：`MapService`、`Rules.mapLength`，前端 `boardDimensions` 与 `tileCoordinates` 使用同一边长口径。地区目前只命名，配置细节见 [配置指南](game-configuration.md)。
- 行动者、跳过回合、移动与机会财富落点：`TurnService`。
- 资产变现、经营与破产清算：`EconomyService`。
- 六种道具窗口、次数、条件和组合：`game-rules/card-events.json`；公共阶段：`turn-rules.json`；配置加载校验：`GameRulesConfig`、`RuleSet`、`RuleExpression`；阶段推进与出牌：`RuleRuntime`。`ItemService` 提供基础道具效果；报价和交换：`TradeService`。
- 完整机会池与灾害补偿订阅：`chance-events.json`；完整财富池：`fortune-events.json`。全图扣款、目标游标、单地块实际灾害、随机补偿与银行奖励由 `EventService`、`GlobalEventFrame` 处理；三个后续个人回合陷阱免疫由 `StatusService`、`PlayerStatus` 处理。
- 大厅、开局、再开、移出离线玩家与命令路由：`GameEngine`。
- 通用流程启动、任务完成和系统推进同步：`application/GameWorkflow`；固定流程：`processes/generic-game-runtime.bpmn20.xml`；无状态回调和受控同步作用域：`infrastructure/flowable/`。
- 原子效果注册和联动顺序：`EffectResolver`；内部运行时和队列：`GameRuntime`、`GameEffect`。四类文件启动时编译为唯一不可变 `RuleSet`，直接注入领域服务，全部对局统一走队列。配置契约见 [回合阶段与道具规则配置](game-rules-dsl.md)。
- 前端 `src/shared/types.ts` 和 Java DTO/VO 同时维护 JSON 契约；`src/shared/rules.ts` 为展示及地图算法对照。任何后端规则变化都要核对展示与实际页面/API 行为。

`StateViews` 向所有角色投影本局经济参数、地区元数据、净资产和并列名次，不输出手牌或报价。净资产按成本估值扣抵押地产固定赎回费用，单地产剩余权益最低为零；排名事件选择随机并列目标尚未接入。开局保存地图及固定现金步骤最终值，重启后的新经济和命名配置仅作用于新局；既有行为规则仍按上述当前规则集执行。

## 一致性与隐私

同一房间的 HTTP 变更、认证、游戏命令、断线和定时任务使用同一把 `ReentrantLock`。不同房间独立，Hikari 池最大连接数为四，InnoDB 在短事务内按涉及记录协调写入，不在事务内等待网络。

命令在锁内先查历史回执，核对身份与规范化 action 摘要，再检查预期版本。旧回执缺少摘要时保留原行为；新回执同 ID 改 action 拒绝，摘要只保存在存储 JSON，不修改公开结果。复制原快照后才运行规则；应用层的 `TransactionTemplate` 覆盖 Flowable 推进、房间快照和命令回执，Repository 的事务加入同一外层事务。认证、断线和移出离线玩家也同步处理任务关联。成功后替换内存快照并推送。数据库保存失败不更改原快照，流程与业务写入一起回滚。

公开快照通过 `StateViews` 显式白名单生成。内部 Room、Player 不直接发送；私人手牌和双方报价逐连接投递。玩家连接替换后，旧连接不能执行命令，其关闭回调也不能覆盖新连接的在线状态。底层发送使用 `ConcurrentWebSocketSessionDecorator`，发送失败不撤回已提交操作。

断线保存失败时保留原状态，定时任务根据有效连接注册再次尝试持久化离线状态。玩法与交易不限时，不再调度到期取消报价。重启先将内存连接状态设为离线，清除旧报价计时值但保留报价；认证成功后继续原操作。不提供旧格式存档迁移或恢复兜底。

新局一局一个 Flowable 实例，以 `gameId` 为业务关联。通用任务循环接受原命令，调用领域校验和效果队列，再等待下一操作或结束。任务 assignee 只标记主要等待人，交易发起者取消报价和房主管理仍使用原领域权限。启动批量读取通用任务并校验内部运行时关联，校验流程实例、任务、房间、对局、决策键及阶段游标，错配时拒绝继续，不静默重建流程。自动阶段、替代骰子结果和暂停效果的游标随房间快照提交，不重做已提交步骤。进行中对局后续抽取/出牌使用重启后当前配置，原队列、全图批次金额、pending 和已提交样本保持保存值；恢复时刷新三项扩展机会说明。房间不再绑定规则版本或引擎模式，房间并发 version 和命令 expectedVersion 保留；历史快照独立保存格式版本与四类配置的规范化摘要，用于显式另建房间恢复时检查兼容性。

## 存储与切换

保留原 `rooms`、`sessions`、`receipts` 的房间号接口及回执语义，前两表补充 `room_id`；新增 `room_archives`、`room_snapshots`、`room_history`，历史与身份按永久房间标识关联。MySQL schema 位于 `backend/src/main/resources/schema.sql`，启动创建缺失表及身份关联列，批量补齐当前格式旧房间的档案和基线，不删除旧数据。所有表使用 InnoDB / utf8mb4；状态与回执保持原 JSON 文本，主键使用 ascii_bin，避免 MySQL 默认比较规则改变命令 ID 语义。

`MysqlRoomRepository` 使用参数化 upsert 保存房间，身份、回执、完整历史快照与本次事实一次事务提交；领域层 `HistoryTrace` 收集事实，Repository 批量追加，失败一起回滚。SQL 只位于 Repository，应用和领域服务不包含数据库语句。启动批量读取所有房间，不逐玩家查询。

历史 SQLite 导入工具（不迁移旧游戏运行时）：停止旧游戏服务 → `npm run db:init` 创建空 MySQL 目标 → `npm run db:migrate:sqlite` 原子导入 → 启动 Java 并刷新浏览器。导入工具只读取得 SQLite 一致性备份，批量读取三表，按行数及字节数分批写入，验证数量后提交；目标非空时拒绝覆盖，源文件保留。导入锁只协调同目标库的导入工具，不替代游戏停服。

后端直接使用 Java 启动，已移除 Node 启动包装器。独立数据库工具加载忽略的 `.env.local`，Java/IDE 则通过进程环境变量或 Spring 参数提供配置，不自动加载该文件。提交版本的 `application.yml` 使用 `DB_*` 占位符及示例默认值，实际数据库凭据不纳入提交。运行时设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，也可用显式 `spring.datasource.*` 参数或 `SPRING_DATASOURCE_*` 环境变量覆盖。Docker Compose 使用 `.env` 和独立 MySQL 服务，数据库文件位于 `mysql-data` 卷。备份应涵盖整个数据库的业务表和 Flowable 表，不能把不同时间的房间快照、命令回执与流程任务混合恢复。`FLOWABLE_SCHEMA_UPDATE` 默认 `true`，允许启动创建缺失引擎表；表结构准备完成后可设为 `false`，不清理已有数据。

仅单实例支持。未来更换数据库可以实现 `RoomRepository`；多实例房间协调需要另外设计，当前不添加分布式锁或消息代理。

## 显式档案恢复

固定存档码是共享查找与恢复入口；`ArchiveService` 提供公开索引、历史和保存点预览，写入与角色分配复用 `RoomService` 的房间锁及事务。完整内部快照只在服务端恢复，公开局面使用保存时的 `StateViews` 投影。

恢复生成新 `roomId`、房间号、存档码及 `gameId`，记录来源；清除旧会话绑定，为恢复者建立新身份。BPMN 的恢复分支直接到等待任务，不调用初始结算。复制已保存的游标、效果、骰子、内容结果、pending 和 trade，只重新绑定流程实例与任务。恢复配置期间冻结推进与自动房主转交，房主批准访客角色后显式继续；恢复大厅解除暂停后可按原规则加入新玩家。

审批角色与状态同事务提交，提交后立即撤销旧连接权限；旧连接沿用认证清理窗口关闭，保证房主交接自己的席位时仍可接收命令回执。恢复请求按源房间与请求 UUID 去重，返回同一房间、同一会话；角色被交接后，旧恢复请求不能重新获取已撤销席位。细节及验收见 [存档与历史恢复](game-archives.md)。
