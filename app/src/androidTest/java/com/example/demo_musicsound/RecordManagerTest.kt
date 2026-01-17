package com.example.demo_musicsound.Audio

import android.Manifest
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RecorderManagerTest {

    // Grant RECORD_AUDIO so the test can actually start recording.
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun startThenStop_createsNonEmptyRecordingFile() {
        // Arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rec = RecorderManager(context)

        var started: File? = null
        var stopped: File? = null

        try {
            // Act
            started = rec.start()
            // Give the recorder a bit of time to write some audio.
            SystemClock.sleep(400)
            stopped = rec.stop()

            // Assert
            assertNotNull("Stop should return a file (or non-null path)", stopped)
            val f = stopped!!
            assertTrue("Recorded file should exist", f.exists())
            assertTrue("Recorded file should not be empty", f.length() > 0)
        } finally {
            // Best-effort cleanup in case the recorder is still running
            try { rec.stop() } catch (_: Throwable) {}
        }
    }
}