package com.davidlg.iptvplayer.ui.ott

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.davidlg.iptvplayer.player.PlaybackFactory
import com.davidlg.iptvplayer.ui.player.ShakaSurface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.data.EpgStore
import com.davidlg.iptvplayer.ui.Poppins
import com.davidlg.iptvplayer.ui.dlClickable

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun OttVod(vm: MainViewModel, kind: String, title: String) {
    val tab = when (kind) {
        "movie" -> com.davidlg.iptvplayer.OttTab.Movie
        "series" -> com.davidlg.iptvplayer.OttTab.Series
        "anime" -> com.davidlg.iptvplayer.OttTab.Anime
        else -> com.davidlg.iptvplayer.OttTab.Fav
    }
    com.davidlg.iptvplayer.ui.vod.VodScreen(vm, tab, false) { vm.ottTab = com.davidlg.iptvplayer.OttTab.Start }
}

/** #liveView — categorías + banner de vista previa + carrusel de canales + guía de programación. */
@Composable
fun OttLive(vm: MainViewModel, initialSection: String = "channels") {
    val live by vm.live.collectAsState()
    val epgVer by vm.epgVersion.collectAsState()
    val playbackPrefs by vm.playbackPrefs.collectAsState()
    val context = LocalContext.current
    val hostView = LocalView.current
    val showEpgImages = playbackPrefs.str("showEpgImages", "on") != "off"
    val useShaka = playbackPrefs.str("playerEngine", "auto") == "shaka"
    val inlinePlayer = remember(playbackPrefs.str("bufferMode", "default"), playbackPrefs.str("bitrateMode", "auto")) {
        PlaybackFactory.newPlayer(context, playbackPrefs.str("bufferMode", "default"), playbackPrefs.str("bitrateMode", "auto"))
    }
    DisposableEffect(inlinePlayer) {
        onDispose { inlinePlayer.release() }
    }
    val groups = remember(live) { live.groupBy { it.group.ifBlank { "Sin grupo" } } }
    val names = remember(groups) { listOf("TODOS") + groups.keys.toList() }
    var group by remember { mutableStateOf("TODOS") }
    val list = remember(live, group) { if (group == "TODOS") live else groups[group].orEmpty() }
    val selectedId = vm.selectedLiveChannelId
    val cur = list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
    DisposableEffect(hostView, cur?.id) {
        hostView.keepScreenOn = cur != null
        onDispose { hostView.keepScreenOn = false }
    }
    LaunchedEffect(cur?.id) { if (cur?.id != selectedId) vm.selectLiveChannel(cur) }
    LaunchedEffect(cur?.id, inlinePlayer, useShaka) {
        if (cur != null && !useShaka) PlaybackFactory.play(context, inlinePlayer, cur, 0L)
        else inlinePlayer.stop()
    }
    val prog = remember(cur, epgVer) { cur?.let { EpgStore.now(it) } }
    val next = remember(cur, epgVer) { cur?.let { EpgStore.next(it) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
      if (maxWidth < 900.dp) {
        var mobileSection by remember(initialSection) { mutableStateOf(initialSection) }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filtro de categorías: conserva el lenguaje visual dorado de DavidLG.
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                names.forEach { g ->
                    val active = g == group
                    Box(
                        Modifier.height(36.dp)
                            .dlClickable(RoundedCornerShape(30.dp), Color.White, 2.dp, 1f, null) { group = g }
                            .background(if (active) Color(0x35168BFF) else Color(0x18FFFFFF), RoundedCornerShape(30.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(g, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 1)
                    }
                }
            }

            // Reproductor integrado real: mantiene ExoPlayer en la pantalla OTT móvil.
            Box(
                Modifier.fillMaxWidth().height(210.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0x88168BFF), RoundedCornerShape(17.dp))
            ) {
                if (useShaka && cur != null) {
                    ShakaSurface(cur, Modifier.fillMaxSize())
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShutterBackgroundColor(android.graphics.Color.BLACK)
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                this.player = inlinePlayer
                            }
                        },
                        update = { it.player = inlinePlayer }
                    )
                }
                Box(Modifier.align(Alignment.TopStart).padding(9.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xB8000000)).padding(horizontal = 9.dp, vertical = 5.dp)) {
                    Text("EN VIVO", color = OttGold, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, letterSpacing = 1.sp)
                }
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(cur?.name ?: "TV en vivo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(prog?.title ?: "Selecciona un canal", color = OttText, fontSize = 10.sp, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xD9168BFF))
                        .dlClickable(RoundedCornerShape(8.dp), Color.White, 1.dp, 1f, null) { cur?.let { vm.play(it, list) } }
                        .padding(horizontal = 11.dp, vertical = 8.dp)) {
                        Text("⛶", color = Color(0xFF06111F), fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (cur == null) {
                    Box(Modifier.fillMaxSize().background(Color(0xAA080808)), contentAlignment = Alignment.Center) {
                        Text("Agrega una lista para comenzar", color = Color.White, fontSize = 12.sp, fontFamily = Poppins)
                    }
                }
            }

            // Pestañas táctiles independientes para canales y programación.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF101E31)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("channels" to "Canales", "epg" to "Programación").forEach { (key, label) ->
                    val active = mobileSection == key
                    Box(
                        Modifier.weight(1f).height(42.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (active) Color(0x35168BFF) else Color.Transparent)
                            .dlClickable(RoundedCornerShape(9.dp), OttGold, 2.dp, 1f, null) { mobileSection = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (active) OttGold else OttText, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins)
                    }
                }
            }

            if (mobileSection == "channels") {
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { channel ->
                        val currentProgram = remember(channel, epgVer) { EpgStore.now(channel) }
                        val selected = cur?.id == channel.id
                        Row(
                            Modifier.fillMaxWidth().height(76.dp)
                                .dlClickable(RoundedCornerShape(13.dp), OttGold, 2.dp, 1f, null) { vm.selectLiveChannel(channel) }
                                .background(if (selected) Color(0xFF123A60) else Color(0xFF101E31), RoundedCornerShape(13.dp))
                                .then(if (selected) Modifier.border(1.dp, OttGold, RoundedCornerShape(13.dp)) else Modifier)
                                .padding(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (channel.logo.isNotBlank()) AsyncImage(channel.logo, null, Modifier.size(49.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(channel.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(currentProgram?.title ?: "Sin información EPG", color = OttText, fontSize = 11.sp, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (showEpgImages && currentProgram?.icon?.isNotBlank() == true) AsyncImage(currentProgram.icon, null, Modifier.size(40.dp, 54.dp).clip(RoundedCornerShape(7.dp)), contentScale = ContentScale.Crop)
                            Text(pad3(channel.number), color = OttGold, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().weight(1f)) {
                    Text("GUÍA DE PROGRAMACIÓN", color = OttGoldSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, letterSpacing = 1.8.sp)
                    Text("Lo que está pasando ahora y lo que viene después", color = OttText.copy(alpha = .68f), fontSize = 10.sp, fontFamily = Poppins, modifier = Modifier.padding(top = 3.dp, bottom = 9.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(list, key = { it.id }) { channel ->
                            MobileGuideCard(channel, epgVer, showEpgImages, cur?.id == channel.id) { vm.selectLiveChannel(channel) }
                        }
                    }
                }
            }
        }
      } else {
    Row(Modifier.fillMaxSize().padding(start = 42.dp, end = 42.dp, top = 14.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        // ------------ Categorías ------------
        Column(
            Modifier.width(340.dp).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Color(0xEB0B1728))
                .border(1.dp, Color(0x29168BFF), RoundedCornerShape(20.dp)).padding(14.dp)
        ) {
            Text("CATEGORÍAS", color = OttGoldSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, letterSpacing = 3.64.sp, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            Text("Explora todos nuestros canales por categoría", color = OttText.copy(alpha = 0.6f), fontSize = 13.sp, fontFamily = Poppins, modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(names) { g ->
                    val n = if (g == "TODOS") live.size else groups[g]?.size ?: 0
                    val on = g == group
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier.fillMaxWidth().height(54.dp)
                            .dlClickable(shape, Color.White, 3.dp, 1f, null) { group = g }
                            .background(if (on) Color(0x29168BFF) else Color(0xFF101E31))
                            .border(1.dp, if (on) OttGold else Color.Transparent, shape)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(g, Modifier.weight(1f), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0x26168BFF)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                            Text(n.toString(), color = OttGold, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins)
                        }
                    }
                }
            }
        }
        // ------------ Columna derecha ------------
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Vista previa real: usa el motor seleccionado en Ajustes (ExoPlayer o Shaka).
            Box(
                Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(22.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0x66168BFF), RoundedCornerShape(22.dp))
            ) {
                if (cur != null) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(.46f).background(Color.Black)) {
                            if (useShaka) {
                                ShakaSurface(cur, Modifier.fillMaxSize())
                            } else {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { viewContext ->
                                        PlayerView(viewContext).apply {
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                            player = inlinePlayer
                                        }
                                    },
                                    update = { it.player = inlinePlayer },
                                )
                            }
                            Box(Modifier.align(Alignment.TopStart).padding(13.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xD9000000)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("● EN VIVO", color = Color(0xFF8DE2B0), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, letterSpacing = 1.sp)
                            }
                        }
                        Column(
                            Modifier.weight(1f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF10243B), Color(0xFF0B1728))))
                                .padding(horizontal = 28.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                                        if (cur.logo.isNotBlank()) AsyncImage(cur.logo, null, Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit)
                                        else Text("TV", color = OttGold, fontWeight = FontWeight.Black, fontFamily = Poppins)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("CANAL ${pad3(cur.number)}", color = OttGoldSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                        Text(cur.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Text(prog?.title ?: "Sin información EPG", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 32.sp)
                                Text(
                                    (prog?.let { fmtAmPm(it.start) + " – " + fmtAmPm(it.stop) + " · " } ?: "") + cur.group,
                                    color = OttCream, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                if (!prog?.desc.isNullOrBlank()) Text(prog?.desc.orEmpty(), color = OttText.copy(alpha = .78f), fontSize = 12.sp, fontFamily = Poppins, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x332F9BFF))) {
                                    Box(Modifier.fillMaxWidth(EpgStore.progress(prog)).height(4.dp).background(OttGold))
                                }
                                GoldPill("▶  Ver en pantalla completa", 14, 20, 10, Modifier) { vm.play(cur, list) }
                            }
                        }
                    }
                } else {
                    Text("Sin canales. Agrega una lista en Ajustes.", color = OttText, fontSize = 20.sp, fontFamily = Poppins, modifier = Modifier.align(Alignment.Center))
                }
            }
            // Carrusel de canales
            val rowState = rememberLazyListState()
            LazyRow(Modifier.fillMaxWidth().height(176.dp), state = rowState, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(list.take(400), key = { it.id }) { c ->
                    Column(
                        Modifier.width(190.dp).height(158.dp)
                            .dlClickable(CardShape, OttGold, 3.dp, 1.05f, { if (it) vm.selectLiveChannel(c) }) { vm.play(c, list) }
                            .background(Brush.verticalGradient(listOf(Color(0xFF17365A), Color(0xFF0B1728))))
                            .border(1.dp, Color(0x29168BFF), CardShape).padding(10.dp)
                    ) {
                        Text(pad3(c.number), color = OttGold, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
                        Box(Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x1FFFFFFF)), contentAlignment = Alignment.Center) {
                            if (c.logo.isNotBlank()) AsyncImage(c.logo, null, Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit)
                            else Text("TV", color = OttGold, fontWeight = FontWeight.Black, fontFamily = Poppins, fontSize = 26.sp)
                        }
                        Text(c.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            // Guía de programación
            Column(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xEB0B1728))
                    .border(1.dp, Color(0x29168BFF), RoundedCornerShape(20.dp)).padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("▣", color = OttGold, fontSize = 22.sp)
                    Text("Guía de Programación", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
                    Pill(group)
                    Box(Modifier.weight(1f))
                    Pill("${list.size} CANALES · GUÍA EN VIVO")
                    Pill(esDate("EEE d 'DE' MMM").uppercase())
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                    Text("CANAL", Modifier.width(370.dp), color = OttGoldDim, fontSize = 12.sp, fontFamily = Poppins, letterSpacing = 2.6.sp, fontWeight = FontWeight.Bold)
                    Text("AHORA", Modifier.width(523.dp), color = OttGoldDim, fontSize = 12.sp, fontFamily = Poppins, letterSpacing = 2.6.sp, fontWeight = FontWeight.Bold)
                    Text("SIGUIENTE", color = OttGoldDim, fontSize = 12.sp, fontFamily = Poppins, letterSpacing = 2.6.sp, fontWeight = FontWeight.Bold)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(list.take(300), key = { it.id }) { c -> EpgRow(vm, c, list, epgVer, showEpgImages) { vm.selectLiveChannel(c) } }
                }
            }
        }
    }
      }
    }
}

