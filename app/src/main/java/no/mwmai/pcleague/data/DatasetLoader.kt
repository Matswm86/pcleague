package no.mwmai.pcleague.data

import android.content.Context
import kotlinx.serialization.json.Json

object DatasetLoader {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    @Volatile private var cached: Dataset? = null

    fun load(context: Context): Dataset {
        cached?.let { return it }
        val text = context.assets.open("pcleague.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<Dataset>(text).also { cached = it }
    }
}
