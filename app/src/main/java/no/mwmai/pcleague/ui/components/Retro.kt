package no.mwmai.pcleague.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.mwmai.pcleague.ui.theme.Dos
import no.mwmai.pcleague.ui.theme.Mono

/** A bordered DOS panel with a centred title bar (┌─ title ─┐ feel). */
/**
 * Bordered DOS panel with a title bar. When [fill] is true the content area
 * takes the frame's remaining (bounded) height — required for any frame that
 * hosts a LazyColumn or a verticalScroll Column. Callers using fill=true must
 * give the frame a bounded height (e.g. Modifier.weight(1f) in a Column).
 */
@Composable
fun DosFrame(
    title: String,
    modifier: Modifier = Modifier,
    right: String? = null,
    fill: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(Dos.Panel)
            .border(2.dp, Dos.Frame, RoundedCornerShape(2.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(Dos.Frame).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title.uppercase(), color = Dos.Black, fontFamily = Mono,
                fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (right != null) Text(right, color = Dos.Black, fontFamily = Mono,
                fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        val inner = if (fill) Modifier.weight(1f).fillMaxWidth().padding(6.dp)
        else Modifier.fillMaxWidth().padding(6.dp)
        Column(inner, content = content)
    }
}

/** Status line at the bottom: cyan bar with key hints, like the original. */
@Composable
fun StatusBar(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().background(Dos.StatusBg).padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text, color = Dos.StatusFg, fontFamily = Mono, fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DosButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: androidx.compose.ui.graphics.Color = Dos.BrightCyan,
    onClick: () -> Unit,
) {
    val fg = if (enabled) Dos.Black else Dos.DarkGray
    Box(
        modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (enabled) color else Dos.DarkGray)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** A menu entry like the office hub: "1  Squad". */
@Composable
fun MenuRow(key: String, label: String, accent: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, color = Dos.Yellow, fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 16.sp, modifier = Modifier.width(34.dp))
        Text(label, color = if (accent) Dos.BrightGreen else Dos.White, fontFamily = Mono,
            fontSize = 16.sp)
    }
}

@Composable
fun cell(text: String, w: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color = Dos.Text,
         bold: Boolean = false) {
    Text(text, color = color, fontFamily = Mono, fontSize = 12.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1, modifier = Modifier.width(w))
}

@Composable
fun cellW(text: String, weight: Float, scope: RowScope, color: androidx.compose.ui.graphics.Color = Dos.Text,
          bold: Boolean = false) {
    with(scope) {
        Text(text, color = color, fontFamily = Mono, fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1, modifier = Modifier.weight(weight))
    }
}

@Composable
fun ScreenScaffold(
    status: String,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Dos.Background)) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(8.dp), content = body)
        StatusBar(status)
    }
}

@Composable
fun <T> DosList(items: List<T>, modifier: Modifier = Modifier, row: @Composable (T) -> Unit) {
    LazyColumn(modifier.fillMaxWidth()) { items(items.size) { row(items[it]) } }
}
