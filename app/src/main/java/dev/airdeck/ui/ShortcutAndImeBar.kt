package dev.airdeck.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.airdeck.R
import dev.airdeck.hid.HidDescriptor
import dev.airdeck.hid.HidKeyMap

@Composable
fun ShortcutBar(
    hapticsEnabled: Boolean,
    onTapShortcut: (usageId: Int, modifierMask: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    fun fire(usage: Int, mod: Int) {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        onTapShortcut(usage, mod)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShortcutChip(stringResource(R.string.shortcut_copy)) {
            fire(HidKeyMap.KEY_C, HidDescriptor.MOD_LEFT_CTRL)
        }
        ShortcutChip(stringResource(R.string.shortcut_paste)) {
            fire(HidKeyMap.KEY_V, HidDescriptor.MOD_LEFT_CTRL)
        }
        ShortcutChip(stringResource(R.string.shortcut_cut)) {
            fire(HidKeyMap.KEY_X, HidDescriptor.MOD_LEFT_CTRL)
        }
        ShortcutChip(stringResource(R.string.shortcut_undo)) {
            fire(HidKeyMap.KEY_Z, HidDescriptor.MOD_LEFT_CTRL)
        }
        ShortcutChip(stringResource(R.string.shortcut_select_all)) {
            fire(HidKeyMap.KEY_A, HidDescriptor.MOD_LEFT_CTRL)
        }
        ShortcutChip(stringResource(R.string.shortcut_alt_tab)) {
            fire(HidKeyMap.KEY_TAB, HidDescriptor.MOD_LEFT_ALT)
        }
        ShortcutChip(stringResource(R.string.shortcut_refresh)) {
            fire(HidKeyMap.KEY_F5, 0)
        }
    }
}

@Composable
private fun ShortcutChip(
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = colors.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.45f)),
        modifier = Modifier.height(32.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
                maxLines = 1,
            )
        }
    }
}

/**
 * Live system-IME (Gboard / SwiftKey / Voice-to-Text) bridge:
 * streams typed ASCII characters, Backspace, and Enter directly to the Bluetooth HID host.
 */
@Composable
fun LiveImeBar(
    onSendText: (String) -> Unit,
    onTapKey: (usageId: Int, modifierMask: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var buffer by remember { mutableStateOf("") }

    OutlinedTextField(
        value = buffer,
        onValueChange = { next ->
            // Compute common prefix to stream only the diff (supports swipe typing, voice input, backspace)
            var prefix = 0
            val minLen = minOf(buffer.length, next.length)
            while (prefix < minLen && buffer[prefix] == next[prefix]) {
                prefix++
            }
            val deletedCount = buffer.length - prefix
            val addedText = next.substring(prefix)

            repeat(deletedCount) {
                onTapKey(HidKeyMap.KEY_BACKSPACE, 0)
            }
            if (addedText.isNotEmpty()) {
                onSendText(addedText)
            }
            // Keep buffer short so it never grows endlessly
            buffer = if (next.length > 64) next.takeLast(24) else next
        },
        placeholder = {
            Text(
                text = stringResource(R.string.live_ime_placeholder),
                fontSize = 13.sp,
                color = colors.onSurfaceVariant.copy(alpha = 0.75f),
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (buffer.isNotEmpty()) {
                    IconButton(onClick = { buffer = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                IconButton(
                    onClick = {
                        onTapKey(HidKeyMap.KEY_ENTER, 0)
                        buffer = ""
                    },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.action_send),
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface.copy(alpha = 0.85f),
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline.copy(alpha = 0.55f),
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(
            onSend = {
                onTapKey(HidKeyMap.KEY_ENTER, 0)
                buffer = ""
            },
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
