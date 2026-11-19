package com.davidlg.iptvplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.Screen
import com.davidlg.iptvplayer.ui.classic.ClassicShell
import com.davidlg.iptvplayer.ui.ott.OttShell
import com.davidlg.iptvplayer.ui.player.PlayerScreen
import com.davidlg.iptvplayer.ui.profiles.ProfilesScreen
import com.davidlg.iptvplayer.ui.settings.SettingsScreen

@Composable
fun App(vm: MainViewModel) {
    val vs by vm.vodSettings.collectAsState()
    when (vm.screen) {
        Screen.Profiles -> ProfilesScreen(vm)
        Screen.Player -> PlayerScreen(vm)
        Screen.Settings -> SettingsScreen(vm) { vm.screen = Screen.Main }
        Screen.Main -> if (vs.str("style", "ott") == "classic") ClassicShell(vm) else OttShell(vm)
    }
}
