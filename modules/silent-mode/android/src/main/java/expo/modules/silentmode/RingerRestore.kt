package expo.modules.silentmode

object RingerRestore {
  fun shouldApplyQuiet(shouldBeSilent: Boolean, priorityAlertActive: Boolean): Boolean =
    shouldBeSilent && !priorityAlertActive

  /** An expiry rather than a flag, so a process killed mid-ring cannot leave the ringer on forever. */
  fun isPriorityAlertActive(untilMillis: Long?, nowMillis: Long): Boolean =
    untilMillis != null && nowMillis < untilMillis
}
