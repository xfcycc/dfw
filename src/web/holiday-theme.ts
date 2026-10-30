import { computed, ref } from 'vue';
import { imageUrl } from './image-assets';

export type HolidayId = 'halloween' | 'christmas' | 'spring';
export type HolidayPreview = HolidayId | 'default' | 'auto';

export const holidayThemes = {
  halloween: { name: '万圣节', caption: '万圣奇妙夜', range: '10 月 25 日 — 11 月 1 日' },
  christmas: { name: '圣诞夜', caption: '圣诞星光夜', range: '12 月 18 日 — 12 月 26 日' },
  spring: { name: '春节', caption: '新春同乐', range: '除夕前 7 天 — 正月十五' },
} as const;

const currentTime = ref(new Date());
const preview = ref<HolidayPreview>('auto');
const solarCalendar = new Intl.DateTimeFormat('en', { timeZone: 'Asia/Shanghai', month: 'numeric', day: 'numeric' });
const lunarCalendar = new Intl.DateTimeFormat('en-u-ca-chinese', { timeZone: 'Asia/Shanghai', month: 'numeric', day: 'numeric' });

/** 输入日期与历法，返回北京时间的月、日；闰月保留原始月份标识。 */
function calendarDay(date: Date, calendar: Intl.DateTimeFormat): { month: string; day: number } {
  const parts = calendar.formatToParts(date);
  return { month: parts.find(part => part.type === 'month')!.value, day: Number(parts.find(part => part.type === 'day')!.value) };
}

/** 输入时间，返回当年节日皮肤；春节从正月初一前八天持续到正月十五。 */
export function holidayAt(date: Date): HolidayId | null {
  const solar = calendarDay(date, solarCalendar);
  if ((solar.month === '10' && solar.day >= 25) || (solar.month === '11' && solar.day === 1)) return 'halloween';
  if (solar.month === '12' && solar.day >= 18 && solar.day <= 26) return 'christmas';
  const lunar = calendarDay(date, lunarCalendar);
  if (lunar.month === '1' && lunar.day <= 15) return 'spring';
  // 除夕是初一前一天，除夕前七天即初一前八天；按北京时间逐日向前查找。
  for (let offset = 1; offset <= 8; offset++) {
    const next = calendarDay(new Date(date.getTime() + offset * 86_400_000), lunarCalendar);
    if (next.month === '1' && next.day === 1) return 'spring';
  }
  return null;
}

export const activeHoliday = computed(() => preview.value === 'auto' ? holidayAt(currentTime.value) : preview.value === 'default' ? null : preview.value);
export const holidayTheme = computed(() => {
  const id = activeHoliday.value;
  return id ? { id, ...holidayThemes[id], background: imageUrl(`/images/holidays/${id}-town.webp`), title: imageUrl(`/images/holidays/${id}-title.webp`) } : null;
});
export const holidayPreview = computed(() => preview.value);

/** 输入预览选项，仅在当前页面临时切换皮肤；不更改服务器或持久化偏好。 */
export function setHolidayPreview(value: HolidayPreview): void {
  preview.value = value;
}

/** 启动北京时间刷新与切回页面刷新，返回卸载时使用的清理函数。 */
export function startHolidayClock(): () => void {
  const refresh = (): void => { currentTime.value = new Date(); };
  refresh();
  const interval = window.setInterval(refresh, 30_000);
  document.addEventListener('visibilitychange', refresh);
  window.addEventListener('focus', refresh);
  return () => {
    window.clearInterval(interval);
    document.removeEventListener('visibilitychange', refresh);
    window.removeEventListener('focus', refresh);
    preview.value = 'auto';
  };
}
