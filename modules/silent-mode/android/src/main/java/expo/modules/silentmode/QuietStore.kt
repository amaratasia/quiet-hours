package expo.modules.silentmode

import android.content.Context
import android.content.SharedPreferences

class QuietStore(context: Context) {
  private val preferences: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  var windows: List<QuietWindow>
    get() = QuietWindow.listFromJson(preferences.getString(KEY_WINDOWS, "[]") ?: "[]")
    set(value) = preferences.edit().putString(KEY_WINDOWS, QuietWindow.listToJson(value)).apply()

  var manualSilenceUntilMillis: Long?
    get() = preferences.getLong(KEY_MANUAL_UNTIL, NO_VALUE).takeIf { it != NO_VALUE }
    set(value) = preferences.edit().apply {
      if (value == null) remove(KEY_MANUAL_UNTIL) else putLong(KEY_MANUAL_UNTIL, value)
    }.apply()

  var quietMode: QuietMode
    get() = QuietMode.fromKey(preferences.getString(KEY_QUIET_MODE, null))
    set(value) = preferences.edit().putString(KEY_QUIET_MODE, value.key).apply()

  /** True only when this app turned Do Not Disturb on, so it never switches off the user's own DND. */
  var silencedByApp: Boolean
    get() = preferences.getBoolean(KEY_SILENCED_BY_APP, false)
    set(value) = preferences.edit().putBoolean(KEY_SILENCED_BY_APP, value).apply()

  var importantPeople: List<ImportantPerson>
    get() = ImportantPerson.listFromJson(preferences.getString(KEY_IMPORTANT_PEOPLE, "[]") ?: "[]")
    set(value) = preferences.edit().putString(KEY_IMPORTANT_PEOPLE, ImportantPerson.listToJson(value)).apply()

  var repeatCallerSettings: RepeatCallerSettings
    get() = RepeatCallerSettings.of(
      preferences.getBoolean(KEY_REPEAT_CALLERS_ENABLED, false),
      preferences.getInt(KEY_REPEAT_CALLERS_THRESHOLD, RepeatCallerSettings.DEFAULT_THRESHOLD)
    )
    set(value) = preferences.edit()
      .putBoolean(KEY_REPEAT_CALLERS_ENABLED, value.enabled)
      .putInt(KEY_REPEAT_CALLERS_THRESHOLD, value.threshold)
      .apply()

  var recentCalls: List<RecentCall>
    get() = RepeatCallers.listFromJson(preferences.getString(KEY_RECENT_CALLS, "[]") ?: "[]")
    set(value) = preferences.edit().putString(KEY_RECENT_CALLS, RepeatCallers.listToJson(value)).apply()

  var priorityAlertUntilMillis: Long?
    get() = readOptionalLong(KEY_PRIORITY_ALERT_UNTIL)
    set(value) = writeOptionalLong(KEY_PRIORITY_ALERT_UNTIL, value)

  var lastWhatsAppSeenAtMillis: Long?
    get() = readOptionalLong(KEY_LAST_WHATSAPP_SEEN_AT)
    set(value) = writeOptionalLong(KEY_LAST_WHATSAPP_SEEN_AT, value)

  var lastRingNumber: String?
    get() = preferences.getString(KEY_LAST_RING_NUMBER, null)
    set(value) = preferences.edit().putString(KEY_LAST_RING_NUMBER, value).apply()

  var lastRingAtMillis: Long?
    get() = readOptionalLong(KEY_LAST_RING_AT)
    set(value) = writeOptionalLong(KEY_LAST_RING_AT, value)

  private fun readOptionalLong(key: String): Long? =
    preferences.getLong(key, NO_VALUE).takeIf { it != NO_VALUE }

  private fun writeOptionalLong(key: String, value: Long?) = preferences.edit().apply {
    if (value == null) remove(key) else putLong(key, value)
  }.apply()

  private companion object {
    const val PREFERENCES_NAME = "quiet_hours"
    const val KEY_WINDOWS = "windows"
    const val KEY_MANUAL_UNTIL = "manual_until_millis"
    const val KEY_SILENCED_BY_APP = "silenced_by_app"
    const val KEY_QUIET_MODE = "quiet_mode"
    const val KEY_IMPORTANT_PEOPLE = "important_people"
    const val KEY_REPEAT_CALLERS_ENABLED = "repeat_callers_enabled"
    const val KEY_REPEAT_CALLERS_THRESHOLD = "repeat_callers_threshold"
    const val KEY_RECENT_CALLS = "recent_calls"
    const val KEY_PRIORITY_ALERT_UNTIL = "priority_alert_until_millis"
    const val KEY_LAST_WHATSAPP_SEEN_AT = "last_whatsapp_seen_at"
    const val KEY_LAST_RING_NUMBER = "last_ring_number"
    const val KEY_LAST_RING_AT = "last_ring_at_millis"
    const val NO_VALUE = -1L
  }
}
