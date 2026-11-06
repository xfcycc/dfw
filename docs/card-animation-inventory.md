# 全卡池动画与素材梳理清单

> 核对日期：2026-11-06。此文件为设计建议，不是实现或上线清单。只新增本清单，不修改运行代码、卡池、素材或规则。

> 当前实现进度：331 张已建立逐卡主体、动作配置、全卡池预览和本地实战公共结果接入。新增 8 张透明图集映射 327 张，复用既有四张 UR 主体。原表的“待制作”和历史统计保留为设计快照；实际交付与检查边界见 [全卡池招式实现](./full-card-spells.md)，不表示 331 张已逐张完成运行验收。

> 后续进度：建议的第二批 12 张已加入独立预览，现共 22 张样板（18 非 UR、4 UR），剩余 309 张。详见 [第二批素材与动作记录](./regular-spell-batch2.md)。下方统计与逐卡进度保留首次梳理快照，不能视为实时完成情况。

## 统计口径

- 按三份运行配置 `content.mechanisms` 中 `enabled=true` 的全部 `groups → parameters → skins` 展开，一张稳定 `skinId` 算一张卡面。规则与 `ContentCatalog.candidates()` 一致。
- 当前共 **331 张：道具 160、机会 103、财富 68**；UR 48 张，非 UR 283 张。此前样板 10 张仅完成独立预览，本清单保留其行并标注“已有预览”，其余 **321 张**均为待设计／制作。
- 包含机会与财富，避免只覆盖可主动使用的道具。停用机制不列入制作清单。未按旧文档的历史统计或文件名推断当前题材。
- 卡名、稀有度、机制与稳定身份是当前配置事实；动作、素材等级、优先级是本次设计建议。这里不重复定义卡牌数值或结算规则。

| 卡池 | 卡面总数 | R | SR | SSR | UR | 已有预览 | 剩余建议 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 道具 | 160 | 23 | 48 | 56 | 33 | 10 | 150 |
| 机会 | 103 | 16 | 43 | 40 | 4 | 0 | 103 |
| 财富 | 68 | 11 | 19 | 27 | 11 | 0 | 68 |

## 素材分级与复用原则

| 等级 | 判断标准 | 工作内容 |
| --- | --- | --- |
| A | 现有主体或基础几何图形足够表达 | 复用卡背、棋子、账单、粒子、已有特效，补动作组合或简单线条；不等于零代码或已接入 |
| B | 需要独特道具，但轮廓简单 | 新绘少量 SVG 图形，如门、鞋、符纸、木桩；动作仍复用，不制作角色动画 |
| C | 外形辨识度依赖复杂轮廓、纹样、怪物或特定武器 | 准备透明 PNG／WebP 或少量分层图；继续复用动作，只有标明独立编排的招式需要额外阶段设计 |

A/B/C 按最终素材需求分级，已有样板仍记录其原素材等级。下表“剩余”已经扣除 10 张样板；C 的卡面数不是需要新生成的图片数，同题材可共享套件。

| 素材级别 | 全部卡面 | 已有预览 | 剩余卡面 | 其中非 UR | 其中 UR |
| --- | ---: | ---: | ---: | ---: | ---: |
| A · 复用现有／基础图形 | 47 | 0 | 47 | 44 | 3 |
| B · 新增简单 SVG | 193 | 6 | 187 | 172 | 15 |
| C · 专属透明图片 | 91 | 4 | 87 | 61 | 26 |

## 动作目录

以下编号是清单中的复用方向，不代表已经存在 16 个独立可配置引擎。T01～T10 有样板动作依据，正式复用还需按来源身份、真实目标和队列整理；其余先复用项目现有能力，再补缺少的动作。

| 编号 | 动作族 | 当前依据／需要补的部分 |
| --- | --- | --- |
| T01 | 坠落、放置、回弹 | 水泥墩、苦无样板；门、陨石、路障可共用落下动作 |
| T02 | 射线、投掷、命中 | 迟缓样板；投掷物需补弧线，射线保留不同形状与命中反应 |
| T03 | 抓取、递送、转移 | 顺手牵羊样板；礼物、钱袋、卡背共用起终点运动 |
| T04 | 拼装、生长、重构 | 修复如初样板；按木构、砖块、机械件替换主体，升起顺序不同 |
| T05 | 切割、核销、减免 | 帮我砍一刀样板；账单保留，剪刀、券章、时间倒转各有命中方式 |
| T06 | 显隐、瞬闪、换位 | 飞雷神样板；单人传送、双方换位和多人错位分别绑定真实数据 |
| T07 | 旋转、吸入、收束 | 神威样板与现有黑洞；保留旋涡／引力核的外形差异 |
| T08 | 实体突进、冲击、碎屑 | 认真一拳样板；巨掌、炸弹、斥力需不同方向与节奏 |
| T09 | 散开、环绕、汇聚 | 千本樱样板；花瓣、纸片、光点可换粒子轮廓与轨迹 |
| T10 | 虚影显现、双方连线 | 死神样板；虚影不是固定死神，神龙、神君各用专属主体 |
| T11 | 小道具揭示、盖章、轻提示 | 现有揭卡与余额提示为基础；补账单、信件等轻量动效，不统一套大招等待 |
| T12 | 光壁展开、附着、受击、解除 | 现有盾图集与样板印记可参考；需要补缎带、钟罩、披风等轮廓与受击反馈 |
| T13 | 挥砍、敲击、连续斩 | 需要新增；棒、锤、剑复用转轴和刀光，武器外形分别制作 |
| T14 | 路径飞行、后退、回溯 | 复用既有移动路径；补云、载具、倒放残影，禁止重新计算移动 |
| T15 | 范围领域、波纹、环境变化 | 需从现有多目标定位扩展；ROOM、封绝、冰域保持不同边界形状 |
| T16 | 环绕、蓄力、变形准备 | 已有硬币、旋涡等动作可参考；补钟盘、晶体、金丹等阶段节奏 |

默认表现：普通卡 0.7～1.9 秒局部反馈，少数重点非 UR 可到约 2.2 秒；招牌 UR 约 3～4 秒。现金和费用类不受该时长下限约束，优先只在揭卡／金额旁轻提示，沿用现有 `income/fee/payment` 零等待。

## 共享素材套件：先按题材合并采购／绘制

| 套件 | 共用卡牌 | 共用内容与保留差异 |
| --- | --- | --- |
| 飞雷神 | 道具与机会中的飞雷神之术 | 同一苦无和术式；道具交换两人，机会只接实际前进 |
| 神龙 | 道具神龙许愿、机会神龙许愿 | 同一神龙和龙珠；升级与全员现金分开编排 |
| 克洛格 | 呀哈哈、又一个呀哈哈 | 同一叶面精灵、石头、种子；各自展示真实收益 |
| 大红藏品 | 摸出非洲之星、九格大红、开箱出大红、下把猛攻 | 同一搜索框和红光，具体藏品轮廓按题材；现金与补给不混用 |
| 刻刻帝 | 七之弹、刻刻帝·四之弹 | 共用钟盘和枪口；前者停摆，后者逆转 |
| 枫丹审判 | 谕示裁定、水神的审判 | 共用天平、法槌、聚光；实际目标不同 |
| 黑白熊 | 黑白熊登场、学级裁判 | 共用广播头像；校门关闭与言弹裁判分别编排 |
| 口袋 | 四次元口袋、万能口袋、掏出秘密道具、道具大派送 | 同一袋口；递给一人、定向候选、全员分发不能混为同一事件 |
| 金云 | 筋斗云、腾云驾雾 | 同一云形，路径完全取实际移动事实 |
| 金箍棒 | 如意金箍棒、大闹天宫、千万别出这个圈 | 同一棒身；分别变长击打、横扫瓦片、画圈防护 |
| 账单／剪刀／券 | 帮我砍一刀、团购、砍一刀成功、这刀真砍成了及费用事件 | 纸张与剪刀共用；现金奖励不误演为减费，未支付不能盖已付章 |
| 建造零件 | 修复如初、基建狂魔、连柱家之术、改造计划等 | 砖块／木梁／机械件三套；主题道具和生成次序不同 |
| 卡背／包裹 | 顺手牵羊、制衡、手牌抹杀、卡包与全员补给 | 共用卡背，隐藏真实牌面和他人手牌数量 |
| 魔杖 | 修复如初、昏昏倒地 | 基础魔杖可共用，修楼与红光命中不同；小樱鸟首杖另做专属套件 |

现有 UR 动态卡面的分层图片只作为候选来源，不视为已经适合棋盘招式。尤其白云无尽、尘歌壶等需先看实际分层和裁切质量，再决定复用；已有卡面动画不能写成已有使用动画。

## 建议制作顺序

1. **先扩动作覆盖**：任意门、竹蜻蜓、筋斗云、金蝉脱壳、团购、闷棍、A.T.力场、蓝银缠绕、替身术、ROOM、如意金箍棒、踩到香蕉皮。优先 A/B，验证门、载具、光壁、缠绕、挥砍及后退动作。
2. **再做高复用专属套件**：神龙、刻刻帝、枫丹审判、克洛格、黑白熊、大红藏品；一套素材服务多张卡，但保持不同规则结果。
3. **最后做单卡高辨识大招**：三重罗生门、神君、猩红腐败、双龙魔影剑、恩佐斯之怒、女娲补天等。复杂形象用透明图片，避免用大量路径重画。
4. **现金／费用轻提示单独收口**：不挤入前述棋盘大招队列，先保留现有零等待；需要时在揭卡界面补对应小道具。

## 接入时必须保留的规则边界

