import imageAssets from './image-assets.json';

/** 输入公共图片路径，返回随前端分发的补给站插画或已登记的云端图片 URL。 */
export function imageUrl(path: string): string {
  if (path === '/images/tiles/supply.png') return new URL('./assets/supply-station.webp', import.meta.url).href;
  return (imageAssets as Record<string, string>)[path] ?? '';
}
