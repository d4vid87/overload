package dev.dwm.liftlog.ui

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

// set once from MainActivity.onCreate
@SuppressLint("StaticFieldLeak")
var appContext: Context? = null

// set in MainActivity.onCreate, nulled in onDestroy
@SuppressLint("StaticFieldLeak")
var appActivity: android.app.Activity? = null

actual fun keepScreenAwake(on: Boolean) {
    val a = appActivity ?: return
    a.runOnUiThread {
        runCatching {
            val flag = android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            if (on) a.window.addFlags(flag) else a.window.clearFlags(flag)
        }
    }
}

private const val CHANNEL_REST = "rest"
private const val NOTIF_ID = 42

private fun vibrator(): Vibrator? = appContext?.let { ctx ->
    if (Build.VERSION.SDK_INT >= 31) {
        (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

actual fun haptic(kind: Haptic) {
    val v = vibrator() ?: return
    runCatching {
        val effect = when (kind) {
            Haptic.Tick -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            Haptic.Click -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            Haptic.Buzz -> VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 180), -1)
        }
        v.vibrate(effect)
    }
}

// separate generators: tempo ticks on media stream, alarm fallback on alarm stream —
// sharing one meant startTone(tempo) preempted the rest-over alarm
private val alarmTone by lazy {
    runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100) }.getOrNull()
}
private var tts: android.speech.tts.TextToSpeech? = null
private var ttsReady = false

// pleasant tempo notes: a soft enveloped sine (marimba-ish pluck), synthesized to PCM once per
// pitch — replaces the harsh DTMF ToneGenerator beeps. DOWN low, UP high, pause a quieter tick.
private const val TONE_SR = 44100
private fun toneBuffer(freqHz: Double, ms: Int, peak: Double): ShortArray {
    val n = TONE_SR * ms / 1000
    val buf = ShortArray(n)
    for (i in 0 until n) {
        val t = i.toDouble() / TONE_SR
        val env = Math.exp(-t * 22.0) * (1.0 - Math.exp(-t * 500.0)) // fast attack, exp decay
        buf[i] = (Math.sin(2.0 * Math.PI * freqHz * t) * env * peak * Short.MAX_VALUE).toInt().toShort()
    }
    return buf
}
private val toneBuffers by lazy {
    mapOf(
        Tone.Low to toneBuffer(392.0, 150, 0.6),   // G4
        Tone.High to toneBuffer(587.0, 150, 0.6),  // D5
        Tone.Tick to toneBuffer(494.0, 90, 0.35),  // B4, softer
    )
}

actual fun playTone(t: Tone) {
    val buf = toneBuffers[t] ?: return
    runCatching {
        val track = android.media.AudioTrack(
            // USAGE_MEDIA, not SONIFICATION: sonification follows the ringer, so on silent/vibrate
            // the system muted every tempo note ("OpPlayAudio ... usage:13 muted" in logcat) and
            // the metronome was completely inaudible. Media volume is what metronome apps use.
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            android.media.AudioFormat.Builder()
                .setSampleRate(TONE_SR)
                .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            buf.size * 2,
            android.media.AudioTrack.MODE_STATIC,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        track.write(buf, 0, buf.size)
        track.play()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            { runCatching { track.stop(); track.release() } },
            buf.size * 1000L / TONE_SR + 60,
        )
    }
}

actual fun playAlarm() {
    val ctx = appContext ?: return
    val played = runCatching {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val stream = android.media.AudioManager.STREAM_ALARM
        val saved = am.getStreamVolume(stream)
        runCatching { am.setStreamVolume(stream, am.getStreamMaxVolume(stream), 0) }
        val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            ?: error("no ringtone")
        val mp = android.media.MediaPlayer()
        mp.setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        mp.setDataSource(ctx, uri)
        mp.prepare()
        mp.start()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching { mp.stop(); mp.release() }
            runCatching { am.setStreamVolume(stream, saved, 0) }
        }, 3000)
    }.isSuccess
    if (!played) runCatching { alarmTone?.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900) }
}

actual fun speak(text: String) {
    val ctx = appContext ?: return
    runCatching {
        if (tts == null) {
            tts = android.speech.tts.TextToSpeech(ctx) { status ->
                ttsReady = status == android.speech.tts.TextToSpeech.SUCCESS
                if (ttsReady) tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "overload")
            }
        } else if (ttsReady) {
            tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "overload")
        }
    }
}

private fun manager(): NotificationManager? =
    appContext?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

private fun ensureChannel(m: NotificationManager) {
    m.createNotificationChannel(
        NotificationChannel(CHANNEL_REST, "Rest timer", NotificationManager.IMPORTANCE_HIGH)
    )
}

/**
 * Tapping the rest notification must reopen the app. Without this the ongoing notification sat in
 * the tray doing nothing — and if the process was killed it couldn't even be dismissed.
 */
private fun openAppIntent(ctx: Context): android.app.PendingIntent? = runCatching {
    val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        ?.addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    android.app.PendingIntent.getActivity(
        ctx, 0, intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
    )
}.getOrNull()

actual fun notifyRest(endsAt: Long?) {
    val ctx = appContext ?: return
    val m = manager() ?: return
    ensureChannel(m)
    if (endsAt == null) {
        m.cancel(NOTIF_ID)
        return
    }
    runCatching {
        val n = NotificationCompat.Builder(ctx, CHANNEL_REST)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Resting")
            .setContentText("Rest timer running")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endsAt)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        m.notify(NOTIF_ID, n)
    }
}

actual fun notifyRestOver() {
    val ctx = appContext ?: return
    val m = manager() ?: return
    ensureChannel(m)
    runCatching {
        val n = NotificationCompat.Builder(ctx, CHANNEL_REST)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Rest over — GO!")
            .setContentText("Next set time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        m.notify(NOTIF_ID, n)
    }
}
