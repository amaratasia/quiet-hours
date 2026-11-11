package expo.modules.silentmode

sealed interface IncomingAlert {
  data class PhoneCall(val number: String?) : IncomingAlert
  data class WhatsAppCall(val senderName: String) : IncomingAlert
  data class WhatsAppMessage(val senderName: String) : IncomingAlert
}

enum class BreakThroughReason { IMPORTANT_PERSON, REPEAT_CALLER }

enum class AlertSource { PHONE_CALL, WHATSAPP_CALL, WHATSAPP_MESSAGE }

object BreakThroughPolicy {
  /** Keyed by contact name when known, so phone and WhatsApp calls from one person count together. */
  fun callerKey(alert: IncomingAlert, contactNameForNumber: String?): String? = when (alert) {
    is IncomingAlert.PhoneCall ->
      contactNameForNumber?.let(::nameKey) ?: PhoneNumbers.significantDigits(alert.number)?.let { "number:$it" }
    is IncomingAlert.WhatsAppCall -> nameKey(alert.senderName)
    is IncomingAlert.WhatsAppMessage -> null
  }

  fun sourceOf(alert: IncomingAlert): AlertSource = when (alert) {
    is IncomingAlert.PhoneCall -> AlertSource.PHONE_CALL
    is IncomingAlert.WhatsAppCall -> AlertSource.WHATSAPP_CALL
    is IncomingAlert.WhatsAppMessage -> AlertSource.WHATSAPP_MESSAGE
  }

  fun decide(
    alert: IncomingAlert,
    people: List<ImportantPerson>,
    settings: RepeatCallerSettings,
    callsInWindowIncludingThis: Int
  ): BreakThroughReason? {
    if (findImportantPerson(alert, people) != null) return BreakThroughReason.IMPORTANT_PERSON

    val isRepeatCall = settings.enabled &&
      isIdentifiableCall(alert) &&
      callsInWindowIncludingThis >= settings.threshold
    return if (isRepeatCall) BreakThroughReason.REPEAT_CALLER else null
  }

  private fun findImportantPerson(alert: IncomingAlert, people: List<ImportantPerson>): ImportantPerson? =
    when (alert) {
      is IncomingAlert.PhoneCall -> people.findByNumber(alert.number)
      is IncomingAlert.WhatsAppCall -> findWhatsAppSender(alert.senderName, people)
      is IncomingAlert.WhatsAppMessage -> findWhatsAppSender(alert.senderName, people)
    }

  /** WhatsApp shows an unsaved sender as their phone number instead of a name. */
  private fun findWhatsAppSender(senderName: String, people: List<ImportantPerson>): ImportantPerson? =
    people.findByName(senderName) ?: people.findByNumber(senderName)

  private fun isIdentifiableCall(alert: IncomingAlert): Boolean = when (alert) {
    is IncomingAlert.PhoneCall -> PhoneNumbers.significantDigits(alert.number) != null
    is IncomingAlert.WhatsAppCall -> alert.senderName.isNotBlank()
    is IncomingAlert.WhatsAppMessage -> false
  }

  private fun nameKey(name: String): String? =
    name.trim().lowercase().takeIf { it.isNotEmpty() }?.let { "name:$it" }
}
