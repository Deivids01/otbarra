package com.davidlg.iptvplayer.ui.ott

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import com.davidlg.iptvplayer.data.sourceRating
import com.davidlg.iptvplayer.data.sourcePoster
import com.davidlg.iptvplayer.data.sourceSynopsis
import com.davidlg.iptvplayer.data.sourceYear
import com.davidlg.iptvplayer.ui.Poppins
import com.davidlg.iptvplayer.ui.dlClickable
import com.davidlg.iptvplayer.ui.profiles.ProfileAvatar

private val HomeBlue = Color(0xFF168BFF)
private val HomePanel = Color(0xFF0D1929)

/** Inicio editorial OTT: portada real del catálogo y carruseles por contenido. */
@Composable
fun OttStart(vm: MainViewModel) {
    val live by vm.live.collectAsState()
    val vod by vm.vod.collectAsState()
    val recents by vm.recents.collectAsState()
    val metadataVersion = vm.vodMetadataVersion
    val config = LocalConfiguration.current
    val width = config.screenWidthDp
    val phone = width < 600
    val tablet = width in 600..899
    val compact = phone || tablet

    val movies = remember(vod) { vod.filter { startKind(it) == "movie" } }
    val series = remember(vod) { vod.filter { startKind(it) == "series" } }
    val anime = remember(vod) { vod.filter { startKind(it) == "anime" } }
    val continueVod = remember(vod, metadataVersion) { vod.filter { vm.vodProgress(it.id) > 10_000L } }
    val lastLive = remember(recents, live) { vm.recentChannels().firstOrNull() }
    val featured = remember(vod, metadataVersion) {
        vod.firstOrNull { resolvedMeta(vm, it, "backdrop", it.sourceBackdrop()).isNotBlank() }
            ?: vod.firstOrNull { resolvedMeta(vm, it, "poster", it.sourcePoster()).isNotBlank() }
            ?: vod.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (phone) 22.dp else 34.dp),
        verticalArrangement = Arrangement.spacedBy(if (phone) 22.dp else 30.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = if (phone) 16.dp else if (tablet) 24.dp else 68.dp)) {
                Text("TU ENTRETENIMIENTO, A TU MANERA", color = HomeBlue, fontSize = if (phone) 9.sp else 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp, fontFamily = Poppins)
                Text(if (phone) "Descubre" else "Descubre algo increíble", color = Color.White, fontSize = if (phone) 25.sp else if (tablet) 31.sp else 38.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, modifier = Modifier.padding(top = 4.dp))
            }
        }
        item {
            if (featured != null) FeaturedTitle(
                vm = vm,
                channel = featured,
                playbackQueue = vod,
                phone = phone,
                tablet = tablet,
                metadataVersion = metadataVersion,
            ) else EmptyCatalogueHero(phone, tablet) {
                vm.ottTab = OttTab.Live
            }
        }
        if (continueVod.isNotEmpty()) {
            item {
                ContentRail("Continúa viendo", "Retoma donde lo dejaste", continueVod, phone, tablet, metadataVersion, vm) { c ->
                    vm.play(c, vod, vm.vodProgress(c.id))
                }
            }
        }
        if (movies.isNotEmpty()) {
            item { ContentRail("Películas", "Historias para cada momento", movies, phone, tablet, metadataVersion, vm) { vm.play(it, vod) } }
        }
        if (series.isNotEmpty()) {
            item { ContentRail("Series", "Un episodio más", series, phone, tablet, metadataVersion, vm) { vm.play(it, vod) } }
        }
        if (anime.isNotEmpty()) {
            item { ContentRail("Anime", "Universos que te esperan", anime, phone, tablet, metadataVersion, vm) { vm.play(it, vod) } }
        }
        if (live.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    RailHeading("TV en vivo", "Tus canales disponibles", phone, tablet)
                    LazyRow(contentPadding = PaddingValues(horizontal = if (phone) 16.dp else if (tablet) 24.dp else 68.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(live.take(24), key = { it.id }) { channel ->
                            LiveShortcut(channel, phone) { vm.selectLiveChannel(channel); vm.play(channel, live) }
                        }
                    }
                }
            }
        }
        if (vod.isEmpty() && live.isEmpty()) {
            item { EmptyCatalogueHero(phone, tablet) { vm.ottTab = OttTab.Live } }
        }
        if (lastLive != null) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = if (phone) 16.dp else if (tablet) 24.dp else 68.dp)
                        .clip(RoundedCornerShape(16.dp)).background(HomePanel).border(1.dp, Color(0x222F9BFF), RoundedCornerShape(16.dp))
                        .dlClickable(RoundedCornerShape(16.dp), HomeBlue, 2.dp, 1.015f) { vm.play(lastLive, live) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF142A43)), contentAlignment = Alignment.Center) {
                        if (lastLive.logo.isNotBlank()) AsyncImage(lastLive.logo, null, Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit)
                        else Text("TV", color = HomeBlue, fontWeight = FontWeight.Black)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("ÚLTIMO CANAL", color = HomeBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text(lastLive.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("VER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(50)).background(HomeBlue).padding(horizontal = 13.dp, vertical = 8.dp))
                }
            }
        }
    }
}

