package expo.modules.silentmode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BreakThroughPolicyTest {
  private val mum = ImportantPerson("1", "Mum", listOf("+44 7700 900123"))
  private val repeatCallersOff = RepeatCallerSettings.of(false, 3)
  private val repeatCallersAtThree = RepeatCallerSettings.of(true, 3)

  @Test
  fun phoneAndWhatsAppCallsFromSameContactShareAKey() {
    assertEquals(
      BreakThroughPolicy.callerKey(IncomingAlert.PhoneCall("07733916287"), contactNameForNumber = "Sowmiya"),
      BreakThroughPolicy.callerKey(IncomingAlert.WhatsAppCall(" sowmiya "), contactNameForNumber = null)
    )
  }

  @Test
  fun unsavedNumberIsKeyedByItsDigits() {
    assertEquals(
      "number:733916287",
      BreakThroughPolicy.callerKey(IncomingAlert.PhoneCall("+44 7733 916287"), contactNameForNumber = null)
    )
  }

  @Test
  fun withheldNumberHasNoKey() {
    assertNull(BreakThroughPolicy.callerKey(IncomingAlert.PhoneCall(null), null))
  }

  @Test
  fun messagesHaveNoCallerKey() {
    assertNull(BreakThroughPolicy.callerKey(IncomingAlert.WhatsAppMessage("Mum"), null))
  }

  @Test
  fun importantPersonBreaksThroughOnFirstCall() {
    val reason = BreakThroughPolicy.decide(IncomingAlert.PhoneCall("07700900123"), listOf(mum), repeatCallersOff, 1)

    assertEquals(BreakThroughReason.IMPORTANT_PERSON, reason)
  }

  @Test
  fun importantPersonWhatsAppCallBreaksThrough() {
    val reason = BreakThroughPolicy.decide(IncomingAlert.WhatsAppCall("mum"), listOf(mum), repeatCallersOff, 1)

    assertEquals(BreakThroughReason.IMPORTANT_PERSON, reason)
  }

  @Test
  fun importantPersonMessageBreaksThrough() {
    val reason = BreakThroughPolicy.decide(IncomingAlert.WhatsAppMessage("Mum"), listOf(mum), repeatCallersOff, 0)

    assertEquals(BreakThroughReason.IMPORTANT_PERSON, reason)
  }

  @Test
  fun strangerBreaksThroughOnlyAtThreshold() {
    val strangerCall = IncomingAlert.PhoneCall("07733916287")

    assertNull(BreakThroughPolicy.decide(strangerCall, listOf(mum), repeatCallersAtThree, 2))
    assertEquals(
      BreakThroughReason.REPEAT_CALLER,
      BreakThroughPolicy.decide(strangerCall, listOf(mum), repeatCallersAtThree, 3)
    )
  }

  @Test
  fun repeatCallersOffNeverBreaksThroughForStranger() {
    assertNull(BreakThroughPolicy.decide(IncomingAlert.PhoneCall("07733916287"), listOf(mum), repeatCallersOff, 9))
  }

  @Test
  fun strangerMessagesNeverBreakThrough() {
    val settings = RepeatCallerSettings.of(true, 2)

    assertNull(BreakThroughPolicy.decide(IncomingAlert.WhatsAppMessage("Bob"), listOf(mum), settings, 5))
  }

  @Test
  fun withheldCallerNeverBreaksThrough() {
    assertNull(BreakThroughPolicy.decide(IncomingAlert.PhoneCall(null), listOf(mum), repeatCallersAtThree, 5))
  }

  @Test
  fun unsavedWhatsAppSenderShownAsNumberMatchesByNumber() {
    val reason = BreakThroughPolicy.decide(IncomingAlert.WhatsAppMessage("+44 7700 900123"), listOf(mum), repeatCallersOff, 0)

    assertEquals(BreakThroughReason.IMPORTANT_PERSON, reason)
  }

  @Test
  fun eachAlertHasASource() {
    assertEquals(AlertSource.PHONE_CALL, BreakThroughPolicy.sourceOf(IncomingAlert.PhoneCall("1")))
    assertEquals(AlertSource.WHATSAPP_CALL, BreakThroughPolicy.sourceOf(IncomingAlert.WhatsAppCall("Mum")))
    assertEquals(AlertSource.WHATSAPP_MESSAGE, BreakThroughPolicy.sourceOf(IncomingAlert.WhatsAppMessage("Mum")))
  }
}
