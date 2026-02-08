package com.example.alarm

import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.alarm.databinding.FragmentSignalBinding
import com.example.alarm.model.Alarm
import com.example.alarm.model.AlarmForegroundService
import com.example.alarm.model.AlarmWorker
import com.example.alarm.model.MyAlarmManager
import com.example.alarm.model.Settings
import com.ncorti.slidetoact.SlideToActView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignalFragment(
    val name: String,
    val id: Long,
    val settings: Settings,
    val myAlarmManager: MyAlarmManager
) : Fragment() {

    private val alarmPlug = Alarm(id = id, name = name)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSignalBinding.inflate(inflater, container, false)
        // todo проверить нужно ли это вообще???
        val updateWorkRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInputData(workDataOf("alarmId" to id))
            .build()
        WorkManager.getInstance(requireContext()).enqueue(updateWorkRequest)
        // todo --------------------------------
        val tmp = Calendar.getInstance().time.toString()
        val str = tmp.split(" ")
        val date = "${str[0]} ${str[1]} ${str[2]}"
        val tmpTime = str[3].split(":")
        val time = "${tmpTime[0]}:${tmpTime[1]}"
        binding.currentTimeTextView.text = time
        binding.currentDateTextView.text = date
        binding.nameTextView.text = name
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
                    myAlarmManager.endProcess(alarmPlug)
                }
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        return binding.root
    }

    fun dropAndRepeatFragment() {
        requireContext().stopService(
            Intent(requireContext(), AlarmForegroundService::class.java)
        )
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().stopService(
            Intent(requireContext(), AlarmForegroundService::class.java)
        )
    }
}