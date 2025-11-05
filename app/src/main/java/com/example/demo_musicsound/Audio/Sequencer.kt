package com.example.demo_musicsound.Audio

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sequencer
 * - Holds per-sound boolean patterns (Compose-friendly)
 * - Ticks in a coroutine and triggers SoundManager on active steps
 */
class Sequencer(
    private var bpm: Int = 120,
    private val steps: Int = 16
) {
    private var job: Job? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> get() = _running

    private val grid: MutableMap<String, SnapshotStateList<Boolean>> = mutableMapOf()

    /** Ensure a pattern exists for this resName */
    fun pattern(resName: String): SnapshotStateList<Boolean> =
        grid.getOrPut(resName) { mutableStateListOf<Boolean>().apply { repeat(steps) { add(false) } } }

    /** Toggle a single step (immediate UI update) */
    fun toggle(resName: String, index: Int) {
        val p = pattern(resName)
        if (index in 0 until p.size) p[index] = !p[index]
    }

    /** Ensure patterns exist for a list of sounds */
    fun ensureAll(resNames: List<String>) { resNames.forEach { pattern(it) } }

    /** Clear patterns for selected sounds */
    fun clear(resNames: List<String>) { resNames.forEach { p -> for (i in pattern(p).indices) pattern(p)[i] = false } }

    /** Clear everything */
    fun clearAll() { grid.values.forEach { p -> for (i in p.indices) p[i] = false } }

    /** Update tempo */
    fun setBpm(value: Int) { bpm = value.coerceIn(40, 240) }

    /** Start ticking */
    fun start(scope: CoroutineScope, sound: SoundManager, onTick: (Int) -> Unit = {}) {
        stop()
        val stepMs = (60000 / bpm) / 4 // 1/16 note
        job = scope.launch(Dispatchers.Main) {
            _running.value = true
            var i = 0
            while (isActive) {
                grid.forEach { (res, list) -> if (list[i]) sound.play(res) }
                onTick(i)
                i = (i + 1) % steps
                delay(stepMs.toLong())
            }
        }
    }

    /** Stop ticking */
    fun stop() {
        job?.cancel(); job = null
        _running.value = false
    }
}