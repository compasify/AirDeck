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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.airdeck.hid.HidDescriptor
import dev.airdeck.hid.HidKeyMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ModKind(val bit: Int) {
    Shift(HidDescriptor.MOD_LEFT_SHIFT),
    Ctrl(HidDescriptor.MOD_LEFT_CTRL),
    Alt(HidDescriptor.MOD_LEFT_ALT),
    Meta(HidDescriptor.MOD_LEFT_META),
    Caps(0),
}

private data class DeckKeySpec(
    val label: String,
    val shiftLabel: String? = null,
    val usageId: Int,
    val weight: Float = 1f,
    val repeatable: Boolean = false,
    val isLetter: Boolean = false,
    val modKind: ModKind? = null,
)

private val ROW_FN = listOf(
    DeckKeySpec("Esc", usageId = HidKeyMap.KEY_ESC, weight = 1.25f),
    DeckKeySpec("F1", usageId = HidKeyMap.KEY_F1),
    DeckKeySpec("F2", usageId = HidKeyMap.KEY_F2),
    DeckKeySpec("F3", usageId = HidKeyMap.KEY_F3),
    DeckKeySpec("F4", usageId = HidKeyMap.KEY_F4),
    DeckKeySpec("F5", usageId = HidKeyMap.KEY_F5),
    DeckKeySpec("F6", usageId = HidKeyMap.KEY_F6),
    DeckKeySpec("F7", usageId = HidKeyMap.KEY_F7),
    DeckKeySpec("F8", usageId = HidKeyMap.KEY_F8),
    DeckKeySpec("F9", usageId = HidKeyMap.KEY_F9),
    DeckKeySpec("F10", usageId = HidKeyMap.KEY_F10),
    DeckKeySpec("F11", usageId = HidKeyMap.KEY_F11),
    DeckKeySpec("F12", usageId = HidKeyMap.KEY_F12),
    DeckKeySpec("Del", usageId = HidKeyMap.KEY_DELETE, weight = 1.3f, repeatable = true),
)

private val ROW_NUM = listOf(
    DeckKeySpec("`", "~", HidKeyMap.KEY_GRAVE),
    DeckKeySpec("1", "!", HidKeyMap.KEY_1),
    DeckKeySpec("2", "@", HidKeyMap.KEY_2),
    DeckKeySpec("3", "#", HidKeyMap.KEY_3),
    DeckKeySpec("4", "$", HidKeyMap.KEY_4),
    DeckKeySpec("5", "%", HidKeyMap.KEY_5),
    DeckKeySpec("6", "^", HidKeyMap.KEY_6),
    DeckKeySpec("7", "&", HidKeyMap.KEY_7),
    DeckKeySpec("8", "*", HidKeyMap.KEY_8),
    DeckKeySpec("9", "(", HidKeyMap.KEY_9),
    DeckKeySpec("0", ")", HidKeyMap.KEY_0),
    DeckKeySpec("-", "_", HidKeyMap.KEY_MINUS),
    DeckKeySpec("=", "+", HidKeyMap.KEY_EQUALS),
    DeckKeySpec("⌫", usageId = HidKeyMap.KEY_BACKSPACE, weight = 1.75f, repeatable = true),
)

private val ROW_TOP = listOf(
    DeckKeySpec("Tab", usageId = HidKeyMap.KEY_TAB, weight = 1.45f),
    DeckKeySpec("q", "Q", HidKeyMap.KEY_Q, isLetter = true),
    DeckKeySpec("w", "W", HidKeyMap.KEY_W, isLetter = true),
    DeckKeySpec("e", "E", HidKeyMap.KEY_E, isLetter = true),
    DeckKeySpec("r", "R", HidKeyMap.KEY_R, isLetter = true),
    DeckKeySpec("t", "T", HidKeyMap.KEY_T, isLetter = true),
    DeckKeySpec("y", "Y", HidKeyMap.KEY_Y, isLetter = true),
    DeckKeySpec("u", "U", HidKeyMap.KEY_U, isLetter = true),
    DeckKeySpec("i", "I", HidKeyMap.KEY_I, isLetter = true),
    DeckKeySpec("o", "O", HidKeyMap.KEY_O, isLetter = true),
    DeckKeySpec("p", "P", HidKeyMap.KEY_P, isLetter = true),
    DeckKeySpec("[", "{", HidKeyMap.KEY_LEFT_BRACKET),
    DeckKeySpec("]", "}", HidKeyMap.KEY_RIGHT_BRACKET),
    DeckKeySpec("\\", "|", HidKeyMap.KEY_BACKSLASH, weight = 1.25f),
)

