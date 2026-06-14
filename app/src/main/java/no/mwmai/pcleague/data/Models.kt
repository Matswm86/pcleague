package no.mwmai.pcleague.data

import kotlinx.serialization.Serializable

/* ---------- dataset (read-only, parsed from assets/pcleague.json) ---------- */

@Serializable
data class Dataset(
    val meta: Meta = Meta(),
    val divisions: List<DivisionDef> = emptyList(),
    val teams: List<TeamDef> = emptyList(),
    val reservePool: List<PlayerDef> = emptyList(),
    val uiStrings: List<UiString> = emptyList(),
    val namePools: NamePools = NamePools(),
)

@Serializable
data class Meta(
    val title: String = "PcLeague 2.1",
    val author: String = "Asle Rokstad",
    val year: Int = 1994,
    val source: String = "",
)

@Serializable
data class DivisionDef(val tier: Int, val name: String, val teamIds: List<Int>)

@Serializable
data class TeamDef(
    val id: Int = 0,
    val name: String = "",
    val division: Int = 0,
    val divPos: Int = 0,
    val global: Int = 0,
    val players: List<PlayerDef> = emptyList(),
)

@Serializable
data class PlayerDef(
    val name: String = "",
    val nat: String = "ENG",
    val role: String = "ATT",     // GK / DEF / MID / ATT
    val age: Int = 20,
    val skill: List<Int> = listOf(5, 5, 5, 5),
    val skillSum: Int = 20,
    val value: Int = 0,
    val fitness: Int = 100,
    val flag: Int = 0,
    val rawStats: String = "",
)

@Serializable
data class UiString(val en: String = "", val no: String = "")

@Serializable
data class NamePools(val first: List<String> = emptyList(), val last: List<String> = emptyList())

/* ---------- runtime game state (serialized into save slots) ---------- */

@Serializable
data class Player(
    val id: Int,
    val name: String,
    val nat: String,
    val role: String,
    var age: Int,
    var skill: Int,          // current overall shape (the "SUM SHAPE NOW")
    val maxSkill: Int,       // ceiling (MAX SUM SHAPE)
    var value: Int,
    var form: Double = 1.0,  // good/bad period multiplier ~0.7..1.3
    var fitness: Int = 100,
    var injuredWeeks: Int = 0,
    var suspended: Int = 0,
    var transferListed: Boolean = false,
    // season tallies
    var apps: Int = 0,
    var goals: Int = 0,
    var yellow: Int = 0,
    var red: Int = 0,
    var ratingSum: Double = 0.0,
    var ratingN: Int = 0,
) {
    val avgRating: Double get() = if (ratingN == 0) 0.0 else ratingSum / ratingN
    val available: Boolean get() = injuredWeeks == 0 && suspended == 0
}

@Serializable
data class Club(
    val id: Int,
    val name: String,
    var division: Int,
    val roster: MutableList<Player> = mutableListOf(),
    val youth: MutableList<Player> = mutableListOf(),
    var lineup: MutableList<Int> = mutableListOf(),   // player ids, first 11 = XI
    var tactic: Int = 1,                              // 0 def, 1 normal, 2 att
    var teamForm: Double = 1.0,
    var budget: Long = 0,
    // league record (current division season)
    var played: Int = 0, var won: Int = 0, var drawn: Int = 0, var lost: Int = 0,
    var gf: Int = 0, var ga: Int = 0, var points: Int = 0,
) {
    val gd: Int get() = gf - ga
}

@Serializable
data class Fixture(
    val round: Int,
    val division: Int,
    val home: Int,
    val away: Int,
    var played: Boolean = false,
    var hg: Int = -1,
    var ag: Int = -1,
)

@Serializable
data class MatchEventLog(
    val minute: Int,
    val text: String,
    val home: Boolean,
)

@Serializable
data class MatchResult(
    val homeId: Int,
    val awayId: Int,
    val hg: Int,
    val ag: Int,
    val events: List<MatchEventLog> = emptyList(),
    val homeScorers: List<String> = emptyList(),
    val awayScorers: List<String> = emptyList(),
)

@Serializable
data class CupTie(
    val round: Int,
    val home: Int,
    val away: Int,
    var played: Boolean = false,
    var hg: Int = -1,
    var ag: Int = -1,
    var winner: Int = -1,
)

@Serializable
data class GameState(
    var seed: Long,
    var lang: String = "en",                 // "en" or "no"
    var managerName: String = "",
    var clubId: Int,
    var season: Int = 1,
    var matchday: Int = 0,                    // index into rounds for league
    var clubs: MutableList<Club> = mutableListOf(),
    var fixtures: MutableList<Fixture> = mutableListOf(),
    var cup: MutableList<CupTie> = mutableListOf(),
    var cupRound: Int = 0,
    var lastResults: MutableList<MatchResult> = mutableListOf(),
    var transferList: MutableList<Int> = mutableListOf(),   // player ids on market
    var nextPlayerId: Int = 0,
    var history: MutableList<String> = mutableListOf(),      // season summaries
    var inbox: MutableList<String> = mutableListOf(),        // news items
) {
    fun club(id: Int): Club = clubs.first { it.id == id }
    val myClub: Club get() = club(clubId)
    fun findPlayer(pid: Int): Player? {
        for (c in clubs) {
            c.roster.firstOrNull { it.id == pid }?.let { return it }
            c.youth.firstOrNull { it.id == pid }?.let { return it }
        }
        return null
    }
    fun ownerOf(pid: Int): Club? = clubs.firstOrNull { c ->
        c.roster.any { it.id == pid } || c.youth.any { it.id == pid }
    }
}
