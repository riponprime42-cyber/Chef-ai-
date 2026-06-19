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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),       // Sleek Indigo 400
    secondary = Color(0xFFC084FC),     // Sleek Purple 400
    tertiary = Color(0xFFF472B6),      // Pink 400 (Rating/Accents)
    background = Color(0xFF0B1220),    // Deep dark slate blue-black
    surface = Color(0xFF162032),       // Card surface
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onTertiary = Color.White,
    onBackground = Color(0xFFE2E8F0),  // Slate 200
    onSurface = Color(0xFFF1F5F9)      // Slate 100
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),      // Indigo 600
    secondary = Color(0xFF7C3AED),    // Purple 600
    tertiary = Color(0xFFEC4899),     // Pink 500 for secondary action highlight
    background = Color(0xFFFDF7FF),   // Soft Sleek Lavender background
    surface = Color(0xFFFFFFFF),      // Clean white inside slate shadow style
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),  // Slate 900
    onSurface = Color(0xFF1E293B)      // Slate 800
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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
