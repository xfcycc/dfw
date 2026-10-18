# 火影原作场景 UR 双卡插画

2026-10-18 使用 image_gen 为用户确认的两种新UR手牌生成独立插画。文案采用佩恩摧毁木叶、大和用木遁解决住宿的原作场景；图像采用原作人物与场景、精细3D动漫收藏玩具质感，不带文字、卡框或稀有度标识。

| 皮肤身份 | 名称 | 运行图片 | 尺寸 | WebP字节 |
| --- | --- | --- | --- | --- |
| card_naruto_shinra_tensei | 神罗天征 | https://pic.caiguoyu.cn/typora/20261018030759921.webp | 1254×1254 | 307610 |
| card_naruto_wood_houses | 木遁·连柱家之术 | https://pic.caiguoyu.cn/typora/20261018030759922.webp | 1254×1254 | 247234 |

生成原图保留在工具返回的用户图片目录，路径与SHA-256见manifest.json；仓库只保存URL和元数据。generation-briefs.json记录生成场景与画风说明，不是逐字工具调用转录。运行图等尺寸压缩为WebP，quality82、method6，不裁切、不缩放，经已有PicGo上传OSS。两张图片已查看，远端HTTP200、image/webp、大小及Content-MD5均与本地压缩文件一致。

插画通过skinId接入现有ContentCard，手牌、详情、抽卡、出牌及升级选择共享图片。未执行前端build、单元／冒烟测试或部署；效果实局与断线恢复仍需已有环境验收。
