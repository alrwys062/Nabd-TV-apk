package com.example.services

import com.example.data.model.ChannelEntity
import com.example.data.model.StreamHealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChannelHealthChecker(baseClient: OkHttpClient) {

    private val probeClient = baseClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun checkStream(url: String, userAgent: String = ""): StreamHealthStatus =
        withContext(Dispatchers.IO) {
            val ua = userAgent.ifBlank { "NabdTV/1.0 (Android)" }
            val headReq = Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .head()
                .build()

            val headOk = runCatching {
                probeClient.newCall(headReq).execute().use { resp ->
                    resp.code in 200..399
                }
            }.getOrDefault(false)

            if (headOk) return@withContext StreamHealthStatus.ONLINE

            val getReq = Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .header("Range", "bytes=0-1023")
                .get()
                .build()

            val getOk = runCatching {
                probeClient.newCall(getReq).execute().use { resp ->
                    resp.code in 200..399
                }
            }.getOrDefault(false)

            if (getOk) StreamHealthStatus.ONLINE else StreamHealthStatus.OFFLINE
        }

    suspend fun checkBatch(
        channels: List<ChannelEntity>,
        onChannelChecked: suspend (channelId: String, status: StreamHealthStatus, doneCount: Int, totalCount: Int) -> Unit
    ) = coroutineScope {
        val semaphore = Semaphore(8)
        var completed = 0
        val total = channels.size
        channels.map { channel ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val status = checkStream(channel.streamUrl, channel.userAgent)
                    completed += 1
                    onChannelChecked(channel.id, status, completed, total)
                }
            }
        }.awaitAll()
    }
}
