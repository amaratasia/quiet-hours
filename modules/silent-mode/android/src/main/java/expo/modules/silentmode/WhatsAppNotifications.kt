package expo.modules.silentmode

data class NotificationFacts(
  val packageName: String,
  val category: String?,
  val title: String?,
  val isGroupConversation: Boolean,
  val conversationTitle: String?,
  val isGroupSummary: Boolean
)

object WhatsAppNotifications {
  val PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
  private const val CATEGORY_CALL = "call"

  fun toAlert(facts: NotificationFacts): IncomingAlert? {
    if (facts.packageName !in PACKAGES || facts.isGroupSummary || isGroupChat(facts)) return null

    val senderName = facts.title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (facts.category == CATEGORY_CALL) {
      IncomingAlert.WhatsAppCall(senderName)
    } else {
      IncomingAlert.WhatsAppMessage(senderName)
    }
  }

  fun isCall(facts: NotificationFacts): Boolean = facts.category == CATEGORY_CALL

  private fun isGroupChat(facts: NotificationFacts): Boolean =
    facts.isGroupConversation || facts.conversationTitle != null
}
