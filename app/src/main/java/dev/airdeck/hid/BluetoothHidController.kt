package dev.airdeck.hid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.math.truncate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PairedHost(
    val name: String,
    val address: String,
    val majorDeviceClass: Int,
)

sealed interface HidConnectionState {
    data object Unsupported : HidConnectionState
    data object PermissionRequired : HidConnectionState
    data object BluetoothDisabled : HidConnectionState
    data object Registering : HidConnectionState
    data object Ready : HidConnectionState
    data class Connecting(val deviceName: String, val address: String) : HidConnectionState
    data class Connected(val deviceName: String, val address: String) : HidConnectionState
    data class Error(val detail: String) : HidConnectionState
}

/**
 * Manages the Android BluetoothHidDevice profile so this phone/tablet acts as a wireless
 * Bluetooth keyboard, touchpad (mouse + scroll), and TV media remote.
 */
class BluetoothHidController(
    private val context: Context,
    private val onHostConnected: (address: String) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val btManager = appContext.getSystemService(BluetoothManager::class.java)
    private val btAdapter: BluetoothAdapter? = btManager?.adapter
    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<HidConnectionState>(HidConnectionState.Registering)
    val state: StateFlow<HidConnectionState> = _state.asStateFlow()

    private val _pairedHosts = MutableStateFlow<List<PairedHost>>(emptyList())
    val pairedHosts: StateFlow<List<PairedHost>> = _pairedHosts.asStateFlow()

    private val stateLock = Any()
    @Volatile private var hidDevice: BluetoothHidDevice? = null
    @Volatile private var connectedDevice: BluetoothDevice? = null
    @Volatile private var isRegistered = false
    @Volatile private var isProxyRequested = false
    @Volatile private var pendingConnectAddress: String? = null

    // Live HID input state (protected by stateLock)
    private var mouseButtons: Int = 0
    private var activeModifiers: Int = 0
    private val pressedKeys = LinkedHashSet<Int>()
    private var pendingDx: Float = 0f
    private var pendingDy: Float = 0f
    private var pendingScrollY: Float = 0f

    private var receiversRegistered = false

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (btState == BluetoothAdapter.STATE_ON) {
                        start()
                    } else if (btState == BluetoothAdapter.STATE_OFF || btState == BluetoothAdapter.STATE_TURNING_OFF) {
                        connectedDevice = null
                        isRegistered = false
                        _state.value = HidConnectionState.BluetoothDisabled
                        _pairedHosts.value = emptyList()
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshPairedHosts()
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            val deviceProxy = proxy as? BluetoothHidDevice ?: return
            hidDevice = deviceProxy
            registerHidApp(deviceProxy)
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = null
            connectedDevice = null
            isRegistered = false
            isProxyRequested = false
            if (btAdapter?.isEnabled == true) {
                _state.value = HidConnectionState.Ready
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isRegistered = registered
            Log.d(TAG, "HID app status changed: registered=$registered plugged=${pluggedDevice?.address}")
            if (!registered) {
                connectedDevice = null
                if (btAdapter?.isEnabled == true && hasBluetoothPermissions()) {
                    _state.value = HidConnectionState.Error("HID profile registration failed")
                }
                return
            }

            refreshPairedHosts()

            val targetAddress = pendingConnectAddress
            if (targetAddress != null) {
                pendingConnectAddress = null
                connectByAddress(targetAddress)
                return
            }

            if (pluggedDevice != null) {
                val name = safeDeviceName(pluggedDevice)
                _state.value = HidConnectionState.Connecting(name, pluggedDevice.address)
                hidDevice?.connect(pluggedDevice)
            } else {
                _state.value = HidConnectionState.Ready
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            val name = safeDeviceName(device)
            Log.d(TAG, "HID connection state changed: $name (${device.address}) -> $state")
            when (state) {
                BluetoothProfile.STATE_CONNECTING -> {
                    _state.value = HidConnectionState.Connecting(name, device.address)
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    resetLocalInputState()
                    _state.value = HidConnectionState.Connected(name, device.address)
                    onHostConnected(device.address)
                    AirDeckHidService.startOrUpdate(appContext, name)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice?.address == device.address || connectedDevice == null) {
                        connectedDevice = null
                        resetLocalInputState()
                        _state.value = if (isRegistered) HidConnectionState.Ready else HidConnectionState.Registering
                        AirDeckHidService.stop(appContext)
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val proxy = hidDevice ?: return
            val report = when (id.toInt()) {
                HidDescriptor.REPORT_ID_KEYBOARD -> HidDescriptor.keyboardReport(0, emptyList())
                HidDescriptor.REPORT_ID_MOUSE -> HidDescriptor.mouseReport(0, 0, 0, 0)
                HidDescriptor.REPORT_ID_CONSUMER -> HidDescriptor.consumerReport(0)
                else -> ByteArray(1)
            }
            try {
                proxy.replyReport(device, type, id, report)
            } catch (t: Throwable) {
                Log.w(TAG, "replyReport failed", t)
            }
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            Log.d(TAG, "HID onSetProtocol: device=${device.address} protocol=$protocol")
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            Log.d(TAG, "HID onInterruptData: reportId=$reportId len=${data.size}")
        }
    }

    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
        )
    } else {
        emptyArray()
    }

    fun hasBluetoothPermissions(): Boolean {
        val required = requiredPermissions()
        return required.all { perm ->
            ContextCompat.checkSelfPermission(appContext, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        registerReceiversIfNeeded()

        val adapter = btAdapter
        if (adapter == null) {
            _state.value = HidConnectionState.Unsupported
            return
        }
        if (!hasBluetoothPermissions()) {
            _state.value = HidConnectionState.PermissionRequired
            return
        }
        if (!adapter.isEnabled) {
            _state.value = HidConnectionState.BluetoothDisabled
            _pairedHosts.value = emptyList()
            return
        }

        refreshPairedHosts()

        val existingProxy = hidDevice
        if (existingProxy != null) {
            if (!isRegistered) {
                registerHidApp(existingProxy)
            } else if (connectedDevice != null) {
                val dev = connectedDevice!!
                _state.value = HidConnectionState.Connected(safeDeviceName(dev), dev.address)
            } else {
                _state.value = HidConnectionState.Ready
            }
            return
        }

        if (!isProxyRequested) {
            _state.value = HidConnectionState.Registering
            isProxyRequested = true
            val ok = adapter.getProfileProxy(appContext, profileListener, BluetoothProfile.HID_DEVICE)
            if (!ok) {
                isProxyRequested = false
                _state.value = HidConnectionState.Unsupported
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp(proxy: BluetoothHidDevice) {
        if (!hasBluetoothPermissions()) {
            _state.value = HidConnectionState.PermissionRequired
            return
        }
        _state.value = HidConnectionState.Registering
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "AirDeck",
            "Wireless Keyboard & Touchpad",
            "AirDeck",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidDescriptor.DESCRIPTOR,
        )
        val outQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX,
        )
        try {
            val submitted = proxy.registerApp(sdp, null, outQos, executor, hidCallback)
            if (!submitted) {
                _state.value = HidConnectionState.Unsupported
            }
        } catch (t: Throwable) {
            Log.e(TAG, "registerApp failed", t)
            _state.value = HidConnectionState.Error(t.message ?: "registerApp failed")
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedHosts() {
        val adapter = btAdapter
        if (adapter == null || !hasBluetoothPermissions() || !adapter.isEnabled) {
            _pairedHosts.value = emptyList()
            return
        }
        val list = try {
            adapter.bondedDevices
                .orEmpty()
                .map { device ->
                    PairedHost(
                        name = safeDeviceName(device),
                        address = device.address,
                        majorDeviceClass = device.bluetoothClass?.majorDeviceClass ?: 0,
                    )
                }
                .sortedBy { it.name.lowercase() }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to query bonded devices", t)
            emptyList()
        }
        _pairedHosts.value = list
    }

    @SuppressLint("MissingPermission")
    fun connectByAddress(address: String) {
        if (!hasBluetoothPermissions()) {
            _state.value = HidConnectionState.PermissionRequired
            return
        }
        val adapter = btAdapter ?: return
        if (!adapter.isEnabled) {
            _state.value = HidConnectionState.BluetoothDisabled
            return
        }
        val proxy = hidDevice
        if (proxy == null || !isRegistered) {
            pendingConnectAddress = address
            start()
            return
        }
        val target = try {
            adapter.getRemoteDevice(address)
        } catch (t: Throwable) {
            Log.w(TAG, "Invalid Bluetooth address $address", t)
            return
        }
        val current = connectedDevice
        if (current != null && current.address != target.address) {
            try {
                proxy.disconnect(current)
            } catch (_: Throwable) {
            }
        }
        _state.value = HidConnectionState.Connecting(safeDeviceName(target), target.address)
        try {
            proxy.connect(target)
        } catch (t: Throwable) {
            Log.e(TAG, "connect failed", t)
            _state.value = HidConnectionState.Error(t.message ?: "Connect failed")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        releaseAll()
        val proxy = hidDevice ?: return
        val device = connectedDevice ?: return
        try {
            proxy.disconnect(device)
        } catch (t: Throwable) {
            Log.w(TAG, "disconnect failed", t)
        }
        connectedDevice = null
        AirDeckHidService.stop(appContext)
        if (isRegistered) {
            _state.value = HidConnectionState.Ready
        }
    }

    // ---- Pointer & Scroll Input ----

    fun movePointer(dx: Float, dy: Float) {
        val reports = mutableListOf<ByteArray>()
        synchronized(stateLock) {
            pendingDx += dx
            pendingDy += dy
            var stepX = truncate(pendingDx).toInt()
            var stepY = truncate(pendingDy).toInt()
            pendingDx -= stepX
            pendingDy -= stepY

            while (stepX != 0 || stepY != 0) {
                val chunkX = stepX.coerceIn(-127, 127)
                val chunkY = stepY.coerceIn(-127, 127)
                reports.add(HidDescriptor.mouseReport(mouseButtons, chunkX, chunkY))
                stepX -= chunkX
                stepY -= chunkY
            }
        }
        for (report in reports) {
            sendReport(HidDescriptor.REPORT_ID_MOUSE, report)
        }
    }

    fun scroll(verticalDelta: Float, @Suppress("UNUSED_PARAMETER") horizontalDelta: Float = 0f) {
        val reports = mutableListOf<ByteArray>()
        synchronized(stateLock) {
            pendingScrollY += verticalDelta
            var stepV = truncate(pendingScrollY).toInt()
            pendingScrollY -= stepV

            while (stepV != 0) {
                val chunkV = stepV.coerceIn(-127, 127)
                reports.add(HidDescriptor.mouseReport(mouseButtons, 0, 0, wheel = chunkV))
                stepV -= chunkV
            }
        }
        for (report in reports) {
            sendReport(HidDescriptor.REPORT_ID_MOUSE, report)
        }
    }

    fun setMouseButton(buttonMask: Int, pressed: Boolean) {
        val report = synchronized(stateLock) {
            mouseButtons = if (pressed) {
                mouseButtons or buttonMask
            } else {
                mouseButtons and buttonMask.inv()
            }
            HidDescriptor.mouseReport(mouseButtons, 0, 0)
        }
        sendReport(HidDescriptor.REPORT_ID_MOUSE, report)
    }

    fun clickMouse(buttonMask: Int) {
        scope.launch {
            setMouseButton(buttonMask, true)
            delay(24)
            setMouseButton(buttonMask, false)
        }
    }

    // ---- Keyboard Input ----

    fun setModifierMask(modifierMask: Int) {
        val report = synchronized(stateLock) {
            activeModifiers = modifierMask and 0xFF
            HidDescriptor.keyboardReport(activeModifiers, pressedKeys.toList())
        }
        sendReport(HidDescriptor.REPORT_ID_KEYBOARD, report)
    }

    fun keyDown(usageId: Int, extraModifierMask: Int = 0) {
        if (usageId == HidKeyMap.KEY_NONE) return
        val report = synchronized(stateLock) {
            if (pressedKeys.size < 6) {
                pressedKeys.add(usageId)
            }
            HidDescriptor.keyboardReport(activeModifiers or extraModifierMask, pressedKeys.toList())
        }
        sendReport(HidDescriptor.REPORT_ID_KEYBOARD, report)
    }

    fun keyUp(usageId: Int, extraModifierMask: Int = 0) {
        val report = synchronized(stateLock) {
            pressedKeys.remove(usageId)
            HidDescriptor.keyboardReport(activeModifiers or extraModifierMask, pressedKeys.toList())
        }
        sendReport(HidDescriptor.REPORT_ID_KEYBOARD, report)
    }

    fun tapKey(usageId: Int, modifierMask: Int = 0) {
        if (usageId == HidKeyMap.KEY_NONE) return
        scope.launch {
            sendReport(HidDescriptor.REPORT_ID_KEYBOARD, HidDescriptor.keyboardReport(modifierMask, listOf(usageId)))
            delay(20)
            val rest = synchronized(stateLock) {
                HidDescriptor.keyboardReport(activeModifiers, pressedKeys.toList())
            }
            sendReport(HidDescriptor.REPORT_ID_KEYBOARD, rest)
        }
    }

    fun sendText(text: String) {
        if (text.isEmpty()) return
        scope.launch {
            for (ch in text) {
                val stroke = HidKeyMap.fromAsciiChar(ch) ?: continue
                val mod = if (stroke.shift) HidDescriptor.MOD_LEFT_SHIFT else 0
                sendReport(HidDescriptor.REPORT_ID_KEYBOARD, HidDescriptor.keyboardReport(mod, listOf(stroke.usageId)))
                delay(16)
                sendReport(HidDescriptor.REPORT_ID_KEYBOARD, HidDescriptor.keyboardReport(0, emptyList()))
                delay(12)
            }
        }
    }

    // ---- TV / Media Consumer Keys ----

    fun tapConsumerKey(usageCode: Int) {
        scope.launch {
            sendReport(HidDescriptor.REPORT_ID_CONSUMER, HidDescriptor.consumerReport(usageCode))
            delay(32)
            sendReport(HidDescriptor.REPORT_ID_CONSUMER, HidDescriptor.consumerReport(0))
        }
    }

    fun releaseAll() {
        resetLocalInputState()
        sendReport(HidDescriptor.REPORT_ID_KEYBOARD, HidDescriptor.keyboardReport(0, emptyList()))
        sendReport(HidDescriptor.REPORT_ID_MOUSE, HidDescriptor.mouseReport(0, 0, 0, 0))
        sendReport(HidDescriptor.REPORT_ID_CONSUMER, HidDescriptor.consumerReport(0))
    }

    private fun resetLocalInputState() {
        synchronized(stateLock) {
            mouseButtons = 0
            activeModifiers = 0
            pressedKeys.clear()
            pendingDx = 0f
            pendingDy = 0f
            pendingScrollY = 0f
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendReport(reportId: Int, data: ByteArray): Boolean {
        val proxy = hidDevice ?: return false
        val device = connectedDevice ?: return false
        if (!hasBluetoothPermissions()) return false
        return try {
            proxy.sendReport(device, reportId, data)
        } catch (t: Throwable) {
            Log.w(TAG, "sendReport($reportId) failed", t)
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String =
        try {
            device.name?.takeIf { it.isNotBlank() } ?: device.address
        } catch (_: Throwable) {
            device.address
        }

    private fun registerReceiversIfNeeded() {
        if (receiversRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(appContext, btReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        receiversRegistered = true
    }

    companion object {
        private const val TAG = "AirDeckHid"
    }
}
