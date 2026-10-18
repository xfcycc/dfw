# 当前背景音乐来源与处理记录

更新日期：2026-10-18。

- 曲目：**编织愿景、草篮咣当、愿望安睡于屋檐、愿望摇曳的街角、奇迹大陆的起点**。
- 文件标注艺人：**无限暖暖 / FoldEcho**。
- 来源：用户提供的五份本地 FLAC，文件名均为 `无限暖暖,FoldEcho - 曲目名.flac`；2026-10-18 新增四首来自用户的 Downloads 目录。
- 用户说明用途：个人聚会娱乐，非商用。
- 此曲不是首版的 CC0 音乐，不沿用 Holizna / Quiet Village 的作者或授权标注；权利仍归原权利人，本记录不构成额外授权。

| 游戏文件 | 曲目（首页、大厅、对局、结算共用） | 格式与时长 |
| --- | --- | --- |
| nikki-bianzhi-yuanjing.m4a | 编织愿景 | AAC / M4A，双声道，48 kHz，目标码率 128 kbps，133.125 秒 |
| nikki-caolan-guangdang.m4a | 草篮咣当 | AAC / M4A，双声道，48 kHz，目标码率 128 kbps，124.000 秒 |
| nikki-yuanwang-anshui-yu-wuyan.m4a | 愿望安睡于屋檐 | AAC / M4A，双声道，48 kHz，目标码率 128 kbps，143.578 秒 |
| nikki-yuanwang-yaoye-de-jiejiao.m4a | 愿望摇曳的街角 | AAC / M4A，双声道，48 kHz，目标码率 128 kbps，125.625 秒 |
| nikki-qiji-dalu-de-qidian.m4a | 奇迹大陆的起点 | AAC / M4A，双声道，48 kHz，目标码率 128 kbps，145.493 秒 |

处理方式：从用户提供的 FLAC 转码，保留完整曲目，未剪辑旋律、未改变速度或音高。新增四首使用系统 `afconvert -f m4af -d aac -b 128000 -q 127` 转换。游戏按上表顺序播放，列表末尾回到第一首，沿用现有音量、淡入、后台暂停和用户手势启用逻辑。原始 FLAC 未修改。

SHA-256：

```text
2ac17e21b7d6ada35b6edf57f0d9629ffc747b8fc43f312ee844de83fe2ceaaa  无限暖暖,FoldEcho - 编织愿景.flac
df2c42b207a0e9150c9ef6cb8203b21018f1d287ef56001febe2e4544a6cb729  nikki-bianzhi-yuanjing.m4a
78a818a651af378a959d72dd6cf4d1384671af842b47bda894636a9f800fbe14  无限暖暖,FoldEcho - 草篮咣当.flac
9bf403adb7ea0443cdd1e30745439f00f88471d03d570215c039d52dd5fcf9be  nikki-caolan-guangdang.m4a
bde740c713f34137b02f297064c9e7ed8adf7100774a4c064c30ae37b5a0a8f5  无限暖暖,FoldEcho - 愿望安睡于屋檐.flac
cb41867747ff61e2d5ff4d58bed7ba745019bbb8efefc2d5523193a0d7a82528  nikki-yuanwang-anshui-yu-wuyan.m4a
e19ca7c38d61b735b0590db129746288861a4572e9c3c059c80d824bb58653b7  无限暖暖,FoldEcho - 愿望摇曳的街角.flac
07598dc9543101116469a559e9cf1316d8b366a5cb2c7a1168f91d956f01bd92  nikki-yuanwang-yaoye-de-jiejiao.m4a
eec01e0320e0fd327c0e7f1fbd8e6b6f94f9b33ff14d2aa449a615ba1dc6d3cf  无限暖暖,FoldEcho - 奇迹大陆的起点.flac
ce5e96645fb0de42bca886aa1659713383f7c5228e532d50ca6aaddac16133df  nikki-qiji-dalu-de-qidian.m4a
```

首版的两首 Quiet Village 音乐及对应 CC0 文本已移除，避免未使用素材继续随前端分发。

`sfx/` 目录的十三类短音效是本项目原创合成资源，不采样 BGM，也不是《无限暖暖》原版音效。通过用户提供音频的频谱与音高分布分析，选择 A、C、D、E、G 音符，以木质拨弦、柔和铃音、少量纸张气流和衰减反射构成。

生成脚本为 `scripts/generate-soft-sounds.py`，需要 Python 和 NumPy，输出 44.1 kHz、16 位单声道 WAV；`preview.wav` 按点击、回合、聊天、骰子、落格、入账、支出、买地、建造、抽卡、道具、错误、胜利的顺序串联试听。

分析边界：当前会话没有听觉输入能力，采用 PCM 信号分析；A 小调只是全曲音高分布的推测，并非乐谱鉴定，乐器音色是本项目的设计选择，未声称辨认出原曲乐器或完成主观试听。
