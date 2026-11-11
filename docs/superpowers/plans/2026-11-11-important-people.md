# Important People and Repeat Callers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** While Quiet Hours has the phone silenced, let phone calls and WhatsApp calls and messages from chosen contacts make a sound. Also let a caller who calls X times within 5 minutes ring.

**Architecture:** Everything native lives in the existing Kotlin module `modules/silent-mode`. A `PHONE_STATE` broadcast receiver and a WhatsApp `NotificationListenerService` both feed one pure decision function (`BreakThroughPolicy`). When it says yes, `PriorityAlert` switches the ringer to normal, plays the sound itself, then hands back to `QuietController.reconcile`. The bound notification listener keeps the app process alive while a ringtone plays. For that reason Notification access is required for both phone calls and WhatsApp (decided in the plan, see Task 0).

**Tech Stack:** Expo SDK 58, React Native 0.88, TypeScript, Kotlin (Expo Modules API), JUnit 4, `expo-contacts` (`Contact.presentPicker()`), React Native `PermissionsAndroid`.

**Spec:** `docs/superpowers/specs/2026-11-11-important-people-design.md`

## Global Constraints

- Android only. On iOS the new sections are hidden (`status.supported === false`), and the iOS stub returns defaults.
- Never block, reject or delay a call. The app only listens.
- Do not use `CallScreeningService` or `ROLE_CALL_SCREENING` (users run Truecaller).
- Restore the ringer only when the app silenced the phone itself (`QuietStore.silencedByApp`). Priority alerts fire only when `silencedByApp == true`.
- Repeat callers: default threshold 3, range 2–10, window fixed at 5 minutes (300 000 ms). Off by default (my call; the spec gives a default number, not an on/off default).
- WhatsApp packages: `com.whatsapp`, `com.whatsapp.w4b`. One-to-one chats only, never groups.
- Ringing cap: 60 seconds for phone and WhatsApp calls.
- `android/` and `ios/` are generated. Manifest entries go in `modules/silent-mode/android/src/main/AndroidManifest.xml`.
- Add JS packages with `npx expo install`. Read `https://docs.expo.dev/versions/v58.0.0/` before using any Expo API.
- Run before declaring any task done: `npx tsc --noEmit`, `npx expo lint`, and (after `npx expo prebuild -p android` if `android/` is missing) `cd android && ./gradlew :silent-mode:testDebugUnitTest`.
- Work on branch `feat/important-people`. Never use "fix" or "hotfix" in branch names or PR titles.
- Follow the owner's style rules: descriptive names, small functions, no comments that restate code, log what failed and why.

## Review Focus

1. **The same number in different formats**: `+44 7733 916287`, `07733916287`, `447733916287` and `0044 7733 916287` must all match one important person. Pinned in Task 1.
2. **The double `PHONE_STATE` broadcast**: with `READ_CALL_LOG` granted, Android sends `RINGING` twice, once without a number and once with one. A call must be counted once and must start at most one ringtone. Pinned in Task 4.
3. **A scheduled `reconcile()` during a priority ring**: an alarm firing mid-call must not silence it again, and the ringer must return to quiet when the ring ends. If the quiet period ended mid-ring, it must go back to normal. Pinned in Task 3.
4. **Withheld and empty numbers**: they must not count toward repeat callers or match anyone. Pinned in Tasks 1 and 2.
5. **WhatsApp group messages and summary notifications**: a group message whose sender is an important person must stay silent, and WhatsApp's group-summary notification must be ignored. Pinned in Task 5.

---

### Task 0: Throwaway spike on the owner's phone (gate)

**Files:** a scratch branch `spike/important-people`. It is never merged.

Purpose: answer the spec's three risk questions, plus one the plan relies on, before building. The phone must have Truecaller installed and holding call screening.

