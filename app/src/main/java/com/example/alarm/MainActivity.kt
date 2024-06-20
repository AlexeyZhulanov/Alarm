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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.alarm.databinding.ActivityMainBinding
import com.example.alarm.model.AlarmService
import com.example.alarm.model.AppVisibilityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private val alarmsService: AlarmService
        get() = Repositories.alarmRepository as AlarmService

    private val currentFragment: Fragment
        get() = supportFragmentManager.findFragmentById(R.id.fragmentContainer)!!

    private val fragmentListener = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            super.onFragmentViewCreated(fm, f, v, savedInstanceState)
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
        private const val FULL_SCREEN_INTENT_PERMISSION_REQUEST_CODE = 1001
        private const val FOREGROUND_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
                .add(R.id.fragmentContainer, AlarmFragment())
                .commit()
        }
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentListener, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.alarm_menu, menu)
        return true
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
                uiScope.launch { alarmsService.offAlarms(applicationContext) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        AppVisibilityTracker.setAppVisible(true)
    }

    override fun onPause() {
        super.onPause()
        AppVisibilityTracker.setAppVisible(false)
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
