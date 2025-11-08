package com.example.demo_musicsound.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.demo_musicsound.Audio.BeatPlayer
import com.example.demo_musicsound.Audio.RecorderManager
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.ln
import kotlin.math.min

/**
 * RecordScreen ora mostra anche i beat esportati da OfflineExporter
 * (cartella: getExternalFilesDir()/exports) e permette di selezionarli
 * per la riproduzione durante la registrazione.
 */
@Composable
fun RecordScreen(
    rec: RecorderManager,
    defaultPlayBeatDuringRec: Boolean = true
) {
    // --- Stato principale ---
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var lastPath by remember { mutableStateOf<String?>(null) }
    var vu by remember { mutableStateOf(0) }

    // Beat: elenco da /exports e selezione
    val exportsDir = remember { File(context.getExternalFilesDir(null), "exports").apply { mkdirs() } }
    var beats by remember { mutableStateOf(loadBeats(exportsDir)) }
    var selectedBeat: File? by remember { mutableStateOf(beats.firstOrNull()) }

    // Player beat e preview
    val beatPlayer = remember { BeatPlayer() }
    var playBeatDuringRec by remember { mutableStateOf(defaultPlayBeatDuringRec) }
    var beatVolume by remember { mutableFloatStateOf(0.9f) }

    val previewPlayer = remember { BeatPlayer() }
    var previewingBeat by remember { mutableStateOf<File?>(null) }

    // Preview registrazioni voce
    val previewRecPlayer = remember { BeatPlayer() }
    var previewingRec by remember { mutableStateOf<File?>(null) }

    // Permessi MIC
    var pendingStart by remember { mutableStateOf(false) }
    val micPermission = Manifest.permission.RECORD_AUDIO
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, micPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val requestMicPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted && pendingStart) {
            startRec(
                rec = rec,
                currentBeatFile = selectedBeat,
                playBeatDuringRec = playBeatDuringRec,
                beatPlayer = beatPlayer,
                beatVolume = beatVolume,
                setRecording = { recording = it },
                setLastPath = { lastPath = it },
                stopAllPreviews = {
                    previewPlayer.stop(); previewingBeat = null
                    previewRecPlayer.stop(); previewingRec = null
                },
                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
            )
        } else if (!granted) {
            Toast.makeText(context, "Permesso microfono negato", Toast.LENGTH_SHORT).show()
        }
        pendingStart = false
    }

    // Aggiorna VU mentre registri
    LaunchedEffect(recording) {
        if (recording) {
            while (true) {
                vu = rec.maxAmp()
                delay(80)
            }
        } else {
            vu = 0
        }
    }

    // Lista registrazioni voce
    var recFiles by remember { mutableStateOf(rec.listRecordings()) }
    LaunchedEffect(recording, lastPath) { recFiles = rec.listRecordings() }

    // Cleanup alla chiusura
    DisposableEffect(Unit) {
        onDispose {
            try { if (recording) rec.stop() } catch (_: Exception) {}
            beatPlayer.stop()
            previewPlayer.stop()
            previewRecPlayer.stop()
        }
    }

    // --- UI ---
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Record", style = MaterialTheme.typography.titleLarge)

        // ====== SEZIONE BEAT ESPORTATI ======
        Card {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Beat esportati", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { beats = loadBeats(exportsDir); if (selectedBeat !in beats) selectedBeat = beats.firstOrNull() }) {
                        Text("Aggiorna")
                    }
                }

                if (beats.isEmpty()) {
                    Text("Nessun beat esportato trovato (cartella: exports).")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(beats, key = { it.absolutePath }) { f ->
                            BeatRow(
                                file = f,
                                isSelected = (selectedBeat == f),
                                isPreviewing = (previewingBeat == f),
                                durationText = formatDurationMs(readDurationMs(f)) ?: "—",
                                onSelect = { selectedBeat = f },
                                onPlay = {
                                    if (previewingBeat == f) {
                                        previewPlayer.stop()
                                        previewingBeat = null
                                    } else {
                                        previewPlayer.stop()
                                        previewPlayer.play(f, loop = false, volume = 1f)
                                        previewingBeat = f
                                    }
                                }
                            )
                        }
                    }
                }

                // Controlli riproduzione beat durante REC
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = playBeatDuringRec,
                        onCheckedChange = { playBeatDuringRec = it },
                        enabled = !recording
                    )
                    Text("Riproduci beat selezionato durante REC")
                    Spacer(Modifier.weight(1f))
                    Text("Vol.")
                    Slider(
                        value = beatVolume,
                        onValueChange = {
                            beatVolume = it
                            beatPlayer.setVolume(beatVolume)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.width(160.dp),
                        enabled = !recording || beatPlayer.isPlaying()
                    )
                }
            }
        }

        // ====== SEZIONE REC ======
        val vuProgress = (min(1f, (20f * ln((vu + 1f)) / 100f))).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { vuProgress }, modifier = Modifier.fillMaxWidth())

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (!recording) {
                        if (!micGranted) {
                            pendingStart = true
                            requestMicPermission.launch(micPermission)
                        } else {
                            startRec(
                                rec = rec,
                                currentBeatFile = selectedBeat,
                                playBeatDuringRec = playBeatDuringRec,
                                beatPlayer = beatPlayer,
                                beatVolume = beatVolume,
                                setRecording = { recording = it },
                                setLastPath = { lastPath = it },
                                stopAllPreviews = {
                                    previewPlayer.stop(); previewingBeat = null
                                    previewRecPlayer.stop(); previewingRec = null
                                },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        }
                    } else {
                        stopRec(
                            rec = rec,
                            beatPlayer = beatPlayer,
                            setRecording = { recording = it },
                            setLastPath = { lastPath = it },
                            refreshFiles = { recFiles = rec.listRecordings() },
                            onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                        )
                    }
                },
                enabled = true // puoi registrare anche senza beat selezionato
            ) {
                Text(if (recording) "Stop" else "Rec")
            }

            Text(
                text = "Ultimo file: ${lastPath ?: "—"}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Divider()

        // ====== SEZIONE REGISTRAZIONI VOCE ======
        Text("Le mie registrazioni", style = MaterialTheme.typography.titleMedium)
        if (recFiles.isEmpty()) {
            Text("Nessuna registrazione ancora.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recFiles, key = { it.absolutePath }) { f ->
                    RecordingRow(
                        file = f,
                        durationText = rec.durationMs(f)?.let { ms -> formatDurationMs(ms) } ?: "—",
                        isPreviewing = (previewingRec == f),
                        onPlay = {
                            if (previewingRec == f) {
                                previewRecPlayer.stop()
                                previewingRec = null
                            } else {
                                previewRecPlayer.stop()
                                previewRecPlayer.play(f, loop = false, volume = 1f)
                                previewingRec = f
                            }
                        },
                        onDelete = {
                            if (previewingRec == f) {
                                previewRecPlayer.stop()
                                previewingRec = null
                            }
                            rec.delete(f)
                            recFiles = rec.listRecordings()
                        }
                    )
                }
            }
        }
    }
}

