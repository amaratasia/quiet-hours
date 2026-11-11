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

  private companion object {
    const val PREFERENCES_NAME = "quiet_hours"
    const val KEY_WINDOWS = "windows"
    const val KEY_MANUAL_UNTIL = "manual_until_millis"
    const val KEY_SILENCED_BY_APP = "silenced_by_app"
    const val KEY_QUIET_MODE = "quiet_mode"
    const val NO_VALUE = -1L
  }
}
