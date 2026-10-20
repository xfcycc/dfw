# 2026-10-19 新增梗皮肤

按用户确认稿新增16张皮肤：机会3张、财富6张、道具7张，其中UR 5张。沿用「眉头一皱」Q版参考：约两头身、手绘赛璐璐、夸张表情、暖色、正方形无文字卡框。

| 卡池 | 新增皮肤 | 稀有度 | 原机制 | 图片 |
| --- | --- | --- | --- | --- |
| 机会 | 放课后茶会 | SSR | `concert` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190256119.webp) |
| 机会 | μ’s，开场！ | SSR | `concert` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190257635.webp) |
| 财富 | 黄金矿工 | SSR | `treasure` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190258712.webp) |
| 财富 | 宝箱怪 | SSR | `treasure` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190259871.webp) |
| 财富 | 大闹天宫 | UR | `global_demolition` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190302221.webp) |
| 财富 | 原来大家都有 | SR | `all_cards` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190305330.webp) |
| 财富 | 三顾茅庐 | UR | `talent` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190308035.webp) |
| 机会 | 乾坤大挪移 | SSR | `shuffle` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190311229.webp) |
| 财富 | 三只小猪 | UR | `full_upgrade` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190312500.webp) |
| 道具 | 神抽 | SSR | `remote` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190318459.webp) |
| 道具 | 修复如初 | SSR | `freeUpgrade` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190319575.webp) |
| 道具 | 神龙许愿 | UR | `fullUpgrade` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190320687.webp) |
| 道具 | 认真一拳 | UR | `fullDemolish` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190323674.webp) |
| 道具 | 三重罗生门 | SSR | `shield` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190326678.webp) |
| 道具 | 移花接木 | SR | `steal` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190327644.webp) |
| 道具 | 炉石回城 | SSR | `homeTeleport` | [OSS插画](https://pic.caiguoyu.cn/typora/20261019190328977.webp) |

## 素材与接入

- [确认文案与生成提示词](generation-briefs.json)：完整名称、台词、故事、出处、参考图与逐张提示词。
- [OSS素材清单](manifest.json)：16张完整卡图、5组UR透明人物与背景，共26张WebP；含原图绝对路径、尺寸、摘要、透明度及远端校验。
- [16张图片总览](index.html)：直接加载OSS图片，可展开台词、故事与效果说明。
- UR分层动画复用 `UrCardScene.vue`，配置见 `ur-card-scenes.ts`、资源见 `ur-card-art.json`；全部16张完整静态图绑定在 `card-illustrations.json`。
- [UR动态对比预览](http://127.0.0.1:5173/ur-preview.html?skin=fortune_wukong_heaven_riot)：本地启动Vite后可切换五张新增UR；复用真实ContentCard组件。素材加载失败或系统减少动态效果时保留完整静态图。

## UR 动态设计

| 皮肤 | 动作与特效 |
| --- | --- |
| 大闹天宫 | 悟空舞棒的前后摆动、天宫震颤、碎瓦飘落 |
| 三顾茅庐 | 登门人物微微作揖、礼物随人物摆动、晨光流动 |
| 三只小猪 | 小猪施工起伏、砖屋轻微升起、木屑上扬 |
| 神龙许愿 | 神龙随背景盘旋、人物争先许愿、龙珠金光脉冲 |
| 认真一拳 | 人物挥拳突进、城市震颤、冲击环扩散、碎石扬起 |

采用已有CSS/SVG分层动画方式，人物与背景独立运动；OSS保存静态WebP图层，动画由前端合成。

## 规则边界

本次只追加原完整效果下的皮肤。逐项对照任务开始时的6份规则JSON，移除本次16个皮肤后与原配置完全一致；机制、组、参数ID、数值、稀有度、权重、原皮肤及经济配置均未变。仍先按完整效果权重抽取，再在该效果中等概率选皮肤；单张旧皮肤在同机制内的出现占比会随皮肤增加而分摊。

人才引进源表保留基准5000与原参数身份，实际生活费由已有经济覆盖为2000；事件直升满级保留基准补偿400，默认新局乘倍率后800。护盾效果说明统一使用「防拆护盾」，避免新皮肤仍显示A.T.力场名称。

皮肤合计263张（机会96、财富69、道具98），财富再次行动12张继续停用，启用251张（机会96、财富57、道具98）。本次只增加追加前后两个明确默认规则摘要的存档兼容关系，不放宽任意外部配置；旧手牌保留冻结内容。

## 上传与验证

26张原图等尺寸转WebP（quality90、method6），不缩放、不裁切；5张人物层保留透明度，压缩前后Alpha逐像素一致。原图共62,116,815字节，WebP共9,528,224字节，约节省84.7%。通过既有本机PicGo上传到项目OSS，每张均核对HTTP200、image/webp、Content-Length和Content-MD5与本地一致。

前端类型检查、后端跳过测试编译、源表再生成逐字一致性和 `git diff --check` 均通过。浏览器总览16张OSS图片全部加载成功；实际ContentCard组件的五组UR均进入ready状态，逐张查看合成构图，暂停/继续控制正常，UR预览页未记录控制台错误。减少动态效果及失败回退检查了源码，未在浏览器模拟。详见[验证记录](verification.json)。未编写或执行单元测试、冒烟测试，未执行前端build，未提交、未部署。页面只检查素材与卡面展示，未进行真实对局验收。
