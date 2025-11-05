package com.example.demo_musicsound.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalDensity
import com.example.demo_musicsound.Audio.Sequencer
import com.example.demo_musicsound.Audio.SoundManager
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.ranges.until


private data class Pad(val label: String, val resName: String)

private val pageA = listOf(
    Pad("KICK", "kick"), Pad("SNARE", "snare"), Pad("HAT", "hat"),
    Pad("CLAP", "clap"), Pad("TOM1", "tom1"), Pad("TOM2", "tom2"),
)
private val pageB = listOf(
    Pad("RIM", "rim"), Pad("SHAK", "shaker"), Pad("OHAT", "ohat"),
    Pad("RIDE", "ride"), Pad("FX1", "fx1"), Pad("FX2", "fx2"),
)
private val allResNames = (pageA + pageB).map { it.resName }


@Composable
fun PadScreen(sound: SoundManager, seq: Sequencer) {
    var curStep by remember { mutableStateOf(0) }
    var bpm by remember { mutableStateOf(120) }
    var selectedTab by remember { mutableStateOf(0) }   // 0 = Bank A, 1 = Bank B
    val running by seq.running.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { seq.ensureAll(allResNames) }

    val padsThisPage = if (selectedTab == 0) pageA else pageB
    val resNamesThisPage = padsThisPage.map { it.resName }

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = { if (bpm > 60) { bpm -= 2; seq.setBpm(bpm) } }) { Text("-") }
            Text("BPM $bpm")
            OutlinedButton(onClick = { if (bpm < 200) { bpm += 2; seq.setBpm(bpm) } }) { Text("+") }

            Spacer(Modifier.weight(1f))

            Button(onClick = {
                if (running) seq.stop()
                else seq.start(scope, sound) { curStep = it }
            }) { Text(if (running) "Stop" else "Play") }

            FilledIconButton(
                onClick = { seq.clear(resNamesThisPage) },
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            ) { Icon(Icons.Filled.Delete, contentDescription = "Clear Page") }
        }

        // Tab Bank A/B
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Bank A") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Bank B") })
        }

        // Grid 3x2
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(padsThisPage) { p ->
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { sound.play(p.resName) },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(p.label) }
                }
            }
        }

        // Sequencer
        SequencerGrid(seq = seq, tracks = padsThisPage, currentStep = curStep)
    }
}

@Composable
private fun SequencerGrid(
    seq: Sequencer,
    tracks: List<Pad>,
    currentStep: Int
) {
    val greenFill   = Color(0xFFA5D6A7)
    val greenBorder = Color(0xFF00C853)
    val currentBg   = Color(0xFFEDE7F6)
    val boxSize     = 18.dp    // 👈 dimensione fissa dei quadratini
    val gap         = 6.dp     // 👈 spazio tra i quadratini

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tracks.size) { idx ->
            val pad = tracks[idx]
            val pattern = seq.pattern(pad.resName)

            Column(Modifier.fillMaxWidth()) {

                // Nome del suono sopra la riga
                Text(
                    pad.label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Riga con i quadratini
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until pattern.size) {
                        val active = pattern[i]
                        val isCurrent = (i == currentStep)

                        Box(
                            Modifier
                                .size(boxSize)
                                .background(
                                    when {
                                        active -> greenFill
                                        isCurrent -> currentBg
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        active -> greenBorder
                                        isCurrent -> Color(0xFF7C4DFF)
                                        else -> Color.Gray
                                    }
                                )
                                .clickable { seq.toggle(pad.resName, i) }
                        )
                    }
                }
            }
        }
    }
}