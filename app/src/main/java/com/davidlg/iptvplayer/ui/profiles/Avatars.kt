package com.davidlg.iptvplayer.ui.profiles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** Diseño de un avatar: degradado a→b y tipo de carita (smile, grin, wink, neutral). Igual que AV en profiles.js. */
private class AvDef(val a: Color, val b: Color, val face: String)

private val AV_DEFS = listOf(
    AvDef(Color(0xFFE5484D), Color(0xFF8E1B20), "smile"),
    AvDef(Color(0xFF3B9CFF), Color(0xFF124F94), "grin"),
    AvDef(Color(0xFF2FBF7A), Color(0xFF126B43), "wink"),
    AvDef(Color(0xFFF5A524), Color(0xFFA8600A), "smile"),
    AvDef(Color(0xFFA35CD6), Color(0xFF4D1F6E), "grin"),
    AvDef(Color(0xFFEE4D9B), Color(0xFF8D1558), "wink"),
    AvDef(Color(0xFF1AC0D6), Color(0xFF0A5C6B), "smile"),
    AvDef(Color(0xFF7B8A96), Color(0xFF2F3A42), "neutral"),
)

/** Número de avatares disponibles. */
internal const val PROFILE_AVATAR_COUNT = 8

private val AV_DARK = Color(0xFF14171C)

/** Avatar de perfil (índice 0..7, 8 diseños de caritas con degradado, igual que profiles.js). Lo usan la barra superior y la pantalla de perfiles. */
@Composable
fun ProfileAvatar(av: Int, size: Dp, modifier: Modifier = Modifier) {
    val d = AV_DEFS[((av % AV_DEFS.size) + AV_DEFS.size) % AV_DEFS.size]
    Canvas(modifier.size(size)) {
        // viewBox 0 0 100 100
        val u = this.size.width / 100f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(d.a, d.b),
                start = Offset(0f, 0f),
                end = Offset(this.size.width, this.size.height),
            ),
        )
        // ojos
        drawCircle(AV_DARK, radius = 5.2f * u, center = Offset(36f * u, 42f * u))
        if (d.face == "wink") {
            val p = Path().apply {
                moveTo(58f * u, 43f * u)
                quadraticBezierTo(64.5f * u, 35f * u, 71f * u, 43f * u)
            }
            drawPath(p, AV_DARK, style = Stroke(width = 4.6f * u, cap = StrokeCap.Round))
        } else {
            drawCircle(AV_DARK, radius = 5.2f * u, center = Offset(64f * u, 42f * u))
        }
        // boca
        when (d.face) {
            "grin" -> avGrin(u)
            "neutral" -> {
                val p = Path().apply {
                    moveTo(33f * u, 68f * u)
                    lineTo(67f * u, 68f * u)
                }
                drawPath(p, AV_DARK, style = Stroke(width = 5f * u, cap = StrokeCap.Round))
            }
            else -> {
                val p = Path().apply {
                    moveTo(29f * u, 61f * u)
                    quadraticBezierTo(50f * u, 83f * u, 71f * u, 61f * u)
                }
                drawPath(p, AV_DARK, style = Stroke(width = 5.2f * u, cap = StrokeCap.Round))
            }
        }
    }
}

private fun DrawScope.avGrin(u: Float) {
    val fill = Path().apply {
        moveTo(27f * u, 59f * u)
        quadraticBezierTo(50f * u, 92f * u, 73f * u, 59f * u)
        close()
    }
    drawPath(fill, AV_DARK)
    val teeth = Path().apply {
        moveTo(33f * u, 61f * u)
        quadraticBezierTo(50f * u, 70f * u, 67f * u, 61f * u)
    }
    drawPath(teeth, Color.White, style = Stroke(width = 4f * u, cap = StrokeCap.Round))
}
