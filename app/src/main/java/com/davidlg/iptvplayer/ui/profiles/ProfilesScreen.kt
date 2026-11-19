package com.davidlg.iptvplayer.ui.profiles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.Screen
import com.davidlg.iptvplayer.data.AppPrefs
import com.davidlg.iptvplayer.data.Profile
import com.davidlg.iptvplayer.ui.Sans
import com.davidlg.iptvplayer.ui.dlClickable
import kotlinx.coroutines.delay

private val PfAcc = Color(0xFFE0C38A)   // acento dorado del modo OTT (--acc-rgb)
private val PfDim = Color(0xFF8F9BAB)

private sealed interface PfDialog {
    data class Edit(val p: Profile?) : PfDialog
    data class Confirm(val p: Profile) : PfDialog
}

/** Pantalla "¿Quién está viendo?" — réplica de profiles.js / profiles.css. */
@Composable
fun ProfilesScreen(vm: MainViewModel) {
    val profiles by vm.profiles.collectAsState()
    val active by vm.activeProfile.collectAsState()
    var manage by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<PfDialog?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var leaving by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }
    val compact = LocalConfiguration.current.screenWidthDp < 900

    // Atrás: cierra diálogo → sale del modo administrar → (si se abrió desde la app) vuelve.
    BackHandler(enabled = dialog != null || manage || vm.profileMode == "switch") {
        when {
            dialog != null -> dialog = null
            manage -> manage = false
            else -> vm.screen = Screen.Main
        }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(3200); toast = null } }
    LaunchedEffect(dialog == null) { if (dialog == null) runCatching { firstFocus.requestFocus() } }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF1B2140), Color(0xFF0A0F1E), Color(0xFF05070D)),
                center = Offset(960f, 0f), radius = 1500f,
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.fillMaxWidth().then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = if (compact) 14.dp else 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "¡Hola!", color = PfAcc, fontSize = if (compact) 18.sp else 34.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans,
                letterSpacing = 1.4.sp, modifier = Modifier.then(if (manage || leaving) Modifier.height(0.dp) else Modifier),
            )
            Text(
                if (leaving) "Cambiando de perfil…" else if (manage) "Administrar perfiles" else "¿Quién está viendo?",
                color = Color(0xFFF4F6F9), fontSize = if (compact) 30.sp else 84.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = if (compact) 28.dp else 84.dp)
                    .height(if (compact) 180.dp else 300.dp)
                    .horizontalScroll(rememberScrollState())
                    .wrapContentWidth(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 54.dp),
                verticalAlignment = Alignment.Top,
            ) {
                profiles.forEachIndexed { i, p ->
                    PfCard(
                        p.name, p.av, manage, p.id == active.id,
                        if (p.id == active.id) Modifier.focusRequester(firstFocus) else Modifier, compact,
                    ) {
                        if (manage) dialog = PfDialog.Edit(p)
                        else if (p.id == active.id) vm.selectProfile(p.id)
                        else { leaving = true; vm.selectProfile(p.id) }
                    }
                }
                if (profiles.size < AppPrefs.MAX_PROFILES) PfAddCard(compact) { dialog = PfDialog.Edit(null) }
            }
            var mfoc by remember { mutableStateOf(false) }
            Box(
                Modifier.padding(top = if (compact) 20.dp else 96.dp).widthIn(min = if (compact) 190.dp else 420.dp).height(if (compact) 50.dp else 84.dp)
                    .dlClickable(RoundedCornerShape(12.dp), Color.White, 0.dp, 1f, { mfoc = it }) { manage = !manage }
                    .background(if (mfoc) Color.White else Color.Transparent)
                    .pfManageStyle(mfoc),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (if (manage) "Listo" else "Administrar perfiles").uppercase(),
                    color = if (mfoc) Color(0xFF111111) else Color(0xBFFFFFFF), fontSize = if (compact) 15.sp else 31.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans,
                    letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = if (compact) 18.dp else 54.dp),
                )
            }
        }

        dialog?.let { d ->
            Dialog(
                onDismissRequest = { dialog = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false,
                ),
            ) {
                Box(
                    Modifier.fillMaxSize().background(Color(0xC7000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    when (d) {
                        is PfDialog.Edit -> EditDialog(
                            vm, d.p, profiles,
                            onDone = { dialog = null },
                            onToast = { toast = it },
                            onDelete = { p -> dialog = PfDialog.Confirm(p) },
                        )
                        is PfDialog.Confirm -> ConfirmDialog(
                            d.p,
                            onCancel = { dialog = null },
                            onDelete = { vm.deleteProfile(d.p.id); dialog = null; toast = "Perfil eliminado" },
                        )
                    }
                }
            }
        }
        toast?.let {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = if (compact) 16.dp else 64.dp).widthIn(max = 1100.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A222D))
                    .border(1.dp, PfAcc.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = if (compact) 12.dp else 40.dp, vertical = if (compact) 10.dp else 20.dp)
            ) { Text(it, color = Color.White, fontSize = if (compact) 12.sp else 27.sp, fontFamily = Sans, textAlign = TextAlign.Center) }
        }
    }
}

