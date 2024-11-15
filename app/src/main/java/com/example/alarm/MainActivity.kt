package com.example.alarm

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.alarm.databinding.ActivityMainBinding
import com.example.alarm.model.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    @Inject
    lateinit var alarmsService: AlarmService
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    private val fragmentListener = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            super.onFragmentViewCreated(fm, f, v, savedInstanceState)
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
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
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
        checkOverlayPermission(this)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, AlarmFragment(), "ALARM_FRAGMENT_TAG")
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
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
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
                    alarmsService.offAlarms(applicationContext)
                    (supportFragmentManager.findFragmentByTag("ALARM_FRAGMENT_TAG") as? AlarmFragment)?.fillAndUpdateBar()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkOverlayPermission(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Необходимы разрешения")
            builder.setMessage("Пожалуйста, предоставьте разрешение на наложение поверх других приложений для правильной работы будильника.")
            builder.setPositiveButton("Настройки") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
            builder.setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
    }
}
