package com.example.media3uamp.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.example.media3uamp.data.Album
import com.example.media3uamp.data.CatalogRepository
import com.example.media3uamp.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UampMediaSessionService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var repository: CatalogRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val libraryDataMutex = Mutex()
    @Volatile private var libraryData: LibraryData? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        repository = CatalogRepository(this)
        session = MediaLibrarySession.Builder(this, player, LibraryCallback()).build()
        createNotificationChannel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session

    override fun onDestroy() {
        session.release()
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

    @UnstableApi
    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ) = serviceScope.future {
            val data = getLibraryData()
            val resolved = ArrayList<MediaItem>(mediaItems.size)
            var resolvedStartIndex = if (startIndex >= 0) startIndex else 0

            mediaItems.forEachIndexed { originalIndex, item ->
                val beforeAddSize = resolved.size
                when {
                    item.mediaId.startsWith(MEDIA_ID_TRACK_PREFIX) -> {
                        val trackId = item.mediaId.substringAfter(MEDIA_ID_TRACK_PREFIX)
                        val full = data.trackById[trackId]?.toTrackItem() ?: item
                        resolved.add(full)
                    }
                    item.mediaId.startsWith(MEDIA_ID_ALBUM_PREFIX) -> {
                        val albumId = item.mediaId.substringAfter(MEDIA_ID_ALBUM_PREFIX)
                        val tracks = data.tracksByAlbumId[albumId].orEmpty().map { it.toTrackItem() }
                        resolved.addAll(tracks)
                    }
                    else -> resolved.add(item)
                }
                if (originalIndex == startIndex) {
                    resolvedStartIndex = beforeAddSize
                }
            }

            if (resolved.isEmpty()) {
                return@future MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            }

            val safeStartIndex = resolvedStartIndex.coerceIn(0, resolved.lastIndex)
            MediaItemsWithStartPosition(resolved, safeStartIndex, startPositionMs)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ) = serviceScope.future {
            if (customCommand.customAction != CUSTOM_COMMAND_REFRESH_CATALOG) {
                return@future SessionResult(SessionError.ERROR_NOT_SUPPORTED)
            }
            repository.clearCache()
            libraryData = null
            val data = getLibraryData(force = true)
            (session as? MediaLibrarySession)?.notifyChildrenChanged(MEDIA_ID_ROOT, data.albums.size, null)
            SessionResult(SessionResult.RESULT_SUCCESS)
        }

        override fun onGetLibraryRoot(
            librarySession: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ) = serviceScope.future {
            LibraryResult.ofItem(rootItem(), params)
        }

        override fun onGetChildren(
            librarySession: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ) = serviceScope.future {
            val data = getLibraryData()
            val (items, knownParent) = when {
                parentId == MEDIA_ID_ROOT -> data.albums.map { it.toAlbumItem() } to true
                parentId.startsWith(MEDIA_ID_ALBUM_PREFIX) -> {
                    val albumId = parentId.substringAfter(MEDIA_ID_ALBUM_PREFIX)
                    data.tracksByAlbumId[albumId].orEmpty().map { it.toTrackItem() } to data.albumById.containsKey(albumId)
                }
                else -> emptyList<MediaItem>() to false
            }
            if (!knownParent) {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            } else {
                LibraryResult.ofItemList(items, params)
            }
        }

        override fun onGetItem(
            librarySession: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ) = serviceScope.future {
            val data = getLibraryData()
            val item = when {
                mediaId == MEDIA_ID_ROOT -> rootItem()
                mediaId.startsWith(MEDIA_ID_ALBUM_PREFIX) -> {
                    val albumId = mediaId.substringAfter(MEDIA_ID_ALBUM_PREFIX)
                    data.albumById[albumId]?.toAlbumItem()
                }
                mediaId.startsWith(MEDIA_ID_TRACK_PREFIX) -> {
                    val trackId = mediaId.substringAfter(MEDIA_ID_TRACK_PREFIX)
                    data.trackById[trackId]?.toTrackItem()
                }
                else -> null
            }
            if (item == null) {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            } else {
                LibraryResult.ofItem(item, null)
            }
        }

        private fun Album.toAlbumItem(): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(title)
                .setArtworkUri(safeUri(artwork))
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
            return MediaItem.Builder()
                .setMediaId("$MEDIA_ID_ALBUM_PREFIX$id")
                .setMediaMetadata(metadata)
                .build()
        }

        private fun Track.toTrackItem(): MediaItem {
            val extras = Bundle().apply {
                putInt("durationSec", (duration ?: 0L).toInt())
            }
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(safeUri(image))
                .setTrackNumber(trackNumber ?: 0)
                .setTotalTrackCount(totalTrackCount ?: 0)
                .setExtras(extras)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
            val uri = safeUri(source)
            return MediaItem.Builder()
                .setMediaId("$MEDIA_ID_TRACK_PREFIX$id")
                .setUri(uri)
                .setMediaMetadata(metadata)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(uri)
                        .build()
                )
                .build()
        }
    }

    private data class LibraryData(
        val albums: List<Album>,
        val albumById: Map<String, Album>,
        val tracksByAlbumId: Map<String, List<Track>>,
        val trackById: Map<String, Track>,
    )

    private suspend fun getLibraryData(force: Boolean = false): LibraryData {
        if (!force) libraryData?.let { return it }
        return libraryDataMutex.withLock {
            if (!force) libraryData?.let { return@withLock it }
            val albums = repository.getAlbums(force)
            val albumById = albums.associateBy { it.id }
            val tracksByAlbumId = albums.associate { it.id to it.tracks }
            val trackById = albums.asSequence()
                .flatMap { it.tracks.asSequence() }
                .associateBy { it.id }
            val data = LibraryData(
                albums = albums,
                albumById = albumById,
                tracksByAlbumId = tracksByAlbumId,
                trackById = trackById,
            )
            libraryData = data
            data
        }
    }

    private fun rootItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(MEDIA_ID_ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("UAMP Library")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun safeUri(s: String?): Uri? {
        if (s.isNullOrBlank()) return null
        val fixed = s.replace(" ", "%20")
        return try {
            Uri.parse(fixed)
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        private const val MEDIA_ID_ROOT = "root"
        private const val MEDIA_ID_ALBUM_PREFIX = "album:"
        private const val MEDIA_ID_TRACK_PREFIX = "track:"
        private const val CUSTOM_COMMAND_REFRESH_CATALOG = "refresh_catalog"
    }
}
