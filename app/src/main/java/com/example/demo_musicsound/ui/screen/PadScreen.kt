package com.example.demo_musicsound.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.demo_musicsound.Audio.OfflineExporter
import com.example.demo_musicsound.Audio.Sequencer
import com.example.demo_musicsound.Audio.SoundManager
import com.example.demo_musicsound.R
import com.example.mybeat.ui.theme.GrayBg
import com.example.mybeat.ui.theme.GraySurface
import com.example.mybeat.ui.theme.OutlineDark
import com.example.mybeat.ui.theme.PurpleAccent
import com.example.mybeat.ui.theme.PurpleBar
import kotlinx.coroutines.launch
import java.io.File

// ------------------------------------------------------------
// MODEL
// ------------------------------------------------------------

private data class Pad(
    val slotId: String,
    val label: String,
    val soundKey: String
)

private val bankA = listOf(
    Pad("A0", "KICK", "kick"),
    Pad("A1", "SNARE", "snare"),
    Pad("A2", "HAT", "hat"),
    Pad("A3", "CLAP", "clap"),
    Pad("A4", "TOM1", "tom1"),
    Pad("A5", "TOM2", "tom2"),
)

private val bankB = listOf(
    Pad("B0", "RIM", "rim"),
    Pad("B1", "SHAK", "shaker"),
    Pad("B2", "OHAT", "ohat"),
    Pad("B3", "RIDE", "ride"),
    Pad("B4", "FX1", "fx1"),
    Pad("B5", "FX2", "fx2"),
)

private val allKeys = (bankA + bankB).map { it.soundKey }

