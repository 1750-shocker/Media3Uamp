package com.example.media3uamp.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackConnectionManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext: Context = context.applicationContext

    private val browserMutex = Mutex()
    private val controllerMutex = Mutex()

    @Volatile
    private var browser: MediaBrowser? = null

    @Volatile
    private var controller: MediaController? = null

    suspend fun getBrowser(): MediaBrowser {
        browser?.let { return it }
        return browserMutex.withLock {
            browser?.let { return it }
            withContext(Dispatchers.Main.immediate) {
                browser ?: run {
                    val token = SessionToken(
                        appContext,
                        ComponentName(appContext, UampMediaSessionService::class.java),
                    )
                    val b = MediaBrowser.Builder(appContext, token).buildAsync().await()
                    browser = b
                    b
                }
            }
        }
    }

    suspend fun getController(): MediaController {
        controller?.let { return it }
        return controllerMutex.withLock {
            controller?.let { return it }
            withContext(Dispatchers.Main.immediate) {
                controller ?: run {
                    val token = SessionToken(
                        appContext,
                        ComponentName(appContext, UampMediaSessionService::class.java),
                    )
                    val c = MediaController.Builder(appContext, token).buildAsync().await()
                    controller = c
                    c
                }
            }
        }
    }
}

