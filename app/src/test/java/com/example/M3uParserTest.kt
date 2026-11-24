package com.example

import com.example.services.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class M3uParserTest {

    @Test
    fun `parses m3u attributes and exports valid m3u`() {
        val sampleM3u = """
            #EXTM3U url-tvg="https://example.com/epg.xml"
            #EXTINF:-1 tvg-id="aljazeera.qa" tvg-name="Al Jazeera" tvg-logo="https://example.com/aj.png" tvg-country="QA" tvg-language="Arabic" group-title="News",Al Jazeera HD
            #EXTVLCOPT:http-user-agent=CustomUA/1.0
            https://example.com/live/aj.m3u8
            #EXTINF:-1 group-title="Sports",SSC Sports 1
            https://example.com/live/ssc1.m3u8
        """.trimIndent()

        val result = M3uParser.parse(StringReader(sampleM3u), "test_server")
        assertEquals("https://example.com/epg.xml", result.detectedEpgUrl)
        assertEquals(2, result.channels.size)

        val ch1 = result.channels[0]
        assertEquals("Al Jazeera HD", ch1.name)
        assertEquals("aljazeera.qa", ch1.tvgId)
        assertEquals("News", ch1.category)
        assertEquals("QA", ch1.country)
        assertEquals("Arabic", ch1.language)
        assertEquals("CustomUA/1.0", ch1.userAgent)

        val exported = M3uParser.exportM3u(result.channels)
        assertTrue(exported.contains("#EXTM3U"))
        assertTrue(exported.contains("Al Jazeera HD"))
        assertTrue(exported.contains("https://example.com/live/aj.m3u8"))
    }
}
