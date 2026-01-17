package com.example.demo_musicsound.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.demo_musicsound.R

@Composable
fun ExportScreen() {
    var beat by remember { mutableStateOf(0.8f) }
    var voice by remember { mutableStateOf(1.0f) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.export_title_demo))

        Text(stringResource(R.string.export_label_beat))
        Slider(value = beat, onValueChange = { beat = it })

        Text(stringResource(R.string.export_label_voice))
        Slider(value = voice, onValueChange = { voice = it })

        Button(onClick = { /* mock */ }) {
            Text(stringResource(R.string.export_action_wav))
        }

        Text(stringResource(R.string.export_footer_demo))
    }
}
