package com.example.demo_musicsound.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExportScreen() {
    var beat by remember { mutableStateOf(0.8f) }
    var voice by remember { mutableStateOf(1.0f) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Export (Demo Lite)")
        Text("Beat"); Slider(beat, { beat = it })
        Text("Voce"); Slider(voice, { voice = it })
        Button(onClick = { /* mock */ }) { Text("Esporta WAV") }
        Text("Nella Lite l’export è dimostrativo: spiega il render offline nelle slide.")
    }
}