private fun Modifier.pfManageStyle(focused: Boolean): Modifier =
    this.border(3.dp, if (focused) Color.White else Color(0x59FFFFFF), RoundedCornerShape(12.dp))

@Composable
private fun PfCard(name: String, av: Int, manage: Boolean, isActive: Boolean, extra: Modifier, compact: Boolean = false, onClick: () -> Unit) {
    val cardSize = if (compact) 116.dp else 212.dp
    var foc by remember { mutableStateOf(false) }
    Column(Modifier.width(cardSize), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(cardSize).then(extra)
                .dlClickable(RoundedCornerShape(18.dp), Color.White, 6.dp, 1.1f, { foc = it }, onClick),
        ) {
            ProfileAvatar(av, cardSize)
            if (manage) {
                Box(Modifier.fillMaxSize().background(Color(0x73000000)))
                Box(
                    Modifier.align(Alignment.Center).size(if (compact) 44.dp else 84.dp).clip(RoundedCornerShape(50))
                        .background(Color(0x8C000000)).border(3.dp, Color.White, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) { Text("✎", color = Color.White, fontSize = if (compact) 22.sp else 44.sp) }
            }
        }
        Text(
            name, color = if (foc) Color.White else PfDim, fontSize = if (compact) 14.sp else 31.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (compact) 8.dp else 24.dp).widthIn(max = 230.dp),
        )
    }
}

@Composable
private fun PfAddCard(compact: Boolean = false, onClick: () -> Unit) {
    val cardSize = if (compact) 116.dp else 212.dp
    var foc by remember { mutableStateOf(false) }
    Column(Modifier.width(cardSize), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(cardSize)
                .dlClickable(RoundedCornerShape(18.dp), Color.White, 4.dp, 1.1f, { foc = it }, onClick)
                .background(if (foc) Color(0x1FFFFFFF) else Color(0x0FFFFFFF))
                .drawBehind {
                    if (!foc) drawRoundRect(
                        Color(0x47FFFFFF), cornerRadius = CornerRadius(18f), size = Size(size.width, size.height),
                        style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))),
                    )
                },
            contentAlignment = Alignment.Center,
        ) { Text("+", color = if (foc) Color.White else Color(0x8CFFFFFF), fontSize = if (compact) 60.sp else 120.sp, fontWeight = FontWeight.Light, fontFamily = Sans) }
        Text(
            "Añadir perfil", color = if (foc) Color.White else PfDim, fontSize = if (compact) 14.sp else 31.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = Sans, modifier = Modifier.padding(top = if (compact) 8.dp else 24.dp),
        )
    }
}

@Composable
private fun PfDlgBox(width: Int, content: @Composable () -> Unit) {
    val compact = LocalConfiguration.current.screenWidthDp < 900
    Column(
        Modifier.fillMaxWidth(if (compact) 0.96f else 1f).widthIn(max = width.dp).heightIn(max = 780.dp)
            .verticalScroll(rememberScrollState()).clip(RoundedCornerShape(24.dp)).background(Color(0xFF10151C))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
            .padding(start = if (compact) 14.dp else 64.dp, end = if (compact) 14.dp else 64.dp, top = if (compact) 16.dp else 56.dp, bottom = if (compact) 16.dp else 46.dp)
    ) { content() }
}

