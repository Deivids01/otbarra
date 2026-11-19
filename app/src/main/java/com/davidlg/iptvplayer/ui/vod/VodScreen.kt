package com.davidlg.iptvplayer.ui.vod

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.OttTab
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.data.sourceBackdrop
import com.davidlg.iptvplayer.data.sourceDuration
import com.davidlg.iptvplayer.data.sourceGenre
import com.davidlg.iptvplayer.data.sourcePoster
import com.davidlg.iptvplayer.data.sourceRating
import com.davidlg.iptvplayer.data.sourceSynopsis
import com.davidlg.iptvplayer.data.sourceYear
import com.davidlg.iptvplayer.ui.Poppins
import com.davidlg.iptvplayer.ui.Sans
import com.davidlg.iptvplayer.ui.dlClickable
import org.json.JSONArray

private val AccentBlue = Color(0xFF168BFF)
private val DeepPanel = Color(0xFF0D1929)
private val SERIES_RX = Regex("\\bs\\d{1,2}\\s?e\\d{1,3}\\b|temporada|season|episodio", RegexOption.IGNORE_CASE)

internal fun vodKind(c: Channel): String {
    val g = c.group.lowercase()
    return when {
        g.contains("anime") -> "anime"
        g.contains("serie") || SERIES_RX.containsMatchIn(c.name) || c.url.contains("/series/") -> "series"
        else -> "movie"
    }
}

private fun readFavs(vm: MainViewModel): Set<String> = try {
    val a = JSONArray(vm.prefs.getString("davidlgVodFavorites", "[]"))
    (0 until a.length()).map { a.getString(it) }.toSet()
} catch (_: Exception) { emptySet() }

private fun writeFavs(vm: MainViewModel, s: Set<String>) = vm.prefs.putString("davidlgVodFavorites", JSONArray(s.toList()).toString())
private fun localMeta(vm: MainViewModel, c: Channel, key: String, source: String): String =
    vm.vodMetadata(c.id).optString(key, "").takeIf { it.isNotBlank() } ?: source

