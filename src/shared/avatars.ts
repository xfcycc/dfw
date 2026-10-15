/** 房间公开形象，与后端 PlayerAvatars 标识一致。 */
export const AVATARS = [
  { id: 'explorer', name: '探险家' }, { id: 'robot', name: '机器人' }, { id: 'wizard', name: '银发魔女' },
  { id: 'pirate', name: '海盗船长' }, { id: 'astronaut', name: '宇航员' }, { id: 'cat', name: '猫咪' },
  { id: 'swordsman', name: '国风剑客' }, { id: 'cyberpunk', name: '赛博少女' }, { id: 'elf', name: '森林精灵' },
  { id: 'panda', name: '熊猫少年' }, { id: 'chef', name: '元气主厨' }, { id: 'rockstar', name: '摇滚少女' },
  { id: 'mermaid', name: '海洋人鱼' }, { id: 'dragon', name: '龙角少年' }, { id: 'detective', name: '复古侦探' },
  { id: 'bunny', name: '甜梦兔兔' },
] as const;
export type AvatarId = typeof AVATARS[number]['id'];
