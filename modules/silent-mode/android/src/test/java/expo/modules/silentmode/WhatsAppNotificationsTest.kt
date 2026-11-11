package expo.modules.silentmode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WhatsAppNotificationsTest {
  private fun facts(
    category: String? = null,
    title: String? = "Mum",
    group: Boolean = false,
    conversationTitle: String? = null,
    summary: Boolean = false,
    packageName: String = "com.whatsapp"
  ) = NotificationFacts(packageName, category, title, group, conversationTitle, summary)

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
}