- 稳定 `skinId` 决定题材；实际目标、位移、金额、状态和结果来自服务端事实。当前 `BoardEffect` 没有专门的来源皮肤字段，正式接入前需整理来源身份传递，不能按日志文字猜卡牌。
- 不凭动画改变目标数量、造成额外位移或伤害。卡池描述讲故事，机制字段决定效果；例如机会“神龙许愿”不是全城升级，“复活甲等队友”不是玩家复活。
- 金额型目前不等待特效，费用尚未支付时不得显示已付；中奖或候选不在前端二次随机。
- 私人牌面只向持有人揭示，公共转移使用固定数量的装饰卡背；不要通过卡背数或粒子数泄露他人真实手牌数量。
- 状态卡区分获得、触发／消耗、解除；首次可完整展示，后续短反馈，不重播整段大招。
- 只保留招式必需的生物／虚影，如死神、神龙、神君。其他题材优先道具、武器、印记；无需为每张卡制作角色动作。
- 文案与历史 ID 不一致时按当前描述：花花草草也是生命不是投石车、我买下了银行不是钢铁侠、关不住的鸟不是宝可梦绳索、千万别出这个圈不是漫威传送门。

## 逐卡清单

“已有预览”只表示本聊天完成的 10 张独立样板；“待制作”不代表完全没有通用特效，只表示尚未完成本行建议的专属使用表现。每行都保留稳定身份，方便检索和后续逐项实施。

### 道具卡 · 160 张

