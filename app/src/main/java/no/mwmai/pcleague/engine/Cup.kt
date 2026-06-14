package no.mwmai.pcleague.engine

import no.mwmai.pcleague.data.CupTie
import no.mwmai.pcleague.data.GameState

/** FA Cup style single-leg knockout across every club. TV money per round. */
object Cup {

    val tvIncome = longArrayOf(
        40_000, 60_000, 100_000, 180_000, 320_000, 600_000, 1_200_000, 2_500_000,
    )

    fun draw(state: GameState, rng: Rng) {
        state.cup.clear()
        state.cupRound = 0
        val ids = state.clubs.map { it.id }.toMutableList()
        // shuffle
        for (i in ids.indices.reversed()) {
            val j = rng.int(0, i + 1); val tmp = ids[i]; ids[i] = ids[j]; ids[j] = tmp
        }
        // give byes so the field becomes a power of two on next complete round
        pairUp(state, ids, 0)
    }

    private fun pairUp(state: GameState, ids: List<Int>, round: Int) {
        var i = 0
        val list = ids.toMutableList()
        while (i + 1 < list.size) {
            state.cup.add(CupTie(round = round, home = list[i], away = list[i + 1]))
            i += 2
        }
        if (list.size % 2 == 1) {
            // odd one out gets a bye: auto-advance via a played tie vs itself marker
            state.cup.add(CupTie(round = round, home = list.last(), away = -1,
                played = true, hg = 1, ag = 0, winner = list.last()))
        }
    }

    fun currentRoundTies(state: GameState): List<CupTie> =
        state.cup.filter { it.round == state.cupRound }

    fun myTie(state: GameState): CupTie? =
        currentRoundTies(state).firstOrNull {
            (it.home == state.clubId || it.away == state.clubId) && !it.played
        }

    /** Advance to next round once all ties of the current round are played. */
    fun advanceIfComplete(state: GameState, rng: Rng): Boolean {
        val ties = currentRoundTies(state)
        if (ties.isEmpty() || ties.any { !it.played }) return false
        val winners = ties.map { it.winner }.filter { it >= 0 }
        if (winners.size <= 1) return false   // cup finished
        state.cupRound++
        pairUp(state, winners, state.cupRound)
        return true
    }

    fun isWinner(state: GameState, clubId: Int): Boolean {
        val ties = state.cup.filter { it.round == state.cupRound }
        return ties.size == 1 && ties[0].played && ties[0].winner == clubId &&
            state.cup.none { it.round > state.cupRound }
    }
}
