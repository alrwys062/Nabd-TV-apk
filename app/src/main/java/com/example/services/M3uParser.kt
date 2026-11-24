package com.example.services

import com.example.data.model.ChannelEntity
import java.io.BufferedReader
import java.io.Reader
import java.security.MessageDigest

data class M3uParseResult(
    val channels: List<ChannelEntity>,
    val detectedEpgUrl: String? = null
)

object M3uParser {

    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")

    fun parse(reader: Reader, serverId: String): M3uParseResult {
        val channels = ArrayList<ChannelEntity>(1024)
        var detectedEpgUrl: String? = null

        var pendingName = ""
        var pendingTvgId = ""
        var pendingLogo = ""
        var pendingGroup = ""
        var pendingCountry = ""
        var pendingLanguage = ""
        var pendingUserAgent = ""
        var hasPendingExtInf = false
        var orderIndex = 0

        BufferedReader(reader, 64 * 1024).use { br ->
            var line: String? = br.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    line = br.readLine()
                    continue
                }

                when {
                    trimmed.startsWith("#EXTM3U", ignoreCase = true) -> {
                        val attrs = parseAttributes(trimmed)
                        detectedEpgUrl = attrs["url-tvg"]
                            ?: attrs["x-tvg-url"]
                            ?: attrs["epg-url"]
                    }

                    trimmed.startsWith("#EXTINF:", ignoreCase = true) -> {
                        hasPendingExtInf = true
                        val commaIdx = findDisplayTitleCommaIndex(trimmed)
                        val headerPart = if (commaIdx >= 0) trimmed.substring(0, commaIdx) else trimmed
                        val titlePart = if (commaIdx >= 0 && commaIdx + 1 < trimmed.length) {
                            trimmed.substring(commaIdx + 1).trim()
                        } else ""

                        val attrs = parseAttributes(headerPart)
                        pendingTvgId = attrs["tvg-id"]?.trim().orEmpty()
                        val tvgName = attrs["tvg-name"]?.trim().orEmpty()
                        pendingName = titlePart.ifBlank { tvgName }.ifBlank { "Channel ${orderIndex + 1}" }
                        pendingLogo = attrs["tvg-logo"]?.trim().orEmpty()
                        pendingGroup = normalizeCategory(attrs["group-title"]?.trim().orEmpty())
                        pendingCountry = normalizeCountry(attrs["tvg-country"]?.trim().orEmpty(), pendingName)
                        pendingLanguage = normalizeLanguage(attrs["tvg-language"]?.trim().orEmpty(), pendingName)
                        pendingUserAgent = ""
                    }

                    trimmed.startsWith("#EXTGRP:", ignoreCase = true) -> {
                        val grp = trimmed.substringAfter(":").trim()
                        if (grp.isNotEmpty() && pendingGroup.isBlank()) {
                            pendingGroup = normalizeCategory(grp)
                        }
                    }

                    trimmed.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                        val opt = trimmed.substringAfter(":").trim()
                        if (opt.startsWith("http-user-agent=", ignoreCase = true)) {
                            pendingUserAgent = opt.substringAfter("=").trim()
                        }
                    }

                    !trimmed.startsWith("#") -> {
                        if (isSupportedStreamUrl(trimmed)) {
                            val name = if (hasPendingExtInf) pendingName else "Stream ${orderIndex + 1}"
                            val category = if (pendingGroup.isNotBlank()) pendingGroup else inferCategoryFromName(name)
                            val country = pendingCountry.ifBlank { "INT" }
                            val language = pendingLanguage.ifBlank { inferLanguageFromName(name) }
                            val id = stableId(serverId, trimmed, name)

                            channels.add(
                                ChannelEntity(
                                    id = id,
                                    name = name,
                                    logoUrl = pendingLogo,
                                    streamUrl = trimmed,
                                    category = category,
                                    country = country,
                                    language = language,
                                    tvgId = pendingTvgId,
                                    userAgent = pendingUserAgent,
                                    serverId = serverId,
                                    sortOrder = orderIndex++
                                )
                            )
                        }
                        hasPendingExtInf = false
                        pendingName = ""
                        pendingTvgId = ""
                        pendingLogo = ""
                        pendingGroup = ""
                        pendingCountry = ""
                        pendingLanguage = ""
                        pendingUserAgent = ""
                    }
                }
                line = br.readLine()
            }
        }

        return M3uParseResult(channels = channels, detectedEpgUrl = detectedEpgUrl)
    }

    fun exportM3u(channels: List<ChannelEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        for (ch in channels) {
            sb.append("#EXTINF:-1")
            if (ch.tvgId.isNotBlank()) sb.append(" tvg-id=\"${ch.tvgId}\"")
            sb.append(" tvg-name=\"${ch.name.replace("\"", "'")}\"")
            if (ch.logoUrl.isNotBlank()) sb.append(" tvg-logo=\"${ch.logoUrl}\"")
            if (ch.country.isNotBlank()) sb.append(" tvg-country=\"${ch.country}\"")
            if (ch.language.isNotBlank()) sb.append(" tvg-language=\"${ch.language}\"")
            if (ch.category.isNotBlank()) sb.append(" group-title=\"${ch.category}\"")
            sb.append(",").appendLine(ch.name)
            if (ch.userAgent.isNotBlank()) {
                sb.appendLine("#EXTVLCOPT:http-user-agent=${ch.userAgent}")
            }
            sb.appendLine(ch.streamUrl)
        }
        return sb.toString()
    }

    private fun parseAttributes(line: String): Map<String, String> {
        val map = HashMap<String, String>()
        for (match in attrRegex.findAll(line)) {
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]
            map[key] = value
        }
        return map
    }

    private fun findDisplayTitleCommaIndex(line: String): Int {
        var inQuotes = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') inQuotes = !inQuotes
            else if (c == ',' && !inQuotes) return i
        }
        return line.lastIndexOf(',')
    }

    private fun isSupportedStreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("rtmp://") ||
            lower.startsWith("rtsp://")
    }

    private fun normalizeCategory(raw: String): String {
        val first = raw.split(";").firstOrNull()?.trim().orEmpty()
        if (first.isBlank()) return "General"
        return when (first.lowercase()) {
            "news" -> "News"
            "sports", "sport" -> "Sports"
            "movies", "cinema", "film" -> "Movies"
            "kids", "children", "animation" -> "Kids"
            "music" -> "Music"
            "religious", "religion", "islamic" -> "Religious"
            "documentary", "science", "education" -> "Documentary"
            "entertainment", "variety" -> "Entertainment"
            "series", "drama" -> "Series"
            "undefined" -> "General"
            else -> first.replaceFirstChar { it.uppercase() }
        }
    }

    private fun normalizeCountry(raw: String, name: String): String {
        val first = raw.split(";").firstOrNull()?.trim().orEmpty()
        if (first.isNotBlank()) return first.uppercase()
        val lower = name.lowercase()
        return when {
            lower.contains("saudi") || lower.contains("ksa") || lower.contains("السعودية") -> "SA"
            lower.contains("egypt") || lower.contains("مصر") -> "EG"
            lower.contains("uae") || lower.contains("dubai") || lower.contains("abu dhabi") || lower.contains("الإمارات") -> "AE"
            lower.contains("qatar") || lower.contains("bein") || lower.contains("aljazeera") || lower.contains("قطر") -> "QA"
            lower.contains("kuwait") || lower.contains("الكويت") -> "KW"
            lower.contains("morocco") || lower.contains("المغرب") -> "MA"
            lower.contains("algeria") || lower.contains("الجزائر") -> "DZ"
            lower.contains("iraq") || lower.contains("العراق") -> "IQ"
            lower.contains("jordan") || lower.contains("الأردن") -> "JO"
            else -> "INT"
        }
    }

    private fun normalizeLanguage(raw: String, name: String): String {
        val first = raw.split(";").firstOrNull()?.trim().orEmpty()
        if (first.isNotBlank()) return first.replaceFirstChar { it.uppercase() }
        return inferLanguageFromName(name)
    }

    private fun inferCategoryFromName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("sport") || lower.contains("ssc") || lower.contains("bein") || lower.contains("رياض") -> "Sports"
            lower.contains("news") || lower.contains("jazeera") || lower.contains("alarabiya") || lower.contains("إخبار") || lower.contains("أخبار") -> "News"
            lower.contains("kid") || lower.contains("cartoon") || lower.contains("spacetoon") || lower.contains("mbc3") || lower.contains("أطفال") -> "Kids"
            lower.contains("movie") || lower.contains("cinema") || lower.contains("aflam") || lower.contains("أفلام") -> "Movies"
            lower.contains("quran") || lower.contains("sunnah") || lower.contains("قرآن") || lower.contains("السنة") -> "Religious"
            lower.contains("music") || lower.contains("rotana") || lower.contains("موسيقى") -> "Music"
            lower.contains("doc") || lower.contains("national geographic") || lower.contains("وثائق") -> "Documentary"
            else -> "General"
        }
    }

    private fun inferLanguageFromName(name: String): String {
        val hasArabicScript = name.any { it in '\u0600'..'\u06FF' }
        return if (hasArabicScript) "Arabic" else "English"
    }

    private fun stableId(serverId: String, url: String, name: String): String {
        val input = "$serverId|$url|$name"
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(20)
    }
}
