package com.example.media3uamp.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibrarySession
import androidx.media3.session.SessionToken
import com.example.media3uamp.data.Album
import com.example.media3uamp.data.CatalogRepository
import com.example.media3uamp.data.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UampMediaLibraryService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var librarySession: MediaLibrarySession
    private lateinit var repository: CatalogRepository

    override fun onCreate() {
        super.onCreate()
        repository = CatalogRepository(this)
        player = ExoPlayer.Builder(this).build()
        librarySession = MediaLibrarySession.Builder(this, player, LibraryCallback(this, repository, player)).build()
        createNotificationChannel()
    }

    override fun onGetSession(token: SessionToken): MediaLibrarySession? = librarySession

    override fun onDestroy() {
        librarySession.release()
        player.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "playback",
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}

private class LibraryCallback(
    private val context: Context,
    private val repository: CatalogRepository,
    private val player: Player
) : MediaLibrarySession.MediaLibrarySessionCallback {
    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaLibrarySession.LibraryBrowser?
    ): MediaLibraryService.MediaLibrarySession.LibraryResult<androidx.media3.common.MediaItem> {
        val root = androidx.media3.common.MediaItem.Builder()
            .setMediaId(ROOT_ALBUMS)
            .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle("Albums").build())
            .build()
        return MediaLibraryService.MediaLibrarySession.LibraryResult.ofItem(root)
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaLibrarySession.LibraryBrowser?,
        parentId: String,
        page: Int,
        pageSize: Int
    ): MediaLibraryService.MediaLibrarySession.LibraryResult<MutableList<androidx.media3.common.MediaItem>> {
        val scope = CoroutineScope(Dispatchers.IO)
        val list = mutableListOf<androidx.media3.common.MediaItem>()
        val latch = java.util.concurrent.CountDownLatch(1)
        scope.launch {
            when {
                parentId == ROOT_ALBUMS -> {
                    val albums = repository.getAlbums()
                    list.addAll(albums.map { it.toMediaItem() })
                }
                parentId.startsWith("album:") -> {
                    val albumId = parentId.substringAfter(":")
                    val tracks = repository.getTracks(albumId)
                    list.addAll(tracks.map { it.toMediaItem(albumId) })
                }
            }
            latch.countDown()
        }
        latch.await()
        return MediaLibraryService.MediaLibrarySession.LibraryResult.ofChildren(list, null)
    }

    override fun onAddMediaItems(
        mediaSession: MediaLibrarySession,
        controller: MediaLibrarySession.ControllerInfo,
        mediaItems: MutableList<androidx.media3.common.MediaItem>
    ): MutableList<androidx.media3.common.MediaItem> {
        val out = mediaItems.map { item ->
            val id = item.mediaId
            when {
                id.startsWith("track:") -> {
                    val trackId = id.substringAfter(":")
                    val albumId = item.mediaMetadata.albumTitle?.toString()?.lowercase()?.replace(" ", "_") ?: ""
                    val tracks = runBlockingIO { repository.getTracks(albumId) }
                    val track = tracks.firstOrNull { it.id == trackId }
                    track?.toMediaItem(albumId) ?: item
                }
                else -> item
            }
        }
        return out.toMutableList()
    }

    private inline fun <T> runBlockingIO(crossinline block: suspend () -> T): T {
        val scope = CoroutineScope(Dispatchers.IO)
        var result: T? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        scope.launch {
            result = block()
            latch.countDown()
        }
        latch.await()
        return result as T
    }

    companion object {
        const val ROOT_ALBUMS = "ALBUMS"
    }
}

