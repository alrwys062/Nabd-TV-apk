package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NabdRoute {
    @Serializable
    data object Splash : NabdRoute

    @Serializable
    data object Welcome : NabdRoute

    @Serializable
    data object Home : NabdRoute

    @Serializable
    data object Live : NabdRoute

    @Serializable
    data object Movies : NabdRoute

    @Serializable
    data object Series : NabdRoute

    @Serializable
    data object Settings : NabdRoute

    @Serializable
    data object Search : NabdRoute

    @Serializable
    data class Library(val initialTab: Int = 0) : NabdRoute

    @Serializable
    data object Epg : NabdRoute

    @Serializable
    data object Admin : NabdRoute

    @Serializable
    data class MovieDetail(val movieId: String) : NabdRoute

    @Serializable
    data class SeriesDetail(val seriesId: String) : NabdRoute

    @Serializable
    data class Player(
        val contentType: String, // "CHANNEL", "MOVIE", "EPISODE", "DOWNLOAD"
        val contentId: String,
        val seriesId: String = ""
    ) : NabdRoute
}
