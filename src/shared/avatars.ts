/** 房间公开形象，与后端 PlayerAvatars 标识一致。 */
export const AVATARS = [
  { id: 'explorer', name: '探险家' }, { id: 'robot', name: '机器人' }, { id: 'wizard', name: '银发魔女' },
  { id: 'pirate', name: '海盗船长' }, { id: 'astronaut', name: '宇航员' }, { id: 'cat', name: '猫咪' },
] as const;
export type AvatarId = typeof AVATARS[number]['id'];
