package com.davidlg.iptvplayer.ui.ott

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidlg.iptvplayer.ui.Poppins
import com.davidlg.iptvplayer.ui.dlClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* Paleta del modo OTT (ott-premium/ott-final) */
internal val OttGold = Color(0xFF168BFF)        // acento azul eléctrico unificado
internal val OttGoldDim = Color(0xFF58B7FF)     // azul claro para etiquetas
internal val OttGoldSoft = Color(0xFF79C7FF)
internal val OttCream = Color(0xFFEAF6FF)
internal val OttBg = Color(0xFF070D18)
internal val OttText = Color(0xFFEAF2FF)
internal val ES = Locale("es", "MX")

internal fun esDate(pattern: String): String = SimpleDateFormat(pattern, ES).format(Date())
internal fun pad3(n: Int) = n.toString().padStart(3, '0')
internal fun fmtAmPm(ms: Long): String =
    if (ms <= 0) "--:--" else SimpleDateFormat("h:mm a", Locale.US).format(Date(ms)).uppercase()

/* ---------- Iconos SVG (viewBox 24, trazo) tomados de index.html ---------- */
internal fun rectPath(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 -$r ${r}h-${w - 2 * r}a$r $r 0 0 1 -$r -${r}v-${h - 2 * r}a$r $r 0 0 1 $r -${r}z"
internal fun circlePath(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 -${2 * r} 0"

internal object Ico {
    val home = listOf("M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
    val movie = listOf(rectPath(3f, 8f, 18f, 12f, 2f), "M3 8l3-5h4L7 8M11 8l3-5h4l-3 5")
    val series = listOf(rectPath(3f, 6f, 18f, 13f, 2f), "M8 2l4 4 4-4")
    val anime = listOf(rectPath(2f, 7f, 20f, 11f, 5f), "M7 10v5M4.5 12.5h5", circlePath(16f, 11f, 1f), circlePath(18.5f, 14f, 1f))
    val live = listOf(circlePath(12f, 12f, 2f), "M7.8 7.8a6 6 0 0 0 0 8.4M16.2 7.8a6 6 0 0 1 0 8.4M4.9 4.9a10 10 0 0 0 0 14.2M19.1 4.9a10 10 0 0 1 0 14.2")
    val fav = listOf("M6 3h12v18l-6-4-6 4z")
    val tv = listOf(rectPath(3f, 5f, 18f, 12f, 2f), "M8 21h8M12 17v4")
    val play = listOf(rectPath(3f, 4f, 18f, 16f, 2.5f), "M10 9l5 3-5 3z")
    val gear = listOf(
        circlePath(12f, 12f, 3.2f),
        "M19.4 13.5a7.7 7.7 0 0 0 0-3l1.9-1.5-1.9-3.3-2.3.9a7.6 7.6 0 0 0-2.6-1.5L14.2 2.6h-4.4l-.4 2.5a7.6 7.6 0 0 0-2.6 1.5l-2.3-.9-1.9 3.3 1.9 1.5a7.7 7.7 0 0 0 0 3l-1.9 1.5 1.9 3.3 2.3-.9a7.6 7.6 0 0 0 2.6 1.5l.4 2.5h4.4l.4-2.5a7.6 7.6 0 0 0 2.6-1.5l2.3.9 1.9-3.3z",
    )
}

@Composable
internal fun SvgIcon(paths: List<String>, size: Dp, color: Color, stroke: Float = 1.8f, modifier: Modifier = Modifier) {
    val parsed = remember(paths) { paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            for (p in parsed) drawPath(p, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Botón dorado en píldora (.ottLiveBtn / #continueBtn). */
@Composable
internal fun GoldPill(
    text: String, fontSize: Int, padH: Int, padV: Int, modifier: Modifier = Modifier,
    spacing: Float = 0f, onClick: () -> Unit,
) {
    var foc by remember { mutableStateOf(false) }
    Box(
        modifier
            .dlClickable(RoundedCornerShape(50), Color.White, 3.dp, 1.04f, { foc = it }, onClick)
            .background(Brush.verticalGradient(listOf(OttCream, Color(0xFF168BFF))))
            .padding(horizontal = padH.dp, vertical = padV.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = Color(0xFF06111F), fontSize = fontSize.sp, fontWeight = FontWeight.Bold,
            fontFamily = Poppins, letterSpacing = spacing.sp, maxLines = 1,
        )
    }
}

/** Fondo/tarjeta de canal sin logo: texto "TV" o iniciales. */
internal fun initials(name: String): String = "TV"
