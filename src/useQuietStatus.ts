import { useCallback, useEffect, useState } from 'react';
import { AppState } from 'react-native';

import SilentMode, { QuietStatus, QuietWindow } from '../modules/silent-mode';

const STATUS_POLL_INTERVAL_MS = 30_000;

export function useQuietStatus() {
  const [status, setStatus] = useState<QuietStatus>(() => SilentMode.refresh());

  const refresh = useCallback(() => setStatus(SilentMode.refresh()), []);

  useEffect(() => {
    const appStateSubscription = AppState.addEventListener('change', (nextState) => {
      if (nextState === 'active') refresh();
    });
    const pollTimer = setInterval(refresh, STATUS_POLL_INTERVAL_MS);

    return () => {
      appStateSubscription.remove();
      clearInterval(pollTimer);
    };
  }, [refresh]);

  return {
    status,
    silenceFor: (minutes: number) => setStatus(SilentMode.silenceFor(minutes)),
    cancelManualSilence: () => setStatus(SilentMode.cancelManualSilence()),
    saveWindows: (windows: QuietWindow[]) => setStatus(SilentMode.setWindows(windows)),
  };
}
