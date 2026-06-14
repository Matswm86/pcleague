package no.mwmai.pcleague.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Save/load full GameState to internal storage. 4 slots, faithful to the
 *  original's named-game save/continue feature. */
object SaveManager {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    const val SLOTS = 4

    private fun dir(ctx: Context): File = File(ctx.filesDir, "saves").apply { mkdirs() }
    private fun file(ctx: Context, slot: Int) = File(dir(ctx), "slot$slot.json")

    data class SlotInfo(val slot: Int, val exists: Boolean, val label: String)

    fun listSlots(ctx: Context): List<SlotInfo> = (0 until SLOTS).map { s ->
        val f = file(ctx, s)
        if (!f.exists()) SlotInfo(s, false, "— empty —")
        else runCatching {
            val st = json.decodeFromString<GameState>(f.readText())
            SlotInfo(s, true, "${st.myClub.name} · S${st.season} R${st.matchday}")
        }.getOrElse { SlotInfo(s, true, "saved game") }
    }

    fun save(ctx: Context, slot: Int, state: GameState) {
        file(ctx, slot).writeText(json.encodeToString(state))
    }

    fun load(ctx: Context, slot: Int): GameState? {
        val f = file(ctx, slot)
        if (!f.exists()) return null
        return runCatching { json.decodeFromString<GameState>(f.readText()) }.getOrNull()
    }

    fun delete(ctx: Context, slot: Int) {
        file(ctx, slot).delete()
    }

    // autosave slot (separate file)
    private fun autoFile(ctx: Context) = File(dir(ctx), "autosave.json")
    fun autosave(ctx: Context, state: GameState) = autoFile(ctx).writeText(json.encodeToString(state))
    fun loadAuto(ctx: Context): GameState? = autoFile(ctx).takeIf { it.exists() }
        ?.let { runCatching { json.decodeFromString<GameState>(it.readText()) }.getOrNull() }
    fun hasAuto(ctx: Context): Boolean = autoFile(ctx).exists()
}
