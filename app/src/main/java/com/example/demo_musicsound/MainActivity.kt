package com.example.demo_musicsound

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.demo_musicsound.Audio.RecorderManager
import com.example.demo_musicsound.Audio.Sequencer
import com.example.demo_musicsound.Audio.SoundManager

import com.example.demo_musicsound.ui.screen.PadScreen
import com.example.demo_musicsound.ui.screen.RecordScreen
import com.example.demo_musicsound.ui.screen.ExportScreen
import com.example.mybeat.ui.theme.MyBeatTheme
import com.example.mybeat.ui.theme.PurpleBar
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.demo_musicsound.community.CommunityViewModelFactory
import com.example.demo_musicsound.community.FirebaseCommunityRepository
import com.google.firebase.storage.FirebaseStorage

import com.example.demo_musicsound.ui.screen.CommunityScreen


class MainActivity : ComponentActivity() {

    private lateinit var sound: SoundManager
    private lateinit var seq: Sequencer
    private lateinit var rec: RecorderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        window.decorView.postDelayed(
            { keepSplash = false },
            1000 // 2 secondi
        )

        // --- Audio engines ---
        sound = SoundManager(this)
        seq = Sequencer()
        rec = RecorderManager(this)

        // --- Sounds Preload ---
        sound.preload("kick", R.raw.kick)
        sound.preload("snare", R.raw.snare)
        sound.preload("hat", R.raw.hat)
        sound.preload("clap", R.raw.clap)
        sound.preload("tom1", R.raw.tom1)
        sound.preload("tom2", R.raw.tom2)
        sound.preload("rim", R.raw.rim)
        sound.preload("shaker", R.raw.shaker)
        sound.preload("ohat", R.raw.ohat)
        sound.preload("ride", R.raw.ride)
        sound.preload("fx1", R.raw.fx1)
        sound.preload("fx2", R.raw.fx2)

        // 🔹 TEST FIRESTORE
        val db = Firebase.firestore
        db.collection("test").add(
            mapOf("hello" to "world", "time" to System.currentTimeMillis())
        ).addOnSuccessListener {
            Log.d("FIREBASE", "Documento scritto con successo!")
        }.addOnFailureListener { e ->
            Log.e("FIREBASE", "Errore Firestore", e)
        }

        setContent {
            MyBeatTheme(useDarkTheme = true) {
                MainScaffold(
                    sound = sound,
                    seq = seq,
                    rec = rec,
                    onRequestRecordPermission = { requestRecordPermission() }
                )
            }
        }
    }

    // Permission request for microphone
    private val recordPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
    fun MainScaffold(
        sound: SoundManager,
        seq: Sequencer,
        rec: RecorderManager,
        onRequestRecordPermission: () -> Unit
    ) {
        var tab by remember { mutableStateOf(0) } // 0=Pad, 1=Record

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        // Title
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "MyBeat",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PurpleBar,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                BottomTextNav(
                    selected = tab,
                    onSelectPad = { tab = 0 },
                    onSelectCommunity = { tab = 2 },
                    onSelectRecord = {
                        onRequestRecordPermission()
                        tab = 1
                    }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                val repo = remember {
                    FirebaseCommunityRepository(
                        db = Firebase.firestore,
                        storage = FirebaseStorage.getInstance()
                    )
                }
                val communityVm: com.example.demo_musicsound.community.CommunityViewModel =
                    viewModel(factory = CommunityViewModelFactory(repo))

                when (tab) {
                    0 -> PadScreen(sound = sound, seq = seq)
                    1 -> RecordScreen(rec = rec)
                    2 -> com.example.demo_musicsound.ui.screen.CommunityScreen(vm = communityVm)
                }

            }
        }
    }

    /* ------------------------  NAVBAR MINIMAL  ------------------------ */

    @Composable
    private fun BottomTextNav(
        selected: Int,
        onSelectPad: () -> Unit,
        onSelectCommunity: () -> Unit,
        onSelectRecord: () -> Unit
    ) {
        val textOn  = Color.White
        val textOff = Color.White.copy(alpha = 0.65f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(PurpleBar)
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // --- PAD (index 0) ---
                TextButton(onClick = onSelectPad) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Pad",
                        tint = if (selected == 0) textOn else textOff
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pad",
                        color = if (selected == 0) textOn else textOff,
                        style = if (selected == 0)
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.bodyMedium
                    )
                }

                // --- RECORD (index 1) ---
                TextButton(onClick = onSelectRecord) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record",
                        tint = if (selected == 1) textOn else textOff
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Record",
                        color = if (selected == 1) textOn else textOff,
                        style = if (selected == 1)
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.bodyMedium
                    )
                }

                // --- COMMUNITY (index 2) ---
                TextButton(onClick = onSelectCommunity) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Community",
                        tint = if (selected == 2) textOn else textOff
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Community",
                        color = if (selected == 2) textOn else textOff,
                        style = if (selected == 2)
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}