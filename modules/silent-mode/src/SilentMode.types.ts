/** Weekdays follow Date.getDay(): 0 = Sunday ... 6 = Saturday. */
export type QuietWindow = {
  id: string;
  startMinute: number;
  endMinute: number;
  weekdays: number[];
  enabled: boolean;
};

export type NativeQuietStatus = {
  supported: boolean;
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
