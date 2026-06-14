package no.mwmai.pcleague

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import no.mwmai.pcleague.ui.PcLeagueApp
import no.mwmai.pcleague.ui.theme.Dos
import no.mwmai.pcleague.ui.theme.PcLeagueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PcLeagueTheme {
                Surface(Modifier.fillMaxSize(), color = Dos.Background) {
                    PcLeagueApp(Modifier.windowInsetsPadding(WindowInsets.systemBars))
                }
            }
        }
    }
}
