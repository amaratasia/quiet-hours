import { StatusBar } from 'expo-status-bar';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

import SilentMode, { QuietMode, QuietStatus, QuietWindow } from './modules/silent-mode';
import { WindowRow } from './src/components/WindowRow';
import { colors } from './src/theme';
import { EVERY_DAY, formatDuration, formatMoment } from './src/time';
import { useQuietStatus } from './src/useQuietStatus';

const SILENCE_PRESET_MINUTES = [30, 60, 120, 180, 240, 480];
const DEFAULT_WINDOW_START_MINUTE = 15 * 60;
const DEFAULT_WINDOW_END_MINUTE = 16 * 60;
const QUIET_MODE_OPTIONS: { quietMode: QuietMode; label: string }[] = [
  { quietMode: 'silent', label: 'Silent' },
  { quietMode: 'vibrate', label: 'Vibrate' },
];

export default function App() {
  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.screen}>
        <StatusBar style="dark" />
        <QuietHoursScreen />
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

function QuietHoursScreen() {
  const { status, silenceFor, cancelManualSilence, setQuietMode, saveWindows } = useQuietStatus();

  const addWindow = () =>
    saveWindows([
      ...status.windows,
      {
        id: String(Date.now()),
        startMinute: DEFAULT_WINDOW_START_MINUTE,
        endMinute: DEFAULT_WINDOW_END_MINUTE,
        weekdays: EVERY_DAY,
        enabled: true,
      },
    ]);

  const updateWindow = (updatedWindow: QuietWindow) =>
    saveWindows(status.windows.map((window) => (window.id === updatedWindow.id ? updatedWindow : window)));

  const deleteWindow = (windowId: string) =>
    saveWindows(status.windows.filter((window) => window.id !== windowId));

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <Text style={styles.title}>Quiet Hours</Text>

      <PermissionBanners status={status} />
      <StatusCard status={status} />

      {status.supported && (
        <>
          <Text style={styles.sectionTitle}>When quiet</Text>
          <QuietModePicker selectedQuietMode={status.quietMode} onSelect={setQuietMode} />

          <Text style={styles.sectionTitle}>Silence for</Text>
          <View style={styles.presetGrid}>
            {SILENCE_PRESET_MINUTES.map((minutes) => (
              <Pressable key={minutes} onPress={() => silenceFor(minutes)} style={styles.presetButton}>
                <Text style={styles.presetText}>{formatDuration(minutes)}</Text>
              </Pressable>
            ))}
          </View>
          {status.manualSilenceUntil !== null && (
            <Pressable onPress={cancelManualSilence} style={styles.secondaryButton}>
              <Text style={styles.secondaryButtonText}>End timer now</Text>
            </Pressable>
          )}
        </>
      )}

      <Text style={styles.sectionTitle}>Schedules</Text>
      <Text style={styles.hint}>Silent inside these windows, normal ringer outside them.</Text>
      {status.windows.map((window) => (
        <WindowRow
          key={window.id}
          window={window}
          onChange={updateWindow}
          onDelete={() => deleteWindow(window.id)}
        />
      ))}
      <Pressable onPress={addWindow} style={styles.secondaryButton}>
        <Text style={styles.secondaryButtonText}>+ Add schedule</Text>
      </Pressable>
    </ScrollView>
  );
}

type QuietModePickerProps = {
  selectedQuietMode: QuietMode;
  onSelect: (quietMode: QuietMode) => void;
};

