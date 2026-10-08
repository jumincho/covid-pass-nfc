package com.jumincho.cvpass.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colours for pass and result states that Material 3 has no role for. */
@Immutable
data class StatusColors(
    val success: Color,
    val onSuccess: Color,
    val pending: Color,
    val onPending: Color,
)

internal val LightStatusColors = StatusColors(
    success = Color(0xFFB7F1D3),
    onSuccess = Color(0xFF00210F),
    pending = Color(0xFFFFDEA6),
    onPending = Color(0xFF271900),
)

internal val DarkStatusColors = StatusColors(
    success = Color(0xFF005236),
    onSuccess = Color(0xFFB7F1D3),
    pending = Color(0xFF5C4200),
    onPending = Color(0xFFFFDEA6),
)

internal val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
