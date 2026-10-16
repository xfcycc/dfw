/** 房间公开形象，与后端 PlayerAvatars 标识一致。 */
export const AVATARS = [
  { id: 'explorer', name: '阿洛' }, { id: 'robot', name: '波波' }, { id: 'wizard', name: '露娜' },
  { id: 'pirate', name: '杰克' }, { id: 'astronaut', name: '星野' }, { id: 'cat', name: '糯米' },
  { id: 'swordsman', name: '云舟' }, { id: 'cyberpunk', name: '霓娜' }, { id: 'elf', name: '艾琳' },
  { id: 'panda', name: '团团' }, { id: 'chef', name: '小满' }, { id: 'rockstar', name: '洛琪' },
  { id: 'mermaid', name: '海莉' }, { id: 'dragon', name: '龙焱' }, { id: 'detective', name: '沈墨' },
  { id: 'bunny', name: '米露' },
  { id: 'pikachu', name: '皮卡丘' }, { id: 'eevee', name: '伊布' },
  { id: 'ninja', name: '影丸' }, { id: 'caixukun', name: '蔡徐坤' },
] as const;
export type AvatarId = typeof AVATARS[number]['id'];