- [ ] **Step 1:** Add to the module, temporarily: permissions `READ_PHONE_STATE` and `READ_CALL_LOG`, a `PHONE_STATE` receiver that logs `TelephonyManager.EXTRA_STATE` and `EXTRA_INCOMING_NUMBER`, and a `NotificationListenerService` that logs, for `com.whatsapp`, `category`, `EXTRA_TITLE`, `EXTRA_TEXT`, `EXTRA_IS_GROUP_CONVERSATION`, `EXTRA_CONVERSATION_TITLE`, `EXTRA_MESSAGES` sender names, `Notification.FLAG_GROUP_SUMMARY`, and `EXTRA_PEOPLE_LIST`.
- [ ] **Step 2:** On `RINGING` with a number, set the ringer to normal and play `RingtoneManager.getRingtone(context, TYPE_RINGTONE)` with `isLooping = true`. Stop it on `OFFHOOK` or `IDLE`.
- [ ] **Step 3:** Build with `npx expo run:android --variant release`, grant permissions and Notification access, and silence the phone with a 30-minute timer.
- [ ] **Step 4:** Collect evidence with `adb logcat -s QuietHours`. For each of these, record the log lines in a "Spike findings" section appended to the spec:
  1. A call from a saved contact and from an unknown number: does `RINGING` arrive with a number while Truecaller is active?
  2. Is the app's ringtone audible, with the screen off, for 30 seconds or more, and does it stop on answer and on hang-up?
  3. What WhatsApp sends for a one-to-one call, a one-to-one message and a group message.
  4. Is the ringtone still playing 30 seconds in when the receiver started it and Notification access is granted? This is the assumption that the process stays alive.
- [ ] **Step 5:** **Gate.** If 1 or 2 fails, stop and bring the findings to the owner before Task 1. If 3 shows the sender's name is not in the title or in `EXTRA_MESSAGES`, adjust Task 5's parse rules to what the logs show, then continue.

---

### Task 1: Important people model and number matching

**Files:**
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/ImportantPerson.kt`
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/PhoneNumbers.kt`
- Test: `modules/silent-mode/android/src/test/java/expo/modules/silentmode/PhoneNumbersTest.kt`
- Test: `modules/silent-mode/android/src/test/java/expo/modules/silentmode/ImportantPersonTest.kt`

**Interfaces:**
- Produces:
  - `data class ImportantPerson(val id: String, val displayName: String, val phoneNumbers: List<String>)`, with `toJson()`, `companion fromJson(JSONObject)`, `listFromJson(String)`, `listToJson(List<ImportantPerson>)`, following the pattern in `QuietWindow.kt`.
  - `object PhoneNumbers { fun significantDigits(rawNumber: String?): String?; fun sameNumber(first: String?, second: String?): Boolean }`
  - `fun List<ImportantPerson>.findByNumber(number: String?): ImportantPerson?`
  - `fun List<ImportantPerson>.findByName(name: String?): ImportantPerson?`. This is a case-insensitive, trimmed, exact match on `displayName`.

Matching rule. Pure Kotlin, because `PhoneNumberUtils` is not usable in JVM unit tests:

