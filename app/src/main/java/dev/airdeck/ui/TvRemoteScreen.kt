package dev.airdeck.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.airdeck.R
import dev.airdeck.hid.HidDescriptor
import dev.airdeck.hid.HidKeyMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TvRemoteScreen(
    hapticsEnabled: Boolean,
    onTapKey: (usageId: Int, modifierMask: Int) -> Unit,
    onConsumerKey: (Int) -> Unit,
    onOpenQuickText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    fun haptic() {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val isWide = maxWidth >= 600.dp
        val dpadSize = if (isWide) {
            minOf(maxHeight * 0.72f, 280.dp).coerceAtLeast(200.dp)
        } else {
            minOf(maxWidth * 0.72f, maxHeight * 0.44f, 260.dp).coerceAtLeast(190.dp)
        }

        if (isWide) {
            Row(
                modifier = Modifier
                    .widthIn(max = 780.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularDPad(
                    size = dpadSize,
                    onDirection = { usage ->
                        haptic()
                        onTapKey(usage, 0)
                    },
                    onOk = {
                        haptic()
                        onTapKey(HidKeyMap.KEY_ENTER, 0)
                    },
                )
                RemoteControlsCluster(
                    onNavKey = { usage ->
                        haptic()
                        onConsumerKey(usage)
                    },
                    onOpenQuickText = onOpenQuickText,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularDPad(
                    size = dpadSize,
                    onDirection = { usage ->
                        haptic()
                        onTapKey(usage, 0)
                    },
                    onOk = {
                        haptic()
                        onTapKey(HidKeyMap.KEY_ENTER, 0)
                    },
                )
                RemoteControlsCluster(
                    onNavKey = { usage ->
                        haptic()
                        onConsumerKey(usage)
                    },
                    onOpenQuickText = onOpenQuickText,
                    modifier = Modifier.widthIn(max = 380.dp),
                )
            }
        }
    }
}

@Composable
private fun CircularDPad(
    size: Dp,
    onDirection: (Int) -> Unit,
    onOk: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val okSize = size * 0.36f

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.5.dp, colors.outline.copy(alpha = 0.65f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        DPadDirectionButton(
            icon = Icons.Default.KeyboardArrowUp,
            onTrigger = { onDirection(HidKeyMap.KEY_UP) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(size * 0.28f),
        )
        DPadDirectionButton(
            icon = Icons.Default.KeyboardArrowDown,
            onTrigger = { onDirection(HidKeyMap.KEY_DOWN) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .size(size * 0.28f),
        )
        DPadDirectionButton(
            icon = Icons.Default.KeyboardArrowLeft,
            onTrigger = { onDirection(HidKeyMap.KEY_LEFT) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .size(size * 0.28f),
        )
        DPadDirectionButton(
            icon = Icons.Default.KeyboardArrowRight,
            onTrigger = { onDirection(HidKeyMap.KEY_RIGHT) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(size * 0.28f),
        )

        Surface(
            onClick = onOk,
            shape = CircleShape,
            color = colors.primaryContainer,
            border = BorderStroke(1.5.dp, colors.primary),
            modifier = Modifier.size(okSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.tv_ok),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun DPadDirectionButton(
    icon: ImageVector,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val triggerState by rememberUpdatedState(onTrigger)

    Surface(
        color = if (pressed) colors.primary.copy(alpha = 0.28f) else colors.surfaceVariant.copy(alpha = 0.45f),
        shape = CircleShape,
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                triggerState()
                val job = scope.launch {
                    delay(380)
                    while (true) {
                        triggerState()
                        delay(110)
                    }
                }
                waitForUpOrCancellation()
                job.cancel()
                pressed = false
            }
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (pressed) colors.primary else colors.onSurface,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun RemoteControlsCluster(
    onNavKey: (Int) -> Unit,
    onOpenQuickText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Navigation row: Back, Home, Menu, Search
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RemoteTileButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = stringResource(R.string.tv_back),
                onClick = { onNavKey(HidDescriptor.CONSUMER_AC_BACK) },
                modifier = Modifier.weight(1f),
            )
            RemoteTileButton(
                icon = Icons.Default.Home,
                label = stringResource(R.string.tv_home),
                onClick = { onNavKey(HidDescriptor.CONSUMER_AC_HOME) },
                modifier = Modifier.weight(1f),
            )
            RemoteTileButton(
                icon = Icons.Default.Menu,
                label = stringResource(R.string.tv_menu),
                onClick = { onNavKey(HidDescriptor.CONSUMER_MENU) },
                modifier = Modifier.weight(1f),
            )
            RemoteTileButton(
                icon = Icons.Default.Search,
                label = stringResource(R.string.tv_search),
                onClick = { onNavKey(HidDescriptor.CONSUMER_AC_SEARCH) },
                modifier = Modifier.weight(1f),
            )
        }

        // Volume row: Vol-, Mute, Vol+
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RemoteTileButton(
                icon = Icons.AutoMirrored.Filled.VolumeDown,
                label = "-",
                onClick = { onNavKey(HidDescriptor.CONSUMER_VOLUME_DOWN) },
                modifier = Modifier.weight(1f),
            )
            RemoteTileButton(
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                label = stringResource(R.string.tv_mute),
                onClick = { onNavKey(HidDescriptor.CONSUMER_MUTE) },
                modifier = Modifier.weight(1f),
            )
            RemoteTileButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "+",
                onClick = { onNavKey(HidDescriptor.CONSUMER_VOLUME_UP) },
                modifier = Modifier.weight(1f),
            )
        }

        // Media row: Prev, Play/Pause, Next, Quick Text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RemoteTileButton(
                icon = Icons.Default.SkipPrevious,
                label = stringResource(R.string.tv_prev),
                onClick = { onNavKey(HidDescriptor.CONSUMER_SCAN_PREV) },
                modifier = Modifier.weight(1f),
            )
            RemoteTileButton(
                icon = Icons.Default.PlayArrow,
                label = stringResource(R.string.tv_play_pause),
                accent = true,
                onClick = { onNavKey(HidDescriptor.CONSUMER_PLAY_PAUSE) },
                modifier = Modifier.weight(1.3f),
            )
            RemoteTileButton(
                icon = Icons.Default.SkipNext,
                label = stringResource(R.string.tv_next),
                onClick = { onNavKey(HidDescriptor.CONSUMER_SCAN_NEXT) },
                modifier = Modifier.weight(1f),
            )
            RemoteTileButton(
                icon = Icons.Default.ContentPaste,
                label = stringResource(R.string.send_text_button),
                onClick = onOpenQuickText,
                modifier = Modifier.weight(1.1f),
            )
        }
    }
}

@Composable
private fun RemoteTileButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (accent) colors.primaryContainer else colors.surface,
        border = BorderStroke(
            1.dp,
            if (accent) colors.primary else colors.outline.copy(alpha = 0.55f),
        ),
        modifier = modifier.height(58.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (accent) colors.primary else colors.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (accent) colors.onPrimaryContainer else colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
