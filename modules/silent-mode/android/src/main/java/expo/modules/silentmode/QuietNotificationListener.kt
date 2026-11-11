package expo.modules.silentmode

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/** Being bound by the system also keeps the process alive while a priority ringtone plays. */
class QuietNotificationListener : NotificationListenerService() {
  private val handledCallKeys = mutableSetOf<String>()

  override fun onNotificationPosted(notification: StatusBarNotification) {
    if (notification.packageName !in WhatsAppNotifications.PACKAGES) return

    val facts = factsOf(notification)
    QuietStore(applicationContext).lastWhatsAppSeenAtMillis = System.currentTimeMillis()
    Log.d(TAG, "WhatsApp notification posted: key=${notification.key} ${describe(facts, notification.notification)}")

    if (WhatsAppNotifications.isAnsweredOrEndedCall(facts)) {
      stopRingingIfAlertStored("whatsapp-call-answered")
      return
    }
    if (WhatsAppNotifications.isCall(facts)) {
      if (!WhatsAppNotifications.isNewCallPost(notification.key, handledCallKeys)) return
      handledCallKeys += notification.key
    }

    val alert = WhatsAppNotifications.toAlert(facts) ?: return
    PriorityAlert.handle(applicationContext, alert, contactNameForNumber = null)
  }

  override fun onNotificationRemoved(notification: StatusBarNotification) {
    if (notification.packageName !in WhatsAppNotifications.PACKAGES) return

    val wasHandledCall = handledCallKeys.remove(notification.key)
    if (wasHandledCall) stopRingingIfAlertStored("whatsapp-call-ended")
  }

  private fun stopRingingIfAlertStored(reason: String) {
    val alertUntil = QuietStore(applicationContext).priorityAlertUntilMillis
    if (RingerRestore.shouldStopPriorityAlert(PriorityAlert.isPlaying(), alertUntil, System.currentTimeMillis())) {
      PriorityAlert.stopRinging(applicationContext, reason, AlertSource.WHATSAPP_CALL)
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
      isGroupSummary = details.flags and Notification.FLAG_GROUP_SUMMARY != 0,
      lastMessageSender = lastMessageSenderOf(extras),
      onlyAlertOnce = details.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0,
      hasFullScreenIntent = details.fullScreenIntent != null
    )
  }

  @Suppress("DEPRECATION")
  private fun lastMessageSenderOf(extras: Bundle): String? {
    val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null
    val lastMessage = messages.lastOrNull() as? Bundle ?: return null
    return lastMessage.getCharSequence(MESSAGE_SENDER_KEY)?.toString()
  }

  private fun describe(facts: NotificationFacts, details: Notification): String =
    "category=${facts.category} hasTitle=${facts.title != null} hasSender=${facts.lastMessageSender != null} " +
      "group=${facts.isGroupConversation} conversationTitle=${facts.conversationTitle != null} " +
      "summary=${facts.isGroupSummary} onlyAlertOnce=${facts.onlyAlertOnce} fullScreen=${facts.hasFullScreenIntent} " +
      "ongoing=${details.flags and Notification.FLAG_ONGOING_EVENT != 0} extras=${details.extras.keySet().sorted()}"

  private companion object {
    const val TAG = "QuietHours"
    const val MESSAGE_SENDER_KEY = "sender"
  }
}
