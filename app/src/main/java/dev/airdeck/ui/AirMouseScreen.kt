package dev.airdeck.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.airdeck.R
import dev.airdeck.hid.HidDescriptor
import kotlin.math.abs

@Composable
fun AirMouseScreen(
    sensitivity: Float,
    hapticsEnabled: Boolean,
    onMove: (dx: Float, dy: Float) -> Unit,
    onScroll: (vertical: Float) -> Unit,
    onButtonChange: (buttonMask: Int, pressed: Boolean) -> Unit,
    onConsumerKey: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = MaterialTheme.colorScheme

    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val gyroSensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    var continuousMode by remember { mutableStateOf(false) }
    var holdAiming by remember { mutableStateOf(false) }
    val isAiming = continuousMode || holdAiming

    val aimingState by rememberUpdatedState(isAiming)
    val sensitivityState by rememberUpdatedState(sensitivity)
    val onMoveState by rememberUpdatedState(onMove)

    DisposableEffect(sensorManager, gyroSensor) {
        if (sensorManager == null || gyroSensor == null) {
            return@DisposableEffect onDispose {}
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!aimingState) return
                // Gyroscope angular velocity (rad/s): yaw around Z moves X, pitch around X moves Y
                val yaw = -event.values[2]
                val pitch = -event.values[0]
                val deadzone = 0.035f
                val filteredX = if (abs(yaw) < deadzone) 0f else yaw
                val filteredY = if (abs(pitch) < deadzone) 0f else pitch
                if (filteredX != 0f || filteredY != 0f) {
                    val scale = 14f * sensitivityState
                    onMoveState(filteredX * scale, filteredY * scale)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    fun haptic() {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (gyroSensor == null) {
                Surface(
                    color = colors.error.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.air_mouse_no_gyro),
                        color = colors.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                // Continuous mode toggle bar
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = null,
                                tint = if (isAiming) colors.primary else colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(R.string.air_mouse_lock_mode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Switch(
                            checked = continuousMode,
                            onCheckedChange = { continuousMode = it },
                        )
                    }
                }

                // Giant Hold-to-Aim pad
                Surface(
                    color = if (isAiming) colors.primaryContainer.copy(alpha = 0.55f) else colors.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = if (isAiming) 2.dp else 1.dp,
                        color = if (isAiming) colors.primary else colors.outline.copy(alpha = 0.55f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                holdAiming = true
                                haptic()
                                waitForUpOrCancellation()
                                holdAiming = false
                            }
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = null,
                            tint = if (isAiming) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (isAiming) {
                                stringResource(R.string.air_mouse_active_hint)
                            } else {
                                stringResource(R.string.air_mouse_hold_hint)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isAiming) colors.onPrimaryContainer else colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Scroll & Back row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        onClick = {
                            haptic()
                            onConsumerKey(HidDescriptor.CONSUMER_AC_BACK)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.tv_back), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        onClick = {
                            haptic()
                            onScroll(3f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.air_mouse_scroll_up), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        onClick = {
                            haptic()
                            onScroll(-3f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.air_mouse_scroll_down), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Big Left / Right click triggers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AirMouseTriggerButton(
                        label = stringResource(R.string.touchpad_left_click),
                        accent = true,
                        hapticsEnabled = hapticsEnabled,
                        onPressedChange = { onButtonChange(HidDescriptor.MOUSE_BUTTON_LEFT, it) },
                        modifier = Modifier.weight(1.3f),
                    )
                    AirMouseTriggerButton(
                        label = stringResource(R.string.touchpad_right_click),
                        accent = false,
                        hapticsEnabled = hapticsEnabled,
                        onPressedChange = { onButtonChange(HidDescriptor.MOUSE_BUTTON_RIGHT, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AirMouseTriggerButton(
    label: String,
    accent: Boolean,
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
        color = when {
            pressed -> colors.primary.copy(alpha = 0.35f)
            accent -> colors.primaryContainer.copy(alpha = 0.75f)
            else -> colors.surfaceVariant.copy(alpha = 0.65f)
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (pressed || accent) colors.primary else colors.outline.copy(alpha = 0.5f),
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
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (pressed || accent) colors.onPrimaryContainer else colors.onSurface,
            )
        }
    }
}
