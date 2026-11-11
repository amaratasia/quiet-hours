import { Pressable, StyleSheet, Switch, Text, View } from 'react-native';

import type { RepeatCallerSettings } from '../../modules/silent-mode';
import { colors } from '../theme';

const MIN_THRESHOLD = 2;
const MAX_THRESHOLD = 10;

type RepeatCallersSectionProps = {
  settings: RepeatCallerSettings;
  onChange: (settings: RepeatCallerSettings) => void;
};

export function RepeatCallersSection({ settings, onChange }: RepeatCallersSectionProps) {
  const changeThreshold = (step: number) => {
    const threshold = Math.min(MAX_THRESHOLD, Math.max(MIN_THRESHOLD, settings.threshold + step));
    onChange({ ...settings, threshold });
  };

  return (
    <>
      <Text style={styles.sectionTitle}>Repeat callers</Text>
      <View style={[styles.card, !settings.enabled && styles.cardDisabled]}>
        <View style={styles.row}>
          <Text style={styles.label}>Ring if someone calls again</Text>
          <Switch
            accessibilityLabel="Ring if someone calls again"
            value={settings.enabled}
            onValueChange={(enabled) => onChange({ ...settings, enabled })}
          />
        </View>

        <View style={styles.row}>
          <Pressable
            accessibilityLabel="Fewer calls"
            disabled={settings.threshold <= MIN_THRESHOLD}
            onPress={() => changeThreshold(-1)}
            style={styles.stepButton}>
            <Text style={styles.stepButtonText}>−</Text>
          </Pressable>
          <Text style={styles.thresholdText}>{settings.threshold}</Text>
          <Pressable
            accessibilityLabel="More calls"
            disabled={settings.threshold >= MAX_THRESHOLD}
            onPress={() => changeThreshold(1)}
            style={styles.stepButton}>
            <Text style={styles.stepButtonText}>+</Text>
          </Pressable>
        </View>

        <Text style={styles.hint}>
          Rings on the {formatOrdinal(settings.threshold)} call from the same number within 5 minutes.
        </Text>
      </View>
    </>
  );
}

function formatOrdinal(count: number): string {
  const lastTwoDigits = count % 100;
  if (lastTwoDigits >= 11 && lastTwoDigits <= 13) return `${count}th`;

  const suffixByLastDigit: Record<number, string> = { 1: 'st', 2: 'nd', 3: 'rd' };
  return `${count}${suffixByLastDigit[count % 10] ?? 'th'}`;
}

const styles = StyleSheet.create({
  sectionTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '700',
    marginTop: 12,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: 14,
    gap: 12,
    padding: 14,
  },
  cardDisabled: {
    opacity: 0.55,
  },
  row: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 12,
  },
  label: {
    color: colors.text,
    flex: 1,
    fontSize: 16,
  },
  stepButton: {
    alignItems: 'center',
    backgroundColor: colors.surfaceMuted,
    borderRadius: 18,
    height: 36,
    justifyContent: 'center',
    width: 36,
  },
  stepButtonText: {
    color: colors.accent,
    fontSize: 20,
    fontWeight: '700',
  },
  thresholdText: {
    color: colors.text,
    fontSize: 18,
    fontVariant: ['tabular-nums'],
    fontWeight: '700',
    minWidth: 24,
    textAlign: 'center',
  },
  hint: {
    color: colors.textMuted,
  },
});
