package com.example.demo_musicsound.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.demo_musicsound.Audio.OfflineExporter
import com.example.demo_musicsound.Audio.Sequencer
import com.example.demo_musicsound.Audio.SoundManager
import com.example.mybeat.ui.theme.*
import kotlinx.coroutines.launch

// ------------------------------------------------------------
// MODEL
// ------------------------------------------------------------

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

// ------------------------------------------------------------
// SCREEN
// ------------------------------------------------------------

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
    var curStep by remember { mutableStateOf(0) } // step corrente per highlight

    // assicura che esistano tutti i pattern
    LaunchedEffect(Unit) { seq.ensureAll(allRes) }

    val page = if (tab == 0) bankA else bankB
    val pageRes = page.map { it.resName }

    // ---- dialog per salvataggio beat ----
    var showNameDialog by remember { mutableStateOf(false) }
    var beatName by rememberSaveable { mutableStateOf(defaultBeatName()) }
    var exporting by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = GrayBg,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --------------------------------------------------------
            // TOP CONTROLS (BPM + Play/Clear/Export)
            // --------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // BPM
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = { if (bpm > 60) { bpm -= 2; seq.setBpm(bpm) } },
                        modifier = Modifier.size(44.dp),
                        border = BorderStroke(1.dp, PurpleAccent),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = Color.White)
                    ) { Text("–") }

                    Text("BPM $bpm", style = MaterialTheme.typography.titleMedium, color = Color.White)

                    OutlinedIconButton(
                        onClick = { if (bpm < 200) { bpm += 2; seq.setBpm(bpm) } },
                        modifier = Modifier.size(44.dp),
                        border = BorderStroke(1.dp, PurpleAccent),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = Color.White)
                    ) { Text("+") }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (running) seq.stop() else seq.start(scope, sound) { step -> curStep = step }
                        },
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (running) "Stop" else "Play") }

                    FilledIconButton(
                        onClick = { seq.clear(pageRes) },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PurpleAccent,
                            contentColor = Color.White
                        )
                    ) { Icon(Icons.Filled.Delete, contentDescription = "Clear Page") }

                    FilledIconButton(
                        onClick = {
                            // apre il popup per chiedere il nome file
                            beatName = defaultBeatName()
                            showNameDialog = true
                        },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PurpleAccent,
                            contentColor = Color.White
                        )
                    ) { Icon(Icons.Filled.Download, contentDescription = "Export") }
                }
            }

            // --------------------------------------------------------
            // BANK SWITCHER (segmented pill flottante)
            // --------------------------------------------------------
            BankSwitch(
                tab = tab,
                onTab = { tab = it },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // --------------------------------------------------------
            // PAD GRID (3x2 rettangolari)
            // --------------------------------------------------------
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val spacing = 10.dp
                val itemHeight = 72.dp
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    items(page) { p ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .clickable { sound.play(p.resName) },
                            colors = CardDefaults.cardColors(
                                containerColor = GraySurface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(p.label, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------------
            // SEQUENCER
            // --------------------------------------------------------
            SequencerGrid(seq = seq, tracks = page, currentStep = curStep)
        }
    }

    // --------------------------------------------------------
    // POPUP: Name before export
    // --------------------------------------------------------
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!exporting) showNameDialog = false },
            title = { Text("Save beat") },
            text = {
                OutlinedTextField(
                    value = beatName,
                    onValueChange = { if (it.length <= 40) beatName = it },
                    label = { Text("File name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !exporting,
                    onClick = {
                        exporting = true
                        scope.launch {
                            try {
                                // prepara i dati come facevi già
                                val names = allRes.filter { resIdOf(context, it) != 0 }
                                val steps = seq.pattern(names.first()).size
                                val tracks = names.map { name ->
                                    OfflineExporter.TrackMix(
                                        resName = name,
                                        pattern = seq.pattern(name).toList(),
                                        sample = OfflineExporter.loadWavPCM16(context, resIdOf(context, name))
                                    )
                                }

                                // export
                                val out = OfflineExporter.exportBeatToWav(
                                    context = context,
                                    bpm = bpm,
                                    steps = steps,
                                    dstSr = 44100,
                                    tracks = tracks
                                )

                                // rinomina al nome scelto (evita conflitti)
                                val finalFile = run {
                                    val target = java.io.File(out.parentFile, beatName.slugOrDefault() + ".wav")
                                    if (out.name != target.name) {
                                        val safe = uniqueTarget(target)
                                        out.renameTo(safe)
                                        safe
                                    } else out
                                }

                                snackbar.showSnackbar("Exported: ${finalFile.name}")
                            } catch (t: Throwable) {
                                snackbar.showSnackbar("Export failed: ${t.message}")
                            } finally {
                                exporting = false
                                showNameDialog = false
                            }
                        }
                    }
                ) { Text(if (exporting) "Saving…" else "Save") }
            },
            dismissButton = {
                TextButton(
                    enabled = !exporting,
                    onClick = { showNameDialog = false }
                ) { Text("Cancel") }
            }
        )
    }
}

