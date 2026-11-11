import { Pressable, StyleSheet, Switch, Text, View } from 'react-native';

import type { QuietWindow } from '../../modules/silent-mode';
import { colors } from '../theme';
import { WEEKDAY_LETTERS } from '../time';
import { TimeField } from './TimeField';

type WindowRowProps = {
  window: QuietWindow;
  onChange: (updatedWindow: QuietWindow) => void;
  onDelete: () => void;
};

export function WindowRow({ window, onChange, onDelete }: WindowRowProps) {
  const toggleWeekday = (weekday: number) => {
    const weekdays = window.weekdays.includes(weekday)
      ? window.weekdays.filter((day) => day !== weekday)
      : [...window.weekdays, weekday].sort();
    onChange({ ...window, weekdays });
  };

  return (
    <View style={[styles.card, !window.enabled && styles.cardDisabled]}>
      <View style={styles.timeRow}>
        <TimeField
          label="Silence starts"
          minuteOfDay={window.startMinute}
          onChange={(startMinute) => onChange({ ...window, startMinute })}
        />
        <Text style={styles.toText}>to</Text>
        <TimeField
          label="Silence ends"
          minuteOfDay={window.endMinute}
          onChange={(endMinute) => onChange({ ...window, endMinute })}
        />
        <View style={styles.spacer} />
        <Switch
          accessibilityLabel="Schedule enabled"
          value={window.enabled}
          onValueChange={(enabled) => onChange({ ...window, enabled })}
        />
      </View>

      <View style={styles.weekdayRow}>
        {WEEKDAY_LETTERS.map((letter, weekday) => {
          const selected = window.weekdays.includes(weekday);
          return (
            <Pressable
              key={weekday}
              accessibilityLabel={`Weekday ${weekday}`}
              accessibilityState={{ selected }}
              onPress={() => toggleWeekday(weekday)}
              style={[styles.weekday, selected && styles.weekdaySelected]}>
              <Text style={[styles.weekdayText, selected && styles.weekdayTextSelected]}>
                {letter}
              </Text>
            </Pressable>
          );
        })}
        <View style={styles.spacer} />
        <Pressable accessibilityLabel="Delete schedule" onPress={onDelete} hitSlop={8}>
          <Text style={styles.deleteText}>Delete</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: 14,
    gap: 12,
    padding: 14,
  },
  cardDisabled: {
    opacity: 0.55,
  },
  timeRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 8,
  },
  toText: {
    color: colors.textMuted,
  },
  spacer: {
    flex: 1,
  },
  weekdayRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 6,
  },
  weekday: {
    alignItems: 'center',
    backgroundColor: colors.surfaceMuted,
    borderRadius: 16,
    height: 32,
    justifyContent: 'center',
    width: 32,
  },
  weekdaySelected: {
    backgroundColor: colors.accent,
  },
  weekdayText: {
    color: colors.textMuted,
    fontWeight: '600',
  },
  weekdayTextSelected: {
    color: colors.accentText,
  },
  deleteText: {
    color: colors.danger,
    fontWeight: '600',
  },
});
