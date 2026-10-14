/** 输入 HTTP 状态，返回服务不可用或请求失败的中文提示。 */
function responseError(status: number): string {
  if (status >= 500) return `游戏服务暂时不可用，请稍后重试（HTTP ${status}）。`;
  if (status === 404) return '未找到游戏接口，请检查服务地址（HTTP 404）。';
  return `操作未完成，请重试（HTTP ${status}）。`;
}

/** 输入业务接口路径及 JSON 参数，返回响应对象；连接、空响应或格式异常统一为可读错误。 */
export async function postJson<T>(path: string, input: object): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api/${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input) });
  } catch { throw new Error('无法连接游戏服务，请检查网络后重试。'); }
  let value: unknown;
  try { value = await response.json(); }
  catch { throw new Error(response.ok ? '游戏服务返回异常，请稍后重试。' : responseError(response.status)); }
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error(response.ok ? '游戏服务返回异常，请稍后重试。' : responseError(response.status));
  if (!response.ok) {
    const error = 'error' in value && typeof value.error === 'string' ? value.error : '';
    throw new Error(error || responseError(response.status));
  }
  return value as T;
}
