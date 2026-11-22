package dev.airdeck

import android.app.Application
import dev.airdeck.data.AirDeckPrefs
import dev.airdeck.hid.BluetoothHidController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AirDeckApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var prefs: AirDeckPrefs
        private set

    lateinit var hidController: BluetoothHidController
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = AirDeckPrefs(this)
        hidController = BluetoothHidController(
            context = this,
            onHostConnected = { address ->
                appScope.launch {
                    prefs.setLastHostAddress(address)
                }
            },
        )
    }
}
