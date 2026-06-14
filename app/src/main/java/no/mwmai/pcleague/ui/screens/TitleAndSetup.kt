package no.mwmai.pcleague.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.mwmai.pcleague.ui.GameViewModel
import no.mwmai.pcleague.ui.Screen
import no.mwmai.pcleague.ui.components.*
import no.mwmai.pcleague.ui.theme.Dos
import no.mwmai.pcleague.ui.theme.Mono

@Composable
fun TitleScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    ScreenScaffold(status = "PcLeague 2.1  ·  (c) Asle Rokstad 1994  ·  Android recreation") {
        Spacer(Modifier.height(24.dp))
        Text("┌────────────────────────┐", color = Dos.Frame, fontFamily = Mono,
            fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(vm.t("title"), color = Dos.Yellow, fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 40.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(vm.t("subtitle"), color = Dos.BrightGreen, fontFamily = Mono,
            fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("└────────────────────────┘", color = Dos.Frame, fontFamily = Mono,
            fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        DosFrame(vm.t("title"), Modifier.padding(horizontal = 12.dp)) {
            MenuRow("1", vm.t("new_game"), accent = true) { nav(Screen.NewGame) }
            if (vm.hasAutosave()) MenuRow("2", vm.t("continue")) {
                if (vm.continueGame()) nav(Screen.Office)
            }
            MenuRow("3", vm.t("load")) { nav(Screen.SaveLoad) }
            MenuRow("4", vm.t("settings")) { nav(Screen.Settings) }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            DosButton("English", color = if (vm.lang == "en") Dos.Yellow else Dos.BrightCyan,
                modifier = Modifier.weight(1f)) { vm.setLang("en") }
            Spacer(Modifier.width(8.dp))
            DosButton("Norsk", color = if (vm.lang == "no") Dos.Yellow else Dos.BrightCyan,
                modifier = Modifier.weight(1f)) { vm.setLang("no") }
        }
    }
}

@Composable
fun NewGameScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    var manager by remember { mutableStateOf("") }
    var selectedClub by remember { mutableStateOf(-1) }
    var divFilter by remember { mutableStateOf(0) }
    val ds = vm.dataset

    ScreenScaffold(status = "${vm.t("select_club")}  ·  ${vm.t("back")}: ◀") {
        DosFrame(vm.t("new_game")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${vm.t("manager_name")}: ", color = Dos.Text, fontFamily = Mono, fontSize = 14.sp)
                Box(Modifier.weight(1f).background(Dos.Black).padding(8.dp)) {
                    BasicTextField(
                        value = manager, onValueChange = { manager = it.take(18) },
                        textStyle = TextStyle(color = Dos.BrightGreen, fontFamily = Mono, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Dos.BrightGreen),
                        singleLine = true,
                    )
                    if (manager.isEmpty()) Text("…", color = Dos.DarkGray, fontFamily = Mono)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            (0..3).forEach { d ->
                DosButton(listOf("PR", "D1", "D2", "D3")[d],
                    color = if (divFilter == d) Dos.Yellow else Dos.BrightCyan,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)) { divFilter = d }
            }
        }
        Spacer(Modifier.height(6.dp))
        DosFrame(vm.divName(divFilter), Modifier.weight(1f), fill = true) {
            val teams = ds.divisions[divFilter].teamIds.map { ds.teams[it] }
            LazyColumn(Modifier.weight(1f)) {
                items(teams.size) { i ->
                    val tm = teams[i]
                    Row(
                        Modifier.fillMaxWidth().background(
                            if (selectedClub == tm.id) Dos.Green else Dos.Panel)
                            .clickable { selectedClub = tm.id }.padding(vertical = 8.dp, horizontal = 6.dp),
                    ) {
                        Text(tm.name, color = if (selectedClub == tm.id) Dos.White else Dos.Text,
                            fontFamily = Mono, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("${tm.players.size}", color = Dos.LightGray, fontFamily = Mono, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            DosButton("◀ ${vm.t("back")}", modifier = Modifier.weight(1f)) { nav(Screen.Title) }
            Spacer(Modifier.width(8.dp))
            DosButton(vm.t("start"), enabled = selectedClub >= 0, color = Dos.BrightGreen,
                modifier = Modifier.weight(1f)) {
                vm.newGame(selectedClub, manager); nav(Screen.Office)
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    ScreenScaffold(status = vm.t("settings")) {
        DosFrame(vm.t("settings")) {
            Text(vm.t("language"), color = Dos.Yellow, fontFamily = Mono, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row {
                DosButton("English", color = if (vm.lang == "en") Dos.Yellow else Dos.BrightCyan,
                    modifier = Modifier.weight(1f)) { vm.setLang("en") }
                Spacer(Modifier.width(8.dp))
                DosButton("Norsk", color = if (vm.lang == "no") Dos.Yellow else Dos.BrightCyan,
                    modifier = Modifier.weight(1f)) { vm.setLang("no") }
            }
            Spacer(Modifier.height(14.dp))
            Text("PcLeague 2.1", color = Dos.White, fontFamily = Mono, fontWeight = FontWeight.Bold)
            Text("Original (c) Asle Rokstad, PcLeague SoftWare 1994.", color = Dos.Text,
                fontFamily = Mono, fontSize = 12.sp)
            Text("Faithful Android recreation with the original", color = Dos.Text,
                fontFamily = Mono, fontSize = 12.sp)
            Text("database: 92 clubs, 1733 players, 4 divisions.", color = Dos.Text,
                fontFamily = Mono, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        DosButton("◀ ${vm.t("back")}") {
            nav(if (vm.state == null) Screen.Title else Screen.Office)
        }
    }
}

@Composable
fun SaveLoadScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    var msg by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    val slots = remember(refresh) { vm.slots() }
    val inGame = vm.state != null

    ScreenScaffold(status = if (inGame) "${vm.t("save")} / ${vm.t("load")}" else vm.t("load")) {
        DosFrame(if (inGame) "${vm.t("save")} / ${vm.t("load")}" else vm.t("load")) {
            slots.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.slot + 1}", color = Dos.Yellow, fontFamily = Mono,
                        fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                    Text(if (s.exists) s.label else vm.t("empty"), color = Dos.Text,
                        fontFamily = Mono, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (inGame) {
                        DosButton(vm.t("save"), color = Dos.BrightGreen) {
                            vm.saveSlot(s.slot); msg = vm.t("saved"); refresh++
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    DosButton(vm.t("load"), enabled = s.exists) {
                        if (vm.loadSlot(s.slot)) nav(Screen.Office) else msg = vm.t("no_save")
                    }
                    if (s.exists) {
                        Spacer(Modifier.width(6.dp))
                        DosButton("X", color = Dos.BrightRed) { vm.deleteSlot(s.slot); refresh++ }
                    }
                }
            }
        }
        if (msg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(msg, color = Dos.BrightGreen, fontFamily = Mono)
        }
        Spacer(Modifier.height(10.dp))
        DosButton("◀ ${vm.t("back")}") { nav(if (inGame) Screen.Office else Screen.Title) }
    }
}

@Composable
fun HistoryScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: return
    ScreenScaffold(status = vm.t("history")) {
        DosFrame(vm.t("inbox"), Modifier.weight(1f), fill = true) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                s.inbox.takeLast(40).reversed().forEach {
                    Text("• $it", color = Dos.Text, fontFamily = Mono, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
                if (s.history.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(vm.t("history"), color = Dos.Yellow, fontFamily = Mono, fontWeight = FontWeight.Bold)
                    s.history.reversed().forEach {
                        Text(it, color = Dos.BrightCyan, fontFamily = Mono, fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        DosButton("◀ ${vm.t("back")}") { nav(Screen.Office) }
    }
}
