package com.example.media3uamp.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class CatalogRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor { msg -> Log.d(TAG, msg) }
            .apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: Catalog? = null
    @Volatile
    private var lastFromNetwork: Boolean = false

    suspend fun loadCatalog(force: Boolean = false): Catalog = withContext(Dispatchers.IO) {
        if (!force) cache?.let { return@withContext it }
        Log.d(TAG, "start loadCatalog force=$force")
        val remote = downloadOrNull(REMOTE_URL)
        lastFromNetwork = remote != null
        val text = remote ?: readAssetOrNull(ASSET_FILE)
        Log.d(TAG, "loadCatalog fromNetwork=$lastFromNetwork textIsNull=${text == null}")
        val parsed = json.decodeFromString<Catalog>(text ?: "{\"music\":[]}")
        cache = parsed
        return@withContext parsed
    }

    suspend fun getAlbums(force: Boolean = false): List<Album> {
        val catalog = loadCatalog(force)
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

    fun clearCache() {
        cache = null
    }

    private fun downloadOrNull(url: String): String? = try {
        Log.d(TAG, "download $url")
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            Log.d(TAG, "response code=${resp.code} success=${resp.isSuccessful}")
            if (!resp.isSuccessful) return null
            val body = resp.body?.string()
            Log.d(TAG, "response body null=${body == null} length=${body?.length}")
            body
        }
    } catch (e: Exception) {
        Log.d(TAG, "download error ${e.message}")
        null
    }

    private fun readAssetOrNull(name: String): String? = try {
        Log.d(TAG, "read asset $name")
        context.assets.open(name).use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        }
    } catch (e: Exception) {
        Log.d(TAG, "asset read error ${e.message}")
        null
    }

    companion object {
        const val TAG = "CatalogRepository"
        const val REMOTE_URL = "https://storage.googleapis.com/uamp/catalog.json"
        const val ASSET_FILE = "catalog.json"
    }
}

