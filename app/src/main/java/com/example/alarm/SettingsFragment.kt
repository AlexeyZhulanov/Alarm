package com.example.alarm

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alarm.databinding.FragmentSettingsBinding
import com.example.alarm.model.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private var globalId: Long = 0
    private lateinit var mediaPlayer: MediaPlayer
    private val alarmViewModel: AlarmViewModel by viewModels()

    @SuppressLint("DiscouragedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        alarmViewModel.registerPreferences(requireContext())
        alarmViewModel.wallpaper.observe(viewLifecycleOwner) {
            updateWallpapers(it)
        }
        val (wallpaper, _) = alarmViewModel.getPreferencesWallpaperAndInterval(requireContext())
        if(wallpaper != "") {
            binding.wallpaperName.text = wallpaper
            val resId = resources.getIdentifier(wallpaper, "drawable", requireContext().packageName)
            if(resId != 0) binding.settingsLayout.background = ContextCompat.getDrawable(requireContext(), resId)
        }
        else {
            binding.wallpaperName.text = "Classic"
        }
        val themeNumber = alarmViewModel.getPreferencesTheme(requireContext())
        if(themeNumber != 0) binding.colorThemeName.text = "Theme ${themeNumber+1}" else binding.colorThemeName.text = "Classic"
        lifecycleScope.launch {
            val settings = alarmViewModel.getSettings()
            binding.melodyName.text = settings.melody
            binding.repeatRadioGroup.isEnabled = settings.repetitions == 1
            binding.switchVibration.isChecked = settings.vibration
            when(settings.repetitions) {
                3 -> binding.repeats3.isChecked = true
                5 -> binding.repeats5.isChecked = true
                else -> binding.repeatsInfinite.isChecked = true
            }
            when(settings.interval) {
                3 -> binding.interval3.isChecked = true
                5 -> binding.interval5.isChecked = true
                else -> binding.interval10.isChecked = true
            }
            globalId = settings.id
        }
        binding.changeMelody.setOnClickListener {
            showSignalsPopupMenu(it)
        }
        binding.playMelody.setOnClickListener {
            lifecycleScope.launch {
                val settings = async(Dispatchers.IO) { alarmViewModel.getSettings() }
                when (settings.await().melody) {
                    getString(R.string.melody1) -> playMelody(R.raw.default_signal1)
                    getString(R.string.melody2) -> playMelody(R.raw.default_signal2)
                    getString(R.string.melody3) -> playMelody(R.raw.default_signal3)
                    getString(R.string.melody4) -> playMelody(R.raw.default_signal4)
                    getString(R.string.melody5) -> playMelody(R.raw.default_signal5)
                    getString(R.string.melody6) -> playMelody(R.raw.signal)
                    getString(R.string.melody7) -> playMelody(R.raw.banjo_signal)
                    getString(R.string.melody8) -> playMelody(R.raw.morning_signal)
                    getString(R.string.melody9) -> playMelody(R.raw.simple_signal)
                    getString(R.string.melody10) -> playMelody(R.raw.fitness_signal)
                    getString(R.string.melody11) -> playMelody(R.raw.medieval_signal)
                    getString(R.string.melody12) -> playMelody(R.raw.introduction_signal)
                    else -> {
                        Toast.makeText(requireContext(), "Melody not selected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        binding.switchVibration.setOnClickListener {
            val s = readSettings(globalId)
            lifecycleScope.launch { alarmViewModel.updateSettings(s) }
        }
        binding.repeats3.setOnClickListener {
            val s = readSettings(globalId)
            lifecycleScope.launch { alarmViewModel.updateSettings(s) }
        }
        binding.repeats5.setOnClickListener {
            val s = readSettings(globalId)
            lifecycleScope.launch { alarmViewModel.updateSettings(s) }
        }
        binding.repeatsInfinite.setOnClickListener {
            val s = readSettings(globalId)
            lifecycleScope.launch { alarmViewModel.updateSettings(s) }
        }
        binding.interval3.setOnClickListener {
            val s = readSettings(globalId)
            lifecycleScope.launch { alarmViewModel.updateSettings(s) }
            updatePrefs(3)
        }
        binding.interval5.setOnClickListener {
            val s = readSettings(globalId)
            lifecycleScope.launch { alarmViewModel.updateSettings(s) }
            updatePrefs(5)
        }
        binding.interval10.setOnClickListener {
            val s = readSettings(globalId)
            lifecycleScope.launch { alarmViewModel.updateSettings(s) }
            updatePrefs(10)
        }
        binding.changeColorTheme.setOnClickListener {
            showColorThemePopupMenu(it, container as ViewGroup)
        }
        binding.changeWallpaper.setOnClickListener {
            showWallpapersPopupMenu(it, container as ViewGroup)
        }
        return binding.root
    }

    private fun readSettings(id: Long): Settings {
        val settings = Settings(
            id = id,
            melody = binding.melodyName.text.toString(),
            vibration = binding.switchVibration.isChecked,
            interval = if(binding.interval3.isChecked) 3
            else if(binding.interval5.isChecked) 5
            else 10,
            repetitions = if(binding.repeats3.isChecked) 3
            else if(binding.repeats5.isChecked) 5
            else 100
        )
        return settings
    }

    private fun updatePrefs(interval: Int) {
        alarmViewModel.editPreferencesInterval(requireContext(), interval)
    }
    private fun showSignalsPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.melody_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            val newMelodyName = when (menuItem.itemId) {
                R.id.melody1 -> getString(R.string.melody1)
                R.id.melody2 -> getString(R.string.melody2)
                R.id.melody3 -> getString(R.string.melody3)
                R.id.melody4 -> getString(R.string.melody4)
                R.id.melody5 -> getString(R.string.melody5)
                R.id.melody6 -> getString(R.string.melody6)
                R.id.melody7 -> getString(R.string.melody7)
                R.id.melody8 -> getString(R.string.melody8)
                R.id.melody9 -> getString(R.string.melody9)
                R.id.melody10 -> getString(R.string.melody10)
                R.id.melody11 -> getString(R.string.melody11)
                R.id.melody12 -> getString(R.string.melody12)
                else -> null
            }

            newMelodyName?.let { name ->
                binding.melodyName.text = name
                val s = readSettings(globalId)
                lifecycleScope.launch { alarmViewModel.updateSettings(s) }
                true
            } ?: false
        }
        popupMenu.show()
    }
    private fun showWallpapersPopupMenu(view: View, container: ViewGroup) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.popup_menu_wallpaper_layout, container, false)

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.showAtLocation(requireView(), Gravity.CENTER, 0, 0)
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val menuItems = listOf(
            MenuItemData("Classic", R.drawable.whitequad),
            MenuItemData("1.", R.drawable.wallpaper1),
            MenuItemData("2.", R.drawable.wallpaper2),
            MenuItemData("3.", R.drawable.wallpaper3),
            MenuItemData("4.", R.drawable.wallpaper4),
            MenuItemData("5.", R.drawable.wallpaper5),
            MenuItemData("6.", R.drawable.wallpaper6),
            MenuItemData("7.", R.drawable.wallpaper7),
            MenuItemData("8.", R.drawable.wallpaper8),
            MenuItemData("9.", R.drawable.wallpaper9),
            MenuItemData("10.", R.drawable.wallpaper10)
        )
        var temp = ""
        val adapter = PopupMenuWallpaperAdapter(menuItems) { menuItem ->
            temp = when(menuItem.title) {
                "Classic" -> ""
                "1." -> "wallpaper1"
                "2." -> "wallpaper2"
                "3." -> "wallpaper3"
                "4." -> "wallpaper4"
                "5." -> "wallpaper5"
                "6." -> "wallpaper6"
                "7." -> "wallpaper7"
                "8." -> "wallpaper8"
                "9." -> "wallpaper9"
                "10." -> "wallpaper10"
                else -> ""
            }
            alarmViewModel.editPreferencesWallpaper(requireContext(), temp)
            popupWindow.dismiss()
        }

        recyclerView.adapter = adapter

        popupWindow.showAsDropDown(view)
    }
    private fun showColorThemePopupMenu(view: View, container: ViewGroup) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.popup_menu_wallpaper_layout, container, false)

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.showAtLocation(requireView(), Gravity.CENTER, 0, 0)
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val menuItems = listOf(
            ColorThemeMenuItem(R.color.colorPrimary, R.color.colorAccent, 0),
            ColorThemeMenuItem(R.color.color1_main, R.color.color1_secondary, 1),
            ColorThemeMenuItem(R.color.color2_main, R.color.color2_secondary, 2),
            ColorThemeMenuItem(R.color.color3_main, R.color.color3_secondary, 3),
            ColorThemeMenuItem(R.color.color4_main, R.color.color4_secondary, 4),
            ColorThemeMenuItem(R.color.color5_main, R.color.color5_secondary, 5),
            ColorThemeMenuItem(R.color.color6_main, R.color.color6_secondary, 6),
            ColorThemeMenuItem(R.color.color7_main, R.color.color7_secondary, 7),
            ColorThemeMenuItem(R.color.color8_main, R.color.color8_secondary, 8)
        )
        val adapter = ColorThemeMenuAdapter(menuItems) { menuItem ->
            alarmViewModel.editPreferencesTheme(requireContext(), menuItem.themeNumber)
            requireActivity().recreate()
            popupWindow.dismiss()
        }

        recyclerView.adapter = adapter

        popupWindow.showAsDropDown(view)
    }

    @SuppressLint("DiscouragedApi")
    private fun updateWallpapers(wallpaper: String) {
        binding.wallpaperName.text = wallpaper
        if(wallpaper != "") {
            val resId = resources.getIdentifier(wallpaper, "drawable", requireContext().packageName)
            if(resId != 0)
                binding.settingsLayout.background = ContextCompat.getDrawable(requireContext(), resId)
        }
        else {
            binding.settingsLayout.background = null
            binding.wallpaperName.text = "Classic"
        }
    }

    private fun playMelody(signalId: Int) {
        if (::mediaPlayer.isInitialized) {
            stopAndReleaseMediaPlayer()
        }

        mediaPlayer = MediaPlayer.create(context, signalId)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            stopAndReleaseMediaPlayer()
            binding.floatingActionButtonVolumeOff.visibility = View.GONE
        }
        binding.floatingActionButtonVolumeOff.visibility = View.VISIBLE
        binding.floatingActionButtonVolumeOff.setOnClickListener {
            stopAndReleaseMediaPlayer()
            binding.floatingActionButtonVolumeOff.visibility = View.GONE
        }
    }

    private fun stopAndReleaseMediaPlayer() {
        if (::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
            mediaPlayer = MediaPlayer()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        alarmViewModel.unregisterPreferences(requireContext())
    }
}