package com.example.alarm

import android.annotation.SuppressLint
import android.graphics.Rect
import android.icu.util.Calendar
import android.icu.util.ULocale
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alarm.databinding.FragmentAlarmBinding
import com.example.alarm.model.Alarm
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class AlarmFragment : Fragment() {

    private lateinit var adapter: AlarmsAdapter
    private lateinit var binding: FragmentAlarmBinding
    private var updateJob: Job? = null

    private var millisToAlarm = mutableMapOf<Long, Long>()
    private val alarmViewModel: AlarmViewModel by viewModels()
    @SuppressLint("DiscouragedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAlarmBinding.inflate(inflater, container, false)
                val (wallpaper, interval) = alarmViewModel.getPreferencesWallpaperAndInterval(requireContext())
                if(wallpaper != "") {
                    val resId = resources.getIdentifier(wallpaper, "drawable", requireContext().packageName)
                    if(resId != 0)
                        binding.alarmLayout.background = ContextCompat.getDrawable(requireContext(), resId)
                }
                adapter = AlarmsAdapter(interval, object : AlarmActionListener {
                    override fun onAlarmEnabled(alarm: Alarm, index: Int) {
                        lifecycleScope.launch {
                            var bool = false
                            if (!alarm.enabled) {
                                bool = true
                                changeAlarmTime(alarm, false)
                                binding.barTextView.text = updateBar()
                            } else {
                                changeAlarmTime(alarm, true)
                                binding.barTextView.text = updateBar()
                            }
                            val idx = async(Dispatchers.IO) {
                                alarmViewModel.updateEnabledAlarm(alarm, bool, requireContext(), index)
                            }
                            adapter.notifyItemChanged(idx.await()) //костыль, но по-другому нет идей как сделать
                        }
                    }
                    override fun onAlarmChange(alarm: Alarm) {
                        BottomSheetFragment(false, alarm, object : BottomSheetListener {
                            override fun onAddAlarm(alarm: Alarm) {
                                return
                            }

                            override fun onChangeAlarm(alarmOld: Alarm, alarmNew: Alarm) {
                                if (alarmNew.enabled) {
                                    changeAlarmTime(alarmOld, true)
                                    changeAlarmTime(alarmNew, false)
                                    binding.barTextView.text = updateBar()
                                }
                            }
                        }).show(childFragmentManager, "ChangeTag")
                    }

                    override fun onAlarmLongClicked() {
                        binding.floatingActionButtonAdd.visibility = View.GONE
                        binding.floatingActionButtonDelete.visibility = View.VISIBLE
                        requireActivity().onBackPressedDispatcher.addCallback(
                            viewLifecycleOwner,
                            object : OnBackPressedCallback(true) {
                                @SuppressLint("NotifyDataSetChanged")
                                override fun handleOnBackPressed() {
                                    if (!adapter.canLongClick) {
                                        adapter.clearPositions()
                                        adapter.notifyDataSetChanged()
                                        binding.floatingActionButtonDelete.visibility = View.GONE
                                        binding.floatingActionButtonAdd.visibility = View.VISIBLE
                                        alarmViewModel.getAndNotify()
                                    } else {
                                        //Removing this callback
                                        remove()
                                        requireActivity().onBackPressedDispatcher.onBackPressed()
                                    }
                                }
                            })
                        binding.floatingActionButtonDelete.setOnClickListener {
                            val alarmsToDelete = adapter.getDeleteList()
                            if (alarmsToDelete.isNotEmpty()) {
                                    alarmViewModel.deleteAlarms(alarmsToDelete, context)
                                    for (a in alarmsToDelete) {
                                        if (a.enabled) changeAlarmTime(a, true)
                                    }
                                    binding.barTextView.text = updateBar()
                                binding.floatingActionButtonDelete.visibility = View.GONE
                                binding.floatingActionButtonAdd.visibility = View.VISIBLE
                                adapter.clearPositions()

                            }
                        }
                    }
                })
                val layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerview.layoutManager = layoutManager
                binding.recyclerview.adapter = adapter
                binding.recyclerview.addItemDecoration(VerticalSpaceItemDecoration(40))
                alarmViewModel.alarms.observe(viewLifecycleOwner) {
                    adapter.alarms = it
                }
                (activity as AppCompatActivity?)!!.setSupportActionBar(binding.toolbar) //adds a button
                binding.floatingActionButtonAdd.setOnClickListener {
                    BottomSheetFragment(true, Alarm(0), object : BottomSheetListener {
                        override fun onAddAlarm(alarm: Alarm) {
                                var id: Long = 0
                                for (a in adapter.alarms) {
                                    if (a.timeHours == alarm.timeHours && a.timeMinutes == alarm.timeMinutes) {
                                        id = a.id
                                        break
                                    }
                                }
                                val alr = Alarm(
                                    id = id,
                                    timeHours = alarm.timeHours,
                                    timeMinutes = alarm.timeMinutes,
                                    name = alarm.name,
                                    enabled = alarm.enabled
                                )
                                changeAlarmTime(alr, false)
                                binding.barTextView.text = updateBar()
                        }

                        override fun onChangeAlarm(alarmOld: Alarm, alarmNew: Alarm) {
                            return
                        }
                    }).show(childFragmentManager, "AddTag")
                }
        alarmViewModel.initCompleted.observe(viewLifecycleOwner) {
            if (it) {
                updateJob = lifecycleScope.launch {
                    millisToAlarm = fillAlarmsTime()
                    while (isActive) {
                        binding.barTextView.text = updateBar()
                        delay(30000)
                    }
                }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateJob?.cancel()
    }

    private suspend fun fillAlarmsTime() : MutableMap<Long, Long> = withContext(Dispatchers.Default){
        val map = mutableMapOf<Long, Long>()
        val calendar = Calendar.getInstance()
        val calendar2 = Calendar.getInstance(ULocale.ROOT)
        for(alr in adapter.alarms) {
            if(alr.enabled) {
                calendar.set(Calendar.HOUR_OF_DAY, alr.timeHours)
                calendar.set(Calendar.MINUTE, alr.timeMinutes)
                calendar.set(Calendar.SECOND, 0)
                val longTime: Long = if (calendar2.timeInMillis > calendar.timeInMillis) {
                    calendar.timeInMillis + 86400000
                } else calendar.timeInMillis
                map[alr.id] = longTime
            }
        }
        val sortedMap = map.toList().sortedBy { it.second }.toMap().toMutableMap()
        return@withContext sortedMap
    }

     private fun changeAlarmTime(alarm: Alarm, isDisable: Boolean) {
        if(isDisable) {
            millisToAlarm.remove(alarm.id)
        }
        else {
            val calendar = Calendar.getInstance()
            val calendar2 = Calendar.getInstance(ULocale.ROOT)
            calendar.set(Calendar.HOUR_OF_DAY, alarm.timeHours)
            calendar.set(Calendar.MINUTE, alarm.timeMinutes)
            calendar.set(Calendar.SECOND, 0)
            val longTime: Long = if (calendar2.timeInMillis > calendar.timeInMillis) {
                calendar.timeInMillis + 86400000
            } else calendar.timeInMillis
            millisToAlarm[alarm.id] = longTime
            millisToAlarm = millisToAlarm.toList().sortedBy { it.second }.toMap().toMutableMap()
        }
    }
    private fun updateBar(): String {
        var txt: String = ""
        if(millisToAlarm.isEmpty()) txt += "Все сигналы\nвыключены"
        else {
            val calendar = Calendar.getInstance(ULocale.ROOT)
            val longTime: Long = millisToAlarm.entries.first().value
            val minutes: Int = if (calendar.timeInMillis > longTime) {
                ((longTime - calendar.timeInMillis) / 60000).toInt()
            } else ((longTime - calendar.timeInMillis) / 60000).toInt()
            when(minutes) {
                0 -> txt += "Звонок менее чем через 1 мин."
                in 1..59 -> txt += "Звонок через\n$minutes мин."
                else -> {
                    val hours = minutes / 60
                    txt += "Звонок через\n$hours ч. ${minutes % 60} мин."
                }
            }
        }
        return txt
    }

    suspend fun fillAndUpdateBar() = withContext(Dispatchers.Default) {
        millisToAlarm = fillAlarmsTime()
        binding.barTextView.text = updateBar()
    }

    override fun onPause() {
        super.onPause()
        updateJob?.cancel()
    }
}

class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.bottom = verticalSpaceHeight
    }
}