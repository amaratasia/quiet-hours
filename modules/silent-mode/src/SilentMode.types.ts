/** Weekdays follow Date.getDay(): 0 = Sunday ... 6 = Saturday. */
export type QuietWindow = {
  id: string;
  startMinute: number;
  endMinute: number;
  weekdays: number[];
  enabled: boolean;
};

export type QuietMode = 'silent' | 'vibrate';

export type ImportantPerson = {
  id: string;
  displayName: string;
  phoneNumbers: string[];
};

export type RepeatCallerSettings = {
  enabled: boolean;
  threshold: number;
};

export type BreakThroughSetup = {
  hasNotificationAccess: boolean;
  /** Both phone state and call log, which Android needs to share the caller's number. */
  hasPhoneAccess: boolean;
  hasContactsAccess: boolean;
  isBatteryOptimisationIgnored: boolean;
  lastWhatsAppSeenAt: number | null;
};

export type NativeQuietStatus = {
  supported: boolean;
  quietMode: QuietMode;
  hasDndAccess: boolean;
  canScheduleExactAlarms: boolean;
  isSilentNow: boolean;
  manualSilenceUntil: number | null;
  nextChangeAt: number | null;
  windowsJson: string;
  importantPeopleJson: string;
  repeatCallers: RepeatCallerSettings;
  setup: BreakThroughSetup;
};

export type QuietStatus = Omit<NativeQuietStatus, 'windowsJson' | 'importantPeopleJson'> & {
  windows: QuietWindow[];
  importantPeople: ImportantPerson[];
};
