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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.demo_musicsound.Audio.BeatPlayer
import com.example.demo_musicsound.R
import com.example.demo_musicsound.community.CommunityBeat
import com.example.demo_musicsound.community.CommunityViewModel
import com.example.demo_musicsound.community.ReferenceTrack
import com.example.mybeat.ui.theme.GrayBg
import com.example.mybeat.ui.theme.GraySurface
import com.example.mybeat.ui.theme.PurpleAccent
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

    // ✅ Load community + profile when login state becomes true
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            vm.load()
            vm.loadUserProfile()
        }
    }

    // --- Local beat player ---
    val player = remember { BeatPlayer() }
    var playingFile by remember { mutableStateOf<File?>(null) }

    // --- Upload dialog ---
    var showUploadDialog by remember { mutableStateOf(false) }

    // --- Details dialog (long press) ---
    var detailsBeat by remember { mutableStateOf<CommunityBeat?>(null) }
    var detailsCoverUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { player.stop() } }

    Scaffold(
        containerColor = GrayBg,
        topBar = { /* EMPTY: parent screen already shows "MyBeat" */ }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Login banner
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
                                stringResource(R.string.community_login_required_title),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.community_login_required_body),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        FilledTonalButton(
                            onClick = { onGoToLogin() },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = PurpleAccent)
                        ) {
                            Text(stringResource(R.string.action_login))
                        }
                    }
                }
            }

            // Dialogs
            if (showUploadDialog) {
                UploadBeatDialog(vm = vm, onDismiss = { showUploadDialog = false })
            }

            detailsBeat?.let { beat ->
                BeatDetailsDialog(
                    beat = beat,
                    coverUrl = detailsCoverUrl,
                    onDismiss = { detailsBeat = null; detailsCoverUrl = null }
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
                title = stringResource(R.string.community_published_projects),
                trailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { if (isLoggedIn) vm.load() else onGoToLogin() }) {
                            Text(stringResource(R.string.action_refresh))
                        }
                        FilledTonalButton(
                            onClick = { if (isLoggedIn) showUploadDialog = true else onGoToLogin() },
                            enabled = isLoggedIn
                        ) { Text(stringResource(R.string.action_upload)) }
                    }
                }
            )

            // ✅ Section 1: independent scroll
            SectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    !isLoggedIn -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(stringResource(R.string.community_login_to_see_published))
                        }
                    }
                    ui.myBeats.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(stringResource(R.string.community_no_published_projects))
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
                                    subtitle = stringResource(R.string.community_by_you),
                                    referenceLine = null,
                                    onLongPress = {
                                        detailsBeat = beat
                                        detailsCoverUrl = coverUrl
                                    },
                                    primaryButton = {
                                        IconButton(
                                            enabled = localFile != null,
                                            onClick = {
                                                if (!isLoggedIn) { onGoToLogin(); return@IconButton }
                                                if (localFile == null) return@IconButton
                                                if (isPlaying) {
                                                    player.stop()
                                                    playingFile = null
                                                } else {
                                                    player.play(localFile, loop = false, volume = 1f)
                                                    playingFile = localFile
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = if (localFile != null) Color.White else Color.White.copy(alpha = 0.35f)
                                            )
                                        }
                                    },
                                    secondaryButton = {
                                        if (localFile == null) {
                                            Text(
                                                stringResource(R.string.community_not_downloaded),
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
            SectionTitle(title = stringResource(R.string.community_get_from_community))

            // ✅ Section 2: independent scroll
            SectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    !isLoggedIn -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(stringResource(R.string.community_login_to_browse))
                        }
                    }
                    ui.communityBeats.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(stringResource(R.string.community_no_community_beats))
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
                                    subtitle = stringResource(R.string.community_by_owner, beat.ownerId),
                                    referenceLine = null,
                                    onLongPress = {
                                        detailsBeat = beat
                                        detailsCoverUrl = coverUrl
                                    },
                                    primaryButton = {
                                        FilledTonalIconButton(
                                            onClick = {
                                                if (!isLoggedIn) { onGoToLogin(); return@FilledTonalIconButton }
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
    referenceLine: String? = null,
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

@OptIn(ExperimentalMaterial3Api::class)
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

    fun resetRef() {
        selectedRef = null
        refResults = emptyList()
        refQuery = ""
    }

    fun goToDetails(file: File) {
        selectedBeat = file
        title = file.nameWithoutExtension
        description = ""
        coverUri = null
        resetRef()
        step = UploadStep.EDIT_DETAILS
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f), // ✅ più corto (prova 0.74–0.82)
            shape = RoundedCornerShape(26.dp),
            tonalElevation = 8.dp,
            color = com.example.mybeat.ui.theme.GraySurface
        ) {
            Column(Modifier.fillMaxSize()) {

                // ---------------- HEADER (coerente) ----------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = when (step) {
                                UploadStep.PICK_LOCAL_BEAT -> stringResource(R.string.community_upload_select_title)
                                UploadStep.EDIT_DETAILS -> stringResource(R.string.community_upload_details_title)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (step) {
                                UploadStep.PICK_LOCAL_BEAT ->  stringResource(R.string.community_upload_local_beats)
                                UploadStep.EDIT_DETAILS -> stringResource(R.string.community_upload_cover_optional)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.action_close),
                            color = com.example.mybeat.ui.theme.PurpleAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.10f))

                // ---------------- BODY ----------------
                when (step) {
                    UploadStep.PICK_LOCAL_BEAT -> {
                        if (localBeats.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.community_upload_no_local_beats),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 14.dp)
                            ) {
                                items(localBeats, key = { it.absolutePath }) { file ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { goToDetails(file) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(alpha = 0.06f)
                                        )
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
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color.White.copy(alpha = 0.10f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AudioFile,
                                                    contentDescription = null,
                                                    tint = Color.White
                                                )
                                            }

                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = file.nameWithoutExtension,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.community_upload_file_kb,
                                                        (file.length() / 1024)
                                                    ),
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }

                                            Text(
                                                text = stringResource(R.string.action_select),
                                                color = com.example.mybeat.ui.theme.PurpleAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
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
                            return@Surface
                        }

                        // contenuto scrollabile + footer fisso sotto
                        Column(Modifier.fillMaxSize()) {

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 10.dp)
                            ) {

                                // ---- COVER CARD ----
                                item {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(alpha = 0.06f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color.White.copy(alpha = 0.10f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (coverUri != null) {
                                                    AsyncImage(
                                                        model = coverUri,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.AudioFile,
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                }
                                            }

                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.community_upload_cover),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = stringResource(R.string.community_upload_cover_optional),
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }

                                            Button(
                                                onClick = { pickCover.launch("image/*") },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = com.example.mybeat.ui.theme.PurpleAccent,
                                                    contentColor = Color.Black
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(stringResource(R.string.action_pick))
                                            }
                                        }
                                    }
                                }

                                // ---- TITLE ----
                                item {
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = {
                                            Text(
                                                stringResource(R.string.field_title),
                                                color = Color.White.copy(alpha = 0.75f)
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = com.example.mybeat.ui.theme.PurpleAccent,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                                            focusedLabelColor = com.example.mybeat.ui.theme.PurpleAccent,
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                                            cursorColor = com.example.mybeat.ui.theme.PurpleAccent
                                        )
                                    )
                                }

                                // ---- REFERENCE ----
                                item {
                                    Text(
                                        text = stringResource(R.string.community_upload_reference_title),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                item {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(alpha = 0.06f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {

                                            selectedRef?.let { sel ->
                                                Text(
                                                    text = "${sel.trackName} — ${sel.artistName}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { selectedRef = null },
                                                        shape = RoundedCornerShape(16.dp)
                                                    ) { Text(stringResource(R.string.action_change)) }

                                                    TextButton(
                                                        onClick = {resetRef()},
                                                    ) { Text(stringResource(R.string.action_clear)) }
                                                }
                                            }

                                            if (selectedRef == null) {
                                                OutlinedTextField(
                                                    value = refQuery,
                                                    onValueChange = { refQuery = it },
                                                    label = {
                                                        Text(
                                                            stringResource(R.string.community_upload_reference_search_label),
                                                            color = Color.White.copy(alpha = 0.75f)
                                                        )
                                                    },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedBorderColor = com.example.mybeat.ui.theme.PurpleAccent,
                                                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                                                        focusedLabelColor = com.example.mybeat.ui.theme.PurpleAccent,
                                                        unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                                                        cursorColor = com.example.mybeat.ui.theme.PurpleAccent
                                                    )
                                                )

                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Button(
                                                        onClick = { vm.searchReferenceTracks(refQuery) { refResults = it } },
                                                        enabled = refQuery.isNotBlank(),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = com.example.mybeat.ui.theme.PurpleAccent,
                                                            contentColor = Color.Black
                                                        ),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ) { Text(stringResource(R.string.action_search)) }

                                                    OutlinedButton(
                                                        onClick = {resetRef()},
                                                        shape = RoundedCornerShape(16.dp)
                                                    ) { Text(stringResource(R.string.action_clear)) }
                                                }

                                                if (refResults.isNotEmpty()) {
                                                    Spacer(Modifier.height(6.dp))
                                                    refResults.forEach { t ->
                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    selectedRef = t
                                                                    refResults = emptyList()
                                                                },
                                                            shape = RoundedCornerShape(16.dp),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = Color.White.copy(alpha = 0.08f)
                                                            )
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(12.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                            ) {
                                                                if (t.artworkUrl.isNotBlank()) {
                                                                    AsyncImage(
                                                                        model = t.artworkUrl,
                                                                        contentDescription = null,
                                                                        modifier = Modifier
                                                                            .size(44.dp)
                                                                            .clip(RoundedCornerShape(12.dp))
                                                                    )
                                                                } else {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(44.dp)
                                                                            .clip(RoundedCornerShape(12.dp))
                                                                            .background(Color.White.copy(alpha = 0.10f)),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.AudioFile,
                                                                            contentDescription = null,
                                                                            tint = Color.White
                                                                        )
                                                                    }
                                                                }

                                                                Column(Modifier.weight(1f)) {
                                                                    Text(
                                                                        t.trackName,
                                                                        color = Color.White,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        fontWeight = FontWeight.SemiBold
                                                                    )
                                                                    Text(
                                                                        t.artistName,
                                                                        color = Color.White.copy(alpha = 0.65f),
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Spacer(Modifier.height(8.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // ---- DESCRIPTION ----
                                item {
                                    OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        label = {
                                            Text(
                                                stringResource(R.string.field_description_optional),
                                                color = Color.White.copy(alpha = 0.75f)
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 96.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = com.example.mybeat.ui.theme.PurpleAccent,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                                            focusedLabelColor = com.example.mybeat.ui.theme.PurpleAccent,
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                                            cursorColor = com.example.mybeat.ui.theme.PurpleAccent
                                        )
                                    )
                                }
                            }

                            // ---------------- FOOTER (sticky) ----------------
                            Divider(color = Color.White.copy(alpha = 0.10f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text(stringResource(R.string.action_close))
                                }

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
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = com.example.mybeat.ui.theme.PurpleAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text(stringResource(R.string.action_publish), fontWeight = FontWeight.SemiBold)
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

    fun stopPreview() {
        try { mp?.stop() } catch (_: Throwable) {}
        try { mp?.release() } catch (_: Throwable) {}
        mp = null
        playingPreview = false
    }

    fun playPreview(seconds: Long = 6L) {
        val url = beat.refPreviewUrl
        if (url.isBlank()) return

        stopPreview()

        playingPreview = true
        val player = MediaPlayer()
        mp = player

        try {
            player.setDataSource(url)
            player.setOnPreparedListener {
                it.start()
                scope.launch {
                    kotlinx.coroutines.delay(seconds * 1000)
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

    Dialog(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = GraySurface
        ){
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                // HEADER
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.community_details_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.community_details_author, beat.ownerId),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        TextButton(
                            onClick = {
                                stopPreview()
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.action_close),
                                color = PurpleAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // COVER
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AudioFile,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }
                    }
                }

                // TITLE
                item {
                    Text(
                        text = beat.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // DESCRIPTION CARD
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.field_description_optional),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            val desc = beat.description.trim()
                            Text(
                                text = if (desc.isNotBlank()) desc else stringResource(R.string.community_details_no_description),
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // REFERENCE CARD
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.community_details_reference_title),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            val hasRef = beat.refTrackName.isNotBlank() || beat.refArtistName.isNotBlank()
                            if (!hasRef) {
                                Text(
                                    text = stringResource(R.string.community_details_no_reference),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.community_details_reference_line,
                                        beat.refTrackName,
                                        beat.refArtistName
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val canPreview = beat.refPreviewUrl.isNotBlank()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { playPreview(seconds = 6) },
                                        enabled = canPreview && !playingPreview,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PurpleAccent,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            text = if (playingPreview)
                                                stringResource(R.string.community_details_preview_playing)
                                            else
                                                stringResource(R.string.community_details_preview_listen),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (playingPreview) {
                                        TextButton(onClick = { stopPreview() }) {
                                            Text(
                                                text = stringResource(R.string.common_stop),
                                                color = Color.White
                                            )
                                        }
                                    }

                                    if (!canPreview) {
                                        Text(
                                            text = stringResource(R.string.community_details_no_preview),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}