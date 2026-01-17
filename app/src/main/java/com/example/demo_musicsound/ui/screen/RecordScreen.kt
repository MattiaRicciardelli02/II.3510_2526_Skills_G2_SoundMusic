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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

    // ✅ Strings (safe composable context)
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
    // STATE
    // ------------------------------------------------------------

    var recording by remember { mutableStateOf(false) }
    var lastPath by remember { mutableStateOf<String?>(null) }

    val beatRows by beatDao
        .observeByOwner(ownerId)
        .collectAsState(initial = emptyList())

    var selectedBeatId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(beatRows) {
        if (selectedBeatId !in beatRows.map { it.id }) {
            selectedBeatId = beatRows.firstOrNull()?.id
        }
    }

    // ------------------------------------------------------------
    // Sync Firebase → Room (solo se loggato)
    // ------------------------------------------------------------

    LaunchedEffect(ownerId) {
        if (ownerId == "guest") return@LaunchedEffect

        try {
            // ⚠️ non usare stringResource qui: siamo fuori contesto UI safe (comunque qui non serve)
            val existing = beatRows.associateBy { it.id }
            val library = repo.getMyLibrary(ownerId)

            for (b in library) {
                val existingRow = existing[b.id]
                val localFile = existingRow
                    ?.filePath
                    ?.let { File(it) }
                    ?.takeIf { it.exists() }
                    ?: repo.downloadLibraryBeatToLocalExports(context, b)

                beatDao.upsert(
                    LocalBeatEntity(
                        id = b.id,
                        ownerId = ownerId,
                        title = b.title,
                        filePath = localFile.absolutePath,
                        createdAt = b.createdAt
                    )
                )
            }
        } catch (_: Throwable) {
            Toast.makeText(context, sLibrarySyncFailed, Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------------
    // AUDIO
    // ------------------------------------------------------------

    val beatPlayer = remember { BeatPlayer() }
    var playBeatDuringRec by remember { mutableStateOf(defaultPlayBeatDuringRec) }
    var beatVolume by remember { mutableFloatStateOf(0.9f) }

    val previewPlayer = remember { BeatPlayer() }
    var previewingBeatId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            beatPlayer.stop()
            previewPlayer.stop()
        }
    }

    // ------------------------------------------------------------
    // MIC PERMISSION
    // ------------------------------------------------------------

    val micPermission = Manifest.permission.RECORD_AUDIO
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                micPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestMicPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            micGranted = granted
            if (!granted) {
                Toast.makeText(context, sMicDenied, Toast.LENGTH_SHORT).show()
            }
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
                Text(
                    sNoBeatsYet,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(beatRows, key = { it.id }) { beat ->
                        BeatRow(
                            beat = beat,
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
        // RECORD CONTROLS
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
                            val beat = beatRows.firstOrNull { it.id == selectedBeatId }
                            if (playBeatDuringRec && beat != null) {
                                val f = File(beat.filePath)
                                if (f.exists()) {
                                    beatPlayer.play(f, true, beatVolume)
                                }
                            }
                            val out = rec.start()
                            lastPath = out.absolutePath
                            recording = true
                        } else {
                            rec.stop()
                            beatPlayer.stop()
                            recording = false
                        }
                    }
                ) {
                    Text(if (recording) sStop else sRec)
                }
            }
        }
    }
}

// ------------------------------------------------------------
// BEAT ROW
// ------------------------------------------------------------

@Composable
private fun BeatRow(
    beat: LocalBeatEntity,
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = beat.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDurationMs(readDurationMs(File(beat.filePath))),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            FilledTonalButton(onClick = onSelect) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSelected) selectedText else selectText)
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
        mmr.release()
    }
}

private fun formatDurationMs(ms: Long?): String {
    ms ?: return "—"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}
