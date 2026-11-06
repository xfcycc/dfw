# 非 UR 正式链路本地检查

2026-11-06。本轮检查 R／SR／SSR 共 283 张卡面，未提交、未部署。

后续原生页面连续回合与重复使用修复见 [连续对局效果检查](continuous-match-effects-validation.md)。

## 覆盖口径

- 283 张卡面的身份、机制与图集映射均核对，无缺项。
- 269 张公开卡面在真实本地游戏快照中取得对应 skinId 的演出片段；14 张私人补给确认只在持有人私人事件中回传，公共片段不带其卡面身份。
- 使用隔离恢复房间、正常双玩家 WebSocket 命令，执行主动出牌、目标确认、材料选择、投票、暗押、支付和防御。按用户授权准备本地手牌、地产、现金、受损记录及已抽中的事件队列；事件目标和结果仍由原规则产生，不新增正式发牌入口。
- 数据准备仅修改本轮新建房间并保留修改前备份。调整局面时保持当前流程任务及决策一致；首轮受内存缓存影响的准备记录已重新核对，以最终服务端快照为覆盖证据。
- 来源覆盖是逐卡主入口检查。页面美术观察是代表性抽查，不表示 283 张均在桌面、手机或全部持续与防御组合中逐帧验收。

## 本轮修复

1. 非 UR 双动作按原总时长先后播放：例如末影珍珠射出后闪现，嘶——嘣！蓄力后冲击，扩建先欠着盖章后生长。UR 与已有状态短提示保持原时间轴。
2. T14 承载道具随棋子副本沿服务端真实路径移动，避免筋斗云等主体停在终点。减少动态时保留静态主体。
3. 费用防御主体定位到实际付款人，中央账单继续显示真实减免前后金额。
4. 明确全员机制保留本人；金额轻提示按实际公开参与者展示，同来源连续金额片段合并目标，不增加结算等待。
5. 迟缓的原子效果接收者与出牌者分开定位，光线从真正出牌者发出。顶部轨迹控制点限制在棋盘内。
6. ROOM、乾坤大挪移补齐公开来源，沿原目标换位。
7. 学级裁判保存公开来源，投票揭晓后使用真实受罚玩家。全弃权不产生休息目标，旧存档无来源时继续通用表现。
8. 黑暗游戏保存公开事件身份，揭晓后只展示真实双方。出牌观察只把真实使用的响应牌视为来源，暗押、净化、燃料、发现中的材料或候选不再误当发动牌。

## 实际分支与页面观察

| 场景 | 实际核对 |
| --- | --- |
| 9 张免租、6 张减半 | 逐张经正常掷骰落入对手地产，进入付款防御并使用对应牌；免租归零，减半按服务端整数规则处理 |
| 6 张解控 | 蓝银缠绕引出的真实响应，解控牌消费且对应控制被抵消 |
| 虚化 | 响应真实换位，双方位置保留，未伪造传送成功 |
| 时间溯行 | 开局自选窗口结束后进入真实重掷响应，消费对应牌并接受新点数 |
| 一刀火葬 | 自家建筑 1 → 0 的献祭与对手建筑 2 → 0 的真实结果，沿原穿透规则执行 |
| 普通防拆盾 | 如意金箍棒命中 1 层盾，盾 1 → 0、建筑保持 2 级；页面为通用拦截层，未播放拆毁专属 |
| 学级裁判／黑暗游戏 | 完成双方秘密选择后揭晓，公开演出保留正确来源及实际目标，不公开暗押卡面 |
| 私人补给 14 张 | 持有人收到对应私人事件；公共 supply 片段无对应私人 skinId |

桌面使用 1280×720，手机使用 390×844 浏览器视口。正式 RoomView 代表性查看筋斗云、末影珍珠、桃园结义、帮我砍一刀、迟缓光线及如意金箍棒被盾拦截。核对主体、目标、路径、第二动作次序、付款金额、全员位置和自然收场。检查用按钮只发送正常本地命令并关闭揭卡，交付前移除；未增加正式演出框。

浏览器观察确认：筋斗云承载主体在真实 traveler 内；末影珍珠两阶段为 936ms／864ms；桃园结义有两名实际收款人的素材锚点；手机账单 ¥1,800 → ¥900，主体锚定付款人；迟缓光线从施术者位置指向对手；护盾拦截没有专属拆毁层。

本轮未重新做操作系统切后台、系统减少动态设置、iOS／Android 真机和低端机帧率验收。相关清理机制沿用已完成的 UR 本地事件模拟验证，见 [UR 检查记录](ur-live-validation.md)。