/* ---------------------------- Composables helper ---------------------------- */

@Composable
private fun BeatRow(
    file: File,
    isSelected: Boolean,
    isPreviewing: Boolean,
    durationText: String,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPreviewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPreviewing) "Stop" else "Play"
                )
            }
            Column(Modifier.weight(1f)) {
                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(durationText, style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalButton(onClick = onSelect) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Select")
                Spacer(Modifier.width(8.dp))
                Text(if (isSelected) "Selezionato" else "Seleziona")
            }
        }
    }
}

@Composable
private fun RecordingRow(
    file: File,
    durationText: String,
    isPreviewing: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPreviewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPreviewing) "Stop" else "Play"
                )
            }
            Column(Modifier.weight(1f)) {
                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(durationText, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

/* ------------------------------- Funzioni I/O ------------------------------- */

private fun loadBeats(exportsDir: File): List<File> {
    val list = exportsDir.listFiles { f -> f.isFile && f.extension.lowercase() in setOf("wav", "m4a", "mp3") }
    return list?.sortedByDescending { it.lastModified() } ?: emptyList()
}

/** Legge la durata in ms di un file audio in locale (minSdk 24 safe). */
private fun readDurationMs(file: File): Long? {
    val mmr = MediaMetadataRetriever()
    return try {
        mmr.setDataSource(file.absolutePath)
        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
    } catch (_: Exception) {
        null
    } finally {
        try { mmr.release() } catch (_: Exception) {}
    }
}

/** mm:ss o hh:mm:ss */
private fun formatDurationMs(ms: Long?): String? {
    ms ?: return null
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/* ----------------------------- Azioni di Record ---------------------------- */

private fun startRec(
    rec: RecorderManager,
    currentBeatFile: File?,
    playBeatDuringRec: Boolean,
    beatPlayer: BeatPlayer,
    beatVolume: Float,
    setRecording: (Boolean) -> Unit,
    setLastPath: (String?) -> Unit,
    stopAllPreviews: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        if (playBeatDuringRec && currentBeatFile != null) {
            beatPlayer.play(currentBeatFile, loop = true, volume = beatVolume)
        }
        val out = rec.start()
        setLastPath(out.absolutePath)
        setRecording(true)
        stopAllPreviews()
    } catch (t: Throwable) {
        beatPlayer.stop()
        setRecording(false)
        onError("Errore avvio registrazione: ${t.message ?: "sconosciuto"}")
    }
}

private fun stopRec(
    rec: RecorderManager,
    beatPlayer: BeatPlayer,
    setRecording: (Boolean) -> Unit,
    setLastPath: (String?) -> Unit,
    refreshFiles: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val saved = rec.stop()
        setRecording(false)
        beatPlayer.stop()
        refreshFiles()
        setLastPath(saved?.absolutePath)
    } catch (t: Throwable) {
        setRecording(false)
        beatPlayer.stop()
        onError("Errore stop registrazione: ${t.message ?: "sconosciuto"}")
    }
}
