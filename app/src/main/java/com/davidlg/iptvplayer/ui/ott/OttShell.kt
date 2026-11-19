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
import androidx.compose.ui.platform.LocalConfiguration
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

/** Contenedor y navegación exclusivos de OTT Nuevo. ClassicShell permanece independiente. */
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
    val width = LocalConfiguration.current.screenWidthDp
    val phone = width < 600
    val desktop = width >= 1180
    var now by remember { mutableStateOf(Date()) }
    var profileMenu by remember { mutableStateOf(false) }
    var navMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }

    val dateText = remember(now) {
        SimpleDateFormat("EEE d MMM", Locale("es", "MX")).format(now).uppercase(Locale("es", "MX"))
    }
    val timeText = remember(now) { SimpleDateFormat("h:mm:ss a", Locale.US).format(now).uppercase(Locale.US) }

    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xF20A111E))
            .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(0.dp))
            .padding(horizontal = if (phone) 9.dp else if (desktop) 28.dp else 18.dp, vertical = if (phone) 7.dp else 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(if (phone) 42.dp else 54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .dlClickable(RoundedCornerShape(8.dp), Color.White, 2.dp, 1.02f) { vm.ottTab = OttTab.Start }
                    .padding(end = 7.dp),
            ) {
                Text(
                    "DavidLG IPTV Player",
                    color = Color.White,
                    fontSize = if (phone) 12.sp else if (desktop) 21.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Poppins,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!phone) Text(
                    "ENTRETENIMIENTO EN UN SOLO LUGAR",
                    color = OttGoldDim,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.15.sp,
                    maxLines = 1,
                )
            }

            if (phone) {
                Box {
                    Box(
                        Modifier.padding(start = 3.dp).size(32.dp).clip(RoundedCornerShape(9.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(9.dp))
                            .dlClickable(RoundedCornerShape(9.dp), OttGold, 2.dp, 1.04f) { navMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Text("☰", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    DropdownMenu(expanded = navMenu, onDismissRequest = { navMenu = false }) {
                        DropdownMenuItem(text = { Text("Inicio") }, onClick = { navMenu = false; vm.ottTab = OttTab.Start })
                        OTT_NAV.forEach { item ->
                            DropdownMenuItem(text = { Text(item.label) }, onClick = { navMenu = false; vm.ottTab = item.tab })
                        }
                        DropdownMenuItem(text = { Text("Mi lista") }, onClick = { navMenu = false; vm.ottTab = OttTab.Fav })
                    }
                }
            } else if (desktop) {
                Spacer(Modifier.width(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OTT_NAV.forEach { item -> OttNavButton(item, vm, compact = false) }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = if (phone) 5.dp else 14.dp),
            ) {
                Text(
                    dateText,
                    color = OttGoldDim,
                    fontSize = if (phone) 7.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = .35.sp,
                    maxLines = 1,
                )
                Text(
                    timeText,
                    color = Color.White,
                    fontSize = if (phone) 9.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins,
                    maxLines = 1,
                )
            }

            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(12.dp))
                        .dlClickable(RoundedCornerShape(12.dp), OttGold, 2.dp, 1.03f) { profileMenu = true }
                        .padding(horizontal = if (phone) 4.dp else 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ProfileAvatar(profile.av, if (phone) 30.dp else 36.dp)
                    if (!phone) {
                        Column {
                            Text(profile.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("CAMBIAR PERFIL", color = OttGoldDim, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = .55.sp)
                        }
                        Text("⌄", color = OttText, fontSize = 16.sp)
                    }
                }
                DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                    profiles.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
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
                    DropdownMenuItem(text = { Text("Mi lista") }, onClick = { profileMenu = false; vm.ottTab = OttTab.Fav })
                    DropdownMenuItem(text = { Text("Administrar perfiles") }, onClick = { profileMenu = false; vm.showProfileSwitcher() })
                }
            }

            Box(
                Modifier.padding(start = if (phone) 5.dp else 8.dp).size(if (phone) 34.dp else 40.dp)
                    .clip(RoundedCornerShape(11.dp)).background(Color(0x18FFFFFF))
                    .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(11.dp))
                    .dlClickable(RoundedCornerShape(11.dp), OttGold, 2.dp, 1.04f) { vm.screen = Screen.Settings },
                contentAlignment = Alignment.Center,
            ) { SvgIcon(Ico.gear, if (phone) 18.dp else 21.dp, OttText, 1.8f) }
        }

        if (!desktop && !phone) {
            Row(
                Modifier.fillMaxWidth().padding(top = 9.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OTT_NAV.forEach { item -> OttNavButton(item, vm, compact = true) }
            }
        }
    }
}

@Composable
private fun OttNavButton(item: OttNavItem, vm: MainViewModel, compact: Boolean) {
    val selected = vm.ottTab == item.tab
    val shape = RoundedCornerShape(11.dp)
    Row(
        Modifier.height(if (compact) 39.dp else 44.dp)
            .clip(shape)
            .background(if (selected) Color(0x3A168BFF) else Color(0x0DFFFFFF))
            .border(1.dp, if (selected) Color(0x88168BFF) else Color(0x18FFFFFF), shape)
            .dlClickable(shape, Color.White, 2.dp, 1.035f) { vm.ottTab = item.tab }
            .padding(horizontal = if (compact) 12.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SvgIcon(item.icon, if (compact) 15.dp else 17.dp, if (selected) Color.White else OttText.copy(alpha = .78f), 1.8f)
        Text(
            item.label,
            color = if (selected) Color.White else OttText.copy(alpha = .84f),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = Poppins,
            maxLines = 1,
        )
    }
}
