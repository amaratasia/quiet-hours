package expo.modules.silentmode

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

data class QuietStatus(
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
    val nowMillis = System.currentTimeMillis()

    clearExpiredManualSilence(store, nowMillis)

    val windows = store.windows
    val manualUntil = store.manualSilenceUntilMillis
    val shouldBeSilent = SilenceSchedule.isSilentAt(nowMillis, windows, manualUntil)
    val hasDndAccess = notificationManager.isNotificationPolicyAccessGranted

    if (hasDndAccess) {
      applySilence(notificationManager, store, shouldBeSilent)
    } else {
      Log.w(TAG, "reconcile($reason): Do Not Disturb access not granted; cannot change ringer state")
    }

    val nextBoundary = SilenceSchedule.nextBoundaryAfter(nowMillis, windows, manualUntil)
    val canScheduleExact = scheduleWakeUp(appContext, nextBoundary)

    Log.i(
      TAG,
      "reconcile($reason): shouldBeSilent=$shouldBeSilent dndAccess=$hasDndAccess " +
        "filter=${notificationManager.currentInterruptionFilter} manualUntil=$manualUntil " +
        "windows=${windows.size} nextWakeUp=$nextBoundary exact=$canScheduleExact"
    )

    return QuietStatus(
      hasDndAccess = hasDndAccess,
      canScheduleExactAlarms = canScheduleExact,
      isSilentNow = shouldBeSilent,
      manualSilenceUntilMillis = store.manualSilenceUntilMillis,
      nextChangeAtMillis = SilenceSchedule.nextStateChangeAfter(nowMillis, windows, manualUntil),
      windows = windows
    )
  }

  private fun clearExpiredManualSilence(store: QuietStore, nowMillis: Long) {
    val manualUntil = store.manualSilenceUntilMillis ?: return
    if (manualUntil <= nowMillis) store.manualSilenceUntilMillis = null
  }

  private fun applySilence(
    notificationManager: NotificationManager,
    store: QuietStore,
    shouldBeSilent: Boolean
  ) {
    val ringerIsOn = notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL

    if (shouldBeSilent && ringerIsOn) {
      notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
      store.silencedByApp = true
      Log.i(TAG, "Turned Do Not Disturb on")
    } else if (!shouldBeSilent && store.silencedByApp) {
      notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
      store.silencedByApp = false
      Log.i(TAG, "Turned Do Not Disturb off")
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
