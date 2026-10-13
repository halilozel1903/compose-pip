package io.github.halilozel1903.pip.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** A dark theme: video apps stay dark so the picture stands out. */
@Composable
fun SampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7CE0C3),
            onPrimary = Color(0xFF00382B),
            primaryContainer = Color(0xFF1C4D41),
            onPrimaryContainer = Color(0xFFB4F5E1),
            secondary = Color(0xFFFFB27A),
            onSecondary = Color(0xFF4A2300),
            background = Color(0xFF0D0F16),
            onBackground = Color(0xFFE6E8F0),
            surface = Color(0xFF0D0F16),
            onSurface = Color(0xFFE6E8F0),
            surfaceVariant = Color(0xFF1C2030),
            onSurfaceVariant = Color(0xFFA9AFC2),
            surfaceContainer = Color(0xFF161925),
            surfaceContainerHigh = Color(0xFF1C2030),
            outline = Color(0xFF3A4054),
            outlineVariant = Color(0xFF2A2F40),
        ),
        content = content,
    )
}
