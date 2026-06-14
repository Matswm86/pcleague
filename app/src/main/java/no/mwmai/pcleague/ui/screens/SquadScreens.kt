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
import no.mwmai.pcleague.engine.MatchEngine
import no.mwmai.pcleague.ui.GameViewModel
import no.mwmai.pcleague.ui.Screen
import no.mwmai.pcleague.ui.components.*
import no.mwmai.pcleague.ui.theme.Dos
import no.mwmai.pcleague.ui.theme.Mono

private fun roleColor(role: String): Color = when (role) {
    "GK" -> Dos.Yellow; "DEF" -> Dos.BrightCyan; "MID" -> Dos.BrightGreen; else -> Dos.BrightMagenta
}

@Composable
private fun playerStatus(vm: GameViewModel, p: Player): Pair<String, Color> = when {
    p.injuredWeeks > 0 -> "${vm.t("injured")}${p.injuredWeeks}" to Dos.BrightRed
    p.suspended > 0 -> "${vm.t("suspended")}${p.suspended}" to Dos.BrightRed
    p.transferListed -> "T" to Dos.Yellow
    else -> "" to Dos.Text
}

@Composable
fun SquadScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    val me = s.myClub
    var sel by remember { mutableStateOf<Int?>(null) }
    val roster = me.roster.sortedWith(compareBy({ roleOrder(it.role) }, { -it.skill }))

    ScreenScaffold(status = "${me.name}  ·  ${roster.size} ${vm.t("squad").lowercase()}") {
        DosFrame(vm.t("squad"), Modifier.weight(1f), right = "${roster.size}", fill = true) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                cell(vm.t("pos"), 38.dp, Dos.Yellow, true)
                cellW(vm.t("name"), 1f, this, Dos.Yellow, true)
                cell(vm.t("age"), 34.dp, Dos.Yellow, true)
                cell(vm.t("sh"), 30.dp, Dos.Yellow, true)
                cell(vm.t("fit"), 34.dp, Dos.Yellow, true)
                cell(vm.t("gls"), 32.dp, Dos.Yellow, true)
            }
            LazyColumn(Modifier.weight(1f)) {
                items(roster.size) { i ->
                    val p = roster[i]
                    val (st, stc) = playerStatus(vm, p)
                    Column {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (sel == p.id) Dos.Green else Color.Transparent)
                                .clickable { sel = if (sel == p.id) null else p.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            cell(vm.roleLabel(p.role), 38.dp, roleColor(p.role), true)
                            Text(p.name, color = if (sel == p.id) Dos.White else Dos.Text,
                                fontFamily = Mono, fontSize = 12.sp, maxLines = 1,
                                modifier = Modifier.weight(1f))
                            if (st.isNotEmpty()) cell(st, 32.dp, stc, true)
                            cell("${p.age}", 34.dp)
                            cell("${p.skill}", 30.dp, Dos.BrightGreen)
                            cell("${p.fitness}", 34.dp,
                                if (p.fitness < 60) Dos.BrightRed else Dos.Text)
                            cell("${p.goals}", 32.dp)
                        }
                        if (sel == p.id) PlayerDetail(vm, p)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        DosButton("◀ ${vm.t("back")}") { nav(Screen.Office) }
    }
}

@Composable
private fun PlayerDetail(vm: GameViewModel, p: Player) {
    Column(Modifier.fillMaxWidth().background(Dos.Black).padding(8.dp)) {
        Row {
            detail(vm.t("value"), "£${Game.fmt(Game.priceFor(p))}")
            detail(vm.t("form"), "%.2f".format(p.form))
            detail(vm.t("att"), "${p.skill}/${p.maxSkill}")
        }
        Row {
            detail("Nat", p.nat)
            detail(vm.t("bookings"), "${p.yellow}")
            detail(vm.t("red_cards"), "${p.red}")
            detail("Rt", if (p.ratingN > 0) "%.1f".format(p.avgRating) else "-")
        }
        if (p.injuredWeeks > 0) Text("${vm.t("injured")}: ${p.injuredWeeks}w", color = Dos.BrightRed,
            fontFamily = Mono, fontSize = 12.sp)
        if (p.suspended > 0) Text("${vm.t("suspended")}: ${p.suspended}", color = Dos.BrightRed,
            fontFamily = Mono, fontSize = 12.sp)
    }
}

