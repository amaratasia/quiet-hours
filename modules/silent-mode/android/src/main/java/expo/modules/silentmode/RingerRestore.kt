package expo.modules.silentmode

object RingerRestore {
  fun shouldApplyQuiet(shouldBeSilent: Boolean, priorityAlertActive: Boolean): Boolean =
    shouldBeSilent && !priorityAlertActive

  /** An expiry rather than a flag, so a process killed mid-ring cannot leave the ringer on forever. */
  fun isPriorityAlertActive(untilMillis: Long?, nowMillis: Long): Boolean =
    untilMillis != null && nowMillis < untilMillis

  /** A fresh process has no player, but a stored alert still means the app turned the ringer on. */
  fun shouldStopPriorityAlert(isPlaying: Boolean, untilMillis: Long?, nowMillis: Long): Boolean =
    isPlaying || isPriorityAlertActive(untilMillis, nowMillis)

  /** Without a player (fresh process) any stop request may clear the stored alert. */
  fun isStopForActiveAlert(activeSource: AlertSource?, requestingSource: AlertSource): Boolean =
    activeSource == null || activeSource == requestingSource
}
