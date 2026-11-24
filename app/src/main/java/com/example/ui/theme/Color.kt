package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Nabd TV Dark Neon Palette (aligned with nabd_tv_style.png)
val NabdBg = Color(0xFF090C1A)
val NabdSurface = Color(0xFF141A30)
val NabdSurfaceElevated = Color(0xFF1C2340)
val NabdBorder = Color(0xFF252E52)

val NabdCyan = Color(0xFF00E5FF)
val NabdViolet = Color(0xFF7C4DFF)
val NabdPink = Color(0xFFFF4081)
val NabdEmerald = Color(0xFF00E676)
val NabdAmber = Color(0xFFFFB300)
val NabdRed = Color(0xFFFF5252)

val NabdTextPrimary = Color(0xFFFFFFFF)
val NabdTextSecondary = Color(0xFF94A3B8)
val NabdTextMuted = Color(0xFF64748B)

// Signature Gradients
val CyanVioletBrush = Brush.linearGradient(
    colors = listOf(NabdCyan, NabdViolet)
)

val VioletPinkBrush = Brush.linearGradient(
    colors = listOf(NabdViolet, NabdPink)
)

val CyanPinkHeroBrush = Brush.linearGradient(
    colors = listOf(NabdViolet, Color(0xFFB338C8), NabdPink)
)

val DarkCardScrimBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0xCC090C1A), Color(0xF5090C1A))
)