| 序号 | 卡名 | 稀有度 | 稳定 skinId | 当前机制 | 素材 | 复用动作 | 辨识元素与演出建议 | 进度 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 任意门 | SSR | `card_dora_anywhere_door` | `remote` | B | T01+T06 | 粉红门框与门扇；开门、棋子穿过、关门，落点用真实移动路径 | 待制作 |
| 2 | 神抽 | SSR | `card_yugioh_destiny_draw` | `remote` | A | T03+T11 | 通用卡背从牌堆抽出并横划；光落在已选点数，不误演为新增手牌 | 待制作 |
| 3 | 这就是「空白」 | SSR | `card_anime_g17` | `remote` | B | T11+T14 | 棋盘网格与棋子推演线；候选路线收束到实际点数 | 待制作 |
| 4 | 时间停止 | SSR | `card_jojo_time_stop` | `move` | B | T15+T14 | 停摆钟盘、冻结飞刀；短暂停色后恢复，仅表现当前格重结算 | 待制作 |
| 5 | 月光宝盒 | SSR | `card_moon_box` | `move` | C | T06+T14 | 带纹样的月光宝盒；盒盖开启、景物倒卷、回到当前格 | 待制作 |
| 6 | 舞空术 | R | `card_dragonball_fly_air` | `move` | A | T14 | 棋子副本浮起、气流托举并按实际路径前进 | 待制作 |
| 7 | 撤离 | R | `card_delta_extract` | `move` | B | T11+T14 | 撤离烟柱、背包图标；信号亮起后快速离开 | 待制作 |
| 8 | 末影珍珠 | R | `card_minecraft_ender_pearl` | `move` | B | T02+T06 | 方块风末影珍珠；弧线投出、紫粒子散开、棋子抵达 | 待制作 |
| 9 | 翔 | R | `card_anime_g13` | `move` | C | T14 | 带白色羽翼的封印杖；展开羽翼、载起棋子，不画小樱 | 待制作 |
| 10 | 瞬步 | R | `card_naruto_shunshin` | `move` | A | T06+T14 | 短残影、衣角速度线；两次显隐接真实移动 | 待制作 |
| 11 | 竹蜻蜓 | R | `card_dora_takecopter` | `move` | B | T16+T14 | 竹蜻蜓；桨叶旋转后托起棋子 | 待制作 |
| 12 | 闪现向前 | R | `card_flash_summoner` | `move` | B | T06+T09 | 金色闪现爆点与紫红羽毛；瞬闪后羽毛收拢 | 待制作 |
| 13 | 筋斗云 | R | `card_journey_kite_cloud` | `move` | B | T14 | 卷曲金云；托起棋子、翻滚后落地 | 待制作 |
| 14 | 全速前进 | R | `card_trisolaris_curvature` | `move` | C | T14 | 自然选择号的简化舰影；引擎亮起、星线拉长，不画舰桥人物 | 待制作 |
| 15 | 眉头一皱 | R | `card_tactic_retreat_best` | `move` | B | T06+T14 | 遁符；悄悄后退半步再闪走，保留谨慎撤退的节奏 | 待制作 |
| 16 | 倒车入库 | R | `card_meme_reverse_parking` | `move` | B | T14 | 小车与倒车箭头；两次小调整后倒入落点 | 待制作 |
| 17 | 时空断裂 | R | `card_starcraft_warp` | `move` | C | T14 | 时间装置轮廓与蓝色时间残影；沿已知视觉轨迹倒放 | 待制作 |
| 18 | 定住七仙女 | SSR | `card_journey_immobilize` | `barrier` | B | T02+T12 | 定字印与桃子小图形；指向光束后定字盖在目标格 | 待制作 |
| 19 | 提莫必须死 | SSR | `card_lol_teemo_mushroom` | `barrier` | C | T01 | 提莫特色蘑菇；落地弹起、缩进地面，后续踩中才释放烟团 | 待制作 |
| 20 | 草丛三兄弟 | SSR | `card_yugioh_trap_card` | `barrier` | B | T01+T08 | 草丛及剑、长枪、旗枪三个轮廓；草一抖三件武器探出，不画三名角色 | 待制作 |
| 21 | 师傅别填 | SSR | `card_minecraft_dig_hole` | `barrier` | B | T01+T04 | 坑口、土堆与小铲；土落入坑中后留下截停标记 | 待制作 |
| 22 | 昏昏倒地 | SSR | `card_hp_obstacle_charm` | `barrier` | A | T02 | 复用魔杖，红光击中后目标副本倾倒；不增加实际击退 | 待制作 |
| 23 | 实心水泥墩 | SSR | `card_meme_concrete_block` | `barrier` | B | T01 | 沿用水泥墩样板：实心路障落地、压缩回弹、扬尘 | 已有预览 |
| 24 | 走不动道 | SSR | `card_starcraft_spider_mine` | `barrier` | B | T12 | 黏住鞋底的心形贴纸与脚印；脚步停住，不画路过人物 | 待制作 |
| 25 | 七之弹 | SSR | `card_anime_g16` | `barrier` | C | T02+T15 | 刻刻帝钟盘与枪口剪影；弹丸携钟纹命中、指针停摆 | 待制作 |
| 26 | 禁止通行 | SSR | `card_anime_g22` | `barrier` | A | T08+T01 | 沿地面推进的冲击线与碎石墙；到目标格立起并截停 | 待制作 |
| 27 | 如来神掌 | SR | `card_kungfu_buddha_palm` | `demolish` | B | T08 | 巨大掌印；从上方压下，停顿后扩出掌形冲击 | 待制作 |
| 28 | 如意金箍棒 | SR | `card_journey_golden_staff` | `demolish` | B | T13 | 金箍棒；由针变长、横扫后收回，不画悟空 | 待制作 |
| 29 | 专业装修队 | SR | `card_meme_demolition_crew` | `demolish` | B | T13+T04 | 施工锤与歪斜工具箱；两次敲击后目标建筑降级 | 待制作 |
| 30 | 沙漠神殿压力板 | SR | `card_minecraft_tnt` | `demolish` | B | T01+T08 | 方块压力板和 TNT 方块；下压、引线亮起、方块碎屑散开 | 待制作 |
| 31 | 花花草草也是生命 | SR | `card_civ6_catapult` | `demolish` | B | T11+T13 | 花草、抛出的物件和说教气泡；物件落到目标，不能沿历史 ID 使用投石车 | 待制作 |
| 32 | 风之伤 | SR | `card_anime_g23` | `demolish` | C | T13+T08 | 铁碎牙巨刃与分叉金色裂地斩；沿目标方向推出 | 待制作 |
| 33 | 金蝉脱壳 | SR | `card_tactic_golden_cicada` | `swap` | B | T06 | 空外袍；棋子消失后袍子落下，另一位置现身 | 待制作 |
| 34 | 道友请留步 | SR | `card_tactic_lure_tiger` | `swap` | B | T03+T06 | 道袍袖口与挽留手势；伸手拉住后双方换位，不画申公豹 | 待制作 |
| 35 | 移形换位 | SR | `card_hp_apparition` | `swap` | B | T06 | 复仇之魂风格的双向紫翼光痕；同步交换两端，不套哈利波特门 | 待制作 |
| 36 | 我们真的回不去了吗 | SR | `card_dora_time_machine` | `swap` | B | T11+T06 | 两只聊天气泡与删改光标；气泡交错、棋子换位，不使用时光机 | 待制作 |
| 37 | 飞雷神之术 | SR | `card_naruto_flying_thunder` | `swap` | B | T01+T06 | 沿用三叉苦无样板；标记、闪光、交换位置 | 已有预览 |
| 38 | 空城计 | SSR | `card_sgyy_empty_city` | `immunity` | B | T01+T12 | 敞开的城门与古琴；琴弦轻动、来袭费用标记退开 | 待制作 |
| 39 | 草船借箭 | SSR | `card_sgyy_borrow_arrows` | `immunity` | C | T12+T03 | 带草垛的小船；雾中接箭，箭留在船侧，不生成收益 | 待制作 |
| 40 | 隐形衣 | SSR | `card_hp_invisibility_cloak` | `immunity` | C | T12 | 星纹隐形披风；覆盖目标再淡去，保留轮廓而不隐藏必要状态 | 待制作 |
| 41 | 掩耳盗铃 | SSR | `card_jjk_domain_expansion` | `immunity` | B | T12+T11 | 铃铛与捂耳手势符号；铃声波纹被罩住，费用取消 | 待制作 |
| 42 | 我能打一整天 | SSR | `card_zelda_shield_parry` | `immunity` | B | T12 | 红白蓝圆盾；迎击、弹开费用标记、盾面轻震 | 待制作 |
| 43 | 金身等死 | SSR | `card_lol_zhonya` | `immunity` | B | T12+T16 | 沙漏与棋子金色剪影；瞬间镀金、短暂停住再恢复 | 待制作 |
| 44 | 马什么梅 | SSR | `card_bullet_flying_play_dumb` | `immunity` | B | T11+T12 | 马／什么／梅三个问号气泡；逐次打断费用提示，不画人物 | 待制作 |
| 45 | 风王结界 | SSR | `card_anime_g03` | `immunity` | A | T12+T16 | 透明剑轮廓与旋风；绕剑风环弹开来袭效果 | 待制作 |
| 46 | 存在感消失 | SSR | `card_anime_g24` | `immunity` | A | T12 | 棋子视觉副本渐隐、费用标记扑空；真实玩家标识仍保留 | 待制作 |
| 47 | 换个颜色再打 | R | `card_wukong_great_sage` | `bonus` | B | T16+T14 | 额前菱形水晶与紫色轮廓；晶体闪亮后轻快加速，不画迪迦全身 | 待制作 |
| 48 | 五速鞋游走 | R | `card_lol_swiftness_boots` | `bonus` | B | T14 | 五速鞋图标；鞋底冒风、短脚印接实际移动 | 待制作 |
| 49 | 你最好现在就跑 | R | `card_wow_sprint` | `bonus` | B | T14 | 斧头剪影与疾跑尾迹；尾迹追来提示加速，不演攻击结算 | 待制作 |
| 50 | 一米六怎么了 | R | `card_aot_odm_gear` | `bonus` | C | T14+T13 | 立体机动装置与双刃；钢索牵引、弧线掠过后收刀 | 待制作 |
| 51 | 超频加速 | R | `card_anime_g15` | `bonus` | C | T16+T14 | 银色机械羽翼；青蓝网格压静、翅膀展开后加速 | 待制作 |
| 52 | 靖妖傩舞 | R | `card_anime_g29` | `bonus` | C | T16+T14 | 降魔傩面与长枪尖；青风环绕后起落，不增加伤害表现 | 待制作 |
| 53 | SUPER！ | SR | `card_onepiece_shipwright` | `freeUpgrade` | B | T04+T16 | 可乐罐、机械工具与星形标志；补充可乐后工具拼装 | 待制作 |
| 54 | 改车消星 | SR | `card_wow_goblin_engineer` | `freeUpgrade` | B | T04+T11 | 卷帘门与喷漆罐；遮住建筑、喷漆、卷门升起显出升级 | 待制作 |
| 55 | 多重影分身 | SR | `card_naruto_shadow_clone` | `freeUpgrade` | A | T09+T04 | 现有棋子副本分成几团烟影共同施工；不单独制作鸣人人物 | 待制作 |
| 56 | 熹妃回宫 | SR | `card_zhenhuan_promotion` | `freeUpgrade` | C | T01+T04 | 宫门与仪仗扇轮廓；宫门打开、建筑层级抬升，不做人形队列 | 待制作 |
| 57 | 一步一步走到最高 | SR | `card_eldenring_blessing` | `freeUpgrade` | B | T04 | 台阶与高座剪影；台阶依次升起，最后一阶接建筑升级 | 待制作 |
| 58 | 修复如初 | SR | `card_hp_reparo` | `freeUpgrade` | B | T04 | 沿用魔杖样板；砖块、窗框拼合，建筑升级 | 已有预览 |
| 59 | 团购 | SR | `card_meme_groupon` | `halfPayment` | B | T05+T11 | 手机团购券；券展开、核销盖章、账单金额减半 | 待制作 |
| 60 | 帮我砍一刀 | SR | `card_meme_pdd_cut` | `halfPayment` | B | T05 | 沿用剪刀切账单样板；一半飘走，剩余金额落定 | 已有预览 |
| 61 | 及时雨 | SR | `card_watermargin_songjiang` | `halfPayment` | B | T03+T05 | 银袋与酒碗；银袋推至账单旁，抵去一半费用 | 待制作 |
| 62 | 舌战群儒 | SR | `card_sgyy_debate` | `halfPayment` | B | T11+T05 | 羽扇与辩论气泡；气泡依次被压下，账单折半 | 待制作 |
| 63 | 时间宝石 | SR | `card_avengers_time_stone` | `halfPayment` | C | T14+T05 | 时间宝石与绿色时间印；费用数字倒转后停在减半结果 | 待制作 |
| 64 | 真相只有一个 | SR | `card_conan_reasoning` | `halfPayment` | C | T11+T05 | 麻醉手表和蝴蝶结变声器；证据光线指向账单并划去一半 | 待制作 |
| 65 | 踹牢门 | SR | `card_gintama_break_jail` | `release` | B | T13+T12 | 拖鞋和牢门锁；一脚踢开锁，拖鞋飞远，控制标记解除 | 待制作 |
| 66 | 月下的魔术师 | SR | `card_sekiro_escape` | `release` | C | T09+T12 | 白礼帽、披风角与鸽子；白鸽飞散，限制标记消失 | 待制作 |
| 67 | 关不住的鸟 | SR | `card_pokemon_escape_rope` | `release` | B | T12 | 雨线与打开的隧道口；雨水冲掉锁链，不使用宝可梦绳索 | 待制作 |
| 68 | 穿透环乌龙 | SR | `card_dora_pass_ring` | `release` | B | T01+T06 | 橙色穿透环；贴住限制边界、开洞、穿过后收环 | 待制作 |
| 69 | 多比是自由的 | SR | `card_hp_portkey` | `release` | B | T06+T12 | 袜子信物与幻影移形旋痕；锁链脱落后短闪，不画多比 | 待制作 |
| 70 | 约定的钥匙 | SR | `card_anime_g04` | `release` | B | T12 | 约定钥匙与锁；钥匙转动、锁开裂并散去 | 待制作 |
| 71 | 顺手牵羊 | SR | `card_watermargin_shiqian` | `steal` | B | T03 | 沿用伸手取牌样板；只夹固定通用卡背 | 已有预览 |
| 72 | 乡绅的钱如数奉还 | SR | `card_bullet_flying_rob` | `steal` | B | T03 | 两只钱袋和分账签；先返还再收取的节奏，只表现实际手牌转移 | 待制作 |
| 73 | 以彼之道，还施彼身 | SR | `card_idiom_empty_hand` | `steal` | B | T03+T06 | 袖口与折返弧线；卡背绕袖一圈后转向施术者 | 待制作 |
| 74 | 潜行100 | SR | `card_stardew_trash_dive` | `steal` | B | T01+T03 | 木桶；罩住目标视觉副本上方，再从旁侧取走卡背 | 待制作 |
| 75 | 东西我收下了 | SR | `card_gintama_yorozuya` | `steal` | B | T03+T14 | 小车与告别手势；卡背装进车里驶回，不画鲁邦 | 待制作 |
| 76 | 移花接木 | SR | `card_wuxia_sleight_hand` | `steal` | B | T03 | 锦囊、小花与袖口；交换道具轮廓，卡背收入袖中 | 待制作 |
| 77 | 虚空抽取 | SR | `card_anime_g07` | `steal` | C | T03+T04 | 虚空巨剑轮廓；光中抽出剑影，映射为卡背转移，不显示私牌 | 待制作 |
| 78 | 偷窃 | SR | `card_anime_g30` | `steal` | A | T03+T11 | 复用手掌与卡背；先空手一抓、再掌心显物，区别于长手伸取 | 待制作 |
| 79 | 十连三金 | SR | `card_meme_eu_king` | `cash` | A | T11 | 固定三个金光记号闪过、余额更新；不暗示实际发放三张卡 | 待制作 |
| 80 | 摸出非洲之星 | SR | `card_delta_sell_gold` | `cash` | C | T11 | 非洲之星红色藏品小图；搜索框揭示，轻提示到账 | 待制作 |
| 81 | 我买下了银行 | SR | `card_avengers_ironman_money` | `cash` | B | T11 | 银行印章与产权文件；收购章盖下后到账，不使用钢铁侠装甲 | 待制作 |
| 82 | 顶金币 | R | `card_mario_coin` | `cash` | B | T01+T11 | 问号砖与金币；砖被顶起、金币弹出，非阻塞到账提示 | 待制作 |
| 83 | 开宝箱 | R | `card_genshin_chest` | `cash` | A | T11 | 现有宝箱素材轻开盖、金币闪出，不新增全屏演出 | 待制作 |
| 84 | 黄金时代的错觉 | R | `card_civ6_golden_age` | `cash` | B | T11 | 聊天气泡与向上行情箭头；轻提示到账，不制作文明建筑 | 待制作 |
| 85 | 拍卖会捡漏 | R | `card_wow_auction` | `cash` | B | T11 | 拍卖槌与蒙尘拍品；落槌、擦亮一角后到账 | 待制作 |
| 86 | 命运硬币 | UR | `card_fate_coin` | `gamble` | B | T16+T11 | 命运硬币正反面；翻转后按已确定结果显示圣光或落雷，不重新随机 | 待制作 |
| 87 | 孤注一掷 | UR | `card_all_in_ticket` | `gamble` | B | T11+T15 | 高薪海报、紧闭大门和票券；聚光后揭示已确定结果，不画主持人 | 待制作 |
| 88 | 爆破鬼才 | SSR | `card_ziggs_blast` | `blast` | C | T02+T08 | 吉格斯标志性炸弹；投掷、引线闪烁、爆炸，复用冲击环 | 待制作 |
| 89 | 谕示裁定 | SSR | `card_oratrice_verdict` | `jail` | C | T11+T12 | 枫丹审判天平和法槌；聚光锁定目标、裁定印落下 | 待制作 |
| 90 | 黑白熊登场 | SSR | `card_anime_g14` | `jail` | C | T10+T12 | 黑白熊广播屏头像；屏幕亮起、校门关闭、控制印记落下 | 待制作 |
| 91 | 神罗天征 | UR | `card_naruto_shinra_tensei` | `fullDemolish` | A | T08+T15 | 同心斥力波与碎屑；从中心向外推开建筑副本，独立范围编排 | 待制作 |
| 92 | 认真一拳 | UR | `card_onepunch_serious_punch` | `fullDemolish` | C | T08 | 沿用认真一拳透明拳套素材与命中时间轴 | 已有预览 |
| 93 | 连柱家之术 | UR | `card_naruto_wood_houses` | `fullUpgrade` | B | T04 | 木柱、横梁、木屋模块；多处依次破土成形，复用拼装动作 | 待制作 |
| 94 | 神龙许愿 | UR | `card_dragonball_shenron_wish` | `fullUpgrade` | C | T10+T16 | 盘曲神龙和七颗龙珠；定姿显现、龙珠聚光、落到实际升级目标 | 待制作 |
| 95 | A.T.力场 | SSR | `card_eva_at_field` | `shield` | B | T12 | 层叠橙色八边形光壁；前后层错时展开，受击时局部涟漪 | 待制作 |
| 96 | 缎带结界 | SSR | `card_anime_g02` | `shield` | B | T10+T12 | 金色缎带；从边缘交织成护盾，再收成持续标记 | 待制作 |
| 97 | 三重罗生门 | UR | `card_naruto_triple_rashomon` | `shield` | C | T01+T12 | 三道鬼面罗生门；可复用门基底，逐道升起和承击 | 待制作 |
| 98 | 制衡 | SR | `card_sgs_zhiheng` | `recycle` | B | T03+T11 | 奏报卷轴；旧卷移走、新卷滑入，私人卡仅用通用背面 | 待制作 |
| 99 | UMR上线 | SR | `card_anime_g10` | `recycle` | B | T11 | 街机按键与 UMR 记分牌；按键连击后刷新卡背 | 待制作 |
| 100 | 从零开始 | SR | `card_anime_g25` | `recycle` | B | T11 | 归零刻度与重新亮起的脚印；旧卡淡去、新卡出现，不演死亡回归 | 待制作 |
| 101 | 发现 | SSR | `card_hs_discover` | `discover` | A | T11 | 现有发现卡背与聚光；只向本人揭示候选，公共端不展示选项 | 待制作 |
| 102 | 闷棍 | SR | `card_wow_sap` | `sap` | B | T13+T12 | 短木棍与绕头星星；从背后敲一下、星星环绕目标 | 待制作 |
| 103 | 等价交换 | UR | `card_fma_exchange` | `exchange` | C | T04+T15 | 炼成阵、蓝色电弧与剑轮廓；阵亮后建筑副本分解重组，独立编排 | 待制作 |
| 104 | 迟缓光线 | R | `card_onepiece_slow` | `slow` | B | T02 | 沿用粉色环形光波与迟滞残影样板 | 已有预览 |
| 105 | 斗转星移 | SSR | `card_tianlong_star_shift` | `swapAny` | A | T06+T16 | 两端星点沿圆弧交错，棋子换位；不复用黑洞主体 | 待制作 |
| 106 | 红线续缘 | SSR | `card_anime_g19` | `swapAny` | B | T10+T06 | 红线结与天书小图；红线牵起双方后交换位置 | 待制作 |
| 107 | 神威 | UR | `card_naruto_kamui` | `kamui` | C | T07 | 沿用神威旋涡透明素材、建筑卷入与封印样板 | 已有预览 |
| 108 | 封绝 | UR | `card_anime_g08` | `kamui` | C | T15+T12 | 赤红封绝纹章与薄壁；区域静止并落封印，不使用神威旋涡 | 待制作 |
| 109 | 手牌抹杀 | SSR | `card_yugioh_hand_destruction` | `handDestruction` | A | T09+T11 | 固定卡背碎成光片再聚拢；不按各人真实手牌数量生成粒子 | 待制作 |
| 110 | 地爆天星 | SSR | `card_naruto_chibaku_tensei` | `gravity` | B | T07+T01 | 黑色引力核与碎石；先吸碎石，再汇聚真实受影响棋子 | 待制作 |
| 111 | 尸鬼封尽 | UR | `card_naruto_reaper_seal` | `reaper` | C | T10+T12 | 沿用死神透明素材、光带和双方封印样板 | 已有预览 |
| 112 | 暗影步 | SR | `card_wow_shadowstep` | `shadowstep` | A | T06 | 脚边黑影展开、棋子沉入并在实际目的地显现 | 待制作 |
| 113 | 传送 | SSR | `card_lol_teleport` | `homeTeleport` | B | T06+T16 | 紫色传送柱与旋转符文；落点先亮，再接棋子传送 | 待制作 |
| 114 | 炉石回城 | SSR | `card_wow_hearthstone_home` | `homeTeleport` | C | T16+T06 | 带螺旋纹的炉石；掌中蓄光、旅店暖光显现 | 待制作 |
| 115 | 赐福指引 | SSR | `card_anime_g01` | `homeTeleport` | B | T14+T11 | 金色赐福丝线；先指出目的地，再接实际传送 | 待制作 |
| 116 | 转移水晶 | SSR | `card_anime_g06` | `homeTeleport` | B | T16+T06 | 蓝色棱晶；举起发光、碎成光粒、目的地聚合 | 待制作 |
| 117 | 黑洞 | SSR | `card_original_blackhole` | `blackhole` | A | T07 | 沿用现有黑洞图集，仅吸走实际清除的附加标记，不吞建筑 | 待制作 |
| 118 | 陨星 | SSR | `card_naruto_meteor` | `meteor` | A | T01+T08 | 沿用现有陨星图集；阴影、落下、冲击，留下实际截停层数 | 待制作 |
| 119 | 时间溯行 | SSR | `card_anime_u01` | `timeRewind` | C | T14 | 圆盾沙漏；翻转沙漏、时间残影倒流，只展示实际回溯结果 | 待制作 |
| 120 | 投影魔术 | UR | `card_anime_u02` | `projection` | C | T04+T16 | 专属武器轮廓和魔术回路线；描边、填实、显出投影结果 | 待制作 |
| 121 | 星爆气流斩 | SSR | `card_anime_u03` | `starburst` | C | T13 | 阐释者与逐暗者双剑；交叉连续斩击，复用斩击轨迹 | 待制作 |
| 122 | 幻想杀手 | UR | `card_anime_u04` | `imagineBreaker` | B | T08+T12 | 右手轮廓与术式碎片；触碰点裂开，异能光纹消散 | 待制作 |
| 123 | 刻刻帝·四之弹 | SSR | `card_anime_u06` | `timeRestore` | C | T02+T14 | 复用刻刻帝钟盘枪口套件；命中后指针逆转，伤损视觉复原 | 待制作 |
| 124 | 矢量反射 | UR | `card_anime_u07` | `vectorReflect` | B | T02+T12 | 带方向箭头的来袭轨迹；触体折返，不能只画通用护盾 | 待制作 |
| 125 | 死亡回归 | UR | `card_anime_u08` | `deathReturn` | C | T14+T15 | 暗紫手影与重复场景轮廓；短暂暗场后回到事实快照，独立编排 | 待制作 |
| 126 | 爆裂魔法 | UR | `card_anime_u09` | `explosion` | C | T16+T08 | 爆裂法杖与多层魔法阵；蓄力、柱状爆点、烟柱，独立编排 | 待制作 |
| 127 | 猩红腐败 | SSR | `card_anime_u10` | `corruption` | C | T09+T15 | 猩红花蕾与腐败蝶；花开、蝶散、侵蚀标记保留 | 待制作 |
| 128 | 契约成立 | SSR | `card_anime_u11` | `contract` | C | T10+T11 | 丘比简化头像与灵魂宝石；宝石点亮后契约印闭合，角色作为契约标志 | 待制作 |
| 129 | 令咒 | UR | `card_anime_u12` | `commandSpell` | C | T11+T12 | 对应令咒纹样；一划熄灭后命令光束落向目标 | 待制作 |
| 130 | 火舞旋风 | UR | `card_anime_u13` | `sevenSwords` | C | T13+T16 | 长虹剑与火焰刃弧；旋斩卷成火旋风 | 待制作 |
| 131 | 超电磁炮 | UR | `card_anime_u14` | `railgun` | B | T16+T02 | 硬币、电弧和橙色直线轨迹；弹起蓄力后高速贯穿 | 待制作 |
| 132 | 回到你原来的样子 | UR | `card_anime_u15` | `sealCard` | C | T07+T11 | 鸟首封印杖与封印阵；散乱魔力收进卡牌轮廓 | 待制作 |
| 133 | 剑雨 | SSR | `card_anime_u16` | `swordRain` | B | T01+T13 | 少量剑影轮廓；错时坠落成剑雨，停在实际目标范围 | 待制作 |
| 134 | 虚空之泪 | SSR | `card_anime_u17` | `voidTears` | B | T02+T07 | 晶亮泪滴与空间裂缝；泪光掠过后景物副本错开 | 待制作 |
| 135 | 绝对零域 | UR | `card_anime_u18` | `absoluteZero` | C | T15+T12 | 无尽酒葫和九尾冰雾剪影；霜线扩散并冻结目标，不画完整雅雅 | 待制作 |
| 136 | 岩脊共鸣 | SSR | `card_genshin_geo_resonance` | `geoResonance` | B | T01+T10 | 岩柱与金色几何纹；柱起、相邻造物依次回应 | 待制作 |
| 137 | 所闻遍计 | SSR | `card_genshin_growth_link` | `growthLink` | C | T10+T04 | 四叶草式草元素印记；两端连线，一侧变化后另一侧长叶 | 待制作 |
| 138 | 尘歌壶 | UR | `card_genshin_serenitea_pot` | `sereniteaPot` | C | T04+T06 | 尘歌壶、亭阁和洞天云雾；壶口开境、建筑按实际结果重排 | 待制作 |
| 139 | 创 | SSR | `card_sakura_create_build` | `createBuild` | C | T04+T11 | 创牌轮廓与封印杖；纸上建筑线条立起成形 | 待制作 |
| 140 | 基建轮班 | SR | `card_arknights_remote_construction` | `remoteConstruction` | B | T11+T04 | 排班表、通讯器和施工灯；轮班确认后工具重新启动 | 待制作 |
| 141 | 给我建座神社吧 | SR | `card_noragami_shrine_growth` | `shrineGrowth` | B | T04 | 小鸟居和迷你神社；零件拼起、双手形托盘接住，不画夜斗 | 待制作 |
| 142 | 斩尽牛杂 | SSR | `card_neta_keqing_rush` | `keqingRush` | C | T06+T13 | 雷楔与紫色剑痕；多点短闪、收剑后停在实际位置 | 待制作 |
| 143 | 煌煌威灵 | UR | `card_neta_thunder_lord` | `thunderLord` | C | T10+T13 | 神君半身巨影与兵刃；巨影成形、兵刃落击，人物本身是招式主体 | 待制作 |
| 144 | 虚化 | SSR | `card_neta_hollowfication` | `hollowfication` | C | T12+T16 | 虚化面具；沿脸侧轮廓展开、黑红灵压短闪 | 待制作 |
| 145 | 一刀火葬 | SSR | `card_neta_fire_burial` | `fireBurial` | B | T13+T15 | 巨型火刃轮廓；地面纹路亮起，火刃自下而上升起 | 待制作 |
| 146 | 天锁斩月 | UR | `card_neta_bankai` | `bankai` | C | T16+T06 | 黑色长刀和链尾；灵压收拢、刀影缩细、短闪突进 | 待制作 |
| 147 | 闪光的亚丝娜 | UR | `card_crossover_asunaFlash` | `asunaFlash` | C | T08+T13 | 细剑与白红色剑穗；连续光轨突刺，不制作亚丝娜人物 | 待制作 |
| 148 | 怕痛就全点防御了 | UR | `card_crossover_defenseSpecialization` | `defenseSpecialization` | C | T12 | 梅普露大盾的独特轮廓；攻击炸在盾面，盾从烟里稳稳露出 | 待制作 |
| 149 | 光的护封剑 | UR | `card_crossover_revealingSwords` | `revealingSwords` | B | T01+T12 | 金色光剑；依次落下并连成封锁边界 | 待制作 |
| 150 | 融合 | UR | `card_crossover_fusion` | `fusion` | C | T07+T04 | 蓝橙融合旋涡与可替换轮廓；双轮廓卷入再聚合，独立编排 | 待制作 |
| 151 | 双龙魔影剑 | SSR | `card_crossover_shadowBlade` | `shadowBlade` | C | T13+T10 | 双龙魔影剑和龙形暗影；挥斩后两道龙影交错飞出 | 待制作 |
| 152 | 双喷 | SSR | `card_crossover_doubleBoost` | `doubleBoost` | B | T14 | 赛车小图与两段喷焰；漂移出弯后连续两次加速 | 待制作 |
| 153 | 蓝银缠绕 | SR | `card_crossover_blueSilverBind` | `blueSilverBind` | B | T04+T12 | 蓝色藤蔓与叶片；从地面生长、缠绕、收紧 | 待制作 |
| 154 | 结丹 | UR | `card_crossover_goldenCore` | `goldenCore` | B | T16+T04 | 洞府阵纹、灵气粒子和金丹；聚气成丹，独立阶段编排但无需人物 | 待制作 |
| 155 | 女娲补天 | UR | `card_crossover_nuwaMend` | `nuwaMend` | C | T04+T15 | 五色神石与天空裂隙；五色光填补裂缝，独立范围编排 | 待制作 |
| 156 | 无名剑法 | SSR | `card_crossover_namelessSword` | `namelessSword` | A | T13 | 简洁剑气和落叶；几道剑气依次穿过烟尘，末尾收束 | 待制作 |
| 157 | 王的压制 | SSR | `card_crossover_kingsPressure` | `kingsPressure` | B | T11+T12 | 王冠、键帽和静止指示；王冠落下，受压制目标键帽锁住，不画选手 | 待制作 |
| 158 | 点燃大海 | UR | `card_neta_ignite_sea` | `igniteSea` | C | 专属 firefly | 流萤／萨姆：装甲局部点火、青绿火翼、推进加速、俯冲斩击与萤光余烬 | 独立分镜已接入本地，待逐卡验收 |
| 159 | 千本樱 | UR | `card_neta_senbonzakura` | `senbonzakura` | C | T09 | 沿用刀刃与樱花素材；先散开再聚集目标 | 已有预览 |
| 160 | 小舞献祭 | UR | `card_neta_xiaowu_sacrifice` | `xiaowuSacrifice` | C | T09+T04 | 兔耳信物与红色魂环光点；光点重聚为生命轮廓，不制作人物相拥动画 | 待制作 |

