package com.example.media3uamp.data

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader

class CatalogRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var cache: Catalog? = null

    suspend fun loadCatalog(): Catalog {
        cache?.let { return it }
        val text = downloadOrNull(REMOTE_URL) ?: readAssetOrNull(ASSET_FILE)
        val parsed = json.decodeFromString(Catalog.serializer(), text ?: "{\"music\":[]}")
        cache = parsed
        return parsed
    }

    suspend fun getAlbums(): List<Album> {
        val catalog = loadCatalog()
        val grouped = catalog.music.groupBy { it.album }
        return grouped.map { (albumTitle, tracks) ->
            val first = tracks.firstOrNull()
            Album(
                id = (albumTitle + "_" + (first?.artist ?: "")).lowercase().replace(" ", "_"),
                title = albumTitle,
                artist = first?.artist ?: "",
                year = null,
                artwork = first?.image,
                tracks = tracks
            )
        }.sortedBy { it.title }
    }

    suspend fun getTracks(albumId: String): List<Track> {
        val albums = getAlbums()
        return albums.firstOrNull { it.id == albumId }?.tracks ?: emptyList()
    }

    private fun downloadOrNull(url: String): String? = try {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            resp.body?.string()
        }
    } catch (_: Exception) { null }

    private fun readAssetOrNull(name: String): String? = try {
        context.assets.open(name).use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        }
    } catch (_: Exception) { null }

    companion object {
        const val REMOTE_URL = "https://storage.googleapis.com/uamp/catalog.json"
        const val ASSET_FILE = "catalog.json"
    }
}

