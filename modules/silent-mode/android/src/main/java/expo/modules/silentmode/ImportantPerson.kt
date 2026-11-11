package expo.modules.silentmode

import org.json.JSONArray
import org.json.JSONObject

data class ImportantPerson(
  val id: String,
  val displayName: String,
  val phoneNumbers: List<String>
) {
  fun toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("displayName", displayName)
    .put("phoneNumbers", JSONArray(phoneNumbers))

  companion object {
    fun fromJson(json: JSONObject): ImportantPerson {
      val numbersJson = json.getJSONArray("phoneNumbers")

      return ImportantPerson(
        id = json.getString("id"),
        displayName = json.getString("displayName"),
        phoneNumbers = (0 until numbersJson.length()).map { numbersJson.getString(it) }
      )
    }

    fun listFromJson(serialized: String): List<ImportantPerson> {
      val array = JSONArray(serialized)
      return (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
    }

    fun listToJson(people: List<ImportantPerson>): String =
      JSONArray(people.map { it.toJson() }).toString()
  }
}

fun List<ImportantPerson>.findByNumber(number: String?): ImportantPerson? =
  firstOrNull { person -> person.phoneNumbers.any { PhoneNumbers.sameNumber(it, number) } }

fun List<ImportantPerson>.findByName(name: String?): ImportantPerson? {
  val wantedName = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
  return firstOrNull { it.displayName.trim().equals(wantedName, ignoreCase = true) }
}
