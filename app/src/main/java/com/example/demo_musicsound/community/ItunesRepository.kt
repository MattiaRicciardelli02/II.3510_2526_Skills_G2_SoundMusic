package com.example.demo_musicsound.community

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

class ItunesRepository {

    suspend fun searchTracks(query: String, limit: Int = 10): List<ReferenceTrack> {
        val q = query.trim()
        if (q.length < 2) return emptyList()

        val encoded = URLEncoder.encode(q, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=$limit"

        return withContext(Dispatchers.IO) {
            val text = URL(url).readText()
            val obj = JSONObject(text)
            val results = obj.optJSONArray("results") ?: return@withContext emptyList()

            val out = ArrayList<ReferenceTrack>(results.length())
            for (i in 0 until results.length()) {
                val r = results.getJSONObject(i)

                val trackId = r.optLong("trackId", 0L).toString()
                val name = r.optString("trackName", "")
                val artist = r.optString("artistName", "")
                val viewUrl = r.optString("trackViewUrl", "")
                val previewUrl = r.optString("previewUrl", "")
                val artwork100 = r.optString("artworkUrl100", "")

                val artworkLarge = artwork100.replace("100x100bb", "600x600bb")

                if (trackId != "0" && name.isNotBlank()) {
                    out.add(
                        ReferenceTrack(
                            provider = "itunes",
                            trackId = trackId,
                            trackName = name,
                            artistName = artist,
                            trackViewUrl = viewUrl,
                            previewUrl = previewUrl,
                            artworkUrl = artworkLarge.ifBlank { artwork100 }
                        )
                    )
                }
            }
            out
        }
    }
}
