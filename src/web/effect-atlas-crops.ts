import atlases from './effect-atlas-crops.json';

export interface AtlasCrop {
  /** 原图宽度，单位为像素。 */
  width: number;
  /** 原图高度，单位为像素。 */
  height: number;
  /** 原图中的显示矩形，依次为横坐标、纵坐标、宽度、高度。 */
  box: number[];
  /** 原图坐标系中的图格归属路径，用于排除相邻图案。 */
  path?: string;
}

/** 输入图集身份和格号，返回经过逐格分离的显示范围及邻图裁切边界。 */
export function atlasCrop(atlas: keyof typeof atlases, cell: number): AtlasCrop {
  const source = atlases[atlas];
  return { width: source.width, height: source.height, ...source.cells[cell]! };
}

/** 输入通用效果身份，返回保持原始帧中心与统一尺寸的全部帧裁切配置。 */
export function effectFrameCrops(kind: 'meteor' | 'treasure'): AtlasCrop[] {
  const atlas = `board-effect-${kind}` as keyof typeof atlases;
  return atlases[atlas].cells.map((_, cell) => atlasCrop(atlas, cell));
}
