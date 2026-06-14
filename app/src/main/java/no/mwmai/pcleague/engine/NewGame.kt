package no.mwmai.pcleague.engine

import no.mwmai.pcleague.data.*

object NewGame {

    /** Starting budget by division tier (in pounds). */
    private val budgetByTier = longArrayOf(4_000_000, 1_800_000, 800_000, 350_000)

    fun create(ds: Dataset, clubId: Int, managerName: String, lang: String, seed: Long): GameState {
        val rng = Rng(seed)
        val state = GameState(seed = seed, clubId = clubId, managerName = managerName, lang = lang)
        var pid = 0

        for (t in ds.teams) {
            val club = Club(id = t.id, name = t.name, division = t.division)
            club.budget = budgetByTier[t.division.coerceIn(0, 3)]
            for (pd in t.players) {
                if (pd.name.isBlank()) continue
                club.roster.add(makePlayer(pid++, pd, rng))
            }
            // ensure a sensible default lineup (best 11 by skill, valid shape)
            club.lineup = pickBestEleven(club).toMutableList()
            state.clubs.add(club)
        }
        state.nextPlayerId = pid

        // Youth squad (16) for the human club only — keeps saves lean.
        val me = state.club(clubId)
        repeat(16) {
            me.youth.add(generateYouth(state.nextPlayerId++, ds.namePools, rng))
        }

        // transfer list: each club lists 1-3 fringe players (the 'T' marked players)
        for (c in state.clubs) {
            val fringe = c.roster.sortedBy { it.skill }.take(rng.int(1, 4))
            for (p in fringe) { p.transferListed = true; state.transferList.add(p.id) }
        }

        League.buildFixtures(state, ds.divisions.map { it.teamIds })
        Cup.draw(state, rng)            // FA Cup first-round draw
        state.inbox.add(if (lang == "no") "Velkommen som manager i ${me.name}!"
                        else "Welcome to ${me.name}!")
        return state
    }

    private fun makePlayer(id: Int, pd: PlayerDef, rng: Rng): Player {
        val cur = pd.skillSum
        // potential ceiling: young players can still grow, veterans cannot
        val growth = when {
            pd.age <= 21 -> rng.int(2, 9)
            pd.age <= 25 -> rng.int(0, 5)
            else -> 0
        }
        val maxS = (cur + growth).coerceAtMost(40)
        return Player(
            id = id, name = pd.name, nat = pd.nat, role = pd.role, age = pd.age,
            skill = cur, maxSkill = maxS, value = (pd.value * 10_000).coerceAtLeast(20_000),
            form = 0.9 + rng.nextDouble() * 0.3, fitness = 90 + rng.int(0, 11),
        )
    }

    fun generateYouth(id: Int, pools: NamePools, rng: Rng): Player {
        val first = if (pools.first.isNotEmpty()) rng.pick(pools.first) else "Youth"
        val last = if (pools.last.isNotEmpty()) rng.pick(pools.last) else "Player"
        val role = rng.pick(listOf("GK", "DEF", "DEF", "MID", "MID", "ATT", "ATT"))
        val base = rng.int(14, 26)        // raw youth skill
        return Player(
            id = id, name = "$first $last".take(18), nat = "ENG", role = role,
            age = rng.int(16, 22), skill = base,
            maxSkill = (base + rng.int(4, 14)).coerceAtMost(40),
            value = rng.int(2, 30) * 10_000, form = 0.9 + rng.nextDouble() * 0.3,
        )
    }

    /** Default XI: 1 GK, then fill 4-4-2-ish by best available skill. */
    fun pickBestEleven(club: Club): List<Int> {
        val gk = club.roster.filter { it.role == "GK" }.sortedByDescending { it.skill }
        val def = club.roster.filter { it.role == "DEF" }.sortedByDescending { it.skill }
        val mid = club.roster.filter { it.role == "MID" }.sortedByDescending { it.skill }
        val att = club.roster.filter { it.role == "ATT" }.sortedByDescending { it.skill }
        val xi = mutableListOf<Int>()
        gk.firstOrNull()?.let { xi.add(it.id) }
        xi.addAll(def.take(4).map { it.id })
        xi.addAll(mid.take(4).map { it.id })
        xi.addAll(att.take(2).map { it.id })
        // pad to 11 from any remaining outfielders
        if (xi.size < 11) {
            val rest = club.roster.filter { it.id !in xi }.sortedByDescending { it.skill }
            xi.addAll(rest.take(11 - xi.size).map { it.id })
        }
        // subs (next 5)
        val subs = club.roster.filter { it.id !in xi }.sortedByDescending { it.skill }.take(5)
        return (xi.take(11) + subs.map { it.id })
    }
}
