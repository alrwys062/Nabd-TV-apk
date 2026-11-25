package com.example.services

import com.example.data.model.ChannelEntity
import com.example.data.model.MovieEntity
import com.example.data.model.SeriesEntity

object AdultContentFilter {

    private val categoryAdultRegex = Regex(
        pattern = """(?i)(?:^|[^a-zA-Z0-9])(xxx|adult|adults|adults\s*only|for\s*adults|porn|porno|erotic|erotica|erotique|18\s*\+|\+\s*18|nsfw|sex\s*tv|playboy|hustler|blue\s*hustler|dorcel|marc\s*dorcel|redlight|red\s*light|brazzers|penthouse|pinko|pink\s*o|vivid|beate\s*uhse|evil\s*angel|private\s*tv|private\s*spice|xxl\s*tv|xxl|erox|sextreme|babes\s*tv|naughty|إباحي|إباحية|اباحي|اباحية|للكبار|كبار\s*فقط)(?:$|[^a-zA-Z0-9])"""
    )

    private val channelAdultRegex = Regex(
        pattern = """(?i)(?:^|[^a-zA-Z0-9])(xxx|porn|porno|erotic|erotica|erotique|erox|eroxxx|18\s*\+|\+\s*18|nsfw|playboy|hustler|blue\s*hustler|brazzers|dorcel|marc\s*dorcel|redlight|red\s*light|penthouse|vivid|beate\s*uhse|evil\s*angel|pinko|pink\s*o|sextreme|sex\s*tv|adult\s*tv|babes\s*tv|reality\s*kings|bang\s*bros|naughty\s*america|mofos|fake\s*taxi|vixen|tushy|blacked|private\s*spice|dusk\s*tv|xxl\s*tv|superone|satisfaction\s*hd|centoxcento|venus\s*hd|إباحي|إباحية|اباحي|اباحية|للكبار\s*فقط|سكس|بورن)(?:$|[^a-zA-Z0-9])"""
    )

    fun isAdultCategory(category: String): Boolean {
        if (category.isBlank()) return false
        return categoryAdultRegex.containsMatchIn(category)
    }

    fun isAdultChannel(name: String, category: String): Boolean {
        if (isAdultCategory(category)) return true
        if (name.isBlank()) return false
        return channelAdultRegex.containsMatchIn(name)
    }

    fun isAdultChannel(channel: ChannelEntity): Boolean =
        isAdultChannel(channel.name, channel.category)

    fun isAdultMovie(movie: MovieEntity): Boolean {
        if (isAdultCategory(movie.category)) return true
        val t = movie.title
        if (t.isBlank()) return false
        return Regex("""(?i)(?:^|[^a-zA-Z0-9])(porn|porno|erotic|erotica|18\s*\+|\+\s*18|brazzers|dorcel|playboy|hustler|إباحي|اباحي|للكبار\s*فقط)(?:$|[^a-zA-Z0-9])""")
            .containsMatchIn(t)
    }

    fun isAdultSeries(series: SeriesEntity): Boolean {
        if (isAdultCategory(series.category)) return true
        val t = series.title
        if (t.isBlank()) return false
        return Regex("""(?i)(?:^|[^a-zA-Z0-9])(porn|porno|erotic|erotica|18\s*\+|\+\s*18|brazzers|dorcel|playboy|hustler|إباحي|اباحي|للكبار\s*فقط)(?:$|[^a-zA-Z0-9])""")
            .containsMatchIn(t)
    }
}
