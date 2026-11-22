package dev.airdeck.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.airdeck.R
import dev.airdeck.hid.HidDescriptor
import kotlin.math.hypot

private const val TAP_TIMEOUT_MS = 230L
private const val DOUBLE_TAP_WINDOW_MS = 260L

@Composable
fun TouchpadSurface(
    connected: Boolean,
    blackoutMode: Boolean,
    pointerSpeed: Float,
    scrollSpeed: Float,
    naturalScroll: Boolean,
    hapticsEnabled: Boolean,
    showMouseButtons: Boolean,
    onMove: (dx: Float, dy: Float) -> Unit,
    onScroll: (vertical: Float, horizontal: Float) -> Unit,
    onButtonChange: (buttonMask: Int, pressed: Boolean) -> Unit,
    onClick: (buttonMask: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val density = LocalDensity.current
    val touchSlopPx = with(density) { 8.dp.toPx() }
    val scrollUnitPx = with(density) { 26.dp.toPx() }

    val speedState by rememberUpdatedState(pointerSpeed)
    val scrollSpeedState by rememberUpdatedState(scrollSpeed)
    val naturalScrollState by rememberUpdatedState(naturalScroll)
    val hapticsState by rememberUpdatedState(hapticsEnabled)
    val onMoveState by rememberUpdatedState(onMove)
    val onScrollState by rememberUpdatedState(onScroll)
    val onButtonChangeState by rememberUpdatedState(onButtonChange)
    val onClickState by rememberUpdatedState(onClick)

    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var dragLockActive by remember { mutableStateOf(false) }
    var hasInteracted by remember { mutableStateOf(false) }
    var lastSingleTapUpMs by remember { mutableLongStateOf(0L) }

    fun hapticClick() {
        if (hapticsState) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    val surfaceBg = if (blackoutMode) Color.Black else Color(0xFF0B1324)
    val borderColor = if (blackoutMode) Color(0xFF141B29) else colors.outline.copy(alpha = 0.55f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            val downTime = System.currentTimeMillis()
                            var maxPointers = 1
                            var movedBeyondSlop = false
                            var totalDistance = 0f
                            var lastCentroid = firstDown.position

                            // Double-tap-and-hold engages drag lock (Left Mouse Button held)
                            val engagedDragLock = (downTime - lastSingleTapUpMs) <= DOUBLE_TAP_WINDOW_MS
                            if (engagedDragLock) {
                                dragLockActive = true
                                onButtonChangeState(HidDescriptor.MOUSE_BUTTON_LEFT, true)
                                hapticClick()
                            }

                            activeTouchPoint = firstDown.position

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break

                                if (pressed.size > maxPointers) {
                                    maxPointers = pressed.size
                                }

                                val centroid = pressed
                                    .fold(Offset.Zero) { acc, c -> acc + c.position } / pressed.size.toFloat()

                                // Recompute delta only when pointer count didn't just jump
                                val prevCentroid = pressed
                                    .fold(Offset.Zero) { acc, c -> acc + c.previousPosition } / pressed.size.toFloat()
                                val delta = centroid - prevCentroid
                                val dist = hypot(delta.x, delta.y)

                                if (dist > 0f) {
                                    totalDistance += dist
                                    if (totalDistance > touchSlopPx) {
                                        movedBeyondSlop = true
                                        hasInteracted = true
                                    }
                                }

                                activeTouchPoint = centroid

                                if (movedBeyondSlop && dist > 0f) {
                                    when (pressed.size) {
                                        1 -> {
                                            // Gentle acceleration curve: precise at slow speed, swift across 4K TV
                                            val accel = 1f + (dist / 18f).coerceAtMost(1.8f)
                                            val factor = speedState * accel * 0.72f
                                            onMoveState(delta.x * factor, delta.y * factor)
                                        }
                                        2 -> {
                                            val dir = if (naturalScrollState) 1f else -1f
                                            val vStep = (delta.y / scrollUnitPx) * scrollSpeedState * 1.4f * dir
                                            val hStep = (delta.x / scrollUnitPx) * scrollSpeedState * 1.4f * (-dir)
                                            onScrollState(vStep, hStep)
                                        }
                                    }
                                }

                                pressed.forEach { it.consume() }
                                lastCentroid = centroid
                            }

                            activeTouchPoint = null
                            val elapsed = System.currentTimeMillis() - downTime

                            if (engagedDragLock) {
                                onButtonChangeState(HidDescriptor.MOUSE_BUTTON_LEFT, false)
                                dragLockActive = false
                                lastSingleTapUpMs = 0L
                            } else if (!movedBeyondSlop && elapsed <= TAP_TIMEOUT_MS) {
                                hasInteracted = true
                                when (maxPointers) {
                                    1 -> {
                                        onClickState(HidDescriptor.MOUSE_BUTTON_LEFT)
                                        hapticClick()
                                        lastSingleTapUpMs = System.currentTimeMillis()
                                    }
                                    2 -> {
                                        onClickState(HidDescriptor.MOUSE_BUTTON_RIGHT)
                                        hapticClick()
                                        lastSingleTapUpMs = 0L
                                    }
                                    else -> {
                                        onClickState(HidDescriptor.MOUSE_BUTTON_MIDDLE)
                                        hapticClick()
                                        lastSingleTapUpMs = 0L
                                    }
                                }
                            } else {
                                lastSingleTapUpMs = 0L
                            }
                        }
                    }
                },
        ) {
            if (!blackoutMode) {
                // Subtle dot grid + active touch glow
                val dotColor = colors.onSurfaceVariant.copy(alpha = 0.14f)
                val glowColor = if (dragLockActive) colors.tertiary else colors.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 28.dp.toPx()
                    var y = step
                    while (y < size.height) {
                        var x = step
                        while (x < size.width) {
                            drawCircle(color = dotColor, radius = 1.2.dp.toPx(), center = Offset(x, y))
                            x += step
                        }
                        y += step
                    }
                    activeTouchPoint?.let { pt ->
                        drawCircle(color = glowColor.copy(alpha = 0.14f), radius = 34.dp.toPx(), center = pt)
                        drawCircle(color = glowColor.copy(alpha = 0.55f), radius = 5.dp.toPx(), center = pt)
                    }
                }
            }

            if (blackoutMode) {
                Text(
                    text = stringResource(R.string.blackout_hint),
                    color = Color(0xFF334155),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                )
            } else if (!hasInteracted) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                ) {
                    if (!connected) {
                        Text(
                            text = stringResource(
                                R.string.touchpad_hint_connect,
                                stringResource(R.string.devices_button),
                            ),
                            color = colors.primary.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = stringResource(R.string.touchpad_hint_gestures),
                        color = colors.onSurfaceVariant.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (showMouseButtons && !blackoutMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MouseClickButton(
                    label = stringResource(R.string.touchpad_left_click),
                    hapticsEnabled = hapticsEnabled,
                    onPressedChange = { pressed ->
                        onButtonChange(HidDescriptor.MOUSE_BUTTON_LEFT, pressed)
                    },
                    modifier = Modifier.weight(1f),
                )
                MouseClickButton(
                    label = stringResource(R.string.touchpad_right_click),
                    hapticsEnabled = hapticsEnabled,
                    onPressedChange = { pressed ->
                        onButtonChange(HidDescriptor.MOUSE_BUTTON_RIGHT, pressed)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MouseClickButton(
    label: String,
    hapticsEnabled: Boolean,
    onPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val hapticsState by rememberUpdatedState(hapticsEnabled)
    val onChangeState by rememberUpdatedState(onPressedChange)
    var pressed by remember { mutableStateOf(false) }

    Surface(
        color = if (pressed) colors.primary.copy(alpha = 0.25f) else colors.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (pressed) colors.primary else colors.outline.copy(alpha = 0.5f),
        ),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    if (hapticsState) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    onChangeState(true)
                    waitForUpOrCancellation()
                    pressed = false
                    onChangeState(false)
                }
            },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (pressed) colors.primary else colors.onSurfaceVariant,
            )
        }
    }
}