/** Catálogo VOD con portada editorial, carruseles y ficha completa editable. */
@Composable
fun VodScreen(vm: MainViewModel, tab: OttTab, classic: Boolean = false, onHome: () -> Unit) {
    val all by vm.vod.collectAsState()
    val metadataVersion = vm.vodMetadataVersion
    var favs by remember { mutableStateOf(readFavs(vm)) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Channel?>(null) }
    var editing by remember { mutableStateOf<Channel?>(null) }
    val width = LocalConfiguration.current.screenWidthDp
    val phone = width < 600
    val tablet = width in 600..899
    val compact = phone || tablet
    val accent = if (classic) Color(0xFF4AA3FF) else AccentBlue
    val font: FontFamily = if (classic) Sans else Poppins
    BackHandler(selected != null || editing != null) {
        if (editing != null) editing = null else selected = null
    }

    val filtered = remember(all, tab, query, favs) {
        all.filter { c ->
            when (tab) {
                OttTab.Movie -> vodKind(c) == "movie"
                OttTab.Series -> vodKind(c) == "series"
                OttTab.Anime -> vodKind(c) == "anime"
                else -> c.id in favs
            }
        }.filter { query.isBlank() || it.name.contains(query, true) || it.group.contains(query, true) }
    }
    val hero = filtered.firstOrNull()
    val rows = remember(filtered) { filtered.groupBy { it.group.ifBlank { "Para descubrir" } }.toList() }
    val continueList = remember(all, metadataVersion) { all.filter { vm.vodProgress(it.id) > 10_000L } }
    LaunchedEffect(all, vm.pendingVodDetailsId) {
        val pending = vm.pendingVodDetailsId
        if (pending != null) {
            selected = all.firstOrNull { it.id == pending }
            vm.consumePendingVodDetails()
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF070D18))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = if (phone) 16.dp else if (tablet) 24.dp else 64.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (tab == OttTab.Fav) "TU COLECCIÓN" else "CATÁLOGO", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(when (tab) { OttTab.Movie -> "Películas"; OttTab.Series -> "Series"; OttTab.Anime -> "Anime"; else -> "Mi lista" }, color = Color.White, fontSize = if (phone) 22.sp else 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
            }
            SearchBox(query, phone, accent, font) { query = it }
            if (!phone) ActionChip("Inicio", accent, font, onHome)
        }

        if (hero == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(if (tab == OttTab.Fav) "Tu lista está esperando" else "Aún no hay títulos aquí", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = font)
                    Text(if (tab == OttTab.Fav) "Agrega películas y series para tenerlas a mano." else "Comprueba tus listas o importa un catálogo VOD.", color = Color(0xFF8FA4BB), fontSize = 12.sp)
                    ActionChip("Volver al inicio", accent, font, onHome)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(if (phone) 22.dp else 28.dp)) {
                item {
                    CatalogHero(vm, hero, phone, tablet, accent, font, metadataVersion,
                        onPlay = { vm.play(hero, filtered) },
                        onDetails = { selected = hero },
                    )
                }
                if (continueList.isNotEmpty() && tab != OttTab.Fav) {
                    item { PosterRail("Continuar viendo", continueList, phone, tablet, accent, font, vm, metadataVersion) { c -> vm.play(c, all, vm.vodProgress(c.id)) } }
                }
                items(rows, key = { it.first }) { (group, titles) ->
                    PosterRail(group, titles, phone, tablet, accent, font, vm, metadataVersion) { selected = it }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    selected?.let { c ->
        TitleDetails(
            vm, c, phone, tablet, accent, font, favs, metadataVersion,
            onPlay = { vm.play(c, all, 0L); selected = null },
            onContinue = { vm.play(c, all, vm.vodProgress(c.id)); selected = null },
            onFavorite = { favs = if (c.id in favs) favs - c.id else favs + c.id; writeFavs(vm, favs) },
            onEdit = { editing = c },
            onClose = { selected = null },
        )
    }

    editing?.let { c ->
        MetadataEditor(vm, c, metadataVersion, onDismiss = { editing = null })
    }
}

@Composable
private fun SearchBox(value: String, phone: Boolean, accent: Color, font: FontFamily, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.width(if (phone) 132.dp else 240.dp).height(if (phone) 40.dp else 46.dp)
        .clip(RoundedCornerShape(11.dp)).background(Color(0xFF101C2C))
        .border(if (focused) 1.5.dp else 1.dp, if (focused) accent else Color(0x263F9FFF), RoundedCornerShape(11.dp))
        .padding(horizontal = 11.dp), contentAlignment = Alignment.CenterStart) {
        BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(color = Color.White, fontSize = if (phone) 10.sp else 13.sp, fontFamily = font), cursorBrush = SolidColor(accent), modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused })
        if (value.isEmpty()) Text("⌕  Buscar", color = Color(0xFF71869C), fontSize = if (phone) 10.sp else 13.sp, fontFamily = font)
    }
}

