package expo.modules.silentmode

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class SilentModeModule : Module() {
  private val context: Context
    get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()

  override fun definition() = ModuleDefinition {
    Name("SilentMode")

    Function("refresh") {
      statusToMap(QuietController.reconcile(context, "refresh"))
    }

    Function("silenceFor") { minutes: Int ->
      QuietStore(context).manualSilenceUntilMillis = System.currentTimeMillis() + minutes * 60_000L
      statusToMap(QuietController.reconcile(context, "silenceFor($minutes)"))
    }

    Function("cancelManualSilence") {
      QuietStore(context).manualSilenceUntilMillis = null
      statusToMap(QuietController.reconcile(context, "cancelManualSilence"))
    }

    Function("setQuietMode") { quietModeKey: String ->
      QuietController.switchQuietMode(context, QuietMode.fromKey(quietModeKey))
      statusToMap(QuietController.reconcile(context, "setQuietMode($quietModeKey)"))
    }

    Function("setWindows") { serializedWindows: String ->
      QuietStore(context).windows = QuietWindow.listFromJson(serializedWindows)
      statusToMap(QuietController.reconcile(context, "setWindows"))
    }

    Function("setImportantPeople") { serializedPeople: String ->
      QuietStore(context).importantPeople = ImportantPerson.listFromJson(serializedPeople)
      statusToMap(QuietController.reconcile(context, "setImportantPeople"))
    }

    Function("setRepeatCallers") { enabled: Boolean, threshold: Int ->
      QuietStore(context).repeatCallerSettings = RepeatCallerSettings.of(enabled, threshold)
      statusToMap(QuietController.reconcile(context, "setRepeatCallers($enabled, $threshold)"))
    }

    Function("openNotificationAccessSettings") {
      openSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    Function("openAppDetailsSettings") {
      openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
    }

    Function("requestIgnoreBatteryOptimisation") {
      openSettings(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()))
    }

    Function("openDndAccessSettings") {
      openSettings(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    Function("openExactAlarmSettings") {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        openSettings(
          Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri())
        )
      }
    }
  }

  private fun openSettings(intent: Intent) {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  private fun packageUri(): Uri = Uri.parse("package:${context.packageName}")

  private fun hasPermission(permission: String): Boolean =
    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

  private fun hasNotificationAccess(): Boolean {
    val listener = ComponentName(context, QuietNotificationListener::class.java).flattenToString()
    val enabledListeners = Settings.Secure.getString(context.contentResolver, ENABLED_NOTIFICATION_LISTENERS) ?: ""
    return enabledListeners.split(":").contains(listener)
  }

  private fun breakThroughSetupToMap(store: QuietStore): Map<String, Any?> = mapOf(
    "hasNotificationAccess" to hasNotificationAccess(),
    "hasPhoneAccess" to (hasPermission(Manifest.permission.READ_PHONE_STATE) && hasPermission(Manifest.permission.READ_CALL_LOG)),
    "hasContactsAccess" to hasPermission(Manifest.permission.READ_CONTACTS),
    "isBatteryOptimisationIgnored" to
      context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
    "lastWhatsAppSeenAt" to store.lastWhatsAppSeenAtMillis?.toDouble()
  )

  private fun statusToMap(status: QuietStatus): Map<String, Any?> {
    val store = QuietStore(context)
    return mapOf(
      "supported" to true,
      "quietMode" to status.quietMode.key,
      "hasDndAccess" to status.hasDndAccess,
      "canScheduleExactAlarms" to status.canScheduleExactAlarms,
      "isSilentNow" to status.isSilentNow,
      "manualSilenceUntil" to status.manualSilenceUntilMillis?.toDouble(),
      "nextChangeAt" to status.nextChangeAtMillis?.toDouble(),
      "windowsJson" to QuietWindow.listToJson(status.windows),
      "importantPeopleJson" to ImportantPerson.listToJson(store.importantPeople),
      "repeatCallers" to store.repeatCallerSettings.let { mapOf("enabled" to it.enabled, "threshold" to it.threshold) },
      "setup" to breakThroughSetupToMap(store)
    )
  }

  private companion object {
    const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
  }
}
