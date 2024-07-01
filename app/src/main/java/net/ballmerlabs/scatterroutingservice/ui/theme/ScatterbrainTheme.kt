package net.ballmerlabs.scatterroutingservice.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat



private val DarkColorSchemeOld = darkColorScheme(
    primary = Purple80,
    secondary = cyanSecondary,
    tertiary = cyanSecondary,
)

private val DarkColorScheme = lightColorScheme(
    primary = brightTeal,
    secondary = brightAntiTeal,
    tertiary = brightGreen,
    errorContainer = brightAntiTeal,
    error = brightAntiTeal,
    secondaryContainer = brightGreen,
    surfaceContainer = surfaceBlue,
    surface = black,
    onSecondary = Color.White,
    surfaceVariant = surfaceGreen,
    surfaceTint = brightGreen,
    surfaceDim = black,
    surfaceContainerLow = dimTeal,
    surfaceContainerHigh = surfaceTeal,
    surfaceContainerHighest = brightTeal,
    surfaceBright = surfaceGreen,
    surfaceContainerLowest = dimTeal,
    background = black,
    onBackground = white,
    onSurface = black
)

/*
private val LightColorScheme = lightColorScheme(
    primary = greyPrimary,
    secondary = cyanSecondary,
    tertiary = lightTertiary,
    tertiaryContainer = lightTertiary
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

 */

//attempt4
private val LightColorScheme = lightColorScheme(
    primary = brightTeal,
    secondary = brightBlue,
    tertiary = brightGreen,
    errorContainer = brightAntiTeal,
    error = brightAntiTeal,
    secondaryContainer = brightGreen,
    surfaceContainer = surfaceBlue,
    surface = dimBlue,
    onSecondary = Color.White,
    surfaceVariant = surfaceGreen,
    surfaceTint = brightGreen,
    surfaceDim = dimBlue,
    surfaceContainerLow = dimTeal,
    surfaceContainerHigh = surfaceTeal,
    surfaceContainerHighest = brightTeal,
    surfaceBright = surfaceGreen,
    surfaceContainerLowest = dimTeal,
    background = dimBlue
)

//attempt3
private val LightColorScheme3 = lightColorScheme(
    primary = Color(0xff9C2468),
    secondary = Color(0xff24689C),
    tertiary = Color(0xFF689C24),
    onSecondary = Color.White,
)

// attempt2
private val LightColorScheme2 = lightColorScheme(
    primary = Color(0xFF57A857),
    secondary = Color(0xff5780A8),
    tertiary = Color(0xFFA857A8),
    tertiaryContainer = Color(0xffb7fffd),
    onSecondary = Color.White,
    onSecondaryContainer = Color.White,
)

/* attempt1
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF43BC87),
    secondary = Color(0xff4A43BC),
    tertiary = Color(0xffBC4378),
    tertiaryContainer = Color(0xffb7fffd),
    onSecondary = Color.White,
    onSecondaryContainer = Color.White,
)

 */

@Composable
fun ScatterbrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
        }
    }

    val typography = Typography(
        headlineSmall = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        labelMedium = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )

    val shape = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
        shapes = shape
    )
}