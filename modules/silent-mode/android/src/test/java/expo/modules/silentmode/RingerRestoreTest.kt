package expo.modules.silentmode

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingerRestoreTest {
  @Test
  fun reconcileDuringPriorityRingDoesNotSilence() {
    assertFalse(RingerRestore.shouldApplyQuiet(shouldBeSilent = true, priorityAlertActive = true))
  }

  @Test
  fun reconcileAfterPriorityRingSilencesAgain() {
    assertTrue(RingerRestore.shouldApplyQuiet(shouldBeSilent = true, priorityAlertActive = false))
  }

  @Test
  fun quietPeriodEndingMidRingDoesNotForceQuiet() {
    assertFalse(RingerRestore.shouldApplyQuiet(shouldBeSilent = false, priorityAlertActive = true))
  }

  @Test
  fun alertIsActiveOnlyBeforeItsExpiry() {
    assertTrue(RingerRestore.isPriorityAlertActive(untilMillis = 60_000L, nowMillis = 59_999L))
    assertFalse(RingerRestore.isPriorityAlertActive(untilMillis = 60_000L, nowMillis = 60_000L))
  }

  @Test
  fun noAlertIsNotActive() {
    assertFalse(RingerRestore.isPriorityAlertActive(untilMillis = null, nowMillis = 0L))
  }
}
