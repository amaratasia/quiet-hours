import { NativeModule, requireNativeModule } from 'expo';

import {
  ImportantPerson,
  NativeQuietStatus,
  QuietMode,
  QuietStatus,
  QuietWindow,
  RepeatCallerSettings,
} from './SilentMode.types';

declare class SilentModeNativeModule extends NativeModule<{}> {
  refresh(): NativeQuietStatus;
  silenceFor(minutes: number): NativeQuietStatus;
  cancelManualSilence(): NativeQuietStatus;
  setQuietMode(quietMode: QuietMode): NativeQuietStatus;
  setWindows(serializedWindows: string): NativeQuietStatus;
  setImportantPeople(serializedPeople: string): NativeQuietStatus;
  setRepeatCallers(enabled: boolean, threshold: number): NativeQuietStatus;
  openNotificationAccessSettings(): void;
  openAppDetailsSettings(): void;
  requestIgnoreBatteryOptimisation(): void;
  openDndAccessSettings(): void;
  openExactAlarmSettings(): void;
}

const nativeModule = requireNativeModule<SilentModeNativeModule>('SilentMode');

function parseStatus({ windowsJson, importantPeopleJson, ...rest }: NativeQuietStatus): QuietStatus {
  return {
    ...rest,
    windows: JSON.parse(windowsJson) as QuietWindow[],
    importantPeople: JSON.parse(importantPeopleJson) as ImportantPerson[],
  };
}

export default {
  refresh: () => parseStatus(nativeModule.refresh()),
  silenceFor: (minutes: number) => parseStatus(nativeModule.silenceFor(minutes)),
  cancelManualSilence: () => parseStatus(nativeModule.cancelManualSilence()),
  setQuietMode: (quietMode: QuietMode) => parseStatus(nativeModule.setQuietMode(quietMode)),
  setWindows: (windows: QuietWindow[]) =>
    parseStatus(nativeModule.setWindows(JSON.stringify(windows))),
  setImportantPeople: (people: ImportantPerson[]) =>
    parseStatus(nativeModule.setImportantPeople(JSON.stringify(people))),
  setRepeatCallers: ({ enabled, threshold }: RepeatCallerSettings) =>
    parseStatus(nativeModule.setRepeatCallers(enabled, threshold)),
  openNotificationAccessSettings: () => nativeModule.openNotificationAccessSettings(),
  openAppDetailsSettings: () => nativeModule.openAppDetailsSettings(),
  requestIgnoreBatteryOptimisation: () => nativeModule.requestIgnoreBatteryOptimisation(),
  openDndAccessSettings: () => nativeModule.openDndAccessSettings(),
  openExactAlarmSettings: () => nativeModule.openExactAlarmSettings(),
};
