package expo.modules.silentmode

object PhoneNumbers {
  private const val MIN_DIGITS = 7
  private const val COMPARED_DIGITS = 10

  /** Trailing digits only, so national, international and 00-prefixed forms of one number agree. */
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