```kotlin
// significantDigits: keep digits only; return null if fewer than 7 remain;
// otherwise return the last 10 digits (or all, if fewer than 10).
// sameNumber: both non-null significant digits; compare the last
// min(10, shorter length) digits.
```

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun ukFormatsMatch() {
  val formats = listOf("+44 7733 916287", "07733916287", "447733916287", "0044 7733 916287", "(07733) 916-287")
  formats.forEach { assertTrue(it, PhoneNumbers.sameNumber("+447733916287", it)) }
}
@Test fun differentNumbersDoNotMatch() = assertFalse(PhoneNumbers.sameNumber("07733916287", "07733916288"))
@Test fun withheldAndShortNumbersNeverMatch() {
  assertNull(PhoneNumbers.significantDigits(null))
  assertNull(PhoneNumbers.significantDigits(""))
  assertNull(PhoneNumbers.significantDigits("Private"))
  assertNull(PhoneNumbers.significantDigits("12345"))
  assertFalse(PhoneNumbers.sameNumber(null, null))
}
@Test fun findsPersonByAnyOfTheirNumbers() {
  val mum = ImportantPerson("1", "Mum", listOf("01234 567890", "+44 7700 900123"))
  assertEquals(mum, listOf(mum).findByNumber("07700900123"))
}
@Test fun findsPersonByNameIgnoringCaseAndSpaces() {
  val mum = ImportantPerson("1", "Mum", emptyList())
  assertEquals(mum, listOf(mum).findByName("  mum "))
  assertNull(listOf(mum).findByName("Mummy"))
}
@Test fun importantPeopleRoundTripThroughJson() {
  val people = listOf(ImportantPerson("1", "Mum", listOf("+447700900123")))
  assertEquals(people, ImportantPerson.listFromJson(ImportantPerson.listToJson(people)))
}
```

- [ ] **Step 2:** Run `cd android && ./gradlew :silent-mode:testDebugUnitTest`. Expected: compile FAIL (unresolved references).
- [ ] **Step 3:** Implement the interfaces above.
- [ ] **Step 4:** Re-run. Expected: PASS, and `SilenceScheduleTest` still passes.
- [ ] **Step 5:** Commit: `feat(silent-mode): add important people model and number matching`

---

### Task 2: Repeat-caller tracking and the break-through decision

**Files:**
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/RepeatCallers.kt`
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/BreakThroughPolicy.kt`
- Test: `modules/silent-mode/android/src/test/java/expo/modules/silentmode/RepeatCallersTest.kt`
- Test: `modules/silent-mode/android/src/test/java/expo/modules/silentmode/BreakThroughPolicyTest.kt`

**Interfaces:**
- Consumes: `ImportantPerson`, `findByNumber`, `findByName`, `PhoneNumbers.significantDigits` (Task 1).
- Produces:
  - `data class RepeatCallerSettings(val enabled: Boolean = false, val threshold: Int = 3)`. `threshold` is clamped to 2..10 on construction via a `companion fun of(enabled, threshold)`.
  - `data class RecentCall(val callerKey: String, val atMillis: Long)`
  - `object RepeatCallers { const val WINDOW_MILLIS = 300_000L; fun record(history: List<RecentCall>, callerKey: String, nowMillis: Long): List<RecentCall>; fun callsInWindow(history: List<RecentCall>, callerKey: String, nowMillis: Long): Int; fun listToJson / listFromJson }`. `record` appends and drops entries older than `WINDOW_MILLIS`.
  - `sealed interface IncomingAlert { data class PhoneCall(val number: String?) ; data class WhatsAppCall(val senderName: String) ; data class WhatsAppMessage(val senderName: String) }`
  - `enum class BreakThroughReason { IMPORTANT_PERSON, REPEAT_CALLER }`
  - `object BreakThroughPolicy { fun callerKey(alert: IncomingAlert, contactNameForNumber: String?): String?; fun decide(alert: IncomingAlert, people: List<ImportantPerson>, settings: RepeatCallerSettings, callsInWindowIncludingThis: Int): BreakThroughReason? }`

Caller key rule (spec: phone and WhatsApp calls from the same person count together). Use `"name:" + lowercase trimmed contactNameForNumber` when the phone number resolves to a saved contact, else `"number:" + significantDigits`. For WhatsApp calls, `"name:" + lowercase trimmed senderName`. `null` for a withheld number. `WhatsAppMessage` never counts toward repeat callers.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun thirdCallWithinFiveMinutesBreaksThrough() {
  var history = emptyList<RecentCall>()
  listOf(0L, 60_000L, 240_000L).forEach { history = RepeatCallers.record(history, "number:7733916287", it) }
  assertEquals(3, RepeatCallers.callsInWindow(history, "number:7733916287", 240_000L))
}
@Test fun callsOlderThanFiveMinutesAreDropped() {
  var history = RepeatCallers.record(emptyList(), "k", 0L)
  history = RepeatCallers.record(history, "k", 300_001L)
  assertEquals(1, RepeatCallers.callsInWindow(history, "k", 300_001L))
}
@Test fun thresholdIsClampedToTwoThroughTen() {
  assertEquals(2, RepeatCallerSettings.of(true, 1).threshold)
  assertEquals(10, RepeatCallerSettings.of(true, 50).threshold)
}
@Test fun phoneAndWhatsAppCallsFromSameContactShareAKey() =
  assertEquals(
    BreakThroughPolicy.callerKey(IncomingAlert.PhoneCall("07733916287"), contactNameForNumber = "Sowmiya"),
    BreakThroughPolicy.callerKey(IncomingAlert.WhatsAppCall(" sowmiya "), contactNameForNumber = null))
@Test fun withheldNumberHasNoKey() =
  assertNull(BreakThroughPolicy.callerKey(IncomingAlert.PhoneCall(null), null))
@Test fun importantPersonBreaksThroughOnFirstCall() { /* decide(PhoneCall(mum's number), [mum], off, 1) == IMPORTANT_PERSON */ }
@Test fun importantPersonMessageBreaksThrough() { /* decide(WhatsAppMessage("Mum"), [mum], off, 0) == IMPORTANT_PERSON */ }
@Test fun strangerBreaksThroughOnlyAtThreshold() { /* settings of(true,3): count 2 -> null, count 3 -> REPEAT_CALLER */ }
@Test fun repeatCallersOffNeverBreaksThroughForStranger() { /* of(false,3), count 9 -> null */ }
@Test fun strangerMessagesNeverBreakThrough() { /* WhatsAppMessage("Bob"), of(true,2), count 5 -> null */ }
```