// ------------------------------------------------------------
// PARTIALS
// ------------------------------------------------------------

@Composable
private fun BankSwitch(
    tab: Int,
    onTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pill = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .clip(pill)
            .background(PurpleBar)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        @Composable
        fun Seg(text: String, selected: Boolean, onClick: () -> Unit) {
            TextButton(
                onClick = onClick,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected) PurpleAccent else Color.Transparent,
                    contentColor   = if (selected) Color.Black else Color.White
                )
            ) { Text(text, style = MaterialTheme.typography.labelLarge) }
        }
        Seg("Bank A", tab == 0) { onTab(0) }
        Seg("Bank B", tab == 1) { onTab(1) }
    }
}

@Composable
private fun SegBtn(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) PurpleAccent else Color.Transparent
    val fg = if (selected) Color.White else TextSecondary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = if (selected) null else BorderStroke(1.dp, OutlineDark),
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Text(
            label,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SequencerGrid(
    seq: Sequencer,
    tracks: List<Pad>,
    currentStep: Int
) {
    val activeFill = Color(0xFF3DDC84)          // verde attivo
    val activeBorder = Color(0xFF2ECF74)
    val nowBorder = PurpleAccent
    val idleBorder = OutlineDark
    val nowBg = nowBorder.copy(alpha = 0.12f)
    val boxSize = 20.dp
    val gap = 3.dp
    val corner = RoundedCornerShape(6.dp)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tracks.size) { idx ->
            val pad = tracks[idx]
            val pattern = seq.pattern(pad.resName)

            Column(Modifier.fillMaxWidth()) {
                Text(
                    pad.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp)
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
                                        active -> activeFill.copy(alpha = 0.18f)
                                        isNow  -> nowBg
                                        else   -> Color.Transparent
                                    },
                                    shape = corner
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        active -> activeBorder
                                        isNow  -> nowBorder
                                        else   -> idleBorder
                                    },
                                    shape = corner
                                )
                                .clickable { seq.toggle(pad.resName, i) }
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// UTILS
// ------------------------------------------------------------

private fun resIdOf(context: android.content.Context, name: String): Int =
    context.resources.getIdentifier(name, "raw", context.packageName)

private fun defaultBeatName(): String =
    "beat_${System.currentTimeMillis() % 100000}"

private fun String.slugOrDefault(): String {
    val slug = lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
    return if (slug.isEmpty()) defaultBeatName() else slug
}

private fun uniqueTarget(target: java.io.File): java.io.File {
    if (!target.exists()) return target
    val base = target.nameWithoutExtension
    val ext = target.extension
    var i = 2
    while (true) {
        val candidate = java.io.File(target.parentFile, "${base}_$i.$ext")
        if (!candidate.exists()) return candidate
        i++
    }
}
