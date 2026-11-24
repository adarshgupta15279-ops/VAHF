package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val VahfLightColorScheme = lightColorScheme(
    primary = VahfNavyDeep,
    onPrimary = VahfWhite,
    primaryContainer = VahfCyanSoftContainer,
    onPrimaryContainer = VahfNavyDeep,
    secondary = VahfCyanPrimary,
    onSecondary = VahfWhite,
    secondaryContainer = VahfSurfaceMuted,
    onSecondaryContainer = VahfNavyMid,
    tertiary = VahfNavySoft,
    onTertiary = VahfWhite,
    background = VahfWhite,
    onBackground = VahfTextPrimary,
    surface = VahfWhite,
    onSurface = VahfTextPrimary,
    surfaceVariant = VahfOffWhite,
    onSurfaceVariant = VahfTextSecondary,
    outline = VahfBorderSubtle,
    outlineVariant = VahfBorderSubtle,
    error = VahfError,
    onError = VahfWhite
)

val VahfShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VahfTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VahfLightColorScheme,
        typography = VahfTypography,
        shapes = VahfShapes,
        content = content
    )
}
