package no.mwmai.pcleague.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.mwmai.pcleague.data.MatchResult
import no.mwmai.pcleague.ui.GameViewModel
import no.mwmai.pcleague.ui.Screen
import no.mwmai.pcleague.ui.components.*
import no.mwmai.pcleague.ui.theme.Dos
import no.mwmai.pcleague.ui.theme.Mono

@Composable
fun MatchScreen(vm: GameViewModel, nav: (Screen) -> Unit) {
    val s = vm.state ?: run { nav(Screen.Title); return }
    val isCup = vm.lastWasCup
    val mine = if (isCup) vm.lastCupResult else vm.lastMatchResults.firstOrNull()

    ScreenScaffold(
        status = if (isCup) "${vm.t("cup")}  ·  ${vm.t("result")}"
        else "${vm.t("result")}  ·  ${vm.t("round")} ${s.matchday}/${vm.totalRounds()}",
    ) {
        if (mine == null) {
            DosFrame(vm.t("results")) {
                Text("—", color = Dos.Text, fontFamily = Mono)
            }
        } else {
            MatchResultCard(vm, mine)
        }
        Spacer(Modifier.height(6.dp))
        DosFrame(vm.t("results"), Modifier.weight(1f), fill = true) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val lines = if (isCup) otherCupResults(vm) else otherResults(vm, mine)
                lines.forEach { line ->
                    Text(line, color = Dos.Text, fontFamily = Mono, fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 1.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            if (isCup) {
                DosButton(vm.t("cup"), modifier = Modifier.weight(1f)) { nav(Screen.Cup) }
                Spacer(Modifier.width(8.dp))
            }
            DosButton("▶ ${vm.t("office")}", color = Dos.BrightGreen,
                modifier = Modifier.weight(1f)) { nav(Screen.Office) }
        }
    }
}

private fun otherCupResults(vm: GameViewModel): List<String> {
    val s = vm.state ?: return emptyList()
    // results from the round that was just played (cupRound may have advanced)
    val playedRound = s.cup.filter { it.played && it.away != -1 }.maxOfOrNull { it.round } ?: return emptyList()
    return s.cup.filter { it.round == playedRound && it.played && it.away != -1 }
        .map { t ->
            val h = s.club(t.home).name.take(16).padEnd(16)
            val a = s.club(t.away).name.take(16)
            "$h ${t.hg}-${t.ag}  $a"
        }
}

@Composable
fun MatchResultCard(vm: GameViewModel, r: MatchResult) {
    val s = vm.state!!
    val home = s.club(r.homeId).name; val away = s.club(r.awayId).name
    DosFrame(vm.t("result")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(home, color = Dos.White, fontFamily = Mono, fontWeight = FontWeight.Bold,
                fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Box(Modifier.background(Dos.Black).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("${r.hg} - ${r.ag}", color = Dos.Yellow, fontFamily = Mono,
                    fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Text(away, color = Dos.White, fontFamily = Mono, fontWeight = FontWeight.Bold,
                fontSize = 16.sp, modifier = Modifier.weight(1f))
        }
        if (r.homeScorers.isNotEmpty() || r.awayScorers.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    r.homeScorers.forEach { Text(it, color = Dos.BrightGreen, fontFamily = Mono,
                        fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
                }
                Text("⚽", fontFamily = Mono, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Column(Modifier.weight(1f)) {
                    r.awayScorers.forEach { Text(it, color = Dos.BrightGreen, fontFamily = Mono,
                        fontSize = 12.sp) }
                }
            }
        }
        val notable = r.events.filter { !it.text.startsWith("GOAL") }
        if (notable.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            notable.take(8).forEach {
                Text("${it.minute}'  ${it.text}", color = Dos.LightGray, fontFamily = Mono,
                    fontSize = 11.sp)
            }
        }
    }
}

private fun otherResults(vm: GameViewModel, mine: MatchResult?): List<String> {
    val s = vm.state ?: return emptyList()
    val round = (s.matchday - 1).coerceAtLeast(0)
    return s.fixtures.filter { it.round == round && it.played &&
        !(mine != null && it.home == mine.homeId && it.away == mine.awayId) }
        .filter { it.division == s.myClub.division }
        .map { f ->
            val h = s.club(f.home).name.take(16).padEnd(16)
            val a = s.club(f.away).name.take(16)
            "$h ${f.hg}-${f.ag}  $a"
        }
}
