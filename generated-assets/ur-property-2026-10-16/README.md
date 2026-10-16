# UR 地产事件卡面

2026-10-16 新增「爆破狂人」（机会）与「基建狂魔」（财富）。使用现有粉发苏暖暖、暖色童话小镇、柔和 3D 玩具风格，沿用 UR 玫瑰金、珠光及宝石细节。两张都是立即处理的事件，不进入手牌。

| 皮肤 | 插画故事 | 原图 |
| --- | --- | --- |
| `chance_demolition_fanatic` | 苏暖暖在烟花告别秀中启动单栋对手建筑的拆除，地基与蓝色业主旗帜保留 | [爆破狂人](chance_demolition_fanatic.png) |
| `fortune_construction_frenzy` | 苏暖暖喝着奶茶指挥模块吊装，第三层与屋顶即将落位，外卖车刚到现场 | [基建狂魔](fortune_construction_frenzy.png) |

- `prompts.json`：生成提示词，人物和渲染参考 `generated-assets/ur-2026-10-15/chance/chance_windfall_jackpot.png`。
- `manifest.json`：皮肤身份、原图与运行图片路径、尺寸、WebP 字节数及 SHA-256。
- `preview.json`：通过实际规则加载器及 `ContentCatalog.describe` 导出的默认卡面资料。
- `index.html`：使用真实 `ContentCard.vue`、UR 边框及项目样式的预览；从 Vite 开发服务的 `/@fs/<本文件绝对路径>` 打开。

两张原图均为 1254×1254，保留原 PNG，不裁切或缩放。运行图片仅作 WebP 编码压缩（quality 82 / method 6），位于 `src/web/public/images/cards/`，由 `card-illustrations.json` 绑定。图片随前端静态资源发布，不需要额外图床上传。卡名、故事、台词、等级和实际效果由原卡片组件渲染，未烘焙进图片。

题材参考：现实酒店爆破告别秀与模块化快速建楼。故事与人物为游戏改编，不复刻企业标志、真实人物或真实建筑。

已逐张查看生成原图，并通过本机 Vite 预览核对实际卡片组件中的插画、UR 边框和完整文字。已核对 350 个皮肤均有插画映射、新 WebP 可读取、尺寸及内容哈希；未执行前端 build 或线上发布。
