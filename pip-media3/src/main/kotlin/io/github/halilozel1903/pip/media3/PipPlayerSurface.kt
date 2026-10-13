package io.github.halilozel1903.pip.media3

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import io.github.halilozel1903.pip.PipState
import io.github.halilozel1903.pip.pipSourceRect

/**
 * Renders [player]'s video into a `SurfaceView`, letterboxed to the video's aspect ratio inside [modifier]'s
 * bounds on a [background], and reports the picture's bounds as the PiP source rect hint of [pipState]. In the
 * PiP window it fills the window, which already has the video's aspect ratio.
 *
 * A `SurfaceView` is what Android recommends for PiP video: it keeps rendering during the transition and the
 * system can resize it seamlessly.
 */
@Composable
public fun PipPlayerSurface(
    player: Player,
    pipState: PipState?,
    modifier: Modifier = Modifier,
    background: Color = Color.Black,
    fallbackAspectRatio: Float = 16f / 9f,
) {
    val snapshot = rememberPlayerPipSnapshot(player)
    val ratio = snapshot.videoAspectRatio ?: fallbackAspectRatio
    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    Box(modifier.background(background), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context -> SurfaceView(context).also { surfaceView = it } },
            modifier = Modifier
                .aspectRatio(ratio)
                .then(if (pipState != null) Modifier.pipSourceRect(pipState) else Modifier),
        )
    }

    DisposableEffect(player, surfaceView) {
        val view = surfaceView
        view?.let(player::setVideoSurfaceView)
        onDispose { view?.let(player::clearVideoSurfaceView) }
    }
}
