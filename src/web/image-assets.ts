import imageAssets from './image-assets.json';

/** 输入原公共图片路径，返回已上传并校验的云端图片 URL。 */
export function imageUrl(path: string): string {
  return (imageAssets as Record<string, string>)[path] ?? '';
}
