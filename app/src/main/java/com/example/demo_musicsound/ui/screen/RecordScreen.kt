package com.example.demo_musicsound.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.demo_musicsound.Audio.BeatPlayer
import com.example.demo_musicsound.Audio.RecorderManager
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.ln
import kotlin.math.min

/* ---------------------------------------------------------- */
/* -----------------------  RECORD  UI  ---------------------- */
/* ---------------------------------------------------------- */

@Composable
fun RecordScreen(
    rec: RecorderManager,
    defaultPlayBeatDuringRec: Boolean = true
) {
    val context = LocalContext.current

    // ---- STATE ----
    var recording by remember { mutableStateOf(false) }
    var lastPath by remember { mutableStateOf<String?>(null) }
    var vu by remember { mutableStateOf(0) }

    val exportsDir = remember { File(context.getExternalFilesDir(null), "exports").apply { mkdirs() } }
    var beats by remember { mutableStateOf(loadBeats(exportsDir)) }
    var selectedBeat: File? by remember { mutableStateOf(beats.firstOrNull()) }

    val beatPlayer = remember { BeatPlayer() }
    var playBeatDuringRec by remember { mutableStateOf(defaultPlayBeatDuringRec) }
    var beatVolume by remember { mutableFloatStateOf(0.9f) }

    val previewBeat = remember { BeatPlayer() }
    var previewingBeat by remember { mutableStateOf<File?>(null) }

    val previewRec = remember { BeatPlayer() }
    var previewingRec by remember { mutableStateOf<File?>(null) }

    // mic permission
    val micPermission = Manifest.permission.RECORD_AUDIO
    var pendingStart by remember { mutableStateOf(false) }
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, micPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val requestMicPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            micGranted = granted
            if (granted && pendingStart) {
                startRec(
                    rec, selectedBeat, playBeatDuringRec, beatPlayer, beatVolume,
                    setRecording = { recording = it },
                    setLastPath = { lastPath = it },
                    stopAllPreviews = {
                        previewBeat.stop(); previewingBeat = null
                        previewRec.stop(); previewingRec = null
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                )
            } else if (!granted) {
                Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
            pendingStart = false
        }

    // VU meter poll
    LaunchedEffect(recording) {
        if (recording) {
            while (true) {
                vu = rec.maxAmp()
                delay(80)
            }
        } else vu = 0
    }

    // recordings list
    var recFiles by remember { mutableStateOf(rec.listRecordings()) }
    LaunchedEffect(recording, lastPath) { recFiles = rec.listRecordings() }

    DisposableEffect(Unit) {
        onDispose {
            try { if (recording) rec.stop() } catch (_: Exception) {}
            beatPlayer.stop(); previewBeat.stop(); previewRec.stop()
        }
    }

    // tokens stilistici
    val boxShape = RoundedCornerShape(24.dp)
    val boxPadding = 16.dp
    val sectionSpacing = 16.dp
    val boxSpacing = 12.dp
    val beatCardColor = Color(0xFF23232B) // grigio più scuro per le card interne

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {

        if (recording) {
            var visible by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                while (true) {
                    visible = !visible
                    delay(600)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFB00020).copy(alpha = 0.9f))
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (visible) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Recording...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        // ======= SECTION: EXPORTED BEATS (TITLE + REFRESH + BOX) =======
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Exported beats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(
                contentPadding = PaddingValues(vertical = 0.dp, horizontal = 8.dp),
                onClick = {
                    beats = loadBeats(exportsDir)
                    if (selectedBeat !in beats) selectedBeat = beats.firstOrNull()
                }
            ) { Text("Refresh") }
        }

        // Box che contiene la lista (max 3 elementi visibili)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 0.dp, max = (65.dp * 3) + (8.dp * 2) + (boxPadding * 2)), // 3 righe
            shape = boxShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
            )
        ) {
            if (beats.isEmpty()) {
                Text(
                        text = "No beats yet.",
                        modifier = Modifier.padding(boxPadding),
                    )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(boxPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(beats, key = { it.absolutePath }) { f ->
                        BeatRow(
                            file = f,
                            isSelected = selectedBeat == f,
                            isPreviewing = previewingBeat == f,
                            durationText = formatDurationMs(readDurationMs(f)),
                            colors = CardDefaults.cardColors(containerColor = beatCardColor),
                            onSelect = { selectedBeat = f },
                            onPlay = {
                                if (previewingBeat == f) {
                                    previewBeat.stop(); previewingBeat = null
                                } else {
                                    previewBeat.stop()
                                    previewBeat.play(f, loop = false, volume = 1f)
                                    previewingBeat = f
                                }
                            }
                        )
                    }
                }
            }
        }

        // ======= SECTION: RECORDING CONTROLS (TITLE + BOX) =======
        Text("Recording controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = boxShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(boxPadding),
                verticalArrangement = Arrangement.spacedBy(boxSpacing)
            ) {
                // toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = playBeatDuringRec, onCheckedChange = { playBeatDuringRec = it })
                    Spacer(Modifier.width(12.dp))
                    Text("Play selected beat while recording")
                }

                // volume row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vol.")
                    Spacer(Modifier.width(12.dp))
                    Slider(
                        modifier = Modifier.weight(1f),
                        value = beatVolume,
                        onValueChange = {
                            beatVolume = it
                            beatPlayer.setVolume(beatVolume)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${(beatVolume * 100).toInt()}%")
                }

                Divider()

                // rec button + last file
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (!recording) {
                                if (!micGranted) {
                                    pendingStart = true
                                    requestMicPermission.launch(micPermission)
                                } else {
                                    startRec(
                                        rec, selectedBeat, playBeatDuringRec, beatPlayer, beatVolume,
                                        setRecording = { recording = it },
                                        setLastPath = { lastPath = it },
                                        stopAllPreviews = {
                                            previewBeat.stop(); previewingBeat = null
                                            previewRec.stop(); previewingRec = null
                                        },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                    )
                                }
                            } else {
                                stopRec(
                                    rec, beatPlayer,
                                    setRecording = { recording = it },
                                    setLastPath = { lastPath = it },
                                    refreshFiles = { recFiles = rec.listRecordings() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                )
                            }
                        }
                    ) {
                        Text(if (recording) "Stop" else "Rec")
                    }

                    Text(
                        text = "Last file: ${lastPath ?: "—"}",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ======= SECTION: MY RECORDINGS (TITLE + BOX) =======
        Text("My recordings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = boxShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
            )
        ) {
            if (recFiles.isEmpty()) {
                Text(
                    "No recordings yet.",
                    modifier = Modifier.padding(boxPadding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(boxPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recFiles, key = { it.absolutePath }) { f ->
                        RecordingRow(
                            file = f,
                            durationText = formatDurationMs(rec.durationMs(f)),
                            isPreviewing = previewingRec == f,
                            onPlay = {
                                if (previewingRec == f) {
                                    previewRec.stop(); previewingRec = null
                                } else {
                                    previewRec.stop(); previewRec.play(f, false, 1f); previewingRec = f
                                }
                            },
                            onDelete = {
                                if (previewingRec == f) { previewRec.stop(); previewingRec = null }
                                rec.delete(f); recFiles = rec.listRecordings()
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------------- ROWS ---------------------------------------- */

@Composable
private fun BeatRow(
    file: File,
    isSelected: Boolean,
    isPreviewing: Boolean,
    durationText: String,
    colors: CardColors,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(65.dp),
        colors = colors,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
            Column(Modifier.weight(1f)) {
                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(durationText, style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalButton(onClick = onSelect) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSelected) "Selected" else "Select")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF23232B)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
            Column(Modifier.weight(1f)) {
                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(durationText, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null) }
        }
    }
}



/* ---------------------------------------------------------- */
/* ----------------------  File helpers  --------------------- */
/* ---------------------------------------------------------- */

private fun loadBeats(dir: File): List<File> =
    dir.listFiles { f -> f.isFile && f.extension.lowercase() in setOf("wav", "m4a", "mp3") }
        ?.sortedByDescending { it.lastModified() } ?: emptyList()

private fun readDurationMs(file: File): Long? {
    val mmr = MediaMetadataRetriever()
    return try {
        mmr.setDataSource(file.absolutePath)
        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
    } catch (_: Exception) { null } finally {
        try { mmr.release() } catch (_: Exception) {}
    }
}

private fun formatDurationMs(ms: Long?): String {
    ms ?: return "—"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/* ---------------------------------------------------------- */
/* ----------------------  Record actions  ------------------- */
/* ---------------------------------------------------------- */

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
        onError("Error starting recording: ${t.message ?: "unknown"}")
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
        onError("Error stopping recording: ${t.message ?: "unknown"}")
    }
}