- [ ] **Step 2:** Run the unit tests. Expected: compile FAIL.
- [ ] **Step 3:** Implement the interfaces above.
- [ ] **Step 4:** Run the unit tests. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(silent-mode): add repeat-caller tracking and break-through policy`

---

### Task 3: Storage, priority alert playback, and reconcile guard

**Files:**
- Modify: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/QuietStore.kt`
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/PriorityAlert.kt`
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/RingerRestore.kt`
- Modify: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/QuietController.kt` (`reconcile`, `applySilence`)
- Test: `modules/silent-mode/android/src/test/java/expo/modules/silentmode/RingerRestoreTest.kt`

**Interfaces:**
- Consumes: Tasks 1–2 types.
- Produces:
  - New `QuietStore` properties: `importantPeople: List<ImportantPerson>`, `repeatCallerSettings: RepeatCallerSettings`, `recentCalls: List<RecentCall>`, `priorityAlertActive: Boolean`, `lastWhatsAppSeenAtMillis: Long?`. Keys: `important_people`, `repeat_callers_enabled`, `repeat_callers_threshold`, `recent_calls`, `priority_alert_active`, `last_whatsapp_seen_at`.
  - `object PriorityAlert { fun startRinging(context: Context, reason: String); fun stopRinging(context: Context, reason: String); fun playMessageSound(context: Context, reason: String); fun handle(context: Context, alert: IncomingAlert, contactNameForNumber: String?) }`
    - `handle` loads the store and returns early unless `silencedByApp`. It records the call when `callerKey` is non-null and the alert is a call, saves `recentCalls`, and calls `BreakThroughPolicy.decide`. On a reason, it starts ringing for calls or plays the message sound for messages. It logs the alert type, the reason or "stays quiet", and the caller key.
    - `startRinging` is a no-op if already ringing. It sets `priorityAlertActive = true`, sets the ringer to `RINGER_MODE_NORMAL`, plays the default ringtone looping (`AudioAttributes.USAGE_NOTIFICATION_RINGTONE`), and posts `stopRinging(context, "cap")` after 60 000 ms on a main-looper `Handler`.
    - `stopRinging` stops the ringtone, cancels the cap, sets `priorityAlertActive = false`, then calls `QuietController.reconcile(context, "priority-alert-ended:$reason")`.
    - `playMessageSound` sets the ringer to normal, plays `TYPE_NOTIFICATION` once, and restores via `reconcile` when playback finishes or after 10 000 ms. The ringtone is held in a private field so it isn't garbage-collected.
  - `object RingerRestore { fun shouldApplyQuiet(shouldBeSilent: Boolean, priorityAlertActive: Boolean): Boolean }`. `QuietController.applySilence` must skip setting quiet while `priorityAlertActive` is true. Restoring to normal when the quiet period ended still runs.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun reconcileDuringPriorityRingDoesNotSilence() = assertFalse(RingerRestore.shouldApplyQuiet(shouldBeSilent = true, priorityAlertActive = true))
@Test fun reconcileAfterPriorityRingSilencesAgain() = assertTrue(RingerRestore.shouldApplyQuiet(shouldBeSilent = true, priorityAlertActive = false))
@Test fun quietPeriodEndingMidRingDoesNotForceQuiet() = assertFalse(RingerRestore.shouldApplyQuiet(shouldBeSilent = false, priorityAlertActive = true))
```

