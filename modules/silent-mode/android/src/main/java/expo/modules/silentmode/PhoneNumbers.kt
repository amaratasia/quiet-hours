package expo.modules.silentmode

object PhoneNumbers {
  private const val MIN_DIGITS = 7
  private const val COMPARED_DIGITS = 9

  /** The last 9 digits, so national (trunk 0), international and 00-prefixed forms agree in 9- and 10-digit plans. */
  fun significantDigits(rawNumber: String?): String? {
    val digits = rawNumber?.filter { it.isDigit() } ?: return null
    if (digits.length < MIN_DIGITS) return null
    return digits.takeLast(COMPARED_DIGITS)
  }

  fun sameNumber(first: String?, second: String?): Boolean {
    val firstDigits = significantDigits(first) ?: return false
    val secondDigits = significantDigits(second) ?: return false
    val comparedLength = minOf(firstDigits.length, secondDigits.length)
    return firstDigits.takeLast(comparedLength) == secondDigits.takeLast(comparedLength)
  }
}
