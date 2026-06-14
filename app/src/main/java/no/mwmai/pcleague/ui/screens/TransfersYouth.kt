package no.mwmai.pcleague.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.mwmai.pcleague.data.Player
import no.mwmai.pcleague.engine.Game
import no.mwmai.pcleague.ui.GameViewModel
import no.mwmai.pcleague.ui.Screen
import no.mwmai.pcleague.ui.components.*
import no.mwmai.pcleague.ui.theme.Dos
import no.mwmai.pcleague.ui.theme.Mono

@Composable
fun TransfersScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    val me = s.myClub
    var msg by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }   // 0 market, 1 my squad
    var sel by remember { mutableStateOf<Int?>(null) }

    val market = s.transferList.mapNotNull { s.findPlayer(it) }
        .filter { p -> s.ownerOf(p.id)?.id != me.id }
        .sortedByDescending { it.skill }
    val mine = me.roster.sortedByDescending { it.skill }

    ScreenScaffold(status = "${vm.t("budget")}: £${Game.fmt(me.budget)}") {
        Row {
            DosButton(vm.t("on_market"), color = if (tab == 0) Dos.Yellow else Dos.BrightCyan,
                modifier = Modifier.weight(1f)) { tab = 0; sel = null }
            Spacer(Modifier.width(8.dp))
            DosButton(vm.t("your_squad"), color = if (tab == 1) Dos.Yellow else Dos.BrightCyan,
                modifier = Modifier.weight(1f)) { tab = 1; sel = null }
        }
        Spacer(Modifier.height(6.dp))
        DosFrame(if (tab == 0) vm.t("transfers") else vm.t("sell"), Modifier.weight(1f), fill = true) {
            val list = if (tab == 0) market else mine
            LazyColumn(Modifier.weight(1f)) {
                items(list.size) { i ->
                    val p = list[i]
                    val owner = s.ownerOf(p.id)
                    Column {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (sel == p.id) Dos.Green else Color.Transparent)
                                .clickable { sel = if (sel == p.id) null else p.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            cell(vm.roleLabel(p.role), 36.dp,
                                when (p.role) { "GK" -> Dos.Yellow; "DEF" -> Dos.BrightCyan
                                    "MID" -> Dos.BrightGreen; else -> Dos.BrightMagenta }, true)
                            Text(p.name, color = if (sel == p.id) Dos.White else Dos.Text,
                                fontFamily = Mono, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            cell("${p.age}", 28.dp)
                            cell("${p.skill}", 26.dp, Dos.BrightGreen)
                            cell("£${Game.fmt(Game.priceFor(p))}", 64.dp, Dos.Yellow)
                        }
                        if (sel == p.id) {
                            Column(Modifier.fillMaxWidth().background(Dos.Black).padding(8.dp)) {
                                if (tab == 0) {
                                    Text("${owner?.name ?: "?"}  ·  ${vm.t("value")} £${Game.fmt(Game.priceFor(p))}",
                                        color = Dos.LightGray, fontFamily = Mono, fontSize = 12.sp)
                                    Spacer(Modifier.height(6.dp))
                                    DosButton("${vm.t("buy")} £${Game.fmt(Game.priceFor(p))}",
                                        color = Dos.BrightGreen) {
                                        msg = vm.buy(p.id, Game.priceFor(p)) ?: "${vm.t("buy")}: ${p.name}"
                                        sel = null
                                    }
                                } else {
                                    DosButton("${vm.t("sell")} £${Game.fmt(Game.priceFor(p))}",
                                        color = Dos.BrightRed) {
                                        msg = vm.sell(p.id) ?: "${vm.t("sell")}: ${p.name}"
                                        sel = null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (msg.isNotEmpty()) Text(msg, color = Dos.BrightGreen, fontFamily = Mono, fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(6.dp))
        DosButton("◀ ${vm.t("back")}") { nav(Screen.Office) }
    }
}

@Composable
fun YouthScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    val me = s.myClub
    var msg by remember { mutableStateOf("") }
    var sel by remember { mutableStateOf<Int?>(null) }
    val youth = me.youth.sortedByDescending { it.maxSkill }

    ScreenScaffold(status = "${vm.t("youth")}  ·  ${youth.size}/16") {
        DosFrame(vm.t("youth"), Modifier.weight(1f), fill = true) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                cell(vm.t("pos"), 36.dp, Dos.Yellow, true)
                cellW(vm.t("name"), 1f, this, Dos.Yellow, true)
                cell(vm.t("age"), 30.dp, Dos.Yellow, true)
                cell(vm.t("sh"), 28.dp, Dos.Yellow, true)
                cell("MAX", 34.dp, Dos.Yellow, true)
            }
            LazyColumn(Modifier.weight(1f)) {
                items(youth.size) { i ->
                    val p = youth[i]
                    Column {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (sel == p.id) Dos.Green else Color.Transparent)
                                .clickable { sel = if (sel == p.id) null else p.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            cell(vm.roleLabel(p.role), 36.dp,
                                when (p.role) { "GK" -> Dos.Yellow; "DEF" -> Dos.BrightCyan
                                    "MID" -> Dos.BrightGreen; else -> Dos.BrightMagenta }, true)
                            Text(p.name, color = if (sel == p.id) Dos.White else Dos.Text,
                                fontFamily = Mono, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            cell("${p.age}", 30.dp)
                            cell("${p.skill}", 28.dp, Dos.BrightGreen)
                            cell("${p.maxSkill}", 34.dp, Dos.Yellow)
                        }
                        if (sel == p.id) Column(Modifier.fillMaxWidth().background(Dos.Black).padding(8.dp)) {
                            Text("Potential ${p.skill} → ${p.maxSkill}", color = Dos.LightGray,
                                fontFamily = Mono, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            DosButton(vm.t("promote"), color = Dos.BrightGreen) {
                                msg = vm.promoteYouth(p.id) ?: "${vm.t("promote")}: ${p.name}"
                                sel = null
                            }
                        }
                    }
                }
            }
        }
        if (msg.isNotEmpty()) Text(msg, color = Dos.BrightGreen, fontFamily = Mono, fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(6.dp))
        DosButton("◀ ${vm.t("back")}") { nav(Screen.Office) }
    }
}
