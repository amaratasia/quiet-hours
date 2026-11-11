package expo.modules.silentmode

import org.json.JSONArray
import org.json.JSONObject

data class RepeatCallerSettings(val enabled: Boolean = false, val threshold: Int = DEFAULT_THRESHOLD) {
  companion object {
    const val DEFAULT_THRESHOLD = 3
    private val THRESHOLD_RANGE = 2..10

    fun of(enabled: Boolean, threshold: Int) =
      RepeatCallerSettings(enabled, threshold.coerceIn(THRESHOLD_RANGE))
  }
}

data class RecentCall(val callerKey: String, val atMillis: Long)

object RepeatCallers {
  const val WINDOW_MILLIS = 300_000L

  fun record(history: List<RecentCall>, callerKey: String, nowMillis: Long): List<RecentCall> =
    (history + RecentCall(callerKey, nowMillis)).filter { isInWindow(it, nowMillis) }

  fun callsInWindow(history: List<RecentCall>, callerKey: String, nowMillis: Long): Int =
    history.count { it.callerKey == callerKey && isInWindow(it, nowMillis) }

  fun listToJson(history: List<RecentCall>): String =
    JSONArray(history.map { JSONObject().put("callerKey", it.callerKey).put("atMillis", it.atMillis) }).toString()

  fun listFromJson(serialized: String): List<RecentCall> {
    val array = JSONArray(serialized)
    return (0 until array.length()).map {
      val call = array.getJSONObject(it)
      RecentCall(call.getString("callerKey"), call.getLong("atMillis"))
    }
  }

  private fun isInWindow(call: RecentCall, nowMillis: Long): Boolean =
    nowMillis - call.atMillis <= WINDOW_MILLIS
}