## 验证与证据

最终执行 `npm run typecheck`、`mvn -o -f backend/pom.xml -Dmaven.test.skip=true compile`、定向 diff 检查。未编写或执行单元测试、冒烟测试，未执行前端 build，未提交或部署。

- 实际命令与公开结果：`/private/tmp/dfw-nonur-results.json`。
- 逐卡来源与目标：`/private/tmp/dfw-nonur-coverage.json`。
- 局面备份：`/private/tmp/dfw-nonur-before.json`、`/private/tmp/dfw-nonur-before-*.json`、`/private/tmp/dfw-nonur-postcommands-before.json`、`/private/tmp/dfw-nonur-shield-before.json`。
- 页面截图：`/private/tmp/dfw-nonur-cloud-desktop.png`、`/private/tmp/dfw-nonur-pearl-desktop.png`、`/private/tmp/dfw-nonur-income-desktop.png`、`/private/tmp/dfw-nonur-coupon-mobile.png`、`/private/tmp/dfw-nonur-slow-mobile.png`、`/private/tmp/dfw-nonur-shield-mobile.png`。
- 全卡预览：`http://127.0.0.1:5187/effects-preview.html?sample=all`。

## 逐卡覆盖清单

房间均为本地隔离模拟局面。下表不含会话凭据，公开类型仅描述展示事实；私人事件不发布其卡面身份。

