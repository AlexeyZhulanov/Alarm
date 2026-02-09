package com.example.alarm.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import com.example.alarm.R
import com.example.alarm.SignalActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmForegroundService : Service() {

    companion object {
        const val ACTION_STOP = "alarm_stop"
        const val ACTION_SNOOZE = "alarm_snooze"
        const val NOTIFICATION_ID = 1001
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private var originalMusicVolume = 0
    private var vibrator: Vibrator? = null
    private var started = false

    @Inject
    lateinit var myAlarmManager: MyAlarmManager

    @Inject
    lateinit var alarmService: AlarmService

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val alarmId = intent.getLongExtra("alarmId", 0L)

                CoroutineScope(Dispatchers.IO).launch {
                    alarmService.updateEnabled(alarmId, false)
                }
                stopAlarm()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                handleSnooze(intent)
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getLongExtra("alarmId", 0L) ?: 0L
        val alarmName = intent?.getStringExtra("alarmName") ?: ""
        val settings = intent?.let {
            IntentCompat.getParcelableExtra(
                it,
                "settings",
                Settings::class.java
            )
        } ?: Settings(0)

        startForeground(
            NOTIFICATION_ID,
            buildNotification(alarmId, alarmName, settings)
        )
        if(!started) {
            started = true
            startSound(settings)
            if (settings.vibration) startVibration()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(
        alarmId: Long,
        alarmName: String,
        settings: Settings
    ): Notification {

        val channelId = getString(R.string.basic_channel_id)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            getString(R.string.basic_notifications),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.alarm_notification)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)

        val fullScreenIntent = Intent(this, SignalActivity::class.java).apply {
            putExtra("alarmId", alarmId)
            putExtra("alarmName", alarmName)
            putExtra("settings", settings)
        }

        val fullScreenPendingIntent =
            PendingIntent.getActivity(
                this,
                alarmId.toInt(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val stopIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = ACTION_STOP
            putExtra("alarmId", alarmId)
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("alarmId", alarmId)
            putExtra("alarmName", alarmName)
            putExtra("settings", settings)
        }

        val snoozePending = PendingIntent.getService(
            this,
            1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_alarm_adaptive_fore)
            .setContentTitle(getString(R.string.alarm))
            .setContentText(alarmName)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                R.drawable.ic_clear,
                getString(R.string.turn_off),
                stopPendingIntent
            )
            .addAction(
                R.drawable.ic_repeat,
                getString(R.string.repeat),
                snoozePending
            )
            .build()
    }

    private fun startSound(settings: Settings) {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()

        val focusResult = audioManager.requestAudioFocus(focusRequest)

        if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer = selectMelody(settings)
            mediaPlayer.isLooping = true
            mediaPlayer.start()
        }
    }

    private fun selectMelody(settings: Settings): MediaPlayer {
        val resId = when (settings.melody) {
            getString(R.string.melody1) -> R.raw.default_signal1
            getString(R.string.melody2) -> R.raw.default_signal2
            getString(R.string.melody3) -> R.raw.default_signal3
            getString(R.string.melody4) -> R.raw.default_signal4
            getString(R.string.melody5) -> R.raw.default_signal5
            getString(R.string.melody6) -> R.raw.signal
            getString(R.string.melody7) -> R.raw.banjo_signal
            getString(R.string.melody8) -> R.raw.morning_signal
            getString(R.string.melody9) -> R.raw.simple_signal
            getString(R.string.melody10) -> R.raw.fitness_signal
            getString(R.string.melody11) -> R.raw.medieval_signal
            getString(R.string.melody12) -> R.raw.introduction_signal
            else -> R.raw.default_signal1
        }

        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            val afd = resources.openRawResourceFd(resId)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            prepare()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 100, 300, 200, 250, 300, 200, 400, 150, 300, 150, 200)

        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopAlarm() {
        try {
            mediaPlayer.stop()
            mediaPlayer.release()
        } catch (_: Exception) {}

        vibrator?.cancel()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private fun handleSnooze(intent: Intent) {
        val alarmId = intent.getLongExtra("alarmId", 0L)
        val alarmName = intent.getStringExtra("alarmName") ?: ""
        val settings = IntentCompat.getParcelableExtra(
            intent,
            "settings",
            Settings::class.java
        ) ?: return

        stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        if (settings.repetitions <= 0) {
            CoroutineScope(Dispatchers.IO).launch {
                alarmService.updateEnabled(alarmId, false)
            }
            return
        }

        settings.repetitions -= 1

        val alarm = Alarm(id = alarmId, name = alarmName)

        CoroutineScope(Dispatchers.Default).launch {
            myAlarmManager.repeatProcess(alarm, settings)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