// ------------------------------------------------------------
// SCREEN
// ------------------------------------------------------------

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PadScreen(
    sound: SoundManager,
    seq: Sequencer,
    padLabels: Map<String, String> = emptyMap(),
    onPadSoundPicked: (slotId: String, pickedUri: Uri, customLabel: String) -> Unit = { _, _, _ -> },
    onBeatExported: (file: File, bpm: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val running by seq.running.collectAsState()

    val defaultPadLabel = stringResource(id = R.string.pad_default_label)

    var bpm by remember { mutableStateOf(120) }
    var tab by remember { mutableStateOf(0) }
    var curStep by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { seq.ensureAll(allKeys) }

    val page = if (tab == 0) bankA else bankB
    val pageKeys = page.map { it.soundKey }

    // ---- export dialog ----
    var showNameDialog by remember { mutableStateOf(false) }
    var beatName by remember { mutableStateOf(defaultBeatName()) }
    var exporting by remember { mutableStateOf(false) }

    // ---- pad settings dialog (NEW FLOW) ----
    var showPadEditDialog by remember { mutableStateOf(false) }
    var editingSlotId by remember { mutableStateOf<String?>(null) }
    var padLabelInput by remember { mutableStateOf("") }

    // chosen audio (kept inside dialog until Save)
    var pickedPadUri by remember { mutableStateOf<Uri?>(null) }
    var pickedPadFileName by remember { mutableStateOf<String?>(null) }

    val uiLabels = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(padLabels) {
        uiLabels.clear()
        uiLabels.putAll(padLabels)
    }

    fun resetPadDialogState() {
        editingSlotId = null
        padLabelInput = ""
        pickedPadUri = null
        pickedPadFileName = null
    }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        pickedPadUri = uri
        pickedPadFileName = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
            }
        }.getOrNull()
    }

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

            // TOP CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (running) seq.stop()
                            else seq.start(scope, sound) { step -> curStep = step }
                        },
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("btn_play_stop"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (running) "Stop" else "Play") }

                    FilledIconButton(
                        onClick = { seq.clear(pageKeys) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_clear"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PurpleAccent,
                            contentColor = Color.White
                        )
                    ) { Icon(Icons.Filled.Delete, contentDescription = "Clear Page") }

                    FilledIconButton(
                        onClick = {
                            beatName = defaultBeatName()
                            showNameDialog = true
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_export"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PurpleAccent,
                            contentColor = Color.White
                        )
                    ) { Icon(Icons.Filled.Download, contentDescription = "Export") }
                }
            }

            BankSwitch(
                tab = tab,
                onTab = { tab = it },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // PAD GRID
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
                        val shownLabel = uiLabels[p.slotId] ?: p.label

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .combinedClickable(
                                    onClick = { sound.play(p.soundKey) },
                                    onLongClick = {
                                        editingSlotId = p.slotId
                                        padLabelInput = shownLabel
                                        pickedPadUri = null
                                        pickedPadFileName = null
                                        showPadEditDialog = true
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = GraySurface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(shownLabel, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }

            SequencerGrid(
                seq = seq,
                tracks = page,
                currentStep = curStep,
                labels = uiLabels
            )

            Text(
                text = "Tip: long-press a pad to rename it and change its sound",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    // -----------------------------
// SAVE BEAT DIALOG (styled)
// -----------------------------
    if (showNameDialog) {
        val dialogBg = Color(0xFF2C2642)
        val cardBg = Color(0xFF3A3354)
        val border = Color.White.copy(alpha = 0.12f)

        AlertDialog(
            onDismissRequest = { if (!exporting) showNameDialog = false },
            containerColor = dialogBg,
            shape = RoundedCornerShape(24.dp),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Text(
                    text = "Save beat",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, border)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            OutlinedTextField(
                                value = beatName,
                                onValueChange = { if (it.length <= 40) beatName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("field_beat_name"),
                                label = { Text("File name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PurpleAccent,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                    focusedLabelColor = PurpleAccent,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                    cursorColor = PurpleAccent
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !exporting,
                    modifier = Modifier.testTag("btn_save_export"),
                    onClick = {
                        exporting = true
                        scope.launch {
                            try {
                                val keys = allKeys.filter { resIdOf(context, it) != 0 }
                                if (keys.isEmpty()) {
                                    snackbar.showSnackbar("Export failed: no raw samples found.")
                                    return@launch
                                }

                                val steps = seq.pattern(keys.first()).size
                                val tracks = keys.map { key ->
                                    OfflineExporter.TrackMix(
                                        resName = key,
                                        pattern = seq.pattern(key).toList(),
                                        sample = OfflineExporter.loadWavPCM16(context, resIdOf(context, key))
                                    )
                                }

                                val out = OfflineExporter.exportBeatToWav(
                                    context = context,
                                    bpm = bpm,
                                    steps = steps,
                                    dstSr = 44100,
                                    tracks = tracks
                                )

                                val finalFile = run {
                                    val target = File(out.parentFile, beatName.slugOrDefault() + ".wav")
                                    if (out.name != target.name) {
                                        val safe = uniqueTarget(target)
                                        out.renameTo(safe)
                                        safe
                                    } else out
                                }

                                onBeatExported(finalFile, bpm)
                                snackbar.showSnackbar("Exported: ${finalFile.name}")
                            } catch (t: Throwable) {
                                snackbar.showSnackbar("Export failed: ${t.message}")
                            } finally {
                                exporting = false
                                showNameDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = PurpleAccent.copy(alpha = 0.35f),
                        disabledContentColor = Color.Black.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (exporting) "Saving…" else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !exporting,
                    modifier = Modifier.testTag("btn_cancel_export"),
                    onClick = { showNameDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.85f)
                    )
                ) { Text("Cancel") }
            }
        )
    }


// -----------------------------
// PAD SETTINGS DIALOG (styled)
// -----------------------------
    if (showPadEditDialog) {
        val dialogBg = Color(0xFF2C2642)
        val cardBg = Color(0xFF3A3354)
        val border = Color.White.copy(alpha = 0.12f)

        AlertDialog(
            onDismissRequest = {
                showPadEditDialog = false
                resetPadDialogState()
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(24.dp),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Text(
                    text = "Pad settings",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Card wrapper (coerente con lo stile Account)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = padLabelInput,
                                onValueChange = { if (it.length <= 12) padLabelInput = it },
                                label = { Text("Pad name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pad_dialog_label"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PurpleAccent,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                    focusedLabelColor = PurpleAccent,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                    cursorColor = PurpleAccent
                                )
                            )

                            // Audio picker card (stesso “card look”)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pad_dialog_audio_card"),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Audio file",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = pickedPadFileName
                                                ?: (pickedPadUri?.toString() ?: "No file selected"),
                                            color = Color.White.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Button(
                                        onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) },
                                        modifier = Modifier.testTag("pad_dialog_pick"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PurpleAccent,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(if (pickedPadUri == null) "Pick" else "Change")
                                    }
                                }
                            }

                            Text(
                                text = "Pick an audio file, then press Save.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = pickedPadUri != null,
                    modifier = Modifier.testTag("pad_dialog_save"),
                    onClick = {
                        val slotId = editingSlotId ?: return@Button
                        val uri = pickedPadUri ?: return@Button
                        val label = padLabelInput.trim().ifEmpty { defaultPadLabel }

                        uiLabels[slotId] = label
                        onPadSoundPicked(slotId, uri, label)

                        scope.launch { snackbar.showSnackbar("Updated pad $slotId") }

                        showPadEditDialog = false
                        resetPadDialogState()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = PurpleAccent.copy(alpha = 0.35f),
                        disabledContentColor = Color.Black.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag("pad_dialog_cancel"),
                    onClick = {
                        showPadEditDialog = false
                        resetPadDialogState()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.85f)
                    )
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
                modifier = Modifier
                    .height(36.dp)
                    .testTag(
                        when (text) {
                            "Bank A" -> "bank_A"
                            "Bank B" -> "bank_B"
                            else -> "bank_$text"
                        }
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected) PurpleAccent else Color.Transparent,
                    contentColor = if (selected) Color.Black else Color.White
                )
            ) { Text(text, style = MaterialTheme.typography.labelLarge) }
        }
        Seg("Bank A", tab == 0) { onTab(0) }
        Seg("Bank B", tab == 1) { onTab(1) }
    }
}

@Composable
private fun SequencerGrid(
    seq: Sequencer,
    tracks: List<Pad>,
    currentStep: Int,
    labels: Map<String, String>
) {
    val activeFill = Color(0xFF3DDC84)
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
            val pattern = seq.pattern(pad.soundKey)
            val shownLabel = labels[pad.slotId] ?: pad.label

            Column(Modifier.fillMaxWidth()) {
                Text(
                    shownLabel,
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
                                .testTag("seq_${pad.soundKey}_$i")
                                .background(
                                    when {
                                        active -> activeFill.copy(alpha = 0.18f)
                                        isNow -> nowBg
                                        else -> Color.Transparent
                                    },
                                    shape = corner
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        active -> activeBorder
                                        isNow -> nowBorder
                                        else -> idleBorder
                                    },
                                    shape = corner
                                )
                                .combinedClickable(
                                    onClick = { seq.toggle(pad.soundKey, i) },
                                    onLongClick = { }
                                )
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