import SilentMode, { QuietStatus } from '../../modules/silent-mode';
import { Banner } from './Banner';

type BreakThroughSetupBannersProps = {
  status: QuietStatus;
  onRequestPhoneAndContacts: () => void;
};

export function BreakThroughSetupBanners({ status, onRequestPhoneAndContacts }: BreakThroughSetupBannersProps) {
  const featureInUse = status.importantPeople.length > 0 || status.repeatCallers.enabled;
  if (!featureInUse) return null;

  const { setup } = status;
  const needsPhoneOrContacts = !setup.hasPhoneAccess || !setup.hasContactsAccess;

  return (
    <>
      {needsPhoneOrContacts && (
        <Banner
          message="Allow phone and contacts access so important calls can ring."
          actions={[{ label: 'Allow', onPress: onRequestPhoneAndContacts }]}
        />
      )}
      {!setup.hasNotificationAccess && (
        <Banner
          message="Allow notification access so Quiet Hours can ring for important people. If Android says the setting is restricted: open App info, tap ⋮, then Allow restricted settings."
          actions={[
            { label: 'Open settings', onPress: SilentMode.openNotificationAccessSettings },
            { label: 'App info', onPress: SilentMode.openAppDetailsSettings },
          ]}
        />
      )}
      {!setup.isBatteryOptimisationIgnored && (
        <Banner
          message="Let Quiet Hours run in the background so it can ring for important calls."
          actions={[{ label: 'Allow', onPress: SilentMode.requestIgnoreBatteryOptimisation }]}
        />
      )}
    </>
  );
}
