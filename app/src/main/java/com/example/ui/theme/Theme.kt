package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PharmaPrimaryDark,
    onPrimary = PharmaOnPrimaryDark,
    primaryContainer = PharmaPrimaryContainerDark,
    onPrimaryContainer = PharmaOnPrimaryContainerDark,
    secondary = PharmaSecondaryDark,
    onSecondary = PharmaOnSecondaryDark,
    secondaryContainer = PharmaSecondaryContainerDark,
    onSecondaryContainer = PharmaOnSecondaryContainerDark,
    tertiary = PharmaTertiaryDark,
    onTertiary = PharmaOnTertiaryDark,
    tertiaryContainer = PharmaTertiaryContainerDark,
    onTertiaryContainer = PharmaOnTertiaryContainerDark,
    background = PharmaBackgroundDark,
    onBackground = PharmaOnBackgroundDark,
    surface = PharmaSurfaceDark,
    onSurface = PharmaOnSurfaceDark,
    surfaceVariant = PharmaSurfaceVariantDark,
    onSurfaceVariant = PharmaOnSurfaceVariantDark,
    outline = PharmaOutlineDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PharmaPrimaryLight,
    onPrimary = PharmaOnPrimaryLight,
    primaryContainer = PharmaPrimaryContainerLight,
    onPrimaryContainer = PharmaOnPrimaryContainerLight,
    secondary = PharmaSecondaryLight,
    onSecondary = PharmaOnSecondaryLight,
    secondaryContainer = PharmaSecondaryContainerLight,
    onSecondaryContainer = PharmaOnSecondaryContainerLight,
    tertiary = PharmaTertiaryLight,
    onTertiary = PharmaOnTertiaryLight,
    tertiaryContainer = PharmaTertiaryContainerLight,
    onTertiaryContainer = PharmaOnTertiaryContainerLight,
    background = PharmaBackgroundLight,
    onBackground = PharmaOnBackgroundLight,
    surface = PharmaSurfaceLight,
    onSurface = PharmaOnSurfaceLight,
    surfaceVariant = PharmaSurfaceVariantLight,
    onSurfaceVariant = PharmaOnSurfaceVariantLight,
    outline = PharmaOutlineLight,
  )

@Composable
fun PharmaFlowTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
