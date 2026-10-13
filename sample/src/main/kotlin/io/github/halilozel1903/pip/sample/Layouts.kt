package io.github.halilozel1903.pip.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import io.github.halilozel1903.pip.HideInPip
import io.github.halilozel1903.pip.PipState
import io.github.halilozel1903.pip.core.AutoEnterStrategy
import io.github.halilozel1903.pip.R as PipR

/*
 * Each layout keeps the video at the same place in the composition and only changes its size in PiP: the
 * SurfaceView survives the transition, so the video keeps playing while the window shrinks and grows.
 */

/** Phones: the video on top, details, PiP options and the playlist below. */
@Composable
fun PhonePlayerLayout(player: Player, pip: PipState, nowPlaying: NowPlaying, options: PipOptions) {
    Column(Modifier.fillMaxSize()) {
        val videoModifier = if (pip.isInPip) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }
        VideoArea(player, pip, videoModifier)
        HideInPip(pip) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                NowPlayingHeader(nowPlaying)
                Spacer(Modifier.height(16.dp))
                TransportControls(player, pip, nowPlaying, Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                PipPanel(pip, nowPlaying, options, Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                ClipList(player, nowPlaying)
            }
        }
    }
}

/** Tablets in landscape: the video and controls on the left, the playlist and PiP options on the right. */
@Composable
fun TabletPlayerLayout(player: Player, pip: PipState, nowPlaying: NowPlaying, options: PipOptions) {
    val outer = if (pip.isInPip) Modifier.fillMaxSize() else Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp)
    Row(outer, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            val videoModifier = if (pip.isInPip) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            VideoArea(player, pip, videoModifier)
            HideInPip(pip) {
                Column(Modifier.padding(top = 20.dp)) {
                    NowPlayingHeader(nowPlaying)
                    Spacer(Modifier.height(16.dp))
                    TransportControls(player, pip, nowPlaying, Modifier.fillMaxWidth())
                }
            }
        }
        HideInPip(pip) {
            Column(
                Modifier
                    .width(380.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                ClipList(player, nowPlaying)
                Spacer(Modifier.height(16.dp))
                PipPanel(pip, nowPlaying, options, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Android TV: the video full screen, with title, progress and D-pad buttons over a gradient. */
@Composable
fun TvPlayerLayout(player: Player, pip: PipState, nowPlaying: NowPlaying, options: PipOptions) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VideoArea(player, pip, Modifier.fillMaxSize(), showPipButton = false)
        HideInPip(pip) {
            val playFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
                    .padding(start = 56.dp, end = 56.dp, top = 96.dp, bottom = 40.dp),
            ) {
                Column {
                    NowPlayingHeader(nowPlaying, large = true)
                    Spacer(Modifier.height(20.dp))
                    ProgressLine(rememberProgress(player), Modifier.fillMaxWidth(), height = 4.dp)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        TvButton(PipR.drawable.pip_ic_skip_previous, "Previous") { player.seekToPreviousMediaItem() }
                        TvButton(
                            if (nowPlaying.isPlaying) PipR.drawable.pip_ic_pause else PipR.drawable.pip_ic_play,
                            if (nowPlaying.isPlaying) "Pause" else "Play",
                            modifier = Modifier.focusRequester(playFocus),
                            primary = true,
                        ) { if (player.isPlaying) player.pause() else player.play() }
                        TvButton(PipR.drawable.pip_ic_skip_next, "Next") { player.seekToNextMediaItem() }
                        if (pip.isSupported) {
                            TvButton(PipR.drawable.pip_ic_enter, "Picture-in-picture") { pip.enter() }
                        }
                    }
                }
            }
            TvPipBadge(pip, nowPlaying, options, Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 56.dp))
        }
    }
}

/** What PiP would do on this TV right now. */
@Composable
private fun TvPipBadge(pip: PipState, nowPlaying: NowPlaying, options: PipOptions, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White,
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "PICTURE-IN-PICTURE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            val status = when {
                !pip.isSupported -> "Not supported on this device"
                pip.autoEnterStrategy == AutoEnterStrategy.Off -> if (options.autoEnter) "Auto-enter while playing" else "Auto-enter off"
                else -> "Auto-enter on Home"
            }
            Text(status, style = MaterialTheme.typography.bodyLarge)
            val ratio = pip.config.aspectRatio?.toString() ?: "default"
            Text(
                "$ratio window · ${pip.describeActions()}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
            )
            if (nowPlaying.videoWidth > 0) {
                Text(
                    "Video ${nowPlaying.videoWidth}×${nowPlaying.videoHeight}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}
