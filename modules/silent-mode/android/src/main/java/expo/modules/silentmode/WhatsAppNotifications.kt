package expo.modules.silentmode

data class NotificationFacts(
  val packageName: String,
  val category: String?,
  val title: String?,
  val isGroupConversation: Boolean,
  val conversationTitle: String?,
  val isGroupSummary: Boolean,
  val lastMessageSender: String? = null,
  val onlyAlertOnce: Boolean = false,
  val hasFullScreenIntent: Boolean = false
)

object WhatsAppNotifications {
  val PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
  private const val CATEGORY_CALL = "call"

  fun toAlert(facts: NotificationFacts): IncomingAlert? {
    if (facts.packageName !in PACKAGES || facts.isGroupSummary || isGroupChat(facts)) return null
    if (facts.onlyAlertOnce || isAnsweredOrEndedCall(facts)) return null

    val senderName = senderNameOf(facts) ?: return null
    return if (isCall(facts)) {
      IncomingAlert.WhatsAppCall(senderName)
    } else {
      IncomingAlert.WhatsAppMessage(senderName)
    }
  }

  fun isCall(facts: NotificationFacts): Boolean = facts.category == CATEGORY_CALL

  /** WhatsApp swaps the ringing notification for an ongoing one, without a full-screen intent, once answered. */
  fun isAnsweredOrEndedCall(facts: NotificationFacts): Boolean = isCall(facts) && !facts.hasFullScreenIntent

  fun isNewCallPost(notificationKey: String, handledCallKeys: Set<String>): Boolean =
    notificationKey !in handledCallKeys

  private fun senderNameOf(facts: NotificationFacts): String? =
    listOf(facts.lastMessageSender, facts.title)
      .firstNotNullOfOrNull { name -> name?.trim()?.takeIf { it.isNotEmpty() } }

  private fun isGroupChat(facts: NotificationFacts): Boolean =
    facts.isGroupConversation || facts.conversationTitle != null
}
