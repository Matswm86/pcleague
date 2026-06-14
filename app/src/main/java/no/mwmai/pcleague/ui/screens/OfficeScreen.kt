package no.mwmai.pcleague.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.mwmai.pcleague.engine.Game
import no.mwmai.pcleague.engine.League
import no.mwmai.pcleague.ui.GameViewModel
import no.mwmai.pcleague.ui.Screen
import no.mwmai.pcleague.ui.components.*
import no.mwmai.pcleague.ui.theme.Dos
import no.mwmai.pcleague.ui.theme.Mono

@Composable
fun OfficeScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    val me = s.myClub
    val table = vm.standings(me.division)
    val pos = table.indexOfFirst { it.id == me.id } + 1
    val totalRounds = vm.totalRounds()
    val seasonOver = s.matchday >= totalRounds

    ScreenScaffold(
        status = "${me.name}  ·  ${vm.divName(me.division)}  ·  ${vm.t("budget")}: £${Game.fmt(me.budget)}",
    ) {
        DosFrame(
            title = me.name,
            right = "${vm.t("season")} ${s.season}",
        ) {
            Row {
                Text("${vm.divName(me.division)}", color = Dos.BrightCyan, fontFamily = Mono,
                    fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${vm.t("pos")} $pos/${table.size}", color = Dos.Yellow, fontFamily = Mono,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row {
                Text("${vm.t("round")} ${(s.matchday).coerceAtMost(totalRounds)}/$totalRounds",
                    color = Dos.Text, fontFamily = Mono, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${me.points} ${vm.t("pts")}  (${me.won}-${me.drawn}-${me.lost})",
                    color = Dos.Text, fontFamily = Mono, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(6.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DosFrame(vm.t("office")) {
                MenuRow("1", vm.t("squad")) { nav(Screen.Squad) }
                MenuRow("2", vm.t("lineup")) { nav(Screen.Lineup) }
                MenuRow("3", vm.t("transfers")) { nav(Screen.Transfers) }
                MenuRow("4", vm.t("youth")) { nav(Screen.Youth) }
                MenuRow("5", vm.t("tables")) { nav(Screen.Tables) }
                MenuRow("6", vm.t("fixtures")) { nav(Screen.Fixtures) }
                MenuRow("7", vm.t("scorers")) { nav(Screen.Scorers) }
                MenuRow("8", vm.t("finances")) { nav(Screen.Finances) }
                MenuRow("9", vm.t("cup")) { nav(Screen.Cup) }
                MenuRow("0", vm.t("inbox")) { nav(Screen.History) }
            }
            Spacer(Modifier.height(6.dp))
            if (s.inbox.isNotEmpty()) {
                DosFrame(vm.t("inbox")) {
                    s.inbox.takeLast(3).reversed().forEach {
                        Text("• $it", color = Dos.BrightGreen, fontFamily = Mono, fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            DosButton(vm.t("save"), modifier = Modifier.weight(1f)) { nav(Screen.SaveLoad) }
            Spacer(Modifier.width(8.dp))
            if (!seasonOver) {
                DosButton("▶ ${vm.t("play_match")}", color = Dos.BrightGreen,
                    modifier = Modifier.weight(2f)) { vm.playMatchday(); nav(Screen.Match) }
            } else {
                DosButton("▶ ${vm.t("next_round")}", color = Dos.Yellow,
                    modifier = Modifier.weight(2f)) { vm.playMatchday(); nav(Screen.Office) }
            }
        }
    }
}
