package io.github.halilozel1903.pip.sample

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import io.github.halilozel1903.pip.HideInPip
import io.github.halilozel1903.pip.PipState
import io.github.halilozel1903.pip.core.AutoEnterStrategy
import io.github.halilozel1903.pip.core.MediaPipActionLayout
import io.github.halilozel1903.pip.media3.PipPlayerSurface
import io.github.halilozel1903.pip.R as PipR

/**
 * The video: Media3 output on a SurfaceView that reports its bounds as the PiP source rect hint. The overlay (PiP
 * button, progress) is hidden in PiP, where the window shows the video alone.
 */
@Composable
fun VideoArea(
    player: Player,
    pip: PipState,
    modifier: Modifier = Modifier,
    showPipButton: Boolean = true,
) {
    Box(modifier.background(Color.Black)) {
        PipPlayerSurface(player = player, pipState = pip, modifier = Modifier.fillMaxSize())
        HideInPip(pip) {
            val progress = rememberProgress(player)
            if (showPipButton && pip.isSupported) {
                Surface(
                    onClick = { pip.enter() },
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Icon(painterResource(PipR.drawable.pip_ic_enter), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PiP", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            ProgressLine(progress, Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** A thin progress bar along the bottom of the video. */
@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, height: Dp = 3.dp) {
    Box(modifier.fillMaxWidth().height(height).background(Color.White.copy(alpha = 0.25f))) {
        Box(Modifier.fillMaxWidth(progress).height(height).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
fun NowPlayingHeader(nowPlaying: NowPlaying, modifier: Modifier = Modifier, large: Boolean = false) {
    Column(modifier) {
        Text(
            "NOW PLAYING · ${nowPlaying.index + 1} OF ${DemoClips.all.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            nowPlaying.clip.title,
            style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "${nowPlaying.clip.description} · generated demo clip",
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Previous, play/pause and next, plus a button that enters PiP. */
@Composable
fun TransportControls(player: Player, pip: PipState, nowPlaying: NowPlaying, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RoundButton(PipR.drawable.pip_ic_skip_previous, "Previous", size = 48.dp) { player.seekToPreviousMediaItem() }
        Spacer(Modifier.width(8.dp))
        RoundButton(
            if (nowPlaying.isPlaying) PipR.drawable.pip_ic_pause else PipR.drawable.pip_ic_play,
            if (nowPlaying.isPlaying) "Pause" else "Play",
            size = 60.dp,
            filled = true,
        ) { if (player.isPlaying) player.pause() else player.play() }
        Spacer(Modifier.width(8.dp))
        RoundButton(PipR.drawable.pip_ic_skip_next, "Next", size = 48.dp) { player.seekToNextMediaItem() }
        Spacer(Modifier.weight(1f))
        if (pip.isSupported) {
            Surface(
                onClick = { pip.enter() },
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = Color.Transparent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(painterResource(PipR.drawable.pip_ic_enter), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enter PiP", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun RoundButton(
    @DrawableRes icon: Int,
    label: String,
    size: Dp,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(size * 0.5f))
        }
    }
}

@Composable
fun ClipList(player: Player, nowPlaying: NowPlaying, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Up next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        DemoClips.all.forEachIndexed { index, clip ->
            val current = index == nowPlaying.index
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (current) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .clickable(role = Role.Button) { player.seekTo(index, 0) }
                    .padding(8.dp),
            ) {
                Image(
                    painterResource(clip.thumbnail),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(88.dp).height(50.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(clip.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        clip.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    if (current) "Playing" else DemoClips.DURATION_LABEL,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The PiP options and what the library is doing with them right now. */
@Composable
fun PipPanel(pip: PipState, nowPlaying: NowPlaying, options: PipOptions, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Picture-in-picture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            OptionSwitch("Auto-enter while playing", options.autoEnter, options.onAutoEnterChange)
            OptionSwitch("Seamless resize", options.seamlessResize, options.onSeamlessResizeChange)
            Text("Window actions", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    MediaPipActionLayout.Skip to "Skip",
                    MediaPipActionLayout.Seek to "Seek",
                    MediaPipActionLayout.PlayPause to "Play",
                    MediaPipActionLayout.None to "None",
                ).forEach { (layout, label) ->
                    Choice(label, selected = options.actionLayout == layout) { options.onActionLayoutChange(layout) }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            PipStatus(pip, nowPlaying)
        }
    }
}

@Composable
fun PipStatus(pip: PipState, nowPlaying: NowPlaying, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!pip.isSupported) {
            StatusRow("Supported", "No: this device has no picture-in-picture")
        }
        StatusRow(
            "Auto-enter",
            when (pip.autoEnterStrategy) {
                AutoEnterStrategy.Params -> "On · setAutoEnterEnabled"
                AutoEnterStrategy.UserLeaveHint -> "On · onUserLeaveHint"
                AutoEnterStrategy.Off -> if (nowPlaying.isPlaying) "Off" else "Off while paused"
            },
        )
        val ratio = pip.config.aspectRatio?.toString() ?: "Default"
        val video = if (nowPlaying.videoWidth > 0) " from ${nowPlaying.videoWidth}×${nowPlaying.videoHeight}" else ""
        StatusRow("Aspect ratio", "$ratio$video")
        StatusRow("Source rect", pip.sourceRectHint?.toString() ?: "Not set")
        StatusRow("Actions", "${pip.describeActions()} (max ${pip.maxActions})")
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OptionSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!checked) }.padding(vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

/** A D-pad friendly pill button for TV with a clear focus ring. */
@Composable
fun TvButton(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val container = when {
        focused -> Color.White
        primary -> MaterialTheme.colorScheme.primary
        else -> Color.White.copy(alpha = 0.14f)
    }
    val content = if (focused || primary) Color(0xFF0D0F16) else Color.White
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 3.dp else 0.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(50)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
