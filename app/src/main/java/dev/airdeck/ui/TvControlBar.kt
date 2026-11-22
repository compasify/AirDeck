package dev.airdeck.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.airdeck.R
import dev.airdeck.hid.HidDescriptor

@Composable
fun TvControlBar(
    hapticsEnabled: Boolean,
    onConsumerKey: (Int) -> Unit,
    onOpenQuickText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    fun fire(usage: Int) {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        onConsumerKey(usage)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvChip(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = stringResource(R.string.tv_back),
            onClick = { fire(HidDescriptor.CONSUMER_AC_BACK) },
        )
        TvChip(
            icon = Icons.Default.Home,
            label = stringResource(R.string.tv_home),
            onClick = { fire(HidDescriptor.CONSUMER_AC_HOME) },
        )
        TvChip(
            icon = Icons.Default.Menu,
            label = stringResource(R.string.tv_menu),
            onClick = { fire(HidDescriptor.CONSUMER_MENU) },
        )
        TvChip(
            icon = Icons.Default.Search,
            label = stringResource(R.string.tv_search),
            onClick = { fire(HidDescriptor.CONSUMER_AC_SEARCH) },
        )
        TvChip(
            icon = Icons.Default.PlayArrow,
            label = stringResource(R.string.tv_play_pause),
            onClick = { fire(HidDescriptor.CONSUMER_PLAY_PAUSE) },
        )
        TvChip(
            icon = Icons.AutoMirrored.Filled.VolumeDown,
            label = "-",
            contentDescription = stringResource(R.string.tv_vol_down),
            onClick = { fire(HidDescriptor.CONSUMER_VOLUME_DOWN) },
        )
        TvChip(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            label = "+",
            contentDescription = stringResource(R.string.tv_vol_up),
            onClick = { fire(HidDescriptor.CONSUMER_VOLUME_UP) },
        )
        TvChip(
            icon = Icons.AutoMirrored.Filled.VolumeOff,
            label = stringResource(R.string.tv_mute),
            onClick = { fire(HidDescriptor.CONSUMER_MUTE) },
        )
        TvChip(
            icon = Icons.Default.ContentPaste,
            label = stringResource(R.string.send_text_button),
            accent = true,
            onClick = onOpenQuickText,
        )
    }
}

@Composable
private fun TvChip(
    icon: ImageVector,
    label: String,
    contentDescription: String = label,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (accent) colors.primaryContainer.copy(alpha = 0.75f) else colors.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            width = 1.dp,
            color = if (accent) colors.primary.copy(alpha = 0.7f) else colors.outline.copy(alpha = 0.45f),
        ),
        modifier = Modifier.height(34.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (accent) colors.primary else colors.onSurface,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (accent) colors.onPrimaryContainer else colors.onSurface,
                maxLines = 1,
            )
        }
    }
}
