/** Weekdays follow Date.getDay(): 0 = Sunday ... 6 = Saturday. */
export type QuietWindow = {
  id: string;
  startMinute: number;
  endMinute: number;
  weekdays: number[];
  enabled: boolean;
};

export type QuietMode = 'silent' | 'vibrate';

export type NativeQuietStatus = {
  supported: boolean;
  quietMode: QuietMode;
  hasDndAccess: boolean;
  canScheduleExactAlarms: boolean;
  isSilentNow: boolean;
  manualSilenceUntil: number | null;
  nextChangeAt: number | null;
  windowsJson: string;
};

export type QuietStatus = Omit<NativeQuietStatus, 'windowsJson'> & {
  windows: QuietWindow[];
};
