package expo.modules.silentmode

import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatCallersTest {
  private val caller = "number:7733916287"

  @Test
  fun thirdCallWithinFiveMinutesIsCounted() {
    var history = emptyList<RecentCall>()
    listOf(0L, 60_000L, 240_000L).forEach { history = RepeatCallers.record(history, caller, it) }

    assertEquals(3, RepeatCallers.callsInWindow(history, caller, 240_000L))
  }

  @Test
  fun callsOlderThanFiveMinutesAreDropped() {
    var history = RepeatCallers.record(emptyList(), caller, 0L)
    history = RepeatCallers.record(history, caller, 300_001L)

    assertEquals(1, RepeatCallers.callsInWindow(history, caller, 300_001L))
    assertEquals(1, history.size)
  }

  @Test
  fun otherCallersAreNotCounted() {
    val history = RepeatCallers.record(RepeatCallers.record(emptyList(), caller, 0L), "number:1111111111", 1L)

    assertEquals(1, RepeatCallers.callsInWindow(history, caller, 1L))
  }

  @Test
  fun thresholdIsClampedToTwoThroughTen() {
    assertEquals(2, RepeatCallerSettings.of(true, 1).threshold)
    assertEquals(10, RepeatCallerSettings.of(true, 50).threshold)
    assertEquals(3, RepeatCallerSettings().threshold)
  }

  @Test
  fun recentCallsRoundTripThroughJson() {
    val history = listOf(RecentCall(caller, 42L))

    assertEquals(history, RepeatCallers.listFromJson(RepeatCallers.listToJson(history)))
  }
}
