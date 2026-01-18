data class CommunityBeat(
    val id: String = "",
    val ownerId: String = "",

    // ✅ NEW: campi display
    val ownerUsername: String = "",
    val ownerDisplayName: String = "",

    val title: String = "",
    val audioPath: String = "",
    val coverPath: String = "",
    val createdAt: Long = 0L,
    val description: String = "",

    val refProvider: String = "",
    val refTrackId: String = "",
    val refTrackName: String = "",
    val refArtistName: String = "",
    val refUrl: String = "",
    val refPreviewUrl: String = "",
    val refArtworkUrl: String = ""
)