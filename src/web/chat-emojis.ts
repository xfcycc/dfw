/** 小红书表情以稳定文字代码保存，实时消息和历史记录使用同一份本地图片。 */
const names = ['微笑', '害羞', '失望', '汗颜', '哇', '喝奶茶', '自拍', '偷笑', '飞吻', '石化', '笑哭', '赞', '蹲后续', '暗中观察', '买爆', '大笑', '色色', '生气', '哭惹', '萌萌哒', '斜眼', '可怜', '鄙视', '皱眉', '抓狂', '捂脸', '派对', '吧唧', '惊恐', '抠鼻', '再见', '叹气', '睡觉', '得意', '吃瓜', '扶墙', '黑薯问号', '黄金薯', '吐舌头', '扯脸', 'doge', '笑哭了', '棒', '亲一个', '完啦', '心心眼', '扶额', '抽泣', '呃', '嘻嘻', '捂嘴笑', '超喜欢', '泪崩', '尬住', '坏笑', '种草', '拔草', '变猪猪', '冰淇淋', '吃粽子', '棒棒糖'];
export const CHAT_EMOJIS = names.map(name => ({ name, code: `[小红书:${name}]`, url: `/images/chat/xiaohongshu/${encodeURIComponent(name)}.webp` }));
export type ChatEmoji = typeof CHAT_EMOJIS[number];
const byCode = new Map(CHAT_EMOJIS.map(emoji => [emoji.code, emoji]));
let preload: Promise<void> | undefined;

/** 输入普通文字，返回文字与已收录表情片段；未知代码保留原文。 */
export function splitChatEmojis(text: string): { text: string; emoji?: ChatEmoji }[] {
  return text.split(/(\[小红书:[^\]\n]+\])/g).filter(Boolean).map(part => ({ text: part, emoji: byCode.get(part) }));
}

/** 无入参，进入房间即加载并解码全部表情；后续进入对局复用缓存，返回加载任务。 */
export function preloadChatEmojis(): Promise<void> {
  return preload ??= Promise.all(CHAT_EMOJIS.map(emoji => {
    const image = new Image(); image.src = emoji.url; return image.decode();
  })).then(() => {}).catch((error: unknown) => { preload = undefined; console.warn('聊天表情预加载失败', error); });
}
