package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NabdApplication
import com.example.data.local.AppSettings
import com.example.data.model.CategoryConfigEntity
import com.example.data.model.ChannelEntity
import com.example.data.model.DownloadEntity
import com.example.data.model.EpgProgramEntity
import com.example.data.model.EpisodeEntity
import com.example.data.model.MovieEntity
import com.example.data.model.SearchHistoryEntity
import com.example.data.model.SeriesEntity
import com.example.data.model.StreamHealthStatus
import com.example.data.model.WatchHistoryEntity
import com.example.data.repository.NabdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveFilterState(
    val query: String = "",
    val category: String = "All",
    val country: String = "All",
    val language: String = "All",
    val sortByAZ: Boolean = false,
    val isGridView: Boolean = true,
    val hideDead: Boolean = false
)

class MainViewModel(
    application: Application,
    private val repository: NabdRepository
) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity(application))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _healthCheckProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val healthCheckProgress: StateFlow<Pair<Int, Int>?> = _healthCheckProgress.asStateFlow()

    private val _selectedHomeCategory = MutableStateFlow("All")
    val selectedHomeCategory: StateFlow<String> = _selectedHomeCategory.asStateFlow()

    private val _liveFilter = MutableStateFlow(LiveFilterState())
    val liveFilter: StateFlow<LiveFilterState> = _liveFilter.asStateFlow()

    val allChannels: StateFlow<List<ChannelEntity>> = repository.allChannelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryConfigs: StateFlow<List<CategoryConfigEntity>> = repository.categoryConfigsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleChannels: StateFlow<List<ChannelEntity>> = combine(
        allChannels,
        settings,
        categoryConfigs
    ) { channels, prefs, configs ->
        val invisible = configs.filter { !it.isVisible }.map { it.name }.toSet()
        val kidsHidden = if (prefs.kidsLockEnabled) prefs.hiddenCategories else emptySet()
        val blocked = invisible + kidsHidden
        channels.filter { ch ->
            !blocked.contains(ch.category) &&
                (!prefs.hideDeadChannels || ch.healthStatus != StreamHealthStatus.OFFLINE)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLiveChannels: StateFlow<List<ChannelEntity>> = combine(
        visibleChannels,
        _liveFilter
    ) { channels, filter ->
        val filtered = channels.filter { ch ->
            val matchCat = filter.category == "All" || ch.category.equals(filter.category, ignoreCase = true)
            val matchCountry = filter.country == "All" || ch.country.equals(filter.country, ignoreCase = true)
            val matchLang = filter.language == "All" || ch.language.equals(filter.language, ignoreCase = true)
            val matchDead = !filter.hideDead || ch.healthStatus != StreamHealthStatus.OFFLINE
            val matchQuery = filter.query.isBlank() ||
                ch.name.contains(filter.query, ignoreCase = true) ||
                ch.category.contains(filter.query, ignoreCase = true) ||
                ch.country.contains(filter.query, ignoreCase = true)
            matchCat && matchCountry && matchLang && matchDead && matchQuery
        }
        if (filter.sortByAZ) filtered.sortedBy { it.name.lowercase() } else filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularChannels: StateFlow<List<ChannelEntity>> = combine(
        visibleChannels,
        _selectedHomeCategory
    ) { channels, cat ->
        val base = if (cat == "All") channels else channels.filter { it.category.equals(cat, true) }
        base.sortedByDescending { it.viewCount }.take(20)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChannels: StateFlow<List<ChannelEntity>> = visibleChannels
        .combine(_selectedHomeCategory) { channels, cat ->
            val base = if (cat == "All") channels else channels.filter { it.category.equals(cat, true) }
            base.sortedByDescending { it.addedAt }.take(20)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredChannels: StateFlow<List<ChannelEntity>> = visibleChannels
        .combine(repository.featuredChannelsFlow) { visible, featured ->
            val visibleIds = visible.map { it.id }.toSet()
            val explicitFeatured = featured.filter { visibleIds.contains(it.id) }
            if (explicitFeatured.isNotEmpty()) explicitFeatured else visible.take(5)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.channelCategoriesFlow
        .combine(settings) { cats, prefs ->
            val kidsHidden = if (prefs.kidsLockEnabled) prefs.hiddenCategories else emptySet()
            cats.filterNot { kidsHidden.contains(it) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countries: StateFlow<List<String>> = repository.channelCountriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val languages: StateFlow<List<String>> = repository.channelLanguagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMovies: StateFlow<List<MovieEntity>> = repository.allMoviesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSeries: StateFlow<List<SeriesEntity>> = repository.allSeriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels: StateFlow<List<ChannelEntity>> = repository.favoriteChannelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteMovies: StateFlow<List<MovieEntity>> = repository.favoriteMoviesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSeries: StateFlow<List<SeriesEntity>> = repository.favoriteSeriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.watchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = repository.downloadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val epgPrograms: StateFlow<List<EpgProgramEntity>> = repository.observeUpcomingEpg()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = repository.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        registerConnectivityCallback(application)
        viewModelScope.launch {
            refreshPlaylists(force = false)
        }
    }

    fun refreshPlaylists(force: Boolean = true) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            val result = repository.refreshAllEnabledServers(force = force)
            result.onFailure {
                _syncMessage.value = it.localizedMessage ?: "Sync error"
            }
            _isSyncing.value = false
        }
    }

    fun completeFirstLaunch(language: String) {
        viewModelScope.launch {
            repository.settingsStore.completeFirstLaunch(language)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            repository.settingsStore.setLanguage(language)
        }
    }

    fun selectHomeCategory(cat: String) {
        _selectedHomeCategory.value = cat
    }

    fun updateLiveFilter(transform: (LiveFilterState) -> LiveFilterState) {
        _liveFilter.value = transform(_liveFilter.value)
    }

    fun runChannelHealthCheck() {
        if (_healthCheckProgress.value != null) return
        viewModelScope.launch {
            val targets = filteredLiveChannels.value.ifEmpty { visibleChannels.value }
            if (targets.isEmpty()) return@launch
            _healthCheckProgress.value = 0 to targets.take(80).size
            repository.runHealthCheck(targets) { done, total ->
                _healthCheckProgress.value = done to total
            }
            _healthCheckProgress.value = null
        }
    }

    fun toggleChannelFavorite(channel: ChannelEntity) {
        viewModelScope.launch { repository.toggleChannelFavorite(channel) }
    }

    fun toggleMovieFavorite(movie: MovieEntity) {
        viewModelScope.launch { repository.toggleMovieFavorite(movie) }
    }

    fun toggleSeriesFavorite(series: SeriesEntity) {
        viewModelScope.launch { repository.toggleSeriesFavorite(series) }
    }

    fun downloadMovie(movie: MovieEntity) {
        viewModelScope.launch { repository.downloadMovie(movie) }
    }

    fun removeDownload(item: DownloadEntity) {
        viewModelScope.launch { repository.removeDownload(item) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun saveSearchQuery(query: String) {
        viewModelScope.launch { repository.saveSearchQuery(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { repository.clearSearchHistory() }
    }

    fun loadEpg(url: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.settingsStore.setEpgUrl(url)
            repository.loadEpgFromUrl(url)
            _isSyncing.value = false
        }
    }

    fun observeEpisodesForSeries(seriesId: String): Flow<List<EpisodeEntity>> =
        repository.observeEpisodesForSeries(seriesId)

    // Settings updates
    fun setDefaultQuality(quality: String) = viewModelScope.launch {
        repository.settingsStore.setDefaultQuality(quality)
    }

    fun setDataSaverMode(enabled: Boolean) = viewModelScope.launch {
        repository.settingsStore.setDataSaverMode(enabled)
    }

    fun setAudioOnlyDefault(enabled: Boolean) = viewModelScope.launch {
        repository.settingsStore.setAudioOnlyDefault(enabled)
    }

    fun setAutoFullscreen(enabled: Boolean) = viewModelScope.launch {
        repository.settingsStore.setAutoFullscreen(enabled)
    }

    fun setAutoNextEpisode(enabled: Boolean) = viewModelScope.launch {
        repository.settingsStore.setAutoNextEpisode(enabled)
    }

    fun setBackgroundPlayback(enabled: Boolean) = viewModelScope.launch {
        repository.settingsStore.setBackgroundPlayback(enabled)
    }

    fun setHideDeadChannels(hide: Boolean) = viewModelScope.launch {
        repository.settingsStore.setHideDeadChannels(hide)
        _liveFilter.value = _liveFilter.value.copy(hideDead = hide)
    }

    fun setAutoRefreshHours(hours: Int) = viewModelScope.launch {
        repository.settingsStore.setAutoRefreshHours(hours)
    }

    fun setKidsLock(enabled: Boolean, newPin: String? = null) = viewModelScope.launch {
        repository.settingsStore.setKidsLock(enabled, newPin)
    }

    fun toggleHiddenCategory(category: String) = viewModelScope.launch {
        val current = settings.value.hiddenCategories.toMutableSet()
        if (current.contains(category)) current.remove(category) else current.add(category)
        repository.settingsStore.setHiddenCategories(current)
    }

    fun setAdminPin(newPin: String) = viewModelScope.launch {
        repository.settingsStore.setAdminPin(newPin)
    }

    fun clearCache() = viewModelScope.launch {
        repository.clearCache()
    }

    private fun checkCurrentConnectivity(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerConnectivityCallback(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
        }
    }

    class Factory(private val app: NabdApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(app, app.repository) as T
        }
    }
}