### 机会卡 · 103 张

| 序号 | 卡名 | 稀有度 | 稳定 skinId | 当前机制 | 素材 | 复用动作 | 辨识元素与演出建议 | 进度 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 九格大红 | SSR | `chance_delta_central_control` | `income` | C | T11 | 复用非洲之星／大红藏品套件；九格搜索框亮红，仅揭卡侧提示 | 待制作 |
| 2 | 重生只中二等奖 | SSR | `chance_lottery_jackpot` | `income` | B | T11 | 彩票号码条；最后一位擦过后显示二等奖，结果取服务端 | 待制作 |
| 3 | 呀哈哈 | SR | `chance_zelda_chest` | `income` | C | T11 | 克洛格叶面小精灵；从石头后探头递种子，不做完整人物动作 | 待制作 |
| 4 | 一字涨停 | SR | `chance_stock_limit_up` | `income` | A | T11 | 上升行情折线和涨停印；非阻塞到账提示 | 待制作 |
| 5 | 挖到钻石 | SR | `chance_minecraft_diamond` | `income` | B | T13+T11 | 像素铁镐与蓝钻；矿块敲开、钻石弹出 | 待制作 |
| 6 | 月底发薪 | R | `chance_salary_arrived` | `income` | A | T11 | 工资短信与余额亮起；不新增棋盘等待 | 待制作 |
| 7 | 回村种地 | R | `chance_stardew_harvest` | `income` | B | T04+T11 | 成熟作物和菜筐；作物弹入筐中、收入提示 | 待制作 |
| 8 | 迎财神 | R | `chance_caishen_blessing` | `income` | B | T11 | 财神帽轮廓、果盘与红包；红包轻落，不画财神全身 | 待制作 |
| 9 | 万事屋开张 | R | `chance_anime_g27` | `income` | B | T11 | 万事屋招牌和委托单；招牌轻摆、委托到账 | 待制作 |
| 10 | 拆迁暴富 | SR | `chance_demolition_compensation` | `income` | B | T11 | 拆迁通知与房屋轮廓；通知盖章后到账 | 待制作 |
| 11 | 我成替身了 | SR | `chance_huang_silang_silver` | `income` | B | T11 | 礼帽、胡子与替身衣袍；轮廓拼到通用剪影上，提示收入 | 待制作 |
| 12 | 劫生辰纲 | R | `chance_liangshan_share` | `income` | B | T03+T11 | 生辰纲箱笼与银锭；箱开、银锭分出 | 待制作 |
| 13 | 报销到账 | R | `chance_reimbursement_arrived` | `income` | A | T11 | 报销单通过章；直接衔接余额变化 | 待制作 |
| 14 | 最终答案 | UR | `chance_windfall_jackpot` | `income` | B | T11+T15 | 答题选项框、话筒和聚光；最终答案锁定后收入提示，不演主持人 | 待制作 |
| 15 | 加班打车 | R | `chance_overtime_taxi` | `fee` | B | T11 | 出租车顶灯与深夜价签；费用揭示，不预演已付款 | 待制作 |
| 16 | 甲方改需求 | R | `chance_client_revision` | `fee` | B | T11 | 多张带版本号的文件；翻回第一版后出现费用提示 | 待制作 |
| 17 | 打点关系 | R | `chance_palace_bribe` | `fee` | B | T03+T11 | 袖口与银袋；银袋滑入袖口，费用待结算 | 待制作 |
| 18 | 再充最后一单 | R | `chance_genshin_topup` | `fee` | A | T11 | 祈愿光点和付款短信；无金光后账单弹出，不新增抽卡结果 | 待制作 |
| 19 | 不幸啊！ | R | `chance_anime_g11` | `fee` | B | T01+T11 | 裂开的购物袋、滚落食材；小物掉落后费用提示 | 待制作 |
| 20 | 双十一剁手 | SR | `chance_double11_haul` | `fee` | B | T11 | 购物车与满减券；商品叠满、账单展开 | 待制作 |
| 21 | 被割韭菜 | SR | `chance_leek_cut` | `fee` | B | T05+T11 | 韭菜和小镰刀；一刀划过、行情下落，费用待结算 | 待制作 |
| 22 | 内卷报班 | SR | `chance_involution_class` | `fee` | B | T11 | 课程书本和藏起的报名单；翻页展示费用 | 待制作 |
| 23 | 赌马输光 | SR | `chance_kogoro_horse` | `fee` | B | T11 | 马蹄和赛马票；票券垂下，按既定费用提示，不模拟赛马下注 | 待制作 |
| 24 | 房东收租 | SR | `chance_rent_due` | `fee` | A | T11 | 门缝递入账单；复用账单主体，不影响原付款防御窗口 | 待制作 |
| 25 | 股市暴跌 | SR | `chance_stock_crash` | `fee` | A | T11 | 行情线坠落、通知静音；费用提示 | 待制作 |
| 26 | 外卖加料 | R | `chance_delivery_fee` | `fee` | B | T11 | 外卖袋和加料小票；票据展开，不能误演为退款 | 待制作 |
| 27 | 缴纳过路费 | R | `chance_warcraft_fee` | `fee` | B | T11 | 小桥栏杆与双面价目表；翻到背面展示收费 | 待制作 |
| 28 | 天价账单 | UR | `chance_catastrophic_bill` | `fee` | A | T11 | 长账单连续展开，最后停在真实金额；UR也不阻塞付款选择 | 待制作 |
| 29 | 飞雷神之术 | SR | `chance_hiraishin` | `forward` | A | T01+T06 | 复用三叉苦无样板；改为单人抵达实际前进终点，不做双方交换 | 待制作 |
| 30 | 下界传送门 | SR | `chance_nether_portal` | `forward` | B | T01+T06 | 像素黑曜石门与紫幕；开门、穿越、关门 | 待制作 |
| 31 | 上车，来不及解释了 | SR | `chance_ryze_realm_warp` | `forward` | B | T15+T06 | 蓝色符文传送圈；圈内聚光后只移动实际受影响玩家 | 待制作 |
| 32 | 点燃木星 | SR | `chance_engine_burn` | `forward` | C | T02+T14 | 行星发动机喷口与木星弧面；蓝焰上冲，接实际移动 | 待制作 |
| 33 | 腾云驾雾 | SR | `chance_somersault_cloud` | `forward` | A | T14 | 复用筋斗云；翻滚托举后落地 | 待制作 |
| 34 | 蘑菇加速 | SR | `chance_kart_boost` | `forward` | B | T11+T14 | 蘑菇和卡丁车；蘑菇缩入车身，喷焰加速 | 待制作 |
| 35 | 我大了五个 | SR | `chance_blackhole_pull` | `back` | A | T07+T14 | 复用黑洞图集和引力线；只演实际后退，不能凭题材牵走五人 | 待制作 |
| 36 | 毒圈推人 | SR | `chance_pubg_zone` | `back` | B | T15+T14 | 绿色毒圈弧线；安全区收缩，棋子沿真实后退路径撤离 | 待制作 |
| 37 | 踩到香蕉皮 | SR | `chance_banana_peel` | `back` | B | T01+T14 | 香蕉皮；脚下滑开，棋子副本后仰倒滑 | 待制作 |
| 38 | 被阴了一枪 | SR | `chance_cs_ambush` | `back` | B | T02+T14 | 拐角准星与短弹痕；目标惊跳后退，不新增伤害结算 | 待制作 |
| 39 | 八卦阵 | SR | `chance_bagua_formation` | `back` | B | T15+T14 | 八卦盘与石堆；道路方向转动后接后退路径 | 待制作 |
| 40 | 甲方退稿 | SR | `chance_client_reject` | `back` | B | T11+T14 | 方案页和退回印；盖章后棋子退回实际落点 | 待制作 |
| 41 | 股票回撤 | SR | `chance_stock_pullback` | `back` | A | T11+T14 | 回撤折线；沿线倒退的表现接实际移动，不扣额外现金 | 待制作 |
| 42 | 彻底躺平 | SR | `chance_lying_flat` | `skip` | B | T01+T12 | 沙发、抱枕与睡眠符号；落坐、抱枕压扁，休息标记保留 | 待制作 |
| 43 | 打工人摆烂 | SR | `chance_worker_slacking` | `skip` | B | T11+T12 | 电脑窗口、鼠标与耳机；光标轻晃后显示休息标记 | 待制作 |
| 44 | 镜花水月 | SR | `chance_sharingan_illusion` | `skip` | B | T15+T12 | 镜面裂纹和刀影；目标影像错位，合拢成幻术标记 | 待制作 |
| 45 | 我架枪，你发财 | SR | `chance_delta_star_stolen` | `skip` | B | T11+T12 | 空搜索箱与架枪准星；准星守着空箱，落下休息标记 | 待制作 |
| 46 | 抢空投 | SR | `chance_airdrop_crate` | `cards` | B | T01+T11 | 空投箱、降落伞与红烟；落箱开盖，私人端再揭实际卡牌 | 待制作 |
| 47 | 丢精灵球 | SR | `chance_pokeball_catch` | `cards` | B | T02+T11 | 精灵球；投出、摇动、锁扣闭合后进入补给揭示 | 待制作 |
| 48 | 四次元口袋 | SR | `chance_four_dim_pocket` | `cards` | B | T03+T11 | 四次元口袋；依次掏出固定道具剪影，实际私牌另揭示 | 待制作 |
| 49 | 开箱出大红 | R | `chance_delta_loot` | `cards` | C | T11 | 复用大红藏品和搜索箱；红光点亮，不按题材额外发现金 | 待制作 |
| 50 | 拆快递福袋 | R | `chance_double11_bag` | `cards` | B | T05+T11 | 快递福袋与封箱胶带；撕开胶带、卡背弹出 | 待制作 |
| 51 | 抽卡十连 | R | `chance_gacha_ten` | `cards` | A | T11 | 复用卡包和金光揭示；十连只是题材，不公开或伪造实际卡数 | 待制作 |
| 52 | 发红包 | SSR | `chance_red_envelope` | `all_income` | B | T03+T11 | 红包拆分飞向实际收款者；同步到账，无多轮阻塞 | 待制作 |
| 53 | 撒银子啦 | SSR | `chance_goose_city_silver` | `all_income` | B | T09+T11 | 银锭从钱袋洒开，飞向实际收款者 | 待制作 |
| 54 | 神龙许愿 | SSR | `chance_shenron_wish` | `all_income` | C | T10+T11 | 复用神龙／龙珠素材；缩短召唤后发放实际全员收益 | 待制作 |
| 55 | 全服补偿 | SSR | `chance_server_compensation` | `all_income` | A | T11 | 补偿邮件、领取印；一次揭示全员收益 | 待制作 |
| 56 | 掌柜的发月钱 | SSR | `chance_tong_boss_wage` | `all_income` | B | T11 | 账本与算盘；翻页点数后显示各人实际到账 | 待制作 |
| 57 | 全员交房租 | SSR | `chance_everyone_rent` | `all_fee` | A | T03+T11 | 账单分发至实际付款者；不提前显示支付成功 | 待制作 |
| 58 | 帝国征税 | SSR | `chance_galactic_tax` | `all_fee` | C | T11 | 征税船剪影与帝国货运税单；税单盖章，不画风暴兵队列 | 待制作 |
| 59 | 全民剁手 | SSR | `chance_double11_all` | `all_fee` | B | T11 | 复用购物车与快递箱；订单飞向各人费用提示 | 待制作 |
| 60 | 团建AA | SSR | `chance_team_building` | `all_fee` | B | T11 | 团建收款码样式图形与分摊账单；仅装饰，不生成可支付二维码 | 待制作 |
| 61 | 朕的钱呢 | SSR | `chance_tithe_palace` | `rich_fee` | B | T11 | 奏折与玉玺；先标出实际受影响对象，再展开费用 | 待制作 |
| 62 | 争百大 | SSR | `chance_platform_cut` | `rich_fee` | B | T11 | 榜单、应援票与刷新箭头；选中实际缴费者，不画选手 | 待制作 |
| 63 | 交保护费 | SSR | `chance_protection_money` | `rich_fee` | B | T11 | 宅门和保护费票据；票据落到实际目标，不使用全员收费动画 | 待制作 |
| 64 | 劫富济贫 | SSR | `chance_rob_rich` | `rich_fee` | B | T03+T11 | 豪宅门牌与银袋；从实际目标移出费用提示，不额外演穷者获钱 | 待制作 |
| 65 | 抄家了 | SSR | `chance_jia_family_raid` | `rich_fee` | B | T11 | 家产册与封条；封条交叉贴上，展示实际费用 | 待制作 |
| 66 | 化缘布施 | SSR | `chance_alms` | `bottom_income` | B | T03+T11 | 化缘钵；钵转向实际受助者、银锭递出 | 待制作 |
| 67 | 礼物不大 | SSR | `chance_weasley_gift` | `bottom_income` | B | T11 | 毛衣包裹与手写信；包裹打开，收益落到实际受助者 | 待制作 |
| 68 | 砍一刀成功 | SSR | `chance_pdd_cut` | `bottom_income` | A | T05+T11 | 复用剪刀、助力条；进度完成后显示实际受助者收益 | 待制作 |
| 69 | 万能口袋 | SSR | `chance_doraemon_help` | `bottom_income` | B | T03+T11 | 复用四次元口袋；礼物递给实际受助者 | 待制作 |
| 70 | 开盲盒 | SR | `chance_blind_box` | `gamble` | B | T11 | 盲盒；撕封条、揭示服务端既定结果 | 待制作 |
| 71 | 开卡包 | SR | `chance_pokemon_pack` | `gamble` | A | T11 | 通用卡包；撕开封口、按既定结果闪光，不重新抽取 | 待制作 |
| 72 | 强化装备 | SR | `chance_gear_upgrade` | `gamble` | B | T13+T11 | 铁砧和装备轮廓；敲击后按既定结果发光或裂开 | 待制作 |
| 73 | 抄底 | SR | `chance_bottom_fishing` | `gamble` | A | T11 | 抄底折线与买入标记；揭示既定结果，不现场生成随机走势 | 待制作 |
| 74 | 梭哈 | SR | `chance_all_in` | `gamble` | B | T03+T11 | 筹码堆；推入后停顿，揭示服务端结果 | 待制作 |
| 75 | 大乐透 | SR | `chance_super_lotto` | `gamble` | B | T11 | 号码球与彩票；依次点亮既定号码，不模拟二次开奖 | 待制作 |
| 76 | 拆迁队误拆 | SSR | `chance_demolition_mistake` | `self_destroy` | B | T13+T11 | 挖掘机铲斗与看反的图纸；误敲目标，再显示实际建筑损失 | 待制作 |
| 77 | 火烧赤壁 | SSR | `chance_chibi_fire` | `self_destroy` | C | T02+T15 | 火船与相连战船剪影；东风带火至实际受影响地产 | 待制作 |
| 78 | 地鸣 | SSR | `chance_titan_trample` | `self_destroy` | C | T01+T15 | 巨足和墙体蒸汽剪影；脚步依次震落建筑副本，不画巨人行军全景 | 待制作 |
| 79 | 嘶——嘣！ | SSR | `chance_creeper_boom` | `self_destroy` | C | T16+T08 | 苦力怕特色脸／轮廓；闪白蓄力后方块爆开 | 待制作 |
| 80 | 纽约大战 | SSR | `chance_ny_battle` | `disaster` | B | T01+T08 | 飞行器残骸与碎块；落向实际目标，避免按题材扩大损失 | 待制作 |
| 81 | 龙焰 | SSR | `chance_dragon_fire` | `disaster` | C | T02+T15 | 龙头或龙翼剪影、扇形龙焰；火扫过真实目标，不画龙骑士 | 待制作 |
| 82 | 基建狂魔 | SR | `chance_infra_madness` | `free_upgrade` | B | T04 | 吊钩和模块楼；落件拼装，复用修楼动作 | 待制作 |
| 83 | 愚公移山 | SR | `chance_yugong_house` | `free_upgrade` | B | T03+T04 | 土筐、铲子与石块；搬走阻挡后建筑升起 | 待制作 |
| 84 | 义体升级 | SR | `chance_cyber_chrome` | `free_upgrade` | C | T04+T11 | 义眼与接口零件；扫描、嵌入、亮起，映射到真实升级目标 | 待制作 |
| 85 | 又是我？ | SR | `chance_scv_build` | `free_upgrade` | C | T04 | SCV机械臂／焊枪；焊点依次亮起，金属框架成形 | 待制作 |
| 86 | 红石大师 | SR | `chance_redstone_tech` | `free_upgrade` | B | T04+T10 | 红石粉、活塞与按钮；信号流过、活塞合拢完成升级 | 待制作 |
| 87 | 金钟罩 | SSR | `chance_golden_bell` | `trap_immunity` | B | T12 | 金钟轮廓；罩下后轻震，陷阱免疫标记保留 | 待制作 |
| 88 | 无懈可击 | SSR | `chance_zongzi_charm` | `next_waiver` | A | T11+T12 | 固定无懈卡背覆盖来袭标记；先获得状态，实际消耗时再短闪 | 待制作 |
| 89 | 复活甲等队友 | SSR | `chance_guardian_angel` | `next_waiver` | C | T12+T16 | 复活甲与羽翼轮廓；光环蓄起，仅演本卡费用免除状态，不复活玩家 | 待制作 |
| 90 | 超级加倍 | SSR | `chance_immobilize` | `rent_double` | B | T11 | 扑克牌与超级加倍按钮；按钮按下后留下倍率提示 | 待制作 |
| 91 | 艺术就是爆炸 | UR | `chance_demolition_fanatic` | `opponent_destroy` | C | T02+T08 | 黏土小鸟；振翅式缩放飞出、远处爆点，独立短编排 | 待制作 |
| 92 | 塔塔开 | SSR | `chance_tatakae` | `all_morale` | B | T11+T14 | 镜面、披风角与号角；短呼应后全员移动增益标记，不画艾伦 | 待制作 |
| 93 | 水神的审判 | SSR | `chance_meropide_day_trip` | `jail` | C | T11+T12 | 复用枫丹审判套件；聚光转向实际被告，法槌敲下 | 待制作 |
| 94 | 新时代 | SSR | `chance_onepiece_uta_concert` | `concert` | C | T09+T15 | 乌塔特色耳机／麦克风；音符铺向实际命中的地产 | 待制作 |
| 95 | 放课后茶会 | SSR | `chance_k_on_after_school` | `concert` | B | T09+T15 | 吉他、茶杯和蛋糕；茶杯放下后音符成串弹出 | 待制作 |
| 96 | μ’s，开场！ | SSR | `chance_lovelive_music_start` | `concert` | B | T09+T15 | 九色应援棒与舞台星形；节拍波纹展开，不绘九位人物 | 待制作 |
| 97 | God knows… | SSR | `chance_anime_g20` | `concert` | C | T09+T15 | 电吉他与 ENOZ 舞台小标；拨弦光波沿目标街区流动 | 待制作 |
| 98 | ROOM | SSR | `chance_onepiece_room_shuffle` | `shuffle` | B | T15+T06 | 半透明蓝色半球与切分网格；领域张开后多人同步错位 | 待制作 |
| 99 | 乾坤大挪移 | SSR | `chance_qiankun_shift` | `shuffle` | A | T16+T06 | 环形掌风与旋转脚印；多人沿弧线换位，区别于传送门 | 待制作 |
| 100 | 学级裁判 | SR | `chance_anime_u05` | `classTrial` | C | T11+T15 | 复用黑白熊头像、言弹与裁判席轮廓；矛盾击破后展示实际结果 | 待制作 |
| 101 | 如果智慧有颜色，那一定是蓝色 | SSR | `chance_crossover_aquaPurify` | `aquaPurify` | B | T15+T12 | 净化阵与卡通水浪；水波扫过实际清除目标，不额外治疗 | 待制作 |
| 102 | 黑暗游戏 | SSR | `chance_crossover_darkGame` | `darkGame` | C | T15+T11 | 千年积木、桌面火光和阴影边界；围出对决空间，不演人物 | 待制作 |
| 103 | 恩佐斯之怒 | UR | `chance_crossover_nzothWrath` | `nzothWrath` | C | T10+T15 | 恩佐斯巨眼与触须；裂隙伸出触须、巨眼睁开，独立范围编排 | 待制作 |

