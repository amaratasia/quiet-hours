package expo.modules.silentmode

/** With call-log access Android sends RINGING twice: once without the number, then once with it. */
object CallRingDeduplicator {
  private const val STATE_RINGING = "RINGING"

  fun isNewRing(state: String, number: String?, lastHandledNumber: String?): Boolean =
    state == STATE_RINGING && number != null && number != lastHandledNumber
}