- [ ] **Step 2:** Run the unit tests. Expected: compile FAIL.
- [ ] **Step 3:** Implement the `QuietStore` properties, `RingerRestore`, and the guard in `applySilence`. Keep `silencedByApp` true while ringing, so that when the quiet period ends `reconcile` restores the ringer to normal. Then implement `PriorityAlert`.
- [ ] **Step 4:** Run the unit tests. Expected: PASS, including `SilenceScheduleTest`.
- [ ] **Step 5:** Commit: `feat(silent-mode): play priority alerts without breaking quiet-period restore`

---

### Task 4: Phone call listener

**Files:**
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/IncomingCallReceiver.kt`
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/CallRingDeduplicator.kt`
- Modify: `modules/silent-mode/android/src/main/AndroidManifest.xml`
- Test: `modules/silent-mode/android/src/test/java/expo/modules/silentmode/CallRingDeduplicatorTest.kt`

**Interfaces:**
- Consumes: `PriorityAlert.handle`, `PriorityAlert.stopRinging`, `IncomingAlert.PhoneCall`.
- Produces: `object CallRingDeduplicator { fun isNewRing(state: String, number: String?, lastHandledNumber: String?): Boolean }`. True only for `RINGING` with a non-null number different from `lastHandledNumber`. The receiver resets `lastHandledNumber` on `IDLE`.

Manifest: add `READ_PHONE_STATE`, `READ_CALL_LOG`, `READ_CONTACTS`, plus a receiver `IncomingCallReceiver`, `exported="true"`, with the intent filter `android.intent.action.PHONE_STATE`.