### 财富卡 · 68 张

| 序号 | 卡名 | 稀有度 | 稳定 skinId | 当前机制 | 素材 | 复用动作 | 辨识元素与演出建议 | 进度 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 发工资了 | R | `fortune_salary_day` | `income` | A | T11 | 工资到账消息与余额亮起；不新增棋盘等待 | 待制作 |
| 2 | 基金终于回本 | R | `fortune_fund_breakeven` | `income` | A | T11 | 行情折线回到成本线；轻提示收益 | 待制作 |
| 3 | 开宝箱爆金 | R | `fortune_chest_gold_drop` | `income` | A | T11 | 复用宝箱开盖和金光；只表现实际现金收益 | 待制作 |
| 4 | 旅途的报酬 | R | `fortune_anime_g26` | `income` | B | T11 | 旅行日记、魔女徽章和扫帚；报酬夹进日记，不画伊蕾娜 | 待制作 |
| 5 | 年终奖到账 | SR | `fortune_year_end_bonus` | `income` | B | T11 | 年终奖信封；揭开封口后提示到账 | 待制作 |
| 6 | 拆迁款下来了 | SSR | `fortune_demolition_payout` | `income` | B | T11 | 复用拆迁通知和到账章；不演房屋被拆除 | 待制作 |
| 7 | 越花越有钱 | SSR | `fortune_spend_more_earn_more` | `income` | B | T11 | 钱袋漏出的金币绕回袋中；循环一次后显示真实收益 | 待制作 |
| 8 | 双十一退款 | R | `fortune_double11_refund` | `income` | B | T11 | 退款包裹和退回箭头；钱款图标回到账户 | 待制作 |
| 9 | 又一个呀哈哈 | R | `fortune_korok_reward` | `income` | C | T11 | 复用克洛格叶面精灵与种子；石头掀开、种子弹出 | 待制作 |
| 10 | 外卖红包膨胀 | R | `fortune_takeout_red_packet` | `income` | B | T11 | 红包与外卖袋；红包数字膨胀到真实金额 | 待制作 |
| 11 | 这刀真砍成了 | R | `fortune_pdd_bargain` | `income` | A | T05+T11 | 复用剪刀和助力条；进度完成，现金提示，不演费用减半 | 待制作 |
| 12 | 五元神明 | R | `fortune_anime_g05` | `income` | B | T11 | 五元硬币、委托便签；硬币落定后委托单亮起 | 待制作 |
| 13 | 甲方结款 | SR | `fortune_client_payment` | `income` | A | T11 | 合同确认章与到账消息；直接接余额变化 | 待制作 |
| 14 | 撤离带货回城 | SR | `fortune_extraction_loot` | `income` | B | T11 | 复用撤离背包和出口标志；包打开清点收益 | 待制作 |
| 15 | 彩票中了小奖 | SR | `fortune_lottery_small_win` | `income` | B | T11 | 复用彩票号码条；小奖印章与实际收益 | 待制作 |
| 16 | 终于过了 | SR | `fortune_boss_rune` | `income` | B | T09+T11 | 卢恩小符文；少量金光聚入余额区域，不重演BOSS战 | 待制作 |
| 17 | 完成A级委托 | SR | `fortune_a_rank_mission` | `income` | B | T11 | A级委托单与完成章；报酬滑出 | 待制作 |
| 18 | 股票涨停 | SR | `fortune_stock_limit_up` | `income` | A | T11 | 复用涨停折线与印章；显示实际收益 | 待制作 |
| 19 | 下把猛攻 | SSR | `fortune_african_star` | `income` | C | T11 | 复用非洲之星与背包；藏品收入包中，不演额外撤离移动 | 待制作 |
| 20 | 创业失败继承家业 | UR | `fortune_fortune_jackpot` | `income` | B | T11 | 创业计划书、家族来电和公司钥匙；计划书合起、钥匙落下，非阻塞收益提示 | 待制作 |
| 21 | 十连双黄 | SR | `fortune_gacha_double_gold` | `cards_two` | A | T11 | 固定双金光的揭卡装饰；实际获得卡牌只向本人展示 | 待制作 |
| 22 | 根本没喝福灵剂 | SR | `fortune_felix_felicis` | `cards_two` | C | T11 | 未倒空的福灵剂瓶与守门球；瓶轻晃后光点落到卡背，不画罗恩 | 待制作 |
| 23 | 集齐无限宝石 | SR | `fortune_infinity_stones` | `cards_two` | C | T16+T11 | 无限手套和宝石；宝石依次亮起后衔接私人补给 | 待制作 |
| 24 | 挖到宝箱 | R | `fortune_treasure_chest_dig` | `cards_one` | B | T13+T11 | 锄头与埋土宝箱；敲到箱盖、清土开箱 | 待制作 |
| 25 | 开出紫装 | R | `fortune_epic_loot_drop` | `cards_one` | B | T11 | 紫色装备轮廓和战利品袋；紫光揭示后私人端获得卡牌 | 待制作 |
| 26 | 锦囊妙计 | SSR | `fortune_silk_bag_plan` | `directed` | B | T11 | 锦囊与卷轴；解绳展开，具体候选仅本人可见 | 待制作 |
| 27 | 掏出秘密道具 | SSR | `fortune_secret_gadget` | `directed` | B | T03+T11 | 复用四次元口袋；掏出道具轮廓，真实选择保留私人揭示 | 待制作 |
| 28 | 这个按钮别乱按 | SSR | `fortune_agasa_invention` | `directed` | B | T11 | 带大按钮的小发明；按钮闪烁、机关弹开，不提前公开候选 | 待制作 |
| 29 | 扩建先欠着 | SR | `fortune_nook_expansion` | `free_upgrade` | B | T11+T04 | 扩建合同与房屋模块；翻到扩建页后拼装，不擅自增加欠款 | 待制作 |
| 30 | 舔包舔到破防 | SR | `fortune_loot_crate_materials` | `free_upgrade` | B | T03+T04 | 物资箱和螺丝；零件倒出后参与拼装 | 待制作 |
| 31 | 大龙BUFF | SR | `fortune_baron_buff` | `free_upgrade` | C | T16+T04 | 纳什男爵增益标记；紫色符文环绕建筑后完成升级 | 待制作 |
| 32 | 更多的水晶塔 | SR | `fortune_base_new_building` | `free_upgrade` | C | T04+T10 | 水晶塔轮廓；晶体升起、供能环亮起 | 待制作 |
| 33 | 精灵工匠 | SR | `fortune_elven_craftsman` | `free_upgrade` | B | T13+T04 | 精灵纹样工具与武器零件；轻敲修整，落到真实升级目标 | 待制作 |
| 34 | 太空电梯通车 | SR | `fortune_space_elevator` | `free_upgrade` | B | T14+T04 | 缆索与电梯吊舱；吊舱上升带动建筑层级增长 | 待制作 |
| 35 | 桃园结义 | SSR | `fortune_peach_garden_share` | `all_income` | B | T11 | 三只结义酒碗与桃花；碰碗后收益分别落向实际参与者 | 待制作 |
| 36 | 压岁钱人人有 | SSR | `fortune_lucky_money_all` | `all_income` | B | T03+T11 | 复用红包；同步递给所有实际收款者 | 待制作 |
| 37 | 公会分红 | SSR | `fortune_guild_dividend` | `all_income` | B | T11 | 公会徽章、名单与钱袋；名单勾选后同步收益 | 待制作 |
| 38 | 替身术 | SSR | `fortune_substitution_jutsu` | `next_waiver` | B | T01+T12 | 绑护额的木桩与白烟；受击时人影换成木头，获取状态时只亮信物 | 待制作 |
| 39 | 固若金汤 | SSR | `fortune_zhongli_shield` | `next_waiver` | C | T12 | 玉璋护盾纹样；金色片层围合，受击时局部亮起 | 待制作 |
| 40 | 千万别出这个圈 | SSR | `fortune_sling_ring_portal` | `next_waiver` | B | T12+T16 | 金箍棒画出的地面圈；环闭合后抵挡费用，不使用漫威传送门 | 待制作 |
| 41 | 今晚全场我买单 | SSR | `fortune_wang_duoyu_pays` | `next_waiver` | B | T03+T11 | 签字笔、菜单与买单印；账单被大手接走，实际免除时才盖章 | 待制作 |
| 42 | 吊车尾逆袭 | SSR | `fortune_underdog_comeback` | `bottom_bonus` | B | T11 | 训练奖章与额外礼袋；先全员基础收益，再标实际加奖对象 | 待制作 |
| 43 | 给新人的鼓励 | SSR | `fortune_frieren_encouragement` | `bottom_bonus` | B | T03+T11 | 旅行礼物与花朵；向实际受助者多放一份，不画芙莉莲 | 待制作 |
| 44 | 劫富济贫 | SSR | `fortune_rob_rich_feed_poor` | `bottom_bonus` | B | T03+T11 | 银袋分发与破旧门牌；基础发放后突出实际额外受助者 | 待制作 |
| 45 | 罗德岛救援 | SSR | `fortune_anime_g28` | `bottom_bonus` | C | T03+T11 | 罗德岛补给箱和医疗徽记；补给送往实际受助者，不增加治疗数值 | 待制作 |
| 46 | 妖怪报恩 | SSR | `fortune_spirit_repayment` | `bottom_bonus` | B | T03+T11 | 小妖怪爪印与袖口礼袋；分完后悄悄再递一份，不需完整妖怪 | 待制作 |
| 47 | 触底反弹 | SSR | `fortune_bottom_rebound` | `bottom_bonus` | A | T11 | 折线触底回升与额外奖励标记；仅轻提示 | 待制作 |
| 48 | 雪中送炭 | SSR | `fortune_timely_help_silver` | `bottom_bonus` | B | T11 | 炭筐、雪花和炉火；炭落炉中，暖光映出收益 | 待制作 |
| 49 | 投资暴雷 | UR | `fortune_fortune_crash` | `fee` | B | T11 | 投资公告、下坠折线和裂开的印章；费用提示，不抢先完成付款 | 待制作 |
| 50 | 基建狂魔 | UR | `fortune_construction_frenzy` | `full_upgrade` | B | T04 | 吊车钩与楼房模块；多地同步落件，复用建造动作 | 待制作 |
| 51 | 三只小猪 | UR | `fortune_three_little_pigs` | `full_upgrade` | C | T04+T10 | 三只小猪的简化头部或工具剪影、稻草木板砖墙；三种材质依次合成砖屋 | 待制作 |
| 52 | 人才引进 | UR | `fortune_talent_recruitment` | `talent` | B | T11+T04 | 简历、聘书与钥匙；聘书展开后分别揭示实际产权和奖励 | 待制作 |
| 53 | 三顾茅庐 | UR | `fortune_three_visits` | `talent` | C | T01+T11 | 草庐门与叠起的礼物；三次敲门、门开，接实际奖励，不画三人队列 | 待制作 |
| 54 | 龙卷风摧毁停车场 | UR | `fortune_parking_tornado` | `global_demolition` | A | T16+T08 | 现有龙卷风图集与碎瓦；按实际目标受损播放，不必画阿福 | 待制作 |
| 55 | 大闹天宫 | UR | `fortune_wukong_heaven_riot` | `global_demolition` | B | T13+T01 | 复用金箍棒，加天宫瓦片；横扫、瓦片落下，按实际损失范围播放 | 待制作 |
| 56 | 大秘宝 | SSR | `fortune_onepiece_treasure` | `treasure` | B | T11+T01 | 泛黄藏宝图与叉号；卷轴展开、实际地产落宝藏标记 | 待制作 |
| 57 | 黄金矿工 | SSR | `fortune_gold_miner` | `treasure` | B | T03+T01 | 摆动钩爪与金块；钩爪拉出金块，宝藏落到真实目标 | 待制作 |
| 58 | 宝箱怪 | SSR | `fortune_frieren_mimic` | `treasure` | C | T11+T03 | 有牙齿的宝箱怪和两只靴子；箱盖咬合、靴子晃动，不制作完整人物 | 待制作 |
| 59 | 找到你了 | SSR | `fortune_anime_g09` | `treasure` | B | T11+T01 | 捉迷藏信笺、花瓣与呼唤气泡；晨光点亮宝藏位置，不使用通用海盗箱作主体 | 待制作 |
| 60 | 道具大派送 | SR | `fortune_dora_supply_all` | `all_cards` | B | T03+T11 | 复用四次元口袋；固定道具轮廓向外分发，私人卡单独揭示 | 待制作 |
| 61 | 原来大家都有 | SR | `fortune_gifts_for_everyone` | `all_cards` | B | T03+T11 | 同款蝴蝶结礼盒；礼盒依次出现，最后拉远显示大家都有 | 待制作 |
| 62 | 樱花庄欢迎会 | SR | `fortune_anime_g18` | `all_cards` | B | T11 | 欢迎横幅、画笔与饮料杯；桌面拼好、礼盒分发 | 待制作 |
| 63 | 樱花庄改造计划 | SSR | `fortune_sakurasou_all_construction` | `allConstruction` | B | T04+T11 | 改造蓝图与画笔；轮廓描出，实际建筑依次更新 | 待制作 |
| 64 | 黄金树的恩惠 | SSR | `fortune_elden_erdtree_blessing` | `erdtreeBlessing` | C | T09+T04 | 黄金树冠剪影和赐福叶片；金叶落向实际目标，远端依次回应 | 待制作 |
| 65 | 天才俱乐部 | UR | `fortune_neta_genius_club` | `geniusClub` | B | T11+T04 | 方案卡、齿轮和推演线；多方案展开后突出实际选择，独立编排但不画三名角色 | 待制作 |
| 66 | 世界的种子 | SSR | `fortune_crossover_worldSeed` | `worldSeed` | C | T04+T15 | 发光种子和数据世界轮廓；点线铺成小世界，收束到真实建设结果 | 待制作 |
| 67 | 白云无尽 | UR | `fortune_crossover_endlessClouds` | `endlessClouds` | C | T09+T15 | 红莲、折扇与白云；先评估既有卡面分层素材能否裁作主体，再做开莲光波，独立编排 | 待制作 |
| 68 | 如果奇迹有颜色，那一定是粉色 | UR | `fortune_crossover_pinkMiracle` | `pinkMiracle` | C | T04+T11 | 衣橱、镜面、裙摆与饰品；几套轮廓轻换装后聚成实际奖励，不做人形换装动画 | 待制作 |

