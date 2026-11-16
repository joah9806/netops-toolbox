package com.qoder.netops.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = Color(0xFF111113),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1C1C1F),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF5A5A60),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F8F6),
    onBackground = Color(0xFF111113),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111113),
    surfaceVariant = Color(0xFFF2F2EF),
    onSurfaceVariant = Color(0xFF71717A),
    outline = Color(0xFFE2E2DD),
    outlineVariant = Color(0xFFECECE7),
    error = Color(0xFFD92D20),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF8C1D18)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF5F5F2),
    onPrimary = Color(0xFF111113),
    primaryContainer = Color(0xFFEDEDE8),
    onPrimaryContainer = Color(0xFF111113),
    secondary = Color(0xFFB4B4BA),
    onSecondary = Color(0xFF111113),
    background = Color(0xFF0B0B0D),
    onBackground = Color(0xFFF3F3F0),
    surface = Color(0xFF151517),
    onSurface = Color(0xFFF3F3F0),
    surfaceVariant = Color(0xFF1E1E22),
    onSurfaceVariant = Color(0xFF9A9AA2),
    outline = Color(0xFF2C2C32),
    outlineVariant = Color(0xFF232328),
    error = Color(0xFFFF6B60),
    onError = Color(0xFF1A0503),
    errorContainer = Color(0xFF3B1210),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Immutable
data class NetOpsPalette(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val terminalBackground: Color,
    val terminalBorder: Color,
    val terminalDim: Color,
    val logoBackground: Color,
    val logoForeground: Color,
    val accentSoft: Color,
    val gridLine: Color
)

private val LightPalette = NetOpsPalette(
    success = Color(0xFF0F9D6E),
    successContainer = Color(0xFFE7F6EF),
    warning = Color(0xFFB7791F),
    warningContainer = Color(0xFFFBF3E2),
    terminalBackground = Color(0xFFFBFBF9),
    terminalBorder = Color(0xFFE8E8E3),
    terminalDim = Color(0xFF8B8B92),
    logoBackground = Color(0xFFFFFFFF),
    logoForeground = Color(0xFF101012),
    accentSoft = Color(0xFFF2F2EF),
    gridLine = Color(0xFFECECE7)
)

private val DarkPalette = NetOpsPalette(
    success = Color(0xFF4ADE9F),
    successContainer = Color(0xFF10251C),
    warning = Color(0xFFF0C674),
    warningContainer = Color(0xFF2A2114),
    terminalBackground = Color(0xFF09090B),
    terminalBorder = Color(0xFF26262C),
    terminalDim = Color(0xFF7C7C84),
    logoBackground = Color(0xFFFFFFFF),
    logoForeground = Color(0xFF101012),
    accentSoft = Color(0xFF1C1C20),
    gridLine = Color(0xFF232328)
)

val LocalNetOpsPalette = staticCompositionLocalOf { LightPalette }

val MonoFamily = FontFamily.Monospace

val TerminalTextStyle = TextStyle(
    fontFamily = MonoFamily,
    fontSize = 12.sp,
    lineHeight = 18.sp
)

private val NetOpsTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        letterSpacing = 1.2.sp
    )
)

@Composable
fun netOpsPalette(): NetOpsPalette = LocalNetOpsPalette.current

@Composable
fun NetOpsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalNetOpsPalette provides if (darkTheme) DarkPalette else LightPalette
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = NetOpsTypography,
            content = content
        )
    }
}