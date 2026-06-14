package no.mwmai.pcleague.ui.screens

import androidx.compose.foundation.background
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
import no.mwmai.pcleague.engine.Cup
import no.mwmai.pcleague.engine.Game
import no.mwmai.pcleague.ui.GameViewModel
import no.mwmai.pcleague.ui.Screen
import no.mwmai.pcleague.ui.components.*
import no.mwmai.pcleague.ui.theme.Dos
import no.mwmai.pcleague.ui.theme.Mono

@Composable
fun TablesScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    var tier by remember { mutableStateOf(s.myClub.division) }
    val table = vm.standings(tier)
    val promoZone = if (tier > 0) 3 else 0
    val relZone = if (tier < 3) table.size - 3 else table.size

    ScreenScaffold(status = vm.divName(tier)) {
        Row {
            (0..3).forEach { d ->
                DosButton(listOf("PR", "D1", "D2", "D3")[d],
                    color = if (tier == d) Dos.Yellow else Dos.BrightCyan,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)) { tier = d }
            }
        }
        Spacer(Modifier.height(6.dp))
        DosFrame(vm.divName(tier), Modifier.weight(1f), fill = true) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                cell("", 22.dp)
                cellW(vm.t("name"), 1f, this, Dos.Yellow, true)
                cell(vm.t("p"), 24.dp, Dos.Yellow, true)
                cell(vm.t("w"), 22.dp, Dos.Yellow, true)
                cell(vm.t("d"), 22.dp, Dos.Yellow, true)
                cell(vm.t("l"), 22.dp, Dos.Yellow, true)
                cell(vm.t("gd"), 30.dp, Dos.Yellow, true)
                cell(vm.t("pts"), 30.dp, Dos.Yellow, true)
            }
            LazyColumn(Modifier.weight(1f)) {
                items(table.size) { i ->
                    val c = table[i]
                    val mine = c.id == s.clubId
                    val zoneColor = when {
                        mine -> Dos.Green
                        i < promoZone -> Color(0xFF06420F)
                        i >= relZone -> Color(0xFF4A0606)
                        else -> Color.Transparent
                    }
                    Row(Modifier.fillMaxWidth().background(zoneColor).padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        cell("${i + 1}", 22.dp, if (i < promoZone) Dos.BrightGreen
                            else if (i >= relZone) Dos.BrightRed else Dos.LightGray, true)
                        Text(c.name, color = if (mine) Dos.White else Dos.Text, fontFamily = Mono,
                            fontSize = 12.sp, fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1, modifier = Modifier.weight(1f))
                        cell("${c.played}", 24.dp)
                        cell("${c.won}", 22.dp)
                        cell("${c.drawn}", 22.dp)
                        cell("${c.lost}", 22.dp)
                        cell("${if (c.gd >= 0) "+" else ""}${c.gd}", 30.dp)
                        cell("${c.points}", 30.dp, Dos.BrightGreen, true)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        DosButton("◀ ${vm.t("back")}") { nav(Screen.Office) }
    }
}

@Composable
fun FixturesScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    val me = s.myClub
    val mine = s.fixtures.filter { (it.home == me.id || it.away == me.id) && it.division == me.division }
        .sortedBy { it.round }

    ScreenScaffold(status = "${vm.t("fixtures")}  ·  ${me.name}") {
        DosFrame(vm.t("fixtures"), Modifier.weight(1f), fill = true) {
            LazyColumn {
                items(mine.size) { i ->
                    val f = mine[i]
                    val home = f.home == me.id
                    val opp = s.club(if (home) f.away else f.home).name
                    val current = f.round == s.matchday
                    Row(Modifier.fillMaxWidth()
                        .background(if (current) Dos.Green else Color.Transparent)
                        .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        cell("${f.round + 1}", 30.dp, Dos.Yellow, true)
                        cell(if (home) "H" else "A", 24.dp,
                            if (home) Dos.BrightGreen else Dos.BrightCyan, true)
                        Text(opp, color = if (current) Dos.White else Dos.Text, fontFamily = Mono,
                            fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        if (f.played) {
                            val mg = if (home) f.hg else f.ag
                            val og = if (home) f.ag else f.hg
                            val res = if (mg > og) vm.t("win") else if (mg < og) vm.t("lose") else vm.t("draw")
                            val rc = if (mg > og) Dos.BrightGreen else if (mg < og) Dos.BrightRed else Dos.Yellow
                            cell("$mg-$og", 44.dp, Dos.Text, true)
                            cell(res, 22.dp, rc, true)
                        } else {
                            cell("- -", 44.dp, Dos.DarkGray)
                            cell("", 22.dp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        DosButton("◀ ${vm.t("back")}") { nav(Screen.Office) }
    }
}

@Composable
fun ScorersScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    var tier by remember { mutableStateOf(s.myClub.division) }
    val scorers: List<Pair<Player, String>> = s.clubs.filter { it.division == tier }
        .flatMap { c -> c.roster.map { it to c.name } }
        .filter { it.first.goals > 0 }
        .sortedByDescending { it.first.goals }
        .take(40)

    ScreenScaffold(status = "${vm.t("scorers")}  ·  ${vm.divName(tier)}") {
        Row {
            (0..3).forEach { d ->
                DosButton(listOf("PR", "D1", "D2", "D3")[d],
                    color = if (tier == d) Dos.Yellow else Dos.BrightCyan,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)) { tier = d }
            }
        }
        Spacer(Modifier.height(6.dp))
        DosFrame(vm.t("scorers"), Modifier.weight(1f), fill = true) {
            if (scorers.isEmpty()) Text("—", color = Dos.Text, fontFamily = Mono)
            LazyColumn(Modifier.weight(1f)) {
                items(scorers.size) { i ->
                    val (p, club) = scorers[i]
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        cell("${i + 1}", 26.dp, Dos.LightGray)
                        Text(p.name, color = Dos.Text, fontFamily = Mono, fontSize = 12.sp,
                            maxLines = 1, modifier = Modifier.weight(1f))
                        Text(club, color = Dos.DarkGray, fontFamily = Mono, fontSize = 11.sp,
                            maxLines = 1, modifier = Modifier.weight(1f))
                        cell("${p.goals}", 30.dp, Dos.BrightGreen, true)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        DosButton("◀ ${vm.t("back")}") { nav(Screen.Office) }
    }
}

@Composable
fun FinancesScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    val me = s.myClub
    val squadValue = me.roster.sumOf { Game.priceFor(it) }
    val wageWeek = me.roster.sumOf { (it.skill * 1_200).toLong() }

    ScreenScaffold(status = "${vm.t("finances")}  ·  ${me.name}") {
        DosFrame(vm.t("finances")) {
            finRow(vm.t("budget"), "£${Game.fmt(me.budget)}", if (me.budget < 0) Dos.BrightRed else Dos.BrightGreen)
            finRow("Squad value", "£${Game.fmt(squadValue)}", Dos.Yellow)
            finRow("Wage bill / wk", "£${Game.fmt(wageWeek)}", Dos.LightGray)
            finRow(vm.t("season"), "${s.season}", Dos.Text)
            finRow(vm.t("squad"), "${me.roster.size}", Dos.Text)
        }
        Spacer(Modifier.height(10.dp))
        Text(if (me.budget < 0)
            "⚠ ${if (vm.lang == "no") "Selg spillere for å overleve." else "Sell players to survive."}"
            else if (vm.lang == "no") "Spill godt for gode inntekter." else "Play well for strong income.",
            color = if (me.budget < 0) Dos.BrightRed else Dos.Text, fontFamily = Mono, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        DosButton("◀ ${vm.t("back")}") { nav(Screen.Office) }
    }
}

@Composable
private fun finRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, color = Dos.Text, fontFamily = Mono, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CupScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    val ties = Cup.currentRoundTies(s)
    val myTie = vm.myCupTie()
    val out = myTie == null && s.cup.none { (it.home == s.clubId || it.away == s.clubId) && !it.played }
    val cupWinner = if (Cup.isWinner(s, s.clubId)) s.myClub.name else null

    ScreenScaffold(status = "${vm.t("cup")}  ·  ${vm.t("round")} ${s.cupRound + 1}") {
        DosFrame(vm.t("cup"), right = "${vm.t("round")} ${s.cupRound + 1}", modifier = Modifier.weight(1f), fill = true) {
            if (cupWinner != null) {
                Text("🏆 ${vm.t("champions_cup")}: $cupWinner", color = Dos.Yellow, fontFamily = Mono,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(8.dp))
            }
            LazyColumn(Modifier.weight(1f)) {
                items(ties.size) { i ->
                    val t = ties[i]
                    if (t.away == -1) return@items   // bye
                    val involved = t.home == s.clubId || t.away == s.clubId
                    Row(Modifier.fillMaxWidth()
                        .background(if (involved) Dos.Green else Color.Transparent)
                        .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.club(t.home).name, color = if (involved) Dos.White else Dos.Text,
                            fontFamily = Mono, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        cell(if (t.played) " ${t.hg}-${t.ag} " else " ${vm.t("vs")} ", 56.dp, Dos.Yellow, true)
                        Text(s.club(t.away).name, color = if (involved) Dos.White else Dos.Text,
                            fontFamily = Mono, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            DosButton("◀ ${vm.t("back")}", modifier = Modifier.weight(1f)) { nav(Screen.Office) }
            Spacer(Modifier.width(8.dp))
            if (myTie != null) {
                DosButton("▶ ${vm.t("play_cup")}", color = Dos.BrightGreen, modifier = Modifier.weight(2f)) {
                    vm.playCup(); nav(Screen.Match)
                }
            } else if (!out && ties.any { !it.played }) {
                DosButton("▶ ${vm.t("next_round")}", color = Dos.Yellow, modifier = Modifier.weight(2f)) {
                    vm.playCup()
                }
            }
        }
    }
}
