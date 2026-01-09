package com.example.media3uamp.data

import android.content.ContentProvider
import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit


const val DOWNLOAD_TIMEOUT_SECONDS = 30L
const val ALBUM_ART_CONTENT_AUTHORITY = "com.example.media3uamp.albumart"

internal class AlbumArtContentProvider : ContentProvider() {
    companion object {
        private val uriMap = mutableMapOf<Uri, Uri>()
        fun mapUri(uri: Uri): Uri {
            val key = uri.toString().hashCode().toUInt().toString(16)
            val contentUri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)//协议：内容URI
                .authority(ALBUM_ART_CONTENT_AUTHORITY)//内容提供者
                .appendPath(key)
                .build()
            uriMap[contentUri] = uri
            return contentUri
        }
    }

    override fun onCreate() = true//表示 ContentProvider 成功创建

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = this.context ?: return null
        val remoteUri = uriMap[uri] ?: throw FileNotFoundException(uri.path)
        val key = uri.lastPathSegment ?: throw FileNotFoundException(uri.path)
        var file = File(context.cacheDir, "albumart_$key")
        if (!file.exists()) {
            val cacheFile = Glide.with(context)
                .asFile()
                .load(remoteUri)
                .submit()//submit() 返回一个 Future，调用 get() 方法来等待下载结果，
                .get(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)//设置了超时为 DOWNLOAD_TIMEOUT_SECONDS 秒。
            cacheFile.renameTo(file)
            file = cacheFile
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: android.content.ContentValues?): Uri? = null
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): android.database.Cursor? = null

    override fun update(
        uri: Uri,
        values: android.content.ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ) = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
    override fun getType(uri: Uri): String? = null

}
