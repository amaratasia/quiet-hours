package expo.modules.silentmode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class QuietAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    QuietController.reconcile(context, "alarm")
  }
}

/** Alarms are wiped on reboot and go stale when the clock or time zone changes, so re-arm them. */
class QuietSystemEventReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    QuietController.reconcile(context, intent.action ?: "system-event")
  }
}