private val ROW_HOME = listOf(
    DeckKeySpec("Caps", usageId = HidKeyMap.KEY_CAPS_LOCK, weight = 1.65f, modKind = ModKind.Caps),
    DeckKeySpec("a", "A", HidKeyMap.KEY_A, isLetter = true),
    DeckKeySpec("s", "S", HidKeyMap.KEY_S, isLetter = true),
    DeckKeySpec("d", "D", HidKeyMap.KEY_D, isLetter = true),
    DeckKeySpec("f", "F", HidKeyMap.KEY_F, isLetter = true),
    DeckKeySpec("g", "G", HidKeyMap.KEY_G, isLetter = true),
    DeckKeySpec("h", "H", HidKeyMap.KEY_H, isLetter = true),
    DeckKeySpec("j", "J", HidKeyMap.KEY_J, isLetter = true),
    DeckKeySpec("k", "K", HidKeyMap.KEY_K, isLetter = true),
    DeckKeySpec("l", "L", HidKeyMap.KEY_L, isLetter = true),
    DeckKeySpec(";", ":", HidKeyMap.KEY_SEMICOLON),
    DeckKeySpec("'", "\"", HidKeyMap.KEY_APOSTROPHE),
    DeckKeySpec("↵", usageId = HidKeyMap.KEY_ENTER, weight = 2.0f),
)

private val ROW_BOTTOM = listOf(
    DeckKeySpec("⇧", usageId = HidKeyMap.KEY_NONE, weight = 2.1f, modKind = ModKind.Shift),
    DeckKeySpec("z", "Z", HidKeyMap.KEY_Z, isLetter = true),
    DeckKeySpec("x", "X", HidKeyMap.KEY_X, isLetter = true),
    DeckKeySpec("c", "C", HidKeyMap.KEY_C, isLetter = true),
    DeckKeySpec("v", "V", HidKeyMap.KEY_V, isLetter = true),
    DeckKeySpec("b", "B", HidKeyMap.KEY_B, isLetter = true),
    DeckKeySpec("n", "N", HidKeyMap.KEY_N, isLetter = true),
    DeckKeySpec("m", "M", HidKeyMap.KEY_M, isLetter = true),
    DeckKeySpec(",", "<", HidKeyMap.KEY_COMMA),
    DeckKeySpec(".", ">", HidKeyMap.KEY_PERIOD),
    DeckKeySpec("/", "?", HidKeyMap.KEY_SLASH),
    DeckKeySpec("↑", usageId = HidKeyMap.KEY_UP, weight = 1.1f, repeatable = true),
    DeckKeySpec("⇧", usageId = HidKeyMap.KEY_NONE, weight = 1.45f, modKind = ModKind.Shift),
)

private val ROW_SPACE = listOf(
    DeckKeySpec("Esc", usageId = HidKeyMap.KEY_ESC, weight = 1.15f),
    DeckKeySpec("Ctrl", usageId = HidKeyMap.KEY_NONE, weight = 1.25f, modKind = ModKind.Ctrl),
    DeckKeySpec("⌘", usageId = HidKeyMap.KEY_NONE, weight = 1.1f, modKind = ModKind.Meta),
    DeckKeySpec("Alt", usageId = HidKeyMap.KEY_NONE, weight = 1.15f, modKind = ModKind.Alt),
    DeckKeySpec("Space", usageId = HidKeyMap.KEY_SPACE, weight = 4.6f),
    DeckKeySpec("←", usageId = HidKeyMap.KEY_LEFT, weight = 1.1f, repeatable = true),
    DeckKeySpec("↓", usageId = HidKeyMap.KEY_DOWN, weight = 1.1f, repeatable = true),
    DeckKeySpec("→", usageId = HidKeyMap.KEY_RIGHT, weight = 1.1f, repeatable = true),
)

