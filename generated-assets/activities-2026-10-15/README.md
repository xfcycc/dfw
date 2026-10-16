# 苏暖暖四角游园会素材

> 2026-10-16 云端迁移：本目录图片已压缩并上传 OSS，本地仅保留元数据、提示词和 URL。历史清单中的原路径与原图摘要用于追溯，当前文件地址、大小及摘要以 `generated-assets/cloud-images.json` 为准；下文原图保留位置及本地发布描述属于迁移前记录。

图片由图片生成工具参考既有苏暖暖原图生成，人物沿用粉色长卷发、蝴蝶结、粉白裙装与奶油色斜挎包。图片中的金额、玩家和规则全部由真实组件叠加。

- [_sources/backgrounds.png](https://pic.caiguoyu.cn/typora/20261016163512506.webp)：2列3行，五套专属场景与轮换广场。
- [_sources/landmarks.png](https://pic.caiguoyu.cn/typora/20261016163719329.webp)：3列2行，五套活动角地标与轮换装饰。
- [_sources/props.png](https://pic.caiguoyu.cn/typora/20261016163656945.webp)：4列4行，八匹马、轮盘两件道具、红包开合、三种手势与幸运图标。
- `atlases/`：马匹、轮盘、红包、猜拳四套独立可拆分PNG图集。
- `extracted/`：原图按固定网格提取的PNG，不改画面内容。
- `image-manifest.json`：28项运行WebP的来源、坐标、尺寸、大小和SHA-256。
- `prompts.json`：人物参考、画风与各图集生成指令记录。
- `audio-manifest.json`：16份原创WAV的时长与采样规格。

运行素材与图集均已上传 OSS，地址见 `src/web/image-assets.json` 和 `generated-assets/cloud-images.json`。`scripts/prepare-activity-art.py` 从云端图集重新提取并压缩，仅在系统临时目录生成待上传文件；`scripts/generate-activity-sounds.py` 只生成活动音效，不改原十三类音效。

已逐图集查看人物、场景、八匹马及猜拳手势。素材元数据检查和组件视觉预览不证明多人联机或随机结算正确；实际验证边界见 `docs/corner-activities.md`。
