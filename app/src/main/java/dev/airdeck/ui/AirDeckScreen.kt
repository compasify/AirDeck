package dev.airdeck.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.airdeck.R
import dev.airdeck.data.AirDeckPrefs
import dev.airdeck.data.AirDeckSettings
import dev.airdeck.data.DeckLayoutPreference
import dev.airdeck.hid.BluetoothHidController
import dev.airdeck.hid.HidConnectionState
import kotlinx.coroutines.launch

private enum class ActiveDeckMode {
    Touchpad,
    Split,
    Keyboard,
    TvRemote,
    AirMouse,
    Numpad,
    Presenter,
}

@Composable
fun AirDeckScreen(
    hidController: BluetoothHidController,
    prefs: AirDeckPrefs,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onDimScreenChange: (Boolean) -> Unit,
) {
    val connectionState by hidController.state.collectAsStateWithLifecycle()
    val pairedHosts by hidController.pairedHosts.collectAsStateWithLifecycle()
    val settings by prefs.settings.collectAsStateWithLifecycle(initialValue = AirDeckSettings())
    val scope = rememberCoroutineScope()

    var manualOverride by remember { mutableStateOf<ActiveDeckMode?>(null) }
    var blackoutMode by remember { mutableStateOf(false) }
    var devicesSheetOpen by remember { mutableStateOf(false) }
    var settingsSheetOpen by remember { mutableStateOf(false) }
    var quickTextOpen by remember { mutableStateOf(false) }

    LaunchedEffect(settings.layoutPreference) {
        manualOverride = null
    }

    LaunchedEffect(settings.keepScreenOn) {
        onKeepScreenOnChange(settings.keepScreenOn)
    }

    LaunchedEffect(blackoutMode) {
        onDimScreenChange(blackoutMode)
    }

    val colors = MaterialTheme.colorScheme
    val isConnected = connectionState is HidConnectionState.Connected

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (blackoutMode) Color.Black else colors.background)
            .safeDrawingPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val isWide = containerWidth >= 600.dp
        val isTallEnoughForSplit = containerHeight >= 460.dp

        val autoMode = if (isWide && isTallEnoughForSplit) {
            ActiveDeckMode.Split
        } else {
            ActiveDeckMode.Touchpad
        }

        val activeMode = manualOverride ?: when (settings.layoutPreference) {
            DeckLayoutPreference.Auto -> autoMode
            DeckLayoutPreference.Touchpad -> ActiveDeckMode.Touchpad
            DeckLayoutPreference.Split -> ActiveDeckMode.Split
            DeckLayoutPreference.Keyboard -> ActiveDeckMode.Keyboard
            DeckLayoutPreference.TvRemote -> ActiveDeckMode.TvRemote
            DeckLayoutPreference.AirMouse -> ActiveDeckMode.AirMouse
            DeckLayoutPreference.Numpad -> ActiveDeckMode.Numpad
            DeckLayoutPreference.Presenter -> ActiveDeckMode.Presenter
        }

        LaunchedEffect(activeMode) {
            if (activeMode != ActiveDeckMode.Touchpad && blackoutMode) {
                blackoutMode = false
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (blackoutMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { blackoutMode = false })
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Surface(
                        onClick = { blackoutMode = false },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LightMode,
                                contentDescription = stringResource(R.string.wake_screen),
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.wake_screen),
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            } else {
                TopDeckBar(
                    connectionState = connectionState,
                    activeMode = activeMode,
                    touchpadOnTop = settings.touchpadOnTop,
                    showTvBar = settings.showTvBar,
                    compact = !isWide,
                    onOpenDevices = {
                        hidController.refreshPairedHosts()
                        devicesSheetOpen = true
                    },
                    onSelectMode = { chosen -> manualOverride = chosen },
                    onSwapSplit = {
                        scope.launch { prefs.setTouchpadOnTop(!settings.touchpadOnTop) }
                    },
                    onToggleTvBar = {
                        scope.launch { prefs.setShowTvBar(!settings.showTvBar) }
                    },
                    onToggleDim = {
                        manualOverride = ActiveDeckMode.Touchpad
                        blackoutMode = true
                    },
                    onOpenSettings = { settingsSheetOpen = true },
                )

                val showQuickTvStrip = settings.showTvBar && activeMode != ActiveDeckMode.TvRemote
                AnimatedVisibility(visible = showQuickTvStrip) {
                    TvControlBar(
                        hapticsEnabled = settings.hapticsEnabled,
                        onConsumerKey = { usage -> hidController.tapConsumerKey(usage) },
                        onOpenQuickText = { quickTextOpen = true },
                    )
                }
            }

            // Main adaptive deck area
            when (activeMode) {
                ActiveDeckMode.Touchpad -> {
                    TouchpadSurface(
                        connected = isConnected,
                        blackoutMode = blackoutMode,
                        pointerSpeed = settings.pointerSpeed,
                        scrollSpeed = settings.scrollSpeed,
                        naturalScroll = settings.naturalScroll,
                        hapticsEnabled = settings.hapticsEnabled,
                        showMouseButtons = settings.showMouseButtons,
                        onMove = { dx, dy -> hidController.movePointer(dx, dy) },
                        onScroll = { v, h -> hidController.scroll(v, h) },
                        onButtonChange = { mask, pressed -> hidController.setMouseButton(mask, pressed) },
                        onClick = { mask -> hidController.clickMouse(mask) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                ActiveDeckMode.Split -> {
                    val compactKeyboard = !isWide
                    val showFnRow = isWide && containerHeight >= 560.dp
                    val maxKbHeight = (containerHeight * 0.52f).coerceIn(210.dp, 340.dp)
                    val deckMaxWidth = if (isWide) 980.dp else containerWidth

                    val touchpadBlock: @Composable () -> Unit = {
                        val padMaxWidth = if (isWide && !settings.touchpadOnTop) 620.dp else deckMaxWidth
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            TouchpadSurface(
                                connected = isConnected,
                                blackoutMode = false,
                                pointerSpeed = settings.pointerSpeed,
                                scrollSpeed = settings.scrollSpeed,
                                naturalScroll = settings.naturalScroll,
                                hapticsEnabled = settings.hapticsEnabled,
                                showMouseButtons = settings.showMouseButtons,
                                onMove = { dx, dy -> hidController.movePointer(dx, dy) },
                                onScroll = { v, h -> hidController.scroll(v, h) },
                                onButtonChange = { mask, pressed -> hidController.setMouseButton(mask, pressed) },
                                onClick = { mask -> hidController.clickMouse(mask) },
                                modifier = Modifier
                                    .widthIn(max = padMaxWidth)
                                    .fillMaxSize(),
                            )
                        }
                    }

                    val keyboardBlock: @Composable () -> Unit = {
                        VirtualKeyboard(
                            compact = compactKeyboard,
                            showFunctionRow = showFnRow,
                            hapticsEnabled = settings.hapticsEnabled,
                            maxKeyboardHeight = maxKbHeight,
                            onModifierMaskChange = { mask -> hidController.setModifierMask(mask) },
                            onKeyDown = { usage, mod -> hidController.keyDown(usage, mod) },
                            onKeyUp = { usage, mod -> hidController.keyUp(usage, mod) },
                            onTapKey = { usage, mod -> hidController.tapKey(usage, mod) },
                        )
                    }

                    if (settings.touchpadOnTop) {
                        touchpadBlock()
                        keyboardBlock()
                    } else {
                        keyboardBlock()
                        touchpadBlock()
                    }
                }

                ActiveDeckMode.Keyboard -> {
                    val compactKeyboard = !isWide
                    val showFnRow = isWide || containerHeight >= 500.dp
                    val maxKbHeight = (containerHeight * 0.62f).coerceIn(220.dp, 360.dp)

                    Column(
                        modifier = Modifier
                            .widthIn(max = 980.dp)
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.weight(1f))
                        LiveImeBar(
                            onSendText = { text -> hidController.sendText(text) },
                            onTapKey = { usage, mod -> hidController.tapKey(usage, mod) },
                        )
                        ShortcutBar(
                            hapticsEnabled = settings.hapticsEnabled,
                            onTapShortcut = { usage, mod -> hidController.tapKey(usage, mod) },
                        )
                        VirtualKeyboard(
                            compact = compactKeyboard,
                            showFunctionRow = showFnRow,
                            hapticsEnabled = settings.hapticsEnabled,
                            maxKeyboardHeight = maxKbHeight,
                            onModifierMaskChange = { mask -> hidController.setModifierMask(mask) },
                            onKeyDown = { usage, mod -> hidController.keyDown(usage, mod) },
                            onKeyUp = { usage, mod -> hidController.keyUp(usage, mod) },
                            onTapKey = { usage, mod -> hidController.tapKey(usage, mod) },
                        )
                    }
                }

                ActiveDeckMode.TvRemote -> {
                    TvRemoteScreen(
                        hapticsEnabled = settings.hapticsEnabled,
                        onTapKey = { usage, mod -> hidController.tapKey(usage, mod) },
                        onConsumerKey = { usage -> hidController.tapConsumerKey(usage) },
                        onOpenQuickText = { quickTextOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                ActiveDeckMode.AirMouse -> {
                    AirMouseScreen(
                        sensitivity = settings.airMouseSensitivity,
                        hapticsEnabled = settings.hapticsEnabled,
                        onMove = { dx, dy -> hidController.movePointer(dx, dy) },
                        onScroll = { v -> hidController.scroll(v) },
                        onButtonChange = { mask, pressed -> hidController.setMouseButton(mask, pressed) },
                        onConsumerKey = { usage -> hidController.tapConsumerKey(usage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                ActiveDeckMode.Numpad -> {
                    NumpadScreen(
                        hapticsEnabled = settings.hapticsEnabled,
                        onTapKey = { usage, mod -> hidController.tapKey(usage, mod) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                ActiveDeckMode.Presenter -> {
                    PresenterScreen(
                        hapticsEnabled = settings.hapticsEnabled,
                        onTapKey = { usage, mod -> hidController.tapKey(usage, mod) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }

    if (devicesSheetOpen) {
        BluetoothDevicesSheet(
            connectionState = connectionState,
            pairedHosts = pairedHosts,
            onDismiss = { devicesSheetOpen = false },
            onRequestPermissions = onRequestPermissions,
            onEnableBluetooth = onEnableBluetooth,
            onMakeDiscoverable = onMakeDiscoverable,
            onOpenBluetoothSettings = onOpenBluetoothSettings,
            onRefresh = { hidController.refreshPairedHosts() },
            onConnect = { address -> hidController.connectByAddress(address) },
            onDisconnect = { hidController.disconnect() },
            onRetry = { hidController.start() },
        )
    }

    if (settingsSheetOpen) {
        SettingsSheet(
            settings = settings,
            onDismiss = { settingsSheetOpen = false },
            onPointerSpeedChange = { v -> scope.launch { prefs.setPointerSpeed(v) } },
            onScrollSpeedChange = { v -> scope.launch { prefs.setScrollSpeed(v) } },
            onAirMouseSensitivityChange = { v -> scope.launch { prefs.setAirMouseSensitivity(v) } },
            onNaturalScrollChange = { v -> scope.launch { prefs.setNaturalScroll(v) } },
            onTouchpadOnTopChange = { v -> scope.launch { prefs.setTouchpadOnTop(v) } },
            onHapticsChange = { v -> scope.launch { prefs.setHapticsEnabled(v) } },
            onKeepScreenOnChange = { v -> scope.launch { prefs.setKeepScreenOn(v) } },
            onShowMouseButtonsChange = { v -> scope.launch { prefs.setShowMouseButtons(v) } },
            onLayoutPreferenceChange = { pref -> scope.launch { prefs.setLayoutPreference(pref) } },
        )
    }

    if (quickTextOpen) {
        QuickTextDialog(
            onDismiss = { quickTextOpen = false },
            onSendText = { text -> hidController.sendText(text) },
        )
    }
}

@Composable
private fun TopDeckBar(
    connectionState: HidConnectionState,
    activeMode: ActiveDeckMode,
    touchpadOnTop: Boolean,
    showTvBar: Boolean,
    compact: Boolean,
    onOpenDevices: () -> Unit,
    onSelectMode: (ActiveDeckMode) -> Unit,
    onSwapSplit: () -> Unit,
    onToggleTvBar: () -> Unit,
    onToggleDim: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val statusText = when (connectionState) {
        is HidConnectionState.Connected -> stringResource(R.string.status_connected, connectionState.deviceName)
        is HidConnectionState.Connecting -> stringResource(R.string.status_connecting, connectionState.deviceName)
        HidConnectionState.Ready -> stringResource(R.string.devices_button)
        HidConnectionState.Registering -> stringResource(R.string.status_registering)
        HidConnectionState.BluetoothDisabled -> stringResource(R.string.status_bluetooth_off)
        HidConnectionState.PermissionRequired -> stringResource(R.string.status_permission_needed)
        HidConnectionState.Unsupported -> stringResource(R.string.status_unsupported)
        is HidConnectionState.Error -> stringResource(R.string.status_error)
    }
    val dotColor = when (connectionState) {
        is HidConnectionState.Connected -> colors.tertiary
        is HidConnectionState.Connecting -> colors.primary
        HidConnectionState.Ready -> colors.onSurfaceVariant
        HidConnectionState.BluetoothDisabled,
        HidConnectionState.PermissionRequired,
        HidConnectionState.Unsupported,
        is HidConnectionState.Error -> colors.error
        HidConnectionState.Registering -> colors.secondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Connection pill
        Surface(
            onClick = onOpenDevices,
            shape = RoundedCornerShape(12.dp),
            color = if (connectionState is HidConnectionState.Connected) {
                colors.primaryContainer.copy(alpha = 0.6f)
            } else {
                colors.surfaceVariant.copy(alpha = 0.7f)
            },
            border = BorderStroke(
                1.dp,
                if (connectionState is HidConnectionState.Connected) colors.primary.copy(alpha = 0.7f) else colors.outline.copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .widthIn(min = 96.dp, max = if (compact) 136.dp else 210.dp)
                .height(36.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
                Icon(
                    imageVector = if (connectionState is HidConnectionState.Connected) {
                        Icons.Default.BluetoothConnected
                    } else {
                        Icons.Default.Bluetooth
                    },
                    contentDescription = null,
                    tint = if (connectionState is HidConnectionState.Connected) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Scrollable mode switcher for all 7 modes
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ModeSwitchChip(
                    icon = Icons.Default.TouchApp,
                    label = if (compact) null else stringResource(R.string.layout_touchpad),
                    contentDescription = stringResource(R.string.layout_touchpad),
                    selected = activeMode == ActiveDeckMode.Touchpad,
                    onClick = { onSelectMode(ActiveDeckMode.Touchpad) },
                )
                ModeSwitchChip(
                    icon = Icons.Default.ViewAgenda,
                    label = if (compact) null else stringResource(R.string.layout_split),
                    contentDescription = stringResource(R.string.layout_split),
                    selected = activeMode == ActiveDeckMode.Split,
                    onClick = { onSelectMode(ActiveDeckMode.Split) },
                )
                ModeSwitchChip(
                    icon = Icons.Default.Keyboard,
                    label = if (compact) null else stringResource(R.string.layout_keyboard),
                    contentDescription = stringResource(R.string.layout_keyboard),
                    selected = activeMode == ActiveDeckMode.Keyboard,
                    onClick = { onSelectMode(ActiveDeckMode.Keyboard) },
                )
                ModeSwitchChip(
                    icon = Icons.Default.SettingsRemote,
                    label = if (compact) null else stringResource(R.string.layout_tv_remote),
                    contentDescription = stringResource(R.string.layout_tv_remote),
                    selected = activeMode == ActiveDeckMode.TvRemote,
                    onClick = { onSelectMode(ActiveDeckMode.TvRemote) },
                )
                ModeSwitchChip(
                    icon = Icons.Default.ScreenRotation,
                    label = if (compact) null else stringResource(R.string.layout_air_mouse),
                    contentDescription = stringResource(R.string.layout_air_mouse),
                    selected = activeMode == ActiveDeckMode.AirMouse,
                    onClick = { onSelectMode(ActiveDeckMode.AirMouse) },
                )
                ModeSwitchChip(
                    icon = Icons.Default.Dialpad,
                    label = if (compact) null else stringResource(R.string.layout_numpad),
                    contentDescription = stringResource(R.string.layout_numpad),
                    selected = activeMode == ActiveDeckMode.Numpad,
                    onClick = { onSelectMode(ActiveDeckMode.Numpad) },
                )
                ModeSwitchChip(
                    icon = Icons.Default.PresentToAll,
                    label = if (compact) null else stringResource(R.string.layout_presenter),
                    contentDescription = stringResource(R.string.layout_presenter),
                    selected = activeMode == ActiveDeckMode.Presenter,
                    onClick = { onSelectMode(ActiveDeckMode.Presenter) },
                )
            }
        }

        // 1-tap swap Touchpad above / below Keyboard when in Split mode
        if (activeMode == ActiveDeckMode.Split) {
            IconButton(
                onClick = onSwapSplit,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = stringResource(R.string.swap_split_position),
                    tint = if (touchpadOnTop) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        // TV bar toggle
        IconButton(
            onClick = onToggleTvBar,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = stringResource(R.string.tv_keys_toggle),
                tint = if (showTvBar) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        // OLED Dim button
        if (activeMode == ActiveDeckMode.Touchpad) {
            IconButton(
                onClick = onToggleDim,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = stringResource(R.string.dim_screen),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Settings button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ModeSwitchChip(
    icon: ImageVector,
    label: String?,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = if (selected) colors.primary.copy(alpha = 0.24f) else Color.Transparent,
        modifier = Modifier.height(32.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (label != null) 8.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            if (label != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
