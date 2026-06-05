package com.org.patientchakravue.firebase

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Plays a LOOPING ringtone for an incoming video call.
 *
 * This is independent of the notification-channel sound (which is only ever a
 * one-shot chime). It gives a real phone-style continuous ring and auto-stops
 * after a timeout so it can never ring forever if the call is ignored.
 *
 * Lifecycle:
 *  - start(): called from FirebaseService when an "incoming_call" push arrives.
 *  - stop():  called when the patient answers (MainActivity routes to the call
 *             screen) or after AUTO_STOP_MS, whichever comes first.
 */
object IncomingCallRingtone {
    private const val TAG = "IncomingCallRingtone"
    private const val AUTO_STOP_MS = 60_000L // matches backend FCM ttl=60

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stop() }

    @Synchronized
    fun start(context: Context) {
        // Never stack two ringers.
        stop()
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
            player = mp

            handler.removeCallbacks(autoStop)
            handler.postDelayed(autoStop, AUTO_STOP_MS)
            Log.d(TAG, "Incoming-call ringtone started (looping)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ringtone", e)
        }
    }

    @Synchronized
    fun stop() {
        handler.removeCallbacks(autoStop)
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {
            }
            try {
                it.release()
            } catch (_: Exception) {
            }
        }
        player = null
    }
}
