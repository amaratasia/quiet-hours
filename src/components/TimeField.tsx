import DateTimePicker, { DateTimePickerAndroid } from '@react-native-community/datetimepicker';
import { Platform, Pressable, StyleSheet, Text } from 'react-native';

import { colors } from '../theme';
import { dateToMinuteOfDay, formatMinuteOfDay, minuteOfDayToDate } from '../time';

type TimeFieldProps = {
  label: string;
  minuteOfDay: number;
  onChange: (minuteOfDay: number) => void;
};

export function TimeField({ label, minuteOfDay, onChange }: TimeFieldProps) {
  if (Platform.OS === 'ios') {
    return (
      <DateTimePicker
        accessibilityLabel={label}
        mode="time"
        display="compact"
        value={minuteOfDayToDate(minuteOfDay)}
        onValueChange={(_event, date) => onChange(dateToMinuteOfDay(date))}
      />
    );
  }

  const openAndroidPicker = () =>
    DateTimePickerAndroid.open({
      mode: 'time',
      value: minuteOfDayToDate(minuteOfDay),
      onValueChange: (_event, date) => onChange(dateToMinuteOfDay(date)),
    });

  return (
    <Pressable accessibilityLabel={label} onPress={openAndroidPicker} style={styles.field}>
      <Text style={styles.fieldText}>{formatMinuteOfDay(minuteOfDay)}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  field: {
    backgroundColor: colors.surfaceMuted,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  fieldText: {
    color: colors.text,
    fontSize: 17,
    fontVariant: ['tabular-nums'],
    fontWeight: '600',
  },
});
