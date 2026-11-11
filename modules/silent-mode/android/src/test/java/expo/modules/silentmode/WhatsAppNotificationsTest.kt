package expo.modules.silentmode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppNotificationsTest {
  private fun facts(
    category: String? = null,
    title: String? = "Mum",
    group: Boolean = false,
    conversationTitle: String? = null,
    summary: Boolean = false,
    packageName: String = "com.whatsapp",
    lastMessageSender: String? = null,
    onlyAlertOnce: Boolean = false,
    hasFullScreenIntent: Boolean = true
  ) = NotificationFacts(
    packageName,
    category,
    title,
    group,
    conversationTitle,
    summary,
    lastMessageSender = lastMessageSender,
    onlyAlertOnce = onlyAlertOnce,
    hasFullScreenIntent = hasFullScreenIntent
  )

  @Test
  fun oneToOneCallBecomesWhatsAppCall() {
    assertEquals(IncomingAlert.WhatsAppCall("Mum"), WhatsAppNotifications.toAlert(facts(category = "call")))
  }

  @Test
  fun oneToOneMessageBecomesWhatsAppMessage() {
    assertEquals(IncomingAlert.WhatsAppMessage("Mum"), WhatsAppNotifications.toAlert(facts(category = "msg")))
  }

  @Test
  fun businessAppIsIncluded() {
    assertNotNull(WhatsAppNotifications.toAlert(facts(packageName = "com.whatsapp.w4b")))
  }

  @Test
  fun groupMessageIsIgnored() {
    assertNull(WhatsAppNotifications.toAlert(facts(group = true)))
  }

  @Test
  fun conversationTitleMeansGroup() {
    assertNull(WhatsAppNotifications.toAlert(facts(conversationTitle = "Family")))
  }

  @Test
  fun groupSummaryIsIgnored() {
    assertNull(WhatsAppNotifications.toAlert(facts(summary = true)))
  }

  @Test
  fun blankTitleIsIgnored() {
    assertNull(WhatsAppNotifications.toAlert(facts(title = "  ")))
  }

  @Test
  fun otherAppsAreIgnored() {
    assertNull(WhatsAppNotifications.toAlert(facts(packageName = "org.telegram.messenger")))
  }

  @Test
  fun messageSenderIsPreferredOverTitle() {
    val alert = WhatsAppNotifications.toAlert(facts(category = "msg", title = "Mum (2 messages)", lastMessageSender = "Mum"))

    assertEquals(IncomingAlert.WhatsAppMessage("Mum"), alert)
  }

  @Test
  fun quietRepostIsIgnored() {
    assertNull(WhatsAppNotifications.toAlert(facts(category = "msg", onlyAlertOnce = true)))
  }

  @Test
  fun callWithoutFullScreenIntentIsNotAnIncomingCall() {
    assertNull(WhatsAppNotifications.toAlert(facts(category = "call", hasFullScreenIntent = false)))
    assertTrue(WhatsAppNotifications.isAnsweredOrEndedCall(facts(category = "call", hasFullScreenIntent = false)))
    assertFalse(WhatsAppNotifications.isAnsweredOrEndedCall(facts(category = "call", hasFullScreenIntent = true)))
  }

  @Test
  fun updateOfAnAlreadyHandledCallIsNotNew() {
    assertTrue(WhatsAppNotifications.isNewCallPost("call-1", handledCallKeys = emptySet()))
    assertFalse(WhatsAppNotifications.isNewCallPost("call-1", handledCallKeys = setOf("call-1")))
  }
}
