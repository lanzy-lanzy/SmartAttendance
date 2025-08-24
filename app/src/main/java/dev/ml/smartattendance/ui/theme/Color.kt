package dev.ml.smartattendance.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary colors
val PrimaryBlue = Color(0xFF1976D2)
val PrimaryVariant = Color(0xFF115293)
val Secondary = Color(0xFF03DAC6)
val SecondaryVariant = Color(0xFF018786)

// Surface colors
val Surface = Color(0xFFFFFBFE)
val OnSurface = Color(0xFF1C1B1F)
val SurfaceVariant = Color(0xFFE7E0EC)
val OnSurfaceVariant = Color(0xFF49454F)

// Accent colors
val Tertiary = Color(0xFF7D5260)
val OnTertiary = Color(0xFFFFFFFF)

// Status colors
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFE53935)
val WarningOrange = Color(0xFFFF9800)

// Background colors
val Background = Color(0xFFFFFBFE)
val OnBackground = Color(0xFF1C1B1F)

// Text colors
val OnPrimary = Color(0xFFFFFFFF)
val Primary = PrimaryBlue

// Disabled colors
val DisabledColor = Color(0xFF787579)

// Gradient colors
val Gradient1 = Color(0xFF1976D2)
val Gradient2 = Color(0xFF42A5F5)
val Gradient3 = Color(0xFF90CAF9)

// Light color scheme
val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Secondary,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF002020),
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = ErrorRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000)
)

// Dark color scheme
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF003738),
    secondaryContainer = Color(0xFF004F50),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10131C),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF10131C),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000)
)