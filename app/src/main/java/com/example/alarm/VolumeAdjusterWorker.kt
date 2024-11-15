package com.example.alarm

import android.content.Context
import android.media.AudioManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class VolumeAdjusterWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // Get current alarm volume
        val currentAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        // Set music volume to match alarm volume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentAlarmVolume, 0)

        return Result.success()
    }
}