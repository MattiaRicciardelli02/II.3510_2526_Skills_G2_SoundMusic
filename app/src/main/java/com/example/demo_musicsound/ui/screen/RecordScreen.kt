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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.demo_musicsound.Audio.BeatPlayer
import com.example.demo_musicsound.Audio.RecorderManager
import com.example.demo_musicsound.R
import com.example.demo_musicsound.community.FirebaseCommunityRepository
import com.example.demo_musicsound.data.LocalBeatDao
import com.example.demo_musicsound.data.LocalBeatEntity
import java.io.File

@Composable
fun RecordScreen(
    rec: RecorderManager,
    beatDao: LocalBeatDao,
    repo: FirebaseCommunityRepository,
    ownerId: String, // uid or "guest"
    defaultPlayBeatDuringRec: Boolean = true
) {
    val context = LocalContext.current

    // Strings
    val sExportedBeats = stringResource(R.string.record_exported_beats)
    val sNoBeatsYet = stringResource(R.string.record_no_beats_yet)
    val sRecordingControls = stringResource(R.string.record_recording_controls)
    val sPlaySelectedBeatWhileRecording = stringResource(R.string.record_play_selected_beat_while_recording)
    val sVol = stringResource(R.string.record_volume_short)
    val sStop = stringResource(R.string.common_stop)
    val sRec = stringResource(R.string.record_rec)
    val sSelected = stringResource(R.string.common_selected)
    val sSelect = stringResource(R.string.common_select)
    val sMicDenied = stringResource(R.string.record_mic_permission_denied)
    val sLibrarySyncFailed = stringResource(R.string.record_library_sync_failed)

    // ------------------------------------------------------------
    // STATE - EXPORTED BEATS (Room)
    // ------------------------------------------------------------

    val beatRows by beatDao.observeByOwner(ownerId).collectAsState(initial = emptyList())
    var selectedBeatId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(beatRows) {
        if (selectedBeatId !in beatRows.map { it.id }) {
            selectedBeatId = beatRows.firstOrNull()?.id
        }
    }

    // ------------------------------------------------------------
    // SYNC Firebase → Room (only if logged)
    // ------------------------------------------------------------

    LaunchedEffect(ownerId) {
        if (ownerId == "guest") return@LaunchedEffect
        try {
            val existing = beatDao.getByOwner(ownerId).associateBy { it.id }
            val library = repo.getMyLibrary(ownerId)

            for (b in library) {
                val existingRow = existing[b.id]
                val localFile = existingRow?.filePath
                    ?.let { File(it) }
                    ?.takeIf { it.exists() && it.isFile }
                    ?: repo.downloadLibraryBeatToLocalExports(context, b)

                beatDao.upsert(
                    LocalBeatEntity(
                        id = b.id,
                        ownerId = ownerId,
                        title = b.title.ifBlank { localFile.nameWithoutExtension },
                        filePath = localFile.absolutePath,
                        createdAt = if (b.createdAt > 0L) b.createdAt else System.currentTimeMillis()
                    )
                )
            }
        } catch (_: Throwable) {
            Toast.makeText(context, sLibrarySyncFailed, Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------------
    // STATE - RECORDINGS (filesystem)
    // ------------------------------------------------------------

    var recordings by remember { mutableStateOf<List<File>>(emptyList()) }
    fun refreshRecordings() { recordings = rec.listRecordings() }

    LaunchedEffect(Unit) { refreshRecordings() }

    var recording by remember { mutableStateOf(false) }
    var pendingRecordingFile by remember { mutableStateOf<File?>(null) }

    // Save dialog
    var showSaveDialog by remember { mutableStateOf(false) }
    var recordingName by remember { mutableStateOf("") }

    // ------------------------------------------------------------
    // AUDIO (preview)
    // ------------------------------------------------------------

    val beatPlayer = remember { BeatPlayer() }
    var playBeatDuringRec by remember { mutableStateOf(defaultPlayBeatDuringRec) }
    var beatVolume by remember { mutableFloatStateOf(0.9f) }

    val previewPlayer = remember { BeatPlayer() }
    var previewingBeatId by remember { mutableStateOf<String?>(null) }

    val recPreviewPlayer = remember { BeatPlayer() }
    var previewingRecPath by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            beatPlayer.stop()
            previewPlayer.stop()
            recPreviewPlayer.stop()
        }
    }

    // ------------------------------------------------------------
    // MIC PERMISSION
    // ------------------------------------------------------------

    val micPermission = Manifest.permission.RECORD_AUDIO
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, micPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val requestMicPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            micGranted = granted
            if (!granted) Toast.makeText(context, sMicDenied, Toast.LENGTH_SHORT).show()
        }

    // ------------------------------------------------------------
    // UI
    // ------------------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // -----------------------
        // EXPORTED BEATS
        // -----------------------
        Text(
            text = sExportedBeats,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
            )
        ) {
            if (beatRows.isEmpty()) {
                Text(sNoBeatsYet, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(beatRows, key = { it.id }) { beat ->
                        BeatRow(
                            title = beat.title,
                            subtitle = formatDurationMs(readDurationMs(File(beat.filePath))),
                            isSelected = beat.id == selectedBeatId,
                            isPreviewing = beat.id == previewingBeatId,
                            selectedText = sSelected,
                            selectText = sSelect,
                            onSelect = { selectedBeatId = beat.id },
                            onPlay = {
                                val file = File(beat.filePath)
                                if (!file.exists()) return@BeatRow

                                if (previewingBeatId == beat.id) {
                                    previewPlayer.stop()
                                    previewingBeatId = null
                                } else {
                                    previewPlayer.stop()
                                    previewPlayer.play(file, false, 1f)
                                    previewingBeatId = beat.id
                                }
                            }
                        )
                    }
                }
            }
        }

        // -----------------------
        // RECORDING CONTROLS
        // -----------------------
        Text(
            text = sRecordingControls,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = playBeatDuringRec,
                        onCheckedChange = { playBeatDuringRec = it }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(sPlaySelectedBeatWhileRecording)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sVol)
                    Spacer(Modifier.width(12.dp))
                    Slider(
                        modifier = Modifier.weight(1f),
                        value = beatVolume,
                        onValueChange = {
                            beatVolume = it
                            beatPlayer.setVolume(it)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${(beatVolume * 100).toInt()}%")
                }

                Divider()

                Button(
                    onClick = {
                        if (!micGranted) {
                            requestMicPermission.launch(micPermission)
                            return@Button
                        }

                        if (!recording) {
                            // start
                            val beat = beatRows.firstOrNull { it.id == selectedBeatId }
                            if (playBeatDuringRec && beat != null) {
                                val f = File(beat.filePath)
                                if (f.exists()) beatPlayer.play(f, true, beatVolume)
                            }
                            val out = rec.start()
                            pendingRecordingFile = out
                            recording = true
                        } else {
                            // stop -> ask name + save
                            rec.stop()
                            beatPlayer.stop()
                            recording = false

                            val f = pendingRecordingFile
                            if (f != null && f.exists()) {
                                recordingName = f.nameWithoutExtension // default suggestion
                                showSaveDialog = true
                            } else {
                                refreshRecordings()
                            }
                        }
                    }
                ) {
                    Text(if (recording) sStop else sRec)
                }
            }
        }

        // -----------------------
        // RECORDINGS LIST (NEW)
        // -----------------------
        Text(
            text = "Recordings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
            )
        ) {
            if (recordings.isEmpty()) {
                Text("No recordings yet.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recordings, key = { it.absolutePath }) { file ->
                        RecordingRow(
                            title = file.nameWithoutExtension,
                            subtitle = formatDurationMs(readDurationMs(file)),
                            isPreviewing = previewingRecPath == file.absolutePath,
                            onPlay = {
                                if (previewingRecPath == file.absolutePath) {
                                    recPreviewPlayer.stop()
                                    previewingRecPath = null
                                } else {
                                    recPreviewPlayer.stop()
                                    recPreviewPlayer.play(file, false, 1f)
                                    previewingRecPath = file.absolutePath
                                }
                            },
                            onDelete = {
                                // optional delete
                                try {
                                    if (previewingRecPath == file.absolutePath) {
                                        recPreviewPlayer.stop()
                                        previewingRecPath = null
                                    }
                                    rec.delete(file)
                                } catch (_: Throwable) {}
                                refreshRecordings()
                            }
                        )
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------
    // SAVE DIALOG (after stop)
    // ------------------------------------------------------------
    if (showSaveDialog) {
        // --- explicit colors (same style as your Account popup) ---
        val dialogBg = Color(0xFF2B2542)                 // viola scuro del popup
        val cardBg = Color.White.copy(alpha = 0.06f)     // box interno soft
        val textPrimary = Color.White
        val textSecondary = Color.White.copy(alpha = 0.65f)
        val outline = Color.White.copy(alpha = 0.25f)
        val accent = Color(0xFF7C4DFF)                   // purple accent
        val accentDisabled = accent.copy(alpha = 0.35f)
        val accentText = Color.Black

        AlertDialog(
            onDismissRequest = {
                // if user cancels: keep file as-is and still show it
                showSaveDialog = false
                refreshRecordings()
                pendingRecordingFile = null
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(24.dp),
            titleContentColor = textPrimary,
            textContentColor = textPrimary,
            title = {
                Text(
                    text = "Save recording",
                    color = textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Field
                    OutlinedTextField(
                        value = recordingName,
                        onValueChange = { if (it.length <= 40) recordingName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = outline,
                            focusedLabelColor = accent,
                            unfocusedLabelColor = textSecondary,
                            cursorColor = accent
                        )
                    )

                    // Helper card (optional but matches your style)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Choose a name for this recording.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val original = pendingRecordingFile
                        if (original != null && original.exists()) {
                            val safeBase = recordingName.trim()
                                .ifBlank { original.nameWithoutExtension }
                                .lowercase()
                                .replace(Regex("[^a-z0-9]+"), "_")
                                .trim('_')
                                .ifBlank { original.nameWithoutExtension }

                            val ext = original.extension.ifBlank { "m4a" }
                            val target = File(original.parentFile, "$safeBase.$ext")
                            val finalTarget = uniqueTarget(target)

                            if (original.absolutePath != finalTarget.absolutePath) {
                                try { original.renameTo(finalTarget) } catch (_: Throwable) {}
                            }
                        }

                        showSaveDialog = false
                        pendingRecordingFile = null
                        refreshRecordings()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = accentText,
                        disabledContainerColor = accentDisabled,
                        disabledContentColor = accentText.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // cancel: don't rename, but keep
                        showSaveDialog = false
                        pendingRecordingFile = null
                        refreshRecordings()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = textPrimary.copy(alpha = 0.85f)
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ------------------------------------------------------------
// ROWS
// ------------------------------------------------------------

@Composable
private fun BeatRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isPreviewing: Boolean,
    selectedText: String,
    selectText: String,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF23232B))
    ) {
        Row(
            modifier = Modifier
                .height(72.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium)
            }

            FilledTonalButton(onClick = onSelect) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSelected) selectedText else selectText)
            }
        }
    }
}

@Composable
private fun RecordingRow(
    title: String,
    subtitle: String,
    isPreviewing: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF23232B))
    ) {
        Row(
            modifier = Modifier
                .height(72.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

// ------------------------------------------------------------
// UTILS
// ------------------------------------------------------------

private fun readDurationMs(file: File): Long? {
    if (!file.exists()) return null
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

fun formatDurationMs(ms: Long?): String {
    ms ?: return "—"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}

private fun uniqueTarget(target: File): File {
    if (!target.exists()) return target
    val base = target.nameWithoutExtension
    val ext = target.extension
    var i = 2
    while (true) {
        val candidate = File(target.parentFile, "${base}_$i.$ext")
        if (!candidate.exists()) return candidate
        i++
    }
}