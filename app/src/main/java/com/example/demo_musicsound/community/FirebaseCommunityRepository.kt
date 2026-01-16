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
        private const val COL_USERS = "users"
        private const val SUB_LIBRARY = "library"
    }

    // ---------------------------
    // Community queries
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
    // Download helpers
    // ---------------------------

    private fun exportsDir(context: Context): File =
        File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }

    private fun slugTitle(title: String): String =
        title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "beat" }

    private fun extFromAudioPath(audioPath: String): String =
        audioPath.substringAfterLast('.', missingDelimiterValue = "wav")
            .takeIf { it.length in 2..5 } ?: "wav"

    /**
     * Stable local filename: title_slug + "_" + beatId + "." + ext
     */
    private fun stableLocalFile(context: Context, beat: CommunityBeat): File {
        val dir = exportsDir(context)
        val safeTitle = slugTitle(beat.title)
        val ext = extFromAudioPath(beat.audioPath)
        val id = beat.id.ifBlank { "unknown" }
        return File(dir, "${safeTitle}_$id.$ext")
    }

    private suspend fun downloadIfMissing(storagePath: String, out: File): File {
        if (out.exists() && out.isFile) return out
        out.parentFile?.mkdirs()

        storage.reference
            .child(storagePath)
            .getFile(out)
            .await()

        return out
    }

    // ---------------------------
    // Download (community)
    // ---------------------------

    suspend fun downloadToLocalExports(context: Context, beat: CommunityBeat): File {
        require(beat.audioPath.isNotBlank()) { "Missing audioPath" }
        val out = stableLocalFile(context, beat)
        return downloadIfMissing(beat.audioPath, out)
    }

    // ---------------------------
    // Covers
    // ---------------------------

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
    // Publish (community) - iTunes reference
    // ---------------------------

    suspend fun publishBeat(
        context: Context,
        ownerId: String,
        localBeatFile: File,
        title: String,
        description: String,
        coverUri: Uri?,
        reference: ReferenceTrack?
    ) {
        require(ownerId.isNotBlank()) { "Missing ownerId (user not logged in)" }

        val docRef = db.collection(COL_BEATS).document()
        val beatId = docRef.id

        // Upload audio
        val ext = localBeatFile.extension.ifBlank { "wav" }
        val audioPath = "beats/$ownerId/$beatId.$ext"
        storage.reference
            .child(audioPath)
            .putFile(Uri.fromFile(localBeatFile))
            .await()

        // Upload cover (optional)
        val coverPath = if (coverUri != null) {
            val coverExt = guessExtensionFromUri(context, coverUri) ?: "jpg"
            val path = "covers/$ownerId/$beatId.$coverExt"
            storage.reference.child(path).putFile(coverUri).await()
            path
        } else ""

        val now = System.currentTimeMillis()

        val data = hashMapOf(
            "ownerId" to ownerId,
            "title" to title,
            "description" to description,
            "audioPath" to audioPath,
            "coverPath" to coverPath,
            "createdAt" to now,

            // Reference track (iTunes)
            "refProvider" to (reference?.provider ?: ""),
            "refTrackId" to (reference?.trackId ?: ""),
            "refTrackName" to (reference?.trackName ?: ""),
            "refArtistName" to (reference?.artistName ?: ""),
            "refUrl" to (reference?.trackViewUrl ?: ""),
            "refPreviewUrl" to (reference?.previewUrl ?: ""),
            "refArtworkUrl" to (reference?.artworkUrl ?: "")
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
    // Library (private per user) - kept, but NO spotify fields
    // ---------------------------

    suspend fun addToLibrary(
        ownerId: String,
        beatId: String,
        localBeatFile: File,
        title: String,
        createdAt: Long = System.currentTimeMillis()
    ): CommunityBeat {
        require(ownerId.isNotBlank()) { "Missing ownerId (user not logged in)" }
        require(beatId.isNotBlank()) { "Missing beatId" }
        require(localBeatFile.exists()) { "Local beat file does not exist" }

        val docRef = db.collection(COL_USERS)
            .document(ownerId)
            .collection(SUB_LIBRARY)
            .document(beatId)

        val ext = localBeatFile.extension.ifBlank { "wav" }
        val audioPath = "library/$ownerId/$beatId.$ext"

        storage.reference
            .child(audioPath)
            .putFile(Uri.fromFile(localBeatFile))
            .await()

        val data = hashMapOf(
            "ownerId" to ownerId,
            "title" to title,
            "audioPath" to audioPath,
            "coverPath" to "",
            "createdAt" to createdAt,
            "description" to "",

            // keep same schema fields used by toCommunityBeat()
            "refProvider" to "",
            "refTrackId" to "",
            "refTrackName" to "",
            "refArtistName" to "",
            "refUrl" to "",
            "refPreviewUrl" to "",
            "refArtworkUrl" to ""
        )

        docRef.set(data).await()

        return CommunityBeat(
            id = beatId,
            ownerId = ownerId,
            title = title,
            audioPath = audioPath,
            coverPath = "",
            createdAt = createdAt,
            description = ""
        )
    }

    suspend fun getMyLibrary(uid: String): List<CommunityBeat> {
        val snap = db.collection(COL_USERS)
            .document(uid)
            .collection(SUB_LIBRARY)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snap.documents.map { d -> d.toCommunityBeat() }
    }

    suspend fun deleteFromLibrary(uid: String, beatId: String) {
        val docRef = db.collection(COL_USERS)
            .document(uid)
            .collection(SUB_LIBRARY)
            .document(beatId)

        val doc = docRef.get().await()
        val audioPath = doc.getString("audioPath").orEmpty()

        docRef.delete().await()

        if (audioPath.isNotBlank()) {
            try {
                storage.reference.child(audioPath).delete().await()
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    suspend fun downloadLibraryBeatToLocalExports(context: Context, beat: CommunityBeat): File {
        require(beat.audioPath.isNotBlank()) { "Missing audioPath" }

        val exportsDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }

        val ext = beat.audioPath
            .substringAfterLast('.', missingDelimiterValue = "wav")
            .takeIf { it.length in 2..5 } ?: "wav"

        val out = File(exportsDir, "${beat.id}.$ext")

        storage.reference
            .child(beat.audioPath)
            .getFile(out)
            .await()

        return out
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
            createdAt = readCreatedAt(this),
            description = getString("description") ?: "",

            // Reference track (iTunes)
            refProvider = getString("refProvider") ?: "",
            refTrackId = getString("refTrackId") ?: "",
            refTrackName = getString("refTrackName") ?: "",
            refArtistName = getString("refArtistName") ?: "",
            refUrl = getString("refUrl") ?: "",
            refPreviewUrl = getString("refPreviewUrl") ?: "",
            refArtworkUrl = getString("refArtworkUrl") ?: ""
        )
    }

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