@Composable
private fun RowScope.detail(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(label, color = Dos.LightGray, fontFamily = Mono, fontSize = 10.sp)
        Text(value, color = Dos.BrightGreen, fontFamily = Mono, fontSize = 13.sp,
            fontWeight = FontWeight.Bold)
    }
}

private fun roleOrder(role: String) = when (role) { "GK" -> 0; "DEF" -> 1; "MID" -> 2; else -> 3 }

@Composable
fun LineupScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    val me = s.myClub
    // working copy of lineup
    var lineup by remember { mutableStateOf(me.lineup.toMutableList()) }
    var picked by remember { mutableStateOf<Int?>(null) }

    fun playerById(id: Int) = me.roster.firstOrNull { it.id == id }
    val xi = lineup.take(11).mapNotNull { playerById(it) }
    val rest = me.roster.filter { it.id !in lineup.take(11) }
        .sortedWith(compareBy({ roleOrder(it.role) }, { -it.skill }))

    val rating = MatchEngine.ratingOf(s, me.copy(lineup = lineup.toMutableList()))

    ScreenScaffold(
        status = "↑↓/tap swap · +/− · ${vm.t("tactic")}: " +
            listOf(vm.t("defensive"), vm.t("normal"), vm.t("attacking"))[me.tactic],
    ) {
        Row {
            (0..2).forEach { tc ->
                DosButton(listOf(vm.t("defensive"), vm.t("normal"), vm.t("attacking"))[tc],
                    color = if (me.tactic == tc) Dos.Yellow else Dos.BrightCyan,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)) { vm.setTactic(tc) }
            }
        }
        Spacer(Modifier.height(4.dp))
        DosFrame("${vm.t("lineup")}  (XI)", right = "STR $rating") {
            xi.forEachIndexed { idx, p ->
                LineupRow(vm, idx + 1, p, picked == p.id, true) {
                    picked = if (picked == p.id) null else p.id
                }
            }
            if (xi.size < 11) Text("⚠ ${11 - xi.size} more needed", color = Dos.BrightRed,
                fontFamily = Mono, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        DosFrame(vm.t("sub"), Modifier.weight(1f), fill = true) {
            LazyColumn {
                items(rest.size) { i ->
                    val p = rest[i]
                    LineupRow(vm, 0, p, picked == p.id, false) {
                        // swap picked (in XI) with this sub, or just bring in
                        val sel = picked
                        if (sel != null && sel in lineup.take(11)) {
                            val newL = lineup.toMutableList()
                            val pi = newL.indexOf(sel)
                            val si = newL.indexOf(p.id)
                            if (si >= 0) { newL[pi] = p.id; newL[si] = sel }
                            else { newL[pi] = p.id; newL.add(sel) }
                            lineup = newL; picked = null; vm.setLineup(newL)
                        } else picked = p.id
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            DosButton("◀ ${vm.t("back")}", modifier = Modifier.weight(1f)) { nav(Screen.Office) }
            Spacer(Modifier.width(8.dp))
            DosButton("Auto", color = Dos.BrightCyan, modifier = Modifier.weight(1f)) {
                val best = no.mwmai.pcleague.engine.NewGame.pickBestEleven(me).toMutableList()
                lineup = best; vm.setLineup(best)
            }
        }
    }
}

@Composable
private fun LineupRow(vm: GameViewModel, num: Int, p: Player, sel: Boolean, inXi: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (sel) Dos.Green else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cell(if (inXi) "$num" else "·", 26.dp, Dos.Yellow, true)
        cell(vm.roleLabel(p.role), 38.dp, roleColor(p.role), true)
        cellW(p.name, 1f, this, if (sel) Dos.White else Dos.Text)
        if (!p.available) cell(if (p.injuredWeeks > 0) vm.t("injured") else vm.t("suspended"),
            46.dp, Dos.BrightRed)
        cell("${p.age}", 30.dp)
        cell("${p.skill}", 28.dp, Dos.BrightGreen)
    }
}
