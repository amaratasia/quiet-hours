# Important People and Repeat Callers — Design

## Why

Quiet Hours silences the ringer on a timer or a weekly schedule. Some people may need to reach the owner urgently while the phone is quiet. Their calls and WhatsApp messages must still make a sound. A caller who keeps calling is probably urgent too, even if they are not on the list.

## What it does

1. **Important people.** The owner picks contacts in the app. While the phone is quiet, these people still get through:
   - normal phone calls ring,
   - WhatsApp calls ring,
   - WhatsApp one-to-one messages play the notification sound.
2. **Repeat callers.** If the same number calls X times within 5 minutes, the Xth call rings, even if the caller is not on the list. X can be set in the app. Phone calls and WhatsApp calls from the same person count together.

Outside quiet periods nothing changes.

## Goals

- Calls from important people never stay silent during a quiet period.
- One shared list for all timers and schedules.
- Keep the existing ringer-mode approach. Do not switch to Do Not Disturb. The previous release moved away from DND on purpose.
- The app never blocks or rejects a call.

## Non-goals

- Telegram, Signal and other messaging apps.
- Group chats. A message from an important person in a group stays silent.
- Separate lists per schedule.
- SMS messages (my call: not asked for; easy to add later through the same notification path).
- iOS. iOS cannot do any of this. Hide the new sections on iOS as the existing timer UI is hidden.

## Anti-goals (failure even if it "works")

- The ringer stays on after the priority call or message ends.
- The app turns the ringer to normal when the user set silent or vibrate themselves and the app did not silence the phone.
- A normal phone call is delayed noticeably or sent to voicemail.

## Constraints

- Expo SDK 58, React Native 0.88, Android-first. Native code lives in the local Expo module `modules/silent-mode` (Kotlin). Follow `AGENTS.md`: read the versioned Expo docs before using any Expo API; add packages with `npx expo install`.
- `android/` and `ios/` are generated. Configure manifest entries in the module's `AndroidManifest.xml` or a config plugin, never by hand in `android/`.
- Existing rule in `QuietController`: the app restores the ringer only if it silenced the phone itself (`silencedByApp`). The new feature must keep this rule.

## How it works (decided)

### Normal phone calls

- **Do not use `CallScreeningService`.** Only one app can hold the call-screening role, and users (the owner included) run Truecaller or similar spam blockers that hold it. Taking the role would silently turn their spam blocking off.
- Instead, listen for the phone-state change to "ringing" (`READ_PHONE_STATE`, plus `READ_CALL_LOG` so Android includes the caller's number). This works alongside Truecaller.
- When an incoming call starts ringing while the app has silenced the phone, and the number matches an important person or this is the Xth call from this number in 5 minutes:
  - switch the ringer to normal and **play the default ringtone from the app**, because Android has already decided the call is silent and will not start its own ringtone mid-call,
  - stop the ringtone when the call is answered or ends (state leaves "ringing"), with a 60-second cap,
  - then return to the quiet state (silent or vibrate, whichever the user chose).
- The app only listens. It never blocks, rejects or delays a call.
- Every incoming call with a number is recorded for the repeat-caller count, whether or not it rang. Withheld numbers do not count.

### WhatsApp calls and messages

- A `NotificationListenerService` in the module. The owner grants Notification access once in Settings.
- It watches notifications from WhatsApp (`com.whatsapp`; my call: also `com.whatsapp.w4b` for WhatsApp Business).
- Match the sender to an important person by the contact display name. WhatsApp titles notifications with the name saved in the phone's contacts. Use the `Person` in the notification if present, otherwise the title.
- Skip group conversations.
- A **call** notification (category `call`) from an important person, or the Xth WhatsApp call from that person in 5 minutes: switch the ringer to normal and play the default ringtone until the notification is removed or answered, with a 60-second cap. Then return to quiet.
- A **message** from an important person: switch the ringer to normal, play the default notification sound once, then return to quiet.

### Settings in the app

- **Important people** section: list of picked contacts, "+ Add person" (contact picker), remove button per row.
- **Repeat callers** section: on/off switch and a number X (my call: default 3, range 2–10). The window is fixed at 5 minutes.
- Permission banners in the existing `Banner` style for each missing grant: contacts, phone state and call log, notification access.

### Setup for everyone who installs the APK

The feature ships to everyone who sideloads the APK from GitHub releases, not only the owner. Phones differ, so setup must be guided and the app must show what is actually working.

- **Restricted settings.** On Android 13+, a sideloaded app cannot get Notification access until the user opens **App info › ⋮ › Allow restricted settings**. Show a banner with these steps when notification access is missing.
- **Background limits.** Samsung, Xiaomi, OnePlus and others stop background services. Ask the user to exempt Quiet Hours from battery optimisation, and show a banner when it is not exempt.
- **Health status.** In the Important people section, show one line per part: "Phone calls: ready / needs permission" and "WhatsApp: ready / needs notification access / not seen yet". "Not seen yet" means the listener has not received any WhatsApp notification since setup.
- **Test it.** Release first as a pre-release on GitHub and have at least two other people with different phone brands try it, before marking it latest.

## Known risks — prove with a throwaway spike before building

The spike runs on the owner's Android phone, with Truecaller installed. It is thrown away after. Three questions:

1. With Truecaller holding call screening, does the phone-state "ringing" event arrive with the caller's number?
2. If the app plays the ringtone itself during a silent incoming call, is it audible and does it stop cleanly on answer and hang-up?
3. What do WhatsApp notifications contain for a one-to-one call, a one-to-one message and a group message: title, `Person` name, category, group flag? This decides whether name matching works.

## Left to the builder

- Number matching. Prefer Android's `PhoneNumberUtils` over hand-written comparison, so `+44 7…`, `07…` and `447…` match.
- Phone-state broadcast receiver or `TelephonyCallback` for the ringing and call-end events.
- How to store the list and call history. The existing code uses `SharedPreferences` with JSON (`QuietStore`, `QuietWindow`); follow that.
- How a temporary "ringing for an important person" state coexists with `reconcile()`. A scheduled reconcile during a priority call must not silence it again, and `silencedByApp` must stay true so the quiet period ends normally.
- Contact picker. Check the SDK 58 docs for `expo-contacts` before choosing.
- Unit tests for the pure logic (number matching, repeat-caller counting), in the style of `SilenceScheduleTest.kt`.
- README: describe the feature, the new permissions and why each is needed.
