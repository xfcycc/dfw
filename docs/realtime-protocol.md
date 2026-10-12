# 原生 WebSocket 联机协议

HTTP 与 `/ws` 共用端口 3001。开发由 Vite 代理，生产使用 HTTPS / WSS。没有 Socket.IO、STOMP 或长轮询。

消息格式：

```json
{"type":"game:command","requestId":"浏览器关联标识","payload":{"id":"UUID","expectedVersion":8,"action":{"type":"roll"}}}
```

`requestId` 只关联网络响应；`payload.id` 才是持久化的业务去重标识。服务端公共状态版本以已提交结果为准。

| 方向 | type | payload 与用途 |
| --- | --- | --- |
| 客户端 → 服务端 | `session:authenticate` | `{token}`，连接首条消息，原始令牌不放 URL |
| 服务端 → 客户端 | `session:authenticated` | `{ok:true}`，之后允许同步或发送命令 |
| 客户端 → 服务端 | `room:sync` | 无载荷，申请本角色全量状态 |
| 服务端 → 客户端 | `room:snapshot` | `PublicRoomState`，所有认证角色可见 |
| 服务端 → 玩家 | `player:private` | `PrivatePlayerState`，只给本人有效连接 |
| 客户端 → 服务端 | `game:command` | `GameCommand`，必须附请求关联标识才能匹配回执 |
| 服务端 → 客户端 | `command:result` | `CommandResult`，带原 `requestId` |
| 服务端 → 客户端 | `room:error` | 认证、同步或消息格式失败的 `CommandResult` |
| 服务端 → 旧连接 | `session:replaced` | 无私人载荷，旧客户端停止自动重连 |
| 客户端 → 服务端 | `heartbeat` | 无载荷，20 秒一次 |
| 服务端 → 客户端 | `heartbeat:ack` | 无业务载荷，用于连接活跃检查 |

认证前不发送任何房间或私人状态；5 秒未认证关闭连接。无效身份返回错误后关闭连接。WebSocket 文本消息上限 64 KiB。服务端连续 60 秒未收到消息关闭连接。

客户端连续 60 秒未收到服务消息重连，重连间隔从 700 毫秒递增到最多 4 秒。每次重新发送认证，接收全量状态后恢复页面；不自动重发掷骰、交易等业务命令。命令等待 8 秒，超时显示同步提示并申请 `room:sync` 核对已提交结果。

`STALE_VERSION` 拒绝旧版本并同步最新状态。相同命令 ID 重放优先返回已保存回执，操作者须与原回执相同。`FORBIDDEN` 用于观战或被替换连接操作；`INVALID_INPUT` 用于原接口边界的参数错误；`SAVE_FAILED` 不包含数据库细节。

观战的公开玩家没有手牌和手牌数量，公开日志不包含抽卡类型或数量。待确认报价内容仅交易双方收到，公共状态仅有 `tradePending` 标志；交易完成后的资金和地产结果进入公开记录。
