# 永久街区租金事件

两张机会 SSR 皮肤共用永久涨租效果，机制合计8%，各皮肤4%。其他机会效果合计92%，相互比例不变；财富与道具池不变。

| 皮肤ID | 名称 | 台词 | OSS卡面 |
| --- | --- | --- | --- |
| chance_landlady | 包租婆，怎么没水了？ | 水费不用花钱啊？ | https://pic.caiguoyu.cn/typora/20261117105327327.webp |
| chance_dingxiu | 得加钱 | 很润～ | https://pic.caiguoyu.cn/typora/20261117105327329.webp |

两张均为1254×1254、无文字无边框的Q版插画，PNG原图等尺寸转WebP（quality=90、method=6），通过本机PicGo上传至既有OSS。HTTP HEAD均返回200及image/webp。

| 皮肤 | WebP字节数 | OSS Content-MD5 |
| --- | ---: | --- |
| 包租婆 | 486778 | fxKMUcpHiYBP1+pR+lGGHg== |
| 得加钱 | 332500 | Obf2Ug2b/TS64bf/E299XQ== |

原图保留位置：

- `/Users/caiguoyu/.codex/generated_images/01a1e72f-f939-7500-b850-ddb91c5d38a6/exec-8084ba0e-ef10-41f7-a9f5-c45048324338.png`
- `/Users/caiguoyu/.codex/generated_images/01a1e72f-f939-7500-b850-ddb91c5d38a6/exec-4e1499b4-2879-460a-8efe-5ccaa1546c91.png`

## 规则

从开局起可抽中，无轮次和产权比例门槛。按现有棋盘街区（四边）等概率选择一处，包含无主、抵押和暂停收租的地产。选中街区的每块地产永久租金加成增加25个百分点，最高100%；已满不重抽、不补现金。

`Tile.permanentRentPercent`随地块保存，旧存档缺省0。产权变化、拆房、卖地和清算回银行均保留；黑洞、净化、白云无尽等不清除。新局重新生成棋盘后归零。

结算顺序为原等级租金、花瓣与演唱会、下次付租翻倍、永久加成、既有半租与付款防御。永久加成乘算后向下取整。停租规则继续有效；加成仅应用在租金付款链路，不改地价、建造、抵押、赎回、变现、净资产、宝藏、交换价或灾害补偿。前端租金展示与机器人租金估值同步识别永久加成。

完整卡面文案以`docs/content/chance.md`为源表，`scripts/sync-game-content.py`生成行为配置。丁修台词按用户选择使用“很润～”，不将其标为谈价场景原句。

旧默认规则存档允许继续使用原进度；新字段缺省零，已生成费用不重算，恢复后的新事件按当前事件池抽取。

## 验证

- `npm run typecheck`通过。
- `mvn -o -f backend/pom.xml -Dmaven.test.skip=true compile`通过。
- 配置静态核对：新机制精确为2/25，两张皮肤各1/25；其他机会效果合计23/25，相互比例不变；财富与道具配置保持不变。
- 两张OSS图片HTTP200、格式、文件长度、Content-MD5与本地WebP一致。
- `git diff --check`通过。未编写或执行单元测试、冒烟测试，未执行前端build；未进行真实对局、重启恢复和浏览器页面验收，未部署。
