package expo.modules.silentmode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {
  @Test
  fun ukFormatsMatch() {
    val formats = listOf("+44 7733 916287", "07733916287", "447733916287", "0044 7733 916287", "(07733) 916-287")

    formats.forEach { assertTrue(it, PhoneNumbers.sameNumber("+447733916287", it)) }
  }

  @Test
  fun differentNumbersDoNotMatch() {
    assertFalse(PhoneNumbers.sameNumber("07733916287", "07733916288"))
  }

  @Test
  fun withheldAndShortNumbersNeverMatch() {
    assertNull(PhoneNumbers.significantDigits(null))
    assertNull(PhoneNumbers.significantDigits(""))
    assertNull(PhoneNumbers.significantDigits("Private"))
    assertNull(PhoneNumbers.significantDigits("12345"))
    assertFalse(PhoneNumbers.sameNumber(null, null))
  }

  @Test
  fun significantDigitsKeepsTheLastNineDigits() {
    assertEquals("733916287", PhoneNumbers.significantDigits("+44 7733 916287"))
  }

  @Test
  fun nineDigitNationalNumbersWithTrunkZeroMatch() {
    assertTrue(PhoneNumbers.sameNumber("+31 6 12345678", "06 12345678"))
    assertTrue(PhoneNumbers.sameNumber("+33 6 12 34 56 78", "06 12 34 56 78"))
    assertTrue(PhoneNumbers.sameNumber("+61 412 345 678", "0412 345 678"))
  }
}
