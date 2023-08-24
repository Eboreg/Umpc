package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.PlaylistType
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.repository.DynamicPlaylistRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class PlaylistListViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    private val dynamicPlaylistRepo: DynamicPlaylistRepository,
) : BaseViewModel(repo, messageRepo), OnMPDChangeListener {
    private val _displayType = MutableStateFlow(PlaylistType.STORED)

    val displayType = _displayType.asStateFlow()
    val dynamicPlaylists = dynamicPlaylistRepo.dynamicPlaylists

    init {
        dynamicPlaylistRepo.loadDynamicPlaylists()
        repo.loadPlaylistsWithSongs()
        repo.registerOnMPDChangeListener(this)
    }

    fun activateDynamicPlaylist(playlist: DynamicPlaylist) = dynamicPlaylistRepo.activateDynamicPlaylist(playlist)

    fun createDynamicPlaylist(filter: DynamicPlaylistFilter, shuffle: Boolean) {
        dynamicPlaylistRepo.addDynamicPlaylist(filter, shuffle)
        dynamicPlaylistRepo.saveDynamicPlaylists()
    }

    fun deleteDynamicPlaylist(playlist: DynamicPlaylist) {
        dynamicPlaylistRepo.deleteDynamicPlaylist(playlist)
        dynamicPlaylistRepo.saveDynamicPlaylists()
    }

    fun setDisplayType(value: PlaylistType) {
        _displayType.value = value
    }

    fun updateDynamicPlaylist(
        playlist: DynamicPlaylist,
        filter: DynamicPlaylistFilter,
        shuffle: Boolean,
    ) = dynamicPlaylistRepo.updateDynamicPlaylist(playlist, filter, shuffle)

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("stored_playlist")) {
            repo.loadPlaylistsWithSongs(forceReload = true)
        }
    }
}
