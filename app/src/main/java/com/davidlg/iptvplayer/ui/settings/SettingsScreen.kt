package com.davidlg.iptvplayer.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.data.Playlist
import com.davidlg.iptvplayer.ui.Sans
import com.davidlg.iptvplayer.ui.dlClickable

private val Gold = Color(0xFFE0C38A)
private val Dim = Color(0xFF9AA3B8)
private val Danger = Color(0xFFFF8A8A)

private enum class Sec(val label: String) { Listas("Listas"), Reproductor("Reproductor"), Red("Red y búfer"), Epg("EPG"), Apariencia("Apariencia"), Vod("Catálogo VOD"), Sistema("Sistema") }

private typealias Opts = List<Pair<String, String>>

private val OnOff: Opts = listOf("on" to "Activado", "off" to "Desactivado")
private val Quality: Opts = listOf("auto" to "Automática", "highest" to "Máxima disponible", "2160" to "4K · 2160p", "1080" to "FHD · 1080p", "720" to "HD · 720p", "480" to "SD · 480p")

@Composable
fun SettingsScreen(vm: MainViewModel, onClose: () -> Unit) {
    var sec by remember { mutableStateOf(Sec.Listas) }
    var dialog by remember { mutableStateOf<Playlist?>(null) }
    var adding by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    BackHandler { if (adding || dialog != null || confirmClear) { adding = false; dialog = null; confirmClear = false } else onClose() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E1426), Color(0xFF070A14))))) {
        val compact = maxWidth < 900.dp
        if (compact) {
            Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text("Ajustes", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                Text("DavidLG IPTV Player", color = Dim, fontSize = 13.sp, fontFamily = Sans, modifier = Modifier.padding(bottom = 10.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Sec.values().forEach { item ->
                        val active = item == sec
                        Box(Modifier.height(42.dp).dlClickable(RoundedCornerShape(12.dp), Color.White, 2.dp, 1f, null) { sec = item }
                            .background(if (active) Color(0xFFDFC48E) else Color(0x22FFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 13.dp), contentAlignment = Alignment.Center) {
                            Text(item.label, color = if (active) Color(0xFF1B140A) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                        }
                    }
                }
                Text(sec.label, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    when (sec) {
                        Sec.Listas -> listas(vm, { adding = true }, { dialog = it }, { confirmClear = true })
                        Sec.Reproductor -> reproductor(vm)
                        Sec.Red -> red(vm)
                        Sec.Epg -> epg(vm)
                        Sec.Apariencia -> apariencia(vm)
                        Sec.Vod -> vod(vm)
                        Sec.Sistema -> sistema(vm)
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(540.dp).fillMaxHeight().background(Color(0x66000000)).padding(start = 52.dp, end = 34.dp, top = 56.dp)) {
                    Text("Ajustes", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(start = 20.dp))
                    Text("DavidLG IPTV · Configuración", color = Dim, fontSize = 20.sp, fontFamily = Sans, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, bottom = 38.dp))
                    Sec.values().forEach { item ->
                        val active = item == sec
                        val shape = RoundedCornerShape(16.dp)
                        Box(Modifier.fillMaxWidth().height(94.dp).padding(bottom = 8.dp)
                            .dlClickable(shape, Color.White, 3.dp, 1f, { if (it) sec = item }) { sec = item }
                            .background(if (active) Color(0xFFDFC48E) else Color.Transparent).padding(start = 30.dp), contentAlignment = Alignment.CenterStart) {
                            Text(item.label, color = if (active) Color(0xFF1B140A) else Color(0xFFC3C9D6), fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight().padding(start = 78.dp, end = 90.dp, top = 56.dp)) {
                    Text(sec.label, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(bottom = 22.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1FFFFFFF)))
                    LazyColumn(Modifier.padding(start = 30.dp)) {
                        when (sec) {
                            Sec.Listas -> listas(vm, { adding = true }, { dialog = it }, { confirmClear = true })
                            Sec.Reproductor -> reproductor(vm)
                            Sec.Red -> red(vm)
                            Sec.Epg -> epg(vm)
                            Sec.Apariencia -> apariencia(vm)
                            Sec.Vod -> vod(vm)
                            Sec.Sistema -> sistema(vm)
                        }
                    }
                }
            }
        }
    }
    if (adding || dialog != null) PlaylistDialog(vm, dialog) { adding = false; dialog = null }
    if (confirmClear) ConfirmBox("¿Borrar todas las fuentes guardadas?", "Se eliminarán las listas de este televisor.") { ok -> confirmClear = false; if (ok) vm.clearAllSources() }
}

private fun androidx.compose.foundation.lazy.LazyListScope.listas(vm: MainViewModel, onAdd: () -> Unit, onEdit: (Playlist) -> Unit, onClear: () -> Unit) {
    item { Row1("＋  Añadir lista", "M3U, Xtream Codes o guía XMLTV", Gold, onAdd) }
    item { Label2("MIS LISTAS") }
    items(100) { i ->
        val pls = vm.playlists.value
        if (i < pls.size) {
            val p = pls[i]
            val on = p.id in vm.activeIds.value
            Row1(p.name, (if (on) "Activa" else "Inactiva") + " · " + when (p.contentType) { "tv" -> "Solo TV en vivo"; "vod" -> "Solo VOD"; else -> "TV en vivo + VOD" } + (if (p.epgUrl.isNotBlank()) " · EPG configurada" else ""), Color.White, { vm.toggleActive(p.id) }, if (on) "✓" else "", { onEdit(p) })
        }
    }
    item { Label2("HERRAMIENTAS") }
    item { Row1("Actualizar listas y guía", "Vuelve a descargar M3U y EPG", Color.White, { vm.reload(true) }) }
    item { Row1("Vaciar caché", "Borra datos descargados y recarga", Color.White, { vm.clearCacheAndReload() }) }
    item { Row1("Borrar fuentes guardadas", "Elimina todas las listas de este televisor", Danger, onClear) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.reproductor(vm: MainViewModel) {
    item { OptRow(vm, false, "playerEngine", "Motor de reproducción", listOf("auto" to "Automático (ExoPlayer por defecto)", "exo" to "ExoPlayer / Media3 (nativo)", "shaka" to "Shaka Player (WebView · experimental)"), "auto") }
    item { OptRow(vm, false, "clock", "Reloj del reproductor", OnOff, "on") }
    item { OptRow(vm, false, "timeFormat", "Formato de hora", listOf("12" to "12 horas · 1:30 p. m.", "24" to "24 horas · 13:30"), "12") }
    item { OptRow(vm, false, "audio", "Idioma preferido", listOf("es" to "Español", "en" to "English", "auto" to "Automático"), "es") }
    item { OptRow(vm, false, "subs", "Mostrar subtítulos", listOf("off" to "Desactivados", "es" to "Español", "en" to "English"), "off") }
    item { OptRow(vm, false, "quality", "Calidad general", Quality, "auto") }
    item { OptRow(vm, false, "fps", "Fotogramas por segundo (FPS)", listOf("auto" to "Automático", "30" to "30 fps", "60" to "60 fps", "120" to "120 fps", "max" to "El máximo que ofrezca el canal"), "auto") }
    item { OptRow(vm, false, "showBuffer", "Mostrar indicador de búfer", OnOff, "on") }
    item { OptRow(vm, false, "showStreamQuality", "Mostrar calidad real del flujo", OnOff, "on") }
    item { OptRow(vm, false, "zapDelayMs", "Retardo antes del cambio de canal", listOf("0" to "Instantáneo", "600" to "0.6 segundos", "1000" to "1 segundo", "2000" to "2 segundos", "3000" to "3 segundos", "4000" to "4 segundos", "5000" to "5 segundos", "6000" to "6 segundos", "8000" to "8 segundos", "10000" to "10 segundos", "15000" to "15 segundos", "20000" to "20 segundos", "25000" to "25 segundos", "30000" to "30 segundos"), "0") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.red(vm: MainViewModel) {
    item { OptRow(vm, false, "bufferMode", "Tamaño del búfer de emisión", listOf("low" to "Sin búfer adicional · mínimo práctico", "default" to "Predeterminado / proxy", "high" to "Aumentado"), "default") }
    item { OptRow(vm, false, "bitrateMode", "Selección de pista por bitrate", listOf("low" to "Preferir bitrate más bajo", "auto" to "Detección automática", "high" to "Preferir bitrate más alto"), "auto") }
    item { Text("El modo reducido disminuye el búfer adicional, pero la reproducción siempre depende de los segmentos y de la red de origen.", color = Dim, fontSize = 18.sp, fontFamily = Sans, modifier = androidx.compose.ui.Modifier.padding(vertical = 16.dp)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.epg(vm: MainViewModel) {
    item { OptRow(vm, false, "showEpgImages", "Mostrar imágenes de programas (si existen)", OnOff, "on") }
    item { Text("Se mostrarán las imágenes declaradas mediante <icon src=...> en la guía XMLTV. Si la guía no incluye imágenes, se conservará el diseño sin imagen.", color = Dim, fontSize = 18.sp, fontFamily = Sans, modifier = androidx.compose.ui.Modifier.padding(vertical = 16.dp)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.apariencia(vm: MainViewModel) {
    item { OptRow(vm, true, "style", "Estilo visual", listOf("ott" to "OTT (nuevo)", "classic" to "Clásico"), "ott") }
    item { OptRow(vm, true, "theme", "Tema de color (OTT)", listOf("champan" to "Medianoche y champán", "violeta" to "Violeta nocturno", "esmeralda" to "Esmeralda", "burdeos" to "Burdeos y oro rosa", "grafito" to "Grafito y plata"), "champan") }
    item { OptRow(vm, true, "bannerArt", "Imagen del banner (TV en vivo)", listOf("right" to "Completa, a la derecha", "full" to "Pantalla completa"), "right") }
    item { OptRow(vm, true, "fastZap", "Cambio rápido de canal", OnOff, "on") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.vod(vm: MainViewModel) {
    item { OptRow(vm, true, "landing", "Pantalla inicial", listOf("hero" to "Portada y filas de contenido", "catalog" to "Ir directamente al catálogo"), "hero") }
    item { OptRow(vm, true, "titles", "Títulos de los pósters (VOD)", listOf("focus" to "Solo al enfocar (estilo Netflix)", "always" to "Siempre visibles"), "focus") }
    item { OptRow(vm, true, "kind", "Mostrar contenido", listOf("all" to "Películas, series y animes", "movies" to "Solo películas", "series" to "Solo series", "anime" to "Solo animes"), "all") }
    item { OptRow(vm, true, "sort", "Orden del catálogo", listOf("source" to "Orden de la lista", "az" to "Nombre: A–Z", "recent" to "Añadidos recientemente", "year" to "Año: más nuevo primero"), "source") }
    item { TextPref(vm) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sistema(vm: MainViewModel) {
    item {
        val live by vm.live.collectAsState(); val v by vm.vod.collectAsState(); val st by vm.status.collectAsState(); val er by vm.errors.collectAsState()
        Column(Modifier.padding(top = 20.dp)) {
            Label2("DIAGNÓSTICO")
            Line("Canales en vivo", live.size.toString()); Line("Películas y series", v.size.toString())
            Line("Estado", st.ifBlank { "Listo" }); Line("Perfil activo", vm.activeProfile.value.name)
            er.forEach { Text("• $it", color = Danger, fontSize = 20.sp, fontFamily = Sans, modifier = Modifier.padding(vertical = 3.dp)) }
        }
    }
}

@Composable private fun Line(k: String, v: String) {
    val compact = LocalConfiguration.current.screenWidthDp < 900
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(k, Modifier.width(if (compact) 140.dp else 360.dp), color = Dim, fontSize = if (compact) 13.sp else 24.sp, fontFamily = Sans)
        Text(v, color = Color.White, fontSize = if (compact) 13.sp else 24.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans)
    }
}

@Composable private fun Label2(t: String) =
    Text(t, color = Gold, fontSize = if (LocalConfiguration.current.screenWidthDp < 900) 13.sp else 20.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, letterSpacing = 4.sp, modifier = Modifier.padding(top = 36.dp, bottom = 12.dp))

@Composable
private fun Row1(title: String, sub: String, col: Color, onClick: () -> Unit, right: String = "›", onLong: (() -> Unit)? = null) {
    val compact = LocalConfiguration.current.screenWidthDp < 900
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .dlClickable(shape, Gold, 2.dp, 1.01f, null, onClick)
            .background(Color(0xFF111E32), shape)
            .border(1.dp, Color(0x1FFFFFFF), shape)
            .padding(horizontal = if (LocalConfiguration.current.screenWidthDp < 900) 13.dp else 22.dp,
                vertical = if (LocalConfiguration.current.screenWidthDp < 900) 13.dp else 19.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = col, fontSize = if (compact) 18.sp else 32.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Dim, fontSize = if (compact) 12.sp else 22.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onLong != null) Text("✎", color = Gold, fontSize = 30.sp, modifier = Modifier.padding(end = 22.dp).dlClickable(RoundedCornerShape(10.dp), Color.White, 3.dp, 1f, null, onLong).padding(8.dp))
        Text(right, color = if (right == "✓") Gold else Dim, fontSize = if (compact) 24.sp else 36.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
    }
}

/** Selector que cambia con OK (equivale a los <select> del panel web). */
@Composable
private fun OptRow(vm: MainViewModel, vod: Boolean, key: String, label: String, opts: Opts, def: String) {
    val st by (if (vod) vm.vodSettings else vm.playbackPrefs).collectAsState()
    val cur = st.str(key, def)
    val selected = opts.firstOrNull { it.first == cur } ?: opts.first()
    var expanded by remember(key) { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Row1(label, selected.second, Color.White, { expanded = true }, "⌄")
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            opts.forEach { (value, title) ->
                DropdownMenuItem(
                    text = { Text(if (value == cur) "✓  $title" else title) },
                    onClick = {
                        if (vod) vm.putVodSetting(key, value) else vm.putPlaybackPref(key, value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TextPref(vm: MainViewModel) {
    val st by vm.vodSettings.collectAsState()
    var v by remember(st) { mutableStateOf(st.str("tmdbKey", "")) }
    Column(Modifier.padding(vertical = 14.dp, horizontal = 12.dp)) {
        Text("API key de TMDB", color = Color.White, fontSize = if (LocalConfiguration.current.screenWidthDp < 900) 18.sp else 32.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
        Field(v, "Clave o token de TMDB") { v = it; vm.putVodSetting("tmdbKey", it.trim()) }
    }
}

@Composable
private fun Field(v: String, hint: String, requester: FocusRequester? = null, onChange: (String) -> Unit) {
    val compact = LocalConfiguration.current.screenWidthDp < 900
    var f by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp).height(if (compact) 48.dp else 64.dp).background(Color(0x14FFFFFF), RoundedCornerShape(12.dp))
            .border(if (f) 3.dp else 1.dp, if (f) Color.White else Color(0x33FFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            v, onChange, singleLine = true, textStyle = TextStyle(color = Color.White, fontSize = if (compact) 16.sp else 26.sp, fontFamily = Sans),
            cursorBrush = SolidColor(Gold), modifier = Modifier.fillMaxWidth()
                .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
                .androidxFocus { f = it },
        )
        if (v.isEmpty()) Text(hint, color = Color(0x66FFFFFF), fontSize = if (compact) 16.sp else 26.sp, fontFamily = Sans)
    }
}

private fun Modifier.androidxFocus(cb: (Boolean) -> Unit): Modifier = this.onFocusChanged { cb(it.isFocused) }

@Composable
private fun PlaylistDialog(vm: MainViewModel, edit: Playlist?, onClose: () -> Unit) {
    val compact = LocalConfiguration.current.screenWidthDp < 900
    val firstFieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFieldFocus.requestFocus() }
    var name by remember { mutableStateOf(edit?.name ?: "") }
    var url by remember { mutableStateOf(edit?.m3uUrl ?: "") }
    var epg by remember { mutableStateOf(edit?.epgUrl ?: "") }
    var type by remember { mutableStateOf(edit?.contentType ?: "mixed") }
    var xt by remember { mutableStateOf(false) }
    var srv by remember { mutableStateOf("") }; var usr by remember { mutableStateOf("") }; var pwd by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).padding(8.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(if (compact) 1f else 0.96f).widthIn(max = 1100.dp).heightIn(max = 760.dp)
            .verticalScroll(rememberScrollState()).focusGroup().background(Color(0xFF111A30), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0x33E0C38A), RoundedCornerShape(24.dp)).padding(if (compact) 16.dp else 36.dp)) {
            Text(if (edit == null) "Añadir lista" else "Editar lista", color = Color.White, fontSize = if (compact) 24.sp else 40.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
            Field(name, "Nombre de la lista", firstFieldFocus) { name = it }
            if (!xt) { Field(url, "URL M3U") { url = it } } else {
                Field(srv, "Servidor (http://host:puerto)") { srv = it }; Field(usr, "Usuario") { usr = it }; Field(pwd, "Contraseña") { pwd = it }
            }
            Field(epg, "URL de guía XMLTV (opcional)") { epg = it }
            Row(Modifier.padding(top = 18.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Btn(if (xt) "Modo: Xtream Codes" else "Modo: URL M3U", false) { xt = !xt }
                Btn(when (type) { "tv" -> "Solo TV en vivo"; "vod" -> "Solo VOD"; else -> "TV en vivo + VOD" }, false) { type = when (type) { "mixed" -> "tv"; "tv" -> "vod"; else -> "mixed" } }
            }
            Row(Modifier.padding(top = 22.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Btn("Guardar", true) {
                    var m3u = url.trim(); var x = epg.trim()
                    if (xt && srv.isNotBlank()) {
                        val s = srv.trim().trimEnd('/')
                        m3u = "$s/get.php?username=${usr.trim()}&password=${pwd.trim()}&type=m3u_plus&output=ts"
                        if (x.isEmpty()) x = "$s/xmltv.php?username=${usr.trim()}&password=${pwd.trim()}"
                    }
                    if (m3u.isNotBlank()) {
                        val nm = name.ifBlank { "Lista ${vm.playlists.value.size + 1}" }
                        if (edit == null) vm.addPlaylist(Playlist(System.currentTimeMillis().toString(), nm, m3u, x, type))
                        else vm.updatePlaylist(edit.copy(name = nm, m3uUrl = m3u, epgUrl = x, contentType = type))
                        onClose()
                    }
                }
                Btn("Cancelar", false, onClose)
                if (edit != null) Btn("Eliminar", false) { vm.removePlaylist(edit.id); onClose() }
            }
        }
    }
}

@Composable
private fun Btn(t: String, primary: Boolean, onClick: () -> Unit) {
    val compact = LocalConfiguration.current.screenWidthDp < 900
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.dlClickable(shape, Color.White, 3.dp, 1.04f, null, onClick)
            .background(if (primary) Color(0xFFDFC48E) else Color(0x1FFFFFFF)).padding(horizontal = if (compact) 12.dp else 36.dp, vertical = if (compact) 10.dp else 16.dp),
    ) { Text(t, color = if (primary) Color(0xFF1B140A) else Color.White, fontSize = if (compact) 13.sp else 24.sp, fontWeight = FontWeight.Bold, fontFamily = Sans) }
}

@Composable
private fun ConfirmBox(title: String, msg: String, res: (Boolean) -> Unit) {
    val compact = LocalConfiguration.current.screenWidthDp < 900
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).padding(8.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.94f).widthIn(max = 860.dp).background(Color(0xFF111A30), RoundedCornerShape(24.dp)).padding(if (compact) 18.dp else 40.dp)) {
            Text(title, color = Color.White, fontSize = if (compact) 20.sp else 38.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
            Text(msg, color = Dim, fontSize = if (compact) 14.sp else 24.sp, fontFamily = Sans, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { Btn("Borrar", true) { res(true) }; Btn("Cancelar", false) { res(false) } }
        }
    }
}
