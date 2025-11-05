package com.example.demo_musicsound.ui.screen

import android.annotation.SuppressLint
import android.media.MediaPlayer
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import com.example.demo_musicsound.Audio.OfflineExporter
import com.example.demo_musicsound.Audio.Sequencer
import com.example.demo_musicsound.Audio.SoundManager

// --- MODEL -------------------------------------------------------------------

private data class Pad(val label: String, val resName: String)

private val bankA = listOf(
    Pad("KICK","kick"), Pad("SNARE","snare"), Pad("HAT","hat"),
    Pad("CLAP","clap"), Pad("TOM1","tom1"), Pad("TOM2","tom2"),
)
private val bankB = listOf(
    Pad("RIM","rim"), Pad("SHAK","shaker"), Pad("OHAT","ohat"),
    Pad("RIDE","ride"), Pad("FX1","fx1"), Pad("FX2","fx2"),
)
private val allRes = (bankA + bankB).map { it.resName }

// --- SCREEN ------------------------------------------------------------------

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PadScreen(
    sound: SoundManager,
    seq: Sequencer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val running by seq.running.collectAsState()

    var bpm by remember { mutableStateOf(120) }
    var tab by remember { mutableStateOf(0) }     // 0 = A, 1 = B
    var curStep by remember { mutableStateOf(0) } // highlight step in play

    // garantisco i pattern per tutti i nomi
    LaunchedEffect(Unit) { seq.ensureAll(allRes) }

    val page = if (tab == 0) bankA else bankB
    val pageRes = page.map { it.resName }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- TOP CONTROLS -------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- BPM CONTROLS ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = {
                            if (bpm > 60) { bpm -= 2; seq.setBpm(bpm) }
                        },
                        modifier = Modifier.size(42.dp)
                    ) { Text("–") }

                    Text("BPM $bpm", style = MaterialTheme.typography.titleMedium)

                    OutlinedIconButton(
                        onClick = {
                            if (bpm < 200) { bpm += 2; seq.setBpm(bpm) }
                        },
                        modifier = Modifier.size(42.dp)
                    ) { Text("+") }
                }

                // --- ACTION BUTTONS (Play, Clear, Share) ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (running) seq.stop() else seq.start(scope, sound) { step -> curStep = step }
                        },
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text(if (running) "Stop" else "Play")
                    }

                    FilledIconButton(
                        onClick = { seq.clear(pageRes) },
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear Page")
                    }

                    FilledIconButton(
                        onClick = {
                            // share/export handler
                            scope.launch {
                                try {
                                    val names = allRes.filter { resIdOf(context, it) != 0 }
                                    val steps = seq.pattern(names.first()).size
                                    val tracks = names.map { name ->
                                        OfflineExporter.TrackMix(
                                            resName = name,
                                            pattern = seq.pattern(name).toList(),
                                            sample = OfflineExporter.loadWavPCM16(context, resIdOf(context, name))
                                        )
                                    }
                                    val out = OfflineExporter.exportBeatToWav(
                                        context = context,
                                        bpm = bpm,
                                        steps = steps,
                                        dstSr = 44100,
                                        tracks = tracks
                                    )
                                    snackbar.showSnackbar("Exported: ${out.name}")
                                } catch (t: Throwable) {
                                    snackbar.showSnackbar("Export failed: ${t.message}")
                                }
                            }
                        },
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF7C4DFF))
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Export")
                    }
                }
            }

            // --- BANKS --------------------------------------------------------
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Bank A") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Bank B") })
            }

            // --- PAD GRID (3x2) ----------------------------------------------
            // --- PAD GRID (3x2 rettangolari che entrano in 200.dp) ----------------------
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // spazio totale disponibile per i pad
            ) {
                val spacing = 8.dp                      // spazio tra le card
                val itemHeight = 80.dp
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    items(page) { p ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()        // usa tutta la larghezza della cella
                                .height(itemHeight)    // <-- rettangolare, non più quadrato
                                .clickable { sound.play(p.resName) },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(p.label)
                            }
                        }
                    }
                }
            }

            // --- SEQUENCER ----------------------------------------------------
            SequencerGrid(seq = seq, tracks = page, currentStep = curStep)
        }
    }
}

// --- UI PARTIALS -------------------------------------------------------------

@Composable
private fun SequencerGrid(
    seq: Sequencer,
    tracks: List<Pad>,
    currentStep: Int
) {
    // colori/size compatti per far entrare i 16 step senza scroll orizzontale
    val activeFill = Color(0xFFA5D6A7)
    val activeBorder = Color(0xFF00C853)
    val nowBg = Color(0xFFEDE7F6)
    val boxSize = 20.dp
    val gap = 4.dp

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tracks.size) { idx ->
            val pad = tracks[idx]
            val pattern = seq.pattern(pad.resName)

            Column(Modifier.fillMaxWidth()) {
                Text(
                    pad.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pattern.size) { i ->
                        val active = pattern[i]
                        val isNow = i == currentStep
                        Box(
                            Modifier
                                .size(boxSize)
                                .background(
                                    when {
                                        active -> activeFill
                                        isNow -> nowBg
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        active -> activeBorder
                                        isNow -> Color(0xFF7C4DFF)
                                        else -> Color.Gray
                                    }
                                )
                                .clickable { seq.toggle(pad.resName, i) } // toggle immediato (verde subito)
                        )
                    }
                }
            }
        }
    }
}

// --- UTILS -------------------------------------------------------------------

private fun resIdOf(context: android.content.Context, name: String): Int =
    context.resources.getIdentifier(name, "raw", context.packageName)