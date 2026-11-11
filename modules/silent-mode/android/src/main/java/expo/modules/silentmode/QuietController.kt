package expo.modules.silentmode

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log

enum class QuietMode(val key: String, val ringerMode: Int) {
  SILENT("silent", AudioManager.RINGER_MODE_SILENT),
  VIBRATE("vibrate", AudioManager.RINGER_MODE_VIBRATE);

  companion object {
    fun fromKey(key: String?): QuietMode = entries.firstOrNull { it.key == key } ?: SILENT
  }
}

data class QuietStatus(
  val quietMode: QuietMode,
  val hasDndAccess: Boolean,
  val canScheduleExactAlarms: Boolean,
  val isSilentNow: Boolean,
  val manualSilenceUntilMillis: Long?,
  val nextChangeAtMillis: Long?,
  val windows: List<QuietWindow>
)

object QuietController {
  private const val TAG = "QuietHours"
  private const val ALARM_REQUEST_CODE = 4711

  fun reconcile(context: Context, reason: String): QuietStatus {
    val appContext = context.applicationContext
    val store = QuietStore(appContext)
    val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    val audioManager = appContext.getSystemService(AudioManager::class.java)
    val nowMillis = System.currentTimeMillis()

    clearExpiredManualSilence(store, nowMillis)
    clearExpiredPriorityAlert(store, nowMillis)

    val windows = store.windows
    val manualUntil = store.manualSilenceUntilMillis
    val shouldBeSilent = SilenceSchedule.isSilentAt(nowMillis, windows, manualUntil)
    val hasDndAccess = notificationManager.isNotificationPolicyAccessGranted

    val priorityAlertActive = RingerRestore.isPriorityAlertActive(store.priorityAlertUntilMillis, nowMillis)

    if (hasDndAccess) {
      applySilence(audioManager, store, shouldBeSilent, priorityAlertActive, store.quietMode)
    } else {
      Log.w(TAG, "reconcile($reason): Do Not Disturb access not granted; cannot change ringer state")
    }

    val nextBoundary = SilenceSchedule.nextBoundaryAfter(nowMillis, windows, manualUntil)
    val wakeUpAt = earliestWakeUp(nextBoundary, store.priorityAlertUntilMillis, nowMillis)
    val canScheduleExact = scheduleWakeUp(appContext, wakeUpAt)

    Log.i(
      TAG,
      "reconcile($reason): shouldBeSilent=$shouldBeSilent dndAccess=$hasDndAccess " +
        "ringerMode=${audioManager.ringerMode} manualUntil=$manualUntil " +
        "windows=${windows.size} priorityAlertActive=$priorityAlertActive " +
        "nextWakeUp=$wakeUpAt exact=$canScheduleExact"
    )

    return QuietStatus(
      quietMode = store.quietMode,
      hasDndAccess = hasDndAccess,
      canScheduleExactAlarms = canScheduleExact,
      isSilentNow = shouldBeSilent,
      manualSilenceUntilMillis = store.manualSilenceUntilMillis,
      nextChangeAtMillis = SilenceSchedule.nextStateChangeAfter(nowMillis, windows, manualUntil),
      windows = windows
    )
  }

  fun switchQuietMode(context: Context, quietMode: QuietMode) {
    val appContext = context.applicationContext
    val store = QuietStore(appContext)
    store.quietMode = quietMode

    val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    if (store.silencedByApp && notificationManager.isNotificationPolicyAccessGranted) {
      appContext.getSystemService(AudioManager::class.java).ringerMode = quietMode.ringerMode
      Log.i(TAG, "Switched active quiet period to ${quietMode.key}")
    }
  }

  private fun clearExpiredManualSilence(store: QuietStore, nowMillis: Long) {
    val manualUntil = store.manualSilenceUntilMillis ?: return
    if (manualUntil <= nowMillis) store.manualSilenceUntilMillis = null
  }

  private fun clearExpiredPriorityAlert(store: QuietStore, nowMillis: Long) {
    val alertUntil = store.priorityAlertUntilMillis ?: return
    if (alertUntil <= nowMillis) store.priorityAlertUntilMillis = null
  }

  /** While a priority alert plays, also wake at its expiry so the ringer goes quiet even if the process died. */
  private fun earliestWakeUp(nextBoundary: Long?, priorityAlertUntil: Long?, nowMillis: Long): Long? {
    val pendingAlertExpiry = priorityAlertUntil?.takeIf { it > nowMillis }
    return listOfNotNull(nextBoundary, pendingAlertExpiry).minOrNull()
  }

  private fun applySilence(
    audioManager: AudioManager,
    store: QuietStore,
    shouldBeSilent: Boolean,
    priorityAlertActive: Boolean,
    quietMode: QuietMode
  ) {
    val ringerIsOn = audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL

    if (RingerRestore.shouldApplyQuiet(shouldBeSilent, priorityAlertActive) && ringerIsOn) {
      audioManager.ringerMode = quietMode.ringerMode
      store.silencedByApp = true
      Log.i(TAG, "Set ringer to ${quietMode.key}")
    } else if (!shouldBeSilent && store.silencedByApp) {
      audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
      store.silencedByApp = false
      Log.i(TAG, "Set ringer back to normal")
    }
  }

  /** Returns whether the wake-up can fire at the exact minute rather than when the OS batches it. */
  private fun scheduleWakeUp(context: Context, triggerAtMillis: Long?): Boolean {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val wakeUpIntent = PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      Intent(context, QuietAlarmReceiver::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    if (triggerAtMillis == null) {
      alarmManager.cancel(wakeUpIntent)
    } else if (canScheduleExact) {
      alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, wakeUpIntent)
    } else {
      Log.w(TAG, "Exact alarm permission missing; wake-up at $triggerAtMillis may run late")
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, wakeUpIntent)
    }
    return canScheduleExact
  }
}
