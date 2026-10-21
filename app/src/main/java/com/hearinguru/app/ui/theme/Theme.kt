package com.hearinguru.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary            = Primary,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary            = Secondary,
    onSecondary          = OnSecondary,
    secondaryContainer   = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary            = Tertiary,
    onTertiary          = OnTertiary,
    tertiaryContainer   = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error            = Error,
    onError          = OnError,
    errorContainer   = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background    = Background,
    onBackground  = OnBackground,
    surface       = Surface,
    onSurface     = OnSurface,
    surfaceVariant    = SurfaceVariant,
    onSurfaceVariant  = OnSurfaceVariant,
    outline        = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface   = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary   = InversePrimary,
    surfaceContainer        = SurfaceContainer,
    surfaceContainerHigh    = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow     = SurfaceContainerLow,
    surfaceContainerLowest  = SurfaceContainerLowest,
)

private val DarkColors = darkColorScheme(
    primary            = DarkPrimary,
    onPrimary          = DarkOnPrimary,
    primaryContainer   = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary            = DarkSecondary,
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary            = DarkTertiary,
    onTertiary          = DarkOnTertiary,
    tertiaryContainer   = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error            = DarkError,
    onError          = DarkOnError,
    errorContainer   = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background    = DarkBackground,
    onBackground  = DarkOnBackground,
    surface       = DarkSurface,
    onSurface     = DarkOnSurface,
    surfaceVariant    = DarkSurfaceVariant,
    onSurfaceVariant  = DarkOnSurfaceVariant,
    outline        = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface   = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary   = DarkInversePrimary,
    surfaceContainer        = DarkSurfaceContainer,
    surfaceContainerHigh    = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceContainerLow     = DarkSurfaceContainerLow,
    surfaceContainerLowest  = DarkSurfaceContainerLowest,
)

// Extended colors not in M3 scheme
data class ExtendedColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = Success,
        successContainer = SuccessContainer,
        onSuccessContainer = Color(0xFF1B3A22),
        inverseSurface = InverseSurface,
        inverseOnSurface = InverseOnSurface,
        inversePrimary = InversePrimary,
    )
}

@Composable
fun HearinGuruTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val extendedColors = if (darkTheme) {
        ExtendedColors(
            success = DarkSuccess,
            successContainer = DarkSuccessContainer,
            onSuccessContainer = Color(0xFF8BC898),
            inverseSurface = DarkInverseSurface,
            inverseOnSurface = DarkInverseOnSurface,
            inversePrimary = DarkInversePrimary,
        )
    } else {
        ExtendedColors(
            success = Success,
            successContainer = SuccessContainer,
            onSuccessContainer = Color(0xFF1B3A22),
            inverseSurface = InverseSurface,
            inverseOnSurface = InverseOnSurface,
            inversePrimary = InversePrimary,
        )
    }

    // Invert status bar icon tint: light mode → light icons, dark mode → dark icons
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

// Convenience accessor
val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current
