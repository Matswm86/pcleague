package no.mwmai.pcleague.engine

import no.mwmai.pcleague.data.*
import kotlin.math.max
import kotlin.math.roundToLong

/** High-level game flow. All mutation of GameState goes through here. */
object Game {

    fun rng(state: GameState): Rng = Rng(state.seed).also {
        // fast-forward the stream deterministically by season+matchday so each
        // step is reproducible from the stored seed.
        repeat((state.season * 10_000 + state.matchday) % 4096) { _ -> it.nextLong() }
    }

    /** Play one league matchday (the player's division round + all AI rounds). */
    fun playMatchday(ds: Dataset, state: GameState): List<MatchResult> {
        val r = rng(state)
        val round = state.matchday
        state.lastResults.clear()
        // league fixtures of this round, all divisions
        val due = state.fixtures.filter { it.round == round && !it.played }
        for (f in due) {
            val res = MatchEngine.simulate(state, f.home, f.away, r)
            f.hg = res.hg; f.ag = res.ag; f.played = true
            League.applyResult(state, f)
            if (f.home == state.clubId || f.away == state.clubId) state.lastResults.add(res)
        }
        // weekly recovery + form drift for everyone
        weeklyTick(state, r)
        finances(state, due)
        state.matchday++
        aiTransfers(ds, state, r)
        if (state.matchday >= League.totalRounds(state)) seasonEnd(ds, state, r)
        return state.lastResults
    }

    /** Play the player's current cup tie (and simulate the rest of the round). */
    fun playCupRound(state: GameState): MatchResult? {
        val r = rng(state)
        var mine: MatchResult? = null
        for (tie in Cup.currentRoundTies(state).filter { !it.played }) {
            val neutralFinal = Cup.currentRoundTies(state).size == 1
            val res = MatchEngine.simulate(state, tie.home, tie.away, r, isCup = true,
                neutral = neutralFinal)
            var hg = res.hg; var ag = res.ag
            // no draws in the cup: settle by extra-time/penalties bias to stronger
            if (hg == ag) {
                if (MatchEngine.ratingOf(state, state.club(tie.home)) >=
                    MatchEngine.ratingOf(state, state.club(tie.away))) hg++ else ag++
            }
            tie.hg = hg; tie.ag = ag; tie.played = true
            tie.winner = if (hg > ag) tie.home else tie.away
            // TV income for both clubs
            val tv = Cup.tvIncome.getOrElse(tie.round) { Cup.tvIncome.last() }
            state.club(tie.home).budget += tv
            state.club(tie.away).budget += tv
            if (tie.home == state.clubId || tie.away == state.clubId) mine = res
        }
        Cup.advanceIfComplete(state, r)
        return mine
    }

    private fun weeklyTick(state: GameState, r: Rng) {
        for (c in state.clubs) {
            for (p in c.roster) {
                if (p.injuredWeeks > 0) p.injuredWeeks--
                if (p.suspended > 0) p.suspended--
                p.fitness = (p.fitness + r.int(10, 22)).coerceAtMost(100)
                // form mean-reverts toward 1.0; young players swing more (per docs)
                val volatility = if (p.age < 23) 0.10 else 0.05
                p.form += (1.0 - p.form) * 0.25 + r.gaussian(0.0, volatility)
                p.form = p.form.coerceIn(0.65, 1.35)
            }
            // team form: more stable for experienced sides
            val avgAge = c.roster.map { it.age }.average().takeIf { !it.isNaN() } ?: 26.0
            val vol = if (avgAge < 25) 0.07 else 0.04
            c.teamForm += (1.0 - c.teamForm) * 0.2 + r.gaussian(0.0, vol)
            c.teamForm = c.teamForm.coerceIn(0.8, 1.2)
        }
    }

    private fun finances(state: GameState, due: List<Fixture>) {
        for (f in due) {
            val h = state.club(f.home)
            // gate income scales with division, home form and result
            val base = when (h.division) { 0 -> 220_000L; 1 -> 90_000L; 2 -> 45_000L; else -> 25_000L }
            val formMult = (0.7 + 0.6 * (h.teamForm - 0.8) / 0.4).coerceIn(0.5, 1.4)
            val resultMult = if (f.hg >= f.ag) 1.0 else 0.85
            h.budget += (base * formMult * resultMult).roundToLong()
            // away club gets a small TV/share, both pay wages
            state.club(f.away).budget += base / 12
        }
        // weekly wage bill ~ proportional to squad skill
        for (c in state.clubs) {
            val wage = c.roster.sumOf { (it.skill * 1_200).toLong() }
            c.budget = max(-5_000_000L, c.budget - wage)
        }
    }

    /* ----------------------------- transfers ----------------------------- */

    fun priceFor(p: Player): Long {
        val ageMult = MatchEngine.agePhase(p.age)
        return max(20_000L, (p.value * (0.6 + p.form * 0.5) * ageMult).roundToLong())
    }

    /** Manager buys a listed player. Returns null on success, else reason. */
    fun buyPlayer(state: GameState, pid: Int, offer: Long): String? {
        val me = state.myClub
        val seller = state.ownerOf(pid) ?: return "Player not found"
        if (seller.id == me.id) return "Already your player"
        if (me.roster.size >= 24) return "Squad full"
        val p = state.findPlayer(pid) ?: return "Player not found"
        val ask = priceFor(p)
        if (offer < (ask * 0.9).toLong()) return "Bid rejected (asking ~£${fmt(ask)})"
        if (me.budget < offer) return "Insufficient funds"
        me.budget -= offer
        seller.budget += offer
        seller.roster.remove(p)
        state.transferList.remove(pid)
        p.transferListed = false
        me.roster.add(p)
        state.inbox.add("Signed ${p.name} for £${fmt(offer)}")
        return null
    }