## 来源与覆盖校验

- 已核对启用机制过滤，与 `ContentCatalog.candidates()` 的展开方式一致。
- 三个卡池合计 331 个稳定身份，清单一一覆盖且无重复；160／103／68 分池数量与配置一致。
- 已有样板身份与 `ur-spell-preview.ts`、`regular-spell-preview.ts` 对照，共 10 张。
- 停用机制：`fortune:extra_action`。
- 本轮只生成设计清单，未执行 build、单元测试、冒烟测试或运行验证，未修改动画实现、提交或部署。

| 配置来源 | SHA-256（用于确认清单对应快照） |
| --- | --- |
| `backend/src/main/resources/game-rules/card-events.json` | `4c593a5df915c39dbcebd6b1bb954036333458ed6d4ab4dc8c0bbc6373b846e6` |
| `backend/src/main/resources/game-rules/chance-events.json` | `d2cb969647e6a2e4b470c9a28cbdd528d6e287cfa751d3faa7334255e2fe9526` |
| `backend/src/main/resources/game-rules/fortune-events.json` | `9e4c2ebe7ff03e6447e5b48712c556fce29ec8365af629a9d25885bb9e19f5d5` |

其他核对文件：`backend/src/main/java/com/dfw/domain/rules/ContentCatalog.java`、`src/web/board-effect-presentation.ts`、`src/shared/types.ts`、`src/web/ur-spell-preview.ts`、`src/web/regular-spell-preview.ts`。
