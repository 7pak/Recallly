package com.at.recallly.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand Colors
val DeepSlate = Color(0xFF1E293B)
val ElectricMint = Color(0xFF10B981)
val SkyAccent = Color(0xFF0EA5E9)

// Neutrals
val SlateWhite = Color(0xFFF8FAFC)
val Slate50 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate700 = Color(0xFF334155)
val Slate900 = Color(0xFF0F172A)

// Semantic
val MintLight = Color(0xFFD1FAE5)
val MintDark = Color(0xFF064E3B)
val ErrorRed = Color(0xFFEF4444)
val ErrorRedDark = Color(0xFFFCA5A5)

val LightColorScheme = lightColorScheme(
    primary = DeepSlate,
    onPrimary = Color.White,
    primaryContainer = Slate200,
    onPrimaryContainer = DeepSlate,
    secondary = ElectricMint,
    onSecondary = Color.White,
    secondaryContainer = MintLight,
    onSecondaryContainer = MintDark,
    tertiary = SkyAccent,
    onTertiary = Color.White,
    background = SlateWhite,
    onBackground = DeepSlate,
    surface = Color.White,
    onSurface = DeepSlate,
    surfaceVariant = Slate50,
    onSurfaceVariant = Slate500,
    outline = Slate400,
    outlineVariant = Slate300,
    error = ErrorRed,
    onError = Color.White,
)

val DarkColorScheme = darkColorScheme(
    primary = Slate200,
    onPrimary = Slate900,
    primaryContainer = Slate700,
    onPrimaryContainer = Slate200,
    secondary = ElectricMint,
    onSecondary = Color.White,
    secondaryContainer = MintDark,
    onSecondaryContainer = MintLight,
    tertiary = SkyAccent,
    onTertiary = Color.White,
    background = Slate900,
    onBackground = Slate200,
    surface = DeepSlate,
    onSurface = Slate200,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    outline = Slate500,
    outlineVariant = Slate700,
    error = ErrorRedDark,
    onError = Slate900,
)
