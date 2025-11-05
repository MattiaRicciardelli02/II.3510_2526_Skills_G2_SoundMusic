package com.example.demo_musicsound

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.demo_musicsound.Audio.RecorderManager
import com.example.demo_musicsound.Audio.Sequencer
import com.example.demo_musicsound.Audio.SoundManager


import com.example.demo_musicsound.ui.screen.PadScreen
import com.example.demo_musicsound.ui.screen.RecordScreen
import com.example.demo_musicsound.ui.screen.ExportScreen



class MainActivity : ComponentActivity() {

    private lateinit var sound: SoundManager
    private lateinit var seq: Sequencer
    private lateinit var rec: RecorderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Audio engines ---
        sound = SoundManager(this)
        seq   = Sequencer()
        rec   = RecorderManager(this)

        // --- Preload TUTTI i 12 suoni (assicurati che esistano in res/raw) ---
        // Puoi commentare temporaneamente quelli che non hai ancora.
        sound.preload("kick",   R.raw.kick)
        sound.preload("snare",  R.raw.snare)
        sound.preload("hat",    R.raw.hat)
        sound.preload("clap",   R.raw.clap)
        sound.preload("tom1",   R.raw.tom1)
        sound.preload("tom2",   R.raw.tom2)
        sound.preload("rim",    R.raw.rim)
        sound.preload("shaker", R.raw.shaker)
        sound.preload("ohat",   R.raw.ohat)
        sound.preload("ride",   R.raw.ride)
        sound.preload("fx1",    R.raw.fx1)
        sound.preload("fx2",    R.raw.fx2)

        setContent {
            MaterialTheme {
                MainScaffold(
                    onRequestRecordPermission = { requestRecordPermission() }
                )
            }
        }
    }

    // Richiesta permesso microfono quando si entra nella tab Record per la prima volta
    private val recordPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private fun requestRecordPermission() {
        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onDestroy() {
        super.onDestroy()
        sound.release()
        seq.stop()
    }

    // UI con 3 tab: Pad / Record / Export
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScaffold(onRequestRecordPermission: () -> Unit) {
        var tab by remember { mutableStateOf(0) }
        Scaffold(
            topBar = { TopAppBar(title = { Text("MyBeat") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        label = { Text("Pad") }
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = {
                            onRequestRecordPermission()
                            tab = 1
                        },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        label = { Text("Record") }
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        label = { Text("Export") }
                    )
                }
            }
        ) { paddingValues ->
            Box(Modifier.padding(paddingValues)) {
                when (tab) {
                    0 -> PadScreen(sound = sound, seq = seq)   // 12 suoni in 2 pagine da 6
                    1 -> RecordScreen(rec = rec)
                    2 -> ExportScreen()
                }
            }
        }
    }
}