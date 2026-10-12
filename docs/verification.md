# MySQL 迁移验证记录

日期：2026-10-12。当前后端为 Spring Boot 4.1.1 / Java 21，持久化为 MySQL / InnoDB，联机为原生 WebSocket。本轮结果不沿用之前 SQLite 的运行结论。

## 静态检查与编译

- `npm run typecheck` 通过：Vue、共享协议和数据库／迁移／冒烟工具类型检查，没有前端构建产物。
- `mvn -f backend/pom.xml -DskipTests package` 通过：生成 MySQL 版 `backend/target/party-monopoly.jar`，未运行测试或前端 build。
- 移除 SQLite JDBC 与 SqliteRoomRepository，新增 MySQL Connector/J、MysqlRoomRepository 和 InnoDB schema。应用和领域服务不增加 SQL。
- `docker compose --env-file .env.example config --quiet` 通过，仅解析配置，没有构建或启动容器。
- 本地连接资料保存在权限 600 的 `.env.local`；Git 与 Docker 构建上下文忽略该文件，Java JAR 不包含本地凭据。

## 本机数据库和真实导入

- 实际连接本机 `127.0.0.1:3306`，版本 8.0.38；目标 `dfw` 原先没有表。
- `npm run db:init` 创建三张缺失表，实际核验 rooms / sessions / receipts 均为 InnoDB、utf8mb4_bin。
- 检查旧游戏端口没有监听后，执行 `npm run db:migrate:sqlite`，真实导入 2 个房间、4 个会话、9 条命令回执。
- 导入前后 `data/game.sqlite` SHA-256 同为 `f4a322c158e4b137ec4ff55cad0dd97eebdd4523cedf067b3c8e46a508a44cdf`，原文件保留。
- 导入工具先取得只读一致性备份，目标必须为空，批量插入并一次事务提交；本轮未删除或覆盖其他库数据。

## HTTP / WebSocket / MySQL 集成冒烟

`npm run smoke` 启动实际 Java JAR，通过随机名创建本次专用的 `dfw_smoke_*` 库，15 组场景全部通过：

1. 同端口静态文件及玩家／观战页面直达回退，使用临时 HTML 夹具，没有前端 build。
2. 首消息认证、认证前数据隔离、无效身份、5 秒认证超时和 HTTP 参数兼容校验。
3. 2～6 人地图、固定格、随机格范围与间距、名称唯一、三档价格分布及多种子配置。
4. 满房、开局后新席位拒绝、观战命令拒绝与私人数据隔离。
5. 回合权限、报价只向双方发送、交易现金、命令去重和旧版本拒绝。
6. Java 与原 TypeScript 的实际相同种子地图配置逐项一致。
7. 初始临时 MySQL 库无表，Java 自行创建 schema；中文与四字节 emoji 名称写入后重启恢复正确。
8. 买地、落点建房、停留、重复升级拒绝、卖房、抵押、赎回、卖地。
9. 六种道具、主动次数限制、路障、免租、租金双方结算；换位的休息／监狱边界。
10. 偿债权限、筹足支付、破产清算、最终胜负和再开地图。
11. 建筑和抵押随交易转移、资金不足零变更；MySQL 触发器模拟回执写入失败，掷骰与交易接受均整体回滚，报价保留。
12. 机会与财富的实际随机事件结算。
13. 房间隔离、连接替换、交易断线冻结、重启恢复报价和历史回执、房主移交及离线清算。
14. 在线交易实际到期自动取消，临时夹具缩短等待时间。
15. SQLite 一致性备份导入另一临时 MySQL 库，三表内容逐项一致；再次导入非空库被拒绝且原内容保留；旧摘要身份、棋盘、资产、手牌与待处理事项恢复正确。

受控落点夹具只在临时 Java 服务停止时批量更新，使用单条 JSON_TABLE 更新多房间，不循环逐条查询。临时库只有 CREATE DATABASE 成功后才登记清理，结束只删除登记库。实际查询确认本轮结束后残留 `dfw_smoke_*` 库数量为 0。

脚本没有直接调用 Java 领域方法，属于真实 HTTP / WebSocket 集成冒烟，未新增单元测试。

## 浏览器验证

使用默认 Java 3001 和 Vite 5173，通过真实 `dfw` 数据库验证已迁移房间 269682：

- localhost 与 127.0.0.1 两个浏览器来源，分别恢复原柚子与桃子身份，没有读取、替换或重新签发其玩家令牌。
- 原现金 4,300 / 6,100、地产、当前位置、各自私人手牌及第 2 次行动恢复；未替用户继续掷骰或交易。
- 第一玩家上线时等待离线行动者，第二玩家恢复后暂停解除；刷新仍回到原席位，房主按原连接规则移交。
- 公共观战实际同步，页面不包含“我的道具”面板。截图保存为 `artifacts/mysql-restored-room.png`。
- 原 Vue 页面与布局未修改。本轮重点验证 MySQL 恢复与联机；手机布局沿用前一轮结果，不重新表述为本轮真实手机硬件验收。

## 部署与边界

Docker / Compose 改为 Java 游戏服务加独立 MySQL 8.4 服务，使用 mysql-data 持久化卷，不公开数据库端口。本地已存在的 3306 MySQL 没有被升级、重启或替换。

未编写单元测试，未执行前端 build、Docker 镜像构建、Git 提交或公网部署。Docker 配置准备不等于实际镜像运行；自然对局数值平衡、负载和真实手机／电视仍未验收。随机事件不穷举所有结果。

验证用 Java / Vite 服务结束后停止；本地 MySQL 和导入数据保留。后续执行 `npm run dev` 可自动加载 `.env.local`，数据库初始化是创建缺失项，不清空现有对局。