private fun startKind(c: Channel): String {
    val g = c.group.lowercase()
    return when {
        g.contains("anime") -> "anime"
        g.contains("serie") || Regex("\\bs\\d{1,2}\\s?e\\d{1,3}\\b|temporada|season|episodio", RegexOption.IGNORE_CASE).containsMatchIn(c.name) -> "series"
        else -> "movie"
    }
}

private fun resolvedMeta(vm: MainViewModel, c: Channel, key: String, source: String): String =
    vm.vodMetadata(c.id).optString(key, "").takeIf { it.isNotBlank() } ?: source

@Composable
private fun FeaturedTitle(vm: MainViewModel, channel: Channel, playbackQueue: List<Channel>, phone: Boolean, tablet: Boolean, metadataVersion: Int) {
    val backdrop = resolvedMeta(vm, channel, "backdrop", channel.sourceBackdrop())
    val poster = resolvedMeta(vm, channel, "poster", channel.sourcePoster().ifBlank { channel.logo })
    val synopsis = resolvedMeta(vm, channel, "synopsis", channel.sourceSynopsis())
    val year = resolvedMeta(vm, channel, "year", channel.sourceYear())
    val genre = resolvedMeta(vm, channel, "genre", channel.sourceGenre())
    val duration = resolvedMeta(vm, channel, "duration", channel.sourceDuration())
    val rating = resolvedMeta(vm, channel, "rating", channel.sourceRating())
    val activeProfile by vm.activeProfile.collectAsState()
    val live by vm.live.collectAsState()
    val height = if (phone) 390.dp else if (tablet) 390.dp else 470.dp
    Box(
        Modifier.fillMaxWidth().padding(horizontal = if (phone) 12.dp else if (tablet) 20.dp else 60.dp)
            .height(height).clip(RoundedCornerShape(if (phone) 22.dp else 28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF10233B), Color(0xFF07111F))))
            .border(1.dp, Color(0x333F9FFF), RoundedCornerShape(if (phone) 22.dp else 28.dp)),
    ) {
        val art = backdrop.ifBlank { poster }
        if (art.isNotBlank()) AsyncImage(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0xF507101D), .48f to Color(0xB907101D), 1f to Color(0x1807101D))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, .56f to Color(0x20000000), 1f to Color(0xD907101D))))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth(if (phone) 1f else if (tablet) .92f else .68f)
                .padding(start = if (phone) 18.dp else 36.dp, end = if (phone) 18.dp else 28.dp, bottom = if (phone) 20.dp else 34.dp),
            verticalArrangement = Arrangement.spacedBy(if (phone) 8.dp else 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(HomeBlue))
                Text("DESTACADO PARA TI", color = Color(0xFF91CFFF), fontSize = if (phone) 9.sp else 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            }
            Text(channel.name, color = Color.White, fontSize = if (phone) 27.sp else if (tablet) 34.sp else 48.sp, lineHeight = if (phone) 31.sp else 54.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Poppins, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOf(year, rating.takeIf { it.isNotBlank() }?.let { "★ $it" }.orEmpty(), duration, genre.ifBlank { channel.group }).filter { it.isNotBlank() }.joinToString("  •  ")
            Text(meta, color = Color(0xFFD4E5F7), fontSize = if (phone) 10.sp else 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (synopsis.isNotBlank()) {
                Column(Modifier.fillMaxWidth(if (phone) 1f else .92f).clip(RoundedCornerShape(12.dp))
                    .background(Color(0x55202F43)).border(1.dp, Color(0x332F9BFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("SINOPSIS", color = Color(0xFF91CFFF), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
                    Text(synopsis, color = Color(0xFFE1EAF4), fontSize = if (phone) 11.sp else 14.sp, lineHeight = if (phone) 16.sp else 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text("Explora los detalles de este título y agrégalo a tu lista personal.", color = Color(0xFFD0DCE9), fontSize = if (phone) 11.sp else 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                HeroAction("▶  Reproducir", true, phone) { vm.play(channel, playbackQueue) }
                HeroAction("ⓘ  Detalles", false, phone) { vm.openVodDetails(channel) }
            }
        }
        if (!phone && !tablet) {
            Column(
                Modifier.align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
                    .width(226.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xCC0A1727))
                    .border(1.dp, Color(0x554B9FE8), RoundedCornerShape(18.dp))
                    .dlClickable(RoundedCornerShape(18.dp), HomeBlue, 2.dp, 1.02f) { vm.showProfileSwitcher() }
                    .padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileAvatar(activeProfile.av, 42.dp)
                    Column(Modifier.weight(1f)) {
                        Text("PERFIL ACTIVO", color = Color(0xFF91CFFF), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Text(activeProfile.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x334B9FE8)))
                Text("TU CATÁLOGO", color = Color(0xFF91CFFF), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(playbackQueue.size.toString(), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Poppins)
                        Text("Títulos VOD", color = Color(0xFF9FB4C9), fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(live.size.toString(), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Poppins)
                        Text("Canales", color = Color(0xFF9FB4C9), fontSize = 9.sp)
                    }
                }
                Text("Administrar perfil  ›", color = Color(0xFFB9DFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        } else if (!phone && poster.isNotBlank() && backdrop.isBlank()) {
            AsyncImage(poster, null, Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).size(width = 118.dp, height = 176.dp).clip(RoundedCornerShape(15.dp)), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun HeroAction(label: String, primary: Boolean, phone: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    Text(label, color = if (primary) Color(0xFF04101E) else Color.White, fontSize = if (phone) 11.sp else 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(shape).background(if (primary) Color.White else Color(0x55334455)).border(1.dp, if (primary) Color.White else Color(0x66FFFFFF), shape)
            .dlClickable(shape, HomeBlue, 2.dp, 1.04f, null, onClick).padding(horizontal = if (phone) 13.dp else 19.dp, vertical = if (phone) 10.dp else 13.dp))
}

@Composable
private fun ContentRail(title: String, subtitle: String, list: List<Channel>, phone: Boolean, tablet: Boolean, metadataVersion: Int, vm: MainViewModel, onPlay: (Channel) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        RailHeading(title, subtitle, phone, tablet)
        LazyRow(contentPadding = PaddingValues(horizontal = if (phone) 16.dp else if (tablet) 24.dp else 68.dp), horizontalArrangement = Arrangement.spacedBy(if (phone) 11.dp else 17.dp)) {
            items(list.take(80), key = { it.id }) { channel ->
                val poster = resolvedMeta(vm, channel, "poster", channel.sourcePoster().ifBlank { channel.logo })
                val progress = vm.vodProgress(channel.id)
                Column(Modifier.width(if (phone) 116.dp else if (tablet) 142.dp else 184.dp).dlClickable(RoundedCornerShape(12.dp), HomeBlue, 2.dp, 1.04f) { onPlay(channel) }) {
                    Box(Modifier.fillMaxWidth().height(if (phone) 166.dp else if (tablet) 204.dp else 266.dp).clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(Color(0xFF19314D), Color(0xFF0A1422))))) {
                        if (poster.isNotBlank()) AsyncImage(poster, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(58.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
                        if (progress > 10_000L) {
                            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(.42f).height(3.dp).background(HomeBlue))
                        }
                    }
                    Text(channel.name, color = Color.White, fontSize = if (phone) 10.sp else 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp, start = 2.dp, end = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun RailHeading(title: String, subtitle: String, phone: Boolean, tablet: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = if (phone) 16.dp else if (tablet) 24.dp else 68.dp).padding(bottom = 12.dp)) {
        Text(title, color = Color.White, fontSize = if (phone) 18.sp else if (tablet) 21.sp else 26.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins)
        if (!phone) Text(subtitle, color = Color(0xFF8EA4BB), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun LiveShortcut(channel: Channel, phone: Boolean, onClick: () -> Unit) {
    Row(Modifier.width(if (phone) 158.dp else 210.dp).height(if (phone) 58.dp else 70.dp)
        .clip(RoundedCornerShape(13.dp)).background(HomePanel).border(1.dp, Color(0x223F9FFF), RoundedCornerShape(13.dp))
        .dlClickable(RoundedCornerShape(13.dp), HomeBlue, 2.dp, 1.03f, null, onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(if (phone) 34.dp else 42.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF162C45)), contentAlignment = Alignment.Center) {
            if (channel.logo.isNotBlank()) AsyncImage(channel.logo, null, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
            else Text("TV", color = HomeBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = Color.White, fontSize = if (phone) 10.sp else 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("EN VIVO", color = HomeBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun EmptyCatalogueHero(phone: Boolean, tablet: Boolean, onLive: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = if (phone) 16.dp else if (tablet) 24.dp else 68.dp)
        .height(if (phone) 240.dp else 330.dp).clip(RoundedCornerShape(22.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF102B49), Color(0xFF07111F))))
        .padding(22.dp), verticalArrangement = Arrangement.Center) {
        Text("DAVIDLG IPTV", color = HomeBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Tu próxima historia comienza aquí", color = Color.White, fontSize = if (phone) 23.sp else 34.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, modifier = Modifier.padding(top = 8.dp))
        Text("Agrega una lista para descubrir películas, series y televisión en vivo.", color = Color(0xFFB8C9DA), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        HeroAction("Explorar TV en vivo", true, phone, onLive)
    }
}
