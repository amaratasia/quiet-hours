const MINUTES_PER_HOUR = 60;
const MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

export const WEEKDAY_LETTERS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
export const EVERY_DAY = [0, 1, 2, 3, 4, 5, 6];

export function minuteOfDayToDate(minuteOfDay: number): Date {
  const date = new Date();
  date.setHours(Math.floor(minuteOfDay / MINUTES_PER_HOUR), minuteOfDay % MINUTES_PER_HOUR, 0, 0);
  return date;
}

export function dateToMinuteOfDay(date: Date): number {
  return date.getHours() * MINUTES_PER_HOUR + date.getMinutes();
}

export function formatMinuteOfDay(minuteOfDay: number): string {
  return formatClockTime(minuteOfDayToDate(minuteOfDay));
}

export function formatClockTime(date: Date): string {
  return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
}

export function formatMoment(timestampMillis: number): string {
  const moment = new Date(timestampMillis);
  const clockTime = formatClockTime(moment);
  const daysAway = Math.round((startOfDay(moment) - startOfDay(new Date())) / MILLIS_PER_DAY);

  if (daysAway === 0) return clockTime;
  if (daysAway === 1) return `tomorrow ${clockTime}`;
  return `${moment.toLocaleDateString([], { weekday: 'short' })} ${clockTime}`;
}

export function formatDuration(minutes: number): string {
  if (minutes < MINUTES_PER_HOUR) return `${minutes}m`;
  return `${minutes / MINUTES_PER_HOUR}h`;
}

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}
