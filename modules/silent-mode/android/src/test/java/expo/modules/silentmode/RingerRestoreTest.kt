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

  @Test
  fun callEndingStopsAStoredAlertEvenWithoutAPlayer() {
    assertTrue(RingerRestore.shouldStopPriorityAlert(isPlaying = false, untilMillis = 60_000L, nowMillis = 1_000L))
    assertTrue(RingerRestore.shouldStopPriorityAlert(isPlaying = true, untilMillis = null, nowMillis = 1_000L))
    assertFalse(RingerRestore.shouldStopPriorityAlert(isPlaying = false, untilMillis = null, nowMillis = 1_000L))
  }

  @Test
  fun stopRequestOnlyStopsTheAlertItsSourceStarted() {
    assertTrue(RingerRestore.isStopForActiveAlert(activeSource = AlertSource.PHONE_CALL, requestingSource = AlertSource.PHONE_CALL))
    assertFalse(RingerRestore.isStopForActiveAlert(activeSource = AlertSource.PHONE_CALL, requestingSource = AlertSource.WHATSAPP_CALL))
    assertTrue(RingerRestore.isStopForActiveAlert(activeSource = null, requestingSource = AlertSource.WHATSAPP_CALL))
  }
}
