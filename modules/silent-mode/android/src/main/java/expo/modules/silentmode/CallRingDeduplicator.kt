package expo.modules.silentmode

/** With call-log access Android sends RINGING twice: once without the number, then once with it. */
object CallRingDeduplicator {
  private const val STATE_RINGING = "RINGING"
  private const val REMEMBER_MILLIS = 120_000L

  fun isNewRing(state: String, number: String?, lastHandledNumber: String?): Boolean =
    state == STATE_RINGING && number != null && number != lastHandledNumber

  /** Forgets the last number after a while, so one missed IDLE broadcast cannot block that caller for good. */
  fun rememberedNumber(lastNumber: String?, lastRingAtMillis: Long?, nowMillis: Long): String? {
    if (lastRingAtMillis == null || nowMillis - lastRingAtMillis >= REMEMBER_MILLIS) return null
    return lastNumber
  }
}
