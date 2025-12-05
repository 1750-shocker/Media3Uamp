package com.example.media3uamp

import com.example.media3uamp.data.Catalog
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogParsingTest {
    @Test
    fun parseCatalogJson() {
        val jsonText = """
            {"music":[
              {"id":"a1","title":"SongA","album":"AlbumA","artist":"ArtistA","source":"https://example.com/a.mp3","image":"https://example.com/a.jpg"},
              {"id":"a2","title":"SongB","album":"AlbumA","artist":"ArtistA","source":"https://example.com/b.mp3","image":"https://example.com/a.jpg"}
            ]}
        """.trimIndent()
        val json = Json { ignoreUnknownKeys = true }
        val catalog = json.decodeFromString(Catalog.serializer(), jsonText)
        assertEquals(2, catalog.music.size)
        assertEquals("AlbumA", catalog.music.first().album)
    }
}

