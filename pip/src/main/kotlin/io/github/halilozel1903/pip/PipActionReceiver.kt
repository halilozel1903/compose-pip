package io.github.halilozel1903.pip

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

/**
 * Delivers taps on picture-in-picture actions back to the app.
 *
 * A PiP window lives in System UI, so its buttons are `RemoteAction`s that fire a `PendingIntent`. This helper
 * creates those intents as explicit, immutable broadcasts to this app only, and registers a receiver that is not
 * exported, so no other app can trigger your actions. [PipController] uses it for you; use it directly when you
 * build `PictureInPictureParams` yourself.
 *
 * Call [register] before the actions can be tapped and [close] when done (for example in `onDestroy`).
 */
public class PipActionReceiver(
    context: Context,
    private val onAction: (actionId: String) -> Unit,
) : AutoCloseable {
    private val appContext = context.applicationContext

    // One broadcast action per receiver, so two screens using PiP one after the other never get each other's taps.
    private val broadcastAction = "${appContext.packageName}.pip.ACTION_${instances.getAndIncrement()}"
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != broadcastAction) return
            val id = intent.getStringExtra(EXTRA_ACTION_ID) ?: return
            onAction(id)
        }
    }

    /** Starts listening for taps. Safe to call more than once. */
    public fun register() {
        if (registered) return
        ContextCompat.registerReceiver(appContext, receiver, IntentFilter(broadcastAction), ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
    }

    /** The `PendingIntent` that reports [actionId] to this receiver. */
    public fun pendingIntent(actionId: String): PendingIntent {
        val intent = Intent(broadcastAction)
            .setPackage(appContext.packageName)
            .putExtra(EXTRA_ACTION_ID, actionId)
        // Intents that differ only in extras are the same PendingIntent: give every action its own request code.
        return PendingIntent.getBroadcast(
            appContext,
            actionId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** The `RemoteAction` for [action], ready for `PictureInPictureParams.Builder.setActions`. */
    public fun remoteAction(action: PipAction): RemoteAction =
        RemoteAction(
            Icon.createWithResource(appContext, action.icon),
            action.title,
            action.contentDescription,
            pendingIntent(action.id),
        ).apply { isEnabled = action.enabled }

    /** Stops listening. Safe to call more than once. */
    override fun close() {
        if (!registered) return
        registered = false
        try {
            appContext.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered.
        }
    }

    public companion object {
        /** The extra holding the [PipAction.id] in a delivered broadcast. */
        public const val EXTRA_ACTION_ID: String = "io.github.halilozel1903.pip.extra.ACTION_ID"

        private val instances = AtomicInteger()
    }
}
