package expo.modules.silentmode

import java.util.Calendar
import java.util.TimeZone

data class SilentPeriod(val startMillis: Long, val endMillis: Long)

object SilenceSchedule {
  private const val MINUTES_PER_DAY = 24 * 60
  private const val MILLIS_PER_MINUTE = 60_000L
  private const val MAX_BOUNDARIES_TO_SCAN = 64

  fun isSilentAt(
    timeMillis: Long,
    windows: List<QuietWindow>,
    manualSilenceUntilMillis: Long?,
    timeZone: TimeZone = TimeZone.getDefault()
  ): Boolean {
    val manuallySilenced = manualSilenceUntilMillis != null && timeMillis < manualSilenceUntilMillis
    if (manuallySilenced) return true

    return periodsAround(timeMillis, windows, timeZone)
      .any { timeMillis >= it.startMillis && timeMillis < it.endMillis }
  }

  fun nextBoundaryAfter(
    timeMillis: Long,
    windows: List<QuietWindow>,
    manualSilenceUntilMillis: Long?,
    timeZone: TimeZone = TimeZone.getDefault()
  ): Long? {
    val windowBoundaries = periodsAround(timeMillis, windows, timeZone)
      .flatMap { listOf(it.startMillis, it.endMillis) }
    val allBoundaries = windowBoundaries + listOfNotNull(manualSilenceUntilMillis)

    return allBoundaries.filter { it > timeMillis }.minOrNull()
  }

  /** Skips boundaries where silence continues, e.g. back-to-back or overlapping windows. */
  fun nextStateChangeAfter(
    timeMillis: Long,
    windows: List<QuietWindow>,
    manualSilenceUntilMillis: Long?,
    timeZone: TimeZone = TimeZone.getDefault()
  ): Long? {
    val silentNow = isSilentAt(timeMillis, windows, manualSilenceUntilMillis, timeZone)
    var candidate = timeMillis

    repeat(MAX_BOUNDARIES_TO_SCAN) {
      candidate = nextBoundaryAfter(candidate, windows, manualSilenceUntilMillis, timeZone)
        ?: return null
      if (isSilentAt(candidate, windows, manualSilenceUntilMillis, timeZone) != silentNow) {
        return candidate
      }
    }
    return null
  }

  private fun periodsAround(
    timeMillis: Long,
    windows: List<QuietWindow>,
    timeZone: TimeZone
  ): List<SilentPeriod> {
    val dayOffsets = -1..8
    return windows
      .filter { it.enabled && it.weekdays.isNotEmpty() }
      .flatMap { window ->
        dayOffsets.mapNotNull { dayOffset -> periodStartingOnDay(window, timeMillis, dayOffset, timeZone) }
      }
  }

  private fun periodStartingOnDay(
    window: QuietWindow,
    referenceMillis: Long,
    dayOffset: Int,
    timeZone: TimeZone
  ): SilentPeriod? {
    val calendar = Calendar.getInstance(timeZone).apply {
      timeInMillis = referenceMillis
      add(Calendar.DAY_OF_YEAR, dayOffset)
    }
    val weekday = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    if (weekday !in window.weekdays) return null

    calendar.set(Calendar.HOUR_OF_DAY, window.startMinute / 60)
    calendar.set(Calendar.MINUTE, window.startMinute % 60)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val startMillis = calendar.timeInMillis
    return SilentPeriod(startMillis, startMillis + durationMinutes(window) * MILLIS_PER_MINUTE)
  }

  /** An end time at or before the start time means the window runs past midnight. */
  private fun durationMinutes(window: QuietWindow): Int {
    val difference = Math.floorMod(window.endMinute - window.startMinute, MINUTES_PER_DAY)
    return if (difference == 0) MINUTES_PER_DAY else difference
  }
}
