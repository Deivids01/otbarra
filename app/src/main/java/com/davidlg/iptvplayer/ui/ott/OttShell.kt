package com.davidlg.iptvplayer.ui.ott

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.OttTab
import com.davidlg.iptvplayer.Screen
import com.davidlg.iptvplayer.ui.Poppins
import com.davidlg.iptvplayer.ui.dlClickable
import com.davidlg.iptvplayer.ui.profiles.ProfileAvatar
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Navegación exclusiva de OTT Nuevo. ClassicShell no se modifica ni comparte esta barra. */
@Composable
fun OttShell(vm: MainViewModel) {
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0B1728), Color(0xFF070D18), Color(0xFF050A12)))
        )
    ) {
        OttTopBar(vm)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (vm.ottTab) {
                OttTab.Start -> OttStart(vm)
                OttTab.Live -> OttLive(vm, "channels")
                OttTab.Epg -> OttLive(vm, "epg")
                OttTab.Movie -> OttVod(vm, "movie", "Películas")
                OttTab.Series -> OttVod(vm, "series", "Series")
                OttTab.Anime -> OttVod(vm, "anime", "Animes")
                OttTab.Fav -> OttVod(vm, "fav", "Mi lista")
            }
        }
    }
}

private data class OttNavItem(val tab: OttTab, val label: String, val icon: List<String>)

private val OTT_NAV = listOf(
    OttNavItem(OttTab.Live, "TV en vivo", Ico.live),
    OttNavItem(OttTab.Epg, "EPG", Ico.tv),
    OttNavItem(OttTab.Movie, "Películas", Ico.movie),
    OttNavItem(OttTab.Series, "Series", Ico.series),
    OttNavItem(OttTab.Anime, "Animes", Ico.anime),
)

@Composable
private fun OttTopBar(vm: MainViewModel) {
    val profile by vm.activeProfile.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 720
    var now by remember { mutableStateOf(Date()) }
    var profileMenu by remember { mutableStateOf(false) }
    var navMenu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }

    val dateText = remember(now) { SimpleDateFormat("EEE, d MMM", Locale("es", "MX")).format(now).uppercase(Locale("es", "MX")) }
    val timeText = remember(now) { SimpleDateFormat("h:mm:ss a", Locale.US).format(now) }

    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xF20A111E))
            .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(0.dp))
            .padding(horizontal = if (compact) 12.dp else 28.dp, vertical = if (compact) 8.dp else 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(if (compact) 46.dp else 54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .dlClickable(RoundedCornerShape(8.dp), Color.White, 1.dp, 1.01f) { vm.ottTab = OttTab.Start }
                    .padding(end = 8.dp),
            ) {
                Text(
                    "DavidLG IPTV Player",
                    color = Color.White,
                    fontSize = if (compact) 14.sp else 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Poppins,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!compact) Text("ENTRETENIMIENTO EN UN SOLO LUGAR", color = OttGoldDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.25.sp)
            }
            if (compact) {
                Box {
                    Box(
                        Modifier.padding(start = 4.dp).size(34.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color(0x18FFFFFF)).border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(10.dp))
                            .dlClickable(RoundedCornerShape(10.dp), OttGold, 2.dp, 1.04f) { navMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Text("☰", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                    DropdownMenu(expanded = navMenu, onDismissRequest = { navMenu = false }) {
                        (OTT_NAV + OttNavItem(OttTab.Fav, "Mi lista", Ico.fav)).forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = { navMenu = false; vm.ottTab = item.tab },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = if (compact) 5.dp else 18.dp)) {
                Text(dateText, color = OttGoldDim, fontSize = if (compact) 7.sp else 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .4.sp, maxLines = 1)
                Text(timeText, color = Color.White, fontSize = if (compact) 11.sp else 17.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 1)
            }
            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(13.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(13.dp))
                        .dlClickable(RoundedCornerShape(13.dp), OttGold, 2.dp, 1.03f) { profileMenu = true }
                        .padding(horizontal = if (compact) 5.dp else 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    ProfileAvatar(profile.av, if (compact) 34.dp else 38.dp)
                    if (!compact) {
                        Column {
                            Text(profile.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("CAMBIAR PERFIL", color = OttGoldDim, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                        }
                        Text("⌄", color = OttText, fontSize = 17.sp)
                    }
                }
                DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                    profiles.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ProfileAvatar(item.av, 32.dp)
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.SemiBold)
                                        if (item.id == profile.id) Text("Perfil actual", color = OttGold, fontSize = 10.sp)
                                    }
                                }
                            },
                            onClick = { profileMenu = false; if (item.id != profile.id) vm.selectProfile(item.id) },
                        )
                    }
                    DropdownMenuItem(text = { Text("Administrar perfiles") }, onClick = { profileMenu = false; vm.showProfileSwitcher() })
                }
            }
            Box(
                Modifier.padding(start = 8.dp).size(if (compact) 38.dp else 42.dp)
                    .clip(RoundedCornerShape(13.dp)).background(Color(0x18FFFFFF))
                    .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(13.dp))
                    .dlClickable(RoundedCornerShape(13.dp), OttGold, 2.dp, 1.04f) { vm.screen = Screen.Settings },
                contentAlignment = Alignment.Center,
            ) { SvgIcon(Ico.gear, if (compact) 20.dp else 22.dp, OttText, 1.8f) }
        }

        if (!compact) Row(
            Modifier.fillMaxWidth().padding(top = 11.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OTT_NAV.forEach { item ->
                val selected = vm.ottTab == item.tab
                Row(
                    Modifier.height(if (compact) 39.dp else 43.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) Color(0x3A168BFF) else Color(0x0DFFFFFF))
                        .border(1.dp, if (selected) Color(0x88168BFF) else Color(0x18FFFFFF), RoundedCornerShape(11.dp))
                        .dlClickable(RoundedCornerShape(11.dp), OttGold, 2.dp, 1.03f) { vm.ottTab = item.tab }
                        .padding(horizontal = if (compact) 11.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SvgIcon(item.icon, if (compact) 16.dp else 18.dp, if (selected) Color.White else OttText.copy(alpha = .78f), 1.8f)
                    Text(item.label, color = if (selected) Color.White else OttText.copy(alpha = .84f), fontSize = if (compact) 11.sp else 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontFamily = Poppins, maxLines = 1)
                }
            }
        }
    }
}
