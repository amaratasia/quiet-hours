package expo.modules.silentmode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportantPersonTest {
  @Test
  fun findsPersonByAnyOfTheirNumbers() {
    val mum = ImportantPerson("1", "Mum", listOf("01234 567890", "+44 7700 900123"))

    assertEquals(mum, listOf(mum).findByNumber("07700900123"))
  }

  @Test
  fun withheldNumberFindsNobody() {
    val mum = ImportantPerson("1", "Mum", listOf("+44 7700 900123"))

    assertNull(listOf(mum).findByNumber(null))
  }

  @Test
  fun findsPersonByNameIgnoringCaseAndSpaces() {
    val mum = ImportantPerson("1", "Mum", emptyList())

    assertEquals(mum, listOf(mum).findByName("  mum "))
    assertNull(listOf(mum).findByName("Mummy"))
  }

  @Test
  fun importantPeopleRoundTripThroughJson() {
    val people = listOf(ImportantPerson("1", "Mum", listOf("+447700900123")))

    assertEquals(people, ImportantPerson.listFromJson(ImportantPerson.listToJson(people)))
  }
}