@Composable
private fun CatalogHero(vm: MainViewModel, c: Channel, phone: Boolean, tablet: Boolean, accent: Color, font: FontFamily, metadataVersion: Int, onPlay: () -> Unit, onDetails: () -> Unit) {
    val backdrop = localMeta(vm, c, "backdrop", c.sourceBackdrop())
    val poster = localMeta(vm, c, "poster", c.sourcePoster().ifBlank { c.logo })
    val synopsis = localMeta(vm, c, "synopsis", c.sourceSynopsis())
    val year = localMeta(vm, c, "year", c.sourceYear())
    val genre = localMeta(vm, c, "genre", c.sourceGenre())
    val height = if (phone) 350.dp else if (tablet) 380.dp else 450.dp
    Box(Modifier.fillMaxWidth().padding(horizontal = if (phone) 12.dp else if (tablet) 20.dp else 56.dp)
        .height(height).clip(RoundedCornerShape(if (phone) 20.dp else 26.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF18395B), Color(0xFF07111F))))
        .border(1.dp, Color(0x333F9FFF), RoundedCornerShape(if (phone) 20.dp else 26.dp))) {
        val image = backdrop.ifBlank { poster }
        if (image.isNotBlank()) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0xF507101D), .55f to Color(0xA807101D), 1f to Color(0x1A07101D))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, .42f to Color(0x18000000), 1f to Color(0xE607101D))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(if (phone) 1f else .77f)
            .padding(start = if (phone) 17.dp else 34.dp, end = 18.dp, bottom = if (phone) 18.dp else 30.dp), verticalArrangement = Arrangement.spacedBy(if (phone) 7.dp else 10.dp)) {
            Text("SELECCIÓN EDITORIAL", color = Color(0xFF8CCBFF), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text(c.name, color = Color.White, fontSize = if (phone) 25.sp else if (tablet) 32.sp else 44.sp, lineHeight = if (phone) 29.sp else 48.sp, fontWeight = FontWeight.ExtraBold, fontFamily = font, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOf(year, genre.ifBlank { c.group }).filter { it.isNotBlank() }.distinct().joinToString("  •  ")
            if (meta.isNotBlank()) Text(meta, color = Color(0xFFD5E5F5), fontSize = if (phone) 10.sp else 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(synopsis.ifBlank { "Consulta la ficha, completa la información del título y organiza tu propia colección." }, color = Color(0xFFE2EAF3), fontSize = if (phone) 10.sp else 13.sp, lineHeight = if (phone) 15.sp else 19.sp, maxLines = if (phone) 3 else 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip("▶  Reproducir", accent, font, onPlay, true)
                ActionChip("ⓘ  Detalles", accent, font, onDetails)
            }
        }
    }
}

@Composable
private fun PosterRail(title: String, list: List<Channel>, phone: Boolean, tablet: Boolean, accent: Color, font: FontFamily, vm: MainViewModel, metadataVersion: Int, onSelect: (Channel) -> Unit) {
    Column {
        Text(title, color = Color.White, fontSize = if (phone) 17.sp else 23.sp, fontWeight = FontWeight.Bold, fontFamily = font,
            modifier = Modifier.padding(horizontal = if (phone) 16.dp else if (tablet) 24.dp else 64.dp, vertical = 7.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = if (phone) 16.dp else if (tablet) 24.dp else 64.dp), horizontalArrangement = Arrangement.spacedBy(if (phone) 10.dp else 15.dp)) {
            items(list.take(100), key = { it.id }) { c ->
                val poster = localMeta(vm, c, "poster", c.sourcePoster().ifBlank { c.logo })
                Column(Modifier.width(if (phone) 112.dp else if (tablet) 138.dp else 174.dp)
                    .dlClickable(RoundedCornerShape(11.dp), accent, 2.dp, 1.04f, null) { onSelect(c) }) {
                    Box(Modifier.fillMaxWidth().height(if (phone) 164.dp else if (tablet) 198.dp else 252.dp)
                        .clip(RoundedCornerShape(11.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1B3551), Color(0xFF0A1524))))) {
                        if (poster.isNotBlank()) AsyncImage(poster, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(c.name.split(" ").take(2).joinToString("") { it.take(1) }.uppercase(), color = Color(0xFF8CCBFF), fontSize = 30.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(c.name, color = Color.White, fontSize = if (phone) 10.sp else 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun TitleDetails(vm: MainViewModel, c: Channel, phone: Boolean, tablet: Boolean, accent: Color, font: FontFamily, favs: Set<String>, metadataVersion: Int, onPlay: () -> Unit, onContinue: () -> Unit, onFavorite: () -> Unit, onEdit: () -> Unit, onClose: () -> Unit) {
    val poster = localMeta(vm, c, "poster", c.sourcePoster().ifBlank { c.logo })
    val backdrop = localMeta(vm, c, "backdrop", c.sourceBackdrop().ifBlank { poster })
    val synopsis = localMeta(vm, c, "synopsis", c.sourceSynopsis())
    val year = localMeta(vm, c, "year", c.sourceYear())
    val genre = localMeta(vm, c, "genre", c.sourceGenre())
    val duration = localMeta(vm, c, "duration", c.sourceDuration())
    val rating = localMeta(vm, c, "rating", c.sourceRating())
    val progress = vm.vodProgress(c.id)
    Box(Modifier.fillMaxSize().background(Color(0xF5070D18)), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxSize().background(Color(0xFF091321))) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (backdrop.isNotBlank()) AsyncImage(backdrop, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x44000000), Color(0xF5091321)))))
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(if (phone) 16.dp else 38.dp), horizontalArrangement = Arrangement.spacedBy(if (phone) 14.dp else 28.dp), verticalAlignment = Alignment.Bottom) {
                    if (!phone && poster.isNotBlank()) AsyncImage(poster, null, Modifier.width(if (tablet) 126.dp else 190.dp).height(if (tablet) 190.dp else 280.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (phone) 8.dp else 12.dp)) {
                        Text(vodKind(c).uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                        Text(c.name, color = Color.White, fontSize = if (phone) 24.sp else if (tablet) 32.sp else 46.sp, fontWeight = FontWeight.ExtraBold, fontFamily = font, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        val meta = listOf(year, genre, duration, if (rating.isNotBlank()) "★ $rating" else "").filter { it.isNotBlank() }.joinToString("  •  ")
                        if (meta.isNotBlank()) Text(meta, color = Color(0xFFB8C9DA), fontSize = if (phone) 10.sp else 13.sp)
                        Text(synopsis.ifBlank { "Sin sinopsis guardada. Puedes añadir una descripción desde Editar información." }, color = Color(0xFFE2EAF3), fontSize = if (phone) 11.sp else 14.sp, lineHeight = if (phone) 16.sp else 21.sp, maxLines = if (phone) 5 else 6, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            ActionChip("▶ Reproducir", accent, font, onPlay, true)
                            if (progress > 10_000L) ActionChip("Continuar", accent, font, onContinue)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = if (phone) 14.dp else 34.dp, vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip(if (c.id in favs) "✓ Mi lista" else "+ Mi lista", accent, font, onFavorite)
                ActionChip("Editar información", accent, font, onEdit)
                ActionChip("Cerrar", accent, font, onClose)
            }
        }
    }
}

@Composable
private fun MetadataEditor(vm: MainViewModel, c: Channel, metadataVersion: Int, onDismiss: () -> Unit) {
    val saved = vm.vodMetadata(c.id)
    val tmdbStatus by vm.tmdbLookupStatus.collectAsState()
    var synopsis by remember(c.id, metadataVersion) { mutableStateOf(saved.optString("synopsis", c.sourceSynopsis())) }
    var poster by remember(c.id, metadataVersion) { mutableStateOf(saved.optString("poster", c.sourcePoster().ifBlank { c.logo })) }
    var backdrop by remember(c.id, metadataVersion) { mutableStateOf(saved.optString("backdrop", c.sourceBackdrop())) }
    var year by remember(c.id, metadataVersion) { mutableStateOf(saved.optString("year", c.sourceYear())) }
    var genre by remember(c.id, metadataVersion) { mutableStateOf(saved.optString("genre", c.sourceGenre())) }
    var duration by remember(c.id, metadataVersion) { mutableStateOf(saved.optString("duration", c.sourceDuration())) }
    var rating by remember(c.id, metadataVersion) { mutableStateOf(saved.optString("rating", c.sourceRating())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101D2D),
        title = { Text("Editar ficha · ${c.name}", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.height(430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Esta información se guarda en DavidLG IPTV y no depende de tu lista M3U.", color = Color(0xFF9DB2C8), fontSize = 11.sp)
                TextButton(onClick = { vm.lookupVodOnTmdb(c) }) { Text("Buscar y completar automáticamente con TMDB", color = AccentBlue) }
                if (tmdbStatus.isNotBlank()) Text(tmdbStatus, color = Color(0xFF9DB2C8), fontSize = 10.sp)
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(synopsis, { synopsis = it }, label = { Text("Sinopsis") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(poster, { poster = it }, label = { Text("URL del póster") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(backdrop, { backdrop = it }, label = { Text("URL de imagen de fondo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(year, { year = it }, label = { Text("Año") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(genre, { genre = it }, label = { Text("Género") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(duration, { duration = it }, label = { Text("Duración") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(rating, { rating = it }, label = { Text("Calificación") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.saveVodMetadata(c.id, mapOf("synopsis" to synopsis, "poster" to poster, "backdrop" to backdrop, "year" to year, "genre" to genre, "duration" to duration, "rating" to rating))
                onDismiss()
            }) { Text("Guardar", color = AccentBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFFB8C9DA)) } },
    )
}

@Composable
private fun ActionChip(label: String, accent: Color, font: FontFamily, onClick: () -> Unit, primary: Boolean = false) {
    val shape = RoundedCornerShape(9.dp)
    Text(label, color = if (primary) Color(0xFF06111F) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = font,
        modifier = Modifier.clip(shape).background(if (primary) Color.White else Color(0x44263C54)).border(1.dp, if (primary) Color.White else Color(0x553F9FFF), shape)
            .dlClickable(shape, accent, 2.dp, 1.04f, null, onClick).padding(horizontal = 12.dp, vertical = 9.dp))
}
