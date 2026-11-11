package expo.modules.silentmode

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log

class IncomingCallReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
    @Suppress("DEPRECATION")
    val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
    val store = QuietStore(context.applicationContext)
    Log.i(TAG, "IncomingCallReceiver: state=$state hasNumber=${number != null}")

    when (state) {
      TelephonyManager.EXTRA_STATE_RINGING -> handleRinging(context, store, state, number)
      else -> handleCallLeftRinging(context, store, state)
    }
  }

  private fun handleRinging(context: Context, store: QuietStore, state: String, number: String?) {
    val nowMillis = System.currentTimeMillis()
    val lastHandledNumber = CallRingDeduplicator.rememberedNumber(store.lastRingNumber, store.lastRingAtMillis, nowMillis)
    if (!CallRingDeduplicator.isNewRing(state, number, lastHandledNumber)) return

    store.lastRingNumber = number
    store.lastRingAtMillis = nowMillis
    PriorityAlert.handle(context, IncomingAlert.PhoneCall(number), contactNameFor(context, number))
  }

  private fun handleCallLeftRinging(context: Context, store: QuietStore, state: String) {
    if (state == TelephonyManager.EXTRA_STATE_IDLE) store.lastRingNumber = null
    if (RingerRestore.shouldStopPriorityAlert(PriorityAlert.isPlaying(), store.priorityAlertUntilMillis, System.currentTimeMillis())) {
      PriorityAlert.stopRinging(context, "call-$state", AlertSource.PHONE_CALL)
    }
  }

  private fun contactNameFor(context: Context, number: String?): String? {
    if (number == null) return null
    if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
      Log.w(TAG, "IncomingCallReceiver: contacts permission missing; repeat calls keyed by number only")
      return null
    }

    val lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    return runCatching {
      context.contentResolver
        .query(lookupUri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.onFailure { Log.w(TAG, "IncomingCallReceiver: contact lookup failed", it) }.getOrNull()
  }

  private companion object {
    const val TAG = "QuietHours"
  }
}