@Composable
private fun MobileGuideCard(channel: Channel, ver: Int, showImages: Boolean, selected: Boolean, onSelect: () -> Unit) {
    val now = System.currentTimeMillis()
    val schedule = remember(channel, ver) { EpgStore.schedule(channel, now - 30 * 60_000L, now + 4 * 60 * 60_000L) }
    val current = remember(channel, ver) { EpgStore.now(channel) }
    val next = remember(channel, ver) { EpgStore.next(channel) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF102A45) else Color(0xFF0D1929))
            .border(1.dp, if (selected) OttGold.copy(alpha = .8f) else Color(0x223F9FFF), RoundedCornerShape(16.dp))
            .dlClickable(RoundedCornerShape(16.dp), OttGold, 2.dp, 1.01f, null, onSelect)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF172D45)), contentAlignment = Alignment.Center) {
                if (channel.logo.isNotBlank()) AsyncImage(channel.logo, null, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
                else Text("TV", color = OttGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(channel.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("CANAL ${pad3(channel.number)}", color = OttText.copy(alpha = .6f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text(if (selected) "SELECCIONADO" else "VER CANAL", color = OttGold, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        }
        if (schedule.isEmpty()) {
            Text("No hay programación disponible para este canal.", color = OttText.copy(alpha = .65f), fontSize = 10.sp)
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                schedule.take(6).forEach { p ->
                    val isCurrent = current?.title == p.title && current.start == p.start
                    Column(
                        Modifier.width(178.dp).clip(RoundedCornerShape(11.dp))
                            .background(if (isCurrent) Color(0xFF153B61) else Color(0xFF142235))
                            .border(1.dp, if (isCurrent) OttGold else Color(0x222F91E8), RoundedCornerShape(11.dp))
                            .padding(9.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(if (isCurrent) "AHORA" else fmtAmPm(p.start), color = if (isCurrent) OttGold else Color(0xFF9DB6D0), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            if (isCurrent) Text("EN VIVO", color = Color(0xFF8DE2B0), fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            if (showImages && p.icon.isNotBlank()) AsyncImage(p.icon, null, Modifier.size(39.dp, 49.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                            Text(p.title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${fmtAmPm(p.start)} – ${fmtAmPm(p.stop)}", color = Color(0xFF9DB6D0), fontSize = 8.sp)
                        if (isCurrent) {
                            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x443F9FFF))) {
                                Box(Modifier.fillMaxWidth(EpgStore.progress(p)).height(3.dp).background(OttGold))
                            }
                        }
                    }
                }
                if (next != null && schedule.none { it.start == next.start }) {
                    Text("SIGUIENTE · ${next.title}", color = OttText.copy(alpha = .75f), fontSize = 10.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
        if (current?.desc?.isNotBlank() == true) Text(current.desc, color = OttText.copy(alpha = .66f), fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Pill(t: String) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0x1F168BFF)).border(1.dp, Color(0x40168BFF), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 5.dp)) {
        Text(t, color = OttGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, letterSpacing = 1.sp, maxLines = 1)
    }
}

@Composable
private fun EpgRow(vm: MainViewModel, c: Channel, list: List<Channel>, ver: Int, showImages: Boolean, onSel: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Row(Modifier.fillMaxWidth().height(88.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.width(250.dp).fillMaxHeight().clip(shape).background(Color(0xFF0D1929))
                .border(1.dp, Color(0x223F9FFF), shape)
                .dlClickable(shape, OttGold, 2.dp, 1.02f, null, onSel).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(pad3(c.number), color = OttGold, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF172D45)), contentAlignment = Alignment.Center) {
                if (c.logo.isNotBlank()) AsyncImage(c.logo, null, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
                else Text("TV", color = OttGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Text(c.name, Modifier.weight(1f), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().clip(shape).background(Color(0xFF0A1422))) {
            val now = System.currentTimeMillis()
            val from = now - 30 * 60_000L
            val until = now + 3 * 60 * 60_000L
            val window = (until - from).toFloat()
            val schedule = remember(c, ver) { EpgStore.schedule(c, from, until) }
            val timelineWidth = maxWidth
            if (schedule.isEmpty()) {
                Text("Sin información de programación", color = OttText.copy(alpha = .6f), fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
            } else {
                Row(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    schedule.forEach { p ->
                        val stop = if (p.stop > p.start) p.stop else p.start + 60 * 60_000L
                        val clippedStart = maxOf(p.start, from)
                        val clippedStop = minOf(stop, until)
                        val fraction = ((clippedStop - clippedStart).coerceAtLeast(1L) / window).coerceIn(.08f, 1f)
                        val cardWidth = timelineWidth * fraction
                        val current = now >= p.start && now < stop
                        Column(
                            Modifier.width(cardWidth).fillMaxHeight().clip(RoundedCornerShape(9.dp))
                                .background(if (current) Color(0xFF153D65) else Color(0xFF142235))
                                .border(1.dp, if (current) OttGold else Color(0x223F9FFF), RoundedCornerShape(9.dp))
                                .dlClickable(RoundedCornerShape(9.dp), OttGold, 2.dp, 1.01f, null, onSel)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(if (current) "AHORA · ${fmtAmPm(p.start)}" else fmtAmPm(p.start), color = if (current) OttGold else Color(0xFF9DB6D0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                if (showImages && p.icon.isNotBlank()) AsyncImage(p.icon, null, Modifier.size(42.dp, 48.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                Text(p.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Text(fmtAmPm(stop), color = Color(0xFF9DB6D0), fontSize = 9.sp)
                            if (current) {
                                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x443F9FFF))) {
                                    Box(Modifier.fillMaxWidth(EpgStore.progress(p)).fillMaxHeight().background(OttGold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
