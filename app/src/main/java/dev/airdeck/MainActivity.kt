package dev.airdeck

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.airdeck.ui.AirDeckScreen
import dev.airdeck.ui.AirDeckTheme

class MainActivity : ComponentActivity() {

    private val app: AirDeckApp
        get() = application as AirDeckApp

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        app.hidController.start()
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        app.hidController.start()
    }

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        app.hidController.refreshPairedHosts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyImmersiveFullscreen()

        if (app.hidController.hasBluetoothPermissions()) {
            app.hidController.start()
        } else {
            val perms = app.hidController.requiredPermissions()
            if (perms.isNotEmpty()) {
                permissionLauncher.launch(perms)
            } else {
                app.hidController.start()
            }
        }

        setContent {
            AirDeckTheme {
                AirDeckScreen(
                    hidController = app.hidController,
                    prefs = app.prefs,
                    onRequestPermissions = {
                        val perms = app.hidController.requiredPermissions()
                        if (perms.isNotEmpty()) {
                            permissionLauncher.launch(perms)
                        } else {
                            app.hidController.start()
                        }
                    },
                    onEnableBluetooth = {
                        try {
                            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        } catch (_: Throwable) {
                            openBluetoothSettings()
                        }
                    },
                    onMakeDiscoverable = {
                        try {
                            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                            }
                            discoverableLauncher.launch(intent)
                        } catch (_: Throwable) {
                            openBluetoothSettings()
                        }
                    },
                    onOpenBluetoothSettings = { openBluetoothSettings() },
                    onKeepScreenOnChange = { keepOn ->
                        if (keepOn) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    },
                    onDimScreenChange = { dimmed ->
                        val params = window.attributes
                        params.screenBrightness = if (dimmed) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        window.attributes = params
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveFullscreen()
        if (app.hidController.hasBluetoothPermissions()) {
            app.hidController.start()
            app.hidController.refreshPairedHosts()
        }
    }

    override fun onPause() {
        super.onPause()
        app.hidController.releaseAll()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveFullscreen()
        }
    }

    private fun applyImmersiveFullscreen() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun openBluetoothSettings() {
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (_: Throwable) {
        }
    }
}
