package com.davidlg.iptvplayer.ui.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.davidlg.iptvplayer.ui.Sans
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* Variables CSS de #tvClassic */
internal val ClassicAcc = Color(0xFF56AAFF)      // --tc-acc: 86,170,255
internal val ClassicOnAcc = Color(0xFF04121F)    // --tc-onacc
internal val ClassicText = Color(0xFFF1F5FA)     // --tc-text
internal val ClassicDim = Color(0xFF8DA2B8)      // --tc-dim

/** Paleta PAL de classic-tv.js (color de fondo de miniaturas sin imagen). */
private val ClassicPal = listOf(
    Color(0xFFE63946), Color(0xFF2A9D8F), Color(0xFFF4A261), Color(0xFF6A4C93),
    Color(0xFF1982C4), Color(0xFF8AC926), Color(0xFFFF595E), Color(0xFF3D5A80),
)

/** hue(s) de classic-tv.js: hash h=(h*31+c)>>>0, PAL[h % 8]. */
internal fun classicHue(s: String): Color {
    var h = 0L
    for (ch in s) h = (h * 31L + ch.code.toLong()) and 0xFFFFFFFFL
    return ClassicPal[(h % ClassicPal.size).toInt()]
}

internal fun classicPad(n: Int): String = n.toString().padStart(3, '0')

/** Formato horario compartido por reloj, EPG y listas del modo clásico. */
internal object ClassicTimePrefs { @Volatile var use24h: Boolean = false }
internal fun classicHm(ms: Long): String =
    if (ms <= 0L) "--:--" else SimpleDateFormat(if (ClassicTimePrefs.use24h) "HH:mm" else "h:mm a", Locale("es", "MX")).format(Date(ms))

internal fun classicProgress(start: Long, stop: Long): Float {
    val d = maxOf(1L, stop - start)
    return ((System.currentTimeMillis() - start).toFloat() / d).coerceIn(0f, 1f)
}

/** Equivalente de la lista virtual: mantiene visible la fila seleccionada. */
@Composable
internal fun ClassicEnsureVisible(state: LazyListState, idx: Int) {
    LaunchedEffect(idx) {
        val info = state.layoutInfo
        val vis = info.visibleItemsInfo
        if (vis.isEmpty()) {
            state.scrollToItem(idx.coerceAtLeast(0))
            return@LaunchedEffect
        }
        val first = vis.first().index
        val lastItem = vis.last()
        val lastFull = if (lastItem.offset + lastItem.size > info.viewportEndOffset) lastItem.index - 1 else lastItem.index
        if (idx < first) state.scrollToItem(idx.coerceAtLeast(0))
        else if (idx > lastFull) state.scrollToItem((idx - (lastFull - first)).coerceAtLeast(0))
    }
}

/** .tcChL: logo contenido o inicial del canal sobre fondo blanco 6 %. */
@Composable
internal fun ClassicLogo(logo: String, name: String, w: Dp, h: Dp, fontSize: Float, modifier: Modifier = Modifier) {
    Box(
        modifier.width(w).height(h).clip(RoundedCornerShape(10.dp)).background(Color(0x0FFFFFFF)),
        contentAlignment = Alignment.Center,
    ) {
        if (logo.isNotBlank()) {
            AsyncImage(model = logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                name.trim().take(1).uppercase().ifEmpty { "?" },
                color = Color(0xB3FFFFFF), fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Sans,
            )
        }
    }
}

/** Barra de progreso fina (.tcProg / .tcChP). */
@Composable
internal fun ClassicBar(fraction: Float, height: Dp, modifier: Modifier = Modifier) {
    Box(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(Color(0x24FFFFFF))) {
        Box(Modifier.fillMaxSize().padding(0.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(Modifier)
                    .clip(RoundedCornerShape(height / 2))
                    .background(Color.Transparent)
            )
        }
        Box(
            Modifier
                .height(height)
                .then(if (fraction > 0f) Modifier.fillFraction(fraction) else Modifier.width(0.dp))
                .background(ClassicAcc)
        )
    }
}

private fun Modifier.fillFraction(f: Float): Modifier = this.then(fillMaxWidth(f))
