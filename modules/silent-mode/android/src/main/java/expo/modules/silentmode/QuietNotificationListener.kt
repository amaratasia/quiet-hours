package expo.modules.silentmode

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/** Being bound by the system also keeps the process alive while a priority ringtone plays. */
class QuietNotificationListener : NotificationListenerService() {
  override fun onNotificationPosted(notification: StatusBarNotification) {
    if (notification.packageName !in WhatsAppNotifications.PACKAGES) return

    val facts = factsOf(notification)
    QuietStore(applicationContext).lastWhatsAppSeenAtMillis = System.currentTimeMillis()
    Log.d(TAG, "WhatsApp notification posted: ${describe(facts, notification.notification)}")

    val alert = WhatsAppNotifications.toAlert(facts) ?: return
    PriorityAlert.handle(applicationContext, alert, contactNameForNumber = null)
  }

  override fun onNotificationRemoved(notification: StatusBarNotification) {
    if (notification.packageName !in WhatsAppNotifications.PACKAGES) return

    val facts = factsOf(notification)
    if (WhatsAppNotifications.isCall(facts) && PriorityAlert.isPlaying()) {
      PriorityAlert.stopRinging(applicationContext, "whatsapp-call-ended")
    }
  }

  private fun factsOf(notification: StatusBarNotification): NotificationFacts {
    val details = notification.notification
    val extras = details.extras
    return NotificationFacts(
      packageName = notification.packageName,
      category = details.category,
      title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
      isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false),
      conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString(),
      isGroupSummary = details.flags and Notification.FLAG_GROUP_SUMMARY != 0
    )
  }

  private fun describe(facts: NotificationFacts, details: Notification): String =
    "category=${facts.category} hasTitle=${facts.title != null} group=${facts.isGroupConversation} " +
      "conversationTitle=${facts.conversationTitle != null} summary=${facts.isGroupSummary} " +
      "fullScreen=${details.fullScreenIntent != null} ongoing=${details.flags and Notification.FLAG_ONGOING_EVENT != 0} " +
      "extras=${details.extras.keySet().sorted()}"

  private companion object {
    const val TAG = "QuietHours"
  }
}
