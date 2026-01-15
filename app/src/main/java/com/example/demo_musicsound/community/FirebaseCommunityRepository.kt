package com.example.demo_musicsound.community

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class FirebaseCommunityRepository(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    companion object {
        private const val COL_BEATS = "beats"
    }

    // ---------------------------
    // Queries
    // ---------------------------

    suspend fun getMyPublished(uid: String): List<CommunityBeat> {
        val snap = db.collection(COL_BEATS)
            .whereEqualTo("ownerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snap.documents.map { d -> d.toCommunityBeat() }
    }

    suspend fun getFromCommunity(uid: String): List<CommunityBeat> {
        val snap = db.collection(COL_BEATS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snap.documents
            .map { d -> d.toCommunityBeat() }
            .filter { it.ownerId.isNotBlank() && it.ownerId != uid }
    }

    // ---------------------------
    // Download
    // ---------------------------

    /**
     * Downloads a beat from Firebase Storage into the local exports folder
     * so your RecordScreen can use it (it reads /files/exports)
     */
    suspend fun downloadToLocalExports(
        context: Context,
        beat: CommunityBeat
    ): File {
        require(beat.audioPath.isNotBlank()) { "Missing audioPath" }

        val exportsDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val safeName = beat.title
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "beat_${System.currentTimeMillis()}" }

        val ext = beat.audioPath
            .substringAfterLast('.', missingDelimiterValue = "wav")
            .takeIf { it.length in 2..5 } ?: "wav"

        val out = uniqueTarget(File(exportsDir, "$safeName.$ext"))

        storage.reference
            .child(beat.audioPath)
            .getFile(out)
            .await()

        return out
    }

    private fun uniqueTarget(target: File): File {
        if (!target.exists()) return target
        val base = target.nameWithoutExtension
        val ext = target.extension.ifBlank { "wav" }
        var i = 2
        while (true) {
            val candidate = File(target.parentFile, "${base}_$i.$ext")
            if (!candidate.exists()) return candidate
            i++
        }
    }

    // ---------------------------
    // Covers
    // ---------------------------

    /**
     * Returns a public (signed) download URL for a cover stored in Firebase Storage.
     * Use this with Coil (AsyncImage).
     */
    suspend fun getCoverDownloadUrl(coverPath: String): String? {
        if (coverPath.isBlank()) return null
        return try {
            storage.reference
                .child(coverPath)
                .downloadUrl
                .await()
                .toString()
        } catch (_: Throwable) {
            null
        }
    }

    // ---------------------------
    // Publish
    // ---------------------------

    suspend fun publishBeat(
        context: Context,
        ownerId: String,           // ✅ uid da FirebaseAuth
        localBeatFile: File,
        title: String,
        description: String,
        coverUri: Uri?,
        spotifyRef: SpotifyTrackRef?
    ) {
        require(ownerId.isNotBlank()) { "Missing ownerId (user not logged in)" }

        // 1) crea doc id prima, così lo usi anche per i path su Storage
        val docRef = db.collection(COL_BEATS).document()
        val beatId = docRef.id

        // 2) upload audio
        val ext = localBeatFile.extension.ifBlank { "wav" }
        val audioPath = "beats/$ownerId/$beatId.$ext"
        storage.reference
            .child(audioPath)
            .putFile(Uri.fromFile(localBeatFile))
            .await()

        // 3) upload cover (opzionale)
        val coverPath = if (coverUri != null) {
            val coverExt = guessExtensionFromUri(context, coverUri) ?: "jpg"
            val path = "covers/$ownerId/$beatId.$coverExt"
            storage.reference.child(path).putFile(coverUri).await()
            path
        } else ""

        // 4) scrivi documento Firestore
        val now = System.currentTimeMillis()
        val data = hashMapOf(
            "ownerId" to ownerId,
            "title" to title,
            "description" to description,
            "audioPath" to audioPath,
            "coverPath" to coverPath,
            "createdAt" to now,

            // Spotify reference (opzionale)
            "spotifyTrackId" to (spotifyRef?.id ?: ""),
            "spotifyTrackName" to (spotifyRef?.name ?: ""),
            "spotifyTrackArtist" to (spotifyRef?.artist ?: ""),
            "spotifyUrl" to (spotifyRef?.url ?: "")
        )

        docRef.set(data).await()
    }

    private fun guessExtensionFromUri(context: Context, uri: Uri): String? {
        val type = context.contentResolver.getType(uri) ?: return null
        return when (type.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> null
        }
    }

    // ---------------------------
    // Helpers
    // ---------------------------

    private fun DocumentSnapshot.toCommunityBeat(): CommunityBeat {
        return CommunityBeat(
            id = id,
            ownerId = getString("ownerId") ?: "",
            title = getString("title") ?: "Untitled",
            audioPath = getString("audioPath") ?: "",
            coverPath = getString("coverPath") ?: "",
            createdAt = readCreatedAt(this)
        )
    }

    /**
     * Reads createdAt in a robust way:
     * - Number (Long)
     * - Timestamp
     * - String that contains a number
     */
    private fun readCreatedAt(doc: DocumentSnapshot): Long {
        val v = doc.get("createdAt") ?: return 0L
        return when (v) {
            is Number -> v.toLong()
            is Timestamp -> v.toDate().time
            is String -> v.toLongOrNull() ?: 0L
            else -> 0L
        }
    }
}