package no.mwmai.pcleague.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import no.mwmai.pcleague.data.*
import no.mwmai.pcleague.engine.Cup
import no.mwmai.pcleague.engine.Game
import no.mwmai.pcleague.engine.League

class GameViewModel(app: Application) : AndroidViewModel(app) {

    val dataset: Dataset by lazy { DatasetLoader.load(getApplication()) }

    var state by mutableStateOf<GameState?>(null)
        private set

    // a monotonically increasing tick to force recomposition after in-place mutation
    var rev by mutableStateOf(0)
        private set

    var lang by mutableStateOf("en")
    var lastMatchResults by mutableStateOf<List<MatchResult>>(emptyList())
    var lastCupResult by mutableStateOf<MatchResult?>(null)
    var lastWasCup by mutableStateOf(false)

    fun t(key: String) = I18n.t(lang, key)

    private fun bump() { rev++ }

    fun hasAutosave(): Boolean = SaveManager.hasAuto(getApplication())

    fun newGame(clubId: Int, manager: String) {
        val seed = (System.nanoTime() xor (clubId.toLong() * 2654435761L))
        val s = no.mwmai.pcleague.engine.NewGame.create(dataset, clubId,
            manager.ifBlank { "Manager" }, lang, seed)
        s.lang = lang
        state = s
        autosave()
        bump()
    }

    fun continueGame(): Boolean {
        val s = SaveManager.loadAuto(getApplication()) ?: return false
        lang = s.lang
        state = s
        bump()
        return true
    }

    fun loadSlot(slot: Int): Boolean {
        val s = SaveManager.load(getApplication(), slot) ?: return false
        lang = s.lang
        state = s
        bump()
        return true
    }

    fun saveSlot(slot: Int) = state?.let { SaveManager.save(getApplication(), slot, it) }
    fun deleteSlot(slot: Int) = SaveManager.delete(getApplication(), slot)
    fun slots() = SaveManager.listSlots(getApplication())

    fun autosave() = state?.let { SaveManager.autosave(getApplication(), it) }

    fun setLang(l: String) { lang = l; state?.let { it.lang = l }; bump() }

    /* ---- actions ---- */

    fun playMatchday() {
        val s = state ?: return
        lastMatchResults = Game.playMatchday(dataset, s)
        lastWasCup = false
        autosave(); bump()
    }

    fun playCup() {
        val s = state ?: return
        lastCupResult = Game.playCupRound(s)
        lastWasCup = true
        autosave(); bump()
    }

    fun setTactic(tac: Int) { state?.myClub?.tactic = tac; autosave(); bump() }

    fun setLineup(ids: List<Int>) { state?.myClub?.lineup = ids.toMutableList(); autosave(); bump() }

    fun buy(pid: Int, offer: Long): String? =
        state?.let { Game.buyPlayer(it, pid, offer).also { _ -> autosave(); bump() } }

    fun sell(pid: Int): String? =
        state?.let { Game.sellPlayer(it, pid).also { _ -> autosave(); bump() } }

    fun promoteYouth(pid: Int): String? =
        state?.let { Game.promoteYouth(it, dataset, pid).also { _ -> autosave(); bump() } }

    /* ---- read helpers ---- */

    fun divName(tier: Int) = when (tier) {
        0 -> t("premier"); 1 -> t("div1"); 2 -> t("div2"); else -> t("div3")
    }

    fun standings(tier: Int) = state?.let { League.standings(it, tier) } ?: emptyList()
    fun totalRounds() = state?.let { League.totalRounds(it) } ?: 0
    fun myCupTie() = state?.let { Cup.myTie(it) }
    fun roleLabel(role: String) = when (role) {
        "GK" -> t("gk"); "DEF" -> t("def"); "MID" -> t("mid"); else -> t("att")
    }
}
