package no.mwmai.pcleague.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import no.mwmai.pcleague.ui.screens.*

enum class Screen { Title, NewGame, Office, Squad, Lineup, Transfers, Tables, Fixtures, Scorers, Finances, Youth, Cup, Match, SaveLoad, Settings, History }

@androidx.compose.runtime.Composable
fun PcLeagueApp(modifier: Modifier = Modifier) {
    val vm: GameViewModel = viewModel()
    var screen by remember { mutableStateOf(Screen.Title) }
    // observe rev so in-place mutations recompose children
    @Suppress("UNUSED_EXPRESSION") vm.rev

    val nav: (Screen) -> Unit = { screen = it }

    when (screen) {
        Screen.Title -> TitleScreen(vm, nav)
        Screen.NewGame -> NewGameScreen(vm, nav)
        Screen.Office -> OfficeScreen(vm, nav)
        Screen.Squad -> SquadScreen(vm, nav)
        Screen.Lineup -> LineupScreen(vm, nav)
        Screen.Transfers -> TransfersScreen(vm, nav)
        Screen.Tables -> TablesScreen(vm, nav)
        Screen.Fixtures -> FixturesScreen(vm, nav)
        Screen.Scorers -> ScorersScreen(vm, nav)
        Screen.Finances -> FinancesScreen(vm, nav)
        Screen.Youth -> YouthScreen(vm, nav)
        Screen.Cup -> CupScreen(vm, nav)
        Screen.Match -> MatchScreen(vm, nav)
        Screen.SaveLoad -> SaveLoadScreen(vm, nav)
        Screen.Settings -> SettingsScreen(vm, nav)
        Screen.History -> HistoryScreen(vm, nav)
    }
}
