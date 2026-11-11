import { NativeModule, requireNativeModule } from 'expo';

import { NativeQuietStatus, QuietMode, QuietStatus, QuietWindow } from './SilentMode.types';

declare class SilentModeNativeModule extends NativeModule<{}> {
  refresh(): NativeQuietStatus;
  silenceFor(minutes: number): NativeQuietStatus;
  cancelManualSilence(): NativeQuietStatus;
  setQuietMode(quietMode: QuietMode): NativeQuietStatus;
  setWindows(serializedWindows: string): NativeQuietStatus;
  openDndAccessSettings(): void;
  openExactAlarmSettings(): void;
}

const nativeModule = requireNativeModule<SilentModeNativeModule>('SilentMode');

function parseStatus({ windowsJson, ...rest }: NativeQuietStatus): QuietStatus {
  return { ...rest, windows: JSON.parse(windowsJson) as QuietWindow[] };
}

export default {
  refresh: () => parseStatus(nativeModule.refresh()),
  silenceFor: (minutes: number) => parseStatus(nativeModule.silenceFor(minutes)),
  cancelManualSilence: () => parseStatus(nativeModule.cancelManualSilence()),
  setQuietMode: (quietMode: QuietMode) => parseStatus(nativeModule.setQuietMode(quietMode)),
  setWindows: (windows: QuietWindow[]) =>
    parseStatus(nativeModule.setWindows(JSON.stringify(windows))),
  openDndAccessSettings: () => nativeModule.openDndAccessSettings(),
  openExactAlarmSettings: () => nativeModule.openExactAlarmSettings(),
};