// Compact 5-row layout for narrow portrait phones
private val COMPACT_ROW_NUM = listOf(
    DeckKeySpec("1", "!", HidKeyMap.KEY_1),
    DeckKeySpec("2", "@", HidKeyMap.KEY_2),
    DeckKeySpec("3", "#", HidKeyMap.KEY_3),
    DeckKeySpec("4", "$", HidKeyMap.KEY_4),
    DeckKeySpec("5", "%", HidKeyMap.KEY_5),
    DeckKeySpec("6", "^", HidKeyMap.KEY_6),
    DeckKeySpec("7", "&", HidKeyMap.KEY_7),
    DeckKeySpec("8", "*", HidKeyMap.KEY_8),
    DeckKeySpec("9", "(", HidKeyMap.KEY_9),
    DeckKeySpec("0", ")", HidKeyMap.KEY_0),
    DeckKeySpec("⌫", usageId = HidKeyMap.KEY_BACKSPACE, weight = 1.35f, repeatable = true),
)

private val COMPACT_ROW_TOP = listOf(
    DeckKeySpec("q", "Q", HidKeyMap.KEY_Q, isLetter = true),
    DeckKeySpec("w", "W", HidKeyMap.KEY_W, isLetter = true),
    DeckKeySpec("e", "E", HidKeyMap.KEY_E, isLetter = true),
    DeckKeySpec("r", "R", HidKeyMap.KEY_R, isLetter = true),
    DeckKeySpec("t", "T", HidKeyMap.KEY_T, isLetter = true),
    DeckKeySpec("y", "Y", HidKeyMap.KEY_Y, isLetter = true),
    DeckKeySpec("u", "U", HidKeyMap.KEY_U, isLetter = true),
    DeckKeySpec("i", "I", HidKeyMap.KEY_I, isLetter = true),
    DeckKeySpec("o", "O", HidKeyMap.KEY_O, isLetter = true),
    DeckKeySpec("p", "P", HidKeyMap.KEY_P, isLetter = true),
)

private val COMPACT_ROW_HOME = listOf(
    DeckKeySpec("a", "A", HidKeyMap.KEY_A, isLetter = true),
    DeckKeySpec("s", "S", HidKeyMap.KEY_S, isLetter = true),
    DeckKeySpec("d", "D", HidKeyMap.KEY_D, isLetter = true),
    DeckKeySpec("f", "F", HidKeyMap.KEY_F, isLetter = true),
    DeckKeySpec("g", "G", HidKeyMap.KEY_G, isLetter = true),
    DeckKeySpec("h", "H", HidKeyMap.KEY_H, isLetter = true),
    DeckKeySpec("j", "J", HidKeyMap.KEY_J, isLetter = true),
    DeckKeySpec("k", "K", HidKeyMap.KEY_K, isLetter = true),
    DeckKeySpec("l", "L", HidKeyMap.KEY_L, isLetter = true),
    DeckKeySpec("↵", usageId = HidKeyMap.KEY_ENTER, weight = 1.45f),
)

private val COMPACT_ROW_BOTTOM = listOf(
    DeckKeySpec("⇧", usageId = HidKeyMap.KEY_NONE, weight = 1.45f, modKind = ModKind.Shift),
    DeckKeySpec("z", "Z", HidKeyMap.KEY_Z, isLetter = true),
    DeckKeySpec("x", "X", HidKeyMap.KEY_X, isLetter = true),
    DeckKeySpec("c", "C", HidKeyMap.KEY_C, isLetter = true),
    DeckKeySpec("v", "V", HidKeyMap.KEY_V, isLetter = true),
    DeckKeySpec("b", "B", HidKeyMap.KEY_B, isLetter = true),
    DeckKeySpec("n", "N", HidKeyMap.KEY_N, isLetter = true),
    DeckKeySpec("m", "M", HidKeyMap.KEY_M, isLetter = true),
    DeckKeySpec(",", "<", HidKeyMap.KEY_COMMA),
    DeckKeySpec(".", ">", HidKeyMap.KEY_PERIOD),
    DeckKeySpec("/", "?", HidKeyMap.KEY_SLASH),
)

private val COMPACT_ROW_SPACE = listOf(
    DeckKeySpec("Esc", usageId = HidKeyMap.KEY_ESC, weight = 1.15f),
    DeckKeySpec("Ctrl", usageId = HidKeyMap.KEY_NONE, weight = 1.2f, modKind = ModKind.Ctrl),
    DeckKeySpec("Tab", usageId = HidKeyMap.KEY_TAB, weight = 1.1f),
    DeckKeySpec("Space", usageId = HidKeyMap.KEY_SPACE, weight = 3.4f),
    DeckKeySpec("←", usageId = HidKeyMap.KEY_LEFT, weight = 0.95f, repeatable = true),
    DeckKeySpec("↑", usageId = HidKeyMap.KEY_UP, weight = 0.95f, repeatable = true),
    DeckKeySpec("↓", usageId = HidKeyMap.KEY_DOWN, weight = 0.95f, repeatable = true),
    DeckKeySpec("→", usageId = HidKeyMap.KEY_RIGHT, weight = 0.95f, repeatable = true),
)

