package com.example.media3uamp.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.guava.await

object PlaybackClient {
    @Volatile
    private var browser: MediaBrowser? = null
    @Volatile
    private var controller: MediaController? = null

    suspend fun getBrowser(context: Context): MediaBrowser = withContext(Dispatchers.Main) {
        browser ?: run {
            val token =
                SessionToken(context, ComponentName(context, UampMediaSessionService::class.java))
            val b = MediaBrowser.Builder(context, token).buildAsync().await()
            browser = b
            b
        }
    }

    suspend fun getController(context: Context): MediaController = withContext(Dispatchers.Main) {
        controller ?: run {
            val token =
                SessionToken(context, ComponentName(context, UampMediaSessionService::class.java))
            val c = MediaController.Builder(context, token).buildAsync().await()
            controller = c
            c
        }
    }
}

