# 四张题材卡面

> 2026-10-16 云端迁移：本目录图片已压缩并上传 OSS，本地仅保留元数据、提示词和 URL。历史清单中的原路径与原图摘要用于追溯，当前文件地址、大小及摘要以 `generated-assets/cloud-images.json` 为准；下文原图保留位置及本地发布描述属于迁移前记录。

2026-10-16 按已确认故事与效果新增四张独立插画。SSR 沿用金色强调，UR 使用更丰富的玫瑰金与珠光；统一为暖色 3D 玩具质感。卡名、台词、故事和玩法由现有卡片组件渲染，未烘焙进图片。

| 皮肤 | 类型 | 插画题材 |
| --- | --- | --- |
| `card_ziggs_blast` | SSR 道具 · 爆破鬼才 | 吉格斯的实验室爆炸与飞出去的窗框 |
| `chance_tatakae` | SSR 机会 · 塔塔开 | 艾伦对镜、调查兵团集合和城墙外的晨光 |
| `fortune_talent_recruitment` | UR 财富 · 人才引进 | 招聘会现场突然热情起来的招聘方与苏暖暖 |
| `fortune_parking_tornado` | UR 财富 · 龙卷风摧毁停车场 | 阿福夸张出招，成龙躲避、小玉围观 |

原 PNG 均为 1254×1254，保留原图，不裁切或缩放。运行 WebP 只作编码压缩（quality 82 / method 6），每张约 173～199 KiB，位于 `src/web/public/images/cards/`，由 `card-illustrations.json` 绑定。静态资源随前端发布，不需要额外图床。

- `prompts.json`：人物、题材、场景和统一风格提示词。
- `manifest.json`：原图／运行资源路径、尺寸、字节数与 SHA-256。
- `preview.json`：实际规则严格加载后，通过 `ContentCatalog.describe` 导出的默认卡面资料。
- `index.html` 和 `preview.js`：使用真实 `ContentCard.vue` 与现有边框的开发预览，通过 Vite 的 `/@fs/<index.html绝对路径>` 查看。

故事与台词按确认稿改编，不宣称为原作逐字引用。完整规则和验证边界见 `docs/game-content-runtime.md`。

已逐张查看生成原图，并通过 Vite 开发预览查看四张真实卡片组件，确认 SSR／UR 边框、插画、完整文字与台词。354 个皮肤全部有插画映射，无重复皮肤 ID；新增四张 WebP 可读取。未执行前端 build 或线上部署。
