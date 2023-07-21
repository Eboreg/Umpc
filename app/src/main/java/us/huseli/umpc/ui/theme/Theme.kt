package us.huseli.umpc.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = UmpcColorLight.Primary,
    onPrimary = UmpcColorLight.OnPrimary,
    primaryContainer = UmpcColorLight.PrimaryContainer,
    onPrimaryContainer = UmpcColorLight.OnPrimaryContainer,
    secondary = UmpcColorLight.Secondary,
    onSecondary = UmpcColorLight.OnSecondary,
    secondaryContainer = UmpcColorLight.SecondaryContainer,
    onSecondaryContainer = UmpcColorLight.OnSecondaryContainer,
    tertiary = UmpcColorLight.Tertiary,
    onTertiary = UmpcColorLight.OnTertiary,
    tertiaryContainer = UmpcColorLight.TertiaryContainer,
    onTertiaryContainer = UmpcColorLight.OnTertiaryContainer,
    error = UmpcColorLight.Error,
    onError = UmpcColorLight.OnError,
    errorContainer = UmpcColorLight.ErrorContainer,
    onErrorContainer = UmpcColorLight.OnErrorContainer,
    outline = UmpcColorLight.Outline,
    background = UmpcColorLight.Background,
    onBackground = UmpcColorLight.OnBackground,
    surface = UmpcColorLight.Surface,
    onSurface = UmpcColorLight.OnSurface,
    surfaceVariant = UmpcColorLight.SurfaceVariant,
    onSurfaceVariant = UmpcColorLight.OnSurfaceVariant,
    inverseSurface = UmpcColorLight.InverseSurface,
    inverseOnSurface = UmpcColorLight.InverseOnSurface,
    inversePrimary = UmpcColorLight.InversePrimary,
    surfaceTint = UmpcColorLight.SurfaceTint,
    outlineVariant = UmpcColorLight.OutlineVariant,
    scrim = UmpcColorLight.Scrim,
)

private val DarkColors = darkColorScheme(
    primary = UmpcColorDark.Primary,
    onPrimary = UmpcColorDark.OnPrimary,
    primaryContainer = UmpcColorDark.PrimaryContainer,
    onPrimaryContainer = UmpcColorDark.OnPrimaryContainer,
    secondary = UmpcColorDark.Secondary,
    onSecondary = UmpcColorDark.OnSecondary,
    secondaryContainer = UmpcColorDark.SecondaryContainer,
    onSecondaryContainer = UmpcColorDark.OnSecondaryContainer,
    tertiary = UmpcColorDark.Tertiary,
    onTertiary = UmpcColorDark.OnTertiary,
    tertiaryContainer = UmpcColorDark.TertiaryContainer,
    onTertiaryContainer = UmpcColorDark.OnTertiaryContainer,
    error = UmpcColorDark.Error,
    onError = UmpcColorDark.OnError,
    errorContainer = UmpcColorDark.ErrorContainer,
    onErrorContainer = UmpcColorDark.OnErrorContainer,
    outline = UmpcColorDark.Outline,
    background = UmpcColorDark.Background,
    onBackground = UmpcColorDark.OnBackground,
    surface = UmpcColorDark.Surface,
    onSurface = UmpcColorDark.OnSurface,
    surfaceVariant = UmpcColorDark.SurfaceVariant,
    onSurfaceVariant = UmpcColorDark.OnSurfaceVariant,
    inverseSurface = UmpcColorDark.InverseSurface,
    inverseOnSurface = UmpcColorDark.InverseOnSurface,
    inversePrimary = UmpcColorDark.InversePrimary,
    surfaceTint = UmpcColorDark.SurfaceTint,
    outlineVariant = UmpcColorDark.OutlineVariant,
    scrim = UmpcColorDark.Scrim,
)

@Suppress("unused")
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

@Suppress("unused")
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun UmpcTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
