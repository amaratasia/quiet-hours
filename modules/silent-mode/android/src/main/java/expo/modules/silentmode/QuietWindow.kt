package expo.modules.silentmode

import org.json.JSONArray
import org.json.JSONObject

/** Weekdays use JavaScript's Date.getDay() numbering: 0 = Sunday ... 6 = Saturday. */
data class QuietWindow(
  val id: String,
  val startMinute: Int,
  val endMinute: Int,
  val weekdays: Set<Int>,
  val enabled: Boolean
) {
  fun toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("startMinute", startMinute)
    .put("endMinute", endMinute)
    .put("weekdays", JSONArray(weekdays.sorted()))
    .put("enabled", enabled)

  companion object {
    fun fromJson(json: JSONObject): QuietWindow {
      val weekdaysJson = json.getJSONArray("weekdays")
      val weekdays = (0 until weekdaysJson.length()).map { weekdaysJson.getInt(it) }.toSet()

      return QuietWindow(
        id = json.getString("id"),
        startMinute = json.getInt("startMinute"),
        endMinute = json.getInt("endMinute"),
        weekdays = weekdays,
        enabled = json.getBoolean("enabled")
      )
    }

    fun listFromJson(serialized: String): List<QuietWindow> {
      val array = JSONArray(serialized)
      return (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
    }

    fun listToJson(windows: List<QuietWindow>): String =
      JSONArray(windows.map { it.toJson() }).toString()
  }
}
