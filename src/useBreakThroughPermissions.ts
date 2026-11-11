import { useCallback } from 'react';
import { PermissionsAndroid } from 'react-native';

const PHONE_AND_CONTACTS_PERMISSIONS = [
  PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
  PermissionsAndroid.PERMISSIONS.READ_CALL_LOG,
  PermissionsAndroid.PERMISSIONS.READ_CONTACTS,
];

export function useBreakThroughPermissions(onPermissionsChanged: () => void) {
  const requestPhoneAndContacts = useCallback(async () => {
    try {
      await PermissionsAndroid.requestMultiple(PHONE_AND_CONTACTS_PERMISSIONS);
    } catch (error) {
      console.warn('Requesting phone and contacts permissions failed', error);
    } finally {
      onPermissionsChanged();
    }
  }, [onPermissionsChanged]);

  return { requestPhoneAndContacts };
}
