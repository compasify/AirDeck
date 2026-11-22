package dev.airdeck.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
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

private enum class ActiveDeckLayout {
    Touchpad,
    Split,
    Keyboard,
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

    var manualOverride by remember { mutableStateOf<ActiveDeckLayout?>(null) }
    var blackoutMode by remember { mutableStateOf(false) }
    var devicesSheetOpen by remember { mutableStateOf(false) }
    var settingsSheetOpen by remember { mutableStateOf(false) }
    var quickTextOpen by remember { mutableStateOf(false) }

    // Reset manual override when user explicitly changes default layout in Settings
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

        val autoLayout = if (isWide && isTallEnoughForSplit) {
            ActiveDeckLayout.Split
        } else {
            ActiveDeckLayout.Touchpad
        }

        val activeLayout = manualOverride ?: when (settings.layoutPreference) {
            DeckLayoutPreference.Auto -> autoLayout
            DeckLayoutPreference.Touchpad -> ActiveDeckLayout.Touchpad
            DeckLayoutPreference.Split -> ActiveDeckLayout.Split
            DeckLayoutPreference.Keyboard -> ActiveDeckLayout.Keyboard
        }

        // Exit blackout mode if user switches away from Touchpad
        LaunchedEffect(activeLayout) {
            if (activeLayout != ActiveDeckLayout.Touchpad && blackoutMode) {
                blackoutMode = false
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (blackoutMode) {
                // Minimal wake bar when screen is dimmed
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
                // Slim top control bar
                TopDeckBar(
                    connectionState = connectionState,
                    activeLayout = activeLayout,
                    showTvBar = settings.showTvBar,
                    compact = !isWide,
                    onOpenDevices = {
                        hidController.refreshPairedHosts()
                        devicesSheetOpen = true
                    },
                    onSelectLayout = { chosen ->
                        manualOverride = chosen
                    },
                    onToggleTvBar = {
                        scope.launch { prefs.setShowTvBar(!settings.showTvBar) }
                    },
                    onToggleDim = {
                        manualOverride = ActiveDeckLayout.Touchpad
                        blackoutMode = true
                    },
                    onOpenSettings = { settingsSheetOpen = true },
                )

                AnimatedVisibility(visible = settings.showTvBar) {
                    TvControlBar(
                        hapticsEnabled = settings.hapticsEnabled,
                        onConsumerKey = { usage -> hidController.tapConsumerKey(usage) },
                        onOpenQuickText = { quickTextOpen = true },
                    )
                }
            }

            // Main adaptive deck area
            when (activeLayout) {
                ActiveDeckLayout.Touchpad -> {
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

                ActiveDeckLayout.Split -> {
                    val touchpadWeight = if (isWide) 0.46f else 0.44f
                    val keyboardWeight = 1f - touchpadWeight
                    val compactKeyboard = !isWide
                    val showFnRow = isWide && containerHeight >= 600.dp

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
                            .fillMaxWidth()
                            .weight(touchpadWeight),
                    )
                    VirtualKeyboard(
                        compact = compactKeyboard,
                        showFunctionRow = showFnRow,
                        hapticsEnabled = settings.hapticsEnabled,
                        onModifierMaskChange = { mask -> hidController.setModifierMask(mask) },
                        onKeyDown = { usage, mod -> hidController.keyDown(usage, mod) },
                        onKeyUp = { usage, mod -> hidController.keyUp(usage, mod) },
                        onTapKey = { usage, mod -> hidController.tapKey(usage, mod) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(keyboardWeight),
                    )
                }

                ActiveDeckLayout.Keyboard -> {
                    val compactKeyboard = !isWide
                    val showFnRow = isWide || containerHeight >= 520.dp
                    VirtualKeyboard(
                        compact = compactKeyboard,
                        showFunctionRow = showFnRow,
                        hapticsEnabled = settings.hapticsEnabled,
                        onModifierMaskChange = { mask -> hidController.setModifierMask(mask) },
                        onKeyDown = { usage, mod -> hidController.keyDown(usage, mod) },
                        onKeyUp = { usage, mod -> hidController.keyUp(usage, mod) },
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
            onNaturalScrollChange = { v -> scope.launch { prefs.setNaturalScroll(v) } },
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
    activeLayout: ActiveDeckLayout,
    showTvBar: Boolean,
    compact: Boolean,
    onOpenDevices: () -> Unit,
    onSelectLayout: (ActiveDeckLayout) -> Unit,
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
                .weight(1f)
                .height(36.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
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
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 3-mode layout switcher: Touchpad | Split | Keyboard
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.45f)),
            modifier = Modifier.height(36.dp),
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                LayoutSwitchChip(
                    icon = Icons.Default.TouchApp,
                    label = if (compact) null else stringResource(R.string.layout_touchpad),
                    contentDescription = stringResource(R.string.layout_touchpad),
                    selected = activeLayout == ActiveDeckLayout.Touchpad,
                    onClick = { onSelectLayout(ActiveDeckLayout.Touchpad) },
                )
                LayoutSwitchChip(
                    icon = Icons.Default.ViewAgenda,
                    label = if (compact) null else stringResource(R.string.layout_split),
                    contentDescription = stringResource(R.string.layout_split),
                    selected = activeLayout == ActiveDeckLayout.Split,
                    onClick = { onSelectLayout(ActiveDeckLayout.Split) },
                )
                LayoutSwitchChip(
                    icon = Icons.Default.Keyboard,
                    label = if (compact) null else stringResource(R.string.layout_keyboard),
                    contentDescription = stringResource(R.string.layout_keyboard),
                    selected = activeLayout == ActiveDeckLayout.Keyboard,
                    onClick = { onSelectLayout(ActiveDeckLayout.Keyboard) },
                )
            }
        }

        // TV bar toggle
        IconButton(
            onClick = onToggleTvBar,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = stringResource(R.string.tv_keys_toggle),
                tint = if (showTvBar) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }

        // Dim / OLED blackout button
        IconButton(
            onClick = onToggleDim,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = stringResource(R.string.dim_screen),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }

        // Settings button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun LayoutSwitchChip(
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
                modifier = Modifier.size(17.dp),
            )
            if (label != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}
