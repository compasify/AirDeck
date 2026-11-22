package dev.airdeck.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.airdeck.R
import dev.airdeck.hid.HidDescriptor
import dev.airdeck.hid.HidKeyMap
import java.util.Locale
import kotlinx.coroutines.delay

private data class NumpadKey(
    val label: String,
    val usageId: Int,
    val modifierMask: Int = 0,
    val weight: Float = 1f,
    val accent: Boolean = false,
)

private val NUMPAD_ROWS = listOf(
    listOf(
        NumpadKey("Esc", HidKeyMap.KEY_ESC, accent = true),
        NumpadKey("Tab", HidKeyMap.KEY_TAB, accent = true),
        NumpadKey("/", HidKeyMap.KEY_SLASH, accent = true),
        NumpadKey("*", HidKeyMap.KEY_8, HidDescriptor.MOD_LEFT_SHIFT, accent = true),
    ),
    listOf(
        NumpadKey("7", HidKeyMap.KEY_7),
        NumpadKey("8", HidKeyMap.KEY_8),
        NumpadKey("9", HidKeyMap.KEY_9),
        NumpadKey("-", HidKeyMap.KEY_MINUS, accent = true),
    ),
    listOf(
        NumpadKey("4", HidKeyMap.KEY_4),
        NumpadKey("5", HidKeyMap.KEY_5),
        NumpadKey("6", HidKeyMap.KEY_6),
        NumpadKey("+", HidKeyMap.KEY_EQUALS, HidDescriptor.MOD_LEFT_SHIFT, accent = true),
    ),
    listOf(
        NumpadKey("1", HidKeyMap.KEY_1),
        NumpadKey("2", HidKeyMap.KEY_2),
        NumpadKey("3", HidKeyMap.KEY_3),
        NumpadKey("⌫", HidKeyMap.KEY_BACKSPACE, accent = true),
    ),
    listOf(
        NumpadKey("0", HidKeyMap.KEY_0, weight = 2f),
        NumpadKey(".", HidKeyMap.KEY_PERIOD),
        NumpadKey("↵", HidKeyMap.KEY_ENTER, accent = true),
    ),
)

@Composable
fun NumpadScreen(
    hapticsEnabled: Boolean,
    onTapKey: (usageId: Int, modifierMask: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in NUMPAD_ROWS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (key in row) {
                        Surface(
                            onClick = {
                                if (hapticsEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                onTapKey(key.usageId, key.modifierMask)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (key.accent) colors.surfaceVariant else Color(0xFF162033),
                            border = BorderStroke(
                                1.dp,
                                if (key.usageId == HidKeyMap.KEY_ENTER) colors.primary else colors.outline.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier
                                .weight(key.weight)
                                .fillMaxSize(),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key.label,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (key.usageId == HidKeyMap.KEY_ENTER) colors.primary else colors.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresenterScreen(
    hapticsEnabled: Boolean,
    onTapKey: (usageId: Int, modifierMask: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            elapsedSeconds++
        }
    }

    fun fire(usage: Int, mod: Int = 0) {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        onTapKey(usage, mod)
    }

    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val timerLabel = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Timer & slideshow controls bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = timerLabel,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { running = !running }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (running) colors.tertiary else colors.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { elapsedSeconds = 0 }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.presenter_reset_timer),
                                tint = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Start / Blackout / Exit row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresenterActionTile(
                    icon = Icons.Default.PlayArrow,
                    label = stringResource(R.string.presenter_start),
                    onClick = { fire(HidKeyMap.KEY_F5) },
                    modifier = Modifier.weight(1f),
                )
                PresenterActionTile(
                    icon = Icons.Default.DarkMode,
                    label = stringResource(R.string.presenter_blank),
                    onClick = { fire(HidKeyMap.KEY_B) },
                    modifier = Modifier.weight(1f),
                )
                PresenterActionTile(
                    icon = Icons.Default.Close,
                    label = stringResource(R.string.presenter_end),
                    onClick = { fire(HidKeyMap.KEY_ESC) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Giant NEXT SLIDE button
            Surface(
                onClick = { fire(HidKeyMap.KEY_RIGHT) },
                shape = RoundedCornerShape(22.dp),
                color = colors.primaryContainer,
                border = BorderStroke(1.5.dp, colors.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.4f),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.presenter_next),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            // PREVIOUS SLIDE button
            Surface(
                onClick = { fire(HidKeyMap.KEY_LEFT) },
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.presenter_prev),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun PresenterActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)),
        modifier = modifier.height(52.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}
