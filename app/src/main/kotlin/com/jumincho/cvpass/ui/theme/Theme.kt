package com.jumincho.cvpass.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Navy and blue of the original 2021 launcher icon. */
object BrandColors {
    val Navy = Color(0xFF002C7A)
    val Blue = Color(0xFF5784FF)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B57D6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE4FF),
    onPrimaryContainer = BrandColors.Navy,
    inversePrimary = Color(0xFFB4C5FF),
    secondary = Color(0xFF525E7D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E2FF),
    onSecondaryContainer = Color(0xFF0E1B37),
    tertiary = Color(0xFF006C4C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF89F8C7),
    onTertiaryContainer = Color(0xFF002114),
    background = Color(0xFFFAF9FF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFAF9FF),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44464F),
    surfaceTint = Color(0xFF2B57D6),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F7),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF757780),
    outlineVariant = Color(0xFFC5C6D0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAF9FF),
    surfaceContainer = Color(0xFFEEEDF4),
    surfaceContainerHigh = Color(0xFFE8E7EF),
    surfaceContainerHighest = Color(0xFFE3E2E9),
    surfaceContainerLow = Color(0xFFF4F3FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDAD9E0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = BrandColors.Navy,
    primaryContainer = Color(0xFF1C44B8),
    onPrimaryContainer = Color(0xFFDCE4FF),
    inversePrimary = Color(0xFF2B57D6),
    secondary = Color(0xFFB9C6EA),
    onSecondary = Color(0xFF23304D),
    secondaryContainer = Color(0xFF3A4664),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFF6CDBAC),
    onTertiary = Color(0xFF003826),
    tertiaryContainer = Color(0xFF005138),
    onTertiaryContainer = Color(0xFF89F8C7),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE3E2E9),
    surfaceVariant = Color(0xFF44464F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    surfaceTint = Color(0xFFB4C5FF),
    inverseSurface = Color(0xFFE3E2E9),
    inverseOnSurface = Color(0xFF2F3036),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8F909A),
    outlineVariant = Color(0xFF44464F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF38393F),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceDim = Color(0xFF121318),
)

/** Colours for pass and result states that Material 3 has no role for. */
@Immutable
data class StatusColors(
    val success: Color,
    val onSuccess: Color,
    val pending: Color,
    val onPending: Color,
)

private val LightStatusColors = StatusColors(
    success = Color(0xFFB7F1D3),
    onSuccess = Color(0xFF00210F),
    pending = Color(0xFFFFDEA6),
    onPending = Color(0xFF271900),
)

private val DarkStatusColors = StatusColors(
    success = Color(0xFF005236),
    onSuccess = Color(0xFFB7F1D3),
    pending = Color(0xFF5C4200),
    onPending = Color(0xFFFFDEA6),
)

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** CV-PASS's Material 3 theme, following the system light or dark setting. */
@Composable
fun CvPassTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
    }
}

/** Accessors for theme values beyond [MaterialTheme]. */
object CvPassTheme {
    /** Colours for success and pending states. */
    val statusColors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}
