package no.mwmai.pcleague.engine

import no.mwmai.pcleague.data.Club
import no.mwmai.pcleague.data.Fixture
import no.mwmai.pcleague.data.GameState

object League {

    /** Circle method round-robin (double: home/away mirrored in 2nd half). */
    fun roundRobin(teamIds: List<Int>): List<Pair<Int, Int>> {
        val ids = teamIds.toMutableList()
        val bye = -1
        if (ids.size % 2 == 1) ids.add(bye)
        val n = ids.size
        val rounds = n - 1
        val half = n / 2
        val pairs = mutableListOf<Pair<Int, Int>>()
        val arr = ids.toMutableList()
        // first leg
        val legA = mutableListOf<List<Pair<Int, Int>>>()
        for (r in 0 until rounds) {
            val day = mutableListOf<Pair<Int, Int>>()
            for (i in 0 until half) {
                val a = arr[i]; val b = arr[n - 1 - i]
                if (a != bye && b != bye) {
                    // alternate home/away by round parity for fairness
                    if (r % 2 == 0) day.add(a to b) else day.add(b to a)
                }
            }
            legA.add(day)
            // rotate (keep first fixed)
            val last = arr.removeAt(n - 1)
            arr.add(1, last)
        }
        legA.forEach { day -> pairs.addAll(day) }
        // second leg = reversed venues
        legA.forEach { day -> day.forEach { (a, b) -> pairs.add(b to a) } }
        return pairs
    }

    fun buildFixtures(state: GameState, divisions: List<List<Int>>) {
        state.fixtures.clear()
        for ((tier, teamIds) in divisions.withIndex()) {
            val present = teamIds.filter { id -> state.clubs.any { it.id == id } }
            val rr = roundRobin(present)
            val perRound = present.size / 2
            rr.forEachIndexed { idx, (h, a) ->
                val round = idx / perRound
                state.fixtures.add(Fixture(round = round, division = tier, home = h, away = a))
            }
        }
    }

    fun roundsInDivision(state: GameState, tier: Int): Int =
        (state.fixtures.filter { it.division == tier }.maxOfOrNull { it.round } ?: -1) + 1

    /** Max rounds across the player's own division (drives matchday count). */
    fun totalRounds(state: GameState): Int =
        roundsInDivision(state, state.myClub.division)

    fun standings(state: GameState, tier: Int): List<Club> =
        state.clubs.filter { it.division == tier }
            .sortedWith(compareByDescending<Club> { it.points }
                .thenByDescending { it.gd }
                .thenByDescending { it.gf }
                .thenBy { it.name })

    fun resetSeasonRecords(state: GameState) {
        for (c in state.clubs) {
            c.played = 0; c.won = 0; c.drawn = 0; c.lost = 0
            c.gf = 0; c.ga = 0; c.points = 0
        }
    }

    fun applyResult(state: GameState, f: Fixture) {
        val h = state.club(f.home); val a = state.club(f.away)
        h.played++; a.played++
        h.gf += f.hg; h.ga += f.ag; a.gf += f.ag; a.ga += f.hg
        when {
            f.hg > f.ag -> { h.won++; a.lost++; h.points += 3 }
            f.hg < f.ag -> { a.won++; h.lost++; a.points += 3 }
            else -> { h.drawn++; a.drawn++; h.points++; a.points++ }
        }
    }
}
