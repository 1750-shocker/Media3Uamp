package com.example.media3uamp.data

import android.os.Bundle
import androidx.annotation.IntDef
import androidx.media3.common.MediaItem

interface MusicSource : Iterable<MediaItem> {
    suspend fun load()
    fun whenReady(performAction: (Boolean) -> Unit): Boolean
    fun search(query: String, extras: Bundle): List<MediaItem>
}

@IntDef(
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
)

@Retention(AnnotationRetention.SOURCE)
annotation class State

const val STATE_CREATED = 1
const val STATE_INITIALIZING = 2
const val STATE_INITIALIZED = 3
const val STATE_ERROR = 4

abstract class AbstractMusicSource : MusicSource {
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    @State
    var state: Int = STATE_CREATED
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    override fun whenReady(performAction: (Boolean) -> Unit): Boolean =
        when (state) {
            STATE_CREATED, STATE_INITIALIZING -> {
                onReadyListeners += performAction
                false
            }

            else -> {
                performAction(state != STATE_ERROR)
                true
            }
        }

}