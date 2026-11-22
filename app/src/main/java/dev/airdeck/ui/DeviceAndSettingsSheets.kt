package dev.airdeck.ui

import android.bluetooth.BluetoothClass
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.airdeck.R
import dev.airdeck.data.AirDeckSettings
import dev.airdeck.data.DeckLayoutPreference
import dev.airdeck.hid.HidConnectionState
import dev.airdeck.hid.PairedHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDevicesSheet(
    connectionState: HidConnectionState,
    pairedHosts: List<PairedHost>,
    onDismiss: () -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRefresh: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sheet_devices_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.sheet_devices_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.action_retry),
                    )
                }
            }

            when (connectionState) {
                HidConnectionState.PermissionRequired -> {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_grant_permission))
                    }
                }
                HidConnectionState.BluetoothDisabled -> {
                    Button(
                        onClick = onEnableBluetooth,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_enable_bluetooth))
                    }
                }
                HidConnectionState.Unsupported -> {
                    Surface(
                        color = colors.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.status_unsupported),
                            color = colors.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
                is HidConnectionState.Error -> {
                    Surface(
                        color = colors.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = connectionState.detail,
                                color = colors.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onRetry) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = onMakeDiscoverable,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_make_discoverable),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(onClick = onOpenBluetoothSettings) {
                            Icon(Icons.Default.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_open_bt_settings), maxLines = 1)
                        }
                    }
                }
            }

            Surface(
                color = colors.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.pairing_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }

            HorizontalDivider(color = colors.outlineVariant)

            Text(
                text = stringResource(R.string.paired_devices_header),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )

            if (pairedHosts.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_paired_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((pairedHosts.size.coerceAtMost(5) * 68).dp),
                ) {
                    items(pairedHosts, key = { it.address }) { host ->
                        val isConnected = connectionState is HidConnectionState.Connected &&
                            connectionState.address == host.address
                        val isConnecting = connectionState is HidConnectionState.Connecting &&
                            connectionState.address == host.address

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isConnected) colors.primaryContainer.copy(alpha = 0.55f) else colors.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isConnected) colors.primary else colors.outline.copy(alpha = 0.4f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = if (isConnected) {
                                        Icons.Default.BluetoothConnected
                                    } else {
                                        iconForDeviceClass(host.majorDeviceClass)
                                    },
                                    contentDescription = null,
                                    tint = if (isConnected) colors.primary else colors.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = host.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = when {
                                            isConnected -> stringResource(R.string.connected_badge)
                                            isConnecting -> stringResource(R.string.connecting_badge)
                                            else -> host.address
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isConnected) colors.tertiary else colors.onSurfaceVariant,
                                    )
                                }
                                if (isConnected) {
                                    OutlinedButton(onClick = onDisconnect) {
                                        Text(stringResource(R.string.action_disconnect))
                                    }
                                } else {
                                    Button(
                                        onClick = { onConnect(host.address) },
                                        enabled = !isConnecting,
                                    ) {
                                        Text(stringResource(R.string.action_connect))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconForDeviceClass(majorClass: Int): ImageVector = when (majorClass) {
    BluetoothClass.Device.Major.AUDIO_VIDEO -> Icons.Default.Tv
    BluetoothClass.Device.Major.COMPUTER -> Icons.Default.Computer
    BluetoothClass.Device.Major.PHONE -> Icons.Default.PhoneAndroid
    else -> Icons.Default.Devices
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AirDeckSettings,
    onDismiss: () -> Unit,
    onPointerSpeedChange: (Float) -> Unit,
    onScrollSpeedChange: (Float) -> Unit,
    onNaturalScrollChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onShowMouseButtonsChange: (Boolean) -> Unit,
    onLayoutPreferenceChange: (DeckLayoutPreference) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            // Default layout mode
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.setting_layout_mode),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
                val modes = listOf(
                    DeckLayoutPreference.Auto to stringResource(R.string.layout_auto),
                    DeckLayoutPreference.Touchpad to stringResource(R.string.layout_touchpad),
                    DeckLayoutPreference.Split to stringResource(R.string.layout_split),
                    DeckLayoutPreference.Keyboard to stringResource(R.string.layout_keyboard),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, (pref, label) ->
                        SegmentedButton(
                            selected = settings.layoutPreference == pref,
                            onClick = { onLayoutPreferenceChange(pref) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        ) {
                            Text(text = label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Pointer speed
            Column {
                Text(
                    text = stringResource(R.string.setting_pointer_speed, settings.pointerSpeed),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = settings.pointerSpeed,
                    onValueChange = onPointerSpeedChange,
                    valueRange = 0.4f..3.0f,
                )
            }

            // Scroll speed
            Column {
                Text(
                    text = stringResource(R.string.setting_scroll_speed, settings.scrollSpeed),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = settings.scrollSpeed,
                    onValueChange = onScrollSpeedChange,
                    valueRange = 0.4f..3.0f,
                )
            }

            HorizontalDivider(color = colors.outlineVariant)

            SettingSwitchRow(
                title = stringResource(R.string.setting_natural_scroll),
                subtitle = stringResource(R.string.setting_natural_scroll_desc),
                checked = settings.naturalScroll,
                onCheckedChange = onNaturalScrollChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.setting_mouse_buttons),
                subtitle = stringResource(R.string.setting_mouse_buttons_desc),
                checked = settings.showMouseButtons,
                onCheckedChange = onShowMouseButtonsChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.setting_haptics),
                subtitle = stringResource(R.string.setting_haptics_desc),
                checked = settings.hapticsEnabled,
                onCheckedChange = onHapticsChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.setting_keep_screen_on),
                subtitle = stringResource(R.string.setting_keep_screen_on_desc),
                checked = settings.keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun QuickTextDialog(
    onDismiss: () -> Unit,
    onSendText: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_text_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.quick_text_hint)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.quick_text_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        clipboard.getText()?.text?.let { pasted ->
                            text += pasted
                        }
                    },
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_paste))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotEmpty()) {
                        onSendText(text)
                    }
                    onDismiss()
                },
                enabled = text.isNotEmpty(),
            ) {
                Text(stringResource(R.string.action_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
