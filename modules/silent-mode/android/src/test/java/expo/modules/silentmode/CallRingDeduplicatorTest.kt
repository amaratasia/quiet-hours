package expo.modules.silentmode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallRingDeduplicatorTest {
  @Test
  fun firstRingWithoutNumberIsIgnored() {
    assertFalse(CallRingDeduplicator.isNewRing("RINGING", null, null))
  }

  @Test
  fun ringWithNumberIsHandledOnce() {
    assertTrue(CallRingDeduplicator.isNewRing("RINGING", "07733916287", null))
    assertFalse(CallRingDeduplicator.isNewRing("RINGING", "07733916287", "07733916287"))
  }

  @Test
  fun offhookIsNotARing() {
    assertFalse(CallRingDeduplicator.isNewRing("OFFHOOK", "07733916287", null))
  }

  @Test
  fun rememberedNumberIsForgottenAfterTwoMinutes() {
    assertEquals("07733916287", CallRingDeduplicator.rememberedNumber("07733916287", lastRingAtMillis = 0L, nowMillis = 119_999L))
    assertNull(CallRingDeduplicator.rememberedNumber("07733916287", lastRingAtMillis = 0L, nowMillis = 120_000L))
    assertNull(CallRingDeduplicator.rememberedNumber("07733916287", lastRingAtMillis = null, nowMillis = 0L))
  }
}
