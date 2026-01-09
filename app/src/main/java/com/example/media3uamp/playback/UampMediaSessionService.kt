package com.example.media3uamp.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.example.media3uamp.data.BrowseTree
import com.example.media3uamp.data.CatalogRepository
import com.example.media3uamp.data.JsonSource
import com.example.media3uamp.data.MEDIA_ID_ALBUM_PREFIX
import com.example.media3uamp.data.MEDIA_ID_ROOT
import com.example.media3uamp.data.MEDIA_ID_TRACK_PREFIX
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
    private lateinit var musicSource: JsonSource
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val browseTreeMutex = Mutex()

    @Volatile
    private var browseTree: BrowseTree? = null

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        repository = CatalogRepository(this)
        musicSource = JsonSource(repository)
        session = MediaLibrarySession.Builder(this, player, LibraryCallback()).build()
        createNotificationChannel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        session

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
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            val tree = getBrowseTree()
            val resolved = ArrayList<MediaItem>(mediaItems.size)
            var resolvedStartIndex = if (startIndex >= 0) startIndex else 0

            mediaItems.forEachIndexed { originalIndex, item ->
                val beforeAddSize = resolved.size
                when {
                    item.mediaId.startsWith(MEDIA_ID_TRACK_PREFIX) -> {
                        val full = tree.getItem(item.mediaId) ?: item
                        resolved.add(full)
                    }
                    //把专辑下的 tracks 展开成很多个 track item（这就是“点专辑播放=顺序播放整张专辑”的实现）
                    item.mediaId.startsWith(MEDIA_ID_ALBUM_PREFIX) -> {
                        val tracks = tree.getChildren(item.mediaId).orEmpty()
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
            browseTree = null
            val tree = getBrowseTree()
            (session as? MediaLibrarySession)?.notifyChildrenChanged(
                MEDIA_ID_ROOT,
                tree.getChildren(MEDIA_ID_ROOT)?.size ?: 0,
                null
            )
            SessionResult(SessionResult.RESULT_SUCCESS)
        }

        override fun onGetLibraryRoot(
            librarySession: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ) = serviceScope.future {
            LibraryResult.ofItem(getBrowseTree().rootItem, params)
        }

        override fun onGetChildren(
            librarySession: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ) = serviceScope.future {
            val tree = getBrowseTree()
            val items = tree.getChildren(parentId)
            if (items == null) {
                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
            } else {
                LibraryResult.ofItemList(items, params)
            }
        }

        override fun onGetItem(
            librarySession: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ) = serviceScope.future {
            val tree = getBrowseTree()
            val item = tree.getItem(mediaId)
            if (item == null) {
                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
            } else {
                LibraryResult.ofItem(item, null)
            }
        }
    }
    private suspend fun getBrowseTree(): BrowseTree {
        browseTree?.let { return it }
        return browseTreeMutex.withLock {
            browseTree?.let { return@withLock it }
            musicSource.load()
            BrowseTree(musicSource).also { browseTree = it }
        }
    }

    private companion object {
        private const val CUSTOM_COMMAND_REFRESH_CATALOG = "refresh_catalog"
    }
}