| 卡牌 | 稀有度 | 本地房间 | 实际演出／私人回传 |
| --- | --- | --- | --- |
| 任意门 | SSR | 417043 | path、supply |
| 神抽 | SSR | 890924 | path |
| 这就是「空白」 | SSR | 508589 | path、supply |
| 时间停止 | SSR | 126764 | path、income、supply |
| 月光宝盒 | SSR | 498081 | path、income、supply |
| 舞空术 | R | 870176 | path、fee |
| 撤离 | R | 384836 | path、fee |
| 末影珍珠 | R | 716547 | path、fee |
| 翔 | R | 884686 | path、fee |
| 瞬步 | R | 279453 | path、supply |
| 竹蜻蜓 | R | 292267 | path |
| 闪现向前 | R | 839393 | path、fee |
| 筋斗云 | R | 122204 | path |
| 全速前进 | R | 477967 | path |
| 眉头一皱 | R | 503323 | path |
| 倒车入库 | R | 695236 | path |
| 时空断裂 | R | 358555 | path |
| 定住七仙女 | SSR | 984512 | spell |
| 提莫必须死 | SSR | 206800 | spell |
| 草丛三兄弟 | SSR | 518108 | spell |
| 师傅别填 | SSR | 584279 | spell |
| 昏昏倒地 | SSR | 556908 | spell |
| 实心水泥墩 | SSR | 950235 | spell |
| 走不动道 | SSR | 338388 | spell |
| 七之弹 | SSR | 416955 | spell |
| 禁止通行 | SSR | 490327 | spell |
| 如来神掌 | SR | 686469 | demolish |
| 如意金箍棒 | SR | 699416 | demolish |
| 专业装修队 | SR | 361015 | demolish |
| 沙漠神殿压力板 | SR | 231013 | demolish |
| 花花草草也是生命 | SR | 501388 | demolish |
| 风之伤 | SR | 706091 | demolish |
| 金蝉脱壳 | SR | 712696 | swap、spell |
| 道友请留步 | SR | 216203 | swap、spell |
| 移形换位 | SR | 343470 | swap、spell |
| 我们真的回不去了吗 | SR | 258842 | swap、spell |
| 飞雷神之术 | SR | 187858 | swap、spell |
| 空城计 | SSR | 736689 | immunity |
| 草船借箭 | SSR | 568628 | immunity |
| 隐形衣 | SSR | 253495 | immunity |
| 掩耳盗铃 | SSR | 401323 | immunity |
| 我能打一整天 | SSR | 268284 | immunity |
| 金身等死 | SSR | 163749 | immunity |
| 马什么梅 | SSR | 387662 | immunity |
| 风王结界 | SSR | 254563 | immunity |
| 存在感消失 | SSR | 448598 | immunity |
| 换个颜色再打 | R | 513343 | moveBoost |
| 五速鞋游走 | R | 380965 | moveBoost |
| 你最好现在就跑 | R | 244012 | moveBoost |
| 一米六怎么了 | R | 892640 | moveBoost |
| 超频加速 | R | 639775 | moveBoost |
| 靖妖傩舞 | R | 889413 | moveBoost |
| SUPER！ | SR | 805586 | upgrade |
| 改车消星 | SR | 889399 | upgrade |
| 多重影分身 | SR | 874046 | upgrade |
| 熹妃回宫 | SR | 153643 | upgrade |
| 一步一步走到最高 | SR | 948254 | upgrade |
| 修复如初 | SR | 882249 | upgrade |
| 团购 | SR | 315566 | halfPayment |
| 帮我砍一刀 | SR | 395507 | halfPayment |
| 及时雨 | SR | 914258 | halfPayment |
| 舌战群儒 | SR | 792441 | halfPayment |
| 时间宝石 | SR | 935592 | halfPayment |
| 真相只有一个 | SR | 785267 | halfPayment |
| 踹牢门 | SR | 885432 | spell |
| 月下的魔术师 | SR | 200390 | spell |
| 关不住的鸟 | SR | 130493 | spell |
| 穿透环乌龙 | SR | 531839 | spell |
| 多比是自由的 | SR | 431880 | spell |
| 约定的钥匙 | SR | 258266 | spell |
| 顺手牵羊 | SR | 188566 | spell |
| 乡绅的钱如数奉还 | SR | 484551 | spell |
| 以彼之道，还施彼身 | SR | 546952 | spell |
| 潜行100 | SR | 811315 | spell |
| 东西我收下了 | SR | 583031 | spell |
| 移花接木 | SR | 131287 | spell |
| 虚空抽取 | SR | 897104 | spell |
| 偷窃 | SR | 623506 | spell |
| 十连三金 | SR | 437291 | income |
| 摸出非洲之星 | SR | 820938 | income |
| 我买下了银行 | SR | 985401 | income |
| 顶金币 | R | 846019 | income |
| 开宝箱 | R | 511225 | income |
| 黄金时代的错觉 | R | 958884 | income |
| 拍卖会捡漏 | R | 199158 | income |
| 爆破鬼才 | SSR | 710090 | teleport |
| 谕示裁定 | SSR | 220507 | teleport、path、jail |
| 黑白熊登场 | SSR | 810875 | teleport、path、jail |
| A.T.力场 | SSR | 780647 | shield |
| 缎带结界 | SSR | 361871 | shield |
| 制衡 | SR | 826014 | recycle |
| UMR上线 | SR | 451275 | recycle |
| 从零开始 | SR | 276459 | recycle |
| 发现 | SSR | 236818 | discover |
| 闷棍 | SR | 278301 | rest、spell |
| 迟缓光线 | R | 434649 | slow |
| 斗转星移 | SSR | 395136 | swap、spell |
| 红线续缘 | SSR | 309547 | swap、spell |
| 手牌抹杀 | SSR | 609254 | spell |
| 地爆天星 | SSR | 635121 | teleport |
| 暗影步 | SR | 835519 | teleport、path、fee |
| 传送 | SSR | 351988 | teleport、path |
| 炉石回城 | SSR | 992507 | teleport、path |
| 赐福指引 | SSR | 764486 | teleport、path |
| 转移水晶 | SSR | 133910 | teleport、path |
| 黑洞 | SSR | 438250 | spell |
| 陨星 | SSR | 843370 | spell |
| 时间溯行 | SSR | 409330 | path |
| 星爆气流斩 | SSR | 887794 | demolish |
| 刻刻帝·四之弹 | SSR | 130207 | spell |
| 猩红腐败 | SSR | 208479 | spell |
| 契约成立 | SSR | 873949 | spell |
| 剑雨 | SSR | 906689 | rest |
| 虚空之泪 | SSR | 677597 | spell |
| 岩脊共鸣 | SSR | 573577 | upgrade |
| 所闻遍计 | SSR | 291813 | spell |
| 创 | SSR | 256844 | upgrade |
| 基建轮班 | SR | 509585 | spell |
| 给我建座神社吧 | SR | 857835 | spell |
| 斩尽牛杂 | SSR | 832762 | demolish、teleport、path、fee |
| 虚化 | SSR | 949442 | spell |
| 一刀火葬 | SSR | 609819 | demolish |
| 双龙魔影剑 | SSR | 515110 | demolish |
| 双喷 | SSR | 998968 | spell |
| 蓝银缠绕 | SR | 531839 | spell |
| 无名剑法 | SSR | 941989 | spell |
| 王的压制 | SSR | 192303 | spell |
| 九格大红 | SSR | 670448 | income |
| 重生只中二等奖 | SSR | 882365 | income |
| 呀哈哈 | SR | 389931 | income |
| 一字涨停 | SR | 437291 | income |
| 挖到钻石 | SR | 634745 | income |
| 月底发薪 | R | 564909 | income |
| 回村种地 | R | 445073 | income |
| 迎财神 | R | 323365 | income |
| 万事屋开张 | R | 685826 | income |
| 拆迁暴富 | SR | 999839 | income |
| 我成替身了 | SR | 432179 | income |
| 劫生辰纲 | R | 546224 | income |
| 报销到账 | R | 812834 | income |
| 加班打车 | R | 113193 | fee |
| 甲方改需求 | R | 947896 | fee |
| 打点关系 | R | 784067 | fee |
| 再充最后一单 | R | 937610 | fee |
| 不幸啊！ | R | 720748 | fee |
| 双十一剁手 | SR | 370856 | fee |
| 被割韭菜 | SR | 618385 | fee |
| 内卷报班 | SR | 901656 | fee |
| 赌马输光 | SR | 765408 | fee |
| 房东收租 | SR | 971913 | fee |
| 股市暴跌 | SR | 365442 | fee |
| 外卖加料 | R | 274792 | fee |
| 缴纳过路费 | R | 415769 | fee |
| 飞雷神之术 | SR | 460532 | movement、path |
| 下界传送门 | SR | 603903 | movement、path |
| 上车，来不及解释了 | SR | 889093 | movement、path |
| 点燃木星 | SR | 322233 | movement、path |
| 腾云驾雾 | SR | 208465 | movement、path |
| 蘑菇加速 | SR | 537652 | movement、path、fee |
| 我大了五个 | SR | 174434 | movement、path |
| 毒圈推人 | SR | 173997 | movement、path |
| 踩到香蕉皮 | SR | 284664 | movement、path |
| 被阴了一枪 | SR | 991994 | movement、path |
| 八卦阵 | SR | 763579 | movement、path |
| 甲方退稿 | SR | 716710 | movement、path |
| 股票回撤 | SR | 212519 | movement、path |
| 彻底躺平 | SR | 582925 | rest |
| 打工人摆烂 | SR | 170191 | rest |
| 镜花水月 | SR | 368477 | rest |
| 我架枪，你发财 | SR | 419577 | rest |
| 抢空投 | SR | 344348 | 私人事件回传，公共身份隐藏 |
| 丢精灵球 | SR | 551030 | 私人事件回传，公共身份隐藏 |
| 四次元口袋 | SR | 854708 | 私人事件回传，公共身份隐藏 |
| 开箱出大红 | R | 565394 | 私人事件回传，公共身份隐藏 |
| 拆快递福袋 | R | 175538 | 私人事件回传，公共身份隐藏 |
| 抽卡十连 | R | 111869 | 私人事件回传，公共身份隐藏 |
| 发红包 | SSR | 442514 | income |
| 撒银子啦 | SSR | 793846 | income |
| 神龙许愿 | SSR | 299182 | income |
| 全服补偿 | SSR | 836452 | income |
| 掌柜的发月钱 | SSR | 947756 | income |
| 全员交房租 | SSR | 223945 | fee |
| 帝国征税 | SSR | 812166 | fee |
| 全民剁手 | SSR | 279316 | fee |
| 团建AA | SSR | 805062 | fee |
| 朕的钱呢 | SSR | 731391 | fee |
| 争百大 | SSR | 239554 | fee |
| 交保护费 | SSR | 146443 | fee |
| 劫富济贫 | SSR | 165270 | fee |
| 抄家了 | SSR | 949447 | fee |
| 化缘布施 | SSR | 556999 | income |
| 礼物不大 | SSR | 868281 | income |
| 砍一刀成功 | SSR | 362576 | income |
| 万能口袋 | SSR | 411447 | income |
| 开盲盒 | SR | 100687 | gambleLose |
| 开卡包 | SR | 810678 | gambleLose |
| 强化装备 | SR | 350607 | gambleLose |
| 抄底 | SR | 572545 | gambleWin |
| 梭哈 | SR | 228148 | gambleLose |
| 大乐透 | SR | 695944 | gambleLose |
| 拆迁队误拆 | SSR | 215612 | demolish、income |
| 火烧赤壁 | SSR | 178578 | demolish、income |
| 地鸣 | SSR | 603321 | demolish、income |
| 嘶——嘣！ | SSR | 259339 | demolish |
| 纽约大战 | SSR | 346771 | demolish、income |
| 龙焰 | SSR | 358320 | demolish、income |
| 基建狂魔 | SR | 221998 | upgrade |
| 愚公移山 | SR | 627586 | upgrade |
| 义体升级 | SR | 113189 | upgrade |
| 又是我？ | SR | 648821 | upgrade |
| 红石大师 | SR | 723195 | upgrade |
| 金钟罩 | SSR | 426177 | trapImmune |
| 无懈可击 | SSR | 535393 | waiver |
| 复活甲等队友 | SSR | 353177 | waiver |
| 超级加倍 | SSR | 772233 | rentDouble |
| 塔塔开 | SSR | 339167 | morale |
| 水神的审判 | SSR | 971362 | teleport |
| 新时代 | SSR | 128991 | concert |
| 放课后茶会 | SSR | 576876 | concert |
| μ’s，开场！ | SSR | 304874 | concert |
| God knows… | SSR | 990741 | concert |
| ROOM | SSR | 994439 | shuffle |
| 乾坤大挪移 | SSR | 425756 | shuffle |
| 学级裁判 | SR | 243376 | spell |
| 如果智慧有颜色，那一定是蓝色 | SSR | 365195 | spell |
| 黑暗游戏 | SSR | 180423 | spell |
| 发工资了 | R | 560107 | income |
| 基金终于回本 | R | 865890 | income |
| 开宝箱爆金 | R | 896492 | income |
| 旅途的报酬 | R | 362980 | income |
| 年终奖到账 | SR | 854599 | income |
| 拆迁款下来了 | SSR | 574193 | income |
| 越花越有钱 | SSR | 707020 | income |
| 双十一退款 | R | 836420 | income |
| 又一个呀哈哈 | R | 615237 | income |
| 外卖红包膨胀 | R | 849376 | income |
| 这刀真砍成了 | R | 951885 | income |
| 五元神明 | R | 982222 | income |
| 甲方结款 | SR | 594234 | income |
| 撤离带货回城 | SR | 403595 | income |
| 彩票中了小奖 | SR | 296019 | income |
| 终于过了 | SR | 996619 | income |
| 完成A级委托 | SR | 744514 | income |
| 股票涨停 | SR | 102342 | income |
| 下把猛攻 | SSR | 235713 | income |
| 十连双黄 | SR | 637577 | 私人事件回传，公共身份隐藏 |
| 根本没喝福灵剂 | SR | 257496 | 私人事件回传，公共身份隐藏 |
| 集齐无限宝石 | SR | 309091 | 私人事件回传，公共身份隐藏 |
| 挖到宝箱 | R | 683211 | 私人事件回传，公共身份隐藏 |
| 开出紫装 | R | 875906 | 私人事件回传，公共身份隐藏 |
| 锦囊妙计 | SSR | 575159 | 私人事件回传，公共身份隐藏 |
| 掏出秘密道具 | SSR | 269189 | 私人事件回传，公共身份隐藏 |
| 这个按钮别乱按 | SSR | 503932 | 私人事件回传，公共身份隐藏 |
| 扩建先欠着 | SR | 268300 | upgrade |
| 舔包舔到破防 | SR | 890924 | upgrade |
| 大龙BUFF | SR | 769005 | upgrade |
| 更多的水晶塔 | SR | 343711 | upgrade |
| 精灵工匠 | SR | 334190 | upgrade |
| 太空电梯通车 | SR | 381140 | upgrade |
| 桃园结义 | SSR | 768449 | income |
| 压岁钱人人有 | SSR | 585718 | income |
| 公会分红 | SSR | 338566 | income |
| 替身术 | SSR | 321294 | waiver |
| 固若金汤 | SSR | 399105 | waiver |
| 千万别出这个圈 | SSR | 549714 | waiver |
| 今晚全场我买单 | SSR | 347673 | waiver |
| 吊车尾逆袭 | SSR | 493269 | income |
| 给新人的鼓励 | SSR | 636746 | income |
| 劫富济贫 | SSR | 252035 | income |
| 罗德岛救援 | SSR | 223174 | income |
| 妖怪报恩 | SSR | 141279 | income |
| 触底反弹 | SSR | 879846 | income |
| 雪中送炭 | SSR | 139047 | income |
| 大秘宝 | SSR | 875002 | treasure |
| 黄金矿工 | SSR | 109441 | treasure |
| 宝箱怪 | SSR | 797093 | treasure |
| 找到你了 | SSR | 676441 | treasure |
| 道具大派送 | SR | 466420 | supply |
| 原来大家都有 | SR | 368263 | supply |
| 樱花庄欢迎会 | SR | 441036 | supply |
| 樱花庄改造计划 | SSR | 268948 | upgrade |
| 黄金树的恩惠 | SSR | 833689 | upgrade |
| 世界的种子 | SSR | 837266 | upgrade |