/**
 * Proportional virtual laptop keyboard.
 *
 * Uses realistic physical key proportions (bounded row height + slim F-row + centered max width)
 * rather than stretching keys to fill arbitrary screen heights.
 */
@Composable
fun VirtualKeyboard(
    compact: Boolean,
    showFunctionRow: Boolean,
    hapticsEnabled: Boolean,
    onModifierMaskChange: (Int) -> Unit,
    onKeyDown: (usageId: Int, modifierMask: Int) -> Unit,
    onKeyUp: (usageId: Int, modifierMask: Int) -> Unit,
    onTapKey: (usageId: Int, modifierMask: Int) -> Unit,
    modifier: Modifier = Modifier,
    maxKeyboardHeight: Dp = 360.dp,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current

    var shiftSticky by remember { mutableStateOf(false) }
    var capsOn by remember { mutableStateOf(false) }
    var ctrlSticky by remember { mutableStateOf(false) }
    var altSticky by remember { mutableStateOf(false) }
    var metaSticky by remember { mutableStateOf(false) }

    val currentModifierMask =
        (if (shiftSticky) HidDescriptor.MOD_LEFT_SHIFT else 0) or
            (if (ctrlSticky) HidDescriptor.MOD_LEFT_CTRL else 0) or
            (if (altSticky) HidDescriptor.MOD_LEFT_ALT else 0) or
            (if (metaSticky) HidDescriptor.MOD_LEFT_META else 0)

    LaunchedEffect(currentModifierMask) {
        onModifierMaskChange(currentModifierMask)
    }

    fun clearOneShotModifiers() {
        shiftSticky = false
        ctrlSticky = false
        altSticky = false
        metaSticky = false
    }

    val includeFnRow = !compact && showFunctionRow
    val rows = remember(compact, includeFnRow) {
        if (compact) {
            listOf(
                COMPACT_ROW_NUM,
                COMPACT_ROW_TOP,
                COMPACT_ROW_HOME,
                COMPACT_ROW_BOTTOM,
                COMPACT_ROW_SPACE,
            )
        } else {
            buildList {
                if (includeFnRow) add(ROW_FN)
                add(ROW_NUM)
                add(ROW_TOP)
                add(ROW_HOME)
                add(ROW_BOTTOM)
                add(ROW_SPACE)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val boardMaxWidth = if (compact) maxWidth else minOf(maxWidth, 980.dp)
        val vPadding = if (compact) 6.dp else 8.dp
        val hPadding = if (compact) 4.dp else 8.dp
        val rowGap = if (compact) 4.dp else 5.dp
        val keyGap = if (compact) 3.dp else 5.dp

        // Calculate natural physical key height from board width
        val unitWidth = if (compact) {
            (boardMaxWidth - hPadding * 2) / 10.6f
        } else {
            (boardMaxWidth - hPadding * 2) / 14.8f
        }
        val idealMainRow = if (compact) {
            (unitWidth * 1.15f).coerceIn(36.dp, 48.dp)
        } else {
            (unitWidth * 0.86f).coerceIn(38.dp, 54.dp)
        }

        // Ensure keyboard fits comfortably within maxKeyboardHeight without squishing unevenly
        val mainRowCount = if (includeFnRow) rows.size - 1 else rows.size
        val fnRatio = if (includeFnRow) 0.68f else 0f
        val chromeHeight = vPadding * 2 + rowGap * (rows.size - 1)
        val availableForKeys = (maxKeyboardHeight - chromeHeight).coerceAtLeast(150.dp)
        val maxAllowedMainRow = availableForKeys / (mainRowCount + fnRatio)

        val mainRowHeight = minOf(idealMainRow, maxAllowedMainRow).coerceAtLeast(30.dp)
        val fnRowHeight = (mainRowHeight * 0.68f).coerceIn(24.dp, 36.dp)

        Column(
            modifier = Modifier
                .widthIn(max = boardMaxWidth)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                .padding(horizontal = hPadding, vertical = vPadding),
            verticalArrangement = Arrangement.spacedBy(rowGap),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                val isFnRow = includeFnRow && rowIndex == 0
                val rowH = if (isFnRow) fnRowHeight else mainRowHeight
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowH),
                    horizontalArrangement = Arrangement.spacedBy(keyGap),
                ) {
                    for (spec in row) {
                        val isModActive = when (spec.modKind) {
                            ModKind.Shift -> shiftSticky
                            ModKind.Caps -> capsOn
                            ModKind.Ctrl -> ctrlSticky
                            ModKind.Alt -> altSticky
                            ModKind.Meta -> metaSticky
                            null -> false
                        }
                        val displayLabel = when {
                            spec.isLetter -> if (shiftSticky xor capsOn) spec.shiftLabel ?: spec.label else spec.label
                            shiftSticky && spec.shiftLabel != null -> spec.shiftLabel
                            else -> spec.label
                        }
                        val secondaryLabel = if (!compact && !isFnRow && !spec.isLetter && spec.shiftLabel != null) {
                            if (shiftSticky) spec.label else spec.shiftLabel
                        } else {
                            null
                        }

                        KeyboardKeyCap(
                            label = displayLabel,
                            secondaryLabel = secondaryLabel,
                            active = isModActive,
                            isActionKey = spec.modKind != null ||
                                spec.usageId == HidKeyMap.KEY_ENTER ||
                                spec.usageId == HidKeyMap.KEY_BACKSPACE ||
                                isFnRow,
                            compact = compact || isFnRow,
                            repeatable = spec.repeatable,
                            onPress = {
                                if (hapticsEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                when (spec.modKind) {
                                    ModKind.Shift -> shiftSticky = !shiftSticky
                                    ModKind.Ctrl -> ctrlSticky = !ctrlSticky
                                    ModKind.Alt -> altSticky = !altSticky
                                    ModKind.Meta -> metaSticky = !metaSticky
                                    ModKind.Caps -> {
                                        capsOn = !capsOn
                                        onTapKey(HidKeyMap.KEY_CAPS_LOCK, 0)
                                    }
                                    null -> onKeyDown(spec.usageId, currentModifierMask)
                                }
                            },
                            onRepeat = {
                                if (spec.modKind == null) {
                                    onTapKey(spec.usageId, currentModifierMask)
                                }
                            },
                            onRelease = {
                                if (spec.modKind == null) {
                                    onKeyUp(spec.usageId, currentModifierMask)
                                    if (shiftSticky || ctrlSticky || altSticky || metaSticky) {
                                        clearOneShotModifiers()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(spec.weight)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardKeyCap(
    label: String,
    secondaryLabel: String?,
    active: Boolean,
    isActionKey: Boolean,
    compact: Boolean,
    repeatable: Boolean,
    onPress: () -> Unit,
    onRepeat: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val keyScope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    val onPressState by rememberUpdatedState(onPress)
    val onRepeatState by rememberUpdatedState(onRepeat)
    val onReleaseState by rememberUpdatedState(onRelease)
    val repeatableState by rememberUpdatedState(repeatable)

    val containerColor = when {
        pressed -> colors.primary.copy(alpha = 0.34f)
        active -> colors.primaryContainer
        isActionKey -> colors.surfaceVariant.copy(alpha = 0.92f)
        else -> Color(0xFF162033)
    }
    val borderColor = when {
        pressed || active -> colors.primary
        else -> colors.outline.copy(alpha = 0.48f)
    }
    val textColor = when {
        pressed || active -> colors.onPrimaryContainer
        else -> colors.onSurface
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(if (compact) 7.dp else 9.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                onPressState()
                val repeatJob = if (repeatableState) {
                    keyScope.launch {
                        delay(420)
                        while (true) {
                            onRepeatState()
                            delay(55)
                        }
                    }
                } else {
                    null
                }
                waitForUpOrCancellation()
                repeatJob?.cancel()
                pressed = false
                onReleaseState()
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp),
        ) {
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    fontSize = 9.sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 3.dp, top = 1.dp),
                )
            }
            if (active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                )
            }
            Text(
                text = label,
                fontSize = when {
                    compact && label.length > 2 -> 10.sp
                    compact -> 12.sp
                    label.length > 3 -> 11.sp
                    else -> 14.sp
                },
                fontWeight = if (isActionKey) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