    fun sellPlayer(state: GameState, pid: Int): String? {
        val me = state.myClub
        val p = me.roster.firstOrNull { it.id == pid } ?: return "Not your player"
        if (me.roster.size <= 12) return "Squad too small to sell"
        val fee = priceFor(p)
        me.roster.remove(p)
        me.budget += fee
        // a random AI club of similar level buys
        val buyer = state.clubs.filter { it.id != me.id && it.roster.size < 22 }
            .minByOrNull { kotlin.math.abs(it.division - me.division) }
        buyer?.roster?.add(p)
        state.inbox.add("Sold ${p.name} for £${fmt(fee)}")
        return null
    }

    fun promoteYouth(state: GameState, ds: Dataset, pid: Int): String? {
        val me = state.myClub
        if (me.roster.size >= 24) return "First-team squad full"
        val y = me.youth.firstOrNull { it.id == pid } ?: return "Not in youth squad"
        me.youth.remove(y)
        me.roster.add(y)
        me.youth.add(NewGame.generateYouth(state.nextPlayerId++, ds.namePools, rng(state)))
        state.inbox.add("Promoted ${y.name} to the first team")
        return null
    }

    private fun aiTransfers(ds: Dataset, state: GameState, r: Rng) {
        // best clubs occasionally sign a strong listed player from a weaker club
        if (!r.chance(0.5)) return
        val buyers = state.clubs.filter { it.id != state.clubId }
            .sortedByDescending { it.budget }.take(6)
        for (b in buyers) {
            if (b.roster.size >= 22 || !r.chance(0.3)) continue
            val target = state.transferList.mapNotNull { state.findPlayer(it) }
                .filter { p -> state.ownerOf(p.id)?.id != b.id }
                .maxByOrNull { it.skill } ?: continue
            val owner = state.ownerOf(target.id) ?: continue
            if (owner.id == state.clubId) continue   // never auto-take the user's player
            val fee = priceFor(target)
            if (b.budget < fee) continue
            b.budget -= fee; owner.budget += fee
            owner.roster.remove(target); b.roster.add(target)
            state.transferList.remove(target.id); target.transferListed = false
        }
    }

    /* ----------------------------- season end ----------------------------- */

    private fun seasonEnd(ds: Dataset, state: GameState, r: Rng) {
        val lines = mutableListOf<String>()
        // champions + promotion/relegation per division
        for (tier in 0..3) {
            val table = League.standings(state, tier)
            if (table.isEmpty()) continue
            lines.add("${ds.divisions[tier].name}: champions ${table.first().name}")
        }
        applyPromotionRelegation(state)
        // age players, develop toward potential, retire veterans
        for (c in state.clubs) {
            val it = c.roster.iterator()
            while (it.hasNext()) {
                val p = it.next()
                p.age++
                if (p.age <= 27 && p.skill < p.maxSkill) p.skill = (p.skill + r.int(0, 3)).coerceAtMost(p.maxSkill)
                if (p.age >= 31) p.skill = max(p.skill - r.int(0, 3), 8)
                if (p.age > 36 && r.chance(0.7)) it.remove()   // retirement
                p.apps = 0; p.goals = 0; p.yellow = 0; p.red = 0
                p.ratingSum = 0.0; p.ratingN = 0; p.fitness = 100; p.injuredWeeks = 0; p.suspended = 0
            }
            // backfill thin squads from reserve pool
            while (c.roster.size < 16 && ds.reservePool.isNotEmpty()) {
                val pd = ds.reservePool[r.int(0, ds.reservePool.size)]
                if (pd.name.isBlank()) continue
                c.roster.add(NewGame.run {
                    Player(state.nextPlayerId++, pd.name, pd.nat, pd.role,
                        r.int(18, 25), pd.skillSum.coerceIn(16, 30),
                        (pd.skillSum + 6).coerceAtMost(40), pd.value.coerceAtLeast(2) * 10_000)
                })
            }
            c.lineup = NewGame.pickBestEleven(c).toMutableList()
        }
        League.resetSeasonRecords(state)
        League.buildFixtures(state, ds.divisions.map { it.teamIds }.let { rebuildDivisions(state) })
        Cup.draw(state, r)
        state.season++; state.matchday = 0
        state.history.add("Season ${state.season - 1}: " + lines.joinToString("; "))
        state.inbox.add("Season ${state.season} begins. " +
            "${state.myClub.name} are in ${ds.divisions[state.myClub.division].name}.")
    }

    private fun rebuildDivisions(state: GameState): List<List<Int>> =
        (0..3).map { tier -> state.clubs.filter { it.division == tier }.map { it.id } }

    private fun applyPromotionRelegation(state: GameState) {
        val up = 3; val down = 3
        for (tier in 0..3) {
            val table = League.standings(state, tier)
            if (table.size < 6) continue
            if (tier < 3) table.takeLast(down).forEach { it.division = tier + 1 }
            if (tier > 0) table.take(up).forEach { it.division = tier - 1 }
        }
    }

    fun fmt(n: Long): String = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000 -> "%dk".format(n / 1_000)
        else -> n.toString()
    }
}