Receiver behaviour:
- **`RINGING` that is a new ring:** resolve `contactNameForNumber` via `ContactsContract.PhoneLookup` (return null and log a warning if `READ_CONTACTS` is missing), then call `PriorityAlert.handle(context, IncomingAlert.PhoneCall(number), name)`.
- **`OFFHOOK` or `IDLE` while a priority alert is active:** call `PriorityAlert.stopRinging(context, state)`.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun firstRingWithoutNumberIsIgnored() = assertFalse(CallRingDeduplicator.isNewRing("RINGING", null, null))
@Test fun ringWithNumberIsHandledOnce() {
  assertTrue(CallRingDeduplicator.isNewRing("RINGING", "07733916287", null))
  assertFalse(CallRingDeduplicator.isNewRing("RINGING", "07733916287", "07733916287"))
}
@Test fun offhookIsNotARing() = assertFalse(CallRingDeduplicator.isNewRing("OFFHOOK", "07733916287", null))
```

- [ ] **Step 2:** Run the unit tests. Expected: compile FAIL.
- [ ] **Step 3:** Implement the deduplicator, the receiver and the manifest entries. Keep `lastHandledNumber` in `QuietStore` (key `last_ring_number`) so it survives a fresh receiver instance.
- [ ] **Step 4:** Run the unit tests. Expected: PASS.
- [ ] **Step 5:** Device check: with an important person saved through `adb shell` or a temporary call to the module, run a 30-minute timer and have that person call. Expected: it rings, stops on answer, and `adb shell dumpsys audio | grep -i "ringer mode"` shows the quiet mode again afterwards. A stranger calling 3 times with repeat callers on: the third call rings.
- [ ] **Step 6:** Commit: `feat(silent-mode): ring for important and repeat phone callers`

---

### Task 5: WhatsApp notification listener

**Files:**
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/WhatsAppNotifications.kt`
- Create: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/QuietNotificationListener.kt`
- Modify: `modules/silent-mode/android/src/main/AndroidManifest.xml`
- Test: `modules/silent-mode/android/src/test/java/expo/modules/silentmode/WhatsAppNotificationsTest.kt`

**Interfaces:**
- Consumes: `PriorityAlert.handle`, `PriorityAlert.stopRinging`, `IncomingAlert.WhatsAppCall`, `IncomingAlert.WhatsAppMessage`.
- Produces:
  - `data class NotificationFacts(val packageName: String, val category: String?, val title: String?, val isGroupConversation: Boolean, val conversationTitle: String?, val isGroupSummary: Boolean)`. Plain data, so it can be tested without Android.
  - `object WhatsAppNotifications { val PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b"); fun toAlert(facts: NotificationFacts): IncomingAlert? }`

Rules for `toAlert`:
- **Return null:** for a package outside `PACKAGES`, a group summary, a group conversation (`isGroupConversation || conversationTitle != null`), or a blank title.
- **`WhatsAppCall(title)`:** when `category == Notification.CATEGORY_CALL` (the string `"call"`).
- **`WhatsAppMessage(title)`:** otherwise.

Adjust these rules to the spike findings from Task 0 if they differ.

Manifest: a `<service android:name="expo.modules.silentmode.QuietNotificationListener" android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" android:exported="true">` with the `android.service.notification.NotificationListenerService` intent filter.

Listener behaviour:
- **`onNotificationPosted`:** build `NotificationFacts` from the extras. For WhatsApp packages, set `lastWhatsAppSeenAtMillis`, then `toAlert` → `PriorityAlert.handle(context, alert, contactNameForNumber = null)`.
- **`onNotificationRemoved`:** for a WhatsApp `call` notification while a priority alert is active, call `stopRinging(context, "whatsapp-call-ended")`.

- [ ] **Step 1: Write failing tests**

```kotlin
private fun facts(category: String? = null, title: String? = "Mum", group: Boolean = false, conversationTitle: String? = null, summary: Boolean = false, pkg: String = "com.whatsapp") =
  NotificationFacts(pkg, category, title, group, conversationTitle, summary)

@Test fun oneToOneCallBecomesWhatsAppCall() = assertEquals(IncomingAlert.WhatsAppCall("Mum"), WhatsAppNotifications.toAlert(facts(category = "call")))
@Test fun oneToOneMessageBecomesWhatsAppMessage() = assertEquals(IncomingAlert.WhatsAppMessage("Mum"), WhatsAppNotifications.toAlert(facts(category = "msg")))
@Test fun businessAppIsIncluded() = assertNotNull(WhatsAppNotifications.toAlert(facts(pkg = "com.whatsapp.w4b")))
@Test fun groupMessageIsIgnored() = assertNull(WhatsAppNotifications.toAlert(facts(group = true)))
@Test fun conversationTitleMeansGroup() = assertNull(WhatsAppNotifications.toAlert(facts(conversationTitle = "Family")))
@Test fun groupSummaryIsIgnored() = assertNull(WhatsAppNotifications.toAlert(facts(summary = true)))
@Test fun otherAppsAreIgnored() = assertNull(WhatsAppNotifications.toAlert(facts(pkg = "org.telegram.messenger")))
```

- [ ] **Step 2:** Run the unit tests. Expected: compile FAIL.
- [ ] **Step 3:** Implement the code above and the manifest entry.
- [ ] **Step 4:** Run the unit tests. Expected: PASS.
- [ ] **Step 5:** Device check:
  - A WhatsApp call from an important person during a timer rings and stops when declined.
  - Their one-to-one message plays one sound.
  - Their message in a group stays silent.
- [ ] **Step 6:** Commit: `feat(silent-mode): let WhatsApp calls and messages from important people through`

---

### Task 6: Module API, setup status and TypeScript bindings

**Files:**
- Modify: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/SilentModeModule.kt`
- Modify: `modules/silent-mode/android/src/main/java/expo/modules/silentmode/QuietController.kt` (`QuietStatus`)
- Modify: `modules/silent-mode/src/SilentMode.types.ts`, `modules/silent-mode/src/SilentModeModule.ts`
- Modify: `modules/silent-mode/ios/SilentModeModule.swift`
- Modify: `modules/silent-mode/android/src/main/AndroidManifest.xml` (add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)

**Interfaces:**
- Consumes: `QuietStore` properties from Task 3.
- Produces, in TypeScript:

```ts
export type ImportantPerson = { id: string; displayName: string; phoneNumbers: string[] };
export type RepeatCallerSettings = { enabled: boolean; threshold: number };
export type BreakThroughSetup = {
  hasNotificationAccess: boolean;
  hasPhoneAccess: boolean;          // READ_PHONE_STATE and READ_CALL_LOG granted
  hasContactsAccess: boolean;
  isBatteryOptimisationIgnored: boolean;
  lastWhatsAppSeenAt: number | null;
};
// NativeQuietStatus gains: importantPeopleJson: string; repeatCallers: RepeatCallerSettings; setup: BreakThroughSetup
// QuietStatus gains: importantPeople: ImportantPerson[] (parsed), repeatCallers, setup
```

  - New module functions, each returning the status except the `open*` ones:
    - `setImportantPeople(serialized: string)`
    - `setRepeatCallers(enabled: boolean, threshold: number)`
    - `openNotificationAccessSettings()`, which opens `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`
    - `openAppDetailsSettings()`, which opens `ACTION_APPLICATION_DETAILS_SETTINGS` for the restricted-settings steps
    - `requestIgnoreBatteryOptimisation()`, which sends `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with `package:` URI
  - The JS wrapper exposes `setImportantPeople(people: ImportantPerson[])` and `setRepeatCallers(settings: RepeatCallerSettings)`.
  - iOS stub: stores `importantPeopleJson` and repeat-caller settings in `UserDefaults`, returns `setup` with all `false` and `null`, and makes the `open*` functions open app settings.

- [ ] **Step 1:** Implement the Kotlin functions and status fields. Use `NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)` for `hasNotificationAccess` and `PowerManager.isIgnoringBatteryOptimizations` for the battery check.
- [ ] **Step 2:** Update the TS types and wrapper, plus the Swift stub.
- [ ] **Step 3:** Run `npx tsc --noEmit` and `cd android && ./gradlew :silent-mode:compileDebugKotlin :silent-mode:testDebugUnitTest`. Expected: no errors, tests PASS.
- [ ] **Step 4:** Commit: `feat(silent-mode): expose important people and setup status to JS`

---

### Task 7: Important people and repeat callers UI

**Files:**
- Create: `src/components/ImportantPeopleSection.tsx`
- Create: `src/components/RepeatCallersSection.tsx`
- Create: `src/components/BreakThroughSetupBanners.tsx`
- Create: `src/useBreakThroughPermissions.ts`
- Modify: `src/useQuietStatus.ts` (add `saveImportantPeople`, `saveRepeatCallers`)
- Modify: `App.tsx` (render the three components under `status.supported`, after "Schedules")
- Modify: `package.json` via `npx expo install expo-contacts`

**Interfaces:**
- Consumes: Task 6 TS API.
- Produces:
  - `useQuietStatus()` additionally returns `saveImportantPeople(people: ImportantPerson[])` and `saveRepeatCallers(settings: RepeatCallerSettings)`.
  - `useBreakThroughPermissions(): { requestPhoneAndContacts: () => Promise<void> }`. This uses React Native `PermissionsAndroid.requestMultiple([READ_PHONE_STATE, READ_CALL_LOG, READ_CONTACTS])`, then refreshes the status.

UI copy (my call; keep wording short):
- **Section "Important people"**, hint: "Their calls and WhatsApp messages still make a sound while quiet."
  - One row per person: the display name, then the first number in muted text, then a "Remove" button styled like the "Delete" button in `WindowRow`.
  - Button "+ Add person": call `Contact.presentPicker()` from `expo-contacts`. Read phones with the instance's phone accessor (see the SDK 58 contacts docs), and store `{ id, displayName, phoneNumbers }`. Ignore cancel (null). Ignore a contact already in the list (same `id`).
  - Two health lines:
    - "Phone calls: Ready" or "Phone calls: Needs phone access"
    - "WhatsApp: Ready", "WhatsApp: Needs notification access" or "WhatsApp: Waiting for a WhatsApp notification". The last one shows when `lastWhatsAppSeenAt === null`.
- **Section "Repeat callers"**:
  - a switch labelled "Ring if someone calls again"
  - a stepper (− / number / +, clamped 2–10)
  - hint: "Rings on the {threshold}th call from the same number within 5 minutes." Show the number with the correct ordinal suffix.
- **Banners**, shown only when `importantPeople.length > 0 || repeatCallers.enabled`, using the existing `Banner` component pattern from `App.tsx`. Move `Banner` to `src/components/Banner.tsx` so both files can use it.
  - No phone or contacts access: "Allow phone and contacts access so important calls can ring." Action: "Allow", which calls `requestPhoneAndContacts`.
  - No notification access: "Allow notification access so Quiet Hours can ring for important people. If Android says the setting is restricted: open App info, tap ⋮, then Allow restricted settings." Actions: "Open settings" (`openNotificationAccessSettings`) and "App info" (`openAppDetailsSettings`). Add an optional second action to `Banner`.
  - Battery optimisation not ignored: "Let Quiet Hours run in the background so it can ring for important calls." Action: "Allow", which calls `requestIgnoreBatteryOptimisation`.

- [ ] **Step 1:** Read `https://docs.expo.dev/versions/v58.0.0/sdk/contacts/` (the picker and phone accessor sections), then run `npx expo install expo-contacts`.
- [ ] **Step 2:** Implement the components and hooks above.
- [ ] **Step 3:** Run `npx tsc --noEmit` and `npx expo lint`. Expected: no errors.
- [ ] **Step 4:** Device check on a release build:
  - Add and remove a person.
  - Toggle repeat callers and step the threshold from 2 to 10 and back. It clamps at both ends.
  - Each banner disappears after its grant.
  - The health lines update after returning from Settings, through the existing `AppState` refresh.
- [ ] **Step 5:** Commit: `feat: add important people and repeat callers settings`

---

### Task 8: README and pre-release

**Files:**
- Modify: `README.md` (feature bullets, Install steps for the new grants including restricted settings and battery, "How it works" lines for both listeners, project layout rows)

- [ ] **Step 1:** Update the README.
  - It must say that the app never blocks calls and works alongside Truecaller.
  - It must list each permission with one line on why it's needed: Phone, Call log, Contacts, Notification access, Battery.
- [ ] **Step 2:** Run `npx tsc --noEmit`, `npx expo lint`, and the Gradle unit tests. Expected: all pass.
- [ ] **Step 3:** Build the APK with `npx expo run:android --variant release`, copy it to `dist/quiet-hours.apk`, and **ask the owner before** publishing a GitHub pre-release `v1.2.0-beta.1` with `gh release create --prerelease`.
- [ ] **Step 4:** Ask the owner to have at least two testers with different phone brands try the pre-release. Mark it latest only after they report back.
- [ ] **Step 5:** Commit: `docs: describe important people and repeat callers`
