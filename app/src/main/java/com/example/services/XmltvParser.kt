package com.example.services

import android.util.Xml
import com.example.data.model.EpgProgramEntity
import org.xmlpull.v1.XmlPullParser
import java.io.Reader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object XmltvParser {

    private val formats = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    )

    fun parse(reader: Reader): List<EpgProgramEntity> {
        val programs = ArrayList<EpgProgramEntity>(512)
        val channelNames = HashMap<String, String>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(reader)

        var currentChannelId = ""
        var currentStart = 0L
        var currentStop = 0L
        var currentTitle = ""
        var currentDesc = ""
        var insideProgramme = false
        var insideChannel = false
        var parsingChannelId = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "channel" -> {
                            insideChannel = true
                            parsingChannelId = parser.getAttributeValue(null, "id").orEmpty()
                        }
                        "display-name" -> {
                            if (insideChannel && parsingChannelId.isNotBlank()) {
                                val text = safeNextText(parser)
                                if (text.isNotBlank()) {
                                    channelNames[parsingChannelId] = text
                                }
                            }
                        }
                        "programme" -> {
                            insideProgramme = true
                            currentChannelId = parser.getAttributeValue(null, "channel").orEmpty()
                            currentStart = parseXmltvDate(parser.getAttributeValue(null, "start").orEmpty())
                            currentStop = parseXmltvDate(parser.getAttributeValue(null, "stop").orEmpty())
                            currentTitle = ""
                            currentDesc = ""
                        }
                        "title" -> {
                            if (insideProgramme) {
                                currentTitle = safeNextText(parser)
                            }
                        }
                        "desc" -> {
                            if (insideProgramme) {
                                currentDesc = safeNextText(parser)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.lowercase()) {
                        "channel" -> {
                            insideChannel = false
                            parsingChannelId = ""
                        }
                        "programme" -> {
                            if (insideProgramme && currentChannelId.isNotBlank() && currentTitle.isNotBlank() && currentStop > currentStart) {
                                programs.add(
                                    EpgProgramEntity(
                                        id = "${currentChannelId}_$currentStart",
                                        channelTvgId = currentChannelId,
                                        channelName = channelNames[currentChannelId] ?: currentChannelId,
                                        title = currentTitle,
                                        description = currentDesc,
                                        startTimeMs = currentStart,
                                        endTimeMs = currentStop
                                    )
                                )
                            }
                            insideProgramme = false
                            if (programs.size >= 4000) return programs
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return programs
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return runCatching { parser.nextText().trim() }.getOrDefault("")
    }

    private fun parseXmltvDate(raw: String): Long {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return 0L
        for (fmt in formats) {
            val parsed = runCatching { fmt.parse(cleaned)?.time }.getOrNull()
            if (parsed != null) return parsed
        }
        return 0L
    }
}
