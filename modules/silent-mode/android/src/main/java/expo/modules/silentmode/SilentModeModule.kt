package expo.modules.silentmode

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

    Function("openDndAccessSettings") {
      openSettings(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    Function("openExactAlarmSettings") {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        openSettings(
          Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        )
      }
    }
  }

  private fun openSettings(intent: Intent) {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  private fun statusToMap(status: QuietStatus): Map<String, Any?> = mapOf(
    "supported" to true,
    "quietMode" to status.quietMode.key,
    "hasDndAccess" to status.hasDndAccess,
    "canScheduleExactAlarms" to status.canScheduleExactAlarms,
    "isSilentNow" to status.isSilentNow,
    "manualSilenceUntil" to status.manualSilenceUntilMillis?.toDouble(),
    "nextChangeAt" to status.nextChangeAtMillis?.toDouble(),
    "windowsJson" to QuietWindow.listToJson(status.windows)
  )
}
