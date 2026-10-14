/** 输入固定存档码，规范化大小写和展示用分隔符。 */
export function normalizeArchiveCode(value: string): string { return value.replace(/[\s-]/g, '').toUpperCase(); }
/** 输入要查看的房间固定码，在导航前选择该档案，避免其他页面更新最近记录时跳错房间。 */
export function selectArchiveCode(value?: string | null): void { if (value) localStorage.setItem('dfw:archive:last', normalizeArchiveCode(value)); }
/** 输入固定存档码，分组展示，复制后仍可直接用于查询。 */
export function formatArchiveCode(value: string): string {
  const code = normalizeArchiveCode(value); return [...(code.slice(0, 20).match(/.{1,5}/g) ?? []), ...(code.slice(20) ? [code.slice(20)] : [])].join('-');
}
/** 输入业务 POST 接口及 JSON 参数，返回其响应，失败时保留服务端可读说明。 */
export async function archiveRequest<T>(path: string, input: object): Promise<T> {
  const response = await fetch(`/api/${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input) });
  const value = await response.json() as T & { error?: string };
  if (!response.ok) throw new Error(value.error || '操作未完成，请重试'); return value;
}
