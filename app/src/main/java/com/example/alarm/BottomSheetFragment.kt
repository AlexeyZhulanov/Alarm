package com.example.alarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.alarm.databinding.FragmentBottomsheetBinding
import com.example.alarm.model.Alarm
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

interface BottomSheetListener {
    fun onAddAlarm(alarm: Alarm)
    fun onChangeAlarm(alarmOld: Alarm, alarmNew: Alarm)
}

@AndroidEntryPoint
class BottomSheetFragment(
    private val isAdd: Boolean,
    private val oldAlarm: Alarm,
    private val alarmViewModel: AlarmViewModel,
    private val bottomSheetListener: BottomSheetListener
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentBottomsheetBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.timePicker.setIs24HourView(true)
        if(isAdd) {
            binding.timePicker.hour = 7
            binding.timePicker.minute = 0
        }
        else {
            binding.heading.text = getString(R.string.change_alarm)
            binding.timePicker.hour = oldAlarm.timeHours
            binding.timePicker.minute = oldAlarm.timeMinutes
            if((oldAlarm.name != "default") && (oldAlarm.name != "")) binding.signalName.setText(oldAlarm.name)
        }
        binding.confirmButton.setOnClickListener {
            if(isAdd) { addNewAlarm() }
            else { changeAlarm(oldAlarm) }
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun addNewAlarm() {
        val alarm = Alarm(
            id = 0,
            timeHours = binding.timePicker.hour,
            timeMinutes = binding.timePicker.minute,
            name = if(binding.signalName.text.toString() == "") "default" else binding.signalName.text.toString(),
            enabled = true
        )
        lifecycleScope.launch {
            alarmViewModel.addAlarm(alarm, requireContext()) { success ->
                if(success) {
                    bottomSheetListener.onAddAlarm(alarm)
                } else {
                    Toast.makeText(context, getString(R.string.error_is_exist), Toast.LENGTH_SHORT).show()
                }
            }
            dismiss()
        }
    }
    private fun changeAlarm(oldAlarm: Alarm) {
        val alarmNew = Alarm(
            id = oldAlarm.id,
            timeHours = binding.timePicker.hour,
            timeMinutes = binding.timePicker.minute,
            name = if(binding.signalName.text.toString() == "") "default" else binding.signalName.text.toString(),
            enabled = oldAlarm.enabled
        )
        lifecycleScope.launch {
            alarmViewModel.updateAlarm(alarmNew, requireContext()) { success ->
                if(success) {
                    bottomSheetListener.onChangeAlarm(oldAlarm, alarmNew)
                } else {
                    Toast.makeText(context, getString(R.string.error_is_exist), Toast.LENGTH_SHORT).show()
                }
            }
            dismiss()
        }
    }
}