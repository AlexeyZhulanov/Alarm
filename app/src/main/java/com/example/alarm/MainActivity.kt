package com.example.alarm

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.example.alarm.databinding.ActivityMainBinding
import com.example.alarm.model.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.view.size
import androidx.core.view.get
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.alarm.model.MyAlarmManager

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var alarmsService: AlarmService

    @Inject
    lateinit var myAlarmManager: MyAlarmManager

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    private val fragmentListener = object : FragmentManager.FragmentLifecycleCallbacks() {}

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)
        val themeNumber = preferences.getInt(PREF_THEME, 0)
        when(themeNumber) {
            0 -> setTheme(R.style.Theme_Alarm)
            1 -> setTheme(R.style.Theme1)
            2 -> setTheme(R.style.Theme2)
            3 -> setTheme(R.style.Theme3)
            4 -> setTheme(R.style.Theme4)
            5 -> setTheme(R.style.Theme5)
            6 -> setTheme(R.style.Theme6)
            7 -> setTheme(R.style.Theme7)
            8 -> setTheme(R.style.Theme8)
            else -> setTheme(R.style.Theme_Alarm)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
        checkOverlayPermission(this)

        if (savedInstanceState == null) {
            restoreEnabledAlarms()

            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, AlarmFragment(), getString(R.string.alarm_fragment_tag))
                .commit()
        }
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentListener, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.alarm_menu, menu)
        val typedValue = TypedValue()
        theme.resolveAttribute(androidx.appcompat.R.attr.colorAccent, typedValue, true)
        val color = typedValue.data
        applyMenuTextColor(menu, color)
        return true
    }

    private fun applyMenuTextColor(menu: Menu, color: Int) {
        for (i in 0 until menu.size) {
            val menuItem = menu[i]
            val spannableTitle = SpannableString(menuItem.title)
            spannableTitle.setSpan(ForegroundColorSpan(color), 0, spannableTitle.length, 0)
            menuItem.title = spannableTitle
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.open_settings -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, SettingsFragment())
                    .addToBackStack("settings")
                    .commit()
                true
            }
            R.id.off_alarms -> {
                uiScope.launch {
                    alarmsService.offAlarms()
                    (supportFragmentManager.findFragmentByTag(getString(R.string.alarm_fragment_tag)) as? AlarmFragment)?.fillAndUpdateBar()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkOverlayPermission(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.need_permissions))
            builder.setMessage(getString(R.string.please_gain_permissions))
            builder.setPositiveButton(getString(R.string.settings_txt)) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            }
            builder.setNegativeButton(getString(R.string.cancel_txt)) { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
    }

    private fun restoreEnabledAlarms() {
        uiScope.launch(Dispatchers.IO) {
            val alarms = alarmsService.getAlarms()
            val settings = alarmsService.getSettings()
            alarms.filter { it.enabled }.forEach { alarm -> myAlarmManager.startProcess(alarm, settings) }
        }
    }
}
