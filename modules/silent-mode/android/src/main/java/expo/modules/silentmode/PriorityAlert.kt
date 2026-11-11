package expo.modules.silentmode

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
  private var isRingtonePlaying = false
  private val stopAtCap = Runnable { activeContext?.let { stopRinging(it, "cap") } }
  private var activeContext: Context? = null

  fun handle(context: Context, alert: IncomingAlert, contactNameForNumber: String?) {
    val store = QuietStore(context.applicationContext)
    val nowMillis = System.currentTimeMillis()
    val callerKey = BreakThroughPolicy.callerKey(alert, contactNameForNumber)
    val callsInWindow = recordCall(store, callerKey, nowMillis)

    if (!store.silencedByApp) {
      Log.i(TAG, "PriorityAlert: ${alertType(alert)} from $callerKey ignored; phone not silenced by the app")
      return
    }

    val reason = BreakThroughPolicy.decide(alert, store.importantPeople, store.repeatCallerSettings, callsInWindow)
    Log.i(TAG, "PriorityAlert: ${alertType(alert)} from $callerKey calls=$callsInWindow -> ${reason ?: "stays quiet"}")

    when {
      reason == null -> Unit
      alert is IncomingAlert.WhatsAppMessage -> playMessageSound(context, reason.name)
      else -> startRinging(context, reason.name)
    }
  }

  fun startRinging(context: Context, reason: String) {
    if (isRingtonePlaying) {
      Log.i(TAG, "PriorityAlert: already ringing; ignoring start($reason)")
      return
    }
    releasePlayer()
    play(context, Settings.System.DEFAULT_RINGTONE_URI, looping = true, capMillis = RING_CAP_MILLIS, reason = reason)
  }

  fun stopRinging(context: Context, reason: String) {
    releasePlayer()
    activeContext = null

    QuietStore(context.applicationContext).priorityAlertUntilMillis = null
    Log.i(TAG, "PriorityAlert: stopped ($reason)")
    QuietController.reconcile(context, "priority-alert-ended:$reason")
  }

  fun playMessageSound(context: Context, reason: String) {
    if (player != null) {
      Log.i(TAG, "PriorityAlert: already playing; skipping message sound ($reason)")
      return
    }
    play(
      context,
      Settings.System.DEFAULT_NOTIFICATION_URI,
      looping = false,
      capMillis = MESSAGE_SOUND_CAP_MILLIS,
      reason = reason
    )
  }

  fun isPlaying(): Boolean = player != null

  private fun releasePlayer() {
    mainHandler.removeCallbacks(stopAtCap)
    player?.runCatching { stop(); release() }
      ?.onFailure { Log.w(TAG, "PriorityAlert: failed to stop player", it) }
    player = null
    isRingtonePlaying = false
  }

  private fun recordCall(store: QuietStore, callerKey: String?, nowMillis: Long): Int {
    if (callerKey == null) return 0
    val history = RepeatCallers.record(store.recentCalls, callerKey, nowMillis)
    store.recentCalls = history
    return RepeatCallers.callsInWindow(history, callerKey, nowMillis)
  }

  private fun play(context: Context, soundUri: Uri, looping: Boolean, capMillis: Long, reason: String) {
    val appContext = context.applicationContext
    QuietStore(appContext).priorityAlertUntilMillis = System.currentTimeMillis() + capMillis
    appContext.getSystemService(AudioManager::class.java).ringerMode = AudioManager.RINGER_MODE_NORMAL

    val startedPlayer = runCatching { createPlayer(appContext, soundUri, looping) }
      .onFailure { Log.e(TAG, "PriorityAlert: cannot play $soundUri ($reason); restoring quiet", it) }
      .getOrNull()
    if (startedPlayer == null) {
      stopRinging(appContext, "playback-failed")
      return
    }

    player = startedPlayer
    isRingtonePlaying = looping
    activeContext = appContext
    if (!looping) startedPlayer.setOnCompletionListener { stopRinging(appContext, "message-sound-finished") }
    mainHandler.postDelayed(stopAtCap, capMillis)
    Log.i(TAG, "PriorityAlert: playing ${if (looping) "ringtone" else "message sound"} ($reason)")

    QuietController.reconcile(appContext, "priority-alert-started:$reason")
  }

  private fun createPlayer(context: Context, soundUri: Uri, looping: Boolean): MediaPlayer =
    MediaPlayer().apply {
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

  private fun alertType(alert: IncomingAlert): String = when (alert) {
    is IncomingAlert.PhoneCall -> "phone call"
    is IncomingAlert.WhatsAppCall -> "WhatsApp call"
    is IncomingAlert.WhatsAppMessage -> "WhatsApp message"
  }
}
