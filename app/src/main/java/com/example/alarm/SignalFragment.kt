package com.example.alarm

import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.alarm.databinding.FragmentSignalBinding
import com.example.alarm.model.AlarmForegroundService
import com.example.alarm.model.MyAlarmManager
import com.example.alarm.model.Settings
import com.ncorti.slidetoact.SlideToActView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignalFragment : Fragment() {

    private var name: String? = null
    private var id: Long? = null
    private var settings: Settings? = null

    @Inject
    lateinit var myAlarmManager: MyAlarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        name = arguments?.getString(ARG_NAME)
        id = arguments?.getLong(ARG_ID)

        settings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_SETTINGS, Settings::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_SETTINGS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSignalBinding.inflate(inflater, container, false)
        val tmp = Calendar.getInstance().time.toString()
        val str = tmp.split(" ")
        val date = "${str[0]} ${str[1]} ${str[2]}"
        val tmpTime = str[3].split(":")
        val time = "${tmpTime[0]}:${tmpTime[1]}"
        binding.currentTimeTextView.text = time
        binding.currentDateTextView.text = date
        binding.nameTextView.text = name

        fun setupRepeatButton() {
            binding.pulsator.start()
            binding.repeatButton.setOnClickListener {
                snoozeFromFragment()
            }
        }
        settings?.let { st ->
            if (st.repetitions <= 0) {
                binding.repeatButton.visibility = View.GONE
            } else setupRepeatButton()
        } ?: setupRepeatButton()

        binding.slideButton.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                stopFromFragment()
            }
        }
        return binding.root
    }

    private fun stopFromFragment() {
        val intent = Intent(
            requireContext(),
            AlarmForegroundService::class.java
        ).apply {
            action = AlarmForegroundService.ACTION_STOP
            putExtra("alarmId", id)
        }

        ContextCompat.startForegroundService(requireContext(), intent)
        requireActivity().finish()
    }

    fun snoozeFromFragment() {
        val intent = Intent(
            requireContext(),
            AlarmForegroundService::class.java
        ).apply {
            action = AlarmForegroundService.ACTION_SNOOZE
            putExtra("alarmId", id)
            putExtra("alarmName", name)
            putExtra("settings", settings)
        }

        ContextCompat.startForegroundService(requireContext(), intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().stopService(
            Intent(requireContext(), AlarmForegroundService::class.java)
        )
    }

    companion object {
        private const val ARG_NAME = "name_arg"
        private const val ARG_ID = "id_arg"
        private const val ARG_SETTINGS = "settings_arg"

        fun newInstance(name: String? = null, id: Long = -1, settings: Settings? = null) = SignalFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_NAME, name)
                putLong(ARG_ID, id)
                putParcelable(ARG_SETTINGS, settings)
            }
        }
    }
}