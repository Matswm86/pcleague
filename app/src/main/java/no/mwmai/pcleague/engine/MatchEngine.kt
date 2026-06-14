package no.mwmai.pcleague.engine

import no.mwmai.pcleague.data.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object MatchEngine {

    private const val HOME_ADV = 1.18

    /** Career/age phase factor on effective skill (peak ~27-29). */
    fun agePhase(age: Int): Double = when {
        age < 20 -> 0.86
        age < 23 -> 0.93
        age <= 29 -> 1.0
        age <= 32 -> 0.95
        age <= 34 -> 0.88
        else -> 0.78
    }

    private fun eff(p: Player): Double {
        val fit = 0.6 + 0.4 * (p.fitness / 100.0)
        return p.skill * p.form * fit * agePhase(p.age)
    }

    private fun selectedXI(state: GameState, club: Club): List<Player> {
        val ids = club.lineup.take(11)
        val picked = ids.mapNotNull { id -> club.roster.firstOrNull { it.id == id } }
            .filter { it.available }
        if (picked.size >= 9) return picked
        // fall back: best available if manager's XI is depleted
        return club.roster.filter { it.available }.sortedByDescending { eff(it) }.take(11)
    }

    private data class Strength(val atk: Double, val def: Double, val mid: Double, val xi: List<Player>)

    private fun strength(state: GameState, club: Club): Strength {
        val xi = selectedXI(state, club)
        val gk = xi.filter { it.role == "GK" }
        val df = xi.filter { it.role == "DEF" }
        val md = xi.filter { it.role == "MID" }
        val at = xi.filter { it.role == "ATT" }
        val tacticAtk = when (club.tactic) { 0 -> 0.85; 2 -> 1.18; else -> 1.0 }
        val tacticDef = when (club.tactic) { 0 -> 1.18; 2 -> 0.85; else -> 1.0 }
        val midSum = md.sumOf { eff(it) }
        val atk = (at.sumOf { eff(it) } + 0.4 * midSum) * tacticAtk * club.teamForm
        val def = (df.sumOf { eff(it) } + gk.sumOf { eff(it) } * 1.3 + 0.4 * midSum) *
            tacticDef * club.teamForm
        return Strength(atk, def, midSum, xi)
    }

    fun simulate(
        state: GameState, homeId: Int, awayId: Int, rng: Rng,
        isCup: Boolean = false, neutral: Boolean = false,
    ): MatchResult {
        val home = state.club(homeId); val away = state.club(awayId)
        val hs = strength(state, home); val as_ = strength(state, away)
        val homeBoost = if (neutral) 1.0 else HOME_ADV

        // expected goals: own attack vs opponent defence, scaled to a footballish range
        val hLambda = xg(hs.atk * homeBoost, as_.def)
        val aLambda = xg(as_.atk, hs.def * if (neutral) 1.0 else 1.05)

        var hg = rng.poisson(hLambda)
        var ag = rng.poisson(aLambda)

        val events = mutableListOf<MatchEventLog>()
        val homeScorers = mutableListOf<String>()
        val awayScorers = mutableListOf<String>()

        // hit-the-post near misses (flavour, no goal)
        repeat(2) {
            if (rng.chance(0.10)) {
                val h = rng.chance(0.5)
                events.add(MatchEventLog(rng.int(1, 90), "hits the post!", h))
            }
        }
        // referee mistake: a disallowed goal or a soft penalty
        if (rng.chance(0.08)) {
            val h = rng.chance(0.5)
            if (rng.chance(0.5)) {
                if (h) hg++ else ag++
                events.add(MatchEventLog(rng.int(1, 90), "scores a controversial penalty", h))
            } else {
                if (h && hg > 0) hg-- else if (!h && ag > 0) ag--
                events.add(MatchEventLog(rng.int(1, 90), "has a goal disallowed", h))
            }
        }

        hg = hg.coerceIn(0, 9); ag = ag.coerceIn(0, 9)

        assignScorers(home, hs.xi, hg, rng, homeScorers, events, true)
        assignScorers(away, as_.xi, ag, rng, awayScorers, events, false)

        // disciplinary + injuries for both XIs
        applyAftermath(home, hs.xi, rng, events, true)
        applyAftermath(away, as_.xi, rng, events, false)

        // ratings (division floor unless cup)
        rateTeam(home, hs.xi, hg, ag, isCup, rng)
        rateTeam(away, as_.xi, ag, hg, isCup, rng)

        events.sortBy { it.minute }
        return MatchResult(homeId, awayId, hg, ag, events, homeScorers, awayScorers)
    }

    private fun xg(atk: Double, def: Double): Double {
        val ratio = atk / max(1.0, atk + def)
        // map possession-ish ratio (0..1, ~0.5 even) to expected goals ~0.3..3.2
        return (0.3 + 3.4 * (ratio * ratio)).coerceIn(0.2, 4.0)
    }

    private fun assignScorers(
        club: Club, xi: List<Player>, goals: Int, rng: Rng,
        names: MutableList<String>, events: MutableList<MatchEventLog>, home: Boolean,
    ) {
        if (goals <= 0 || xi.isEmpty()) return
        val weights = xi.map { p ->
            p to when (p.role) {
                "ATT" -> eff(p) * 3.0; "MID" -> eff(p) * 1.3; "DEF" -> eff(p) * 0.3; else -> 0.02
            }
        }
        val total = weights.sumOf { it.second }
        repeat(goals) {
            var r = rng.nextDouble() * total
            var scorer = weights.first().first
            for ((p, w) in weights) { r -= w; if (r <= 0) { scorer = p; break } }
            scorer.goals++
            names.add(scorer.name)
            events.add(MatchEventLog(rng.int(1, 90), "GOAL — ${scorer.name}", home))
        }
    }

    private fun applyAftermath(
        club: Club, xi: List<Player>, rng: Rng,
        events: MutableList<MatchEventLog>, home: Boolean,
    ) {
        for (p in xi) {
            p.apps++
            p.fitness = max(20, p.fitness - rng.int(8, 18))
            // yellow card
            if (rng.chance(0.11)) {
                p.yellow++
                events.add(MatchEventLog(rng.int(1, 90), "booked — ${p.name}", home))
                if (p.yellow % 5 == 0) p.suspended = max(p.suspended, 1)
            }
            // red card
            if (rng.chance(0.012)) {
                p.red++
                p.suspended = max(p.suspended, 2)
                events.add(MatchEventLog(rng.int(1, 90), "SENT OFF — ${p.name}", home))
            }
            // injury
            if (rng.chance(0.022)) {
                p.injuredWeeks = rng.int(1, 7)
                events.add(MatchEventLog(rng.int(1, 90), "injured — ${p.name}", home))
            }
        }
    }

    private fun rateTeam(club: Club, xi: List<Player>, gf: Int, ga: Int, isCup: Boolean, rng: Rng) {
        val floor = if (isCup) 1 else when (club.division) { 0 -> 4; 1 -> 3; 2 -> 2; else -> 1 }
        val resultBonus = when { gf > ga -> 1.0; gf < ga -> -1.0; else -> 0.0 }
        for (p in xi) {
            var r = 5.5 + resultBonus + (p.form - 1.0) * 4.0 + rng.gaussian(0.0, 1.0)
            r += p.goals.coerceAtMost(3) * 0.0   // goals already counted via tally; small handled below
            if (p.role == "ATT") r += 0.0
            if ((p.role == "GK" || p.role == "DEF") && ga == 0) r += 1.0
            val rating = r.coerceIn(floor.toDouble(), 10.0)
            p.ratingSum += rating; p.ratingN++
        }
    }

    fun describe(r: MatchResult, state: GameState): String {
        val h = state.club(r.homeId).name; val a = state.club(r.awayId).name
        return "$h ${r.hg}-${r.ag} $a"
    }

    // exposed for previews / team-strength bars
    fun ratingOf(state: GameState, club: Club): Int {
        val s = strength(state, club)
        return min(99, ((s.atk + s.def) / 6.0).toInt())
    }

    fun unused() = abs(min(1, 1))
}
