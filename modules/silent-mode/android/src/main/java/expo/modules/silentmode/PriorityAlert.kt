package expo.modules.silentmode

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

object PriorityAlert {
  private const val TAG = "QuietHours"
  private const val RING_CAP_MILLIS = 60_000L
  private const val MESSAGE_SOUND_CAP_MILLIS = 10_000L

  private val mainHandler = Handler(Looper.getMainLooper())
  private var player: MediaPlayer? = null
  private var activeSource: AlertSource? = null
  private val stopAtCap = Runnable { activeContext?.let { stopRinging(it, "cap") } }
  private var activeContext: Context? = null

  fun handle(context: Context, alert: IncomingAlert, contactNameForNumber: String?) {
    val store = QuietStore(context.applicationContext)
    val nowMillis = System.currentTimeMillis()
    val callerKey = BreakThroughPolicy.callerKey(alert, contactNameForNumber)
    val callsInWindow = recordCall(store, callerKey, nowMillis)

    if (!store.silencedByApp) {
      Log.i(TAG, "PriorityAlert: ${alertType(alert)} ignored; phone not silenced by the app")
      return
    }

    val reason = BreakThroughPolicy.decide(alert, store.importantPeople, store.repeatCallerSettings, callsInWindow)
    Log.i(TAG, "PriorityAlert: ${alertType(alert)} calls=$callsInWindow -> ${reason ?: "stays quiet"}")
    Log.d(TAG, "PriorityAlert: ${alertType(alert)} caller=${callerKey ?: describeSender(alert)}")

    when {
      reason == null -> Unit
      alert is IncomingAlert.WhatsAppMessage -> playMessageSound(context, reason.name)
      else -> startRinging(context, reason.name, BreakThroughPolicy.sourceOf(alert))
    }
  }

  fun startRinging(context: Context, reason: String, source: AlertSource) {
    if (activeSource == AlertSource.PHONE_CALL || activeSource == AlertSource.WHATSAPP_CALL) {
      Log.i(TAG, "PriorityAlert: already ringing for $activeSource; ignoring start($reason) for $source")
      return
    }
    releasePlayer()
    play(context, Settings.System.DEFAULT_RINGTONE_URI, source, RING_CAP_MILLIS, reason)
  }

  /** A null source means stop whatever is playing, e.g. the 60-second cap. */
  fun stopRinging(context: Context, reason: String, requestingSource: AlertSource? = null) {
    if (requestingSource != null && !RingerRestore.isStopForActiveAlert(activeSource, requestingSource)) {
      Log.i(TAG, "PriorityAlert: $requestingSource asked to stop ($reason) but $activeSource is playing; ignored")
      return
    }
    val stoppedSource = activeSource
    releasePlayer()
    activeContext = null

    QuietStore(context.applicationContext).priorityAlertUntilMillis = null
    Log.i(TAG, "PriorityAlert: stopped ${stoppedSource ?: "stored alert"} ($reason)")
    QuietController.reconcile(context, "priority-alert-ended:$reason")
  }

  fun playMessageSound(context: Context, reason: String) {
    if (player != null) {
      Log.i(TAG, "PriorityAlert: $activeSource already playing; skipping message sound ($reason)")
      return
    }
    play(context, Settings.System.DEFAULT_NOTIFICATION_URI, AlertSource.WHATSAPP_MESSAGE, MESSAGE_SOUND_CAP_MILLIS, reason)
  }

  fun isPlaying(): Boolean = player != null

  private fun releasePlayer() {
    mainHandler.removeCallbacks(stopAtCap)
    player?.let(::releaseQuietly)
    player = null
    activeSource = null
  }

  private fun releaseQuietly(mediaPlayer: MediaPlayer) {
    runCatching { mediaPlayer.stop() }.onFailure { Log.w(TAG, "PriorityAlert: failed to stop player", it) }
    runCatching { mediaPlayer.release() }.onFailure { Log.w(TAG, "PriorityAlert: failed to release player", it) }
  }

  private fun recordCall(store: QuietStore, callerKey: String?, nowMillis: Long): Int {
    if (callerKey == null) return 0
    val history = RepeatCallers.record(store.recentCalls, callerKey, nowMillis)
    store.recentCalls = history
    return RepeatCallers.callsInWindow(history, callerKey, nowMillis)
  }

  private fun play(context: Context, soundUri: Uri, source: AlertSource, capMillis: Long, reason: String) {
    val appContext = context.applicationContext
    if (!turnRingerOn(appContext, reason)) return
    QuietStore(appContext).priorityAlertUntilMillis = System.currentTimeMillis() + capMillis

    val looping = source != AlertSource.WHATSAPP_MESSAGE
    val startedPlayer = createPlayer(appContext, soundUri, looping, reason)
    if (startedPlayer == null) {
      stopRinging(appContext, "playback-failed")
      return
    }

    player = startedPlayer
    activeSource = source
    activeContext = appContext
    startedPlayer.setOnErrorListener { _, what, extra ->
      stopRinging(appContext, "player-error what=$what extra=$extra")
      true
    }
    if (!looping) startedPlayer.setOnCompletionListener { stopRinging(appContext, "message-sound-finished") }
    mainHandler.postDelayed(stopAtCap, capMillis)
    Log.i(TAG, "PriorityAlert: playing ${if (looping) "ringtone" else "message sound"} for $source ($reason)")

    QuietController.reconcile(appContext, "priority-alert-started:$reason")
  }

  /** Leaving silent mode needs Do Not Disturb access since Android 7, and the user may have revoked it. */
  private fun turnRingerOn(context: Context, reason: String): Boolean {
    if (!context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted) {
      Log.w(TAG, "PriorityAlert: Do Not Disturb access revoked; cannot ring for $reason")
      return false
    }
    return runCatching { context.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_NORMAL }
      .onFailure { Log.e(TAG, "PriorityAlert: could not turn the ringer on for $reason", it) }
      .isSuccess
  }

  private fun createPlayer(context: Context, soundUri: Uri, looping: Boolean, reason: String): MediaPlayer? {
    val mediaPlayer = MediaPlayer()
    return runCatching {
      mediaPlayer.apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(if (looping) AudioAttributes.USAGE_NOTIFICATION_RINGTONE else AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        setDataSource(context, soundUri)
        isLooping = looping
        prepare()
        start()
      }
    }.onFailure {
      Log.e(TAG, "PriorityAlert: cannot play $soundUri ($reason), e.g. default sound set to None; restoring quiet", it)
      releaseQuietly(mediaPlayer)
    }.getOrNull()
  }

  private fun alertType(alert: IncomingAlert): String = when (alert) {
    is IncomingAlert.PhoneCall -> "phone call"
    is IncomingAlert.WhatsAppCall -> "WhatsApp call"
    is IncomingAlert.WhatsAppMessage -> "WhatsApp message"
  }

  private fun describeSender(alert: IncomingAlert): String = when (alert) {
    is IncomingAlert.PhoneCall -> "withheld"
    is IncomingAlert.WhatsAppCall -> alert.senderName
    is IncomingAlert.WhatsAppMessage -> alert.senderName
  }
}
