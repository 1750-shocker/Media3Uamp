package com.example.media3uamp.di

import android.content.Context
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import com.example.media3uamp.data.JsonSource
import com.example.media3uamp.data.CatalogRepository
import com.example.media3uamp.data.MusicSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { msg -> Log.d("OkHttp", msg) }
            .apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }
}

@Module
@InstallIn(dagger.hilt.android.components.ServiceComponent::class)
object ServiceModule {
    @Provides
    @ServiceScoped
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer =
        ExoPlayer.Builder(context).build()

    @Provides
    @ServiceScoped
    fun provideMusicSource(repository: CatalogRepository): MusicSource =
        JsonSource(repository)
}
