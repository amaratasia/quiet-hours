import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors } from '../theme';

type BannerAction = {
  label: string;
  onPress: () => void;
};

type BannerProps = {
  message: string;
  actions?: BannerAction[];
};

export function Banner({ message, actions = [] }: BannerProps) {
  return (
    <View style={styles.banner}>
      <Text style={styles.bannerText}>{message}</Text>
      {actions.length > 0 && (
        <View style={styles.actionRow}>
          {actions.map(({ label, onPress }) => (
            <Pressable key={label} onPress={onPress} style={styles.bannerButton}>
              <Text style={styles.bannerButtonText}>{label}</Text>
            </Pressable>
          ))}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
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
  actionRow: {
    flexDirection: 'row',
    gap: 8,
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
