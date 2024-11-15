package com.example.alarm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.Calendar
import android.icu.util.ULocale
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.alarm.databinding.FragmentSignalBinding
import com.example.alarm.model.Alarm
import com.example.alarm.model.MyAlarmManager
import com.example.alarm.model.Settings
import com.ncorti.slidetoact.SlideToActView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SignalFragment(
    val name: String,
    val id: Long,
    val settings: Settings? = Settings(0)
) : Fragment() {

    private val alarmPlug = Alarm(id = id, name = name)
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private var originalMusicVolume: Int = 0
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("testSignalFrag", "works")
        val binding = FragmentSignalBinding.inflate(inflater, container, false)

        val updateWorkRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInputData(workDataOf("alarmId" to alarmPlug.id, "enabled" to 0))
            .build()

        WorkManager.getInstance(requireContext()).enqueue(updateWorkRequest)
        selectMelody(settings!!)
        if(settings.vibration) startVibrator()
        val tmp = Calendar.getInstance().time.toString()
        val str = tmp.split(" ")
        val date = "${str[0]} ${str[1]} ${str[2]}"
        val tmpTime = str[3].split(":")
        val time = "${tmpTime[0]}:${tmpTime[1]}"
        binding.currentTimeTextView.text = time
        binding.currentDateTextView.text = date
        binding.nameTextView.text = name
        val fragmentContext = requireContext()
        if(settings.repetitions <= 0) {
            binding.repeatButton.visibility = View.GONE
        }
        else {
            binding.pulsator.start()
            binding.repeatButton.setOnClickListener {
                dropAndRepeatFragment()
            }
        }
        binding.slideButton.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                uiScope.launch {
                    MyAlarmManager(fragmentContext, alarmPlug, Settings(0)).endProcess()
                }
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        return binding.root
    }

    fun dropAndRepeatFragment() {
        settings!!.repetitions -= 1
        if(settings!!.repetitions > -1) {
            uiScope.launch {
                val ctx = requireContextOrNull()
                if (ctx == null) {
                    Log.e("dropAndRepeatFragment", "Context is null")
                    return@launch
                }
                MyAlarmManager(ctx, alarmPlug, Settings(0)).endProcess()
                MyAlarmManager(ctx, alarmPlug, settings).repeatProcess()
                showTurnOffNotification()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    // Helper function to safely get context
    private fun Fragment.requireContextOrNull(): Context? {
        return if (isAdded) requireContext() else null
    }

    @SuppressLint("LaunchActivityFromNotification")
    fun showTurnOffNotification() {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "basic_channel_id"
        val channelName = "Basic Notifications"

        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW).apply {
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                description = "Alarm notification"
                setSound(null, null)
            }
        notificationManager.createNotificationChannel(channel)

        val updateWorkRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInputData(workDataOf("alarmId" to id, "enabled" to 0))
            .build()

        WorkManager.getInstance(requireContext()).enqueue(updateWorkRequest)

        val filter = IntentFilter(LOCAL_BROADCAST_KEY3)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(turnOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        }
        val turnOffIntent = Intent(LOCAL_BROADCAST_KEY3).apply {
            putExtra("alarmId", id)
            putExtra("notificationId",3)
        }
        val turnOffPendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            turnOffIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance(ULocale.ROOT)
        calendar.timeInMillis = System.currentTimeMillis() + settings!!.interval.toLong()*60000
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        var hoursText = hours.toString()
        var minutesText = minutes.toString()
        if(hours <= 9) hoursText = "0$hoursText"
        if(minutes <= 9) minutesText = "0$minutesText"
        val notificationBuilder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.mipmap.ic_alarm_adaptive_fore)
            .setContentTitle("Будильник")
            .setContentText("Повтор сигнала сработает в $hoursText:$minutesText")
            .addAction(R.drawable.ic_clear, "Turn Off", turnOffPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(null)
            .setFullScreenIntent(turnOffPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)

        notificationManager.notify(3, notificationBuilder.build())
    }

    private val turnOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val cont = context
            uiScope.launch {
                val alarmId = intent.getLongExtra("alarmId", 0)
                val notificationId = intent.getIntExtra("notificationId",-1)
                val alarmPlug = Alarm(alarmId)
                MyAlarmManager(cont, alarmPlug, Settings(0)).endProcess()
                val notificationManager =
                    cont.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }
        }
    }

    private fun selectMelody(settings: Settings) {
        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaPlayer = when (settings.melody) {
            getString(R.string.melody1) -> MediaPlayer.create(context, R.raw.default_signal1)
            getString(R.string.melody2) -> MediaPlayer.create(context, R.raw.default_signal2)
            getString(R.string.melody3) -> MediaPlayer.create(context, R.raw.default_signal3)
            getString(R.string.melody4) -> MediaPlayer.create(context, R.raw.default_signal4)
            getString(R.string.melody5) -> MediaPlayer.create(context, R.raw.default_signal5)
            getString(R.string.melody6) -> MediaPlayer.create(context, R.raw.signal)
            getString(R.string.melody7) -> MediaPlayer.create(context, R.raw.banjo_signal)
            getString(R.string.melody8) -> MediaPlayer.create(context, R.raw.morning_signal)
            getString(R.string.melody9) -> MediaPlayer.create(context, R.raw.simple_signal)
            getString(R.string.melody10) -> MediaPlayer.create(context, R.raw.fitness_signal)
            getString(R.string.melody11) -> MediaPlayer.create(context, R.raw.medieval_signal)
            getString(R.string.melody12) -> MediaPlayer.create(context, R.raw.introduction_signal)
            else -> MediaPlayer.create(context, R.raw.default_signal1)
        }
            mediaPlayer.isLooping = true
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            // Store original music volume
            originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            // Get current alarm volume
            val currentAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

            // Set music volume to match alarm volume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentAlarmVolume, 0)

        // Build AudioFocusRequest
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
        audioManager.requestAudioFocus(focusRequest)
        mediaPlayer.start()
    }

    private fun startVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 100, 300, 200, 250, 300, 200, 400, 150, 300, 150, 200)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))

    }
    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer.stop()
        mediaPlayer.release()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0) // Restore original music volume
        audioManager.abandonAudioFocusRequest(focusRequest) // Abandon audio focus request
        if(settings!!.vibration) vibrator.cancel()
    }
}
const val LOCAL_BROADCAST_KEY3 = "alarm_turnoff"