package com.example.services

import android.content.Context
import android.os.Environment
import com.example.data.local.NabdDao
import com.example.data.model.DownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadService(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val dao: NabdDao
) {
    suspend fun downloadMedia(
        contentId: String,
        title: String,
        posterUrl: String,
        remoteUrl: String
    ) = withContext(Dispatchers.IO) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()

        val safeName = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40).ifBlank { contentId }
        val ext = remoteUrl.substringAfterLast('.', "mp4").substringBefore('?').take(5).ifBlank { "mp4" }
        val targetFile = File(dir, "${safeName}_${contentId.take(8)}.$ext")

        dao.upsertDownload(
            DownloadEntity(
                contentId = contentId,
                title = title,
                posterUrl = posterUrl,
                remoteUrl = remoteUrl,
                localFilePath = targetFile.absolutePath,
                progressPercent = 0,
                status = "DOWNLOADING"
            )
        )

        try {
            val req = Request.Builder().url(remoteUrl).build()
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    dao.upsertDownload(
                        DownloadEntity(
                            contentId = contentId,
                            title = title,
                            posterUrl = posterUrl,
                            remoteUrl = remoteUrl,
                            status = "FAILED"
                        )
                    )
                    return@withContext
                }
                val body = response.body ?: return@withContext
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                var lastProgress = -1

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else 50
                            if (percent != lastProgress && (percent % 5 == 0 || percent == 100)) {
                                lastProgress = percent
                                dao.upsertDownload(
                                    DownloadEntity(
                                        contentId = contentId,
                                        title = title,
                                        posterUrl = posterUrl,
                                        remoteUrl = remoteUrl,
                                        localFilePath = targetFile.absolutePath,
                                        progressPercent = percent,
                                        status = "DOWNLOADING",
                                        totalBytes = totalBytes,
                                        downloadedBytes = downloadedBytes
                                    )
                                )
                            }
                        }
                    }
                }

                dao.upsertDownload(
                    DownloadEntity(
                        contentId = contentId,
                        title = title,
                        posterUrl = posterUrl,
                        remoteUrl = remoteUrl,
                        localFilePath = targetFile.absolutePath,
                        progressPercent = 100,
                        status = "COMPLETED",
                        totalBytes = totalBytes,
                        downloadedBytes = downloadedBytes
                    )
                )
            }
        } catch (_: Exception) {
            dao.upsertDownload(
                DownloadEntity(
                    contentId = contentId,
                    title = title,
                    posterUrl = posterUrl,
                    remoteUrl = remoteUrl,
                    status = "FAILED"
                )
            )
        }
    }

    suspend fun removeDownload(item: DownloadEntity) = withContext(Dispatchers.IO) {
        if (item.localFilePath.isNotBlank()) {
            runCatching { File(item.localFilePath).delete() }
        }
        dao.deleteDownload(item.contentId)
    }
}