function QuietModePicker({ selectedQuietMode, onSelect }: QuietModePickerProps) {
  return (
    <View style={styles.quietModePicker}>
      {QUIET_MODE_OPTIONS.map(({ quietMode, label }) => {
        const selected = quietMode === selectedQuietMode;
        return (
          <Pressable
            key={quietMode}
            accessibilityRole="radio"
            accessibilityState={{ selected }}
            onPress={() => onSelect(quietMode)}
            style={[styles.quietModeOption, selected && styles.quietModeOptionSelected]}>
            <Text style={[styles.quietModeText, selected && styles.quietModeTextSelected]}>
              {label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

function StatusCard({ status }: { status: QuietStatus }) {
  const quietHeadline = status.quietMode === 'vibrate' ? 'Vibrate' : 'Silent';
  const headline = status.isSilentNow ? quietHeadline : 'Ringer on';
  const detail = describeNextChange(status);

  return (
    <View style={[styles.statusCard, status.isSilentNow && styles.statusCardSilent]}>
      <Text style={[styles.statusHeadline, status.isSilentNow && styles.statusTextSilent]}>
        {headline}
      </Text>
      <Text style={[styles.statusDetail, status.isSilentNow && styles.statusTextSilent]}>
        {detail}
      </Text>
    </View>
  );
}

function describeNextChange(status: QuietStatus): string {
  if (status.nextChangeAt === null) {
    return status.isSilentNow ? 'Until you turn it off' : 'Nothing scheduled';
  }
  const when = formatMoment(status.nextChangeAt);
  return status.isSilentNow ? `Ringer back on at ${when}` : `Next silent at ${when}`;
}

function PermissionBanners({ status }: { status: QuietStatus }) {
  if (!status.supported) {
    return (
      <Banner
        message="iOS doesn't let apps switch silent mode or Focus on. Schedules saved here are a plan only. Set them up in Settings › Focus › Do Not Disturb › Add Schedule."
      />
    );
  }

  return (
    <>
      {!status.hasDndAccess && (
        <Banner
          message="Allow Do Not Disturb access so Quiet Hours can silence your phone."
          actionLabel="Allow access"
          onAction={SilentMode.openDndAccessSettings}
        />
      )}
      {!status.canScheduleExactAlarms && (
        <Banner
          message="Allow alarms & reminders so silence starts and ends on the exact minute."
          actionLabel="Allow alarms"
          onAction={SilentMode.openExactAlarmSettings}
        />
      )}
    </>
  );
}

type BannerProps = {
  message: string;
  actionLabel?: string;
  onAction?: () => void;
};

function Banner({ message, actionLabel, onAction }: BannerProps) {
  return (
    <View style={styles.banner}>
      <Text style={styles.bannerText}>{message}</Text>
      {actionLabel && onAction && (
        <Pressable onPress={onAction} style={styles.bannerButton}>
          <Text style={styles.bannerButtonText}>{actionLabel}</Text>
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    backgroundColor: colors.background,
    flex: 1,
  },
  content: {
    gap: 12,
    padding: 16,
    paddingBottom: 48,
  },
  title: {
    color: colors.text,
    fontSize: 30,
    fontWeight: '700',
    marginBottom: 4,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '700',
    marginTop: 12,
  },
  hint: {
    color: colors.textMuted,
    marginTop: -6,
  },
  statusCard: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    gap: 4,
    padding: 20,
  },
  statusCardSilent: {
    backgroundColor: colors.silent,
  },
  statusHeadline: {
    color: colors.text,
    fontSize: 28,
    fontWeight: '700',
  },
  statusDetail: {
    color: colors.textMuted,
    fontSize: 16,
  },
  statusTextSilent: {
    color: colors.accentText,
  },
  quietModePicker: {
    backgroundColor: colors.surfaceMuted,
    borderRadius: 12,
    flexDirection: 'row',
    gap: 4,
    padding: 4,
  },
  quietModeOption: {
    alignItems: 'center',
    borderRadius: 9,
    flex: 1,
    paddingVertical: 12,
  },
  quietModeOptionSelected: {
    backgroundColor: colors.accent,
  },
  quietModeText: {
    color: colors.textMuted,
    fontSize: 16,
    fontWeight: '600',
  },
  quietModeTextSelected: {
    color: colors.accentText,
  },
  presetGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  presetButton: {
    alignItems: 'center',
    backgroundColor: colors.accent,
    borderRadius: 12,
    flexBasis: '31%',
    flexGrow: 1,
    paddingVertical: 14,
  },
  presetText: {
    color: colors.accentText,
    fontSize: 17,
    fontWeight: '700',
  },
  secondaryButton: {
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: 12,
    paddingVertical: 14,
  },
  secondaryButtonText: {
    color: colors.accent,
    fontSize: 16,
    fontWeight: '600',
  },
  banner: {
    backgroundColor: colors.warningBackground,
    borderRadius: 12,
    gap: 10,
    padding: 14,
  },
  bannerText: {
    color: colors.warningText,
    fontSize: 15,
  },
  bannerButton: {
    alignSelf: 'flex-start',
    backgroundColor: colors.warningText,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  bannerButtonText: {
    color: colors.warningBackground,
    fontWeight: '600',
  },
});
