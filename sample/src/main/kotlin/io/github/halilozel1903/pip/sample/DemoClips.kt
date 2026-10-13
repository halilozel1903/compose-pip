package io.github.halilozel1903.pip.sample

import androidx.annotation.DrawableRes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * The sample's playlist: three 10 second clips bundled in `assets/clips`. They are generated demo footage, drawn
 * frame by frame by `scripts/generate-demo-clips.py` (no camera footage, no third-party artwork) and encoded as
 * 1280 x 720 H.264, so the app and its screenshots need no network.
 */
data class DemoClip(
    val id: String,
    val title: String,
    val description: String,
    @param:DrawableRes val thumbnail: Int,
) {
    val uri: String get() = "asset:///clips/$id.mp4"

    fun mediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setDescription(description)
                .build(),
        )
        .build()
}

object DemoClips {
    val all: List<DemoClip> = listOf(
        DemoClip("aurora", "Aurora", "Northern lights over a mountain ridge", R.drawable.thumb_aurora),
        DemoClip("tide", "Tide", "Sunset over a rolling sea", R.drawable.thumb_tide),
        DemoClip("lanterns", "Lanterns", "Sky lanterns rising over a lake", R.drawable.thumb_lanterns),
    )

    const val DURATION_LABEL: String = "0:10"
}
