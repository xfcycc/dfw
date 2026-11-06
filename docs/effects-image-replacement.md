# 特效图片云端替换记录

2026-11-07。沿用项目现有 PicGo／OSS 图床，将 11 张本地图集替换为运行云端资源；未部署。

## 处理与核验

- 原始 PNG 保留在本机原路径，仓库继续忽略图片二进制，只保存映射、来源和核验记录。
- 等尺寸转为 WebP，quality 90、method 6，不裁切、不缩放。11 张 alpha 通道均与原 PNG 完全一致。
- PNG 合计 26,179,795 字节，WebP 合计 8,039,990 字节。
- 通过既有 PicGo 上传到 pic.caiguoyu.cn；每张 GET 为 HTTP 200、image/webp，Content-Length、Content-MD5 与响应字节均和本地文件一致。
- 主体图集、原四张 UR、流萤分层、UR 分层的渲染与预加载均使用 image-assets.json + imageUrl，不依赖本机 public 中的 PNG。
- 331 张卡的逻辑身份、图集格子与动作配置保持不变。

## 资源映射

| 逻辑图片 | 尺寸 | WebP 字节 | 云端地址 |
| --- | --- | ---: | --- |
| `/images/full-card-spells/complex-0.png` | 1254×1254 | 898,068 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449833.webp) |
| `/images/full-card-spells/complex-1.png` | 1254×1254 | 824,398 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449834.webp) |
| `/images/full-card-spells/firefly-parts.png` | 1254×1254 | 307,654 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449835.webp) |
| `/images/full-card-spells/props-0.png` | 1354×1161 | 816,810 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449836.webp) |
| `/images/full-card-spells/props-1.png` | 1354×1161 | 836,402 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449837.webp) |
| `/images/full-card-spells/props-2.png` | 1354×1161 | 915,646 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449838.webp) |
| `/images/full-card-spells/props-3.png` | 1354×1161 | 794,980 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449839.webp) |
| `/images/full-card-spells/props-4.png` | 1354×1161 | 835,674 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449841.webp) |
| `/images/full-card-spells/props-5.png` | 1354×1161 | 692,498 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449842.webp) |
| `/images/full-card-spells/ur-parts.png` | 1254×1254 | 812,784 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449843.webp) |
| `/images/ur-spell-preview/spell-atlas.png` | 1254×1254 | 305,076 | [OSS WebP](https://pic.caiguoyu.cn/typora/20261107010449844.webp) |

完整摘要、HTTP 与 MD5 核验记录见 [oss-assets.json](../src/web/public/images/full-card-spells/oss-assets.json)。

## 验证范围

执行前端类型检查、映射与定向 diff 检查；浏览器分别查看神威、筋斗云和点燃大海，确认原四张 UR 图集、普通道具图集和流萤分层的云端加载与播放，未发现底色或错格。此次页面复查覆盖三类代表，不等于重新逐张验收全卡池。未执行前端 build、单元测试或冒烟测试，未部署。
