package com.davidlg.iptvplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.davidlg.iptvplayer.R

/**
 * Lienzo adaptativo: Android TV conserva el lienzo 16:9; móviles y tablets usan
 * las dimensiones reales en dp y ocupan toda la pantalla.
 */
@Composable
fun DesignCanvas(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val ratio = if (maxHeight > 0.dp) maxWidth / maxHeight else 1f
        val useNativeLayout = maxWidth < 900.dp || ratio < 1.20f
        if (useNativeLayout) {
            // Mantener la densidad nativa activa los breakpoints compactos de cada pantalla.
            Box(Modifier.fillMaxSize()) { content() }
        } else {
            val scale = minOf(constraints.maxWidth / 1920f, constraints.maxHeight / 1080f)
            CompositionLocalProvider(LocalDensity provides Density(density = scale, fontScale = 1f)) {
                Box(Modifier.size(1920.dp, 1080.dp), contentAlignment = Alignment.Center) { content() }
            }
        }
    }
}

/** Poppins (títulos y interfaz OTT). */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_bold, FontWeight.ExtraBold),
    Font(R.font.poppins_bold, FontWeight.Black),
)

/** Arial/Helvetica de la web (ajustes, clásico, reproductor) → sans-serif del sistema. */
val Sans: FontFamily = FontFamily.SansSerif

/** Fondo base de la web (#020711). Cada pantalla define sus propios degradados según su CSS. */
val InkBg = Color(0xFF020711)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = InkBg, surface = InkBg, onBackground = Color(0xFFF5F8FC), onSurface = Color(0xFFF5F8FC)),
        content = content,
    )
}

/**
 * Elemento enfocable/táctil. Al enfocar con el control remoto: zoom + borde. Úsalo como base de botones y tarjetas.
 * (La app web usa outline:3px solid #fff en :focus; ajusta focusColor/ focusWidth según el CSS de cada pieza.)
 */
fun Modifier.dlClickable(
    shape: Shape = RoundedCornerShape(16.dp),
    focusColor: Color = Color.White,
    focusWidth: Dp = 3.dp,
    focusScale: Float = 1f,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (focused) focusScale else 1f, label = "dlFocusScale")
    this
        .scale(sc)
        .then(if (focused && focusWidth > 0.dp) Modifier.border(focusWidth, focusColor, shape) else Modifier)
        .clip(shape)
        .onFocusChanged { focused = it.isFocused; onFocusChange?.invoke(it.isFocused) }
        .clickable(onClick = onClick)
}