@Composable
private fun PfButton(text: String, kind: String = "", modifier: Modifier = Modifier, onClick: () -> Unit) {
    val compact = LocalConfiguration.current.screenWidthDp < 900
    var foc by remember { mutableStateOf(false) }
    val bg = when {
        foc && kind == "danger" -> Color(0xFFFF6E6E)
        foc -> Color.White
        kind == "pri" -> PfAcc.copy(alpha = 0.25f)
        kind == "danger" -> Color(0x33FF6464)
        else -> Color(0x1AFFFFFF)
    }
    val fg = when {
        foc && kind == "danger" -> Color(0xFF2A0707)
        foc -> Color(0xFF111111)
        kind == "danger" -> Color(0xFFFFB3B3)
        else -> Color.White
    }
    Box(
        modifier.widthIn(min = if (compact) 0.dp else 230.dp).height(if (compact) 48.dp else 80.dp).dlClickable(RoundedCornerShape(14.dp), Color.Transparent, 0.dp, 1f, { foc = it }, onClick)
            .background(bg).padding(horizontal = if (compact) 10.dp else 36.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontSize = if (compact) 12.sp else 30.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans) }
}

@Composable
private fun EditDialog(
    vm: MainViewModel, p: Profile?, all: List<Profile>,
    onDone: () -> Unit, onToast: (String) -> Unit, onDelete: (Profile) -> Unit,
) {
    val config = LocalConfiguration.current
    val compact = config.screenWidthDp < 900
    var name by remember { mutableStateOf(p?.name ?: "") }
    var av by remember { mutableStateOf(p?.av ?: (all.size % PROFILE_AVATAR_COUNT)) }
    val active by vm.activeProfile.collectAsState()
    val canDelete = p != null && p.id != "p1" && p.id != active.id
    val note = when {
        p?.id == "p1" -> "El perfil principal no se puede eliminar."
        p != null && p.id == active.id -> "Estás usando este perfil; cambia a otro para poder eliminarlo."
        else -> ""
    }
    var fieldFocused by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(180)
        runCatching { nameFocus.requestFocus() }
        if (compact) keyboard?.show()
    }

    // El diálogo usa una ventana que se redimensiona con el teclado. El contenido
    // puede desplazarse, pero la fila de acciones queda fija fuera del scroll.
    BoxWithConstraints(
        Modifier.fillMaxWidth(if (compact) .96f else 1f)
            .widthIn(max = 1180.dp)
            .imePadding()
            .fillMaxHeight(if (compact) .94f else .86f)
            .heightIn(max = (config.screenHeightDp - 24).coerceAtLeast(280).dp)
    ) {
        val reservedForActions = if (compact) 154.dp else 210.dp
        val bodyMaxHeight = (maxHeight - reservedForActions).coerceAtLeast(110.dp)
        val actionWidth = if (compact) maxOf(110, (config.screenWidthDp - 100) / 2).dp else 230.dp

        Column(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF10151C))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = if (compact) 14.dp else 64.dp, vertical = if (compact) 14.dp else 34.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (p == null) "Añadir perfil" else "Editar perfil",
                color = Color.White, fontSize = if (compact) 24.sp else 48.sp,
                fontWeight = FontWeight.Bold, fontFamily = Sans,
            )
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = bodyMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(top = if (compact) 12.dp else 26.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 56.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(if (compact) 76.dp else 190.dp).clip(RoundedCornerShape(18.dp))) {
                        ProfileAvatar(av, if (compact) 76.dp else 190.dp)
                    }
                    Column(Modifier.fillMaxWidth()) {
                        Text("NOMBRE", color = PfDim, fontSize = if (compact) 12.sp else 23.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, letterSpacing = 3.2.sp)
                        Box(
                            Modifier.padding(top = 6.dp, bottom = if (compact) 12.dp else 24.dp)
                                .fillMaxWidth().height(if (compact) 48.dp else 76.dp)
                                .clip(RoundedCornerShape(14.dp)).background(Color(0x4D000000))
                                .border(2.dp, if (fieldFocused) Color.White else Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = if (compact) 10.dp else 26.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            BasicTextField(
                                value = name, onValueChange = { if (it.length <= 14) name = it }, singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = if (compact) 16.sp else 30.sp, fontFamily = Sans),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth().focusRequester(nameFocus).onFocusChanged {
                                    fieldFocused = it.isFocused
                                    if (it.isFocused && compact) keyboard?.show()
                                    if (!it.isFocused) keyboard?.hide()
                                },
                                decorationBox = { inner ->
                                    if (name.isEmpty()) Text("Nombre del perfil", color = Color(0x59FFFFFF), fontSize = if (compact) 15.sp else 30.sp, fontFamily = Sans)
                                    inner()
                                },
                            )
                        }
                        Text("AVATAR", color = PfDim, fontSize = if (compact) 12.sp else 23.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, letterSpacing = 3.2.sp)
                        Column(Modifier.padding(top = 7.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 14.dp)) {
                            for (row in 0 until 2) Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 16.dp)) {
                                for (i in row * 4 until row * 4 + 4) {
                                    var foc by remember { mutableStateOf(false) }
                                    Box(
                                        Modifier.size(if (compact) 42.dp else 82.dp)
                                            .dlClickable(RoundedCornerShape(14.dp), Color.White, 3.dp, 1.08f, { foc = it }) { av = i },
                                    ) {
                                        ProfileAvatar(i, if (compact) 42.dp else 82.dp)
                                        if (i != av && !foc) Box(Modifier.fillMaxSize().background(Color(0x4D000000)))
                                        if (i == av) Box(Modifier.fillMaxSize().border(3.dp, if (foc) Color.White else PfAcc, RoundedCornerShape(14.dp)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(top = if (compact) 8.dp else 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 20.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PfButton("Guardar", "pri", Modifier.width(actionWidth)) {
                        keyboard?.hide()
                        val n = name.replace(Regex("\\s+"), " ").trim()
                        when {
                            n.isEmpty() -> onToast("Escribe un nombre para el perfil")
                            all.any { it.id != p?.id && it.name.equals(n, ignoreCase = true) } -> onToast("Ya existe un perfil con ese nombre")
                            else -> {
                                if (p != null) { vm.editProfile(p.id, n, av); onDone() }
                                else if (vm.addProfile(n, av)) { onToast("Perfil creado"); onDone() }
                                else onToast("No se pudo crear el perfil. Comprueba el límite de perfiles.")
                            }
                        }
                    }
                    PfButton("Cancelar", "", Modifier.width(actionWidth)) {
                        keyboard?.hide()
                        onDone()
                    }
                    if (canDelete && p != null && !compact) PfButton("Eliminar perfil", "danger") { onDelete(p) }
                }
                if (canDelete && p != null && compact) {
                    PfButton("Eliminar perfil", "danger", Modifier.fillMaxWidth()) { keyboard?.hide(); onDelete(p) }
                }
                if (note.isNotBlank()) Text(note, color = PfDim, fontSize = if (compact) 10.sp else 18.sp, fontFamily = Sans, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ConfirmDialog(p: Profile, onCancel: () -> Unit, onDelete: () -> Unit) {
    val compact = LocalConfiguration.current.screenWidthDp < 900
    PfDlgBox(860) {
        Text("Eliminar perfil", color = Color.White, fontSize = if (compact) 24.sp else 48.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
        Text(
            "¿Eliminar «${p.name}»? Se borrarán sus favoritos, su historial y sus preferencias.",
            color = Color(0xCCFFFFFF), fontSize = if (compact) 14.sp else 29.sp, fontFamily = Sans, lineHeight = if (compact) 20.sp else 43.sp, modifier = Modifier.padding(top = if (compact) 12.dp else 34.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = if (compact) 16.dp else 40.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 20.dp, Alignment.End)) {
            PfButton("Eliminar", "danger") { onDelete() }
            PfButton("Cancelar", "pri") { onCancel() }
        }
    }
}
