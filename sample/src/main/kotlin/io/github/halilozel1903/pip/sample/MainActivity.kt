package io.github.halilozel1903.pip.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.channels.Channel

/**
 * A video player with picture-in-picture on phones, tablets and Android TV. It plays the bundled demo clips with
 * Media3 ExoPlayer; `rememberPlayerPipState` gives it auto-enter while playing, the video's aspect ratio,
 * previous / play-pause / next actions in the PiP window and a source rect hint for the transition.
 *
 * Extras (used by `scripts/screenshots.sh`):
 * - `--es scene player`: logs `SCENE_READY player` once video frames are on screen and playback has run a few
 *   seconds, or `SCENE_FAILED player: <reason>`.
 * - `--ez enter_pip true`: enters picture-in-picture right away, like the in-app PiP button.
 */
class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private val pipRequests = Channel<Unit>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always dark, so the system bars get light icons whatever the system theme is.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val isScene = intent.getStringExtra(EXTRA_SCENE) != null
        player = ExoPlayer.Builder(this).build().apply {
            setMediaItems(DemoClips.all.map { it.mediaItem() })
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            prepare()
        }
        handlePipExtra(intent)
        setContent {
            SampleTheme {
                // The Surface makes text default to onBackground, so it stays readable on the dark theme.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    PlayerScreen(player = player, pipRequests = pipRequests, reportScene = isScene)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePipExtra(intent)
    }

    private fun handlePipExtra(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_ENTER_PIP, false)) {
            player.play()
            pipRequests.trySend(Unit)
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SCENE = "scene"
        const val EXTRA_ENTER_PIP = "enter_pip"
    }
}
