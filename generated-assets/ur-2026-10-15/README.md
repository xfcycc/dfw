# UR 卡面与新增道具插画

2026-10-15 按现有粉发苏暖暖、暖色童话小镇、柔和 3D 玩具质感生成，共 9 张独立插画：6 张新 UR 具名卡面及 3 类 UR 通用图。UR 使用更丰富的玫瑰金、珠光和红宝石细节；负面事件以表情、账单或流出金币表达。卡名、等级、金额和效果由游戏组件渲染，不烘焙到插画中。

| 类型 | 卡面 | 文件 |
| --- | --- | --- |
| chance | 城市大奖 | [chance_windfall_jackpot](chance/chance_windfall_jackpot.png) |
| chance | 天价账单 | [chance_catastrophic_bill](chance/chance_catastrophic_bill.png) |
| fortune | 财富暴击 | [fortune_fortune_jackpot](fortune/fortune_fortune_jackpot.png) |
| fortune | 投资暴雷 | [fortune_fortune_crash](fortune/fortune_fortune_crash.png) |
| card | 命运硬币 | [card_fate_coin](card/card_fate_coin.png) |
| fallback | UR机会通用卡面 | [ur_chance](fallback/ur_chance.png) |
| card | 孤注一掷 | [card_all_in_ticket](card/card_all_in_ticket.png) |
| fallback | UR财富通用卡面 | [ur_fortune](fallback/ur_fortune.png) |
| fallback | UR道具通用卡面 | [ur_card](fallback/ur_card.png) |

## 文件与接入

- `index.html`：可离线打开的九张卡面预览，使用当前项目卡面样式的快照，点击插画可查看原图。
- `prompts.json`：每张独立提示词；使用旧卡面原图作为人物与风格参考。
- `generation-results.json`：生成结果到本地原始文件的映射；原生成文件保留。
- `manifest.json`：PNG／WebP 路径、尺寸、SHA-256、卡池资料、OSS URL 及远端校验。
- `oss-upload-response.json`：本机既有 PicGo 上传结果。

9 张生成原图实际均为 1254×1254 PNG，未裁切、未缩放，只转换成 WebP quality 82／method 6。PNG 总计 22,096,800 字节，WebP 总计 1,950,916 字节，减少 91.17%。运行使用 OSS WebP，本地 PNG 和 WebP 均保留用于追溯。

`src/web/card-illustrations.json` 新增 9 项地址，原 342 项保持不变。当前 348 个真实皮肤 ID 全部有独立插画，额外 `ur_chance`／`ur_fortune`／`ur_card` 是 UR 通用图。`ContentCard.vue` 找不到独立 UR 插画时按池使用本档通用图，不再裁切 SSR 图集；原 R／SR／SSR 旧手牌兜底保留。

## 实际验证边界

9 张原图已逐张视觉查看；核对原图／WebP 尺寸、文件可读性、内容哈希唯一性、皮肤覆盖与旧地址保留。9 张 OSS 图片均核对 HTTP 200、image/webp、Content-Length 与 Content-MD5。前端类型检查、差异检查通过，并查看本地静态卡面预览的图片、UR 标识及文字排版。未执行前端 build、单元测试、冒烟测试、后端／数据库操作、真实联机或线上部署；未提交或推送代码。
