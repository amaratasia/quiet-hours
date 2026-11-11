package expo.modules.silentmode

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceScheduleTest {
  private val timeZone = TimeZone.getTimeZone("Europe/London")
  private val everyDay = (0..6).toSet()

  /** 2026-11-10 is a Tuesday. */
  private fun at(day: Int, hour: Int, minute: Int = 0): Long =
    Calendar.getInstance(timeZone).apply {
      clear()
      set(2026, Calendar.NOVEMBER, day, hour, minute, 0)
    }.timeInMillis

  private fun window(startHour: Int, endHour: Int, weekdays: Set<Int> = everyDay, enabled: Boolean = true) =
    QuietWindow("w$startHour-$endHour", startHour * 60, endHour * 60, weekdays, enabled)

  private fun isSilent(time: Long, windows: List<QuietWindow>, manualUntil: Long? = null) =
    SilenceSchedule.isSilentAt(time, windows, manualUntil, timeZone)

  private fun nextChange(time: Long, windows: List<QuietWindow>, manualUntil: Long? = null) =
    SilenceSchedule.nextStateChangeAfter(time, windows, manualUntil, timeZone)

  @Test
  fun silentOnlyInsideWindowWithExclusiveEnd() {
    val threeToFour = listOf(window(15, 16))

    assertFalse(isSilent(at(10, 14, 59), threeToFour))
    assertTrue(isSilent(at(10, 15), threeToFour))
    assertTrue(isSilent(at(10, 15, 59), threeToFour))
    assertFalse(isSilent(at(10, 16), threeToFour))
  }

  @Test
  fun nextChangeIsWindowStartThenEnd() {
    val threeToFour = listOf(window(15, 16))

    assertEquals(at(10, 15), nextChange(at(10, 9), threeToFour))
    assertEquals(at(10, 16), nextChange(at(10, 15, 30), threeToFour))
    assertEquals(at(11, 15), nextChange(at(10, 16), threeToFour))
  }

  @Test
  fun overnightWindowSpansMidnight() {
    val tenToSeven = listOf(window(22, 7))

    assertTrue(isSilent(at(10, 23), tenToSeven))
    assertTrue(isSilent(at(11, 6, 59), tenToSeven))
    assertFalse(isSilent(at(11, 7), tenToSeven))
    assertEquals(at(11, 7), nextChange(at(10, 23), tenToSeven))
  }

  @Test
  fun overnightWindowBelongsToTheDayItStarts() {
    val mondayNightOnly = listOf(window(22, 7, weekdays = setOf(1)))

    assertTrue(isSilent(at(10, 3), mondayNightOnly))
    assertFalse(isSilent(at(10, 23), mondayNightOnly))
  }

  @Test
  fun weekdayFilterSkipsUnselectedDays() {
    val weekendsOnly = listOf(window(15, 16, weekdays = setOf(0, 6)))

    assertFalse(isSilent(at(10, 15, 30), weekendsOnly))
    assertEquals(at(14, 15), nextChange(at(10, 9), weekendsOnly))
  }

  @Test
  fun disabledWindowIsIgnored() {
    val disabled = listOf(window(15, 16, enabled = false))

    assertFalse(isSilent(at(10, 15, 30), disabled))
    assertNull(nextChange(at(10, 9), disabled))
  }

  @Test
  fun manualSilenceLastsUntilItsEnd() {
    val manualUntil = at(10, 13)

    assertTrue(isSilent(at(10, 10), emptyList(), manualUntil))
    assertFalse(isSilent(at(10, 13), emptyList(), manualUntil))
    assertEquals(manualUntil, nextChange(at(10, 10), emptyList(), manualUntil))
  }

  @Test
  fun backToBackPeriodsMergeIntoOneSilence() {
    val windows = listOf(window(15, 16), window(16, 17))

    assertEquals(at(10, 17), nextChange(at(10, 15, 30), windows))
  }

  @Test
  fun manualSilenceRunningIntoWindowEndsWithTheWindow() {
    val manualUntilHalfThree = at(10, 15, 30)

    assertEquals(at(10, 16), nextChange(at(10, 14), listOf(window(15, 16)), manualUntilHalfThree))
  }
}
