import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

export const notificationPreferences = reactive({
  enabled: localStorage.getItem('dfw:notifications') === 'on',
  showDetails: localStorage.getItem('dfw:notification-details') === 'on',
});
export const notificationPermission = ref<NotificationPermission | 'unsupported'>('default');
export const notificationError = ref('');
export const notificationRequesting = ref(false);
const activeNotifications = new Map<string, Notification>();

/** 无入参，读取当前浏览器的通知能力及授权状态，无返回值。 */
export function refreshNotificationPermission(): void {
  notificationPermission.value = window.isSecureContext && 'Notification' in window ? Notification.permission : 'unsupported';
}

/** 无入参，关闭当前页面发出的系统通知，无返回值。 */
export function clearNotifications(): void {
  for (const notification of activeNotifications.values()) notification.close();
  activeNotifications.clear();
}

/** 无入参，由设置按钮切换通知；仅用户主动开启时请求授权，无返回值。 */
export async function toggleNotifications(): Promise<void> {
  if (notificationRequesting.value) return;
  notificationError.value = '';
  if (notificationPreferences.enabled) { notificationPreferences.enabled = false; clearNotifications(); return; }
  refreshNotificationPermission();
  if (notificationPermission.value === 'unsupported' || notificationPermission.value === 'denied') return;
  notificationRequesting.value = true;
  try {
    if (notificationPermission.value !== 'granted') notificationPermission.value = await Notification.requestPermission();
    notificationPreferences.enabled = notificationPermission.value === 'granted';
  } catch { notificationError.value = '通知授权未完成，请检查浏览器的网站通知设置。'; }
  finally { notificationRequesting.value = false; }
}

/** 输入标题、正文和分组标识，按详情偏好展示获授权的后台通知；同组替换，点击返回页面。 */
export function notifyBackground(title: string, body: string, tag: string): void {
  if (!document.hidden || !notificationPreferences.enabled) return;
  refreshNotificationPermission();
  if (notificationPermission.value !== 'granted') return;
  try {
    activeNotifications.get(tag)?.close();
    const notification = new Notification(notificationPreferences.showDetails ? title : '通知提醒', {
      body: notificationPreferences.showDetails ? body : '你有一条新提醒，请打开页面查看。', tag, silent: true,
    });
    activeNotifications.set(tag, notification);
    notification.onclick = () => { window.focus(); clearNotifications(); };
    notification.onclose = () => { if (activeNotifications.get(tag) === notification) activeNotifications.delete(tag); };
    notification.onerror = () => { notificationError.value = '当前浏览器未能显示通知，请检查网站及系统通知设置；部分手机浏览器不支持。'; };
  } catch { notificationError.value = '当前浏览器未能显示通知，请检查网站及系统通知设置；部分手机浏览器不支持。'; }
}

/** 无入参，保存通知与详情偏好；隐藏详情、返回页面或离开应用时清理已有通知。 */
export function useNotificationEnvironment(): void {
  watch(notificationPreferences, preferences => {
    localStorage.setItem('dfw:notifications', preferences.enabled ? 'on' : 'off');
    localStorage.setItem('dfw:notification-details', preferences.showDetails ? 'on' : 'off');
    if (!preferences.enabled || !preferences.showDetails) clearNotifications();
  }, { flush: 'sync' });
  /** 无入参，同步授权状态，回到页面后关闭已读通知，无返回值。 */
  function visibility(): void { refreshNotificationPermission(); if (!document.hidden) clearNotifications(); }
  onMounted(() => { visibility(); document.addEventListener('visibilitychange', visibility); window.addEventListener('focus', visibility); });
  onBeforeUnmount(() => { clearNotifications(); document.removeEventListener('visibilitychange', visibility); window.removeEventListener('focus', visibility); });
}
