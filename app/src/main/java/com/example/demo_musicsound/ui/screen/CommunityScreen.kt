package com.example.demo_musicsound.ui.screen

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.demo_musicsound.Audio.BeatPlayer
import com.example.demo_musicsound.community.CommunityBeat
import com.example.demo_musicsound.community.CommunityViewModel
import com.example.demo_musicsound.community.ReferenceTrack
import com.example.mybeat.ui.theme.GrayBg
import com.example.mybeat.ui.theme.GraySurface
import com.example.mybeat.ui.theme.PurpleAccent
import com.example.mybeat.ui.theme.PurpleBar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    vm: CommunityViewModel,
    onGoToLogin: () -> Unit = {},
    onGoToRegister: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()

    // --- Auth (UI side) ---
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            isLoggedIn = fbAuth.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // --- Local beat player ---
    val player = remember { BeatPlayer() }
    var playingFile by remember { mutableStateOf<File?>(null) }

    // --- Upload dialog ---
    var showUploadDialog by remember { mutableStateOf(false) }

    // --- Details dialog (long press) ---
    var detailsBeat by remember { mutableStateOf<CommunityBeat?>(null) }
    var detailsCoverUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    // Load only when logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) vm.load()
    }

    Scaffold(
        containerColor = GrayBg,
        topBar = { /* EMPTY: evita doppia barra e glitch */ }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Banner login
            if (!isLoggedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GraySurface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Login required",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Sign in to view and publish beats.",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        FilledTonalButton(
                            onClick = { onGoToLogin() },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = PurpleAccent)
                        ) { Text("Login") }
                    }
                }
            }

            // Dialog upload
            if (showUploadDialog) {
                UploadBeatDialog(
                    vm = vm,
                    onDismiss = { showUploadDialog = false }
                )
            }

            // Dialog dettagli (long press)
            detailsBeat?.let { beat ->
                BeatDetailsDialog(
                    beat = beat,
                    coverUrl = detailsCoverUrl,
                    onDismiss = {
                        detailsBeat = null
                        detailsCoverUrl = null
                    }
                )
            }

            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            ui.message?.let { msg ->
                AssistChip(onClick = {}, label = { Text(msg) })
            }

            // -------------------------
            // Published projects header
            // -------------------------
            SectionTitle(
                title = "Published projects",
                trailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { if (isLoggedIn) vm.load() else onGoToLogin() }) {
                            Text("Refresh")
                        }
                        FilledTonalButton(
                            onClick = {
                                if (isLoggedIn) showUploadDialog = true else onGoToLogin()
                            },
                            enabled = isLoggedIn
                        ) { Text("Upload") }
                    }
                }
            )

            // ✅ Sezione 1: scroll indipendente (weight)
            SectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // <- prende una quota dello schermo
            ) {
                when {
                    !isLoggedIn -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState("Login to see your published projects.")
                        }
                    }

                    ui.myBeats.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState("No published projects yet.")
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            items(ui.myBeats, key = { it.id }) { beat ->
                                val coverUrl = ui.coverUrls[beat.id]
                                val localFile = findLocalExport(ctx, beat)

                                val isPlaying =
                                    localFile != null &&
                                            playingFile?.absolutePath == localFile.absolutePath &&
                                            player.isPlaying()

                                BeatRow(
                                    coverUrl = coverUrl,
                                    title = beat.title,
                                    subtitle = "by you",
                                    referenceLine = null, // ✅ niente inspired by in lista
                                    onLongPress = {
                                        detailsBeat = beat
                                        detailsCoverUrl = coverUrl
                                    },
                                    primaryButton = {
                                        IconButton(
                                            enabled = localFile != null,
                                            onClick = {
                                                if (!isLoggedIn) {
                                                    onGoToLogin(); return@IconButton
                                                }
                                                if (localFile == null) return@IconButton

                                                if (isPlaying) {
                                                    player.stop()
                                                    playingFile = null
                                                } else {
                                                    player.play(
                                                        localFile,
                                                        loop = false,
                                                        volume = 1f
                                                    )
                                                    playingFile = localFile
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = if (localFile != null) Color.White else Color.White.copy(
                                                    alpha = 0.35f
                                                )
                                            )
                                        }
                                    },
                                    secondaryButton = {
                                        if (localFile == null) {
                                            Text(
                                                "Not downloaded",
                                                color = Color.White.copy(alpha = 0.6f),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------
            // Get from community header
            // -------------------------
            SectionTitle(title = "Get from community")

            // ✅ Sezione 2: scroll indipendente (weight)
            SectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // <- seconda quota dello schermo
            ) {
                when {
                    !isLoggedIn -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState("Login to browse and download community beats.")
                        }
                    }

                    ui.communityBeats.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState("No community beats available.")
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            items(ui.communityBeats, key = { it.id }) { beat ->
                                val coverUrl = ui.coverUrls[beat.id]

                                BeatRow(
                                    coverUrl = coverUrl,
                                    title = beat.title,
                                    subtitle = "by ${beat.ownerId}",
                                    referenceLine = null, // ✅ niente inspired by in lista
                                    onLongPress = {
                                        detailsBeat = beat
                                        detailsCoverUrl = coverUrl
                                    },
                                    primaryButton = {
                                        FilledTonalIconButton(
                                            onClick = {
                                                if (!isLoggedIn) {
                                                    onGoToLogin(); return@FilledTonalIconButton
                                                }
                                                vm.download(ctx, beat)
                                            },
                                            enabled = isLoggedIn,
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = PurpleAccent,
                                                contentColor = Color.Black
                                            )
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    /* ---------------- UI helpers ---------------- */

@Composable
private fun SectionTitle(
    title: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        trailing?.invoke()
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        )
    ) { content() }
}


@Composable
private fun EmptyState(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.8f),
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun BeatRow(
    coverUrl: String?,
    title: String,
    subtitle: String,
    referenceLine: String? = null, // kept optional
    onLongPress: (() -> Unit)? = null,
    primaryButton: @Composable () -> Unit,
    secondaryButton: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* no-op */ },
                onLongClick = { onLongPress?.invoke() }
            ),
        colors = CardDefaults.cardColors(containerColor = GraySurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.AudioFile, contentDescription = null, tint = Color.White)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // not used anymore in list
                referenceLine?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            secondaryButton?.invoke()
            primaryButton()
        }
    }
}

/* ---------------- Local file helper ---------------- */

private fun findLocalExport(context: Context, beat: CommunityBeat): File? {
    val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
    val slug = beat.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    return dir.listFiles()
        ?.filter { it.isFile && it.nameWithoutExtension.startsWith(slug) }
        ?.maxByOrNull { it.lastModified() }
}

/* ---------------- Upload dialog ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
private enum class UploadStep { PICK_LOCAL_BEAT, EDIT_DETAILS }

@Composable
private fun UploadBeatDialog(
    vm: CommunityViewModel,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current

    var step by remember { mutableStateOf(UploadStep.PICK_LOCAL_BEAT) }

    val exportsDir = remember {
        File(ctx.getExternalFilesDir(null), "exports").apply { mkdirs() }
    }
    val localBeats = remember { loadLocalBeats(exportsDir) }

    var selectedBeat by remember { mutableStateOf<File?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var coverUri by remember { mutableStateOf<Uri?>(null) }
    val pickCover = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> coverUri = uri }

    // iTunes reference
    var refQuery by remember { mutableStateOf("") }
    var refResults by remember { mutableStateOf<List<ReferenceTrack>>(emptyList()) }
    var selectedRef by remember { mutableStateOf<ReferenceTrack?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (step) {
                            UploadStep.PICK_LOCAL_BEAT -> "Select a beat to publish"
                            UploadStep.EDIT_DETAILS -> "Publish details"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                Divider()

                when (step) {
                    UploadStep.PICK_LOCAL_BEAT -> {
                        if (localBeats.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No local beats found in exports.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(localBeats, key = { it.absolutePath }) { file ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedBeat = file
                                                title = file.nameWithoutExtension
                                                description = ""
                                                coverUri = null
                                                selectedRef = null
                                                refResults = emptyList()
                                                refQuery = ""
                                                step = UploadStep.EDIT_DETAILS
                                            },
                                        shape = RoundedCornerShape(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Default.AudioFile, contentDescription = null)
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    file.nameWithoutExtension,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${(file.length() / 1024)} KB",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text("Select")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    UploadStep.EDIT_DETAILS -> {
                        val beat = selectedBeat
                        if (beat == null) {
                            step = UploadStep.PICK_LOCAL_BEAT
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 18.dp)
                            ) {

                                item {
                                    TextButton(onClick = {
                                        step = UploadStep.PICK_LOCAL_BEAT
                                        selectedBeat = null
                                    }) { Text("Back") }
                                }

                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (coverUri != null) {
                                                AsyncImage(
                                                    model = coverUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(Icons.Default.AudioFile, contentDescription = null)
                                            }
                                        }

                                        Column(Modifier.weight(1f)) {
                                            Text("Cover", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Optional (recommended)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Button(onClick = { pickCover.launch("image/*") }) {
                                            Text("Pick")
                                        }
                                    }
                                }

                                item {
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item { Text("Reference track (iTunes)", fontWeight = FontWeight.SemiBold) }

                                item {
                                    selectedRef?.let { sel ->
                                        AssistChip(
                                            onClick = { /* no-op */ },
                                            label = { Text("Selected: ${sel.trackName} — ${sel.artistName}") },
                                            trailingIcon = {
                                                TextButton(onClick = { selectedRef = null }) { Text("Change") }
                                            }
                                        )
                                    }
                                }

                                if (selectedRef == null) {
                                    item {
                                        OutlinedTextField(
                                            value = refQuery,
                                            onValueChange = { refQuery = it },
                                            label = { Text("Search a song") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = { vm.searchReferenceTracks(refQuery) { refResults = it } },
                                                enabled = refQuery.isNotBlank()
                                            ) { Text("Search") }

                                            OutlinedButton(
                                                onClick = {
                                                    selectedRef = null
                                                    refResults = emptyList()
                                                    refQuery = ""
                                                }
                                            ) { Text("Clear") }
                                        }
                                    }

                                    if (refResults.isNotEmpty()) {
                                        items(refResults, key = { it.trackId }) { t ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedRef = t
                                                        refResults = emptyList()
                                                    },
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (t.artworkUrl.isNotBlank()) {
                                                        AsyncImage(
                                                            model = t.artworkUrl,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(44.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                        )
                                                        Spacer(Modifier.width(12.dp))
                                                    }
                                                    Column(Modifier.weight(1f)) {
                                                        Text(t.trackName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text(t.artistName, style = MaterialTheme.typography.labelMedium)
                                                        if (t.previewUrl.isNotBlank()) {
                                                            Text("Preview available", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        label = { Text("Description (optional)") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 110.dp)
                                    )
                                }

                                item {
                                    Spacer(Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            vm.publish(
                                                context = ctx,
                                                localBeatFile = beat,
                                                title = title.trim().ifBlank { beat.nameWithoutExtension },
                                                description = description.trim(),
                                                coverUri = coverUri,
                                                reference = selectedRef,
                                                onDone = onDismiss
                                            )
                                        },
                                        enabled = title.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Publish") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadLocalBeats(exportsDir: File): List<File> {
    return exportsDir.listFiles { f ->
        f.isFile && f.extension.lowercase() in setOf("wav", "m4a", "mp3")
    }?.sortedByDescending { it.lastModified() } ?: emptyList()
}

/* ---------------- Details dialog (long press) ---------------- */

@Composable
private fun BeatDetailsDialog(
    beat: CommunityBeat,
    coverUrl: String?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var playingPreview by remember { mutableStateOf(false) }
    var mp by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try { mp?.stop() } catch (_: Throwable) {}
            try { mp?.release() } catch (_: Throwable) {}
            mp = null
        }
    }

    fun playPreview(seconds: Long = 6L) {
        val url = beat.refPreviewUrl
        if (url.isBlank()) return

        try { mp?.stop() } catch (_: Throwable) {}
        try { mp?.release() } catch (_: Throwable) {}
        mp = null

        playingPreview = true
        val player = MediaPlayer()
        mp = player

        try {
            player.setDataSource(url)
            player.setOnPreparedListener {
                it.start()
                scope.launch {
                    delay(seconds * 1000)
                    try { it.stop() } catch (_: Throwable) {}
                    try { it.release() } catch (_: Throwable) {}
                    if (mp === it) mp = null
                    playingPreview = false
                }
            }
            player.setOnErrorListener { p, _, _ ->
                try { p.release() } catch (_: Throwable) {}
                if (mp === p) mp = null
                playingPreview = false
                true
            }
            player.prepareAsync()
        } catch (_: Throwable) {
            playingPreview = false
            try { player.release() } catch (_: Throwable) {}
            if (mp === player) mp = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // ✅ shorter dialog that grows only as needed (up to a max), then scrolls
                .heightIn(min = 260.dp, max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Beat details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }

                // ✅ Cover NOT clickable anymore
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.AudioFile, contentDescription = null)
                        }
                    }
                }

                item {
                    Text(beat.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Author: ${beat.ownerId}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    if (beat.description.isNotBlank()) {
                        Text(beat.description, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            "No description.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item { Divider() }

                // ✅ Preview button near reference section (not on the cover)
                item {
                    val hasRef = beat.refTrackName.isNotBlank() || beat.refArtistName.isNotBlank()
                    if (hasRef) {
                        Text("Reference track", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${beat.refTrackName} — ${beat.refArtistName}".trim(),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(6.dp))

                        val canPreview = beat.refPreviewUrl.isNotBlank()
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { playPreview(seconds = 6) },
                                enabled = canPreview && !playingPreview
                            ) {
                                Text(if (playingPreview) "Playing..." else "Listen preview")
                            }

                            if (!canPreview) {
                                Text(
                                    "No preview available.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            "No reference track selected.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
