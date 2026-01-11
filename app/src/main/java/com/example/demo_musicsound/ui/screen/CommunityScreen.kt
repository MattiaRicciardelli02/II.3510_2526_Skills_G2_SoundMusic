package com.example.demo_musicsound.ui.screen

import android.content.Context
import androidx.compose.foundation.background
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
import coil.compose.AsyncImage
import com.example.demo_musicsound.Audio.BeatPlayer
import com.example.demo_musicsound.community.CommunityBeat
import com.example.demo_musicsound.community.CommunityViewModel
import com.example.mybeat.ui.theme.GrayBg
import com.example.mybeat.ui.theme.GraySurface
import com.example.mybeat.ui.theme.PurpleAccent
import com.example.mybeat.ui.theme.PurpleBar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(vm: CommunityViewModel) {
    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()

    val player = remember { BeatPlayer() }
    var playingFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = GrayBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Community",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PurpleBar
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (ui.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            ui.message?.let { msg ->
                AssistChip(onClick = {}, label = { Text(msg) })
            }

            /* ---------------- Published projects ---------------- */

            SectionTitle(
                title = "Published projects",
                trailing = {
                    TextButton(onClick = { vm.load() }) { Text("Refresh") }
                }
            )

            SectionCard {
                if (ui.myBeats.isEmpty()) {
                    EmptyState("No published projects yet.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(ui.myBeats, key = { it.id }) { beat ->
                            val coverUrl = ui.coverUrls[beat.id] // NEW
                            val localFile = findLocalExport(ctx, beat)

                            val isPlaying =
                                localFile != null &&
                                        playingFile?.absolutePath == localFile.absolutePath &&
                                        player.isPlaying()

                            BeatRow(
                                coverUrl = coverUrl,
                                title = beat.title,
                                subtitle = "by you",
                                primaryButton = {
                                    IconButton(
                                        enabled = localFile != null,
                                        onClick = {
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

            /* ---------------- Get from community ---------------- */

            SectionTitle(title = "Get from community")

            SectionCard {
                if (ui.communityBeats.isEmpty()) {
                    EmptyState("No community beats available.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(ui.communityBeats, key = { it.id }) { beat ->
                            val coverUrl = ui.coverUrls[beat.id] // NEW

                            BeatRow(
                                coverUrl = coverUrl,
                                title = beat.title,
                                subtitle = "by ${beat.ownerId}",
                                primaryButton = {
                                    FilledTonalIconButton(
                                        onClick = { vm.download(ctx, beat) },
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
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
    primaryButton: @Composable () -> Unit,
    secondaryButton: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = Color.White
                    )
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
