package com.davidlg.iptvplayer.ui.vod

import androidx.compose.runtime.Composable
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.OttTab

/** Pestaña "Películas y series" del estilo clásico (mismo catálogo, paleta azul). */
@Composable
fun ClassicVodTab(vm: MainViewModel) { VodScreen(vm, OttTab.Movie, classic = true) { } }
