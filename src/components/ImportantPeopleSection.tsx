import { Contact } from 'expo-contacts';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { BreakThroughSetup, ImportantPerson } from '../../modules/silent-mode';
import { colors } from '../theme';

type ImportantPeopleSectionProps = {
  people: ImportantPerson[];
  setup: BreakThroughSetup;
  onChange: (people: ImportantPerson[]) => void;
};

export function ImportantPeopleSection({ people, setup, onChange }: ImportantPeopleSectionProps) {
  const addPerson = async () => {
    const pickedPerson = await pickImportantPerson();
    if (pickedPerson === null || people.some((person) => person.id === pickedPerson.id)) return;
    onChange([...people, pickedPerson]);
  };

  const removePerson = (personId: string) => onChange(people.filter((person) => person.id !== personId));

  return (
    <>
      <Text style={styles.sectionTitle}>Important people</Text>
      <Text style={styles.hint}>Their calls and WhatsApp messages still make a sound while quiet.</Text>

      {people.map((person) => (
        <View key={person.id} style={styles.personRow}>
          <View style={styles.personDetails}>
            <Text style={styles.personName}>{person.displayName}</Text>
            {person.phoneNumbers.length > 0 && (
              <Text style={styles.personNumber}>{person.phoneNumbers[0]}</Text>
            )}
          </View>
          <Pressable
            accessibilityLabel={`Remove ${person.displayName}`}
            onPress={() => removePerson(person.id)}
            hitSlop={8}>
            <Text style={styles.removeText}>Remove</Text>
          </Pressable>
        </View>
      ))}

      <Pressable onPress={addPerson} style={styles.secondaryButton}>
        <Text style={styles.secondaryButtonText}>+ Add person</Text>
      </Pressable>

      <Text style={styles.healthLine}>Phone calls: {describePhoneCallSetup(setup)}</Text>
      <Text style={styles.healthLine}>WhatsApp: {describeWhatsAppSetup(setup)}</Text>
    </>
  );
}

async function pickImportantPerson(): Promise<ImportantPerson | null> {
  try {
    const contact = await Contact.presentPicker();
    if (contact === null) return null;

    const [displayName, phones] = await Promise.all([contact.getFullName(), contact.getPhones()]);
    const phoneNumbers = phones.flatMap((phone) => (phone.number ? [phone.number] : []));
    return { id: contact.id, displayName, phoneNumbers };
  } catch (error) {
    console.warn('Picking an important person failed', error);
    return null;
  }
}

function describePhoneCallSetup(setup: BreakThroughSetup): string {
  if (!setup.hasPhoneAccess) return 'Needs phone access';
  if (!setup.hasNotificationAccess) return 'Needs notification access';
  return 'Ready';
}

function describeWhatsAppSetup(setup: BreakThroughSetup): string {
  if (!setup.hasNotificationAccess) return 'Needs notification access';
  if (setup.lastWhatsAppSeenAt === null) return 'Waiting for a WhatsApp notification';
  return 'Ready';
}

const styles = StyleSheet.create({
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
  personRow: {
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: 14,
    flexDirection: 'row',
    padding: 14,
  },
  personDetails: {
    flex: 1,
    gap: 2,
  },
  personName: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '600',
  },
  personNumber: {
    color: colors.textMuted,
  },
  removeText: {
    color: colors.danger,
    fontWeight: '600',
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
  healthLine: {
    color: colors.textMuted,
  },
});
